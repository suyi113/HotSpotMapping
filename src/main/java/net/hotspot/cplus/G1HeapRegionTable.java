
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class G1HeapRegionTable {

    private static final long BASE_OFF        = VMStructsHelper.findOffsetOrDefault("G1HeapRegionTable::_base",        16L);
    private static final long LENGTH_OFF      = VMStructsHelper.findOffsetOrDefault("G1HeapRegionTable::_length",      24L);
    private static final long BIASED_BASE_OFF = VMStructsHelper.findOffsetOrDefault("G1HeapRegionTable::_biased_base", 32L);
    private static final long BIAS_OFF        = VMStructsHelper.findOffsetOrDefault("G1HeapRegionTable::_bias",        40L);
    private static final long SHIFT_BY_OFF    = VMStructsHelper.findOffsetOrDefault("G1HeapRegionTable::_shift_by",    48L);

    private final long address;

    public final long base;
    public final long length;
    public final long biasedBase;
    public final long bias;
    public final int  shiftBy;

    public G1HeapRegionTable(long address) {
        this.address    = address;
        this.base       = UnsafeUtils.unsafe.getLong(address + BASE_OFF);
        this.length     = UnsafeUtils.unsafe.getLong(address + LENGTH_OFF);
        this.biasedBase = UnsafeUtils.unsafe.getLong(address + BIASED_BASE_OFF);
        this.bias       = UnsafeUtils.unsafe.getLong(address + BIAS_OFF);
        this.shiftBy    = UnsafeUtils.unsafe.getInt (address + SHIFT_BY_OFF);
    }

    public long getAddress() { return address; }

    public HeapRegion at(int index) {
        long r = UnsafeUtils.unsafe.getLong(base + (long) index * 8L);
        return r == 0 ? null : new HeapRegion(index, r);
    }

    @Override
    public String toString() {
        return String.format(
                "G1HeapRegionTable[address=0x%x, base=0x%x, length=%d, biasedBase=0x%x, bias=%d, shiftBy=%d]",
                address, base, length, biasedBase, bias, shiftBy);
    }
}