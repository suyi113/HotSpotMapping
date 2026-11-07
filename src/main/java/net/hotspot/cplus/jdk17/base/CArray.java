package net.hotspot.cplus.jdk17.base;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public abstract class CArray implements AutoCloseable {
    protected static final Unsafe UNSAFE;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected long address;
    protected final long length;
    protected final int elementSize;
    private boolean freed;

    protected CArray(long length, int elementSize) {
        if (length < 0) throw new IllegalArgumentException();
        if (elementSize <= 0) throw new IllegalArgumentException();
        this.length = length;
        this.elementSize = elementSize;
        long bytes = Math.multiplyExact(length, (long) elementSize);
        this.address = UNSAFE.allocateMemory(Math.max(bytes, 1L));
        UNSAFE.setMemory(this.address, bytes, (byte) 0);
    }

    protected final void checkAlive() {
        if (freed) throw new IllegalStateException();
    }

    protected final void checkIndex(long index) {
        checkAlive();
        if (index < 0 || index >= length) throw new ArrayIndexOutOfBoundsException();
    }

    protected final long elementAddress(long index) {
        checkIndex(index);
        return address + index * elementSize;
    }

    protected final byte getByte(long i) { return UNSAFE.getByte(elementAddress(i)); }
    protected final short getShort(long i) { return UNSAFE.getShort(elementAddress(i)); }
    protected final char getChar(long i) { return UNSAFE.getChar(elementAddress(i)); }
    protected final int getInt(long i) { return UNSAFE.getInt(elementAddress(i)); }
    protected final long getLong(long i) { return UNSAFE.getLong(elementAddress(i)); }
    protected final float getFloat(long i) { return UNSAFE.getFloat(elementAddress(i)); }
    protected final double getDouble(long i) { return UNSAFE.getDouble(elementAddress(i)); }
    protected final long getAddress(long i) { return UNSAFE.getAddress(elementAddress(i)); }

    protected final void putByte(long i, byte v) { UNSAFE.putByte(elementAddress(i), v); }
    protected final void putShort(long i, short v) { UNSAFE.putShort(elementAddress(i), v); }
    protected final void putChar(long i, char v) { UNSAFE.putChar(elementAddress(i), v); }
    protected final void putInt(long i, int v) { UNSAFE.putInt(elementAddress(i), v); }
    protected final void putLong(long i, long v) { UNSAFE.putLong(elementAddress(i), v); }
    protected final void putFloat(long i, float v) { UNSAFE.putFloat(elementAddress(i), v); }
    protected final void putDouble(long i, double v) { UNSAFE.putDouble(elementAddress(i), v); }
    protected final void putAddress(long i, long v) { UNSAFE.putAddress(elementAddress(i), v); }

    public final long length() { return length; }
    public final int elementSize() { return elementSize; }
    public final long sizeInBytes() { return length * elementSize; }
    public final boolean isFreed() { return freed; }

    public final void clear() {
        checkAlive();
        UNSAFE.setMemory(address, sizeInBytes(), (byte) 0);
    }

    public final void fill(byte value) {
        checkAlive();
        UNSAFE.setMemory(address, sizeInBytes(), value);
    }

    public final void copyFrom(CArray src, long srcIndex, long dstIndex, long count) {
        checkAlive();
        src.checkAlive();
        if (src.elementSize != this.elementSize) throw new IllegalArgumentException();
        if (count < 0) throw new IllegalArgumentException();
        if (srcIndex < 0 || srcIndex + count > src.length || dstIndex < 0 || dstIndex + count > this.length)
            throw new ArrayIndexOutOfBoundsException();
        UNSAFE.copyMemory(
                src.address + srcIndex * src.elementSize,
                this.address + dstIndex * this.elementSize,
                count * this.elementSize);
    }

    @Override
    public final void close() {
        if (!freed) {
            UNSAFE.freeMemory(address);
            address = 0L;
            freed = true;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{length=" + length + ", elementSize=" + elementSize
                + ", bytes=" + sizeInBytes() + (freed ? ", freed" : ", @" + Long.toHexString(address)) + "}";
    }
}
