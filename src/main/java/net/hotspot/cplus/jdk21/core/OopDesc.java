package net.hotspot.cplus.jdk21.core;

import net.hotspot.cplus.jdk21.klass.InstanceKlass;
import net.hotspot.cplus.jdk21.klass.Klass;

import com.sun.management.HotSpotDiagnosticMXBean;
import net.hotspot.cplus.jdk21.core.UnsafeUtils;

import java.lang.management.ManagementFactory;

public class OopDesc {

    public static final boolean USE_COMPRESSED_OOPS;
    public static final boolean USE_COMPRESSED_CLASS_POINTERS;

    public static final long HEAP_BASE;

    public static final int  HEAP_SHIFT;

    public static final long KLASS_BASE;

    public static final int  KLASS_SHIFT;

    private static final long MARK_OFFSET  = 0L;
    private static final long KLASS_OFFSET = 8L;

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

        long heapBase = UnsafeUtils.staticLongValue("CompressedOops::_narrow_oop._base", 0L);
        int heapShift = UnsafeUtils.staticIntValue("CompressedOops::_narrow_oop._shift", 3);
        long klassBase = UnsafeUtils.staticLongValue("CompressedKlassPointers::_narrow_klass._base", 0L);
        int klassShift = UnsafeUtils.staticIntValue("CompressedKlassPointers::_narrow_klass._shift", 3);

        String override = System.getProperty("hotspot.heapBase");
        if (override != null && !override.isEmpty()) {
            try {
                heapBase = Long.decode(override);
            } catch (NumberFormatException ignored) { }
        }
        HEAP_BASE   = heapBase;
        HEAP_SHIFT  = heapShift;
        KLASS_BASE  = klassBase;
        KLASS_SHIFT = klassShift;
    }

    private long address;

    private final Object quote;

    public long mark;
    public long klass;
    public int  narrowKlass;

    public static OopDesc of(Object instance) {
        if (instance == null) return null;
        return new OopDesc(addressOf(instance), instance);
    }

    public static OopDesc of(long oopAddress, Object anchor) {
        return new OopDesc(oopAddress, anchor);
    }

    public static OopDesc of(long oopAddress) {
        return new OopDesc(oopAddress, null);
    }

    public OopDesc(long address, Object anchor) {
        this.address = address;
        this.quote = anchor;
        if (address == 0) return;
        readFields();
    }

    public boolean hasQuote() {
        return quote != null;
    }

    public void refreshQuote() {
        if (quote == null) throw new NullPointerException("Quote can't be null");
        this.address = addressOf(quote);
        readFields();
    }

    public void refreshQuoteIfPossible() {
        if (quote != null) {
            this.address = addressOf(quote);
            readFields();
        }
    }

    private void readFields() {
        mark = UnsafeUtils.unsafe.getLong(address + MARK_OFFSET);
        if (USE_COMPRESSED_CLASS_POINTERS) {
            narrowKlass = UnsafeUtils.unsafe.getInt(address + KLASS_OFFSET);
            klass = KLASS_BASE + ((narrowKlass & 0xFFFFFFFFL) << KLASS_SHIFT);
        } else {
            narrowKlass = 0;
            klass = UnsafeUtils.unsafe.getLong(address + KLASS_OFFSET);
        }
    }

    public static long addressOf(Object o) {
        if (o == null) return 0;
        Object[] holder = new Object[] { o };
        long base = UnsafeUtils.unsafe.arrayBaseOffset(Object[].class);
        if (USE_COMPRESSED_OOPS) {
            int narrow = UnsafeUtils.unsafe.getInt(holder, base);
            long unsigned = narrow & 0xFFFFFFFFL;
            return HEAP_BASE + (unsigned << HEAP_SHIFT);
        } else {
            return UnsafeUtils.unsafe.getLong(holder, base);
        }
    }

    public long getAddress() { return address; }

    public long fieldBase() { return address + (USE_COMPRESSED_CLASS_POINTERS ? 12L : 16L); }

    public boolean isLocked()       { return (mark & 0x3L) != 0; }
    public boolean isUnlocked()     { return (mark & 0x3L) == 0x1L; }
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
    public Object getObject(int offset) {
        if (USE_COMPRESSED_OOPS) {
            int narrow = UnsafeUtils.unsafe.getInt(address + offset);
            long oop = HEAP_BASE + ((narrow & 0xFFFFFFFFL) << HEAP_SHIFT);
            return oop == 0 ? null : UnsafeUtils.oopToJavaObject(oop);
        }
        long oop = UnsafeUtils.unsafe.getLong(address + offset);
        return oop == 0 ? null : UnsafeUtils.oopToJavaObject(oop);
    }

    @Override
    public String toString() {
        String k = USE_COMPRESSED_CLASS_POINTERS
                ? String.format("narrowKlass=0x%x, klass=0x%x", narrowKlass, klass)
                : String.format("klass=0x%x", klass);
        return String.format("OopDesc[address=0x%x, mark=0x%x, %s]", address, mark, k);
    }
}
