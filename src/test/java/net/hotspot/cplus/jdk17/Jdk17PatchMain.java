package net.hotspot.cplus.jdk17;

import net.hotspot.cplus.jdk17.codecache.CodeBuilder;
import net.hotspot.cplus.jdk17.core.UnsafeUtils;
import net.hotspot.cplus.jdk17.klass.ConstMethod;
import net.hotspot.cplus.jdk17.klass.ConstantPool;
import net.hotspot.cplus.jdk17.klass.InstanceKlass;
import net.hotspot.cplus.jdk17.klass.Method;
import net.hotspot.cplus.jdk17.klass.Symbol;

import java.util.ArrayList;
import java.util.List;

public final class Jdk17PatchMain {

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("java.version = " + System.getProperty("java.version"));

        patchInterpreted();
        patchCompiled();
        utilsChecks();
        cNativeChecks();
        scanMissingFieldChecks();
        nativeHookChecks();
        restoreChecks();
        restoreMetadataChecks();
        shellcodeChecks();
        patchJdkClass();

        System.out.println();
        System.out.println("通过: " + passed + "    失败: " + failed);
        if (failed > 0) {
            for (String f : failures) {
                System.out.println("  - " + f);
            }
            System.exit(1);
        }
        System.out.println("结果: PASS");
    }

    private static void patchInterpreted() {
        check("替换前 f(10) == 11", PatchTarget.f(10) == 11, "actual=" + PatchTarget.f(10));

        InstanceKlass ik = new InstanceKlass(PatchTarget.class);
        Method m = find(ik, "f", "(I)I");
        check("找到 f(I)I", m != null);
        if (m == null) return;
        check("f 尚未编译", m.code == 0, "code=0x" + Long.toHexString(m.code));

        CodeBuilder cb = new CodeBuilder();
        cb.iload(0);
        cb.invokestatic(ik, ExternalHelper.class, "twice", "(I)I");
        cb.ireturn();
        byte[] code = cb.build(ik);
        check("替换前常量池不含 ExternalHelper",
                !cpHasClass(new ConstantPool(ik.constants), "net/hotspot/cplus/jdk17/ExternalHelper"));
        check("扩展后常量池含 ExternalHelper",
                cpHasClass(new ConstantPool(ik.currentCpAddress()),
                        "net/hotspot/cplus/jdk17/ExternalHelper"));

        new ConstMethod(m.constMethod).putCodeAndResize(code);
        ik.commitCpChanges();
        m.setNotCompilable();

        ConstantPool afterCp = new ConstantPool(ik.constants);
        int cacheIdx = (code[2] & 0xFF) | ((code[3] & 0xFF) << 8);
        int cpIdx = afterCp.getCache().entryConstantPoolIndex(cacheIdx);
        int classIdx = (int) (afterCp.slotAt(cpIdx) & 0xFFFF);
        check("invokestatic 指向 ExternalHelper",
                "net/hotspot/cplus/jdk17/ExternalHelper".equals(cpClassName(afterCp, classIdx)),
                "cacheIdx=" + cacheIdx + ", cpIdx=" + cpIdx + ", " + cpClassName(afterCp, classIdx));

        check("替换后 f(10) == 20", PatchTarget.f(10) == 20, "actual=" + PatchTarget.f(10));
    }

    private static void patchCompiled() {
        int acc = 0;
        for (int i = 0; i < 300000; i++) {
            acc += PatchTarget.hot(i);
        }
        check("hot() 已执行", acc != 0);

        InstanceKlass ik = new InstanceKlass(PatchTarget.class);
        Method m = find(ik, "hot", "(I)I");
        check("找到 hot(I)I", m != null);
        if (m == null) return;
        check("hot() 已被 JIT 编译", m.code != 0, "code=0x" + Long.toHexString(m.code));

        m.forceInterpreted();
        check("forceInterpreted 后 code == 0", m.code == 0, "code=0x" + Long.toHexString(m.code));

        CodeBuilder cb = new CodeBuilder();
        cb.iload(0);
        cb.invokestatic(ik, ExternalHelper.class, "twice", "(I)I");
        cb.ireturn();
        new ConstMethod(m.constMethod).putCodeAndResize(cb.build(ik));
        ik.commitCpChanges();
        m.setNotCompilable();

        check("替换后 hot(10) == 20", PatchTarget.hot(10) == 20, "actual=" + PatchTarget.hot(10));

        long sum = 0;
        for (int i = 0; i < 300000; i++) {
            sum += PatchTarget.hot(i);
        }
        check("30 万次热调用不崩溃", sum != 0, "sum=" + sum);
        check("热调用后仍执行新字节码", PatchTarget.hot(10) == 20, "actual=" + PatchTarget.hot(10));
        check("方法没有被重新编译", m.code == 0, "code=0x" + Long.toHexString(m.code));
    }

    private static void utilsChecks() {
        check("classNameOf(String)", "java/lang/String".equals(UnsafeUtils.classNameOf("abc")),
                UnsafeUtils.classNameOf("abc"));
        check("sizeOf(new Object()) == 16", UnsafeUtils.sizeOf(new Object()) == 16,
                "size=" + UnsafeUtils.sizeOf(new Object()));
        check("sizeOf(new int[4]) == 32", UnsafeUtils.sizeOf(new int[4]) == 32,
                "size=" + UnsafeUtils.sizeOf(new int[4]));
        check("findLoadedClass(java/lang/String)",
                UnsafeUtils.findLoadedClass("java/lang/String") == String.class);
        check("currentThreadAddress 非 0", UnsafeUtils.currentThreadAddress() != 0,
                "0x" + Long.toHexString(UnsafeUtils.currentThreadAddress()));
        check("heapUsed > 0", UnsafeUtils.heapUsed() > 0, "used=" + UnsafeUtils.heapUsed());
    }

    private static void restoreChecks() {
        check("还原前 f(10) == 20", PatchTarget.f(10) == 20, "actual=" + PatchTarget.f(10));
        int n = UnsafeUtils.restoreClass(PatchTarget.class);
        check("restoreClass 恢复的方法数 > 0", n > 0, "n=" + n);
        check("还原后 f(10) == 11", PatchTarget.f(10) == 11, "actual=" + PatchTarget.f(10));
        check("还原后 hot(10) == 289", PatchTarget.hot(10) == 289, "actual=" + PatchTarget.hot(10));
    }

    private static void shellcodeChecks() {
        long pidFn = net.hotspot.cplus.jdk17.core.Shellcode.kernel32("GetCurrentProcessId");
        check("解析 kernel32!GetCurrentProcessId", pidFn != 0,
                "0x" + Long.toHexString(pidFn));

        long original = 0;
        for (int i = 0; i < 200000; i++) {
            original = ShellTarget.shell();
        }

        InstanceKlass ik = new InstanceKlass(ShellTarget.class);
        Method m = find(ik, "shell", "()J");
        check("shell() 已被 JIT 编译", m != null && m.code != 0,
                m == null ? "not found" : ("code=0x" + Long.toHexString(m.code)));
        if (m == null || m.code == 0) return;
        check("shell() 原始返回值不是 pid", original != ProcessHandle.current().pid());

        net.hotspot.cplus.jdk17.core.Shellcode.write(
                m, net.hotspot.cplus.jdk17.core.Shellcode.call(pidFn));

        long got = callShellOnce();
        long pid = ProcessHandle.current().pid();
        check("shellcode 调用 GetCurrentProcessId 得到 pid", got == pid,
                "got=" + got + ", pid=" + pid + ", original=" + original);

        catalog();
    }

    private static void catalog() {
        long ticks = runStub(net.hotspot.cplus.jdk17.core.Shellcode.kernel32("GetTickCount64"));
        check("kernel32!GetTickCount64 > 0", ticks > 0, "ticks=" + ticks);

        long tid = runStub(net.hotspot.cplus.jdk17.core.Shellcode.kernel32("GetCurrentThreadId"));
        check("kernel32!GetCurrentThreadId > 0", tid > 0, "tid=" + tid);

        long feature = runStub(
                net.hotspot.cplus.jdk17.core.Shellcode.kernel32("IsProcessorFeaturePresent"), 17);
        check("kernel32!IsProcessorFeaturePresent(17) 是 0/1",
                feature == 0 || feature == 1, "ret=" + feature);

        String[] jvmSymbols = {"JVM_GC", "JVM_ActiveProcessorCount", "JVM_CurrentTimeMillis",
                "JVM_TotalMemory", "JVM_FreeMemory", "JVM_MaxMemory"};
        for (String s : jvmSymbols) {
            long addr = net.hotspot.cplus.jdk17.core.Shellcode.symbol(s);
            check("解析 jvm!" + s, addr != 0, "0x" + Long.toHexString(addr));
        }
        String locked = net.hotspot.cplus.jdk17.core.Shellcode.exportOf(
                net.hotspot.cplus.jdk17.core.Shellcode.symbol("JVM_GC"));
        check("反查地址锁定 JVM_GC", "JVM_GC".equals(locked), String.valueOf(locked));
    }

    private static long runStub(long fn, long... args) {
        InstanceKlass ik = new InstanceKlass(ShellTarget.class);
        Method m = find(ik, "shell", "()J");
        byte[] code;
        if (args.length == 0) {
            code = net.hotspot.cplus.jdk17.core.Shellcode.call(fn);
        } else if (args.length == 1) {
            code = net.hotspot.cplus.jdk17.core.Shellcode.call(fn, args[0]);
        } else {
            code = net.hotspot.cplus.jdk17.core.Shellcode.call(fn, args[0], args[1]);
        }
        net.hotspot.cplus.jdk17.core.Shellcode.write(m, code);
        return callShellOnce();
    }

    private static long callShellOnce() {
        return ShellTarget.shell();
    }

    private static void cNativeChecks() {
        check("symbol(gHotSpotVMStructs) 非 0",
                net.hotspot.cplus.jdk17.core.CNative.symbol("gHotSpotVMStructs") != 0);
        check("symbol(JVM_GC) 非 0",
                net.hotspot.cplus.jdk17.core.CNative.symbol("JVM_GC") != 0);

        long jvmti = net.hotspot.cplus.jdk17.core.CNative.symbol(
                "JvmtiExport::_can_access_local_variables");
        check("symbol(未导出静态字段) 回退到 VMStructs", jvmti != 0,
                "0x" + Long.toHexString(jvmti));
        check("反查到引用该静态字段的指令",
                net.hotspot.cplus.jdk17.core.CNative.findRipReferences(jvmti, 8).length > 0);

        net.hotspot.cplus.jdk17.core.CNative.Resolution r1 =
                net.hotspot.cplus.jdk17.core.CNative.resolve(
                        "JvmtiExport::_can_access_local_variables");
        check("resolve(静态字段) 命中",
                r1.kind == net.hotspot.cplus.jdk17.core.CNative.Kind.STATIC_FIELD
                        && r1.address != 0, r1.toString());
        check("resolve 附带调用链证据", r1.evidence.length > 0);

        net.hotspot.cplus.jdk17.core.CNative.Resolution r2 =
                net.hotspot.cplus.jdk17.core.CNative.resolve("JVM_GC");
        check("resolve(导出符号) 命中",
                r2.kind == net.hotspot.cplus.jdk17.core.CNative.Kind.EXPORT
                        && r2.address != 0, r2.toString());

        net.hotspot.cplus.jdk17.core.CNative.Resolution r3 =
                net.hotspot.cplus.jdk17.core.CNative.resolve("Klass::_layout_helper");
        check("resolve(实例字段) 命中",
                r3.kind == net.hotspot.cplus.jdk17.core.CNative.Kind.INSTANCE_FIELD
                        && r3.offset == 8, r3.toString());

        long thread = net.hotspot.cplus.jdk17.core.UnsafeUtils.currentThreadAddress();
        long off = net.hotspot.cplus.jdk17.core.CNative.fieldOffset("JavaThread::_thread_state");
        check("JavaThread::_thread_state 偏移 > 0", off > 0, "off=" + off);
        check("fieldAddress = base + offset",
                net.hotspot.cplus.jdk17.core.CNative.fieldAddress(
                        thread, "JavaThread::_thread_state") == thread + off);

        long mem = net.hotspot.cplus.jdk17.core.UnsafeUtils.unsafe.allocateMemory(64);
        try {
            net.hotspot.cplus.jdk17.core.UnsafeUtils.unsafe.putLong(mem + 32, 0x7788L);
            check("findFieldByValue 命中 +32",
                    net.hotspot.cplus.jdk17.core.CNative.findFieldByValue(mem, 0x7788L, 64) == 32);
        } finally {
            net.hotspot.cplus.jdk17.core.UnsafeUtils.unsafe.freeMemory(mem);
        }
    }

    private static void scanMissingFieldChecks() {
        boolean missing = false;
        try {
            net.hotspot.cplus.jdk17.core.VMStructsHelper.findOffset("JavaThread::_jni_environment");
        } catch (Throwable t) {
            missing = true;
        }
        check("VMStructs 不含 JavaThread::_jni_environment", missing);

        long thread = net.hotspot.cplus.jdk17.core.UnsafeUtils.currentThreadAddress();
        long env = net.hotspot.cplus.jdk17.core.CNative.jniEnvironment(thread);
        check("扫描到 _jni_environment 字段", env != 0,
                "off=" + (env - thread) + ", env=0x" + Long.toHexString(env));

        net.hotspot.cplus.jdk17.core.CNative.Resolution probe =
                net.hotspot.cplus.jdk17.core.CNative.resolveJniEnvironment(thread);
        check("带特征判据的 resolve 定位 _jni_environment",
                probe.kind == net.hotspot.cplus.jdk17.core.CNative.Kind.PROBE_HIT
                        && probe.address == env, probe.toString());
        check("仅靠名字找不到（必须给判据/线索）",
                net.hotspot.cplus.jdk17.core.CNative.resolve(
                        "JavaThread::_jni_environment").kind
                        == net.hotspot.cplus.jdk17.core.CNative.Kind.NOT_FOUND);

        long table = env == 0 ? 0
                : net.hotspot.cplus.jdk17.core.UnsafeUtils.unsafe.getLong(env);
        long getVersion = table == 0 ? 0
                : net.hotspot.cplus.jdk17.core.UnsafeUtils.unsafe.getLong(table + 32);
        check("JNIEnv 表指针非 0", table != 0);
        check("GetVersion 函数指针非 0", getVersion != 0);
    }

    private static void nativeHookChecks() {
        for (int i = 0; i < 200000; i++) {
            ShellTarget.shell();
        }
        InstanceKlass ik = new InstanceKlass(ShellTarget.class);
        Method sm = find(ik, "shell", "()J");
        check("ShellTarget.shell 已编译", sm != null && sm.code != 0);
        if (sm == null || sm.code == 0) return;

        InstanceKlass rk = new InstanceKlass(Runtime.class);
        Method tm = find(rk, "totalMemory", "()J");
        long saved = tm.nativeFunction();
        check("读到 Runtime.totalMemory 的 native function 指针", saved != 0,
                "0x" + Long.toHexString(saved));

        long target = net.hotspot.cplus.jdk17.core.Shellcode.symbol("JVM_CurrentTimeMillis");
        check("解析 JVM_CurrentTimeMillis", target != 0);
        long shellcode = new net.hotspot.cplus.jdk17.codecache.NMethod(sm.code).verifiedEntryPoint;
        net.hotspot.cplus.jdk17.core.Shellcode.write(
                sm, net.hotspot.cplus.jdk17.core.Shellcode.tailCall(target));
        tm.setNativeFunction(shellcode);

        long v = callTotalMemoryOnce();
        long now = System.currentTimeMillis();
        check("经 native wrapper 调用 JVM_CurrentTimeMillis",
                Math.abs(v - now) < 10000, "ret=" + v + ", now=" + now);

        tm.setNativeFunction(saved);
        check("恢复 native function 后 totalMemory 正常",
                callTotalMemoryOnce() > 0, "ret=" + callTotalMemoryOnce());
    }

    private static long callTotalMemoryOnce() {
        return Runtime.getRuntime().totalMemory();
    }

    private static void restoreMetadataChecks() {
        net.hotspot.cplus.jdk17.klass.ClassFile cf;
        try {
            cf = net.hotspot.cplus.jdk17.klass.ClassFile.parse(readBytes(RestoreTarget.class));
        } catch (Throwable t) {
            check("解析 RestoreTarget.class", false, t.toString());
            return;
        }
        net.hotspot.cplus.jdk17.klass.ClassFile.MethodInfo mi = cf.find("compute", "(I)I");
        check("找到 compute 的 class 文件信息", mi != null);
        if (mi == null) return;
        check("class 文件有异常表", mi.exceptionTable.length > 0);
        check("class 文件有 LineNumberTable", mi.lineNumbers != null && mi.lineNumbers.length > 0);
        check("class 文件有 LocalVariableTable", !mi.localVars.isEmpty());
        check("class 文件有 StackMapTable", mi.stackMap != null && mi.stackMap.length > 0);
        check("class 文件有注解", mi.annotations != null && mi.annotations.length > 0);
        check("compute(10) 正常", RestoreTarget.compute(10) == 33);

        int n = UnsafeUtils.restoreClass(RestoreTarget.class);
        check("restoreClass 恢复 RestoreTarget", n > 0, "n=" + n);

        InstanceKlass ik = new InstanceKlass(RestoreTarget.class);
        Method m = find(ik, "compute", "(I)I");
        ConstMethod cm = new ConstMethod(m.constMethod);
        check("异常表长度还原", cm.exceptionTableLength() == mi.exceptionTable.length / 4,
                "runtime=" + cm.exceptionTableLength() + ", file=" + mi.exceptionTable.length / 4);
        check("局部变量表长度还原", cm.localvariableTableLength() == mi.localVars.size(),
                "runtime=" + cm.localvariableTableLength() + ", file=" + mi.localVars.size());
        check("行号表存在", cm.hasLinenumberTable());
        check("StackMapTable 长度还原", cm.stackMapLength() == mi.stackMap.length,
                "runtime=" + cm.stackMapLength() + ", file=" + mi.stackMap.length);
        check("注解长度还原", cm.methodAnnotationsLength() == mi.annotations.length,
                "runtime=" + cm.methodAnnotationsLength() + ", file=" + mi.annotations.length);
        check("方法访问标志还原", (m.accessFlags & 0xFFFF) == mi.accessFlags,
                "runtime=0x" + Integer.toHexString(m.accessFlags));
        check("还原后 compute(10) 仍为 33", RestoreTarget.compute(10) == 33);
    }

    private static byte[] readBytes(Class<?> c) {
        String path = "/" + c.getName().replace('.', '/') + ".class";
        try (java.io.InputStream in = c.getResourceAsStream(path)) {
            return in == null ? new byte[0] : in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void patchJdkClass() {
        Class<?> unsafeClass;
        try {
            unsafeClass = Class.forName("sun.misc.Unsafe");
        } catch (Throwable t) {
            check("加载 sun.misc.Unsafe", false, t.toString());
            return;
        }
        check("加载 sun.misc.Unsafe", true);

        boolean threw = false;
        try {
            sun.misc.Unsafe.getUnsafe();
        } catch (SecurityException e) {
            threw = true;
        }
        check("原 getUnsafe() 抛 SecurityException", threw);

        InstanceKlass ik = new InstanceKlass(unsafeClass);
        Method m = find(ik, "getUnsafe", "()Lsun/misc/Unsafe;");
        check("找到 Unsafe.getUnsafe", m != null);
        if (m == null) return;

        m.forceInterpreted();
        PatchPrint.printed = false;

        CodeBuilder cb = new CodeBuilder();
        cb.invokestatic(ik, PatchPrint.class, "print", "()V");
        cb.aconst_null();
        cb.areturn();
        byte[] code = cb.build(ik);

        new ConstMethod(m.constMethod).putCodeAndResize(code);
        ik.commitCpChanges();
        m.setNotCompilable();

        Object r = sun.misc.Unsafe.getUnsafe();
        check("patch 后 getUnsafe() 返回 null", r == null, String.valueOf(r));
        check("patch 后确实调用了 PatchPrint.print", PatchPrint.printed);

        try {
            long mem = UnsafeUtils.unsafe.allocateMemory(16);
            UnsafeUtils.unsafe.freeMemory(mem);
            check("patch 后 Unsafe 其他方法仍可用", true);
        } catch (Throwable t) {
            check("patch 后 Unsafe 其他方法仍可用", false, t.toString());
        }
        check("Unsafe.getUnsafe 已被标记为不可编译", m.isNotCompilable() && m.isDontInline());
    }

    private static String cpClassName(ConstantPool cp, int classIdx) {
        long slot = cp.slotAt(classIdx);
        int nameIdx = (int) ((slot >>> 16) & 0xFFFF);
        long sym = cp.symbolAt(nameIdx);
        return sym == 0 ? null : new Symbol(sym).value;
    }

    private static boolean cpHasClass(ConstantPool cp, String className) {
        for (int i = 1; i < cp.length; i++) {
            int tag = UnsafeUtils.unsafe.getByte(cp.tags + 4 + i) & 0xFF;
            if (tag == 7 && className.equals(cpClassName(cp, i))) {
                return true;
            }
            if (tag == 5 || tag == 6) i++;
        }
        return false;
    }

    private static Method find(InstanceKlass ik, String name, String sig) {        for (Method m : ik.getMethods()) {
            if (name.equals(m.name()) && sig.equals(m.signature())) {
                return m;
            }
        }
        return null;
    }

    private static void check(String label, boolean ok) {
        check(label, ok, "");
    }

    private static void check(String label, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            failures.add(label + (detail == null || detail.isEmpty() ? "" : " -> " + detail));
        }
        System.out.printf("  [%s] %s%s%n", ok ? "PASS" : "FAIL", label,
                detail == null || detail.isEmpty() ? "" : "   (" + detail + ")");
    }

    private Jdk17PatchMain() {
    }
}
