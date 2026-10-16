package net.hotspot.cplus.base;

import net.hotspot.cplus.OopDesc;
import net.hotspot.cplus.Utils.UnsafeUtils;

import java.util.Arrays;


public final class ObjectArray implements AutoCloseable {

    private static final int OOP_SHIFT = 3;
    private static final long HOLDER_ELEM_BASE =
            UnsafeUtils.unsafe.arrayBaseOffset(Object[].class);

    private final OopDesc[] entries;

    private boolean freed;

    public ObjectArray(int length) {
        if (length < 0) throw new IllegalArgumentException();
        this.entries = new OopDesc[length];
    }

    public long    length()  { return entries.length; }
    public boolean isFreed() { return freed; }

    public void set(int i, Object obj) {
        checkIndex(i);
        refreshAt(i);
        entries[i] = (obj == null) ? null : OopDesc.of(obj);
    }

    public void putOopAddress(int i, long oopAddr) {
        checkIndex(i);
        refreshAt(i);
        if (oopAddr == 0L) {
            entries[i] = null;
            return;
        }
        Object anchor = readOopAsObject(oopAddr);
        if (anchor == null) {
            entries[i] = null;
            return;
        }
        entries[i] = OopDesc.of(oopAddr, anchor);
    }

    public Object get(int i) {
        checkIndex(i);
        OopDesc od = refreshAt(i);
        if (od == null) return null;
        Object o = readOopAsObject(od.getAddress());
        if (o == null) entries[i] = null;
        return o;
    }

    public OopDesc getOopDesc(int i) {
        checkIndex(i);
        return refreshAt(i);
    }

    public long getOopAddress(int i) {
        checkIndex(i);
        OopDesc od = refreshAt(i);
        return od == null ? 0L : od.getAddress();
    }

    public void setNull(int i) {
        checkIndex(i);
        refreshAt(i);
        entries[i] = null;
    }

    public boolean isNull(int i) {
        checkIndex(i);
        return refreshAt(i) == null;
    }

    public void clear() {
        checkAlive();
        for (int i = 0; i < entries.length; i++) refreshAt(i);
        Arrays.fill(entries, null);
    }

    public void copyFrom(ObjectArray src, int srcIndex, int dstIndex, int count) {
        checkAlive();
        src.checkAlive();
        if (count < 0) throw new IllegalArgumentException();
        if (srcIndex < 0 || srcIndex + count > src.entries.length
                || dstIndex < 0 || dstIndex + count > entries.length)
            throw new ArrayIndexOutOfBoundsException();

        for (int k = 0; k < count; k++) {
            src.refreshAt(srcIndex + k);
            refreshAt(dstIndex + k);
        }
        System.arraycopy(src.entries, srcIndex, entries, dstIndex, count);
    }

    private OopDesc refreshAt(int i) {
        OopDesc od = entries[i];
        if (od == null) return null;

        if (!od.hasQuote()) {
            Object anchor = readOopAsObject(od.getAddress());
            if (anchor == null) {
                entries[i] = null;
                return null;
            }
            entries[i] = od = OopDesc.of(od.getAddress(), anchor);
        }

        od.refreshQuoteIfPossible();

        if (readOopAsObject(od.getAddress()) == null) {
            entries[i] = null;
            return null;
        }

        return od;
    }

    private static Object readOopAsObject(long oopAddr) {
        if (oopAddr == 0L) return null;

        Object[] holder = new Object[1];
        long slot = OopDesc.addressOf(holder) + HOLDER_ELEM_BASE;

        if (OopDesc.USE_COMPRESSED_OOPS) {
            int narrow = (int) ((oopAddr - OopDesc.HEAP_BASE) >>> OOP_SHIFT);
            UnsafeUtils.unsafe.putInt(slot, narrow);
        } else {
            UnsafeUtils.unsafe.putLong(slot, oopAddr);
        }

        Object o = holder[0];
        holder[0] = null;
        return o;
    }

    private void checkAlive() {
        if (freed) throw new IllegalStateException("ObjectArray has been closed");
    }

    private void checkIndex(int i) {
        checkAlive();
        if (i < 0 || i >= entries.length) throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public void close() {
        if (!freed) {
            for (int i = 0; i < entries.length; i++) refreshAt(i);
            Arrays.fill(entries, null);
            freed = true;
        }
    }

    @Override
    public String toString() {
        return "ObjectArray{length=" + entries.length
                + ", compressedOops=" + OopDesc.USE_COMPRESSED_OOPS
                + (freed ? ", freed" : "") + "}";
    }
}