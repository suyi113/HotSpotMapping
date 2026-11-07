package net.hotspot.cplus.jdk21.ci;

import net.hotspot.cplus.jdk21.core.UnsafeUtils;

public class CiType extends CiMetadata {
    private static final long BASIC_TYPE_OFF = 24L;

    public CiType(long address) { super(address); }

    public byte basicType() {
        return UnsafeUtils.unsafe.getByte(address + BASIC_TYPE_OFF);
    }
}
