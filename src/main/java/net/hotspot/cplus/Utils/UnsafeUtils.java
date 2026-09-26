package net.hotspot.cplus.Utils;

import net.hotspot.cplus.*;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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


}
