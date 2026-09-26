
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class ConstMethod extends MetaspaceObj {
    private static final long FINGERPRINT_OFFSET           = findOffsetOrDefault("ConstMethod::_fingerprint",           0L);
    private static final long CONSTANTS_OFFSET             = findOffsetOrDefault("ConstMethod::_constants",             8L);
    private static final long STACKMAP_DATA_OFFSET         = findOffsetOrDefault("ConstMethod::_stackmap_data",        16L);
    private static final long CONST_METHOD_SIZE_OFFSET     = findOffsetOrDefault("ConstMethod::_const_method_size",    24L);
    private static final long FLAGS_OFFSET                 = findOffsetOrDefault("ConstMethod::_flags",                28L);
    private static final long RESULT_TYPE_OFFSET           = findOffsetOrDefault("ConstMethod::_result_type",          30L);
    private static final long CODE_SIZE_OFFSET             = findOffsetOrDefault("ConstMethod::_code_size",            32L);
    private static final long NAME_INDEX_OFFSET            = findOffsetOrDefault("ConstMethod::_name_index",           34L);
    private static final long SIGNATURE_INDEX_OFFSET       = findOffsetOrDefault("ConstMethod::_signature_index",      36L);
    private static final long METHOD_IDNUM_OFFSET          = findOffsetOrDefault("ConstMethod::_method_idnum",         38L);
    private static final long MAX_STACK_OFFSET             = findOffsetOrDefault("ConstMethod::_max_stack",            40L);
    private static final long MAX_LOCALS_OFFSET            = findOffsetOrDefault("ConstMethod::_max_locals",           42L);
    private static final long SIZE_OF_PARAMETERS_OFFSET    = findOffsetOrDefault("ConstMethod::_size_of_parameters",   44L);
    private static final long ORIG_METHOD_IDNUM_OFFSET     = findOffsetOrDefault("ConstMethod::_orig_method_idnum",    46L);

    private static final long CONST_METHOD_HEADER_SIZE     = 48L;

    public static final int HAS_LINENUMBER_TABLE      = 0x0001;
    public static final int HAS_CHECKED_EXCEPTIONS    = 0x0002;
    public static final int HAS_LOCALVARIABLE_TABLE   = 0x0004;
    public static final int HAS_EXCEPTION_TABLE       = 0x0008;
    public static final int HAS_GENERIC_SIGNATURE     = 0x0010;
    public static final int HAS_METHOD_PARAMETERS     = 0x0020;
    public static final int IS_OVERPASS               = 0x0040;
    public static final int HAS_METHOD_ANNOTATIONS    = 0x0080;
    public static final int HAS_PARAMETER_ANNOTATIONS = 0x0100;
    public static final int HAS_TYPE_ANNOTATIONS      = 0x0200;
    public static final int HAS_DEFAULT_ANNOTATIONS   = 0x0400;

    public long fingerprint;
    public long constants;
    public long stackmapData;
    public int  constMethodSize;
    public short flags;
    public byte resultType;
    public short codeSize;
    public short nameIndex;
    public short signatureIndex;
    public short methodIdnum;
    public short maxStack;
    public short maxLocals;
    public short sizeOfParameters;
    public short origMethodIdnum;

    public ConstMethod(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    public ConstMethod(Class<?> clazz) {
        super(clazz);
        if (getAddress() == 0) return;
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        if (base == 0) return;
        fingerprint      = UnsafeUtils.unsafe.getLong(base + FINGERPRINT_OFFSET);
        constants        = UnsafeUtils.unsafe.getLong(base + CONSTANTS_OFFSET);
        stackmapData     = UnsafeUtils.unsafe.getLong(base + STACKMAP_DATA_OFFSET);
        constMethodSize  = UnsafeUtils.unsafe.getInt(base + CONST_METHOD_SIZE_OFFSET);
        flags            = UnsafeUtils.unsafe.getShort(base + FLAGS_OFFSET);
        resultType       = UnsafeUtils.unsafe.getByte(base + RESULT_TYPE_OFFSET);
        codeSize         = UnsafeUtils.unsafe.getShort(base + CODE_SIZE_OFFSET);
        nameIndex        = UnsafeUtils.unsafe.getShort(base + NAME_INDEX_OFFSET);
        signatureIndex   = UnsafeUtils.unsafe.getShort(base + SIGNATURE_INDEX_OFFSET);
        methodIdnum      = UnsafeUtils.unsafe.getShort(base + METHOD_IDNUM_OFFSET);
        maxStack         = UnsafeUtils.unsafe.getShort(base + MAX_STACK_OFFSET);
        maxLocals        = UnsafeUtils.unsafe.getShort(base + MAX_LOCALS_OFFSET);
        sizeOfParameters = UnsafeUtils.unsafe.getShort(base + SIZE_OF_PARAMETERS_OFFSET);
        origMethodIdnum  = UnsafeUtils.unsafe.getShort(base + ORIG_METHOD_IDNUM_OFFSET);
    }

    public boolean hasLinenumberTable()     { return (flags & HAS_LINENUMBER_TABLE) != 0; }
    public boolean hasCheckedExceptions()   { return (flags & HAS_CHECKED_EXCEPTIONS) != 0; }
    public boolean hasLocalvariableTable()  { return (flags & HAS_LOCALVARIABLE_TABLE) != 0; }
    public boolean hasExceptionHandler()    { return (flags & HAS_EXCEPTION_TABLE) != 0; }
    public boolean hasGenericSignature()    { return (flags & HAS_GENERIC_SIGNATURE) != 0; }
    public boolean hasMethodParameters()    { return (flags & HAS_METHOD_PARAMETERS) != 0; }
    public boolean isOverpass()             { return (flags & IS_OVERPASS) != 0; }
    public boolean hasMethodAnnotations()   { return (flags & HAS_METHOD_ANNOTATIONS) != 0; }
    public boolean hasParameterAnnotations(){ return (flags & HAS_PARAMETER_ANNOTATIONS) != 0; }
    public boolean hasTypeAnnotations()     { return (flags & HAS_TYPE_ANNOTATIONS) != 0; }
    public boolean hasDefaultAnnotations()  { return (flags & HAS_DEFAULT_ANNOTATIONS) != 0; }

    public long codeBase() {
        return getAddress() + CONST_METHOD_HEADER_SIZE;
    }

    public byte[] code() {
        int size = codeSize & 0xFFFF;
        if (size == 0) return new byte[0];
        byte[] bytes = new byte[size];
        long base = codeBase();
        for (int i = 0; i < size; i++) {
            bytes[i] = UnsafeUtils.unsafe.getByte(base + i);
        }
        return bytes;
    }

    public void putCode(byte[] newCode) {
        if (newCode == null || newCode.length == 0) return;
        int size = codeSize & 0xFFFF;
        if (newCode.length > size) {
            throw new IllegalArgumentException(
                    "new code length " + newCode.length + " exceeds code_size " + size);
        }
        long base = codeBase();
        for (int i = 0; i < newCode.length; i++) {
            UnsafeUtils.unsafe.putByte(base + i, newCode[i]);
        }
    }

    public void putCodeAndResize(byte[] newCode) {
        if (newCode == null) return;

        short newFlags = (short) (flags
                & ~HAS_LINENUMBER_TABLE
                & ~HAS_LOCALVARIABLE_TABLE
                & ~HAS_GENERIC_SIGNATURE
                & ~HAS_METHOD_PARAMETERS
                & ~HAS_CHECKED_EXCEPTIONS
                & ~HAS_EXCEPTION_TABLE
                & ~HAS_METHOD_ANNOTATIONS
                & ~HAS_PARAMETER_ANNOTATIONS
                & ~HAS_TYPE_ANNOTATIONS
                & ~HAS_DEFAULT_ANNOTATIONS);
        UnsafeUtils.unsafe.putShort(getAddress() + FLAGS_OFFSET, newFlags);
        this.flags = newFlags;

        long base = codeBase();
        for (int i = 0; i < newCode.length; i++) {
            UnsafeUtils.unsafe.putByte(base + i, newCode[i]);
        }
        UnsafeUtils.unsafe.putShort(getAddress() + CODE_SIZE_OFFSET, (short) newCode.length);
        this.codeSize = (short) newCode.length;
        readFields();
    }

    public void putCode(InstanceKlass ik, CodeBuilder builder) {
        byte[] newCode = builder.build(ik);
        putCodeAndResize(newCode);
    }

    public void setMaxStack(int v) {
        UnsafeUtils.unsafe.putShort(getAddress() + MAX_STACK_OFFSET, (short) v);
        this.maxStack = (short) v;
    }

    public void setMaxLocals(int v) {
        UnsafeUtils.unsafe.putShort(getAddress() + MAX_LOCALS_OFFSET, (short) v);
        this.maxLocals = (short) v;
    }
    public void putCodeExtended(byte[] newCode) {
        if (newCode == null) return;
        int maxSpace = constMethodSize - (int) CONST_METHOD_HEADER_SIZE;

        short newFlags = (short) (flags
                & ~HAS_LINENUMBER_TABLE
                & ~HAS_LOCALVARIABLE_TABLE
                & ~HAS_GENERIC_SIGNATURE
                & ~HAS_METHOD_PARAMETERS
                & ~HAS_CHECKED_EXCEPTIONS
                & ~HAS_EXCEPTION_TABLE
                & ~HAS_METHOD_ANNOTATIONS
                & ~HAS_PARAMETER_ANNOTATIONS
                & ~HAS_TYPE_ANNOTATIONS
                & ~HAS_DEFAULT_ANNOTATIONS);
        UnsafeUtils.unsafe.putShort(getAddress() + FLAGS_OFFSET, newFlags);
        this.flags = newFlags;

        long base = codeBase();
        for (int i = 0; i < newCode.length; i++) {
            UnsafeUtils.unsafe.putByte(base + i, newCode[i]);
        }
        UnsafeUtils.unsafe.putShort(getAddress() + CODE_SIZE_OFFSET, (short) newCode.length);
        this.codeSize = (short) newCode.length;
        readFields();
    }
    @Override
    public String toString() {
        return String.format(
                "ConstMethod[address=0x%x, constants=0x%x, flags=0x%04X, codeSize=%d, " +
                        "nameIndex=%d, sigIndex=%d, methodIdnum=%d, maxStack=%d, maxLocals=%d, sizeOfParameters=%d]",
                getAddress(), constants, flags & 0xFFFF, codeSize & 0xFFFF,
                nameIndex & 0xFFFF, signatureIndex & 0xFFFF, methodIdnum & 0xFFFF,
                maxStack & 0xFFFF, maxLocals & 0xFFFF, sizeOfParameters & 0xFFFF);
    }
}