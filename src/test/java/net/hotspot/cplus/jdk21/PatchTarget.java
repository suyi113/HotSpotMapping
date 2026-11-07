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
}
