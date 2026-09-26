package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiObjArrayKlass extends CiArrayKlass {
    private static final long ELEMENT_KLASS_OFF      = 48L;
    private static final long BASE_ELEMENT_KLASS_OFF = 56L;

    public CiObjArrayKlass(long address) { super(address); }

    public CiKlass elementKlass() {
        long p = UnsafeUtils.unsafe.getLong(address + ELEMENT_KLASS_OFF);
        return p == 0 ? null : new CiKlass(p);
    }

    public CiKlass baseElementKlass() {
        long p = UnsafeUtils.unsafe.getLong(address + BASE_ELEMENT_KLASS_OFF);
        return p == 0 ? null : new CiKlass(p);
    }
}