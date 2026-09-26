package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CompileTask {

    private static final long METHOD_OFF =
            VMStructsHelper.findOffsetOrDefault("CompileTask::_method", -1L);
    private static final long OSR_BCI_OFF =
            VMStructsHelper.findOffsetOrDefault("CompileTask::_osr_bci", -1L);
    private static final long COMP_LEVEL_OFF =
            VMStructsHelper.findOffsetOrDefault("CompileTask::_comp_level", -1L);
    private static final long COMPILE_ID_OFF =
            VMStructsHelper.findOffsetOrDefault("CompileTask::_compile_id", -1L);

    private final long address;

    public final long methodAddr;
    public final int  osrBci;
    public final int  compLevel;
    public final int  compileId;

    public CompileTask(long address) {
        this.address = address;
        this.methodAddr = METHOD_OFF < 0 ? 0
                : UnsafeUtils.unsafe.getLong(address + METHOD_OFF);
        this.osrBci = OSR_BCI_OFF < 0 ? -1
                : UnsafeUtils.unsafe.getInt(address + OSR_BCI_OFF);
        this.compLevel = COMP_LEVEL_OFF < 0 ? -1
                : UnsafeUtils.unsafe.getInt(address + COMP_LEVEL_OFF);
        this.compileId = COMPILE_ID_OFF < 0 ? -1
                : UnsafeUtils.unsafe.getInt(address + COMPILE_ID_OFF);
    }

    public long getAddress() { return address; }

    public Method getMethod() {
        return methodAddr == 0 ? null : new Method(methodAddr);
    }

    @Override
    public String toString() {
        return String.format(
                "CompileTask[address=0x%x, method=0x%x, level=%d, osr=%d, id=%d]",
                address, methodAddr, compLevel, osrBci, compileId);
    }
}