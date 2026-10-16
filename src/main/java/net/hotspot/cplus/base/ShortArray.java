package net.hotspot.cplus.base;

public final class ShortArray extends CArray {

    public ShortArray(long length) {
        super(length, 2);
    }

    public short get(long i) {
        return getShort(i);
    }

    public void set(long i, short v) {
        putShort(i, v);
    }
}