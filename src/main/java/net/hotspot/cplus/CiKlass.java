package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


public class CiKlass extends CiType {
    private static final long NAME_OFF = 32L;

    public CiKlass(long address) { super(address); }

    public long nameAddr() {
        return UnsafeUtils.unsafe.getLong(address + NAME_OFF);
    }

    public CiSymbol name() {
        long n = nameAddr();
        return n == 0 ? null : new CiSymbol(n);
    }

    @Override
    public String toString() {
        CiSymbol n = name();
        return String.format("CiKlass[addr=0x%x, name=%s]",
                address, n == null ? "?" : n.value());
    }
}