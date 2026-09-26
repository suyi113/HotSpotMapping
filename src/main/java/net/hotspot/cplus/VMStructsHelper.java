package net.hotspot.cplus;



import net.hotspot.cplus.Utils.UnsafeUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class VMStructsHelper {

    private static Map<String, VMStructs.VMStructEntry> FIELDS;
    private static Map<String, VMStructs.VMTypeEntry>   TYPES;

    private VMStructsHelper() {}

    private static synchronized void ensureLoaded() {
        if (FIELDS != null) return;
        try {
            UnsafeUtils.registerJvmDll();
            Method findNative = ClassLoader.class.getDeclaredMethod(
                    "findNative", ClassLoader.class, String.class);
            UnsafeUtils.setAccessible(findNative,true);
            ClassLoader cl = VMStructsHelper.class.getClassLoader();

            long structsPtr = UnsafeUtils.unsafe.getLong(
                    (long) findNative.invoke(null, cl, "gHotSpotVMStructs"));
            long typesPtr = UnsafeUtils.unsafe.getLong(
                    (long) findNative.invoke(null, cl, "gHotSpotVMTypes"));

            Map<String, VMStructs.VMStructEntry> fields = new HashMap<>();
            long cur = structsPtr;
            while (true) {
                VMStructs.VMStructEntry e = new VMStructs.VMStructEntry(cur);
                if (e.isLastEntry()) break;
                fields.put(e.typeName + "::" + e.fieldName, e);
                cur += VMStructs.VMStructEntry.SIZE;
            }

            Map<String, VMStructs.VMTypeEntry> types = new HashMap<>();
            cur = typesPtr;
            while (true) {
                VMStructs.VMTypeEntry e = new VMStructs.VMTypeEntry(cur);
                if (e.isLastEntry()) break;
                types.put(e.typeName, e);
                cur += VMStructs.VMTypeEntry.SIZE;
            }

            TYPES  = types;
            FIELDS = fields;
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }


    public static long findOffset(String typeAndField) {
        ensureLoaded();
        VMStructs.VMStructEntry e = FIELDS.get(typeAndField);
        if (e == null) {
            throw new IllegalStateException("VMStructs field not found: " + typeAndField);
        }
        if (e.isStaticField()) return -1L;
        return e.offset;
    }

    public static long findStaticAddress(String typeAndField) {
        ensureLoaded();
        VMStructs.VMStructEntry e = FIELDS.get(typeAndField);
        if (e == null) {
            throw new IllegalStateException("VMStructs field not found: " + typeAndField);
        }
        if (!e.isStaticField()) return -1L;
        return e.fieldAddress;
    }

    public static long findOffsetOrDefault(String name, long fallback) {
        try {
            return VMStructsHelper.findOffset(name);
        } catch (Exception e) {
            return fallback;
        }
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
}