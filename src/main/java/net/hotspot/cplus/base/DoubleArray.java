package net.hotspot.cplus.base;

public final class DoubleArray extends CArray {

    public DoubleArray(long length) {
        super(length, 8);
    }

    public double get(long i) {
        return getDouble(i);
    }

    public void set(long i, double v) {
        putDouble(i, v);
    }
}