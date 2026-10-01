package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

import java.util.ArrayList;
import java.util.List;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

/**
 * Java mapping of HotSpot's oops/methodData.hpp MethodData.
 *
 * class MethodData : public Metadata {
 *   Method* _method;                 // 8
 *   int _size;                       // 16
 *   int _hint_di;                    // 20
 *   Mutex _extra_data_lock;          // 24
 *   CompilerCounters _compiler_counters;  // 88
 *   intx _eflags;                    // 168
 *   intx _arg_local;                 // 176
 *   intx _arg_stack;                 // 184
 *   intx _arg_returned;              // 192
 *   int _creation_mileage;           // 200
 *   InvocationCounter _invocation_counter;  // 204
 *   InvocationCounter _backedge_counter;    // 208
 *   int _invocation_counter_start;   // 212
 *   int _backedge_counter_start;     // 216
 *   uint _tenure_traps;              // 220
 *   int _invoke_mask;                // 224
 *   int _backedge_mask;              // 228
 *   short _num_loops;                // 232
 *   short _num_blocks;               // 234
 *   WouldProfile _would_profile;     // 236
 *   int _jvmci_ir_size;              // 240  (JDK 17: _jvmci_ir_size, then _failed_speculations)
 *   FailedSpeculation* _failed_speculations;  // 248
 *   int _data_size;                  // 256
 *   int _parameters_type_data_di;    // 260
 *   intptr_t _data[1];               // 264
 * };
 */
public class MethodData {

    // ---------- MethodData field offsets ----------
    private static final long METHOD_OFFSET            = findOffsetOrDefault("MethodData::_method",                    8L);
    private static final long SIZE_OFFSET              = findOffsetOrDefault("MethodData::_size",                      16L);
    private static final long HINT_DI_OFFSET           = findOffsetOrDefault("MethodData::_hint_di",                   20L);
    private static final long EXTRA_DATA_LOCK_OFFSET   = findOffsetOrDefault("MethodData::_extra_data_lock",           24L);
    private static final long COMPILER_COUNTERS_OFFSET = findOffsetOrDefault("MethodData::_compiler_counters",         88L);
    private static final long EFLAGS_OFFSET            = findOffsetOrDefault("MethodData::_eflags",                    168L);
    private static final long ARG_LOCAL_OFFSET         = findOffsetOrDefault("MethodData::_arg_local",                 176L);
    private static final long ARG_STACK_OFFSET         = findOffsetOrDefault("MethodData::_arg_stack",                 184L);
    private static final long ARG_RETURNED_OFFSET      = findOffsetOrDefault("MethodData::_arg_returned",              192L);
    private static final long CREATION_MILEAGE_OFFSET  = findOffsetOrDefault("MethodData::_creation_mileage",          200L);
    private static final long INVOCATION_CNT_OFFSET    = findOffsetOrDefault("MethodData::_invocation_counter",        204L);
    private static final long BACKEDGE_CNT_OFFSET      = findOffsetOrDefault("MethodData::_backedge_counter",          208L);
    private static final long INVOCATION_START_OFFSET  = findOffsetOrDefault("MethodData::_invocation_counter_start",  212L);
    private static final long BACKEDGE_START_OFFSET    = findOffsetOrDefault("MethodData::_backedge_counter_start",    216L);
    private static final long TENURE_TRAPS_OFFSET      = findOffsetOrDefault("MethodData::_tenure_traps",              220L);
    private static final long INVOKE_MASK_OFFSET       = findOffsetOrDefault("MethodData::_invoke_mask",               224L);
    private static final long BACKEDGE_MASK_OFFSET     = findOffsetOrDefault("MethodData::_backedge_mask",             228L);
    private static final long RTM_STATE_OFFSET         = findOffsetOrDefault("MethodData::_rtm_state",                 -1L);
    private static final long NUM_LOOPS_OFFSET         = findOffsetOrDefault("MethodData::_num_loops",                 232L);
    private static final long NUM_BLOCKS_OFFSET        = findOffsetOrDefault("MethodData::_num_blocks",                234L);
    private static final long WOULD_PROFILE_OFFSET     = findOffsetOrDefault("MethodData::_would_profile",             236L);
    private static final long JVMCI_IR_SIZE_OFFSET     = findOffsetOrDefault("MethodData::_jvmci_ir_size",             240L);
    private static final long FAILED_SPECS_OFFSET      = findOffsetOrDefault("MethodData::_failed_speculations",       248L);
    private static final long DATA_SIZE_OFFSET         = findOffsetOrDefault("MethodData::_data_size",                 256L);
    private static final long PARAMS_DI_OFFSET         = findOffsetOrDefault("MethodData::_parameters_type_data_di",   260L);
    private static final long DATA_OFFSET              = findOffsetOrDefault("MethodData::_data[0]",                   264L);

    // ---------- CompilerCounters field offsets (relative to CompilerCounters base) ----------
    private static final long CC_NOF_DECOMPILES        = findOffsetOrDefault("CompilerCounters::_nof_decompiles",           0L);
    private static final long CC_NOF_OVERFLOW_RECOMP   = findOffsetOrDefault("CompilerCounters::_nof_overflow_recompiles", 4L);
    private static final long CC_NOF_OVERFLOW_TRAPS    = findOffsetOrDefault("CompilerCounters::_nof_overflow_traps",      8L);
    private static final long CC_TRAP_HIST             = findOffsetOrDefault("CompilerCounters::_trap_hist._array[0]",     16L);

    // ---------- Constants ----------
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

    // ---------- Fields ----------
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

    private final long address;

    public MethodData(long address) {
        if (address == 0) {
            throw new IllegalArgumentException("MethodData: address == 0");
        }
        this.address = address;
        readFields();
    }

    public final void readFields() {
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
        return parametersTypeDataDi != NO_PARAMETERS
                && parametersTypeDataDi != PARAMETERS_UNINITIALIZED;
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

    public DataLayout dataLayoutAt(int di) {
        if (di < 0 || di >= dataSize) {
            throw new IndexOutOfBoundsException("MethodData: di=" + di + " dataSize=" + dataSize);
        }
        return new DataLayout(dataBase() + di);
    }

    /**
     * Walk the data array in order. Uses {@link #cellCountFor} to compute
     * each DataLayout's stride.
     */
    public List<DataLayout> allDataLayouts() {
        List<DataLayout> out = new ArrayList<>();
        int di = 0;
        while (di < dataSize) {
            DataLayout dl = dataLayoutAt(di);
            if (dl.tag == TAG_NO_TAG) break;
            if (dl.tag < 0 || dl.tag > TAG_SPECULATIVE_TRAP_DATA) {
                // Unknown tag; stop scanning to avoid runaway loop.
                break;
            }

            out.add(dl);

            int cellCount = cellCountFor(dl);
            if (cellCount < 0) break;

            int step = (DataLayout.HEADER_CELLS + cellCount) * 8;
            if (step <= 0) break;

            di += step;
        }
        return out;
    }

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

    public CompilerCounters counters() {
        return new CompilerCounters(address + COMPILER_COUNTERS_OFFSET);
    }

    public InvocationCounter invocationCounter() {
        return new InvocationCounter(address + INVOCATION_CNT_OFFSET);
    }

    public InvocationCounter backedgeCounter() {
        return new InvocationCounter(address + BACKEDGE_CNT_OFFSET);
    }


    public static int cellCountFor(DataLayout dl) {
        switch (dl.tag) {
            case TAG_BIT_DATA:               return 0;
            case TAG_COUNTER_DATA:           return 1;
            case TAG_JUMP_DATA:              return 2;
            case TAG_BRANCH_DATA:            return 3;
            case TAG_RECEIVER_TYPE_DATA:
            case TAG_VIRTUAL_CALL_DATA:      return 6;
            case TAG_RET_DATA:               return 7;
            case TAG_MULTI_BRANCH_DATA:
            case TAG_ARG_INFO_DATA:
            case TAG_PARAMETERS_TYPE_DATA:   return dl.intAt(0) + 1;
            case TAG_CALL_TYPE_DATA:         return 2 + dl.intAt(1);
            case TAG_VIRTUAL_CALL_TYPE_DATA: return 7 + dl.intAt(6);
            case TAG_SPECULATIVE_TRAP_DATA:  return 1;
            default:                         return 0;
        }
    }

    public static String tagName(int tag) {
        switch (tag) {
            case TAG_NO_TAG:                 return "no_tag";
            case TAG_BIT_DATA:               return "bit_data";
            case TAG_COUNTER_DATA:           return "counter_data";
            case TAG_JUMP_DATA:              return "jump_data";
            case TAG_RECEIVER_TYPE_DATA:     return "receiver_type_data";
            case TAG_VIRTUAL_CALL_DATA:      return "virtual_call_data";
            case TAG_RET_DATA:               return "ret_data";
            case TAG_BRANCH_DATA:            return "branch_data";
            case TAG_MULTI_BRANCH_DATA:      return "multi_branch_data";
            case TAG_ARG_INFO_DATA:          return "arg_info_data";
            case TAG_CALL_TYPE_DATA:         return "call_type_data";
            case TAG_VIRTUAL_CALL_TYPE_DATA: return "virtual_call_type_data";
            case TAG_PARAMETERS_TYPE_DATA:   return "parameters_type_data";
            case TAG_SPECULATIVE_TRAP_DATA:  return "speculative_trap_data";
            default:                         return "tag=" + tag;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "MethodData[address=0x%x, method=0x%x, size=%d, dataSize=%d, paramsDi=%d, " +
                        "initStart=%d, backedgeStart=%d, tenureTraps=%d, numLoops=%d, numBlocks=%d, " +
                        "wouldProfile=%d, hintDi=%d, rtmState=%d, jvmciIrSize=%d]",
                address, method, size, dataSize, parametersTypeDataDi,
                invocationCounterStart, backedgeCounterStart, tenureTraps,
                numLoops, numBlocks, wouldProfile, hintDi, rtmState, jvmciIrSize);
    }


    public static final class CompilerCounters {
        private final long base;

        public CompilerCounters(long base) { this.base = base; }

        public long getAddress() { return base; }

        public int nofDecompiles()         { return UnsafeUtils.unsafe.getInt(base + CC_NOF_DECOMPILES); }
        public int nofOverflowRecompiles() { return UnsafeUtils.unsafe.getInt(base + CC_NOF_OVERFLOW_RECOMP); }
        public int nofOverflowTraps()      { return UnsafeUtils.unsafe.getInt(base + CC_NOF_OVERFLOW_TRAPS); }
        public long trapHistBase()         { return base + CC_TRAP_HIST; }
        public byte trapHistAt(int reason) { return UnsafeUtils.unsafe.getByte(trapHistBase() + reason); }

        @Override
        public String toString() {
            return String.format(
                    "CompilerCounters[address=0x%x, nofDecompiles=%d, nofOverflowRecompiles=%d, nofOverflowTraps=%d]",
                    base, nofDecompiles(), nofOverflowRecompiles(), nofOverflowTraps());
        }
    }

    public static final class InvocationCounter {
        private final long base;

        public InvocationCounter(long base) { this.base = base; }

        public long getAddress() { return base; }

        public int  raw()         { return UnsafeUtils.unsafe.getInt(base); }
        public void setRaw(int v) { UnsafeUtils.unsafe.putInt(base, v); }

        @Override
        public String toString() {
            return String.format("InvocationCounter[address=0x%x, raw=0x%x]", base, raw());
        }
    }
}