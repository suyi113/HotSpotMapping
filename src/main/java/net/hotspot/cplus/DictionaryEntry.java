package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


public final class DictionaryEntry {

    private static final long MIN_VALID_ADDR = 0x0000_0001_0000L;
    private static final long MAX_VALID_ADDR = 0x0000_7FFF_FFFF_FFFFL;

    private static final long HASH_OFF = findOffset(0L,
            "HashtableEntry<InstanceKlass*, mtClass>::_hash",
            "DictionaryEntry::_hash",
            "BasicHashtableEntry<mtClass>::_hash",
            "HashtableEntry::_hash");

    private static final long NEXT_OFF = findOffset(8L,
            "HashtableEntry<InstanceKlass*, mtClass>::_next",
            "DictionaryEntry::_next",
            "BasicHashtableEntry<mtClass>::_next",
            "HashtableEntry::_next");

    private static final long LITERAL_OFF = findOffset(16L,
            "HashtableEntry<InstanceKlass*, mtClass>::_literal",
            "DictionaryEntry::_literal",
            "DictionaryEntry::_instance_klass",
            "BasicHashtableEntry<mtClass>::_literal",
            "HashtableEntry::_literal");

    private static final long PD_SET_OFF = findOffset(24L,
            "DictionaryEntry::_pd_set");

    private final long address;

    public final int  hash;
    public final long literal;
    public final long next;
    public final long pdSet;

    public DictionaryEntry(long address) {
        if (address == 0) {
            throw new IllegalArgumentException("DictionaryEntry: address == 0");
        }
        if (address < 0 || address > MAX_VALID_ADDR) {
            throw new IllegalArgumentException(String.format(
                    "DictionaryEntry: address outside user space: 0x%x", address));
        }
        if ((address & 7L) != 0) {
            throw new IllegalArgumentException(String.format(
                    "DictionaryEntry: address not 8-byte aligned: 0x%x", address));
        }
        if (address < MIN_VALID_ADDR) {
            throw new IllegalArgumentException(String.format(
                    "DictionaryEntry: address too small: 0x%x", address));
        }

        this.address = address;
        this.hash    = UnsafeUtils.unsafe.getInt (safeAdd(address, HASH_OFF));
        this.literal = UnsafeUtils.unsafe.getLong(safeAdd(address, LITERAL_OFF));
        this.next    = UnsafeUtils.unsafe.getLong(safeAdd(address, NEXT_OFF));
        this.pdSet   = UnsafeUtils.unsafe.getLong(safeAdd(address, PD_SET_OFF));

        checkPointerField("_literal", literal);
        checkPointerField("_next",    next);
        checkPointerField("_pd_set",  pdSet);
    }

    public long getAddress() { return address; }

    public long instanceKlassAddr() { return literal; }

    public InstanceKlass instanceKlass() {
        return literal == 0 ? null : new InstanceKlass(literal);
    }

    public DictionaryEntry nextEntry() {
        return next == 0 ? null : new DictionaryEntry(next);
    }

    public static long nextFieldOffset()    { return NEXT_OFF; }
    public static long literalFieldOffset() { return LITERAL_OFF; }

    private static long safeAdd(long base, long off) {
        long r = base + off;
        if (r < base) {
            throw new ArithmeticException(String.format(
                    "DictionaryEntry: address overflow: 0x%x + %d", base, off));
        }
        return r;
    }

    private static void checkPointerField(String fieldName, long value) {
        if (value == 0) return;
        if (value < MIN_VALID_ADDR || value > MAX_VALID_ADDR || (value & 7L) != 0) {
            throw new IllegalStateException(String.format(
                    "DictionaryEntry: field %s is not a valid pointer: 0x%x",
                    fieldName, value));
        }
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
    public static boolean unlinkFromDictionary(Class<?> target) {
        if (target == null) {
            throw new IllegalArgumentException("unlinkFromDictionary: target == null");
        }

        InstanceKlass ik = new InstanceKlass(target);
        long targetKlass = ik.getAddress();

        long cld = ik.classLoaderData();
        if (cld == 0) return false;

        long dictOff  = VMStructsHelper.findOffsetOrDefault(
                "ClassLoaderData::_dictionary", -1L);
        long dictAddr = 0L;
        if (dictOff >= 0) {
            dictAddr = UnsafeUtils.unsafe.getLong(cld + dictOff);
        }

        if (!isPlausibleDictionary(dictAddr)) {
            long probed = probeDictionaryOffset(cld);
            if (probed < 0) return false;
            dictOff  = probed;
            dictAddr = UnsafeUtils.unsafe.getLong(cld + dictOff);
        }

        if (dictAddr == 0) return false;

        int  tableSize = UnsafeUtils.unsafe.getInt (dictAddr);
        long buckets   = UnsafeUtils.unsafe.getLong(dictAddr + 8L);

        if (tableSize <= 0 || tableSize > (1 << 20)) return false;
        if (buckets == 0 || (buckets & 7L) != 0 || buckets < 0x10000L) return false;

        final long NEXT_OFF    = DictionaryEntry.nextFieldOffset();
        final long LITERAL_OFF = DictionaryEntry.literalFieldOffset();

        for (int i = 0; i < tableSize; i++) {
            long bucketAddr = buckets + (long) i * 8L;
            long entry      = UnsafeUtils.unsafe.getLong(bucketAddr);
            if (entry == 0) continue;

            long prev       = 0L;
            int  chainDepth = 0;

            while (entry != 0) {
                long literal = UnsafeUtils.unsafe.getLong(entry + LITERAL_OFF);
                long next    = UnsafeUtils.unsafe.getLong(entry + NEXT_OFF);

                if (literal == targetKlass) {
                    if (prev == 0) {
                        UnsafeUtils.unsafe.putLong(bucketAddr, next);
                    } else {
                        UnsafeUtils.unsafe.putLong(prev + NEXT_OFF, next);
                    }
                    return true;
                }

                if (++chainDepth > 100_000) {
                    break;
                }

                prev  = entry;
                entry = next;
            }
        }
        return false;
    }

    public static long probeDictionaryOffset(long cld) {
        if (cld < MIN_VALID_ADDR || (cld & 7L) != 0) return -1L;

        long best = -1L;
        for (int i = 0; i < 32; i++) {
            long cand;
            try {
                cand = UnsafeUtils.unsafe.getLong(cld + (long) i * 8L);
            } catch (Throwable t) {
                break;
            }
            if (cand < MIN_VALID_ADDR || (cand & 7L) != 0) continue;
            if (isPlausibleDictionary(cand)) {
                best = i * 8L;
                break;
            }
        }
        return best;
    }

    public static boolean isPlausibleDictionary(long addr) {
        if (addr == 0) return false;
        if (addr < MIN_VALID_ADDR || (addr & 7L) != 0) return false;
        if (addr > MAX_VALID_ADDR) return false;

        try {
            int  tableSize = UnsafeUtils.unsafe.getInt (addr);
            long buckets   = UnsafeUtils.unsafe.getLong(addr + 8L);
            int  numEnt    = UnsafeUtils.unsafe.getInt (addr + 16L);
            long loader    = UnsafeUtils.unsafe.getLong(addr + 24L);

            if (tableSize <= 0 || tableSize > (1 << 20)) return false;
            if (buckets == 0)                            return false;
            if ((buckets & 7L) != 0)                     return false;
            if (buckets < MIN_VALID_ADDR)                return false;
            if (numEnt < 0 || numEnt > tableSize * 4)    return false;
            if (loader != 0 && (loader < MIN_VALID_ADDR || (loader & 7L) != 0)) return false;

            long b0 = UnsafeUtils.unsafe.getLong(buckets);
            if (b0 != 0 && ((b0 & 7L) != 0 || b0 < MIN_VALID_ADDR)) return false;

            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    @Override
    public String toString() {
        return String.format(
                "DictionaryEntry[address=0x%x, hash=0x%08x, klass=0x%x, next=0x%x, pdSet=0x%x]",
                address, hash, literal, next, pdSet);
    }
}