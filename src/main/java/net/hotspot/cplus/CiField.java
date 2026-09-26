package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiField extends CiBaseObject {
    private static final long HANDLE_OFF     = 16L;
    private static final long HOLDER_OFF     = 24L;
    private static final long NAME_OFF       = 32L;
    private static final long SIGNATURE_OFF  = 40L;
    private static final long OFFSET_OFF     = 48L;
    private static final long IS_CONSTANT_OFF = 52L;

    public CiField(long address) { super(address); }

    public CiInstanceKlass holder() {
        long p = UnsafeUtils.unsafe.getLong(address + HOLDER_OFF);
        return p == 0 ? null : new CiInstanceKlass(p);
    }

    public CiSymbol name() {
        long p = UnsafeUtils.unsafe.getLong(address + NAME_OFF);
        return p == 0 ? null : new CiSymbol(p);
    }

    public CiSymbol signature() {
        long p = UnsafeUtils.unsafe.getLong(address + SIGNATURE_OFF);
        return p == 0 ? null : new CiSymbol(p);
    }

    public int offset() {
        return UnsafeUtils.unsafe.getInt(address + OFFSET_OFF);
    }

    public boolean isConstant() {
        return UnsafeUtils.unsafe.getByte(address + IS_CONSTANT_OFF) != 0;
    }
}