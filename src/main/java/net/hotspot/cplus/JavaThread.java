package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class JavaThread {

    private static final long THREAD_OBJ_OFF =
            VMStructsHelper.findOffsetOrDefault("JavaThread::_threadObj", 0L);
    private static final long THREAD_STATE_OFF =
            VMStructsHelper.findOffsetOrDefault("JavaThread::_thread_state", 0L);
    private static final long OSTHREAD_OFF =
            VMStructsHelper.findOffsetOrDefault("JavaThread::_osthread", 0L);

    private final long address;

    public final long threadObjHandle;
    public final int  threadState;
    public final long osThread;

    public JavaThread(long address) {
        this.address = address;
        this.threadObjHandle = THREAD_OBJ_OFF == 0 ? 0
                : UnsafeUtils.unsafe.getLong(address + THREAD_OBJ_OFF);
        this.threadState = THREAD_STATE_OFF == 0 ? -1
                : UnsafeUtils.unsafe.getInt(address + THREAD_STATE_OFF);
        this.osThread = OSTHREAD_OFF == 0 ? 0
                : UnsafeUtils.unsafe.getLong(address + OSTHREAD_OFF);
    }

    public long getAddress() { return address; }

    public Object getThreadObject() {
        if (threadObjHandle == 0) return null;
        long oop = UnsafeUtils.unsafe.getLong(threadObjHandle);
        if (oop == 0) return null;
        return UnsafeUtils.oopToJavaObject(oop);
    }

    @Override
    public String toString() {
        return String.format(
                "JavaThread[address=0x%x, threadObj=0x%x, state=%d, osThread=0x%x]",
                address, threadObjHandle, threadState, osThread);
    }
}