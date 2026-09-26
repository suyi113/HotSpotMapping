package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


public class CiSymbol extends CiMetadata {
    private static final long SYMBOL_OFF = 24L;

    public CiSymbol(long address) { super(address); }

    public long symbolAddr() {
        return UnsafeUtils.unsafe.getLong(address + SYMBOL_OFF);
    }

    public String value() {
        long s = symbolAddr();
        return s == 0 ? null : new Symbol(s).value;
    }

    @Override
    public String toString() {
        return String.format("CiSymbol[addr=0x%x, value=\"%s\"]",
                address, value());
    }
}