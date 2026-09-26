package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiMethod extends CiMetadata {

    private static final long FLAGS_OFF               = 24L;
    private static final long NAME_OFF                = 32L;
    private static final long HOLDER_OFF              = 40L;
    private static final long SIGNATURE_OFF           = 48L;
    private static final long METHOD_DATA_OFF         = 56L;
    private static final long METHOD_BLOCKS_OFF       = 64L;

    private static final long CODE_SIZE_OFF           = 72L;
    private static final long MAX_STACK_OFF           = 76L;
    private static final long MAX_LOCALS_OFF          = 80L;
    private static final long INTRINSIC_ID_OFF        = 84L;
    private static final long HANDLER_COUNT_OFF       = 88L;
    private static final long NMETHOD_AGE_OFF         = 92L;
    private static final long INTERP_INV_OFF          = 96L;
    private static final long INTERP_THROW_OFF        = 100L;
    private static final long INSTR_SIZE_OFF          = 104L;
    private static final long SIZE_OF_PARAMS_OFF      = 108L;

    private static final long USES_MONITORS_OFF       = 112L;
    private static final long BALANCED_MONITORS_OFF   = 113L;
    private static final long IS_C1_COMPILABLE_OFF    = 114L;
    private static final long IS_C2_COMPILABLE_OFF    = 115L;
    private static final long CAN_BE_PARSED_OFF       = 116L;
    private static final long CAN_BE_STATICALLY_BOUND_OFF = 117L;
    private static final long HAS_RESERVED_STACK_ACCESS_OFF = 118L;
    private static final long IS_OVERPASS_OFF         = 119L;

    private static final long CODE_CACHE_OFF          = 120L;
    private static final long EXCEPTION_HANDLERS_OFF  = 128L;

    public CiMethod(long address) { super(address); }

    public long methodAddr() { return metadataAddr(); }

    public Method method() {
        long m = methodAddr();
        return (m == 0 || m < 0x10000L) ? null : new Method(m);
    }

    public CiSymbol name() {
        long p = UnsafeUtils.unsafe.getLong(address + NAME_OFF);
        return p == 0 ? null : new CiSymbol(p);
    }

    public CiInstanceKlass holder() {
        long p = UnsafeUtils.unsafe.getLong(address + HOLDER_OFF);
        return p == 0 ? null : new CiInstanceKlass(p);
    }

    public long signatureAddr() {
        return UnsafeUtils.unsafe.getLong(address + SIGNATURE_OFF);
    }

    public int codeSize()   { return UnsafeUtils.unsafe.getInt(address + CODE_SIZE_OFF); }
    public int maxStack()   { return UnsafeUtils.unsafe.getInt(address + MAX_STACK_OFF); }
    public int maxLocals()  { return UnsafeUtils.unsafe.getInt(address + MAX_LOCALS_OFF); }
    public int handlerCount() { return UnsafeUtils.unsafe.getInt(address + HANDLER_COUNT_OFF); }
    public int nmethodAge() { return UnsafeUtils.unsafe.getInt(address + NMETHOD_AGE_OFF); }
    public int sizeOfParameters() { return UnsafeUtils.unsafe.getInt(address + SIZE_OF_PARAMS_OFF); }

    public int interpreterInvocationCount() {
        return UnsafeUtils.unsafe.getInt(address + INTERP_INV_OFF);
    }
    public int interpreterThrowoutCount() {
        return UnsafeUtils.unsafe.getInt(address + INTERP_THROW_OFF);
    }
    public int instructionsSize() {
        return UnsafeUtils.unsafe.getInt(address + INSTR_SIZE_OFF);
    }

    public boolean usesMonitors() {
        return UnsafeUtils.unsafe.getByte(address + USES_MONITORS_OFF) != 0;
    }
    public boolean balancedMonitors() {
        return UnsafeUtils.unsafe.getByte(address + BALANCED_MONITORS_OFF) != 0;
    }
    public boolean isC1Compilable() {
        return UnsafeUtils.unsafe.getByte(address + IS_C1_COMPILABLE_OFF) != 0;
    }
    public boolean isC2Compilable() {
        return UnsafeUtils.unsafe.getByte(address + IS_C2_COMPILABLE_OFF) != 0;
    }
    public boolean canBeParsed() {
        return UnsafeUtils.unsafe.getByte(address + CAN_BE_PARSED_OFF) != 0;
    }
    public boolean canBeStaticallyBound() {
        return UnsafeUtils.unsafe.getByte(address + CAN_BE_STATICALLY_BOUND_OFF) != 0;
    }
    public boolean hasReservedStackAccess() {
        return UnsafeUtils.unsafe.getByte(address + HAS_RESERVED_STACK_ACCESS_OFF) != 0;
    }
    public boolean isOverpass() {
        return UnsafeUtils.unsafe.getByte(address + IS_OVERPASS_OFF) != 0;
    }

    public long cachedCodePtr() {
        return UnsafeUtils.unsafe.getLong(address + CODE_CACHE_OFF);
    }

    public void setCodeSize(int v)  { UnsafeUtils.unsafe.putInt(address + CODE_SIZE_OFF, v); }
    public void setMaxStack(int v)  { UnsafeUtils.unsafe.putInt(address + MAX_STACK_OFF, v); }
    public void setMaxLocals(int v) { UnsafeUtils.unsafe.putInt(address + MAX_LOCALS_OFF, v); }
    public void clearCachedCode()   { UnsafeUtils.unsafe.putLong(address + CODE_CACHE_OFF, 0L); }

    public void refreshFromMethod() {
        Method m = method();
        if (m == null) return;
        ConstMethod cm = new ConstMethod(m.constMethod);
        if (cm.getAddress() == 0) return;
        setCodeSize(cm.codeSize & 0xFFFF);
        setMaxStack(cm.maxStack & 0xFFFF);
        setMaxLocals(cm.maxLocals & 0xFFFF);
        clearCachedCode();
    }

    @Override
    public String toString() {
        Method m = method();
        String nm = (m == null) ? "?" : m.name();
        return String.format(
                "CiMethod[addr=0x%x, method=0x%x, %s, codeSize=%d, " +
                        "maxStack=%d, maxLocals=%d, c1=%b, c2=%b]",
                address, methodAddr(), nm,
                codeSize(), maxStack(), maxLocals(),
                isC1Compilable(), isC2Compilable());
    }
}