
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class HeapRegionType {

    private static final long TAG_OFF = VMStructsHelper.findOffsetOrDefault("HeapRegionType::_tag", 0L);

    private static final int FREE_TAG                = 0;
    private static final int YOUNG_TAG               = 1;
    private static final int EDEN_TAG                = 2;
    private static final int SURV_TAG                = 3;
    private static final int STARTS_HUMONGOUS_TAG    = 12;
    private static final int CONTINUES_HUMONGOUS_TAG = 13;
    private static final int OLD_TAG                 = 16;

    private static final int TAG_MASK     = 0x1F;
    private static final int PINNED_MASK  = 0x08;
    private static final int ARCHIVE_MASK = 0x20;

    private final long address;
    public final int tag;

    public HeapRegionType(long address) {
        this.address = address;
        this.tag     = UnsafeUtils.unsafe.getInt(address + TAG_OFF) & 0xFF;
    }

    public long getAddress() { return address; }

    private int raw() { return tag & TAG_MASK; }

    public boolean isFree()               { return raw() == FREE_TAG; }
    public boolean isYoung()              { return raw() == YOUNG_TAG || raw() == EDEN_TAG || raw() == SURV_TAG; }
    public boolean isEden()               { return raw() == EDEN_TAG; }
    public boolean isSurvivor()           { return raw() == SURV_TAG; }
    public boolean isHumongous()          { return raw() == STARTS_HUMONGOUS_TAG || raw() == CONTINUES_HUMONGOUS_TAG; }
    public boolean isStartsHumongous()    { return raw() == STARTS_HUMONGOUS_TAG; }
    public boolean isContinuesHumongous() { return raw() == CONTINUES_HUMONGOUS_TAG; }
    public boolean isOld()                { return raw() == OLD_TAG; }
    public boolean isPinned()             { return (tag & PINNED_MASK) != 0; }
    public boolean isArchive()            { return (tag & ARCHIVE_MASK) != 0; }

    public String name() {
        if (isFree())               return "Free";
        if (isStartsHumongous())    return "StartsHumongous";
        if (isContinuesHumongous()) return "ContinuesHumongous";
        if (isEden())               return "Eden";
        if (isSurvivor())           return "Survivor";
        if (isOld())                return "Old";
        if (isYoung())              return "Young";
        return "tag=" + tag;
    }

    @Override
    public String toString() {
        return String.format("HeapRegionType[address=0x%x, tag=0x%x, name=%s]",
                address, tag, name());
    }
}