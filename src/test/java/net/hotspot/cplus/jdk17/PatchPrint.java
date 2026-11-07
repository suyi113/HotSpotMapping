package net.hotspot.cplus.jdk17;

public class PatchPrint {

    public static boolean printed;

    public static void print() {
        printed = true;
        System.out.println("[PATCHED] patched method was called");
    }
}
