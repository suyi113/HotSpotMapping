package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class VMStructsHelper {

    private static Map<String, VMStructs.VMStructEntry>       FIELDS;
    private static Map<String, VMStructs.VMTypeEntry>         TYPES;
    private static Map<String, VMStructs.VMIntConstantEntry>  INTS;
    private static Map<String, VMStructs.VMLongConstantEntry> LONGS;
    private static Map<String, VMStructs.VMAddressEntry>      ADDRS;

    private VMStructsHelper() {}

    private static synchronized void ensureLoaded() {
        if (FIELDS != null) return;
        try {
            UnsafeUtils.registerJvmDll();
            Method findNative = ClassLoader.class.getDeclaredMethod(
                    "findNative", ClassLoader.class, String.class);
            UnsafeUtils.setAccessible(findNative, true);
            ClassLoader cl = VMStructsHelper.class.getClassLoader();

            long structsPtr   = readPtr(findNative, cl, "gHotSpotVMStructs");
            long typesPtr     = readPtr(findNative, cl, "gHotSpotVMTypes");
            long intsPtr      = readPtr(findNative, cl, "gHotSpotVMIntConstants");
            long longsPtr     = readPtr(findNative, cl, "gHotSpotVMLongConstants");
            long addressesPtr = readPtr(findNative, cl, "gHotSpotVMAddresses");

            // ---- gHotSpotVMStructs ----
            Map<String, VMStructs.VMStructEntry> fields = new HashMap<>();
            long cur = structsPtr;
            while (true) {
                VMStructs.VMStructEntry e = new VMStructs.VMStructEntry(cur);
                if (e.isLastEntry()) break;
                fields.put(e.typeName + "::" + e.fieldName, e);
                cur += VMStructs.VMStructEntry.SIZE;
            }

            // ---- gHotSpotVMTypes ----
            Map<String, VMStructs.VMTypeEntry> types = new HashMap<>();
            cur = typesPtr;
            while (true) {
                VMStructs.VMTypeEntry e = new VMStructs.VMTypeEntry(cur);
                if (e.isLastEntry()) break;
                types.put(e.typeName, e);
                cur += VMStructs.VMTypeEntry.SIZE;
            }

            // ---- gHotSpotVMIntConstants ----
            Map<String, VMStructs.VMIntConstantEntry> ints = new HashMap<>();
            if (intsPtr != 0) {
                cur = intsPtr;
                while (true) {
                    VMStructs.VMIntConstantEntry e = new VMStructs.VMIntConstantEntry(cur);
                    if (e.isLastEntry()) break;
                    ints.put(e.name, e);
                    cur += VMStructs.VMIntConstantEntry.SIZE;
                }
            }

            // ---- gHotSpotVMLongConstants ----
            Map<String, VMStructs.VMLongConstantEntry> longs = new HashMap<>();
            if (longsPtr != 0) {
                cur = longsPtr;
                while (true) {
                    VMStructs.VMLongConstantEntry e = new VMStructs.VMLongConstantEntry(cur);
                    if (e.isLastEntry()) break;
                    longs.put(e.name, e);
                    cur += VMStructs.VMLongConstantEntry.SIZE;
                }
            }

            // ---- gHotSpotVMAddresses ----
            Map<String, VMStructs.VMAddressEntry> addrs = new HashMap<>();
            if (addressesPtr != 0) {
                cur = addressesPtr;
                while (true) {
                    VMStructs.VMAddressEntry e = new VMStructs.VMAddressEntry(cur);
                    if (e.isLastEntry()) break;
                    addrs.put(e.name, e);
                    cur += VMStructs.VMAddressEntry.SIZE;
                }
            }

            TYPES  = types;
            FIELDS = fields;
            INTS   = ints;
            LONGS  = longs;
            ADDRS  = addrs;
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private static long readPtr(Method findNative, ClassLoader cl, String name) throws Exception {
        try {
            long p = (long) findNative.invoke(null, cl, name);
            if (p == 0) return 0L;
            return UnsafeUtils.unsafe.getLong(p);
        } catch (Throwable t) {
            return 0L;
        }
    }

    // ============================================================
    //  原有 API
    // ============================================================

    public static long findOffset(String typeAndField) {
        ensureLoaded();
        VMStructs.VMStructEntry e = FIELDS.get(typeAndField);
        if (e == null) throw new IllegalStateException("VMStructs field not found: " + typeAndField);
        if (e.isStaticField()) return -1L;
        return e.offset;
    }

    public static long findStaticAddress(String typeAndField) {
        ensureLoaded();
        VMStructs.VMStructEntry e = FIELDS.get(typeAndField);
        if (e == null) throw new IllegalStateException("VMStructs field not found: " + typeAndField);
        if (!e.isStaticField()) return -1L;
        return e.fieldAddress;
    }

    public static long findOffsetOrDefault(String name, long fallback) {
        try { return findOffset(name); } catch (Exception e) { return fallback; }
    }

    public static long findSize(String typeName) {
        ensureLoaded();
        VMStructs.VMTypeEntry e = TYPES.get(typeName);
        return e == null ? -1L : e.size;
    }

    public static boolean hasField(String typeAndField) {
        ensureLoaded();
        return FIELDS.containsKey(typeAndField);
    }

    public static VMStructs.VMTypeEntry findType(String typeName) {
        ensureLoaded();
        return TYPES.get(typeName);
    }

    // ============================================================
    //  新增 API
    // ============================================================

    public static Integer findIntConstant(String name) {
        ensureLoaded();
        VMStructs.VMIntConstantEntry e = INTS.get(name);
        return e == null ? null : e.value;
    }

    public static Long findLongConstant(String name) {
        ensureLoaded();
        VMStructs.VMLongConstantEntry e = LONGS.get(name);
        return e == null ? null : e.value;
    }

    public static Long findAddress(String name) {
        ensureLoaded();
        VMStructs.VMAddressEntry e = ADDRS.get(name);
        return e == null ? null : e.value;
    }

    public static int fieldCount()      { ensureLoaded(); return FIELDS.size(); }
    public static int typeCount()       { ensureLoaded(); return TYPES.size(); }
    public static int intConstantCount(){ ensureLoaded(); return INTS.size(); }
    public static int longConstantCount(){ ensureLoaded(); return LONGS.size(); }
    public static int addressCount()    { ensureLoaded(); return ADDRS.size(); }

    public static Collection<VMStructs.VMStructEntry>       allFields()        { ensureLoaded(); return FIELDS.values(); }
    public static Collection<VMStructs.VMTypeEntry>         allTypes()         { ensureLoaded(); return TYPES.values(); }
    public static Collection<VMStructs.VMIntConstantEntry>  allIntConstants()  { ensureLoaded(); return INTS.values(); }
    public static Collection<VMStructs.VMLongConstantEntry> allLongConstants() { ensureLoaded(); return LONGS.values(); }
    public static Collection<VMStructs.VMAddressEntry>      allAddresses()     { ensureLoaded(); return ADDRS.values(); }

    // ============================================================
    //  dump
    // ============================================================

    /** 打印全部内容。量很大（几万行），建议重定向到文件。 */
    public static void dumpAll() {
        ensureLoaded();
        System.out.printf("[VMS] ===== VMStructs 全符号 dump =====%n");
        System.out.printf("[VMS] fields=%d types=%d ints=%d longs=%d addrs=%d%n",
                FIELDS.size(), TYPES.size(), INTS.size(), LONGS.size(), ADDRS.size());

        dumpAllTypes();
        dumpAllFields();
        dumpAllIntConstants();
        dumpAllLongConstants();
        dumpAllAddresses();
        System.out.println("[VMS] ===== dump 结束 =====");
    }

    public static void dumpAllTypes() {
        ensureLoaded();
        System.out.printf("%n[VMS] ----- types (%d) -----%n", TYPES.size());
        TYPES.values().stream()
                .sorted(Comparator.comparing(t -> t.typeName))
                .forEach(t -> System.out.printf("[type ] %-60s size=%-6d super=%-40s oop=%d int=%d%n",
                        t.typeName, t.size,
                        t.superclassName == null ? "null" : t.superclassName,
                        t.isOopType, t.isIntegerType));
    }

    public static void dumpAllFields() {
        ensureLoaded();
        System.out.printf("%n[VMS] ----- fields (%d) -----%n", FIELDS.size());
        FIELDS.values().stream()
                .sorted(Comparator
                        .comparing((VMStructs.VMStructEntry e) -> e.typeName)
                        .thenComparing(e -> e.fieldName))
                .forEach(e -> {
                    if (e.isStaticField()) {
                        System.out.printf("[field] %s::%s  type=%-30s  static  addr=0x%x%n",
                                e.typeName, e.fieldName, e.typeString, e.fieldAddress);
                    } else {
                        System.out.printf("[field] %s::%s  type=%-30s  offset=%d%n",
                                e.typeName, e.fieldName, e.typeString, e.offset);
                    }
                });
    }

    public static void dumpAllIntConstants() {
        ensureLoaded();
        System.out.printf("%n[VMS] ----- int constants (%d) -----%n", INTS.size());
        INTS.values().stream()
                .sorted(Comparator.comparing(e -> e.name))
                .forEach(e -> System.out.printf("[int  ] %-60s = %d (0x%x)%n",
                        e.name, e.value, e.value));
    }

    public static void dumpAllLongConstants() {
        ensureLoaded();
        System.out.printf("%n[VMS] ----- long constants (%d) -----%n", LONGS.size());
        LONGS.values().stream()
                .sorted(Comparator.comparing(e -> e.name))
                .forEach(e -> System.out.printf("[long ] %-60s = %d (0x%x)%n",
                        e.name, e.value, e.value));
    }

    public static void dumpAllAddresses() {
        ensureLoaded();
        System.out.printf("%n[VMS] ----- addresses (%d) -----%n", ADDRS.size());
        ADDRS.values().stream()
                .sorted(Comparator.comparing(e -> e.name))
                .forEach(e -> System.out.printf("[addr ] %-60s = 0x%x%n",
                        e.name, e.value));
    }

    // ============================================================
    //  搜索
    // ============================================================

    /**
     * 按子串搜字段/类型/常量名，大小写不敏感。
     * 例：search("ClassLoaderData")
     *     search("_dictionary")
     *     search("DictionaryEntry")
     */
    public static void search(String keyword) {
        ensureLoaded();
        String kw = keyword.toLowerCase(Locale.ROOT);
        System.out.printf("%n[VMS] ===== search \"%s\" =====%n", keyword);

        long nTypes = TYPES.values().stream()
                .filter(t -> t.typeName != null && t.typeName.toLowerCase(Locale.ROOT).contains(kw))
                .peek(t -> System.out.printf("[type ] %-60s size=%-6d super=%s%n",
                        t.typeName, t.size,
                        t.superclassName == null ? "null" : t.superclassName))
                .count();

        long nFields = FIELDS.values().stream()
                .filter(e -> (e.typeName  != null && e.typeName.toLowerCase(Locale.ROOT).contains(kw))
                        || (e.fieldName != null && e.fieldName.toLowerCase(Locale.ROOT).contains(kw)))
                .peek(e -> {
                    if (e.isStaticField()) {
                        System.out.printf("[field] %s::%s  type=%-30s  static  addr=0x%x%n",
                                e.typeName, e.fieldName, e.typeString, e.fieldAddress);
                    } else {
                        System.out.printf("[field] %s::%s  type=%-30s  offset=%d%n",
                                e.typeName, e.fieldName, e.typeString, e.offset);
                    }
                })
                .count();

        long nInts = INTS.values().stream()
                .filter(e -> e.name != null && e.name.toLowerCase(Locale.ROOT).contains(kw))
                .peek(e -> System.out.printf("[int  ] %-60s = %d (0x%x)%n",
                        e.name, e.value, e.value))
                .count();

        long nLongs = LONGS.values().stream()
                .filter(e -> e.name != null && e.name.toLowerCase(Locale.ROOT).contains(kw))
                .peek(e -> System.out.printf("[long ] %-60s = %d (0x%x)%n",
                        e.name, e.value, e.value))
                .count();

        long nAddrs = ADDRS.values().stream()
                .filter(e -> e.name != null && e.name.toLowerCase(Locale.ROOT).contains(kw))
                .peek(e -> System.out.printf("[addr ] %-60s = 0x%x%n",
                        e.name, e.value))
                .count();

        System.out.printf("[VMS] 命中: types=%d fields=%d ints=%d longs=%d addrs=%d%n",
                nTypes, nFields, nInts, nLongs, nAddrs);
    }

    /**
     * 列出某个类型的所有字段。等价于 search 但更精确。
     * 例：dumpType("ClassLoaderData")
     *     dumpType("DictionaryEntry")
     */
    public static void dumpType(String typeName) {
        ensureLoaded();
        System.out.printf("%n[VMS] ===== type %s =====%n", typeName);

        VMStructs.VMTypeEntry t = TYPES.get(typeName);
        if (t != null) {
            System.out.printf("[type ] %s size=%d super=%s oop=%d int=%d%n",
                    t.typeName, t.size,
                    t.superclassName == null ? "null" : t.superclassName,
                    t.isOopType, t.isIntegerType);
        } else {
            System.out.printf("[type ] %s 不在 gHotSpotVMTypes 里%n", typeName);
        }

        List<VMStructs.VMStructEntry> hits = FIELDS.values().stream()
                .filter(e -> typeName.equals(e.typeName))
                .sorted(Comparator.comparing(e -> e.fieldName))
                .collect(Collectors.toList());

        if (hits.isEmpty()) {
            System.out.printf("[VMS] 没有字段属于 %s%n", typeName);
            return;
        }

        System.out.printf("[VMS] fields of %s (%d):%n", typeName, hits.size());
        for (VMStructs.VMStructEntry e : hits) {
            if (e.isStaticField()) {
                System.out.printf("  %-40s %-30s static  addr=0x%x%n",
                        e.fieldName, e.typeString, e.fieldAddress);
            } else {
                System.out.printf("  %-40s %-30s offset=%d%n",
                        e.fieldName, e.typeString, e.offset);
            }
        }
    }

    /** 把整个 dump 写成一个 Java 常量类，方便以后硬编码。 */
    public static String generateConstantsClass() {
        ensureLoaded();
        StringBuilder sb = new StringBuilder();
        sb.append("// Auto-generated by VMStructsHelper.generateConstantsClass()\n");
        sb.append("package net.hotspot.cplus.generated;\n\n");
        sb.append("public final class VMStructsConstants {\n");

        sb.append("  // ---- offsets ----\n");
        FIELDS.values().stream()
                .filter(e -> !e.isStaticField())
                .sorted(Comparator
                        .comparing((VMStructs.VMStructEntry e) -> e.typeName)
                        .thenComparing(e -> e.fieldName))
                .forEach(e -> {
                    String ident = sanitize(e.typeName + "__" + e.fieldName);
                    sb.append(String.format("  public static final long %s = %dL;%n",
                            ident, e.offset));
                });

        sb.append("\n  // ---- static addresses (run-time only, do not hardcode!) ----\n");
        FIELDS.values().stream()
                .filter(VMStructs.VMStructEntry::isStaticField)
                .sorted(Comparator
                        .comparing((VMStructs.VMStructEntry e) -> e.typeName)
                        .thenComparing(e -> e.fieldName))
                .forEach(e -> {
                    String ident = sanitize(e.typeName + "__" + e.fieldName);
                    sb.append(String.format(
                            "  // %s::%s -> 0x%x (本次运行的值，不可硬编码)%n",
                            e.typeName, e.fieldName, e.fieldAddress));
                });

        sb.append("}\n");
        return sb.toString();
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }
}