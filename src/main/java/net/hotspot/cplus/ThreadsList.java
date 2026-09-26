package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class ThreadsList {

    private static final long LENGTH_OFF =
            VMStructsHelper.findOffsetOrDefault("ThreadsList::_length", 4L);
    private static final long THREADS_OFF =
            VMStructsHelper.findOffsetOrDefault("ThreadsList::_threads", 16L);

    private final long address;

    public final int length;

    public ThreadsList(long address) {
        this.address = address;
        this.length  = UnsafeUtils.unsafe.getInt(address + LENGTH_OFF);
    }

    public long getAddress() { return address; }

    public JavaThread threadAt(int i) {
        if (i < 0 || i >= length) {
            throw new IndexOutOfBoundsException("i=" + i + ", len=" + length);
        }
        long arrayBase = UnsafeUtils.unsafe.getLong(address + THREADS_OFF);
        long jt = UnsafeUtils.unsafe.getLong(arrayBase + (long) i * 8L);
        return jt == 0 ? null : new JavaThread(jt);
    }

    public JavaThread[] toJavaThreads() {
        JavaThread[] out = new JavaThread[length];
        for (int i = 0; i < length; i++) out[i] = threadAt(i);
        return out;
    }

    @Override
    public String toString() {
        return String.format("ThreadsList[address=0x%x, length=%d]", address, length);
    }
}