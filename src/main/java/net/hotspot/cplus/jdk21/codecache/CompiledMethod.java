package net.hotspot.cplus.jdk21.codecache;

import net.hotspot.cplus.jdk21.core.VMStructsHelper;
import net.hotspot.cplus.jdk21.klass.Method;

import net.hotspot.cplus.jdk21.core.UnsafeUtils;

import static net.hotspot.cplus.jdk21.core.VMStructsHelper.findOffsetOrDefault;

public class CompiledMethod extends CodeBlob {

    private static final long METHOD_OFFSET                  = findOffsetOrDefault("CompiledMethod::_method",                  120L);
    private static final long SCOPES_DATA_BEGIN_OFFSET       = findOffsetOrDefault("CompiledMethod::_scopes_data_begin",       128L);
    private static final long DEOPT_HANDLER_BEGIN_OFFSET     = findOffsetOrDefault("CompiledMethod::_deopt_handler_begin",     136L);
    private static final long DEOPT_MH_HANDLER_BEGIN_OFFSET  = findOffsetOrDefault("CompiledMethod::_deopt_mh_handler_begin",  144L);
    private static final long EXCEPTION_CACHE_OFFSET         = findOffsetOrDefault("CompiledMethod::_exception_cache",         184L);

    public long method;
    public long scopesDataBegin;
    public long deoptHandlerBegin;
    public long deoptMhHandlerBegin;
    public long exceptionCache;

    public CompiledMethod(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        method                 = UnsafeUtils.unsafe.getLong(base + METHOD_OFFSET);
        scopesDataBegin        = UnsafeUtils.unsafe.getLong(base + SCOPES_DATA_BEGIN_OFFSET);
        deoptHandlerBegin      = UnsafeUtils.unsafe.getLong(base + DEOPT_HANDLER_BEGIN_OFFSET);
        deoptMhHandlerBegin    = UnsafeUtils.unsafe.getLong(base + DEOPT_MH_HANDLER_BEGIN_OFFSET);
        exceptionCache         = UnsafeUtils.unsafe.getLong(base + EXCEPTION_CACHE_OFFSET);
    }

    @Override
    public boolean isNmethod() {
        return false;
    }

    @Override
    public boolean isRuntimeStub() {
        return false;
    }

    public boolean isCompiled() {
        return true;
    }

    public Method getMethod() {
        return method == 0 ? null : new Method(method);
    }

    public String methodName() {
        Method m = getMethod();
        return m == null ? null : m.name();
    }

    public String methodSignature() {
        Method m = getMethod();
        return m == null ? null : m.signature();
    }

    public boolean isNativeMethod() {
        Method m = getMethod();
        return m != null && m.isNative();
    }

    public boolean isJavaMethod() {
        Method m = getMethod();
        return m != null && !m.isNative();
    }

    public boolean canBeDeoptimized() {
        return isJavaMethod();
    }

    public long scopesDataBegin()     { return scopesDataBegin; }
    public long deoptHandlerBegin()   { return deoptHandlerBegin; }
    public long deoptMhHandlerBegin() { return deoptMhHandlerBegin; }
    public long exceptionCache()      { return exceptionCache; }

    @Override
    public String toString() {
        Method m = getMethod();
        return String.format(
                "CompiledMethod[address=0x%x, name=%s, method=0x%x, scopesDataBegin=0x%x, "
                        + "deoptHandler=0x%x, deoptMhHandler=0x%x, exceptionCache=0x%x, "
                        + "frameSize=%d, frameCompleteOffset=%d]",
                getAddress(), m == null ? "?" : m.name() + m.signature(),
                method, scopesDataBegin, deoptHandlerBegin, deoptMhHandlerBegin,
                exceptionCache, frameSize, frameCompleteOffset);
    }
}
