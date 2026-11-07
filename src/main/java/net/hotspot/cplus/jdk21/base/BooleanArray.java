package net.hotspot.cplus.jdk21.base;

public final class BooleanArray extends CArray {

    public BooleanArray(long length) {
        super(length, 1);
    }

    public boolean get(long i) {
        return getByte(i) != 0;
    }

    public void set(long i, boolean v) {
        putByte(i, (byte) (v ? 1 : 0));
    }
}
