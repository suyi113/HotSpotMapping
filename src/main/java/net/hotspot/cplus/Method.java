package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class Method extends Metadata {

    private static final long CONST_METHOD_OFFSET           = findOffsetOrDefault("Method::_constMethod",            8L);
    private static final long METHOD_DATA_OFFSET            = findOffsetOrDefault("Method::_method_data",            16L);
    private static final long METHOD_COUNTERS_OFFSET        = findOffsetOrDefault("Method::_method_counters",        24L);
    private static final long ACCESS_FLAGS_OFFSET           = findOffsetOrDefault("Method::_access_flags",           40L);
    private static final long VTABLE_INDEX_OFFSET           = findOffsetOrDefault("Method::_vtable_index",           44L);
    private static final long INTRINSIC_ID_OFFSET           = findOffsetOrDefault("Method::_intrinsic_id",           48L);
    private static final long FLAGS_OFFSET                  = findOffsetOrDefault("Method::_flags",                  50L);
    private static final long I2I_ENTRY_OFFSET              = findOffsetOrDefault("Method::_i2i_entry",              56L);
    private static final long FROM_COMPILED_ENTRY_OFFSET    = findOffsetOrDefault("Method::_from_compiled_entry",    64L);
    private static final long CODE_OFFSET                   = findOffsetOrDefault("Method::_code",                   72L);
    private static final long FROM_INTERPRETED_ENTRY_OFFSET = findOffsetOrDefault("Method::_from_interpreted_entry", 80L);
    private static final long ADAPTER_OFFSET                = findOffsetOrDefault("Method::_adapter",                32L);

    public long constMethod;
    public long methodData;
    public long methodCounters;
    public int  accessFlags;
    public int  vtableIndex;
    public short intrinsicId;
    public short flags;
    public long i2iEntry;
    public long fromCompiledEntry;
    public long code;
    public long fromInterpretedEntry;

    public Method(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    public Method(Class<?> clazz) {
        super(clazz);
        if (getAddress() == 0) return;
        readFields();
    }

    public void readFields() {
        long base = getAddress();
        if (base == 0) return;
        constMethod          = UnsafeUtils.unsafe.getLong(base + CONST_METHOD_OFFSET);
        methodData           = UnsafeUtils.unsafe.getLong(base + METHOD_DATA_OFFSET);
        methodCounters       = UnsafeUtils.unsafe.getLong(base + METHOD_COUNTERS_OFFSET);
        accessFlags          = UnsafeUtils.unsafe.getInt(base + ACCESS_FLAGS_OFFSET);
        vtableIndex          = UnsafeUtils.unsafe.getInt(base + VTABLE_INDEX_OFFSET);
        intrinsicId          = UnsafeUtils.unsafe.getShort(base + INTRINSIC_ID_OFFSET);
        flags                = UnsafeUtils.unsafe.getShort(base + FLAGS_OFFSET);
        i2iEntry             = UnsafeUtils.unsafe.getLong(base + I2I_ENTRY_OFFSET);
        fromCompiledEntry    = UnsafeUtils.unsafe.getLong(base + FROM_COMPILED_ENTRY_OFFSET);
        code                 = UnsafeUtils.unsafe.getLong(base + CODE_OFFSET);
        fromInterpretedEntry = UnsafeUtils.unsafe.getLong(base + FROM_INTERPRETED_ENTRY_OFFSET);
    }

    @Override
    public boolean isMethod() {
        return true;
    }

    public boolean isCompiled() {
        return code != 0;
    }

    public String name() {
        if (constMethod == 0) return null;
        ConstMethod cm = new ConstMethod(constMethod);
        if (cm.constants == 0 || cm.nameIndex == 0) return null;
        ConstantPool cp = new ConstantPool(cm.constants);
        long symbolAddr = cp.symbolAt(cm.nameIndex & 0xFFFF);
        if (symbolAddr == 0) return null;
        return new Symbol(symbolAddr).value;
    }

    public String signature() {
        if (constMethod == 0) return null;
        ConstMethod cm = new ConstMethod(constMethod);
        if (cm.constants == 0 || cm.signatureIndex == 0) return null;
        ConstantPool cp = new ConstantPool(cm.constants);
        long symbolAddr = cp.symbolAt(cm.signatureIndex & 0xFFFF);
        if (symbolAddr == 0) return null;
        return new Symbol(symbolAddr).value;
    }

    public AccessFlags accessFlags() {
        return new AccessFlags(accessFlags);
    }


    public void forceInterpreted() {
        long base = getAddress();
        long adapter  = unsafe.getLong(base + ADAPTER_OFFSET);
        long i2iEntry = unsafe.getLong(base + I2I_ENTRY_OFFSET);

        unsafe.putLong(base + CODE_OFFSET, 0L);

        if (adapter != 0) {
            long c2iEntry = unsafe.getLong(adapter + 32L);
            unsafe.putLong(base + FROM_COMPILED_ENTRY_OFFSET, c2iEntry);
        } else {
            unsafe.putLong(base + FROM_COMPILED_ENTRY_OFFSET, i2iEntry);
        }

        unsafe.putLong(base + FROM_INTERPRETED_ENTRY_OFFSET, i2iEntry);

        readFields();
    }

    public NMethod code() {
        if (code == 0) return null;
        return new NMethod(code);
    }
    public boolean hasNMethod() {
        return code != 0;
    }
    @Override
    public String toString() {
        return String.format(
                "Method[address=0x%x, constMethod=0x%x, accessFlags=0x%x (%s), vtableIndex=%d, intrinsicId=%d, code=0x%x]",
                getAddress(), constMethod, accessFlags,
                accessFlags().toString(AccessFlags.Kind.METHOD),
                vtableIndex, intrinsicId, code);
    }
}