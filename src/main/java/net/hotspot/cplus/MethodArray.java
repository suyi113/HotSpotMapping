package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class MethodArray {

    private static final long LENGTH_OFFSET = 0L;
    private static final long DATA_OFFSET   = 8L;

    private final long address;
    public final int length;
    public final long[] methodAddresses;

    public MethodArray(long address) {
        this.address = address;
        if (address == 0) {
            this.length = 0;
            this.methodAddresses = new long[0];
            return;
        }
        this.length = UnsafeUtils.unsafe.getInt(address + LENGTH_OFFSET);
        this.methodAddresses = new long[length];
        for (int i = 0; i < length; i++) {
            methodAddresses[i] = UnsafeUtils.unsafe.getLong(address + DATA_OFFSET + i * 8L);
        }
    }

    public long getAddress() {
        return address;
    }

    public Method methodAt(int i) {
        if (i < 0 || i >= length) throw new IndexOutOfBoundsException("index: " + i + ", length: " + length);
        return new Method(methodAddresses[i]);
    }

    public Method[] toMethods() {
        Method[] result = new Method[length];
        for (int i = 0; i < length; i++) {
            result[i] = new Method(methodAddresses[i]);
        }
        return result;
    }

    @Override
    public String toString() {
        return "MethodArray[address=0x" + Long.toHexString(address) + ", length=" + length + "]";
    }
}