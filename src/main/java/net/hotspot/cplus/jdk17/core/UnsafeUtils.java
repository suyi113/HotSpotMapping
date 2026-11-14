package net.hotspot.cplus.jdk17.core;


import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.MemoryPoolMXBean;

import net.hotspot.cplus.jdk17.classloading.ClassLoaderData;
import net.hotspot.cplus.jdk17.codecache.CodeBuilder;
import net.hotspot.cplus.jdk17.klass.ConstantPool;
import net.hotspot.cplus.jdk17.klass.ConstantPoolCache;
import net.hotspot.cplus.jdk17.klass.Symbol;
import net.hotspot.cplus.jdk17.klass.ConstMethod;
import net.hotspot.cplus.jdk17.klass.InstanceKlass;
import net.hotspot.cplus.jdk17.klass.Klass;
import net.hotspot.cplus.jdk17.threads.JavaThread;
import net.hotspot.cplus.jdk17.threads.ThreadsList;
import net.hotspot.cplus.jdk17.threads.ThreadsSMRSupport;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.spi.AttachProvider;
import sun.misc.Unsafe;

import java.lang.management.ManagementFactory;
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

    public static void registerJvmDll() {
        String javaHome = System.getProperty("java.home");
        String jvmDll = javaHome + "\\bin\\server\\jvm.dll";
        java.io.File dll = new java.io.File(jvmDll);
        if (!dll.exists()) {
            System.err.println("[HotSpotMapping] jvm.dll not found: " + jvmDll);
            return;
        }
        try {
            System.load(jvmDll);
            System.out.println("[HotSpotMapping] jvm.dll loaded explicitly");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[HotSpotMapping] jvm.dll already loaded (expected), skipping. reason=" + e.getMessage());
        } catch (Throwable t) {
            System.err.println("[HotSpotMapping] registerJvmDll failed:");
            t.printStackTrace();
        }
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
    /**
     * 把一个类的方法字节码、max_stack/max_locals 和编译标记恢复成 class 文件里的状态，
     * 并丢弃所有已编译的旧代码（含内联副本）。
     *
     * @return 恢复的方法数量
     */
    public static int restoreClass(Class<?> c) {
        byte[] bytes = readClassBytes(c);
        net.hotspot.cplus.jdk17.klass.ClassFile cf =
                net.hotspot.cplus.jdk17.klass.ClassFile.parse(bytes);
        InstanceKlass ik = new InstanceKlass(c);

        java.util.List<net.hotspot.cplus.jdk17.klass.Method> restored = new java.util.ArrayList<>();
        for (net.hotspot.cplus.jdk17.klass.Method m : ik.methodsArray().toMethods()) {
            net.hotspot.cplus.jdk17.klass.ClassFile.MethodInfo mi =
                    cf.find(m.name(), m.signature());
            if (mi == null || mi.code == null) continue;
            byte[] code = rewriteForRuntime(ik, mi.code);
            ConstMethod cm = new ConstMethod(m.constMethod);
            cm.setCodeAndSize(code);
            cm.setMaxStack(mi.maxStack);
            cm.setMaxLocals(mi.maxLocals);
            ClassRestorer.restoreMethod(ik, m, mi);
            m.clearNotCompilable();
            m.forceInterpreted();
            restored.add(m);
        }

        ConstantPool cp = new ConstantPool(ik.constants);
        if (cf.constantPoolCount < cp.length) {
            unsafe.putInt(cp.getAddress() + ConstantPool.LENGTH_OFFSET, cf.constantPoolCount);
        }

        deoptimizeCallers(restored);
        return restored.size();
    }

    private static byte[] readClassBytes(Class<?> c) {
        String path = "/" + c.getName().replace('.', '/') + ".class";
        try (java.io.InputStream in = c.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("class file not found: " + path);
            }
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private static byte[] rewriteForRuntime(InstanceKlass ik, byte[] code) {
        byte[] out = code.clone();
        int pc = 0;
        while (pc < out.length) {
            int op = out[pc] & 0xFF;
            if (op >= 0xB2 && op <= 0xB9) {
                int cpIdx = ((out[pc + 1] & 0xFF) << 8) | (out[pc + 2] & 0xFF);
                int cacheIdx = ik.cacheIndexFor(cpIdx);
                out[pc + 1] = (byte) cacheIdx;
                out[pc + 2] = (byte) (cacheIdx >>> 8);
            }
            pc += instrLength(out, pc);
        }
        return out;
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
        java.util.List<net.hotspot.cplus.jdk17.klass.Method> targets = new java.util.ArrayList<>();
        for (net.hotspot.cplus.jdk17.klass.Method m : ik.methodsArray().toMethods()) {
            if ("checkCanSetAccessible".equals(m.name())) {
                targets.add(m);
            }
        }
        if (targets.isEmpty()) {
            throw new IllegalStateException("Can't find it checkCanSetAccessible");
        }
        for (net.hotspot.cplus.jdk17.klass.Method t : targets) {
            t.setNotCompilable();
            if (t.code != 0) t.forceInterpreted();
            boolean bool = t.signature().endsWith(")Z");
            CodeBuilder cb = new CodeBuilder();
            if (bool) {
                cb.iconst(1);
                cb.ireturn();
            } else {
                cb.return_();
            }
            byte[] newCode = cb.build(ik);
            ConstMethod cm = new ConstMethod(t.constMethod);
            int slot = cm.codeSize & 0xFFFF;
            if (newCode.length > slot) continue;
            cm.putCodeAndResize(newCode);
            cm.setMaxStack(bool ? 1 : 0);
            cm.setMaxLocals(Math.max(cm.maxLocals & 0xFFFF, 5));
        }
        for (net.hotspot.cplus.jdk17.klass.Method t : targets) {
            freezeTarget(t);
            deoptimizeCallers(t);
        }
    }
    public static void hookCheckAccess() throws Exception {
        InstanceKlass ik = new InstanceKlass(java.lang.reflect.AccessibleObject.class);
        net.hotspot.cplus.jdk17.klass.Method target = null;
        for (net.hotspot.cplus.jdk17.klass.Method m : ik.methodsArray().toMethods()) {
            if ("checkAccess".equals(m.name())
                    && "(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;I)V".equals(m.signature())) {
                target = m;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Can't find it checkAccess");
        }
        target.setNotCompilable();
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
        freezeTarget(target);
        deoptimizeCallers(target);
    }

    /**
     * 模拟 retransform 的原理：找出所有「调用并可能内联了 target」的已编译方法，
     * 把它们强制退回解释执行，丢弃带内联副本的 nmethod。
     */
    public static int deoptimizeCallers(net.hotspot.cplus.jdk17.klass.Method target) {
        java.util.List<net.hotspot.cplus.jdk17.klass.Method> one = new java.util.ArrayList<>();
        one.add(target);
        return deoptimizeCallers(one);
    }

    public static int deoptimizeCallers(
            java.util.Collection<net.hotspot.cplus.jdk17.klass.Method> targets) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (net.hotspot.cplus.jdk17.klass.Method t : targets) {
            if (t.name() != null && t.signature() != null) {
                keys.add(t.name() + '\u0000' + t.signature());
            }
        }
        if (keys.isEmpty()) return 0;

        int total = 0;
        int quiet = 0;
        for (int pass = 0; pass < 100 && quiet < 20; pass++) {
            int n = deoptimizeOnce(keys);
            total += n;
            if (n == 0) {
                quiet++;
            } else {
                quiet = 0;
            }
            if (quiet < 20) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return total;
    }

    private static int deoptimizeOnce(java.util.Set<String> keys) {
        int count = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Class<?> c : getAllLoadedClass()) {
                if (c.isArray() || c.isPrimitive()) continue;
                try {
                    InstanceKlass ik = new InstanceKlass(c);
                    if (ik.constants == 0) continue;
                    ConstantPool cp = new ConstantPool(ik.constants);
                    ConstantPoolCache cpc = cp.getCache();
                    if (cpc == null) continue;
                    for (net.hotspot.cplus.jdk17.klass.Method m : ik.methodsArray().toMethods()) {
                        if (m.code == 0) continue;
                        String own = m.name() + '\u0000' + m.signature();
                        if (keys.contains(own)) continue;
                        if (invokesAny(m, cp, cpc, keys)) {
                            m.setNotCompilable();
                            m.forceInterpreted();
                            keys.add(own);
                            count++;
                            changed = true;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return count;
    }

    private static void freezeTarget(net.hotspot.cplus.jdk17.klass.Method target) {
        int quiet = 0;
        for (int i = 0; i < 600 && quiet < 60; i++) {
            if (target.code != 0) {
                target.forceInterpreted();
                quiet = 0;
            } else {
                quiet++;
            }
            if (quiet >= 60) break;
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static boolean invokesAny(net.hotspot.cplus.jdk17.klass.Method m,
                                      ConstantPool cp, ConstantPoolCache cpc,
                                      java.util.Set<String> keys) {
        ConstMethod cm = new ConstMethod(m.constMethod);
        int len = cm.codeSize & 0xFFFF;
        if (len <= 2) return false;
        byte[] code = cm.code();
        int pc = 0;
        while (pc < len) {
            int op = code[pc] & 0xFF;
            if (op == 0xB6 || op == 0xB7 || op == 0xB8 || op == 0xB9) {
                if (pc + 2 >= len) break;
                int idx = (code[pc + 1] & 0xFF) | ((code[pc + 2] & 0xFF) << 8);
                String key = refKey(cp, cpc, idx);
                if (key != null && keys.contains(key)) return true;
            }
            pc += instrLength(code, pc);
        }
        return false;
    }

    private static String refKey(ConstantPool cp, ConstantPoolCache cpc, int cacheIdx) {
        try {
            int cpIdx = cpc.entryConstantPoolIndex(cacheIdx);
            if (cpIdx <= 0 || cpIdx >= cp.length) return null;
            int tag = unsafe.getByte(cp.tags + 4 + cpIdx) & 0xFF;
            if (tag != 10 && tag != 11) return null;
            long slot = cp.slotAt(cpIdx);
            int natIdx = (int) ((slot >>> 16) & 0xFFFF);
            long nat = cp.slotAt(natIdx);
            int nameIdx = (int) (nat & 0xFFFF);
            int sigIdx = (int) ((nat >>> 16) & 0xFFFF);
            long n = cp.symbolAt(nameIdx);
            long s = cp.symbolAt(sigIdx);
            if (n == 0 || s == 0) return null;
            return new Symbol(n).value + '\u0000' + new Symbol(s).value;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int instrLength(byte[] code, int pc) {
        int op = code[pc] & 0xFF;
        switch (op) {
            case 0x10: case 0x12: case 0x15: case 0x16: case 0x17: case 0x18: case 0x19:
            case 0x36: case 0x37: case 0x38: case 0x39: case 0x3a:
            case 0xa9: case 0xbc:
                return 2;
            case 0x11: case 0x13: case 0x14: case 0x84:
            case 0x99: case 0x9a: case 0x9b: case 0x9c: case 0x9d: case 0x9e:
            case 0x9f: case 0xa0: case 0xa1: case 0xa2: case 0xa3: case 0xa4:
            case 0xa5: case 0xa6: case 0xa7: case 0xa8:
            case 0xb2: case 0xb3: case 0xb4: case 0xb5: case 0xb6: case 0xb7: case 0xb8:
            case 0xbb: case 0xbd: case 0xc0: case 0xc1: case 0xc6: case 0xc7:
                return 3;
            case 0xc5:
                return 4;
            case 0xb9: case 0xba: case 0xc8: case 0xc9:
                return 5;
            case 0xaa: {
                int p = (pc + 4) & ~3;
                int low = read4(code, p + 4);
                int high = read4(code, p + 8);
                return p + 12 + (high - low + 1) * 4 - pc;
            }
            case 0xab: {
                int p = (pc + 4) & ~3;
                int npairs = read4(code, p + 4);
                return p + 8 + npairs * 8 - pc;
            }
            case 0xc4: {
                int op2 = code[pc + 1] & 0xFF;
                return op2 == 0x84 ? 6 : 4;
            }
            default:
                return 1;
        }
    }

    private static int read4(byte[] code, int p) {
        return ((code[p] & 0xFF) << 24) | ((code[p + 1] & 0xFF) << 16)
                | ((code[p + 2] & 0xFF) << 8) | (code[p + 3] & 0xFF);
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
    public static long addressOf(Object o) {
        return OopDesc.addressOf(o);
    }

    public static long klassAddressOf(Object o) {
        if (o == null) return 0;
        return OopDesc.of(o).klass;
    }

    public static InstanceKlass instanceKlassOf(Object o) {
        long k = klassAddressOf(o);
        return k == 0 ? null : new InstanceKlass(k);
    }

    public static String classNameOf(Object o) {
        InstanceKlass ik = instanceKlassOf(o);
        return ik == null ? null : ik.getName();
    }

    public static long sizeOf(Object o) {
        if (o == null) return 0;
        long k = klassAddressOf(o);
        if (k == 0) return 0;
        Klass klass = new Klass(k);
        int lh = klass.layoutHelper();
        if (lh > 0) {
            return lh & ~1;
        }
        if (lh >= 0) {
            return 0;
        }
        int log2 = lh & 0x3F;
        int header = (lh >>> 16) & 0xFF;
        int lenOffset = OopDesc.USE_COMPRESSED_CLASS_POINTERS ? 12 : 16;
        int length = unsafe.getInt(OopDesc.addressOf(o) + lenOffset);
        return header + ((long) length << log2);
    }

    public static void main(String[] args){
       System.out.println( CNative.resolve("JvmtiExport::_should_post_class_file_load_hook"));
    }

    public static byte[] read(long address, int length) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = unsafe.getByte(address + i);
        }
        return out;
    }

    public static void write(long address, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            unsafe.putByte(address + i, bytes[i]);
        }
    }

    public static Class<?> findLoadedClass(String name) {
        String internal = name.replace('.', '/');
        for (Class<?> c : getAllLoadedClass()) {
            if (internal.equals(c.getName().replace('.', '/'))) {
                return c;
            }
        }
        return null;
    }

    public static List<Class<?>> getLoadedClasses(String prefix) {
        String internal = prefix.replace('.', '/');
        List<Class<?>> out = new ArrayList<>();
        for (Class<?> c : getAllLoadedClass()) {
            if (c.getName().replace('.', '/').startsWith(internal)) {
                out.add(c);
            }
        }
        return out;
    }

    public static long threadAddress(Thread target) {
        if (target == null) return 0;
        long listAddr = ThreadsSMRSupport.javaThreadList();
        if (listAddr == 0) return 0;
        ThreadsList list = new ThreadsList(listAddr);
        for (int i = 0; i < list.length; i++) {
            JavaThread jt = list.threadAt(i);
            if (jt != null && jt.getThreadObject() == target) {
                return jt.getAddress();
            }
        }
        return 0;
    }

    public static long currentThreadAddress() {
        return threadAddress(Thread.currentThread());
    }

    public static String vmOption(String name) {
        try {
            HotSpotDiagnosticMXBean bean =
                    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            return bean.getVMOption(name).getValue();
        } catch (Throwable t) {
            return null;
        }
    }

    public static long heapUsed() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    public static long heapCommitted() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted();
    }

    public static long metaspaceUsed() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equals(pool.getName())) {
                return pool.getUsage().getUsed();
            }
        }
        return 0;
    }

    public static void patchMethod(InstanceKlass ik, net.hotspot.cplus.jdk17.klass.Method m, byte[] code) {
        if (m.code != 0) {
            m.forceInterpreted();
        }
        ConstMethod cm = new ConstMethod(m.constMethod);
        cm.putCodeAndResize(code);
        ik.commitCpChanges();
        m.setNotCompilable();
    }


}
