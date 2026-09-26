package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class FieldArray {

    private static final long LENGTH_OFFSET = 0L;
    private static final long DATA_OFFSET   = 4L;

    private final long address;
    public final int length;
    public final int fieldCount;

    public FieldArray(long address) {
        this.address = address;
        if (address == 0) {
            this.length = 0;
            this.fieldCount = 0;
            return;
        }
        this.length = UnsafeUtils.unsafe.getInt(address + LENGTH_OFFSET);
        this.fieldCount = length / FieldInfo.FIELD_SLOTS;
    }

    public long fieldInfoAddr(int index) {
        if (index < 0 || index >= fieldCount) {
            throw new IndexOutOfBoundsException("index: " + index + ", fieldCount: " + fieldCount);
        }
        return address + DATA_OFFSET + index * FieldInfo.FIELD_SLOTS * 2L;
    }

    public FieldInfo fieldAt(int index) {
        return new FieldInfo(fieldInfoAddr(index));
    }

    public FieldInfo[] toFields() {
        FieldInfo[] result = new FieldInfo[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            result[i] = new FieldInfo(fieldInfoAddr(i));
        }
        return result;
    }

    public long getAddress() { return address; }

    @Override
    public String toString() {
        return "FieldArray[address=0x" + Long.toHexString(address)
                + ", length=" + length + ", fieldCount=" + fieldCount + "]";
    }
}