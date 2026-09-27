package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class HeapScanner {

    private HeapScanner() {}

    private static final long OOP_KLASS_OFF =
            VMStructsHelper.findOffsetOrDefault("oopDesc::_metadata._compressed_klass", 8L);
    private static final long LAYOUT_HELPER_OFF =
            VMStructsHelper.findOffsetOrDefault("Klass::_layout_helper", 8L);

    private static final long KLASS_SUPER_OFF =
            VMStructsHelper.findOffsetOrDefault("Klass::_super", 128L);
    private static final long KLASS_JAVA_MIRROR_OFF =
            VMStructsHelper.findOffsetOrDefault("Klass::_java_mirror", 112L);

    private static final int ARRAY_LENGTH_OFF =
            OopDesc.USE_COMPRESSED_CLASS_POINTERS ? 12 : 16;

    private static final long NARROW_KLASS_BASE;
    private static final int  NARROW_KLASS_SHIFT;

    static {
        NARROW_KLASS_BASE = UnsafeUtils.unsafe.getLong(
                VMStructsHelper.findStaticAddress(
                        "CompressedKlassPointers::_narrow_klass._base"));
        NARROW_KLASS_SHIFT = UnsafeUtils.unsafe.getInt(
                VMStructsHelper.findStaticAddress(
                        "CompressedKlassPointers::_narrow_klass._shift"));
    }

    public static List<Object> getAllInstances(Class<?> clazz) {
        return getAllInstances(clazz, false, false);
    }

    public static List<Object> getAllInstancesIncludingSubclasses(Class<?> clazz) {
        return getAllInstances(clazz, true, false);
    }

    public static List<Object> getAllInstancesIncludingSuperclasses(Class<?> clazz) {
        return getAllInstances(clazz, false, true);
    }

    public static List<Object> getAllInstances(Class<?> clazz,
                                               boolean includeSubclasses,
                                               boolean includeSuperclasses) {
        try {
            System.gc();
            Thread.sleep(300);
            System.runFinalization();
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long targetKlass = new Klass(clazz).getAddress();

        Set<Long> targetKlasses = new HashSet<>();
        targetKlasses.add(targetKlass);

        if (includeSubclasses) {
            targetKlasses.addAll(collectSubclasses(targetKlass));
        }
        if (includeSuperclasses) {
            targetKlasses.addAll(collectSuperclasses(targetKlass));
        }

        return getAllInstancesOfKlasses(targetKlasses);
    }

    public static List<Object> getAllInstancesOfKlass(long targetKlass) {
        return getAllInstancesOfKlasses(Collections.singleton(targetKlass));
    }

    public static List<Object> getAllInstancesOfKlasses(Set<Long> targetKlasses) {

        Set<Long> validKlasses = collectAllKlassAddrs();

        Object[] holder = new Object[1];
        long holderElemAddr = OopDesc.addressOf(holder)
                + UnsafeUtils.unsafe.arrayBaseOffset(Object[].class);
        boolean compressedOop = OopDesc.USE_COMPRESSED_OOPS;

        List<Object> result = new ArrayList<>();
        G1CollectedHeap g1 = G1CollectedHeap.current();

        for (HeapRegion r : g1.hrm().allRegions()) {
            HeapRegionType t = r.type();
            if (t.isFree() || t.isContinuesHumongous()) continue;

            long cur   = r.bottom;
            long limit = r.end;

            while (cur < limit) {
                if ((cur & 7L) != 0) { cur += 8; continue; }
                if (cur + OOP_KLASS_OFF + 4 > limit) break;

                int  nk    = UnsafeUtils.unsafe.getInt(cur + OOP_KLASS_OFF);
                long klass = NARROW_KLASS_BASE
                        + ((long)(nk & 0xFFFFFFFFL) << NARROW_KLASS_SHIFT);

                if (!validKlasses.contains(klass)) {
                    cur += 8;
                    continue;
                }

                int lh = UnsafeUtils.unsafe.getInt(klass + LAYOUT_HELPER_OFF);

                long sizeWords;
                if (lh > 0) {
                    sizeWords = (lh & 0xFFFFFFFFL) >>> 3;
                    if (sizeWords < 1 || sizeWords > 0x100000) { cur += 8; continue; }
                } else if (lh < -2) {
                    int log2Elem    =  lh        & 0x3F;
                    int headerBytes = (lh >>> 16) & 0x3FFF;
                    if (log2Elem > 4) { cur += 8; continue; }
                    if (cur + ARRAY_LENGTH_OFF + 4 > limit) break;
                    int length = UnsafeUtils.unsafe.getInt(cur + ARRAY_LENGTH_OFF);
                    if (length < 0) { cur += 8; continue; }
                    long sizeBytes = (long) headerBytes + ((long) length << log2Elem);
                    sizeWords = (sizeBytes + 7) >>> 3;
                    if (sizeWords < 1 || sizeWords > 0x100000) { cur += 8; continue; }
                } else {
                    cur += 8;
                    continue;
                }

                if (targetKlasses.contains(klass)) {
                    if (compressedOop) {
                        int narrow = (int) ((cur - OopDesc.HEAP_BASE) >>> 3);
                        UnsafeUtils.unsafe.putInt(holderElemAddr, narrow);
                    } else {
                        UnsafeUtils.unsafe.putLong(holderElemAddr, cur);
                    }
                    Object o = holder[0];
                    holder[0] = null;
                    if (o != null) result.add(o);
                }

                cur += (sizeWords << 3);
            }
        }

        return result;
    }

    public static Set<Long> collectSuperclasses(long targetKlass) {
        Set<Long> result = new LinkedHashSet<>();
        long k = targetKlass;
        int guard = 0;
        while (k != 0 && guard++ < 256) {
            if (!result.add(k)) break;
            k = UnsafeUtils.unsafe.getLong(k + KLASS_SUPER_OFF);
        }
        return result;
    }

    public static Set<Long> collectSubclasses(long targetKlass) {
        Set<Long> all = collectAllKlassAddrs();
        Set<Long> result = new LinkedHashSet<>();
        result.add(targetKlass);

        for (long k : all) {
            long cur = k;
            int guard = 0;
            while (cur != 0 && guard++ < 256) {
                if (cur == targetKlass) {
                    result.add(k);
                    break;
                }
                cur = UnsafeUtils.unsafe.getLong(cur + KLASS_SUPER_OFF);
            }
        }
        return result;
    }

    public static List<Class<?>> getSubclasses(Class<?> clazz) {
        long target = new Klass(clazz).getAddress();
        Set<Long> subs = collectSubclasses(target);
        List<Class<?>> list = new ArrayList<>();
        for (long k : subs) {
            Class<?> c = klassToClass(k);
            if (c != null) list.add(c);
        }
        return list;
    }

    public static List<Class<?>> getSuperclasses(Class<?> clazz) {
        long target = new Klass(clazz).getAddress();
        Set<Long> sups = collectSuperclasses(target);
        List<Class<?>> list = new ArrayList<>();
        for (long k : sups) {
            Class<?> c = klassToClass(k);
            if (c != null) list.add(c);
        }
        return list;
    }

    public static Class<?> klassToClass(long klassAddr) {
        long mirror = UnsafeUtils.unsafe.getLong(klassAddr + KLASS_JAVA_MIRROR_OFF);
        if (mirror == 0) return null;

        Object[] holder = new Object[1];
        long holderElemAddr = OopDesc.addressOf(holder)
                + UnsafeUtils.unsafe.arrayBaseOffset(Object[].class);

        if (OopDesc.USE_COMPRESSED_OOPS) {
            int narrow = (int) ((mirror - OopDesc.HEAP_BASE) >>> 3);
            UnsafeUtils.unsafe.putInt(holderElemAddr, narrow);
        } else {
            UnsafeUtils.unsafe.putLong(holderElemAddr, mirror);
        }
        Object o = holder[0];
        holder[0] = null;
        return (Class<?>) o;
    }

    public static Set<Long> collectAllKlassAddrs() {
        Set<Long> set = new HashSet<>();

        long cldHeadAddr = VMStructsHelper.findStaticAddress("ClassLoaderDataGraph::_head");
        long cldPtr = UnsafeUtils.unsafe.getLong(cldHeadAddr);

        long cldKlassesOff = VMStructsHelper.findOffset("ClassLoaderData::_klasses");
        long cldNextOff    = VMStructsHelper.findOffset("ClassLoaderData::_next");
        long kNextLinkOff  = VMStructsHelper.findOffset("Klass::_next_link");

        Set<Long> seenCld = new HashSet<>();
        while (cldPtr != 0) {
            if (!seenCld.add(cldPtr)) break;
            long klassPtr = UnsafeUtils.unsafe.getLong(cldPtr + cldKlassesOff);
            while (klassPtr != 0) {
                if (!set.add(klassPtr)) break;
                klassPtr = UnsafeUtils.unsafe.getLong(klassPtr + kNextLinkOff);
            }
            cldPtr = UnsafeUtils.unsafe.getLong(cldPtr + cldNextOff);
        }
        return set;
    }
}