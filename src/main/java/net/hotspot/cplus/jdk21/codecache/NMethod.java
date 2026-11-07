package net.hotspot.cplus.jdk21.codecache;

import net.hotspot.cplus.jdk21.core.VMStructsHelper;
import net.hotspot.cplus.jdk21.klass.Method;

import net.hotspot.cplus.jdk21.core.UnsafeUtils;

import static net.hotspot.cplus.jdk21.core.VMStructsHelper.findOffsetOrDefault;

public class NMethod extends CompiledMethod {

    private static final long OSR_LINK_OFFSET             = findOffsetOrDefault("nmethod::_osr_link",             208L);
    private static final long ENTRY_POINT_OFFSET          = findOffsetOrDefault("nmethod::_entry_point",          224L);
    private static final long VERIFIED_ENTRY_POINT_OFFSET = findOffsetOrDefault("nmethod::_verified_entry_point", 232L);
    private static final long OSR_ENTRY_POINT_OFFSET      = findOffsetOrDefault("nmethod::_osr_entry_point",      240L);

    private static final long ENTRY_BCI_OFFSET            = findOffsetOrDefault("nmethod::_entry_bci",            252L);
    private static final long EXCEPTION_OFFSET_OFFSET     = findOffsetOrDefault("nmethod::_exception_offset",     256L);
    private static final long CONSTS_OFFSET_OFFSET        = findOffsetOrDefault("nmethod::_consts_offset",        264L);
    private static final long STUB_OFFSET_OFFSET          = findOffsetOrDefault("nmethod::_stub_offset",          268L);
    private static final long OOPS_OFFSET_OFFSET          = findOffsetOrDefault("nmethod::_oops_offset",          272L);
    private static final long METADATA_OFFSET_OFFSET      = findOffsetOrDefault("nmethod::_metadata_offset",      276L);
    private static final long SCOPES_PCS_OFFSET_OFFSET    = findOffsetOrDefault("nmethod::_scopes_pcs_offset",    284L);
    private static final long DEPENDENCIES_OFFSET_OFFSET  = findOffsetOrDefault("nmethod::_dependencies_offset",  288L);
    private static final long HANDLER_TABLE_OFFSET_OFFSET = findOffsetOrDefault("nmethod::_handler_table_offset", 292L);
    private static final long NUL_CHK_TABLE_OFFSET_OFFSET = findOffsetOrDefault("nmethod::_nul_chk_table_offset", 296L);
    private static final long NMETHOD_END_OFFSET_OFFSET   = findOffsetOrDefault("nmethod::_nmethod_end_offset",   308L);
    private static final long ORIG_PC_OFFSET_OFFSET       = findOffsetOrDefault("nmethod::_orig_pc_offset",       312L);
    private static final long COMPILE_ID_OFFSET           = findOffsetOrDefault("nmethod::_compile_id",           316L);
    private static final long COMP_LEVEL_OFFSET           = findOffsetOrDefault("nmethod::_comp_level",           336L);
    public  static final long STATE_OFFSET                = findOffsetOrDefault("nmethod::_state",                340L);

    public static final int NOT_INSTALLED = -1;
    public static final int IN_USE        =  0;
    public static final int NOT_USED      =  1;
    public static final int NOT_ENTRANT   =  2;
    public static final int UNLOADED      =  3;
    public static final int ZOMBIE        =  4;

    private static final String[] STATE_NAMES = {
            "in_use", "not_used", "not_entrant", "unloaded", "zombie"
    };

    public long osrLink;

    public long entryPoint;
    public long verifiedEntryPoint;
    public long osrEntryPoint;

    public int entryBci;
    public int exceptionOffset;
    public int constsOffset;
    public int stubOffset;
    public int oopsOffset;
    public int metadataOffset;
    public int scopesPcsOffset;
    public int dependenciesOffset;
    public int handlerTableOffset;
    public int nulChkTableOffset;
    public int nmethodEndOffset;
    public int origPcOffset;
    public int compileId;
    public int compLevel;
    public byte state;

    public NMethod(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        if (base == 0) return;

        osrLink                = UnsafeUtils.unsafe.getLong(base + OSR_LINK_OFFSET);
        entryPoint             = UnsafeUtils.unsafe.getLong(base + ENTRY_POINT_OFFSET);
        verifiedEntryPoint     = UnsafeUtils.unsafe.getLong(base + VERIFIED_ENTRY_POINT_OFFSET);
        osrEntryPoint          = UnsafeUtils.unsafe.getLong(base + OSR_ENTRY_POINT_OFFSET);

        entryBci               = UnsafeUtils.unsafe.getInt(base + ENTRY_BCI_OFFSET);
        exceptionOffset        = UnsafeUtils.unsafe.getInt(base + EXCEPTION_OFFSET_OFFSET);
        constsOffset           = UnsafeUtils.unsafe.getInt(base + CONSTS_OFFSET_OFFSET);
        stubOffset             = UnsafeUtils.unsafe.getInt(base + STUB_OFFSET_OFFSET);
        oopsOffset             = UnsafeUtils.unsafe.getInt(base + OOPS_OFFSET_OFFSET);
        metadataOffset         = UnsafeUtils.unsafe.getInt(base + METADATA_OFFSET_OFFSET);
        scopesPcsOffset        = UnsafeUtils.unsafe.getInt(base + SCOPES_PCS_OFFSET_OFFSET);
        dependenciesOffset     = UnsafeUtils.unsafe.getInt(base + DEPENDENCIES_OFFSET_OFFSET);
        handlerTableOffset     = UnsafeUtils.unsafe.getInt(base + HANDLER_TABLE_OFFSET_OFFSET);
        nulChkTableOffset      = UnsafeUtils.unsafe.getInt(base + NUL_CHK_TABLE_OFFSET_OFFSET);
        nmethodEndOffset       = UnsafeUtils.unsafe.getInt(base + NMETHOD_END_OFFSET_OFFSET);
        origPcOffset           = UnsafeUtils.unsafe.getInt(base + ORIG_PC_OFFSET_OFFSET);
        compileId              = UnsafeUtils.unsafe.getInt(base + COMPILE_ID_OFFSET);

        compLevel              = UnsafeUtils.unsafe.getByte(base + COMP_LEVEL_OFFSET) & 0xFF;
        state                  = UnsafeUtils.unsafe.getByte(base + STATE_OFFSET);
    }

    @Override
    public boolean isNmethod() {
        return true;
    }

    public String stateName() {
        int s = state;
        return (s >= 0 && s < STATE_NAMES.length) ? STATE_NAMES[s] : ("unknown(" + s + ")");
    }

    public boolean isInUse()        { return state == IN_USE; }
    public boolean isNotInstalled() { return state == NOT_INSTALLED; }
    public boolean isUnloaded()     { return state == UNLOADED; }
    public boolean isZombie()       { return state == ZOMBIE; }
    public boolean isNotEntrant()   { return state == NOT_ENTRANT; }

    public int compLevel() {
        return compLevel;
    }

    public long entryPoint()          { return entryPoint; }
    public long verifiedEntryPoint()  { return verifiedEntryPoint; }
    public long osrEntryPoint()       { return osrEntryPoint; }

    public boolean isOsr() {
        return entryBci != -1;
    }

    public long codeBegin() {
        return codeBegin;
    }

    @Override
    public String toString() {
        Method m = getMethod();
        return String.format(
                "NMethod[address=0x%x, method=%s, state=%s, compLevel=%d, size=%d, "
                        + "entry=0x%x, verifiedEntry=0x%x, entryBci=%d, compileId=%d]",
                getAddress(),
                m == null ? "?" : m.name() + m.signature(),
                stateName(), compLevel, size,
                entryPoint, verifiedEntryPoint, entryBci, compileId);
    }
}
