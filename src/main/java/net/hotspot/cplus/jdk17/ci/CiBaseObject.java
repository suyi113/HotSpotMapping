package net.hotspot.cplus.jdk17.ci;

import net.hotspot.cplus.jdk17.core.UnsafeUtils;

public class CiBaseObject {
    protected static final long IDENT_OFF = 8L;

    protected final long address;

    public CiBaseObject(long address) { this.address = address; }

    public long getAddress() { return address; }

    public int ident() { return UnsafeUtils.unsafe.getInt(address + IDENT_OFF); }
}
