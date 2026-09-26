package net.hotspot.cplus;




import java.lang.management.ManagementFactory;
import com.sun.management.HotSpotDiagnosticMXBean;
import net.hotspot.cplus.Utils.UnsafeUtils;


public class OopDesc {

    public static final boolean USE_COMPRESSED_OOPS;
    public static final boolean USE_COMPRESSED_CLASS_POINTERS;
    public static final long    HEAP_BASE;

    private static final int  OOP_SHIFT    = 3;
    private static final int  KLASS_SHIFT  = 3;
    private static final long MARK_OFFSET  = 0L;
    private static final long KLASS_OFFSET = 8L;

    private static volatile long cachedKlassBase = 0L;

    static {
        boolean coops = false;
        boolean ccp   = false;
        try {
            HotSpotDiagnosticMXBean bean =
                    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            coops = Boolean.parseBoolean(bean.getVMOption("UseCompressedOops").getValue());
            ccp   = Boolean.parseBoolean(bean.getVMOption("UseCompressedClassPointers").getValue());
        } catch (Throwable ignored) { }
        USE_COMPRESSED_OOPS = coops;
        USE_COMPRESSED_CLASS_POINTERS = ccp;
        HEAP_BASE = Long.getLong("hotspot.heapBase", 0L);

    }

    private final long address;

    public long mark;
    public long klass;
    public int  narrowKlass;
    public long klassBase;

    public static OopDesc of(Object instance) {
        if (instance == null) return null;
        long addr = addressOf(instance);
        return new OopDesc(addr, instance);
    }

    public static OopDesc of(long oopAddress, Object anchor) {
        return new OopDesc(oopAddress, anchor);
    }

    public static OopDesc of(long oopAddress) {
        return new OopDesc(oopAddress, null);
    }

    public OopDesc(long address, Object anchor) {
        this.address = address;
        if (address == 0) return;
        readFields(anchor);
    }

    private void readFields(Object anchor) {
        mark = UnsafeUtils.unsafe.getLong(address + MARK_OFFSET);

        if (USE_COMPRESSED_CLASS_POINTERS) {
            narrowKlass = UnsafeUtils.unsafe.getInt(address + KLASS_OFFSET);

            if (anchor != null) {
                klassBase = computeKlassBaseFor(anchor, narrowKlass);
            } else {
                klassBase = cachedKlassBase;
                if (klassBase == 0L) {
                    Object probe = new Object();
                    int probeNarrow = UnsafeUtils.unsafe.getInt(probe, KLASS_OFFSET);
                    long probeBase = computeKlassBaseFor(probe, probeNarrow);
                    cachedKlassBase = probeBase;
                    klassBase = probeBase;
                }
            }

            long unsigned = narrowKlass & 0xFFFFFFFFL;
            klass = klassBase + (unsigned << KLASS_SHIFT);
        } else {
            klass = UnsafeUtils.unsafe.getLong(address + KLASS_OFFSET);
            klassBase = 0L;
        }
    }

    private static long computeKlassBaseFor(Object anchor, int narrowKlass) {
        InstanceKlass realIk = new InstanceKlass(anchor.getClass());
        long realAddr = realIk.getAddress();
        long offset = ((narrowKlass & 0xFFFFFFFFL) << KLASS_SHIFT);
        return realAddr - offset;
    }

    public static long addressOf(Object o) {
        if (o == null) return 0;
        Object[] holder = new Object[] { o };
        long base = UnsafeUtils.unsafe.arrayBaseOffset(Object[].class);
        if (USE_COMPRESSED_OOPS) {
            int narrow = UnsafeUtils.unsafe.getInt(holder, base);
            long unsigned = narrow & 0xFFFFFFFFL;
            return HEAP_BASE + (unsigned << OOP_SHIFT);
        } else {
            return UnsafeUtils.unsafe.getLong(holder, base);
        }
    }

    public long getAddress() { return address; }
    public long fieldBase()  { return address + (USE_COMPRESSED_CLASS_POINTERS ? 12L : 16L); }

    public boolean isLocked()       { return (mark & 0x3L) != 0; }
    public boolean isUnlocked()     { return (mark & 0x3L) == 0x1L; }
    public boolean hasBiasPattern() { return (mark & 0x5L) == 0x5L; }
    public boolean isForwarded()    { return (mark & 0x3L) == 0x3L; }
    public int     age()            { return (int) ((mark >>> 3) & 0xF); }

    public InstanceKlass getInstanceKlass() {
        return new InstanceKlass(klass);
    }

    public boolean isInstanceOf(InstanceKlass ik) {
        return klass == ik.getAddress();
    }

    public float getFloat(int offset)  { return UnsafeUtils.unsafe.getFloat(address + offset); }
    public void  putFloat(int offset, float v)  { UnsafeUtils.unsafe.putFloat(address + offset, v); }
    public int   getInt(int offset)    { return UnsafeUtils.unsafe.getInt(address + offset); }
    public void  putInt(int offset, int v)     { UnsafeUtils.unsafe.putInt(address + offset, v); }
    public long  getLong(int offset)   { return UnsafeUtils.unsafe.getLong(address + offset); }
    public void  putLong(int offset, long v)   { UnsafeUtils.unsafe.putLong(address + offset, v); }

    @Override
    public String toString() {
        String k = USE_COMPRESSED_CLASS_POINTERS
                ? String.format("narrowKlass=0x%x, base=0x%x, klass=0x%x",
                narrowKlass, klassBase, klass)
                : String.format("klass=0x%x", klass);
        return String.format("OopDesc[address=0x%x, mark=0x%x, %s]",
                address, mark, k);
    }
}