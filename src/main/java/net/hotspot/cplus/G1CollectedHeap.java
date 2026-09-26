
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class G1CollectedHeap extends CollectedHeap {

    private static final long SUMMARY_BYTES_USED_OFF =
            VMStructsHelper.findOffsetOrDefault("G1CollectedHeap::_summary_bytes_used", 568L);
    private static final long HRM_OFF =
            VMStructsHelper.findOffsetOrDefault("G1CollectedHeap::_hrm", 304L);
    private static final long G1MM_OFF =
            VMStructsHelper.findOffsetOrDefault("G1CollectedHeap::_g1mm", 856L);
    private static final long OLD_SET_OFF =
            VMStructsHelper.findOffsetOrDefault("G1CollectedHeap::_old_set", 160L);

    public final long summaryBytesUsed;
    public final long g1mm;
    public final long oldSetAddress;

    public G1CollectedHeap(long address) {
        super(address);
        this.summaryBytesUsed = UnsafeUtils.unsafe.getLong(address + SUMMARY_BYTES_USED_OFF);
        this.g1mm             = UnsafeUtils.unsafe.getLong(address + G1MM_OFF);
        this.oldSetAddress    = address + OLD_SET_OFF;
    }

    public HeapRegionManager hrm() {
        return new HeapRegionManager(address + HRM_OFF);
    }

    public static G1CollectedHeap current() {
        long addr = UnsafeUtils.unsafe.getLong(
                VMStructsHelper.findStaticAddress("Universe::_collectedHeap"));
        return new G1CollectedHeap(addr);
    }

    @Override
    public String toString() {
        return String.format(
                "G1CollectedHeap[address=0x%x, summaryBytesUsed=%d, g1mm=0x%x]",
                address, summaryBytesUsed, g1mm);
    }
}