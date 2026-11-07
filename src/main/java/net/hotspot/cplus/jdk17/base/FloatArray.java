package net.hotspot.cplus.jdk17.base;

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
