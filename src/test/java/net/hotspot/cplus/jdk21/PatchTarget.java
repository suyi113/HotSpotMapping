package net.hotspot.cplus.jdk21;

public class PatchTarget {

    public static int target(int x) {
        return x + 1;
    }

    public static int helper(int x) {
        return x * 2;
    }

    public static int untouched(int x) {
        return x + 100;
    }

    public static int compiled(int x) {
        int s = 0;
        for (int i = 0; i < 8; i++) {
            s += (x * i) ^ (x >>> 3);
        }
        return s + 1;
    }

    public static int spare(int x) {
        return x + 5;
    }
}
