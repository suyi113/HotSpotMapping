package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


public class SystemDictionary {


    private static final long RESOLUTION_ERRORS_ADDR    = findStatic("SystemDictionary::_resolution_errors");
    private static final long INVOKE_METHOD_TABLE_ADDR  = findStatic("SystemDictionary::_invoke_method_table");
    private static final long PD_CACHE_TABLE_ADDR       = findStatic("SystemDictionary::_pd_cache_table");
    private static final long JAVA_SYSTEM_LOADER_ADDR   = findStatic("SystemDictionary::_java_system_loader");
    private static final long JAVA_PLATFORM_LOADER_ADDR = findStatic("SystemDictionary::_java_platform_loader");

    private static final long WK_KLASSES_OLD_ADDR = findStatic("SystemDictionary::_well_known_klasses");
    private static final long WK_KLASSES_NEW_ADDR = findStatic("vmClasses::_klasses");

    public static final long WELL_KNOWN_KLASSES_ADDR =
            WK_KLASSES_NEW_ADDR != 0 ? WK_KLASSES_NEW_ADDR : WK_KLASSES_OLD_ADDR;

    private static final long BOX_KLASSES_ADDR = findStatic("SystemDictionary::_box_klasses");

    private static final int WK_KLASSES_MAX = 160;

    private static final int T_BOOLEAN = 4;
    private static final int T_CHAR    = 5;
    private static final int T_FLOAT   = 6;
    private static final int T_DOUBLE  = 7;
    private static final int T_BYTE    = 8;
    private static final int T_SHORT   = 9;
    private static final int T_INT     = 10;
    private static final int T_LONG    = 11;
    private static final int T_VOID    = 14;

    private SystemDictionary() {}


    public static long resolutionErrors() { return UnsafeUtils.unsafe.getLong(RESOLUTION_ERRORS_ADDR); }
    public static long invokeMethodTable() { return UnsafeUtils.unsafe.getLong(INVOKE_METHOD_TABLE_ADDR); }
    public static long pdCacheTable()     { return UnsafeUtils.unsafe.getLong(PD_CACHE_TABLE_ADDR); }

    public static long javaSystemLoaderOop() {
        long h = UnsafeUtils.unsafe.getLong(JAVA_SYSTEM_LOADER_ADDR);
        return h == 0 ? 0 : UnsafeUtils.unsafe.getLong(h);
    }

    public static long javaPlatformLoaderOop() {
        long h = UnsafeUtils.unsafe.getLong(JAVA_PLATFORM_LOADER_ADDR);
        return h == 0 ? 0 : UnsafeUtils.unsafe.getLong(h);
    }

    public static Object javaSystemLoader() {
        long o = javaSystemLoaderOop();
        return o == 0 ? null : UnsafeUtils.oopToJavaObject(o);
    }

    public static Object javaPlatformLoader() {
        long o = javaPlatformLoaderOop();
        return o == 0 ? null : UnsafeUtils.oopToJavaObject(o);
    }

    public static InstanceKlass wellKnownKlassAt(int index) {
        if (WELL_KNOWN_KLASSES_ADDR == 0) {
            throw new IllegalStateException("well_known_klasses Address not available");
        }
        if (index < 0 || index >= WK_KLASSES_MAX) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        long k = UnsafeUtils.unsafe.getLong(WELL_KNOWN_KLASSES_ADDR + (long) index * 8L);
        return k == 0 ? null : new InstanceKlass(k);
    }

    public static InstanceKlass wellKnownKlass(String extName) {
        if (WELL_KNOWN_KLASSES_ADDR == 0) return null;
        for (int i = 0; i < WK_KLASSES_MAX; i++) {
            long k = UnsafeUtils.unsafe.getLong(WELL_KNOWN_KLASSES_ADDR + (long) i * 8L);
            if (k == 0) continue;
            try {
                String n = new InstanceKlass(k).getExternalName();
                if (extName.equals(n)) return new InstanceKlass(k);
            } catch (Throwable ignored) { }
        }
        return null;
    }

    public static int clearWellKnownKlass(String extName) {
        if (WELL_KNOWN_KLASSES_ADDR == 0) return 0;
        int cleared = 0;
        for (int i = 0; i < WK_KLASSES_MAX; i++) {
            long p = WELL_KNOWN_KLASSES_ADDR + (long) i * 8L;
            long k = UnsafeUtils.unsafe.getLong(p);
            if (k == 0) continue;
            try {
                String n = new InstanceKlass(k).getExternalName();
                if (extName.equals(n)) {
                    UnsafeUtils.unsafe.putLong(p, 0L);
                    System.out.printf("[SystemDictionary] cleared WK[%d] %s (was 0x%x)%n",
                            i, extName, k);
                    cleared++;
                }
            } catch (Throwable ignored) { }
        }
        return cleared;
    }

    public static InstanceKlass boxKlass(int type) {
        if (type < T_BOOLEAN || type > T_VOID) {
            throw new IllegalArgumentException("type: " + type);
        }
        int idx = type - T_BOOLEAN;
        long k = UnsafeUtils.unsafe.getLong(BOX_KLASSES_ADDR + (long) idx * 8L);
        return k == 0 ? null : new InstanceKlass(k);
    }

    public static InstanceKlass boxKlassFor(Class<?> primitive) {
        int t;
        if      (primitive == boolean.class) t = T_BOOLEAN;
        else if (primitive == char.class)    t = T_CHAR;
        else if (primitive == float.class)   t = T_FLOAT;
        else if (primitive == double.class)  t = T_DOUBLE;
        else if (primitive == byte.class)    t = T_BYTE;
        else if (primitive == short.class)   t = T_SHORT;
        else if (primitive == int.class)     t = T_INT;
        else if (primitive == long.class)    t = T_LONG;
        else throw new IllegalArgumentException("not a primitive: " + primitive);
        return boxKlass(t);
    }


    public static void dumpWellKnownKlasses() {
        System.out.printf("[SystemDictionary] well-known klasses @ 0x%x%n",
                WELL_KNOWN_KLASSES_ADDR);
        int emptyRun = 0;
        for (int i = 0; i < WK_KLASSES_MAX; i++) {
            long k = UnsafeUtils.unsafe.getLong(WELL_KNOWN_KLASSES_ADDR + (long) i * 8L);
            if (k == 0) {
                emptyRun++;
                if (emptyRun > 5) {
                    System.out.printf("[SystemDictionary]   ...(Consecutive empty starting from [%d], stopping)%n",
                            i - emptyRun + 1);
                    break;
                }
                System.out.printf("[SystemDictionary]   [%d] (null)%n", i);
                continue;
            }
            emptyRun = 0;
            String n;
            try { n = new InstanceKlass(k).getExternalName(); }
            catch (Throwable t) { n = "<bad>"; }
            System.out.printf("[SystemDictionary]   [%d] 0x%x %s%n", i, k, n);
        }
    }

    public static void dumpOffsets() {
        System.out.printf(
                "[SystemDictionary] static addresses:%n" +
                        "  _resolution_errors     @ 0x%x%n" +
                        "  _invoke_method_table   @ 0x%x%n" +
                        "  _pd_cache_table        @ 0x%x%n" +
                        "  _well_known_klasses    @ 0x%x  (old)%n" +
                        "  vmClasses::_klasses    @ 0x%x  (new)%n" +
                        "  _box_klasses           @ 0x%x%n" +
                        "  _java_system_loader    @ 0x%x%n" +
                        "  _java_platform_loader  @ 0x%x%n",
                RESOLUTION_ERRORS_ADDR, INVOKE_METHOD_TABLE_ADDR, PD_CACHE_TABLE_ADDR,
                WK_KLASSES_OLD_ADDR, WK_KLASSES_NEW_ADDR,
                BOX_KLASSES_ADDR,
                JAVA_SYSTEM_LOADER_ADDR, JAVA_PLATFORM_LOADER_ADDR);
    }

    private static long findStatic(String name) {
        try {
            long v = VMStructsHelper.findStaticAddress(name);
            if (v > 0) return v;
        } catch (Throwable ignored) { }
        return 0L;
    }

    @Override
    public String toString() {
        return String.format(
                "SystemDictionary[resErr=0x%x, wkKlasses=0x%x, boxKlasses=0x%x, " +
                        "sysLoader=0x%x, platLoader=0x%x]",
                resolutionErrors(), WELL_KNOWN_KLASSES_ADDR,
                BOX_KLASSES_ADDR, javaSystemLoaderOop(), javaPlatformLoaderOop());
    }
}