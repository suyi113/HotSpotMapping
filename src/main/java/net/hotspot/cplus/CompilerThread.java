package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CompilerThread extends JavaThread {

    private static final long ENV_OFF =
            VMStructsHelper.findOffsetOrDefault("CompilerThread::_env", -1L);

    public CompilerThread(long address) {
        super(address);
    }

    public long env() {
        if (ENV_OFF < 0) return 0;
        return UnsafeUtils.unsafe.getLong(getAddress() + ENV_OFF);
    }

    public ciEnv getEnv() {
        long e = env();
        return e == 0 ? null : new ciEnv(e);
    }

    public CompileTask getCurrentTask() {
        ciEnv env = getEnv();
        return env == null ? null : env.getTask();
    }
}