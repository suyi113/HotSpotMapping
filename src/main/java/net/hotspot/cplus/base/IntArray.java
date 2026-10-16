package net.hotspot.cplus.base;

public final class IntArray extends CArray {

    public IntArray(long length) {
        super(length, 4);
    }

    public int get(long i) {
        return getInt(i);
    }

    public void set(long i, int v) {
        putInt(i, v);
    }

    public void fill(int value) {
        checkAlive();
        for (long i = 0; i < length; i++) putInt(i, value);
    }
}