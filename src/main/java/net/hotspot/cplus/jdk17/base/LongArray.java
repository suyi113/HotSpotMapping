package net.hotspot.cplus.jdk17.base;

public final class LongArray extends CArray {

    public LongArray(long length) {
        super(length, 8);
    }

    public long get(long i) {
        return getLong(i);
    }

    public void set(long i, long v) {
        putLong(i, v);
    }
}
