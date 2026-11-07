package net.hotspot.cplus.jdk17.ci;

import net.hotspot.cplus.jdk17.core.UnsafeUtils;

public class CiArrayKlass extends CiKlass {
    private static final long DIMENSION_OFF = 40L;

    public CiArrayKlass(long address) { super(address); }

    public int dimension() {
        return UnsafeUtils.unsafe.getInt(address + DIMENSION_OFF);
    }
}
