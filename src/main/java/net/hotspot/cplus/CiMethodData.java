package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiMethodData extends CiMetadata {
    private static final long DATA_SIZE_OFF       = 24L;
    private static final long STATE_OFF           = 28L;
    private static final long EXTRA_DATA_SIZE_OFF = 32L;
    private static final long DATA_OFF            = 40L;
    private static final long HINT_DI_OFF         = 48L;
    private static final long EFLAGS_OFF          = 52L;
    private static final long ARG_LOCAL_OFF       = 56L;
    private static final long ARG_STACK_OFF       = 60L;
    private static final long ARG_RETURNED_OFF    = 64L;
    private static final long CURRENT_MILEAGE_OFF = 68L;

    public CiMethodData(long address) { super(address); }

    public int dataSize()       { return UnsafeUtils.unsafe.getInt(address + DATA_SIZE_OFF); }
    public byte state()         { return UnsafeUtils.unsafe.getByte(address + STATE_OFF); }
    public int extraDataSize()  { return UnsafeUtils.unsafe.getInt(address + EXTRA_DATA_SIZE_OFF); }
    public long data()          { return UnsafeUtils.unsafe.getLong(address + DATA_OFF); }
    public int hintDi()         { return UnsafeUtils.unsafe.getInt(address + HINT_DI_OFF); }
    public int eflags()         { return UnsafeUtils.unsafe.getInt(address + EFLAGS_OFF); }
    public int argLocal()       { return UnsafeUtils.unsafe.getInt(address + ARG_LOCAL_OFF); }
    public int argStack()       { return UnsafeUtils.unsafe.getInt(address + ARG_STACK_OFF); }
    public int argReturned()    { return UnsafeUtils.unsafe.getInt(address + ARG_RETURNED_OFF); }
    public int currentMileage() { return UnsafeUtils.unsafe.getInt(address + CURRENT_MILEAGE_OFF); }
}