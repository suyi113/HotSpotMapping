
package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


public class CollectedHeap {

    protected final long address;

    public CollectedHeap(long address) {
        this.address = address;
    }

    public long getAddress() { return address; }

    public static CollectedHeap current() {
        long addr = UnsafeUtils.unsafe.getLong(
                VMStructsHelper.findStaticAddress("Universe::_collectedHeap"));
        return new CollectedHeap(addr);
    }

    @Override
    public String toString() {
        return String.format("CollectedHeap[address=0x%x]", address);
    }
}