package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public final class ThreadsSMRSupport {

    private ThreadsSMRSupport() {}

    private static final long JAVA_THREAD_LIST_ADDR;

    static {
        JAVA_THREAD_LIST_ADDR = VMStructsHelper.findStaticAddress(
                "ThreadsSMRSupport::_java_thread_list");
    }

    public static long javaThreadList() {
        return UnsafeUtils.unsafe.getLong(JAVA_THREAD_LIST_ADDR);
    }

    public static ThreadsList getJavaThreadList() {
        long p = javaThreadList();
        return p == 0 ? null : new ThreadsList(p);
    }
}