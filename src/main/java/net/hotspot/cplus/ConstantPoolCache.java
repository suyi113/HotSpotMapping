package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class ConstantPoolCache extends MetaspaceObj{

    private static final long LENGTH_OFFSET              = findOffsetOrDefault("ConstantPool::_length",               0L);
    private static final long CONSTANT_POOL_OFFSET       = findOffsetOrDefault("ConstantPool::_constant_pool",        8L);
    private static final long RESOLVED_REFERENCES_OFFSET = findOffsetOrDefault("ConstantPool::_resolved_references", 16L);
    private static final long REFERENCE_MAP_OFFSET       = findOffsetOrDefault("ConstantPool::_reference_map",       24L);

    public  static final long HEADER_SIZE = 32L;

    public static final long ENTRY_INDICES_OFFSET = findOffsetOrDefault("ConstantPoolCacheEntry::_indices", 0L);
    public static final long ENTRY_F1_OFFSET      = findOffsetOrDefault("ConstantPoolCacheEntry::_f1",      8L);
    public static final long ENTRY_F2_OFFSET      = findOffsetOrDefault("ConstantPoolCacheEntry::_f2",      16L);
    public static final long ENTRY_FLAGS_OFFSET   = findOffsetOrDefault("ConstantPoolCacheEntry::_flags",   24L);
    public static final long ENTRY_SIZE           = 32L;


    public int  length;
    public long constantPool;
    public long resolvedReferences;
    public long referenceMap;

    public ConstantPoolCache(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    public ConstantPoolCache(Class<?> clazz) {
        super(clazz);
        if (getAddress() == 0) return;
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        if (base == 0) return;
        length             = UnsafeUtils.unsafe.getInt(base + LENGTH_OFFSET);
        constantPool       = UnsafeUtils.unsafe.getLong(base + CONSTANT_POOL_OFFSET);
        resolvedReferences = UnsafeUtils.unsafe.getLong(base + RESOLVED_REFERENCES_OFFSET);
        referenceMap       = UnsafeUtils.unsafe.getLong(base + REFERENCE_MAP_OFFSET);
    }

    public long getAddress() { return address; }

    public long entryAddr(int i) {
        if (i < 0 || i > length) {
            throw new IndexOutOfBoundsException("index: " + i + ", length: " + length);
        }
        return address + HEADER_SIZE + i * ENTRY_SIZE;
    }

    public int entryIndices(int i) {
        return (int) UnsafeUtils.unsafe.getLong(entryAddr(i) + ENTRY_INDICES_OFFSET);
    }

    public long entryF1(int i) {
        return UnsafeUtils.unsafe.getLong(entryAddr(i) + ENTRY_F1_OFFSET);
    }

    public long entryF2(int i) {
        return UnsafeUtils.unsafe.getLong(entryAddr(i) + ENTRY_F2_OFFSET);
    }

    public int entryFlags(int i) {
        return (int) UnsafeUtils.unsafe.getLong(entryAddr(i) + ENTRY_FLAGS_OFFSET);
    }

    public int entryConstantPoolIndex(int i) {
        return entryIndices(i) & 0xFFFF;
    }

    public int entryBytecode1(int i) {
        return (entryIndices(i) >>> 16) & 0xFF;
    }

    public int entryBytecode2(int i) {
        return (entryIndices(i) >>> 24) & 0xFF;
    }
    public void dumpEntry(int i) {
        long addr = entryAddr(i);
        System.out.printf("[CPC] entry[%d] @ 0x%x%n", i, addr);
        for (int k = 0; k < 4; k++) {
            long v = UnsafeUtils.unsafe.getLong(addr + k * 8L);
            System.out.printf("[CPC]   +%d: 0x%016x  lo32=0x%08x  hi32=0x%08x%n",
                    k * 8, v, (int) v, (int) (v >>> 32));
        }
    }
    public void findBytes(int target) {
        for (int i = 0; i < length; i++) {
            long addr = entryAddr(i);
            for (int off = 0; off < 32; off += 2) {
                int v2 = UnsafeUtils.unsafe.getShort(addr + off) & 0xFFFF;
                if (v2 == target) {
                    System.out.printf("[SCAN] u2 0x%04x found: entry[%d]+%d%n", target, i, off);
                }
            }
            for (int off = 0; off < 32; off += 4) {
                int v4 = UnsafeUtils.unsafe.getInt(addr + off);
                if (v4 == target) {
                    System.out.printf("[SCAN] u4 0x%08x found: entry[%d]+%d%n", target, i, off);
                }
                long v8 = UnsafeUtils.unsafe.getLong(addr + off);
                if (v8 == target) {
                    System.out.printf("[SCAN] u8 0x%016x found: entry[%d]+%d%n", target, i, off);
                }
            }
        }
    }
    @Override
    public String toString() {
        return String.format(
                "ConstantPoolCache[address=0x%x, length=%d, constantPool=0x%x, " +
                        "resolvedReferences=0x%x, referenceMap=0x%x]",
                getAddress(), length, constantPool, resolvedReferences, referenceMap);
    }
}
