
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class CompiledMethod extends CodeBlob {

    public  static final long MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET = findOffsetOrDefault("CompiledMethod::_mark_for_deoptimization_status", 104L);
    private static final long BITFIELDS_OFFSET                      = findOffsetOrDefault("CompiledMethod::_bitfields",                       108L);
    private static final long METHOD_OFFSET                         = findOffsetOrDefault("CompiledMethod::_method",                          112L);
    private static final long SCOPES_DATA_BEGIN_OFFSET              = findOffsetOrDefault("CompiledMethod::_scopes_data_begin",               120L);
    private static final long DEOPT_HANDLER_BEGIN_OFFSET            = findOffsetOrDefault("CompiledMethod::_deopt_handler_begin",             128L);
    private static final long DEOPT_MH_HANDLER_BEGIN_OFFSET         = findOffsetOrDefault("CompiledMethod::_deopt_mh_handler_begin",          136L);
    private static final long PC_DESC_CACHE_OFFSET                  = findOffsetOrDefault("CompiledMethod::_pc_desc_cache",                   144L);
    private static final long EXCEPTION_CACHE_OFFSET                = findOffsetOrDefault("CompiledMethod::_exception_cache",                 176L);
    private static final long GC_DATA_OFFSET                        = findOffsetOrDefault("CompiledMethod::_gc_data",                         184L);

    public static final int PC_DESC_CACHE_SIZE = 4;

    public static final int NOT_MARKED          = 0;
    public static final int DEOPTIMIZE          = 1;
    public static final int DEOPTIMIZE_NOUPDATE = 2;

    public static final int HAS_UNSAFE_ACCESS_BIT         = 1 << 0;
    public static final int HAS_METHOD_HANDLE_INVOKES_BIT = 1 << 1;
    public static final int HAS_WIDE_VECTORS_BIT          = 1 << 2;

    public int    markForDeoptimizationStatus;
    public int    bitfields;
    public long   method;
    public long   scopesDataBegin;
    public long   deoptHandlerBegin;
    public long   deoptMhHandlerBegin;
    public long[] pcDescCache = new long[PC_DESC_CACHE_SIZE];
    public long   exceptionCache;
    public long   gcData;

    public CompiledMethod(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        markForDeoptimizationStatus = UnsafeUtils.unsafe.getInt(base + MARK_FOR_DEOPTIMIZATION_STATUS_OFFSET);
        bitfields                   = UnsafeUtils.unsafe.getInt(base + BITFIELDS_OFFSET);
        method                      = UnsafeUtils.unsafe.getLong(base + METHOD_OFFSET);
        scopesDataBegin             = UnsafeUtils.unsafe.getLong(base + SCOPES_DATA_BEGIN_OFFSET);
        deoptHandlerBegin           = UnsafeUtils.unsafe.getLong(base + DEOPT_HANDLER_BEGIN_OFFSET);
        deoptMhHandlerBegin         = UnsafeUtils.unsafe.getLong(base + DEOPT_MH_HANDLER_BEGIN_OFFSET);
        for (int i = 0; i < PC_DESC_CACHE_SIZE; i++) {
            pcDescCache[i] = UnsafeUtils.unsafe.getLong(base + PC_DESC_CACHE_OFFSET + i * 8L);
        }
        exceptionCache = UnsafeUtils.unsafe.getLong(base + EXCEPTION_CACHE_OFFSET);
        gcData         = UnsafeUtils.unsafe.getLong(base + GC_DATA_OFFSET);
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
        if (method == 0) return null;
        return new Method(method);
    }

    public boolean isNativeMethod() {
        Method m = getMethod();
        return m != null && m.accessFlags().isNative();
    }

    public boolean isJavaMethod() {
        Method m = getMethod();
        return m != null && !m.accessFlags().isNative();
    }

    public boolean hasUnsafeAccess()        { return (bitfields & HAS_UNSAFE_ACCESS_BIT)         != 0; }
    public boolean hasMethodHandleInvokes() { return (bitfields & HAS_METHOD_HANDLE_INVOKES_BIT) != 0; }
    public boolean hasWideVectors()         { return (bitfields & HAS_WIDE_VECTORS_BIT)          != 0; }

    public boolean isMarkedForDeoptimization() {
        return markForDeoptimizationStatus != NOT_MARKED;
    }

    public boolean updateRecompileCounts() {
        return markForDeoptimizationStatus != DEOPTIMIZE_NOUPDATE;
    }

    public boolean canBeDeoptimized() {
        return isJavaMethod();
    }

    public long scopesDataBegin()     { return scopesDataBegin; }
    public long deoptHandlerBegin()   { return deoptHandlerBegin; }
    public long deoptMhHandlerBegin() { return deoptMhHandlerBegin; }
    public long exceptionCache()      { return exceptionCache; }
    public long gcData()              { return gcData; }

    public long pcDescAt(int index) {
        if (index < 0 || index >= PC_DESC_CACHE_SIZE) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        return pcDescCache[index];
    }

    public long lastPcDesc() {
        return pcDescCache[0];
    }

    @Override
    public String toString() {
        return String.format(
                "CompiledMethod[address=0x%x, name=0x%x, method=0x%x, scopesDataBegin=0x%x, " +
                        "deoptHandler=0x%x, deoptMhHandler=0x%x, exceptionCache=0x%x, " +
                        "markStatus=%d, bitfields=0x%x, frameSize=%d, frameCompleteOffset=%d]",
                getAddress(), name, method, scopesDataBegin,
                deoptHandlerBegin, deoptMhHandlerBegin, exceptionCache,
                markForDeoptimizationStatus, bitfields, frameSize, frameCompleteOffset);
    }
}