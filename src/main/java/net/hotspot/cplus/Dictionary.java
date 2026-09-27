package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


public class Dictionary {

    private static final long TABLE_SIZE_OFF = findOffset(0L,
            "BasicHashtable::_table_size",
            "Dictionary::_table_size",
            "Hashtable::_table_size",
            "Dictionary::_length",
            "BasicHashtable::_length");

    private static final long BUCKETS_OFF = findOffset(8L,
            "BasicHashtable::_buckets",
            "Dictionary::_buckets",
            "Hashtable::_buckets");

    private static final long NUMBER_OF_ENTRIES_OFF = findOffset(16L,
            "BasicHashtable::_number_of_entries",
            "Dictionary::_number_of_entries",
            "Hashtable::_number_of_entries");

    private static final long RESIZABLE_OFF = findOffset(20L,
            "Dictionary::_resizable");

    private static final long NEEDS_RESIZING_OFF = findOffset(21L,
            "Dictionary::_needs_resizing");

    private static final long LOADER_DATA_OFF = findOffset(24L,
            "Dictionary::_loader_data");

    private final long address;

    public final int     tableSize;
    public final long    buckets;
    public final int     numberOfEntries;
    public final boolean resizable;
    public final boolean needsResizing;
    public final long    loaderData;

    public Dictionary(long address) {
        if (address == 0) throw new IllegalArgumentException("address is null");
        this.address          = address;
        this.tableSize        = UnsafeUtils.unsafe.getInt (address + TABLE_SIZE_OFF);
        this.buckets          = UnsafeUtils.unsafe.getLong(address + BUCKETS_OFF);
        this.numberOfEntries  = UnsafeUtils.unsafe.getInt (address + NUMBER_OF_ENTRIES_OFF);
        this.resizable        = UnsafeUtils.unsafe.getByte(address + RESIZABLE_OFF)     != 0;
        this.needsResizing    = UnsafeUtils.unsafe.getByte(address + NEEDS_RESIZING_OFF) != 0;
        this.loaderData       = UnsafeUtils.unsafe.getLong(address + LOADER_DATA_OFF);
    }

    public long getAddress() { return address; }

    public DictionaryEntry bucket(int i) {
        if (i < 0 || i >= tableSize)
            throw new IndexOutOfBoundsException("bucket " + i + " / " + tableSize);
        long e = UnsafeUtils.unsafe.getLong(buckets + (long) i * 8L);
        return e == 0 ? null : new DictionaryEntry(e);
    }

    public long bucketAddr(int i) {
        if (i < 0 || i >= tableSize)
            throw new IndexOutOfBoundsException("bucket " + i + " / " + tableSize);
        return buckets + (long) i * 8L;
    }

    public ClassLoaderData loader() {
        return loaderData == 0 ? null : new ClassLoaderData(loaderData);
    }

    public static void dumpOffsets() {
        System.out.printf(
                "Dictionary offsets: _table_size=%d _buckets=%d _number_of_entries=%d " +
                        "_resizable=%d _needs_resizing=%d _loader_data=%d%n",
                TABLE_SIZE_OFF, BUCKETS_OFF, NUMBER_OF_ENTRIES_OFF,
                RESIZABLE_OFF, NEEDS_RESIZING_OFF, LOADER_DATA_OFF);
    }

    private static long findOffset(long fallback, String... names) {
        for (String n : names) {
            try {
                long v = VMStructsHelper.findOffset(n);
                if (v >= 0) return v;
            } catch (Throwable ignored) { }
        }
        return fallback;
    }

    @Override
    public String toString() {
        return String.format(
                "Dictionary[address=0x%x, tableSize=%d, numberOfEntries=%d, " +
                        "resizable=%b, needsResizing=%b, loaderData=0x%x, buckets=0x%x]",
                address, tableSize, numberOfEntries,
                resizable, needsResizing, loaderData, buckets);
    }

}