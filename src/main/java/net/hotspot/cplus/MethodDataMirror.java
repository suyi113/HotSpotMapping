package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

import java.util.ArrayList;
import java.util.List;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

/**
 * MethodData 的 Java 映射。
 * 字段偏移优先从 VMStructs 拿，拿不到时回退到源码推算的常量。
 * 条件编译字段（RTM/JVMCI）不存在时偏移量为 -1，对应字段值为 -1 或 0。
 */
public class MethodDataMirror {

    // ---------- MethodData 字段偏移 ----------
    private static final long METHOD_OFFSET            = findOffsetOrDefault("MethodData::_method",                    8L);
    private static final long SIZE_OFFSET              = findOffsetOrDefault("MethodData::_size",                      16L);
    private static final long HINT_DI_OFFSET           = findOffsetOrDefault("MethodData::_hint_di",                   20L);
    private static final long EXTRA_DATA_LOCK_OFFSET   = findOffsetOrDefault("MethodData::_extra_data_lock",           24L);
    private static final long COMPILER_COUNTERS_OFFSET = findOffsetOrDefault("MethodData::_compiler_counters",         128L);
    private static final long EFLAGS_OFFSET            = findOffsetOrDefault("MethodData::_eflags",                    208L);
    private static final long ARG_LOCAL_OFFSET         = findOffsetOrDefault("MethodData::_arg_local",                 216L);
    private static final long ARG_STACK_OFFSET         = findOffsetOrDefault("MethodData::_arg_stack",                 224L);
    private static final long ARG_RETURNED_OFFSET      = findOffsetOrDefault("MethodData::_arg_returned",              232L);
    private static final long CREATION_MILEAGE_OFFSET  = findOffsetOrDefault("MethodData::_creation_mileage",          240L);
    private static final long INVOCATION_CNT_OFFSET    = findOffsetOrDefault("MethodData::_invocation_counter",        244L);
    private static final long BACKEDGE_CNT_OFFSET      = findOffsetOrDefault("MethodData::_backedge_counter",          248L);
    private static final long INVOCATION_START_OFFSET  = findOffsetOrDefault("MethodData::_invocation_counter_start",  252L);
    private static final long BACKEDGE_START_OFFSET    = findOffsetOrDefault("MethodData::_backedge_counter_start",    256L);
    private static final long TENURE_TRAPS_OFFSET      = findOffsetOrDefault("MethodData::_tenure_traps",              260L);
    private static final long INVOKE_MASK_OFFSET       = findOffsetOrDefault("MethodData::_invoke_mask",               264L);
    private static final long BACKEDGE_MASK_OFFSET     = findOffsetOrDefault("MethodData::_backedge_mask",             268L);
    private static final long RTM_STATE_OFFSET         = findOffsetOrDefault("MethodData::_rtm_state",                 -1L);
    private static final long NUM_LOOPS_OFFSET         = findOffsetOrDefault("MethodData::_num_loops",                 272L);
    private static final long NUM_BLOCKS_OFFSET        = findOffsetOrDefault("MethodData::_num_blocks",                274L);
    private static final long WOULD_PROFILE_OFFSET     = findOffsetOrDefault("MethodData::_would_profile",             276L);
    private static final long JVMCI_IR_SIZE_OFFSET     = findOffsetOrDefault("MethodData::_jvmci_ir_size",             280L);
    private static final long FAILED_SPECS_OFFSET      = findOffsetOrDefault("MethodData::_failed_speculations",       288L);
    private static final long DATA_SIZE_OFFSET         = findOffsetOrDefault("MethodData::_data_size",                 296L);
    private static final long PARAMS_DI_OFFSET         = findOffsetOrDefault("MethodData::_parameters_type_data_di",   300L);
    private static final long DATA_OFFSET              = findOffsetOrDefault("MethodData::_data[0]",                   304L);

    // ---------- 常量 ----------
    public static final int NO_PARAMETERS            = -2;
    public static final int PARAMETERS_UNINITIALIZED = -1;

    public static final int TAG_NO_TAG                 = 0;
    public static final int TAG_BIT_DATA               = 1;
    public static final int TAG_COUNTER_DATA           = 2;
    public static final int TAG_JUMP_DATA              = 3;
    public static final int TAG_RECEIVER_TYPE_DATA     = 4;
    public static final int TAG_VIRTUAL_CALL_DATA      = 5;
    public static final int TAG_RET_DATA               = 6;
    public static final int TAG_BRANCH_DATA            = 7;
    public static final int TAG_MULTI_BRANCH_DATA      = 8;
    public static final int TAG_ARG_INFO_DATA          = 9;
    public static final int TAG_CALL_TYPE_DATA         = 10;
    public static final int TAG_VIRTUAL_CALL_TYPE_DATA = 11;
    public static final int TAG_PARAMETERS_TYPE_DATA   = 12;
    public static final int TAG_SPECULATIVE_TRAP_DATA  = 13;

    public static final int WOULD_PROFILE_UNKNOWN = 0;
    public static final int WOULD_PROFILE_NO      = 1;
    public static final int WOULD_PROFILE_PROFILE = 2;

    // ---------- 字段 ----------
    public long  method;
    public int   size;
    public int   hintDi;
    public long  eflags;
    public long  argLocal;
    public long  argStack;
    public long  argReturned;
    public int   creationMileage;
    public int   invocationCounterStart;
    public int   backedgeCounterStart;
    public int   tenureTraps;
    public int   invokeMask;
    public int   backedgeMask;
    public int   rtmState;
    public short numLoops;
    public short numBlocks;
    public int   wouldProfile;
    public int   jvmciIrSize;
    public long  failedSpeculations;
    public int   dataSize;
    public int   parametersTypeDataDi;

    private long address;

    public MethodDataMirror(long address) {
        this.address = address;
        readFields();
    }

    private void readFields() {
        long base = address;
        method                 = UnsafeUtils.unsafe.getLong(base + METHOD_OFFSET);
        size                   = UnsafeUtils.unsafe.getInt(base + SIZE_OFFSET);
        hintDi                 = UnsafeUtils.unsafe.getInt(base + HINT_DI_OFFSET);
        eflags                 = UnsafeUtils.unsafe.getLong(base + EFLAGS_OFFSET);
        argLocal               = UnsafeUtils.unsafe.getLong(base + ARG_LOCAL_OFFSET);
        argStack               = UnsafeUtils.unsafe.getLong(base + ARG_STACK_OFFSET);
        argReturned            = UnsafeUtils.unsafe.getLong(base + ARG_RETURNED_OFFSET);
        creationMileage        = UnsafeUtils.unsafe.getInt(base + CREATION_MILEAGE_OFFSET);
        invocationCounterStart = UnsafeUtils.unsafe.getInt(base + INVOCATION_START_OFFSET);
        backedgeCounterStart   = UnsafeUtils.unsafe.getInt(base + BACKEDGE_START_OFFSET);
        tenureTraps            = UnsafeUtils.unsafe.getInt(base + TENURE_TRAPS_OFFSET);
        invokeMask             = UnsafeUtils.unsafe.getInt(base + INVOKE_MASK_OFFSET);
        backedgeMask           = UnsafeUtils.unsafe.getInt(base + BACKEDGE_MASK_OFFSET);
        rtmState               = RTM_STATE_OFFSET < 0 ? -1 : UnsafeUtils.unsafe.getInt(base + RTM_STATE_OFFSET);
        numLoops               = UnsafeUtils.unsafe.getShort(base + NUM_LOOPS_OFFSET);
        numBlocks              = UnsafeUtils.unsafe.getShort(base + NUM_BLOCKS_OFFSET);
        wouldProfile           = UnsafeUtils.unsafe.getInt(base + WOULD_PROFILE_OFFSET);
        jvmciIrSize            = JVMCI_IR_SIZE_OFFSET < 0 ? -1 : UnsafeUtils.unsafe.getInt(base + JVMCI_IR_SIZE_OFFSET);
        failedSpeculations     = FAILED_SPECS_OFFSET < 0 ? 0L : UnsafeUtils.unsafe.getLong(base + FAILED_SPECS_OFFSET);
        dataSize               = UnsafeUtils.unsafe.getInt(base + DATA_SIZE_OFFSET);
        parametersTypeDataDi   = UnsafeUtils.unsafe.getInt(base + PARAMS_DI_OFFSET);
    }

    public long getAddress() { return address; }

    public long dataBase() { return address + DATA_OFFSET; }
    public long dataEnd()  { return dataBase() + dataSize; }

    public boolean hasParametersTypeData() {
        return parametersTypeDataDi != NO_PARAMETERS && parametersTypeDataDi != PARAMETERS_UNINITIALIZED;
    }

    public int parametersSizeInBytes() {
        if (!hasParametersTypeData()) return 0;
        return dataLayoutAt(parametersTypeDataDi).sizeInBytes();
    }

    public long extraDataBase()  { return dataEnd(); }
    public long extraDataLimit() { return address + size - parametersSizeInBytes(); }
    public int  extraDataSize()  { return (int) (extraDataLimit() - extraDataBase()); }

    public void setHintDi(int di) {
        hintDi = di;
        UnsafeUtils.unsafe.putInt(address + HINT_DI_OFFSET, di);
    }

    public void setParametersTypeDataDi(int di) {
        parametersTypeDataDi = di;
        UnsafeUtils.unsafe.putInt(address + PARAMS_DI_OFFSET, di);
    }

    public DataLayoutMirror dataLayoutAt(int di) {
        if (di < 0 || di >= dataSize)
            throw new IndexOutOfBoundsException("di=" + di + " dataSize=" + dataSize);
        return new DataLayoutMirror(dataBase() + di);
    }

    public List<DataLayoutMirror> allDataLayouts() {
        List<DataLayoutMirror> out = new ArrayList<>();
        int di = 0;
        while (di < dataSize) {
            DataLayoutMirror dl = dataLayoutAt(di);
            out.add(dl);
            int cellCount = cellCountFor(dl);
            int step = (DataLayoutMirror.HEADER_CELLS + cellCount) * 8;
            if (step <= 0) break;
            di += step;
        }
        return out;
    }

    /** 清空 profile，把参数 profile 标记为“无”。 */
    public void resetProfile() {
        long start = dataBase();
        long end   = address + size;
        for (long a = start; a < end; a += 8) {
            UnsafeUtils.unsafe.putLong(a, 0L);
        }
        setHintDi(0);
        setParametersTypeDataDi(NO_PARAMETERS);
        readFields();
    }

    public static int cellCountFor(DataLayoutMirror dl) {
        switch (dl.tag) {
            case TAG_BIT_DATA:                return 0;
            case TAG_COUNTER_DATA:            return 1;
            case TAG_JUMP_DATA:               return 2;
            case TAG_BRANCH_DATA:             return 3;
            case TAG_RECEIVER_TYPE_DATA:
            case TAG_VIRTUAL_CALL_DATA:       return 1 + 2 * 2;
            case TAG_RET_DATA:                return 1 + 3 * 2;
            case TAG_MULTI_BRANCH_DATA:
            case TAG_ARG_INFO_DATA:
            case TAG_PARAMETERS_TYPE_DATA:
            case TAG_CALL_TYPE_DATA:
            case TAG_VIRTUAL_CALL_TYPE_DATA:
                return dl.intAt(0);
            case TAG_SPECULATIVE_TRAP_DATA:   return 2;
            default:                          return 0;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "MethodDataMirror[address=0x%x, method=0x%x, size=%d, dataSize=%d, paramsDi=%d, " +
                        "initStart=%d, backedgeStart=%d, tenureTraps=%d, numLoops=%d, numBlocks=%d, " +
                        "wouldProfile=%d, hintDi=%d, rtmState=%d, jvmciIrSize=%d]",
                address, method, size, dataSize, parametersTypeDataDi,
                invocationCounterStart, backedgeCounterStart, tenureTraps,
                numLoops, numBlocks, wouldProfile, hintDi, rtmState, jvmciIrSize);
    }
}