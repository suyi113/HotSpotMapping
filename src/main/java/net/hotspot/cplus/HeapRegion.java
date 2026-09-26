
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class HeapRegion {

    private static final long BOTTOM_OFF         = VMStructsHelper.findOffsetOrDefault("HeapRegion::_bottom",         0L);
    private static final long TOP_OFF            = VMStructsHelper.findOffsetOrDefault("HeapRegion::_top",            16L);
    private static final long END_OFF            = VMStructsHelper.findOffsetOrDefault("HeapRegion::_end",            8L);
    private static final long COMPACTION_TOP_OFF = VMStructsHelper.findOffsetOrDefault("HeapRegion::_compaction_top", 24L);
    private static final long TYPE_OFF           = VMStructsHelper.findOffsetOrDefault("HeapRegion::_type",           148L);

    private final long address;

    public final int  index;
    public final long bottom;
    public final long top;
    public final long end;
    public final long compactionTop;

    public HeapRegion(int index, long address) {
        this.index         = index;
        this.address       = address;
        this.bottom        = UnsafeUtils.unsafe.getLong(address + BOTTOM_OFF);
        this.top           = UnsafeUtils.unsafe.getLong(address + TOP_OFF);
        this.end           = UnsafeUtils.unsafe.getLong(address + END_OFF);
        this.compactionTop = UnsafeUtils.unsafe.getLong(address + COMPACTION_TOP_OFF);
    }

    public long getAddress() { return address; }

    public long used() { return top - bottom; }
    public long size() { return end - bottom; }
    public boolean isEmpty() { return bottom == top; }

    public HeapRegionType type() {
        return new HeapRegionType(address + TYPE_OFF);
    }

    @Override
    public String toString() {
        return String.format(
                "HeapRegion[%d, address=0x%x, bottom=0x%x, top=0x%x, end=0x%x, used=%d, type=%s]",
                index, address, bottom, top, end, used(), type().name());
    }
}