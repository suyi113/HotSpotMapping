package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public final class JvmtiExport {

    private JvmtiExport() {}

    private static final long ANCHOR =
            VMStructsHelper.findStaticAddress("JvmtiExport::_can_access_local_variables");

    private static final long A_LOCAL_VARS =
            VMStructsHelper.findStaticAddress("JvmtiExport::_can_access_local_variables");
    private static final long A_HOTSWAP =
            VMStructsHelper.findStaticAddress("JvmtiExport::_can_hotswap_or_post_breakpoint");
    private static final long A_POST_EXC =
            VMStructsHelper.findStaticAddress("JvmtiExport::_can_post_on_exceptions");
    private static final long A_WALK =
            VMStructsHelper.findStaticAddress("JvmtiExport::_can_walk_any_space");


    private static final long OFF_CAN_ACCESS_LOCAL_VARIABLES          = 0;
    private static final long OFF_CAN_HOTSWAP_OR_POST_BREAKPOINT      = 1;
    private static final long OFF_CAN_MODIFY_ANY_CLASS                = 2;
    private static final long OFF_CAN_WALK_ANY_SPACE                  = 3;
    private static final long OFF_CAN_GET_SOURCE_DEBUG_EXTENSION      = 4;
    private static final long OFF_CAN_MAINTAIN_ORIGINAL_METHOD_ORDER  = 5;
    private static final long OFF_CAN_POST_INTERPRETER_EVENTS         = 6;
    private static final long OFF_CAN_POST_ON_EXCEPTIONS              = 7;
    private static final long OFF_CAN_POST_BREAKPOINT                 = 8;
    private static final long OFF_CAN_POST_FIELD_ACCESS               = 9;
    private static final long OFF_CAN_POST_FIELD_MODIFICATION         = 10;
    private static final long OFF_CAN_POST_METHOD_ENTRY               = 11;
    private static final long OFF_CAN_POST_METHOD_EXIT                = 12;
    private static final long OFF_CAN_POP_FRAME                       = 13;
    private static final long OFF_CAN_FORCE_EARLY_RETURN              = 14;
    private static final long OFF_EARLY_VMSTART_RECORDED              = 15;
    private static final long OFF_CAN_GET_OWNED_MONITOR_INFO          = 16;
    private static final long OFF_SHOULD_POST_SINGLE_STEP             = 17;
    private static final long OFF_SHOULD_POST_FIELD_ACCESS            = 18;
    private static final long OFF_SHOULD_POST_FIELD_MODIFICATION      = 19;
    private static final long OFF_SHOULD_POST_CLASS_LOAD              = 20;
    private static final long OFF_SHOULD_POST_CLASS_PREPARE           = 21;
    private static final long OFF_SHOULD_POST_CLASS_UNLOAD            = 22;
    private static final long OFF_SHOULD_POST_NATIVE_METHOD_BIND      = 23;
    private static final long OFF_SHOULD_POST_COMPILED_METHOD_LOAD    = 24;
    private static final long OFF_SHOULD_POST_COMPILED_METHOD_UNLOAD  = 25;
    private static final long OFF_SHOULD_POST_DYNAMIC_CODE_GENERATED  = 26;
    private static final long OFF_SHOULD_POST_MONITOR_CONTENDED_ENTER = 27;
    private static final long OFF_SHOULD_POST_MONITOR_CONTENDED_ENTERED = 28;
    private static final long OFF_SHOULD_POST_MONITOR_WAIT            = 29;
    private static final long OFF_SHOULD_POST_MONITOR_WAITED          = 30;
    private static final long OFF_SHOULD_POST_DATA_DUMP               = 31;
    private static final long OFF_SHOULD_POST_GC_START                = 32;
    private static final long OFF_SHOULD_POST_GC_FINISH               = 33;
    private static final long OFF_SHOULD_POST_ON_EXCEPTIONS           = 34;
    private static final long OFF_SHOULD_POST_THREAD_LIFE             = 35;
    private static final long OFF_SHOULD_POST_OBJECT_FREE             = 36;
    private static final long OFF_SHOULD_POST_RESOURCE_EXHAUSTED      = 37;
    private static final long OFF_SHOULD_CLEAN_UP_HEAP_OBJECTS        = 38;
    private static final long OFF_SHOULD_POST_VM_OBJECT_ALLOC         = 39;
    private static final long OFF_SHOULD_POST_SAMPLED_OBJECT_ALLOC    = 40;
    private static final long OFF_SHOULD_POST_CLASS_FILE_LOAD_HOOK = 42;

    private static boolean flag(long off) {
        return UnsafeUtils.unsafe.getByte(ANCHOR + off) != 0;
    }

    public static boolean canAccessLocalVariables()        { return flag(OFF_CAN_ACCESS_LOCAL_VARIABLES); }
    public static boolean canHotswapOrPostBreakpoint()     { return flag(OFF_CAN_HOTSWAP_OR_POST_BREAKPOINT); }
    public static boolean canModifyAnyClass()              { return flag(OFF_CAN_MODIFY_ANY_CLASS); }
    public static boolean canWalkAnySpace()                { return flag(OFF_CAN_WALK_ANY_SPACE); }
    public static boolean canGetSourceDebugExtension()     { return flag(OFF_CAN_GET_SOURCE_DEBUG_EXTENSION); }
    public static boolean canMaintainOriginalMethodOrder() { return flag(OFF_CAN_MAINTAIN_ORIGINAL_METHOD_ORDER); }
    public static boolean canPostInterpreterEvents()       { return flag(OFF_CAN_POST_INTERPRETER_EVENTS); }
    public static boolean canPostOnExceptions()            { return flag(OFF_CAN_POST_ON_EXCEPTIONS); }
    public static boolean canPostBreakpoint()              { return flag(OFF_CAN_POST_BREAKPOINT); }
    public static boolean canPostFieldAccess()             { return flag(OFF_CAN_POST_FIELD_ACCESS); }
    public static boolean canPostFieldModification()       { return flag(OFF_CAN_POST_FIELD_MODIFICATION); }
    public static boolean canPostMethodEntry()             { return flag(OFF_CAN_POST_METHOD_ENTRY); }
    public static boolean canPostMethodExit()              { return flag(OFF_CAN_POST_METHOD_EXIT); }
    public static boolean canPopFrame()                    { return flag(OFF_CAN_POP_FRAME); }
    public static boolean canForceEarlyReturn()            { return flag(OFF_CAN_FORCE_EARLY_RETURN); }
    public static boolean earlyVmstartRecorded()           { return flag(OFF_EARLY_VMSTART_RECORDED); }
    public static boolean canGetOwnedMonitorInfo()         { return flag(OFF_CAN_GET_OWNED_MONITOR_INFO); }

    public static boolean shouldPostSingleStep()           { return flag(OFF_SHOULD_POST_SINGLE_STEP); }
    public static boolean shouldPostFieldAccess()          { return flag(OFF_SHOULD_POST_FIELD_ACCESS); }
    public static boolean shouldPostFieldModification()    { return flag(OFF_SHOULD_POST_FIELD_MODIFICATION); }
    public static boolean shouldPostClassLoad()            { return flag(OFF_SHOULD_POST_CLASS_LOAD); }
    public static boolean shouldPostClassPrepare()         { return flag(OFF_SHOULD_POST_CLASS_PREPARE); }
    public static boolean shouldPostClassUnload()          { return flag(OFF_SHOULD_POST_CLASS_UNLOAD); }
    public static boolean shouldPostNativeMethodBind()     { return flag(OFF_SHOULD_POST_NATIVE_METHOD_BIND); }
    public static boolean shouldPostCompiledMethodLoad()   { return flag(OFF_SHOULD_POST_COMPILED_METHOD_LOAD); }
    public static boolean shouldPostCompiledMethodUnload() { return flag(OFF_SHOULD_POST_COMPILED_METHOD_UNLOAD); }
    public static boolean shouldPostDynamicCodeGenerated() { return flag(OFF_SHOULD_POST_DYNAMIC_CODE_GENERATED); }
    public static boolean shouldPostMonitorContendedEnter()   { return flag(OFF_SHOULD_POST_MONITOR_CONTENDED_ENTER); }
    public static boolean shouldPostMonitorContendedEntered() { return flag(OFF_SHOULD_POST_MONITOR_CONTENDED_ENTERED); }
    public static boolean shouldPostMonitorWait()          { return flag(OFF_SHOULD_POST_MONITOR_WAIT); }
    public static boolean shouldPostMonitorWaited()        { return flag(OFF_SHOULD_POST_MONITOR_WAITED); }
    public static boolean shouldPostDataDump()             { return flag(OFF_SHOULD_POST_DATA_DUMP); }
    public static boolean shouldPostGcStart()              { return flag(OFF_SHOULD_POST_GC_START); }
    public static boolean shouldPostGcFinish()             { return flag(OFF_SHOULD_POST_GC_FINISH); }
    public static boolean shouldPostOnExceptions()         { return flag(OFF_SHOULD_POST_ON_EXCEPTIONS); }
    public static boolean shouldPostThreadLife()           { return flag(OFF_SHOULD_POST_THREAD_LIFE); }
    public static boolean shouldPostObjectFree()           { return flag(OFF_SHOULD_POST_OBJECT_FREE); }
    public static boolean shouldPostResourceExhausted()    { return flag(OFF_SHOULD_POST_RESOURCE_EXHAUSTED); }
    public static boolean shouldCleanUpHeapObjects()       { return flag(OFF_SHOULD_CLEAN_UP_HEAP_OBJECTS); }
    public static boolean shouldPostVmObjectAlloc()        { return flag(OFF_SHOULD_POST_VM_OBJECT_ALLOC); }
    public static boolean shouldPostSampledObjectAlloc()   { return flag(OFF_SHOULD_POST_SAMPLED_OBJECT_ALLOC); }
    public static boolean shouldPostClassFileLoadHook()    { return flag(OFF_SHOULD_POST_CLASS_FILE_LOAD_HOOK);}

    public static long fieldAddress(long off) {
        return ANCHOR + off;
    }
}