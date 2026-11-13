package net.hotspot.cplus.jdk17;

import net.hotspot.cplus.jdk17.core.UnsafeUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class Jdk17HookMain {

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Field value = String.class.getDeclaredField("value");
        check("初始不可访问", !canGet(value));

        for (int i = 0; i < 50000_000; i++) {
            canGet(value);
        }
        check("预热后仍不可访问", !canGet(value));

        UnsafeUtils.hookCheckAccess();
        check("hook 后可以访问", canGet(value));

        boolean always = true;
        for (int i = 0; i < 200_000; i++) {
            if (!canGet(value)) {
                always = false;
                break;
            }
        }
        check("20 万次调用后 hook 仍有效", always);
        check("hook 后仍可访问", canGet(value));

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

    private static boolean canGet(Field f) {
        try {
            f.get("abc");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            failures.add(label);
        }
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + label);
    }

    private Jdk17HookMain() {
    }
}
