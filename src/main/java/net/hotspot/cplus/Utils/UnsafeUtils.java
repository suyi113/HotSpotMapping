package net.hotspot.cplus.Utils;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.spi.AttachProvider;
import net.hotspot.cplus.*;
import net.hotspot.cplus.Dictionary;
import net.hotspot.cplus.agent.AgentMain;
import sun.misc.Unsafe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.util.*;

public class UnsafeUtils {

    private static final Object[] HOLDER = new Object[1];
    private static final long HOLDER_ELEM_ADDR;

    public static Unsafe unsafe;
    static {
        try {
            Field usf = Unsafe.class.getDeclaredField("theUnsafe");
            usf.setAccessible(true);
            unsafe = (Unsafe)usf.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static {
        System.gc();
        long holderOop = OopDesc.addressOf(HOLDER);
        HOLDER_ELEM_ADDR = holderOop
                + UnsafeUtils.unsafe.arrayBaseOffset(Object[].class);
    }


    public static void registerJvmDll(){
        String javaHome = System.getProperty("java.home");
        String jvmDll = javaHome + "\\bin\\server\\jvm.dll";
        System.load(jvmDll);
    }

    public static Object oopToJavaObject(long oop) {
        if (oop == 0) return null;

        long holderOop = OopDesc.addressOf(HOLDER);
        long elemAddr = holderOop
                + UnsafeUtils.unsafe.arrayBaseOffset(Object[].class);

        if (OopDesc.USE_COMPRESSED_OOPS) {
            int narrow = (int) ((oop - OopDesc.HEAP_BASE) >>> 3);
            UnsafeUtils.unsafe.putInt(elemAddr, narrow);
        } else {
            UnsafeUtils.unsafe.putLong(elemAddr, oop);
        }

        Object result = HOLDER[0];
        HOLDER[0] = null;
        return result;
    }

    public static long getKlassLong(Class clazz){
        return unsafe.getLong(clazz,16);
    }
    public static void setAccessible(Field field,boolean flag){
        try {
            unsafe.putBoolean(field,12,flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAccessible(Constructor constructor, boolean flag){
        try {
            unsafe.putBoolean(constructor,12,flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAccessible(Method method, boolean flag){
        try {
            unsafe.putBoolean(method,12,flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?>[] getAllLoadedClass() {
        long cldHeadAddr = VMStructsHelper.findStaticAddress("ClassLoaderDataGraph::_head");
        long cldPtr = unsafe.getLong(cldHeadAddr);

        long cldKlassesOff = VMStructsHelper.findOffset("ClassLoaderData::_klasses");
        long cldNextOff    = VMStructsHelper.findOffset("ClassLoaderData::_next");
        long kNextLinkOff  = VMStructsHelper.findOffset("Klass::_next_link");
        long kMirrorOff    = VMStructsHelper.findOffset("Klass::_java_mirror");

        List<Class<?>> out = new ArrayList<>();
        Set<Long> seenCld = new HashSet<>();

        while (cldPtr != 0) {
            if (!seenCld.add(cldPtr)) break;

            long klassPtr = unsafe.getLong(cldPtr + cldKlassesOff);

            Set<Long> localChain = new HashSet<>();
            while (klassPtr != 0) {
                if (!localChain.add(klassPtr)) {
                    System.err.println("[getAllLoadedClass] Klass 环 @ 0x"
                            + Long.toHexString(klassPtr));
                    break;
                }

                try {
                    long oopHandleAddr = unsafe.getLong(klassPtr + kMirrorOff);
                    if (oopHandleAddr != 0) {
                        long mirrorOop = unsafe.getLong(oopHandleAddr);
                        if (mirrorOop != 0) {
                            Object o = oopToJavaObject(mirrorOop);
                            if (o instanceof Class<?> c) {
                                out.add(c);
                            }
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[getAllLoadedClass] klass=0x"
                            + Long.toHexString(klassPtr) + " 失败");
                    t.printStackTrace();
                }

                klassPtr = unsafe.getLong(klassPtr + kNextLinkOff);
            }

            cldPtr = unsafe.getLong(cldPtr + cldNextOff);
        }

        return out.toArray(new Class<?>[0]);
    }

    public static void hookCheckCanSetAccessible() throws Exception {
        InstanceKlass ik = new InstanceKlass(java.lang.reflect.AccessibleObject.class);

        net.hotspot.cplus.Method target = null;
        for (net.hotspot.cplus.Method m : ik.methodsArray().toMethods()) {
            if ("checkCanSetAccessible".equals(m.name())
                    && "(Ljava/lang/Class;Ljava/lang/Class;Z)Z".equals(m.signature())) {
                target = m;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Can't find it checkCanSetAccessible");
        }

        if (target.code != 0) target.forceInterpreted();

        ConstMethod cm = new ConstMethod(target.constMethod);
        int slot = cm.codeSize & 0xFFFF;

        CodeBuilder cb = new CodeBuilder();
        cb.iconst(1);
        cb.ireturn();
        byte[] newCode = cb.build(ik);

        if (newCode.length > slot) {
            throw new IllegalStateException(
                    "New bytecode " + newCode.length + "  exceeded slots  " + slot);
        }

        cm.putCodeAndResize(newCode);
        cm.setMaxStack(1);
        cm.setMaxLocals(Math.max(cm.maxLocals & 0xFFFF, 3));
    }
    public static void hookCheckAccess() throws Exception {
        InstanceKlass ik = new InstanceKlass(java.lang.reflect.AccessibleObject.class);
        net.hotspot.cplus.Method target = null;
        for (net.hotspot.cplus.Method m : ik.methodsArray().toMethods()) {
            if ("checkAccess".equals(m.name())
                    && "(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;I)V".equals(m.signature())) {
                target = m;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Can't find it checkAccess");
        }
        if (target.code != 0) target.forceInterpreted();
        ConstMethod cm = new ConstMethod(target.constMethod);
        int slot = cm.codeSize & 0xFFFF;
        CodeBuilder cb = new CodeBuilder();
        cb.return_();
        byte[] newCode = cb.build(ik);
        if (newCode.length > slot) {
            throw new IllegalStateException(
                    "New bytecode " + newCode.length + "  exceeded slots  " + slot);
        }
        cm.putCodeAndResize(newCode);
        cm.setMaxStack(0);
        cm.setMaxLocals(Math.max(cm.maxLocals & 0xFFFF, 5));
    }

    public static List<Thread> getAllThreads() {
        List<Thread> out = new ArrayList<>();

        long listAddr = ThreadsSMRSupport.javaThreadList();
        if (listAddr == 0) return out;

        ThreadsList list = new ThreadsList(listAddr);
        for (int i = 0; i < list.length; i++) {
            JavaThread jt = list.threadAt(i);
            if (jt == null) continue;

            Object o = jt.getThreadObject();
            if (o instanceof Thread t) {
                out.add(t);
            }
        }
        return out;
    }

    public static JavaThread[] getAllJavaThreads() {
        long listAddr = ThreadsSMRSupport.javaThreadList();
        if (listAddr == 0) return new JavaThread[0];
        return new ThreadsList(listAddr).toJavaThreads();
    }

    public static boolean unlinkFromCld(Class<?> target) {
        InstanceKlass targetIk = new InstanceKlass(target);
        long targetAddr = targetIk.getAddress();

        long cldHeadAddr = VMStructsHelper.findStaticAddress("ClassLoaderDataGraph::_head");
        long cldPtr = unsafe.getLong(cldHeadAddr);
        long cldKlassesOff = VMStructsHelper.findOffset("ClassLoaderData::_klasses");
        long cldNextOff    = VMStructsHelper.findOffset("ClassLoaderData::_next");
        long kNextLinkOff  = VMStructsHelper.findOffset("Klass::_next_link");

        Set<Long> seenCld = new HashSet<>();
        while (cldPtr != 0) {
            if (!seenCld.add(cldPtr)) break;

            long headAddr = cldPtr + cldKlassesOff;
            long cur = unsafe.getLong(headAddr);

            if (cur == targetAddr) {
                long next = unsafe.getLong(cur + kNextLinkOff);
                unsafe.putLong(headAddr, next);
                return true;
            }

            Set<Long> seen = new HashSet<>();
            while (cur != 0) {
                if (!seen.add(cur)) break;
                long next = unsafe.getLong(cur + kNextLinkOff);
                if (next == targetAddr) {
                    long after = unsafe.getLong(targetAddr + kNextLinkOff);
                    unsafe.putLong(cur + kNextLinkOff, after);
                    return true;
                }
                cur = next;
            }

            cldPtr = unsafe.getLong(cldPtr + cldNextOff);
        }
        return false;
    }

    public static void AbleAgent() {
        try {
            Class<?> hotspot = Class.forName("sun.tools.attach.HotSpotVirtualMachine");
            Field attach = hotspot.getDeclaredField("ALLOW_ATTACH_SELF");
            setAccessible(attach, true);
            unsafe.putBoolean(unsafe.staticFieldBase(attach), unsafe.staticFieldOffset(attach), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws Exception{
        try {
            hookCheckAccess();
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            System.err.println(f.get(null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadAgent(String path){
        try {
            AbleAgent();
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            int p = nameOfRunningVM.indexOf('@');
            String pid = nameOfRunningVM.substring(0, p);
            VirtualMachine vm = null;
            Method providers = Class.forName("com.sun.tools.attach.spi.AttachProvider").getMethod("providers");
            Method attachVirtualMachine = Class.forName("com.sun.tools.attach.spi.AttachProvider").getMethod("attachVirtualMachine", String.class);
            for (AttachProvider provider:(List<AttachProvider>)providers.invoke(null)){
                vm = (VirtualMachine) attachVirtualMachine.invoke(provider,pid);
            }
            Method loadAgent = Class.forName("com.sun.tools.attach.VirtualMachine").getMethod("loadAgent", String.class);
            loadAgent.invoke(vm,path);
            Method detach = Class.forName("com.sun.tools.attach.VirtualMachine").getMethod("detach");
            detach.invoke(vm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
