package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class ConstantPool extends Metadata {

    private static final long TAGS_OFFSET                    = findOffsetOrDefault("ConstantPool::_tags",                    8L);
    private static final long CACHE_OFFSET                   = findOffsetOrDefault("ConstantPool::_cache",                   16L);
    private static final long POOL_HOLDER_OFFSET             = findOffsetOrDefault("ConstantPool::_pool_holder",             24L);
    private static final long OPERANDS_OFFSET                = findOffsetOrDefault("ConstantPool::_operands",                32L);
    private static final long RESOLVED_KLASSES_OFFSET        = findOffsetOrDefault("ConstantPool::_resolved_klasses",        40L);
    private static final long MAJOR_VERSION_OFFSET           = findOffsetOrDefault("ConstantPool::_major_version",           48L);
    private static final long MINOR_VERSION_OFFSET           = findOffsetOrDefault("ConstantPool::_minor_version",           50L);
    private static final long GENERIC_SIGNATURE_INDEX_OFFSET = findOffsetOrDefault("ConstantPool::_generic_signature_index", 52L);
    private static final long SOURCE_FILE_NAME_INDEX_OFFSET  = findOffsetOrDefault("ConstantPool::_source_file_name_index",  54L);
    private static final long FLAGS_OFFSET                   = findOffsetOrDefault("ConstantPool::_flags",                   56L);
    private static final long LENGTH_OFFSET                  = findOffsetOrDefault("ConstantPool::_length",                  60L);
    private static final long SAVED_OFFSET                   = findOffsetOrDefault("ConstantPool::_saved",                   64L);

    private static final long BASE_OFFSET                    = 72L;

    public static final int HAS_PRERESOLUTION    = 1;
    public static final int ON_STACK             = 2;
    public static final int IS_SHARED            = 4;
    public static final int HAS_DYNAMIC_CONSTANT = 8;

    public long tags;
    public long cache;
    public long poolHolder;
    public long operands;
    public long resolvedKlasses;
    public short majorVersion;
    public short minorVersion;
    public short genericSignatureIndex;
    public short sourceFileNameIndex;
    public short flags;
    public int  length;
    public int  saved;

    public ConstantPool(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    public ConstantPool(Class<?> clazz) {
        super(new InstanceKlass(clazz).constants);
        if (getAddress() == 0) return;
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        if (base == 0) return;
        tags                  = UnsafeUtils.unsafe.getLong(base + TAGS_OFFSET);
        cache                 = UnsafeUtils.unsafe.getLong(base + CACHE_OFFSET);
        poolHolder            = UnsafeUtils.unsafe.getLong(base + POOL_HOLDER_OFFSET);
        operands              = UnsafeUtils.unsafe.getLong(base + OPERANDS_OFFSET);
        resolvedKlasses       = UnsafeUtils.unsafe.getLong(base + RESOLVED_KLASSES_OFFSET);
        majorVersion          = UnsafeUtils.unsafe.getShort(base + MAJOR_VERSION_OFFSET);
        minorVersion          = UnsafeUtils.unsafe.getShort(base + MINOR_VERSION_OFFSET);
        genericSignatureIndex = UnsafeUtils.unsafe.getShort(base + GENERIC_SIGNATURE_INDEX_OFFSET);
        sourceFileNameIndex   = UnsafeUtils.unsafe.getShort(base + SOURCE_FILE_NAME_INDEX_OFFSET);
        flags                 = UnsafeUtils.unsafe.getShort(base + FLAGS_OFFSET);
        length                = UnsafeUtils.unsafe.getInt(base + LENGTH_OFFSET);
        saved                 = UnsafeUtils.unsafe.getInt(base + SAVED_OFFSET);
    }

    public boolean hasPreresolution()    { return (flags & HAS_PRERESOLUTION) != 0; }
    public boolean onStack()             { return (flags & ON_STACK) != 0; }
    public boolean isShared()            { return (flags & IS_SHARED) != 0; }
    public boolean hasDynamicConstant()  { return (flags & HAS_DYNAMIC_CONSTANT) != 0; }

    public boolean isWithinBounds(int index) {
        return index >= 0 && index < length;
    }

    public long baseAddress() {
        return getAddress() + BASE_OFFSET;
    }

    public long slotAt(int which) {
        if (!isWithinBounds(which)) {
            throw new IndexOutOfBoundsException("index: " + which + ", length: " + length);
        }
        return UnsafeUtils.unsafe.getLong(baseAddress() + which * 8L);
    }

    public long symbolAt(int which) {
        return slotAt(which) & ~1L;
    }

    public int intAt(int which) {
        if (!isWithinBounds(which)) {
            throw new IndexOutOfBoundsException("index: " + which + ", length: " + length);
        }
        return UnsafeUtils.unsafe.getInt(baseAddress() + which * 8L);
    }

    public ConstantPoolCache getCache() {
        if (cache == 0) return null;
        return new ConstantPoolCache(cache);
    }

    @Override
    public String toString() {
        return String.format(
                "ConstantPool[address=0x%x, poolHolder=0x%x, length=%d, major=%d, minor=%d, " +
                        "genericSigIndex=%d, sourceFileIndex=%d, flags=0x%04X]",
                getAddress(), poolHolder, length,
                majorVersion & 0xFFFF, minorVersion & 0xFFFF,
                genericSignatureIndex & 0xFFFF, sourceFileNameIndex & 0xFFFF, flags & 0xFFFF);
    }
}