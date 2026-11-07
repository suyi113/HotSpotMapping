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
        int cpIdx = (code[2] & 0xFF) | ((code[3] & 0xFF) << 8);
        int classIdx = (int) (afterCp.slotAt(cpIdx) & 0xFFFF);
        check("invokestatic 指向 ExternalHelper",
                "net/hotspot/cplus/jdk17/ExternalHelper".equals(cpClassName(afterCp, classIdx)),
                cpClassName(afterCp, classIdx));

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
