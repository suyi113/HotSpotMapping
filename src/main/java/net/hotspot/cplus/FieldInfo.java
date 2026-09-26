package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class FieldInfo {

    public static final int ACCESS_FLAGS_OFFSET    = 0;
    public static final int NAME_INDEX_OFFSET      = 1;
    public static final int SIGNATURE_INDEX_OFFSET = 2;
    public static final int INITVAL_INDEX_OFFSET   = 3;
    public static final int LOW_PACKED_OFFSET      = 4;
    public static final int HIGH_PACKED_OFFSET     = 5;
    public static final int FIELD_SLOTS            = 6;

    public static final int FIELDINFO_TAG_SIZE     = 2;
    public static final int FIELDINFO_TAG_OFFSET   = 1 << 0;
    public static final int FIELDINFO_TAG_CONTENDED= 1 << 1;

    private final long address;

    public int  accessFlags;
    public int  nameIndex;
    public int  signatureIndex;
    public int  initvalIndex;
    public int  lowPacked;
    public int  highPacked;

    public int  offset;
    public boolean isContended;
    public int  contendedGroup;

    public FieldInfo(long address) {
        this.address = address;
        if (address == 0) return;
        readFields();
    }

    private void readFields() {
        accessFlags    = UnsafeUtils.unsafe.getShort(address + ACCESS_FLAGS_OFFSET * 2)    & 0xFFFF;
        nameIndex      = UnsafeUtils.unsafe.getShort(address + NAME_INDEX_OFFSET * 2)      & 0xFFFF;
        signatureIndex = UnsafeUtils.unsafe.getShort(address + SIGNATURE_INDEX_OFFSET * 2) & 0xFFFF;
        initvalIndex   = UnsafeUtils.unsafe.getShort(address + INITVAL_INDEX_OFFSET * 2)   & 0xFFFF;
        lowPacked      = UnsafeUtils.unsafe.getShort(address + LOW_PACKED_OFFSET * 2)      & 0xFFFF;
        highPacked     = UnsafeUtils.unsafe.getShort(address + HIGH_PACKED_OFFSET * 2)     & 0xFFFF;

        isContended = (lowPacked & FIELDINFO_TAG_CONTENDED) != 0;

        if ((lowPacked & FIELDINFO_TAG_OFFSET) != 0) {
            int packed = (highPacked << 16) | lowPacked;
            offset = packed >> FIELDINFO_TAG_SIZE;
            contendedGroup = -1;
        } else {
            offset = -1;
            contendedGroup = isContended ? highPacked : -1;
        }
    }

    public boolean isOffsetSet()    { return (lowPacked & FIELDINFO_TAG_OFFSET) != 0; }
    public boolean isStable()       { return (accessFlags & 0x0020) != 0; }
    public boolean isInternal()     { return (accessFlags & 0x0400) != 0; }

    public long getAddress() { return address; }

    public String name(ConstantPool cp) {
        if (isInternal()) return null;
        long symbolAddr = cp.symbolAt(nameIndex);
        if (symbolAddr == 0) return null;

        return new Symbol(symbolAddr).value;
    }

    public String signature(ConstantPool cp) {
        if (isInternal()) return null;
        long symbolAddr = cp.symbolAt(signatureIndex);
        return new Symbol(symbolAddr).value;
    }
    @Override
    public String toString() {
        return String.format(
                "FieldInfo[address=0x%x, accessFlags=0x%04X, nameIndex=%d, sigIndex=%d, initvalIndex=%d, " +
                        "offset=%d, isContended=%s, contendedGroup=%d]",
                address, accessFlags, nameIndex, signatureIndex, initvalIndex,
                offset, isContended, contendedGroup);
    }
}