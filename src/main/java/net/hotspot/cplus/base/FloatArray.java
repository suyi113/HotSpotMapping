package net.hotspot.cplus.base;

public final class FloatArray extends CArray {

    public FloatArray(long length) {
        super(length, 4);
    }

    public float get(long i) {
        return getFloat(i);
    }

    public void set(long i, float v) {
        putFloat(i, v);
    }
}