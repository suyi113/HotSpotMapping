package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiMetadata extends CiBaseObject {
    protected static final long METADATA_OFF = 16L;

    public CiMetadata(long address) { super(address); }

    public long metadataAddr() {
        return UnsafeUtils.unsafe.getLong(address + METADATA_OFF);
    }
}