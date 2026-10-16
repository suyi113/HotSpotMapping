package net.hotspot.cplus.base;

public final class ByteArray extends CArray {

    public ByteArray(long length) {
        super(length, 1);
    }

    public byte get(long i) {
        return getByte(i);
    }

    public void set(long i, byte v) {
        putByte(i, v);
    }
}