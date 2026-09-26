
package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


import java.util.ArrayList;
import java.util.List;

public class HeapRegionManager {

    private static final long REGIONS_OFF =
            VMStructsHelper.findOffsetOrDefault("HeapRegionManager::_regions", 96L);

    private final long address;

    public HeapRegionManager(long address) {
        this.address = address;
    }

    public long getAddress() { return address; }

    public G1HeapRegionTable regions() {
        return new G1HeapRegionTable(address + REGIONS_OFF);
    }

    public List<HeapRegion> allRegions() {
        G1HeapRegionTable table = regions();
        List<HeapRegion> out = new ArrayList<>((int) table.length);
        for (int i = 0; i < table.length; i++) {
            long r = UnsafeUtils.unsafe.getLong(table.base + (long) i * 8L);
            if (r == 0) continue;
            out.add(new HeapRegion(i, r));
        }
        return out;
    }

    @Override
    public String toString() {
        return String.format("HeapRegionManager[address=0x%x]", address);
    }
}