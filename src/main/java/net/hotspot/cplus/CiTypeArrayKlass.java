package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiTypeArrayKlass extends CiArrayKlass {
    private static final long ELEMENT_TYPE_OFF = 44L;

    public CiTypeArrayKlass(long address) { super(address); }

    public byte elementType() {
        return UnsafeUtils.unsafe.getByte(address + ELEMENT_TYPE_OFF);
    }
}