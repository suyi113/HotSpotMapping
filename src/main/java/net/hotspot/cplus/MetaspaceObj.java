package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

import sun.misc.Unsafe;

public class MetaspaceObj{
    protected Unsafe unsafe = UnsafeUtils.unsafe;
    private static final long VPTR_OFFSET  = VMStructsHelper.findOffsetOrDefault("MetaspaceObj::_vptr",  0L);
    private static final long VALID_OFFSET = VMStructsHelper.findOffsetOrDefault("MetaspaceObj::_valid", 8L);

    public long vptr;

    public int valid;

    protected final long address;



    public MetaspaceObj(long address) {
        if (address == 0) {
            throw new IllegalArgumentException("address is null");
        }
        this.address = address;

        this.vptr = UnsafeUtils.unsafe.getLong(address + VPTR_OFFSET);

        this.valid = UnsafeUtils.unsafe.getInt(address + VALID_OFFSET);
    }

    public MetaspaceObj(Class<?> clazz) {
        this(UnsafeUtils.getKlassLong(clazz));
    }


    public long getAddress() {
        return address;
    }

    public int identityHash() {
        return (int) address;
    }

    public boolean isValid() {
        return valid == 0;
    }

    @Override
    public String toString() {
        return String.format("MetaspaceObj[address=0x%x, vptr=0x%x, valid=%d]",
                address, vptr, valid);
    }
}
