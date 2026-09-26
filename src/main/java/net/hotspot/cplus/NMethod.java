
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class NMethod extends CompiledMethod {
    private static final long ENTRY_BCI_OFFSET                   = findOffsetOrDefault("nmethod::_entry_bci",                   192L);
    private static final long OSR_LINK_OFFSET                    = findOffsetOrDefault("nmethod::_osr_link",                    200L);
    private static final long OOPS_DO_MARK_LINK_OFFSET           = findOffsetOrDefault("nmethod::_oops_do_mark_link",           208L);

    private static final long ENTRY_POINT_OFFSET                 = findOffsetOrDefault("nmethod::_entry_point",                 216L);
    private static final long VERIFIED_ENTRY_POINT_OFFSET        = findOffsetOrDefault("nmethod::_verified_entry_point",        224L);
    private static final long OSR_ENTRY_POINT_OFFSET             = findOffsetOrDefault("nmethod::_osr_entry_point",             232L);

    private static final long EXCEPTION_OFFSET_OFFSET            = findOffsetOrDefault("nmethod::_exception_offset",            240L);
    private static final long UNWIND_HANDLER_OFFSET_OFFSET       = findOffsetOrDefault("nmethod::_unwind_handler_offset",       244L);
    private static final long CONSTS_OFFSET_OFFSET               = findOffsetOrDefault("nmethod::_consts_offset",               248L);
    private static final long STUB_OFFSET_OFFSET                 = findOffsetOrDefault("nmethod::_stub_offset",                 252L);
    private static final long OOPS_OFFSET_OFFSET                 = findOffsetOrDefault("nmethod::_oops_offset",                 256L);
    private static final long METADATA_OFFSET_OFFSET             = findOffsetOrDefault("nmethod::_metadata_offset",             260L);
    private static final long SCOPES_DATA_OFFSET_OFFSET          = findOffsetOrDefault("nmethod::_scopes_data_offset",          264L);
    private static final long SCOPES_PCS_OFFSET_OFFSET           = findOffsetOrDefault("nmethod::_scopes_pcs_offset",           268L);
    private static final long DEPENDENCIES_OFFSET_OFFSET         = findOffsetOrDefault("nmethod::_dependencies_offset",         272L);
    private static final long NATIVE_INVOKERS_OFFSET_OFFSET      = findOffsetOrDefault("nmethod::_native_invokers_offset",      276L);
    private static final long HANDLER_TABLE_OFFSET_OFFSET        = findOffsetOrDefault("nmethod::_handler_table_offset",        280L);
    private static final long NUL_CHK_TABLE_OFFSET_OFFSET        = findOffsetOrDefault("nmethod::_nul_chk_table_offset",        284L);
    private static final long SPECULATIONS_OFFSET_OFFSET         = findOffsetOrDefault("nmethod::_speculations_offset",         288L);
    private static final long JVMCI_DATA_OFFSET_OFFSET           = findOffsetOrDefault("nmethod::_jvmci_data_offset",           292L);
    private static final long NMETHOD_END_OFFSET_OFFSET          = findOffsetOrDefault("nmethod::_nmethod_end_offset",          296L);

    private static final long ORIG_PC_OFFSET_OFFSET              = findOffsetOrDefault("nmethod::_orig_pc_offset",              300L);
    private static final long COMPILE_ID_OFFSET                  = findOffsetOrDefault("nmethod::_compile_id",                  304L);
    private static final long COMP_LEVEL_OFFSET                  = findOffsetOrDefault("nmethod::_comp_level",                  308L);

    private static final long HAS_FLUSHED_DEPENDENCIES_OFFSET    = findOffsetOrDefault("nmethod::_has_flushed_dependencies",    312L);
    private static final long UNLOAD_REPORTED_OFFSET             = findOffsetOrDefault("nmethod::_unload_reported",             313L);
    private static final long LOAD_REPORTED_OFFSET               = findOffsetOrDefault("nmethod::_load_reported",               314L);
    public  static final long STATE_OFFSET                       = findOffsetOrDefault("nmethod::_state",                       315L);

    private static final long LOCK_COUNT_OFFSET                  = findOffsetOrDefault("nmethod::_lock_count",                  320L);
    private static final long STACK_TRAVERSAL_MARK_OFFSET        = findOffsetOrDefault("nmethod::_stack_traversal_mark",        324L);
    private static final long HOTNESS_COUNTER_OFFSET             = findOffsetOrDefault("nmethod::_hotness_counter",             332L);
    private static final long IS_UNLOADING_STATE_OFFSET          = findOffsetOrDefault("nmethod::_is_unloading_state",          336L);
    private static final long NATIVE_RECEIVER_SP_OFFSET_OFFSET   = findOffsetOrDefault("nmethod::_native_receiver_sp_offset",   340L);
    private static final long NATIVE_BASIC_LOCK_SP_OFFSET_OFFSET = findOffsetOrDefault("nmethod::_native_basic_lock_sp_offset", 344L);

    public static final int NOT_INSTALLED = -1;
    public static final int IN_USE        =  0;
    public static final int NOT_USED      =  1;
    public static final int NOT_ENTRANT   =  2;
    public static final int UNLOADED      =  3;
    public static final int ZOMBIE        =  4;

    public static final int CLAIM_WEAK_REQUEST    = 0;
    public static final int CLAIM_WEAK_DONE       = 1;
    public static final int CLAIM_STRONG_REQUEST  = 2;
    public static final int CLAIM_STRONG_DONE     = 3;

    public int  entryBci;
    public long osrLink;
    public long oopsDoMarkLink;

    public long entryPoint;
    public long verifiedEntryPoint;
    public long osrEntryPoint;

    public int  exceptionOffset;
    public int  unwindHandlerOffset;
    public int  constsOffset;
    public int  stubOffset;
    public int  oopsOffset;
    public int  metadataOffset;
    public int  scopesDataOffset;
    public int  scopesPcsOffset;
    public int  dependenciesOffset;
    public int  nativeInvokersOffset;
    public int  handlerTableOffset;
    public int  nulChkTableOffset;
    public int  speculationsOffset;
    public int  jvmciDataOffset;
    public int  nmethodEndOffset;

    public int  origPcOffset;
    public int  compileId;
    public int  compLevel;

    public boolean hasFlushedDependencies;
    public boolean unloadReported;
    public boolean loadReported;
    public byte    state;

    public int  lockCount;
    public long stackTraversalMark;
    public int  hotnessCounter;
    public byte isUnloadingState;
    public int  nativeReceiverSpOffset;
    public int  nativeBasicLockSpOffset;

    public NMethod(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        if (base == 0) return;

        entryBci                  = UnsafeUtils.unsafe.getInt (base + ENTRY_BCI_OFFSET);
        osrLink                   = UnsafeUtils.unsafe.getLong(base + OSR_LINK_OFFSET);
        oopsDoMarkLink            = UnsafeUtils.unsafe.getLong(base + OOPS_DO_MARK_LINK_OFFSET);

        entryPoint                = UnsafeUtils.unsafe.getLong(base + ENTRY_POINT_OFFSET);
        verifiedEntryPoint        = UnsafeUtils.unsafe.getLong(base + VERIFIED_ENTRY_POINT_OFFSET);
        osrEntryPoint             = UnsafeUtils.unsafe.getLong(base + OSR_ENTRY_POINT_OFFSET);

        exceptionOffset           = UnsafeUtils.unsafe.getInt (base + EXCEPTION_OFFSET_OFFSET);
        unwindHandlerOffset       = UnsafeUtils.unsafe.getInt (base + UNWIND_HANDLER_OFFSET_OFFSET);
        constsOffset              = UnsafeUtils.unsafe.getInt (base + CONSTS_OFFSET_OFFSET);
        stubOffset                = UnsafeUtils.unsafe.getInt (base + STUB_OFFSET_OFFSET);
        oopsOffset                = UnsafeUtils.unsafe.getInt (base + OOPS_OFFSET_OFFSET);
        metadataOffset            = UnsafeUtils.unsafe.getInt (base + METADATA_OFFSET_OFFSET);
        scopesDataOffset          = UnsafeUtils.unsafe.getInt (base + SCOPES_DATA_OFFSET_OFFSET);
        scopesPcsOffset           = UnsafeUtils.unsafe.getInt (base + SCOPES_PCS_OFFSET_OFFSET);
        dependenciesOffset        = UnsafeUtils.unsafe.getInt (base + DEPENDENCIES_OFFSET_OFFSET);
        nativeInvokersOffset      = UnsafeUtils.unsafe.getInt (base + NATIVE_INVOKERS_OFFSET_OFFSET);
        handlerTableOffset        = UnsafeUtils.unsafe.getInt (base + HANDLER_TABLE_OFFSET_OFFSET);
        nulChkTableOffset         = UnsafeUtils.unsafe.getInt (base + NUL_CHK_TABLE_OFFSET_OFFSET);
        speculationsOffset        = UnsafeUtils.unsafe.getInt (base + SPECULATIONS_OFFSET_OFFSET);
        jvmciDataOffset           = UnsafeUtils.unsafe.getInt (base + JVMCI_DATA_OFFSET_OFFSET);
        nmethodEndOffset          = UnsafeUtils.unsafe.getInt (base + NMETHOD_END_OFFSET_OFFSET);

        origPcOffset              = UnsafeUtils.unsafe.getInt (base + ORIG_PC_OFFSET_OFFSET);
        compileId                 = UnsafeUtils.unsafe.getInt (base + COMPILE_ID_OFFSET);
        compLevel                 = UnsafeUtils.unsafe.getInt (base + COMP_LEVEL_OFFSET);

        hasFlushedDependencies    = UnsafeUtils.unsafe.getByte(base + HAS_FLUSHED_DEPENDENCIES_OFFSET) != 0;
        unloadReported            = UnsafeUtils.unsafe.getByte(base + UNLOAD_REPORTED_OFFSET)          != 0;
        loadReported              = UnsafeUtils.unsafe.getByte(base + LOAD_REPORTED_OFFSET)            != 0;
        state                     = UnsafeUtils.unsafe.getByte(base + STATE_OFFSET);

        lockCount                 = UnsafeUtils.unsafe.getInt (base + LOCK_COUNT_OFFSET);
        stackTraversalMark        = UnsafeUtils.unsafe.getLong(base + STACK_TRAVERSAL_MARK_OFFSET);
        hotnessCounter            = UnsafeUtils.unsafe.getInt (base + HOTNESS_COUNTER_OFFSET);
        isUnloadingState          = UnsafeUtils.unsafe.getByte(base + IS_UNLOADING_STATE_OFFSET);
        nativeReceiverSpOffset    = UnsafeUtils.unsafe.getInt (base + NATIVE_RECEIVER_SP_OFFSET_OFFSET);
        nativeBasicLockSpOffset   = UnsafeUtils.unsafe.getInt (base + NATIVE_BASIC_LOCK_SP_OFFSET_OFFSET);

        readFields0();
    }

    private void readFields0() {}

    @Override
    public boolean isNmethod() { return true; }

    @Override
    public boolean isRuntimeStub() { return false; }


    public Method getMethod() { return super.getMethod(); }

    public boolean isOsrMethod() { return entryBci != -1 ; }

    public int     rawState()       { return state; }
    public boolean isNotInstalled() { return state == NOT_INSTALLED; }
    public boolean isInUse()        { return state <= IN_USE; }
    public boolean isAlive()        { return state <  UNLOADED; }
    public boolean isNotEntrant()   { return state == NOT_ENTRANT; }
    public boolean isZombie()       { return state == ZOMBIE; }
    public boolean isUnloadedState(){ return state == UNLOADED; }

    public long osrLink()          { return osrLink; }
    public int  oopsDoMarkState()  { return (int) (oopsDoMarkLink & 0x3L); }
    public long oopsDoMarkNext()   { return oopsDoMarkLink & ~0x3L; }

    public long entryPoint()          { return entryPoint; }
    public long verifiedEntryPoint()  { return verifiedEntryPoint; }
    public long osrEntryPoint()       { return osrEntryPoint; }

    public int compileId() { return compileId; }
    public int compLevel() { return compLevel; }

    public boolean hasFlushedDependencies() { return hasFlushedDependencies; }
    public boolean unloadReported()         { return unloadReported; }
    public boolean loadReported()           { return loadReported; }

    public boolean isLockedByVm()           { return lockCount > 0; }
    public int  lockCount()                 { return lockCount; }
    public long stackTraversalMark()        { return stackTraversalMark; }
    public int  hotnessCounter()            { return hotnessCounter; }
    public int  isUnloadingState()          { return isUnloadingState & 0xFF; }

    public int nativeReceiverSpOffset()     { return nativeReceiverSpOffset; }
    public int nativeBasicLockSpOffset()    { return nativeBasicLockSpOffset; }

    public long headerBegin()  { return getAddress(); }
    public long headerEnd()    { return getAddress() + headerSize; }

    public long constsBegin()             { return headerBegin() + constsOffset; }
    public long constsEnd()               { return getAddress()  + (codeBegin - getAddress()); }

    public long stubBegin()               { return headerBegin() + stubOffset; }
    public long stubEnd()                 { return headerBegin() + oopsOffset; }

    public long exceptionBegin()          { return headerBegin() + exceptionOffset; }
    public long unwindHandlerBegin()      { return unwindHandlerOffset != -1
            ? headerBegin() + unwindHandlerOffset : 0L; }

    public long oopsBegin()               { return headerBegin() + oopsOffset; }
    public long oopsEnd()                 { return headerBegin() + metadataOffset; }
    public long metadataBegin()           { return headerBegin() + metadataOffset; }
    public long metadataEnd()             { return scopesDataBegin; }

    public long scopesDataEnd()           { return headerBegin() + scopesPcsOffset; }
    public long scopesPcsBegin()          { return headerBegin() + scopesPcsOffset; }
    public long scopesPcsEnd()            { return headerBegin() + dependenciesOffset; }

    public long dependenciesBegin()       { return headerBegin() + dependenciesOffset; }
    public long dependenciesEnd()         { return headerBegin() + nativeInvokersOffset; }

    public long nativeInvokersBegin()     { return headerBegin() + nativeInvokersOffset; }
    public long nativeInvokersEnd()       { return headerBegin() + handlerTableOffset; }

    public long handlerTableBegin()       { return headerBegin() + handlerTableOffset; }
    public long handlerTableEnd()         { return headerBegin() + nulChkTableOffset; }

    public long nulChkTableBegin()        { return headerBegin() + nulChkTableOffset; }
    public long nulChkTableEnd()          { return headerBegin() + speculationsOffset; }

    public long speculationsBegin()       { return headerBegin() + speculationsOffset; }
    public long speculationsEnd()         { return headerBegin() + jvmciDataOffset; }

    public long jvmciDataBegin()          { return headerBegin() + jvmciDataOffset; }
    public long jvmciDataEnd()            { return headerBegin() + nmethodEndOffset; }

    public int oopsSize()          { return (int) (oopsEnd()         - oopsBegin());         }
    public int metadataSize()      { return (int) (metadataEnd()     - metadataBegin());     }
    public int dependenciesSize()  { return (int) (dependenciesEnd() - dependenciesBegin()); }
    public int speculationsSize()  { return (int) (speculationsEnd() - speculationsBegin()); }
    public int jvmciDataSize()     { return (int) (jvmciDataEnd()    - jvmciDataBegin());    }

    public boolean oopsContains(long addr)         { return oopsBegin()         <= addr && addr < oopsEnd();         }
    public boolean metadataContains(long addr)     { return metadataBegin()     <= addr && addr < metadataEnd();     }
    public boolean scopesDataContains(long addr)   { return scopesDataBegin    <= addr && addr < scopesDataEnd();   }
    public boolean scopesPcsContains(long addr)    { return scopesPcsBegin()    <= addr && addr < scopesPcsEnd();    }

    @Override
    public String toString() {
        Method m = getMethod();
        return String.format(
                "NMethod[address=0x%x, name=0x%x, method=0x%x, methodName=%s, compileId=%d, compLevel=%d, " +
                        "state=%d, entryBci=%d, osr=%s, hotness=%d, lockCount=%d, " +
                        "verifiedEntry=0x%x, entry=0x%x, osrEntry=0x%x, " +
                        "consts=%d, stub=%d, exception=%d, unwind=%d, " +
                        "oops=%d, metadata=%d, scopesData=%d, scopesPcs=%d, " +
                        "deps=%d, invokers=%d, handler=%d, nulChk=%d, " +
                        "spec=%d, jvmci=%d, end=%d]",
                getAddress(), name, method,
                m == null ? "null" : m.name(),
                compileId, compLevel, state, entryBci, isOsrMethod(),
                hotnessCounter, lockCount,
                verifiedEntryPoint, entryPoint, osrEntryPoint,
                constsOffset, stubOffset, exceptionOffset, unwindHandlerOffset,
                oopsOffset, metadataOffset, scopesDataOffset, scopesPcsOffset,
                dependenciesOffset, nativeInvokersOffset, handlerTableOffset,
                nulChkTableOffset, speculationsOffset, jvmciDataOffset, nmethodEndOffset);
    }
}