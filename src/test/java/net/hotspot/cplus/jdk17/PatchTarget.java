package net.hotspot.cplus.jdk17;

public class PatchTarget {

    public static int f(int x) {
        return x + 1;
    }

    public static int g(int x) {
        return x * 2;
    }

    public static int hot(int x) {
        int s = 0;
        for (int i = 0; i < 8; i++) {
            s += (x * i) ^ (x >>> 3);
        }
        return s + 1;
    }
}
