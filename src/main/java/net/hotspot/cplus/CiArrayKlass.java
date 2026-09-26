package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiArrayKlass extends CiKlass {
    private static final long DIMENSION_OFF = 40L;

    public CiArrayKlass(long address) { super(address); }

    public int dimension() {
        return UnsafeUtils.unsafe.getInt(address + DIMENSION_OFF);
    }
}