package net.hotspot.cplus.jdk21;

public class PatchPrint {

    public static boolean printed;

    public static void print() {
        printed = true;
        System.out.println("[PATCHED] patched method was called");
    }
}
