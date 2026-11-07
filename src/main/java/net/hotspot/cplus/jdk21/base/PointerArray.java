package net.hotspot.cplus.jdk21.base;

public final class PointerArray extends CArray {

    private static final int ADDRESS_SIZE = UNSAFE.addressSize();

    public PointerArray(long length) {
        super(length, ADDRESS_SIZE);
    }

    public long get(long i) {
        return getAddress(i);
    }

    public void set(long i, long v) {
        putAddress(i, v);
    }

    public void setNull(long i) {
        putAddress(i, 0L);
    }

    public boolean isNull(long i) {
        return getAddress(i) == 0L;
    }
}
