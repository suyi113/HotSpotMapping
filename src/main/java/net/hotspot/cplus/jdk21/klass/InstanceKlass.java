package net.hotspot.cplus.jdk21.klass;

import net.hotspot.cplus.jdk21.codecache.BytecodeUnlinker;
import net.hotspot.cplus.jdk21.codecache.CodeBuilder;
import net.hotspot.cplus.jdk21.codecache.NMethod;
import net.hotspot.cplus.jdk21.core.OopDesc;
import net.hotspot.cplus.jdk21.core.VMStructsHelper;
import net.hotspot.cplus.jdk21.threads.JavaThread;

import net.hotspot.cplus.jdk21.core.UnsafeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.hotspot.cplus.jdk21.core.VMStructsHelper.findOffsetOrDefault;

public class InstanceKlass extends Klass {

    private static final long ANNOTATIONS_OFFSET                  = findOffsetOrDefault("InstanceKlass::_annotations",                  184L);
    private static final long PACKAGE_ENTRY_OFFSET                = findOffsetOrDefault("InstanceKlass::_package_entry",                192L);
    private static final long ARRAY_KLASSES_OFFSET                = findOffsetOrDefault("InstanceKlass::_array_klasses",                200L);
    private static final long CONSTANTS_OFFSET                    = findOffsetOrDefault("InstanceKlass::_constants",                    208L);
    private static final long INNER_CLASSES_OFFSET                = findOffsetOrDefault("InstanceKlass::_inner_classes",                216L);
    private static final long NEST_MEMBERS_OFFSET                 = findOffsetOrDefault("InstanceKlass::_nest_members",                 224L);
    private static final long NEST_HOST_OFFSET                    = findOffsetOrDefault("InstanceKlass::_nest_host",                    232L);
    private static final long PERMITTED_SUBCLASSES_OFFSET         = findOffsetOrDefault("InstanceKlass::_permitted_subclasses",        240L);
    private static final long RECORD_COMPONENTS_OFFSET            = findOffsetOrDefault("InstanceKlass::_record_components",           248L);
    private static final long SOURCE_DEBUG_EXTENSION_OFFSET       = findOffsetOrDefault("InstanceKlass::_source_debug_extension",      256L);
    private static final long NONSTATIC_FIELD_SIZE_OFFSET         = findOffsetOrDefault("InstanceKlass::_nonstatic_field_size",        264L);
    private static final long STATIC_FIELD_SIZE_OFFSET            = findOffsetOrDefault("InstanceKlass::_static_field_size",           268L);
    private static final long NONSTATIC_OOP_MAP_SIZE_OFFSET       = findOffsetOrDefault("InstanceKlass::_nonstatic_oop_map_size",      272L);
    private static final long ITABLE_LEN_OFFSET                   = findOffsetOrDefault("InstanceKlass::_itable_len",                  276L);
    private static final long NEST_HOST_INDEX_OFFSET              = findOffsetOrDefault("InstanceKlass::_nest_host_index",             280L);
    private static final long THIS_CLASS_INDEX_OFFSET             = findOffsetOrDefault("InstanceKlass::_this_class_index",            282L);
    private static final long STATIC_OOP_FIELD_COUNT_OFFSET       = findOffsetOrDefault("InstanceKlass::_static_oop_field_count",      284L);
    private static final long IDNUM_ALLOCATED_COUNT_OFFSET        = findOffsetOrDefault("InstanceKlass::_idnum_allocated_count",       286L);
    private static final long IS_MARKED_DEPENDENT_OFFSET          = findOffsetOrDefault("InstanceKlass::_is_marked_dependent",         288L);
    private static final long INIT_STATE_OFFSET                   = findOffsetOrDefault("InstanceKlass::_init_state",                  289L);
    private static final long REFERENCE_TYPE_OFFSET               = findOffsetOrDefault("InstanceKlass::_reference_type",              290L);
    private static final long MISC_FLAGS_OFFSET                   = findOffsetOrDefault("InstanceKlass::_misc_flags",                  292L);
    private static final long MISC_STATUS_OFFSET                  = findOffsetOrDefault("InstanceKlass::_misc_status",                 294L);
    private static final long INIT_THREAD_OFFSET                  = findOffsetOrDefault("InstanceKlass::_init_thread",                 296L);
    private static final long OOP_MAP_CACHE_OFFSET                = findOffsetOrDefault("InstanceKlass::_oop_map_cache",               304L);
    private static final long JNI_IDS_OFFSET                      = findOffsetOrDefault("InstanceKlass::_jni_ids",                     312L);
    private static final long METHODS_JMETHOD_IDS_OFFSET          = findOffsetOrDefault("InstanceKlass::_methods_jmethod_ids",         320L);
    private static final long DEP_CONTEXT_OFFSET                  = findOffsetOrDefault("InstanceKlass::_dep_context",                 328L);
    private static final long DEP_CONTEXT_LAST_CLEANED_OFFSET     = findOffsetOrDefault("InstanceKlass::_dep_context_last_cleaned",    336L);
    private static final long OSR_NMETHODS_HEAD_OFFSET            = findOffsetOrDefault("InstanceKlass::_osr_nmethods_head",           344L);
    private static final long BREAKPOINTS_OFFSET                  = findOffsetOrDefault("InstanceKlass::_breakpoints",                 352L);
    private static final long PREVIOUS_VERSIONS_OFFSET            = findOffsetOrDefault("InstanceKlass::_previous_versions",           360L);
    private static final long CACHED_CLASS_FILE_OFFSET            = findOffsetOrDefault("InstanceKlass::_cached_class_file",           368L);
    private static final long JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET = findOffsetOrDefault("InstanceKlass::_jvmti_cached_class_field_map",376L);
    private static final long METHODS_OFFSET                      = findOffsetOrDefault("InstanceKlass::_methods",                     384L);
    private static final long DEFAULT_METHODS_OFFSET              = findOffsetOrDefault("InstanceKlass::_default_methods",             392L);
    private static final long LOCAL_INTERFACES_OFFSET             = findOffsetOrDefault("InstanceKlass::_local_interfaces",            400L);
    private static final long TRANSITIVE_INTERFACES_OFFSET        = findOffsetOrDefault("InstanceKlass::_transitive_interfaces",       408L);
    private static final long METHOD_ORDERING_OFFSET              = findOffsetOrDefault("InstanceKlass::_method_ordering",             416L);
    private static final long DEFAULT_VTABLE_INDICES_OFFSET       = findOffsetOrDefault("InstanceKlass::_default_vtable_indices",      424L);
    private static final long FIELDINFO_STREAM_OFFSET             = findOffsetOrDefault("InstanceKlass::_fieldinfo_stream",            432L);
    private static final long FIELDS_STATUS_OFFSET                = findOffsetOrDefault("InstanceKlass::_fields_status",               440L);

    public static final int INIT_STATE_ALLOCATED            = 0;
    public static final int INIT_STATE_LOADED               = 1;
    public static final int INIT_STATE_LINKED               = 2;
    public static final int INIT_STATE_BEING_INITIALIZED    = 3;
    public static final int INIT_STATE_FULLY_INITIALIZED    = 4;
    public static final int INIT_STATE_INITIALIZATION_ERROR = 5;

    public static final int REF_NONE                = 0;
    public static final int REF_OTHER               = 1;
    public static final int REF_STRONG              = 2;
    public static final int REF_SOFT                = 3;
    public static final int REF_WEAK                = 4;
    public static final int REF_FINAL               = 5;
    public static final int REF_PHANTOM             = 6;

    public static final int MISC_REWRITTEN                            = 1 << 0;
    public static final int MISC_HAS_NONSTATIC_FIELDS                 = 1 << 1;
    public static final int MISC_SHOULD_VERIFY_CLASS                  = 1 << 2;
    public static final int MISC_IS_CONTENDED                         = 1 << 3;
    public static final int MISC_HAS_NONSTATIC_CONCRETE_METHODS       = 1 << 4;
    public static final int MISC_DECLARES_NONSTATIC_CONCRETE_METHODS  = 1 << 5;
    public static final int MISC_SHARED_LOADING_FAILED                = 1 << 6;
    public static final int MISC_IS_SHARED_BOOT_CLASS                 = 1 << 7;
    public static final int MISC_IS_SHARED_PLATFORM_CLASS             = 1 << 8;
    public static final int MISC_IS_SHARED_APP_CLASS                  = 1 << 9;
    public static final int MISC_HAS_CONTENDED_ANNOTATIONS            = 1 << 10;
    public static final int MISC_HAS_LOCALVARIABLE_TABLE              = 1 << 11;
    public static final int MISC_HAS_MIRANDA_METHODS                  = 1 << 12;
    public static final int MISC_HAS_VANILLA_CONSTRUCTOR              = 1 << 13;
    public static final int MISC_HAS_FINAL_METHOD                     = 1 << 14;

    public static final int STATUS_IS_BEING_REDEFINED  = 1 << 0;
    public static final int STATUS_HAS_RESOLVED_METHODS = 1 << 1;
    public static final int STATUS_HAS_BEEN_REDEFINED  = 1 << 2;
    public static final int STATUS_IS_SCRATCH_CLASS    = 1 << 3;
    public static final int STATUS_IS_MARKED_DEPENDENT = 1 << 4;

    public long annotations;
    public long packageEntry;
    public long arrayKlasses;
    public long constants;
    public long innerClasses;
    public long nestMembers;
    public long nestHost;
    public long permittedSubclasses;
    public long recordComponents;
    public long sourceDebugExtension;
    public int  nonstaticFieldSize;
    public int  staticFieldSize;
    public int  nonstaticOopMapSize;
    public int  itableLen;
    public short nestHostIndex;
    public short thisClassIndex;
    public short staticOopFieldCount;
    public short idnumAllocatedCount;
    public boolean markedDependent;
    public byte initState;
    public byte referenceType;
    public short miscFlags;
    public byte miscStatus;
    public long initThread;
    public long oopMapCache;
    public long jniIds;
    public long methodsJmethodIds;
    public long depContext;
    public long depContextLastCleaned;
    public long osrNmethodsHead;
    public long breakpoints;
    public long previousVersions;
    public long cachedClassFile;
    public long jvmtiCachedClassFieldMap;
    public long methods;
    public long defaultMethods;
    public long localInterfaces;
    public long transitiveInterfaces;
    public long methodOrdering;
    public long defaultVtableIndices;
    public long fieldinfoStream;
    public long fieldsStatus;

    public InstanceKlass(long address) {
        super(address);
        readFields();
        restoreExpandedState();
    }

    public InstanceKlass(Class<?> clazz) {
        super(clazz);
        readFields();
        restoreExpandedState();
    }

    public void readFields() {
        long base = getAddress();
        annotations                  = UnsafeUtils.unsafe.getLong(base + ANNOTATIONS_OFFSET);
        packageEntry                 = UnsafeUtils.unsafe.getLong(base + PACKAGE_ENTRY_OFFSET);
        arrayKlasses                 = UnsafeUtils.unsafe.getLong(base + ARRAY_KLASSES_OFFSET);
        constants                    = UnsafeUtils.unsafe.getLong(base + CONSTANTS_OFFSET);
        innerClasses                 = UnsafeUtils.unsafe.getLong(base + INNER_CLASSES_OFFSET);
        nestMembers                  = UnsafeUtils.unsafe.getLong(base + NEST_MEMBERS_OFFSET);
        nestHost                     = UnsafeUtils.unsafe.getLong(base + NEST_HOST_OFFSET);
        permittedSubclasses          = UnsafeUtils.unsafe.getLong(base + PERMITTED_SUBCLASSES_OFFSET);
        recordComponents             = UnsafeUtils.unsafe.getLong(base + RECORD_COMPONENTS_OFFSET);
        sourceDebugExtension         = UnsafeUtils.unsafe.getLong(base + SOURCE_DEBUG_EXTENSION_OFFSET);
        nonstaticFieldSize           = UnsafeUtils.unsafe.getInt(base + NONSTATIC_FIELD_SIZE_OFFSET);
        staticFieldSize              = UnsafeUtils.unsafe.getInt(base + STATIC_FIELD_SIZE_OFFSET);
        nonstaticOopMapSize          = UnsafeUtils.unsafe.getInt(base + NONSTATIC_OOP_MAP_SIZE_OFFSET);
        itableLen                    = UnsafeUtils.unsafe.getInt(base + ITABLE_LEN_OFFSET);
        nestHostIndex                = UnsafeUtils.unsafe.getShort(base + NEST_HOST_INDEX_OFFSET);
        thisClassIndex               = UnsafeUtils.unsafe.getShort(base + THIS_CLASS_INDEX_OFFSET);
        staticOopFieldCount          = UnsafeUtils.unsafe.getShort(base + STATIC_OOP_FIELD_COUNT_OFFSET);
        idnumAllocatedCount          = UnsafeUtils.unsafe.getShort(base + IDNUM_ALLOCATED_COUNT_OFFSET);
        markedDependent              = UnsafeUtils.unsafe.getByte(base + IS_MARKED_DEPENDENT_OFFSET) != 0;
        initState                    = UnsafeUtils.unsafe.getByte(base + INIT_STATE_OFFSET);
        referenceType                = UnsafeUtils.unsafe.getByte(base + REFERENCE_TYPE_OFFSET);
        miscFlags                    = UnsafeUtils.unsafe.getShort(base + MISC_FLAGS_OFFSET);
        miscStatus                   = UnsafeUtils.unsafe.getByte(base + MISC_STATUS_OFFSET);
        initThread                   = UnsafeUtils.unsafe.getLong(base + INIT_THREAD_OFFSET);
        oopMapCache                  = UnsafeUtils.unsafe.getLong(base + OOP_MAP_CACHE_OFFSET);
        jniIds                       = UnsafeUtils.unsafe.getLong(base + JNI_IDS_OFFSET);
        methodsJmethodIds            = UnsafeUtils.unsafe.getLong(base + METHODS_JMETHOD_IDS_OFFSET);
        depContext                   = UnsafeUtils.unsafe.getLong(base + DEP_CONTEXT_OFFSET);
        depContextLastCleaned        = UnsafeUtils.unsafe.getLong(base + DEP_CONTEXT_LAST_CLEANED_OFFSET);
        osrNmethodsHead              = UnsafeUtils.unsafe.getLong(base + OSR_NMETHODS_HEAD_OFFSET);
        breakpoints                  = UnsafeUtils.unsafe.getLong(base + BREAKPOINTS_OFFSET);
        previousVersions             = UnsafeUtils.unsafe.getLong(base + PREVIOUS_VERSIONS_OFFSET);
        cachedClassFile              = UnsafeUtils.unsafe.getLong(base + CACHED_CLASS_FILE_OFFSET);
        jvmtiCachedClassFieldMap     = UnsafeUtils.unsafe.getLong(base + JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET);
        methods                      = UnsafeUtils.unsafe.getLong(base + METHODS_OFFSET);
        defaultMethods               = UnsafeUtils.unsafe.getLong(base + DEFAULT_METHODS_OFFSET);
        localInterfaces              = UnsafeUtils.unsafe.getLong(base + LOCAL_INTERFACES_OFFSET);
        transitiveInterfaces         = UnsafeUtils.unsafe.getLong(base + TRANSITIVE_INTERFACES_OFFSET);
        methodOrdering               = UnsafeUtils.unsafe.getLong(base + METHOD_ORDERING_OFFSET);
        defaultVtableIndices         = UnsafeUtils.unsafe.getLong(base + DEFAULT_VTABLE_INDICES_OFFSET);
        fieldinfoStream              = UnsafeUtils.unsafe.getLong(base + FIELDINFO_STREAM_OFFSET);
        fieldsStatus                 = UnsafeUtils.unsafe.getLong(base + FIELDS_STATUS_OFFSET);
    }

    public void refresh() {
        readFields();
    }

    @Override
    public boolean isInstanceKlass() {
        return true;
    }

    public boolean isInstance() {
        return true;
    }

    public long annotations()            { return annotations; }
    public long packageEntry()           { return packageEntry; }
    public long arrayKlasses()           { return arrayKlasses; }
    public long constants()              { return constants; }
    public long innerClasses()           { return innerClasses; }
    public long nestMembers()            { return nestMembers; }
    public long nestHost()               { return nestHost; }
    public long permittedSubclasses()    { return permittedSubclasses; }
    public long recordComponents()       { return recordComponents; }
    public long sourceDebugExtension()   { return sourceDebugExtension; }
    public int  nonstaticFieldSize()     { return nonstaticFieldSize; }
    public int  staticFieldSize()        { return staticFieldSize; }
    public int  nonstaticOopMapSize()    { return nonstaticOopMapSize; }
    public int  itableLen()              { return itableLen; }
    public short nestHostIndex()         { return nestHostIndex; }
    public short thisClassIndex()        { return thisClassIndex; }
    public short staticOopFieldCount()   { return staticOopFieldCount; }
    public short idnumAllocatedCount()   { return idnumAllocatedCount; }
    public boolean isMarkedDependentFlag() { return markedDependent; }
    public byte initState()              { return initState; }
    public byte referenceType()          { return referenceType; }
    public short miscFlags()             { return miscFlags; }
    public byte miscStatus()             { return miscStatus; }
    public long initThread()             { return initThread; }
    public long oopMapCache()            { return oopMapCache; }
    public long jniIds()                 { return jniIds; }
    public long methodsJmethodIds()      { return methodsJmethodIds; }
    public long depContext()             { return depContext; }
    public long depContextLastCleaned()  { return depContextLastCleaned; }
    public long osrNmethodsHead()        { return osrNmethodsHead; }
    public long breakpoints()            { return breakpoints; }
    public long previousVersions()       { return previousVersions; }
    public long cachedClassFile()        { return cachedClassFile; }
    public long jvmtiCachedClassFieldMap() { return jvmtiCachedClassFieldMap; }
    public long methods()                { return methods; }
    public long defaultMethods()         { return defaultMethods; }
    public long localInterfaces()        { return localInterfaces; }
    public long transitiveInterfaces()    { return transitiveInterfaces; }
    public long methodOrdering()         { return methodOrdering; }
    public long defaultVtableIndices()   { return defaultVtableIndices; }
    public long fieldinfoStream()        { return fieldinfoStream; }
    public long fieldsStatus()           { return fieldsStatus; }

    public ConstantPool getConstantPool() {
        return constants == 0 ? null : new ConstantPool(constants);
    }

    public boolean isLoaded()            { return initState >= INIT_STATE_LOADED; }
    public boolean isLinked()            { return initState >= INIT_STATE_LINKED; }
    public boolean isInitialized()       { return initState == INIT_STATE_FULLY_INITIALIZED; }
    public boolean isNotInitialized()    { return initState <  INIT_STATE_BEING_INITIALIZED; }
    public boolean isBeingInitialized()  { return initState == INIT_STATE_BEING_INITIALIZED; }
    public boolean isInErrorState()      { return initState == INIT_STATE_INITIALIZATION_ERROR; }

    public boolean isRewritten()                            { return (miscFlags & MISC_REWRITTEN) != 0; }
    public boolean hasNonstaticFields()                     { return (miscFlags & MISC_HAS_NONSTATIC_FIELDS) != 0; }
    public boolean shouldVerifyClass()                      { return (miscFlags & MISC_SHOULD_VERIFY_CLASS) != 0; }
    public boolean isContended()                            { return (miscFlags & MISC_IS_CONTENDED) != 0; }
    public boolean hasNonstaticConcreteMethods()            { return (miscFlags & MISC_HAS_NONSTATIC_CONCRETE_METHODS) != 0; }
    public boolean declaresNonstaticConcreteMethods()       { return (miscFlags & MISC_DECLARES_NONSTATIC_CONCRETE_METHODS) != 0; }
    public boolean sharedLoadingFailed()                    { return (miscFlags & MISC_SHARED_LOADING_FAILED) != 0; }
    public boolean isSharedBootClass()                      { return (miscFlags & MISC_IS_SHARED_BOOT_CLASS) != 0; }
    public boolean isSharedPlatformClass()                  { return (miscFlags & MISC_IS_SHARED_PLATFORM_CLASS) != 0; }
    public boolean isSharedAppClass()                       { return (miscFlags & MISC_IS_SHARED_APP_CLASS) != 0; }
    public boolean isSharedUnregisteredClass()              { return (miscFlags & (MISC_IS_SHARED_BOOT_CLASS | MISC_IS_SHARED_PLATFORM_CLASS | MISC_IS_SHARED_APP_CLASS)) == 0; }
    public boolean hasContendedAnnotations()                { return (miscFlags & MISC_HAS_CONTENDED_ANNOTATIONS) != 0; }
    public boolean hasLocalvariableTable()                  { return (miscFlags & MISC_HAS_LOCALVARIABLE_TABLE) != 0; }
    public boolean hasMirandaMethods()                      { return (miscFlags & MISC_HAS_MIRANDA_METHODS) != 0; }
    public boolean hasVanillaConstructor()                  { return (miscFlags & MISC_HAS_VANILLA_CONSTRUCTOR) != 0; }
    public boolean hasFinalMethod()                         { return (miscFlags & MISC_HAS_FINAL_METHOD) != 0; }

    public boolean isBeingRedefined()      { return (miscStatus & STATUS_IS_BEING_REDEFINED) != 0; }
    public boolean hasResolvedMethods()    { return (miscStatus & STATUS_HAS_RESOLVED_METHODS) != 0; }
    public boolean hasBeenRedefined()      { return (miscStatus & STATUS_HAS_BEEN_REDEFINED) != 0; }
    public boolean isScratchClass()        { return (miscStatus & STATUS_IS_SCRATCH_CLASS) != 0; }
    public boolean isMarkedDependent()     { return (miscStatus & STATUS_IS_MARKED_DEPENDENT) != 0; }

    public MethodArray methodsArray() {
        if (methods == 0) return new MethodArray(0);
        return new MethodArray(methods);
    }

    public Method[] getMethods() {
        return methodsArray().toMethods();
    }

    public int methodsCount() {
        return methods == 0 ? 0 : methodsArray().length;
    }

    public Method findMethod(String name, String signature) {
        if (methods == 0) return null;
        for (Method m : methodsArray().toMethods()) {
            if (name.equals(m.name()) && signature.equals(m.signature())) {
                return m;
            }
        }
        return null;
    }

    public Method findMethod(String name) {
        if (methods == 0) return null;
        for (Method m : methodsArray().toMethods()) {
            if (name.equals(m.name())) {
                return m;
            }
        }
        return null;
    }

    public boolean hasNativeMethod() {
        if (methods == 0) return false;
        for (Method m : methodsArray().toMethods()) {
            if (m.isNative()) return true;
        }
        return false;
    }

    public int numJavaFields() {
        U5Reader r = openFieldStream();
        return r == null ? 0 : r.next();
    }

    public int numInjectedFields() {
        U5Reader r = openFieldStream();
        if (r == null) return 0;
        r.next();
        return r.next();
    }

    public int fieldCount() {
        return numJavaFields() + numInjectedFields();
    }

    public FieldInfo[] getFields() {
        U5Reader r = openFieldStream();
        if (r == null) return new FieldInfo[0];

        int javaFields = r.next();
        int injectedFields = r.next();
        int total = javaFields + injectedFields;

        FieldInfo[] out = new FieldInfo[total];
        for (int i = 0; i < total && r.hasMore(); i++) {
            FieldInfo f = new FieldInfo();
            f.index          = i;
            f.nameIndex      = r.next();
            f.signatureIndex = r.next();
            f.offset         = r.next();
            f.accessFlags    = r.next();
            f.fieldFlags     = r.next();
            if ((f.fieldFlags & FieldInfo.FF_INITIALIZED) != 0) f.initializerIndex      = r.next();
            if ((f.fieldFlags & FieldInfo.FF_GENERIC) != 0)     f.genericSignatureIndex = r.next();
            if ((f.fieldFlags & FieldInfo.FF_CONTENDED) != 0)   f.contentionGroup       = r.next();
            out[i] = f;
        }
        return out;
    }

    public FieldInfo findField(String name, String signature) {
        ConstantPool cp = getConstantPool();
        for (FieldInfo f : getFields()) {
            if (name.equals(f.name(cp)) && signature.equals(f.signature(cp))) {
                return f;
            }
        }
        return null;
    }

    private U5Reader openFieldStream() {
        if (fieldinfoStream == 0) return null;
        int length = UnsafeUtils.unsafe.getInt(fieldinfoStream);
        if (length <= 0) return null;

        byte[] data = new byte[length];
        long base = fieldinfoStream + 4L;
        for (int i = 0; i < length; i++) {
            data[i] = UnsafeUtils.unsafe.getByte(base + i);
        }
        return new U5Reader(data);
    }

    private static final class U5Reader {
        private final byte[] data;
        private int pos;

        U5Reader(byte[] data) {
            this.data = data;
        }

        boolean hasMore() {
            return pos < data.length;
        }

        int next() {
            if (pos >= data.length) return 0;
            int b0 = data[pos++] & 0xFF;
            int sum = b0 - 1;
            if (sum < 191) {
                return sum;
            }
            int shift = 6;
            for (int i = 1; ; i++) {
                int b = data[pos++] & 0xFF;
                sum += (b - 1) << shift;
                if (b < 192 || i == 4) {
                    return sum;
                }
                shift += 6;
            }
        }
    }

    public boolean isRecord() {
        return recordComponents != 0;
    }

    private static final int HEADER_SIZE   = 72;
    private static final int TAGS_HEADER   = 4;
    private static final int CP_OFF_LENGTH = 60;
    private static final int CP_OFF_TAGS   = 8;

    private static final int TAG_UTF8               = 1;
    private static final int TAG_CLASS              = 7;
    private static final int TAG_STRING             = 8;
    private static final int TAG_FIELDREF           = 9;
    private static final int TAG_METHODREF          = 10;
    private static final int TAG_INTERFACEMETHODREF = 11;
    private static final int TAG_NAMEANDTYPE        = 12;

    private static final long OOP_ARRAY_LENGTH_OFFSET = 12L;
    private static final long OOP_ARRAY_DATA_OFFSET   = 16L;

    public static final int BC_GETSTATIC = 0xB2;
    public static final int BC_PUTSTATIC = 0xB3;
    public static final int BC_GETFIELD  = 0xB4;
    public static final int BC_PUTFIELD  = 0xB5;

    public static final int TOS_STATE_SHIFT      = 28;
    public static final int IS_FIELD_ENTRY_SHIFT = 26;
    public static final int IS_FINAL_SHIFT       = 22;
    public static final int IS_VOLATILE_SHIFT    = 21;
    public static final int FIELD_INDEX_MASK     = 0xFFFF;

    private long expandedCpAddr              = 0L;
    private long expandedTagsAddr            = 0L;
    private int  expandedLength              = 0;
    private long expandedResolvedKlassesAddr = 0L;
    private int  expandedResolvedKlassesLength = 0;

    private final Map<String, Long> symbolCache = new HashMap<>();

    private static final ConcurrentHashMap<Long, long[]>  EXPANDED   = new ConcurrentHashMap<>();
    private static final List<Object>                     PINNED     = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Long, Integer> CACHE_NEXT = new ConcurrentHashMap<>();

    private void restoreExpandedState() {
        long[] st = EXPANDED.get(getAddress());
        if (st != null) {
            expandedCpAddr                = st[0];
            expandedTagsAddr              = st[1];
            expandedLength                = (int) st[2];
            expandedResolvedKlassesAddr   = st[3];
            expandedResolvedKlassesLength = (int) st[4];
        }
    }

    private void publishExpandedState() {
        EXPANDED.put(getAddress(), new long[]{
                expandedCpAddr,
                expandedTagsAddr,
                expandedLength,
                expandedResolvedKlassesAddr,
                expandedResolvedKlassesLength
        });
    }

    public long currentCpAddress() {
        return expandedCpAddr != 0L ? expandedCpAddr : constants;
    }

    public void ensureCpCapacity(int extraSlots) {
        if (expandedCpAddr != 0L) return;
        doFirstExpand(Math.max(extraSlots, 32));
        readFields();
    }

    public int addUtf8(String s, Class<?>... donors) {
        ensureCpCapacity(1);
        int existing = findExistingUtf8(s);
        if (existing > 0) return existing;

        long symAddr = resolveSymbol(s, donors);
        if (symAddr == 0) {
            throw new IllegalStateException("Cannot find string in JVM symbol table: " + s);
        }
        int idx = expandedLength;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_UTF8);
        UnsafeUtils.unsafe.putLong(expandedCpAddr + HEADER_SIZE + idx * 8L, symAddr);
        bumpLength();
        readFields();
        return idx;
    }

    private void ensureResolvedKlassesCapacity(int rkIndex) {
        if (expandedResolvedKlassesAddr == 0L) {
            long oldArr = UnsafeUtils.unsafe.getLong(expandedCpAddr + 40L);
            int  oldLen = (oldArr == 0) ? 0 : UnsafeUtils.unsafe.getInt(oldArr);

            int newLen = Math.max(oldLen, rkIndex + 1) + 32;
            long newSize = 8L + (long) newLen * 8L;
            long newAddr = UnsafeUtils.unsafe.allocateMemory(newSize);
            for (long i = 0; i < newSize; i++) {
                UnsafeUtils.unsafe.putByte(newAddr + i, (byte) 0);
            }
            UnsafeUtils.unsafe.putInt(newAddr, newLen);
            if (oldArr != 0) {
                int copyLen = Math.min(oldLen, newLen);
                for (int i = 0; i < copyLen; i++) {
                    long v = UnsafeUtils.unsafe.getLong(oldArr + 8L + (long) i * 8L);
                    UnsafeUtils.unsafe.putLong(newAddr + 8L + (long) i * 8L, v);
                }
            }
            UnsafeUtils.unsafe.putLong(expandedCpAddr + 40L, newAddr);
            expandedResolvedKlassesAddr = newAddr;
            expandedResolvedKlassesLength = oldLen;
            publishExpandedState();
            return;
        }

        int currentLen = UnsafeUtils.unsafe.getInt(expandedResolvedKlassesAddr);
        if (rkIndex < currentLen) return;

        int newLen = Math.max(currentLen * 2, rkIndex + 32);
        long newSize = 8L + (long) newLen * 8L;
        long newAddr = UnsafeUtils.unsafe.allocateMemory(newSize);
        for (long i = 0; i < newSize; i++) {
            UnsafeUtils.unsafe.putByte(newAddr + i, (byte) 0);
        }
        UnsafeUtils.unsafe.putInt(newAddr, newLen);
        for (int i = 0; i < currentLen; i++) {
            long v = UnsafeUtils.unsafe.getLong(expandedResolvedKlassesAddr + 8L + (long) i * 8L);
            UnsafeUtils.unsafe.putLong(newAddr + 8L + (long) i * 8L, v);
        }
        UnsafeUtils.unsafe.putLong(expandedCpAddr + 40L, newAddr);
        expandedResolvedKlassesAddr = newAddr;
        publishExpandedState();
    }

    private void putResolvedKlass(int rkIndex, long klassAddr) {
        if (expandedResolvedKlassesAddr == 0L) {
            throw new IllegalStateException("rkArray not initialized");
        }
        UnsafeUtils.unsafe.putLong(
                expandedResolvedKlassesAddr + 8L + (long) rkIndex * 8L,
                klassAddr);
        if (rkIndex >= expandedResolvedKlassesLength) {
            expandedResolvedKlassesLength = rkIndex + 1;
        }
        publishExpandedState();
    }

    public int addClass(String className, Class<?>... donors) {
        long klassAddr = findKlassByDonors(className, donors);
        if (klassAddr == 0) {
            throw new IllegalStateException("Class not found: " + className);
        }
        ensureCpCapacity(1);

        int existing = findExistingClassByKlass(klassAddr);
        if (existing > 0) return existing;

        int nameIdx = addUtf8(className, donors);

        int rkIndex = expandedResolvedKlassesLength;
        ensureResolvedKlassesCapacity(rkIndex);
        putResolvedKlass(rkIndex, klassAddr);

        int idx = expandedLength;
        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_CLASS);
        UnsafeUtils.unsafe.putShort(base,      (short) rkIndex);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) nameIdx);

        bumpLength();
        readFields();
        return idx;
    }

    private long findKlassByDonors(String className, Class<?>... donors) {
        for (Class<?> c : donors) {
            if (c == null) continue;
            if (c.getName().replace('.', '/').equals(className)) {
                return new InstanceKlass(c).getAddress();
            }
        }
        return 0;
    }

    private int findExistingClassByKlass(long klassAddr) {
        ConstantPool oldCp = new ConstantPool(constants);
        long oldResolved = UnsafeUtils.unsafe.getLong(oldCp.getAddress() + 40L);
        int idx = findClassInRange(oldCp, oldResolved, 1, oldCp.length, klassAddr);
        if (idx > 0) return idx;

        if (expandedResolvedKlassesAddr != 0L) {
            return findClassInRange(null, expandedResolvedKlassesAddr,
                    1, expandedLength, klassAddr);
        }
        return -1;
    }

    private int findClassInRange(ConstantPool cp, long resolvedKlassesAddr,
                                 int from, int to, long klassAddr) {
        if (resolvedKlassesAddr == 0L) return -1;
        long tagsData = (cp != null)
                ? cp.tags + TAGS_HEADER
                : expandedTagsAddr + TAGS_HEADER;
        for (int i = from; i < to; i++) {
            int tag = UnsafeUtils.unsafe.getByte(tagsData + i) & 0xFF;
            if (tag == TAG_CLASS) {
                long slot = (cp != null)
                        ? cp.slotAt(i)
                        : UnsafeUtils.unsafe.getLong(expandedCpAddr + HEADER_SIZE + i * 8L);
                int rkIdx = (int) (slot & 0xFFFF);
                long k = UnsafeUtils.unsafe.getLong(resolvedKlassesAddr + 8L + (long) rkIdx * 8L);
                if (k == klassAddr) {
                    return i;
                }
            }
            if (tag == 5 || tag == 6) i++;
        }
        return -1;
    }

    public int addNameAndType(String name, String desc, Class<?>... donors) {
        int nameIdx = addUtf8(name, donors);
        int descIdx = addUtf8(desc, donors);
        ensureCpCapacity(1);

        int existing = findExistingNat(nameIdx, descIdx);
        if (existing > 0) return existing;

        int idx = expandedLength;
        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_NAMEANDTYPE);
        UnsafeUtils.unsafe.putShort(base,      (short) nameIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) descIdx);
        bumpLength();
        readFields();
        return idx;
    }

    public int addFieldref(String owner, String name, String desc, Class<?>... donors) {
        int classIdx = addClass(owner, donors);
        int natIdx   = addNameAndType(name, desc, donors);
        ensureCpCapacity(1);
        int idx = expandedLength;
        ensureCpCacheHasIndex(idx);
        initCacheEntryIndices(idx, idx);
        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_FIELDREF);
        UnsafeUtils.unsafe.putShort(base,      (short) classIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) natIdx);
        bumpLength();
        readFields();
        return idx;
    }

    public int addMethodref(String owner, String name, String desc, Class<?>... donors) {
        int classIdx = addClass(owner, donors);
        int natIdx   = addNameAndType(name, desc, donors);
        ensureCpCapacity(1);
        int existing = findExistingRef(TAG_METHODREF, classIdx, natIdx);
        if (existing > 0) return existing;

        int idx = expandedLength;
        ensureCpCacheHasIndex(idx);
        initCacheEntryIndices(idx, idx);

        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_METHODREF);
        UnsafeUtils.unsafe.putShort(base,      (short) classIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) natIdx);
        bumpLength();
        readFields();
        return idx;
    }

    public int addInterfaceMethodref(String owner, String name, String desc, Class<?>... donors) {
        int classIdx = addClass(owner, donors);
        int natIdx   = addNameAndType(name, desc, donors);
        ensureCpCapacity(1);
        int existing = findExistingRef(TAG_INTERFACEMETHODREF, classIdx, natIdx);
        if (existing > 0) return existing;

        int idx = expandedLength;
        ensureCpCacheHasIndex(idx);
        initCacheEntryIndices(idx, idx);

        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_INTERFACEMETHODREF);
        UnsafeUtils.unsafe.putShort(base,      (short) classIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) natIdx);
        bumpLength();
        readFields();
        return idx;
    }

    private void ensureCpCacheHasIndex(int idx) {
        ConstantPool cp = new ConstantPool(currentCpAddress());
        ConstantPoolCache cpc = cp.getCache();
        if (cpc == null) {
            throw new IllegalStateException("CPC is null");
        }
        if (idx >= cpc.length) {
            ensureCpCacheCapacity(idx - cpc.length + 1);
        }
    }

    private void initCacheEntryIndices(int cpcIdx, int cpIdx) {
        ConstantPool cp = new ConstantPool(currentCpAddress());
        ConstantPoolCache cpc = cp.getCache();
        if (cpc == null) {
            throw new IllegalStateException("CPC is null");
        }
        if (cpcIdx >= cpc.length) {
            ensureCpCacheCapacity(cpcIdx - cpc.length + 1);
            cpc = new ConstantPool(currentCpAddress()).getCache();
        }
        long entryAddr = cpc.entryAddr(cpcIdx);
        for (int k = 0; k < ConstantPoolCache.ENTRY_SIZE; k++) {
            UnsafeUtils.unsafe.putByte(entryAddr + k, (byte) 0);
        }
        UnsafeUtils.unsafe.putInt(
                entryAddr + ConstantPoolCache.ENTRY_INDICES_OFFSET,
                cpIdx & 0xFFFF);
    }

    public int addCacheEntry(int cpIndex) {
        ConstantPool cp = new ConstantPool(currentCpAddress());
        ConstantPoolCache cpc = cp.getCache();
        if (cpc == null) {
            throw new IllegalStateException("ConstantPoolCache is null");
        }

        ConstantPoolCache finalCpc = cpc;
        int idx = CACHE_NEXT.computeIfAbsent(getAddress(), k -> finalCpc.length);
        if (idx >= cpc.length) {
            ensureCpCacheCapacity(idx - cpc.length + 32);
            cpc = new ConstantPool(currentCpAddress()).getCache();
        }

        long addr = cpc.entryAddr(idx);
        for (int i = 0; i < (int) ConstantPoolCache.ENTRY_SIZE; i++) {
            UnsafeUtils.unsafe.putByte(addr + i, (byte) 0);
        }
        UnsafeUtils.unsafe.putShort(
                addr + ConstantPoolCache.ENTRY_INDICES_OFFSET,
                (short) (cpIndex & 0xFFFF));

        CACHE_NEXT.put(getAddress(), idx + 1);
        return idx;
    }

    public int addResolvedFieldEntry(
            int cpIndex,
            long fieldHolderKlass,
            int fieldOffset,
            int fieldIndex,
            int tosState,
            boolean isStatic,
            boolean isFinal,
            boolean isVolatile) {

        ConstantPool cp = new ConstantPool(currentCpAddress());
        ConstantPoolCache cpc = cp.getCache();
        if (cpc == null) {
            throw new IllegalStateException("ConstantPoolCache is null");
        }

        ConstantPoolCache finalCpc = cpc;
        int idx = CACHE_NEXT.computeIfAbsent(getAddress(), k -> finalCpc.length);
        if (idx >= cpc.length) {
            ensureCpCacheCapacity(idx - cpc.length + 32);
            cpc = new ConstantPool(currentCpAddress()).getCache();
        }

        long addr = cpc.entryAddr(idx);
        for (int i = 0; i < (int) ConstantPoolCache.ENTRY_SIZE; i++) {
            UnsafeUtils.unsafe.putByte(addr + i, (byte) 0);
        }

        UnsafeUtils.unsafe.putAddress(
                addr + ConstantPoolCache.ENTRY_F1_OFFSET, fieldHolderKlass);
        UnsafeUtils.unsafe.putLong(
                addr + ConstantPoolCache.ENTRY_F2_OFFSET, fieldOffset);

        int flags = (tosState << TOS_STATE_SHIFT)
                | (1 << IS_FIELD_ENTRY_SHIFT)
                | ((isFinal ? 1 : 0) << IS_FINAL_SHIFT)
                | ((isVolatile ? 1 : 0) << IS_VOLATILE_SHIFT)
                | (fieldIndex & FIELD_INDEX_MASK);
        UnsafeUtils.unsafe.putInt(addr + ConstantPoolCache.ENTRY_FLAGS_OFFSET, flags);

        int getCode = isStatic ? BC_GETSTATIC : BC_GETFIELD;
        int putCode = isFinal ? 0 : (isStatic ? BC_PUTSTATIC : BC_PUTFIELD);
        int indices = ((putCode & 0xFF) << 24)
                | ((getCode & 0xFF) << 16)
                | (cpIndex & 0xFFFF);
        UnsafeUtils.unsafe.putInt(addr + ConstantPoolCache.ENTRY_INDICES_OFFSET, indices);

        CACHE_NEXT.put(getAddress(), idx + 1);
        return idx;
    }

    public void commitCpChanges() {
        if (expandedCpAddr == 0L) {
            return;
        }

        ConstantPool      preCp  = new ConstantPool(expandedCpAddr);
        ConstantPoolCache preCpc = preCp.getCache();
        int oldCpcLen = preCpc == null ? 0 : preCpc.length;

        if (oldCpcLen < expandedLength) {
            ensureCpCacheCapacity(expandedLength - oldCpcLen);
        }

        ConstantPool      oldCp     = new ConstantPool(constants);
        long              oldCpAddr = oldCp.getAddress();
        ConstantPoolCache oldCpc    = oldCp.getCache();

        UnsafeUtils.unsafe.putLong(getAddress() + CONSTANTS_OFFSET, expandedCpAddr);
        this.constants = expandedCpAddr;

        for (Method m : methodsArray().toMethods()) {
            if (m.constMethod != 0) {
                UnsafeUtils.unsafe.putLong(
                        m.constMethod + ConstMethod.constantsFieldOffset(),
                        expandedCpAddr);
            }
        }

        ConstantPool      newCp  = new ConstantPool(currentCpAddress());
        ConstantPoolCache newCpc = newCp.getCache();

        if (newCpc != null) {
            UnsafeUtils.unsafe.putLong(
                    newCpc.getAddress() + ConstantPoolCache.CONSTANT_POOL_OFFSET,
                    expandedCpAddr);
        }

        long oldCacheFieldAddr = oldCpAddr + ConstantPool.CACHE_OFFSET;
        if (newCpc != null) {
            UnsafeUtils.unsafe.putLong(oldCacheFieldAddr, newCpc.getAddress());
        } else {
            UnsafeUtils.unsafe.putLong(oldCacheFieldAddr, 0L);
        }

        long prev = UnsafeUtils.unsafe.getLong(getAddress() + PREVIOUS_VERSIONS_OFFSET);
        while (prev != 0) {
            long pvCp = UnsafeUtils.unsafe.getLong(prev);
            if (pvCp == oldCpAddr) {
                UnsafeUtils.unsafe.putLong(prev, expandedCpAddr);
            }
            prev = UnsafeUtils.unsafe.getLong(prev + 24L);
        }

        readFields();
    }

    private void bumpLength() {
        expandedLength++;
        UnsafeUtils.unsafe.putInt(expandedCpAddr + CP_OFF_LENGTH, expandedLength);
        UnsafeUtils.unsafe.putInt(expandedTagsAddr, expandedLength);
        publishExpandedState();
    }

    private int findExistingUtf8(String s) {
        if (expandedCpAddr == 0L) return -1;
        long tagsData = expandedTagsAddr + TAGS_HEADER;
        for (int i = 1; i < expandedLength; i++) {
            int tag = UnsafeUtils.unsafe.getByte(tagsData + i) & 0xFF;
            if (tag == TAG_UTF8) {
                long symAddr = UnsafeUtils.unsafe.getLong(expandedCpAddr + HEADER_SIZE + i * 8L) & ~1L;
                if (symAddr != 0) {
                    try {
                        if (s.equals(new Symbol(symAddr).value)) return i;
                    } catch (Throwable ignored) { }
                }
            }
            if (tag == 5 || tag == 6) i++;
        }
        return -1;
    }

    private int findExistingNat(int nameIdx, int descIdx) {
        long tagsData = expandedTagsAddr + TAGS_HEADER;
        for (int i = 1; i < expandedLength; i++) {
            int tag = UnsafeUtils.unsafe.getByte(tagsData + i) & 0xFF;
            if (tag == TAG_NAMEANDTYPE) {
                long base = expandedCpAddr + HEADER_SIZE + i * 8L;
                int nameOfEntry = UnsafeUtils.unsafe.getShort(base)     & 0xFFFF;
                int descOfEntry = UnsafeUtils.unsafe.getShort(base + 2) & 0xFFFF;
                if (nameOfEntry == nameIdx && descOfEntry == descIdx) return i;
            }
            if (tag == 5 || tag == 6) i++;
        }
        return -1;
    }

    private int findExistingRef(int wantTag, int classIdx, int natIdx) {
        long tagsData = expandedTagsAddr + TAGS_HEADER;
        for (int i = 1; i < expandedLength; i++) {
            int tag = UnsafeUtils.unsafe.getByte(tagsData + i) & 0xFF;
            if (tag == wantTag) {
                long base = expandedCpAddr + HEADER_SIZE + i * 8L;
                int classOfEntry = UnsafeUtils.unsafe.getShort(base)     & 0xFFFF;
                int natOfEntry   = UnsafeUtils.unsafe.getShort(base + 2) & 0xFFFF;
                if (classOfEntry == classIdx && natOfEntry == natIdx) return i;
            }
            if (tag == 5 || tag == 6) i++;
        }
        return -1;
    }

    private void doFirstExpand(int extraSlots) {
        ConstantPool oldCp = new ConstantPool(constants);
        long oldCpAddr = oldCp.getAddress();
        int  oldLength = oldCp.length;
        int  oldTagsLen = UnsafeUtils.unsafe.getInt(oldCp.tags);
        long oldHeaderSize = oldCp.baseAddress() - oldCpAddr;

        long copyHeader = Math.max(HEADER_SIZE, oldHeaderSize);

        int newLength = oldLength + Math.max(extraSlots, 32);
        long newCpSize   = copyHeader + (long) newLength * 8L;
        long newTagsSize = TAGS_HEADER + newLength;

        long newCpAddr   = UnsafeUtils.unsafe.allocateMemory(newCpSize);
        long newTagsAddr = UnsafeUtils.unsafe.allocateMemory(newTagsSize);
        for (long i = 0; i < newCpSize; i++) {
            UnsafeUtils.unsafe.putByte(newCpAddr + i, (byte) 0);
        }
        for (long i = 0; i < newTagsSize; i++) {
            UnsafeUtils.unsafe.putByte(newTagsAddr + i, (byte) 0);
        }

        for (long i = 0; i < copyHeader; i++) {
            UnsafeUtils.unsafe.putByte(newCpAddr + i, UnsafeUtils.unsafe.getByte(oldCpAddr + i));
        }
        long cpVtable = UnsafeUtils.unsafe.getAddress(oldCpAddr);
        UnsafeUtils.unsafe.putAddress(newCpAddr, cpVtable);

        long oldEntryBase = oldCpAddr + oldHeaderSize;
        long newEntryBase = newCpAddr + copyHeader;
        for (int i = 0; i < oldLength; i++) {
            UnsafeUtils.unsafe.putLong(newEntryBase + i * 8L,
                    UnsafeUtils.unsafe.getLong(oldEntryBase + i * 8L));
        }
        UnsafeUtils.unsafe.putInt(newTagsAddr, newLength);
        long oldTagsData = oldCp.tags + TAGS_HEADER;
        for (int i = 0; i < oldTagsLen; i++) {
            UnsafeUtils.unsafe.putByte(newTagsAddr + TAGS_HEADER + i,
                    UnsafeUtils.unsafe.getByte(oldTagsData + i));
        }
        UnsafeUtils.unsafe.putInt(newCpAddr + CP_OFF_LENGTH, newLength);
        UnsafeUtils.unsafe.putLong(newCpAddr + CP_OFF_TAGS, newTagsAddr);

        this.expandedCpAddr   = newCpAddr;
        this.expandedTagsAddr = newTagsAddr;
        this.expandedLength   = oldLength;
        publishExpandedState();

        readFields();
    }

    private long resolveSymbol(String s, Class<?>... donors) {
        Long cached = symbolCache.get(s);
        if (cached != null) return cached;

        long sym = findSymbolInPool(s, new ConstantPool(constants));
        if (sym != 0) {
            symbolCache.put(s, sym);
            return sym;
        }

        for (Class<?> c : donors) {
            if (c == null) continue;
            try {
                ConstantPool cp = new ConstantPool(new InstanceKlass(c).constants);
                sym = findSymbolInPool(s, cp);
                if (sym != 0) {
                    symbolCache.put(s, sym);
                    return sym;
                }
            } catch (Throwable ignored) { }
        }
        return 0;
    }

    private long findSymbolInPool(String target, ConstantPool cp) {
        long tagsData = cp.tags + TAGS_HEADER;
        for (int i = 1; i < cp.length; i++) {
            int tag = UnsafeUtils.unsafe.getByte(tagsData + i) & 0xFF;
            if (tag == TAG_UTF8) {
                long symAddr = cp.slotAt(i) & ~1L;
                if (symAddr != 0) {
                    try {
                        if (target.equals(new Symbol(symAddr).value)) return symAddr;
                    } catch (Throwable ignored) { }
                }
            }
            if (tag == 5 || tag == 6) i++;
        }
        return 0;
    }

    public List<NMethod> osrNMethods() {
        List<NMethod> out = new ArrayList<>();
        long head = osrNmethodsHead;
        while (head != 0) {
            NMethod nm = new NMethod(head);
            out.add(nm);
            head = nm.osrLink;
        }
        return out;
    }

    public NMethod osrNMethodFor(Method target) {
        long head = osrNmethodsHead;
        long targetAddr = target.getAddress();
        while (head != 0) {
            NMethod nm = new NMethod(head);
            if (nm.method == targetAddr) return nm;
            head = nm.osrLink;
        }
        return null;
    }

    public void ensureCpCacheCapacity(int extraEntries) {
        ConstantPool cp = new ConstantPool(currentCpAddress());
        ConstantPoolCache cpc = cp.getCache();
        if (cpc == null) return;

        int oldLen = cpc.length;
        int newLen = oldLen + Math.max(extraEntries, 32);
        long oldAddr = cpc.getAddress();

        long headerSize = ConstantPoolCache.HEADER_SIZE;
        long newSize = headerSize + (long) newLen * ConstantPoolCache.ENTRY_SIZE;
        long newAddr = UnsafeUtils.unsafe.allocateMemory(newSize);

        for (long i = 0; i < newSize; i++) {
            UnsafeUtils.unsafe.putByte(newAddr + i, (byte) 0);
        }
        for (long i = 0; i < headerSize; i++) {
            UnsafeUtils.unsafe.putByte(newAddr + i, UnsafeUtils.unsafe.getByte(oldAddr + i));
        }

        UnsafeUtils.unsafe.putInt(newAddr + ConstantPoolCache.LENGTH_OFFSET, newLen);

        long oldEntriesBytes = (long) oldLen * ConstantPoolCache.ENTRY_SIZE;
        for (long i = 0; i < oldEntriesBytes; i++) {
            UnsafeUtils.unsafe.putByte(
                    newAddr + headerSize + i,
                    UnsafeUtils.unsafe.getByte(oldAddr + headerSize + i));
        }

        for (int i = oldLen; i < newLen; i++) {
            long entryAddr = newAddr + headerSize + (long) i * ConstantPoolCache.ENTRY_SIZE;
            for (int k = 0; k < ConstantPoolCache.ENTRY_SIZE; k++) {
                UnsafeUtils.unsafe.putByte(entryAddr + k, (byte) 0);
            }
            UnsafeUtils.unsafe.putInt(
                    entryAddr + ConstantPoolCache.ENTRY_INDICES_OFFSET,
                    i);
        }

        long oldResolvedRefs = UnsafeUtils.unsafe.getLong(
                oldAddr + ConstantPoolCache.RESOLVED_REFERENCES_OFFSET);
        long oldRefMap = UnsafeUtils.unsafe.getLong(
                oldAddr + ConstantPoolCache.REFERENCE_MAP_OFFSET);

        Object[] newResolvedArr = new Object[newLen];
        PINNED.add(newResolvedArr);
        long newResolvedArrOop = OopDesc.addressOf(newResolvedArr);

        if (oldResolvedRefs != 0L) {
            long oldArrOop = UnsafeUtils.unsafe.getLong(oldResolvedRefs);
            if (oldArrOop != 0L) {
                int oldArrLen = UnsafeUtils.unsafe.getInt(oldArrOop + (int) OOP_ARRAY_LENGTH_OFFSET);
                int copyLen = Math.min(oldArrLen, newLen);
                for (int i = 0; i < copyLen; i++) {
                    int narrow = UnsafeUtils.unsafe.getInt(
                            oldArrOop + OOP_ARRAY_DATA_OFFSET + (long) i * 4L);
                    UnsafeUtils.unsafe.putInt(
                            newResolvedArrOop + OOP_ARRAY_DATA_OFFSET + (long) i * 4L, narrow);
                }
            }
        }

        long newOopSlotAddr = UnsafeUtils.unsafe.allocateMemory(8L);
        UnsafeUtils.unsafe.putLong(newOopSlotAddr, newResolvedArrOop);

        long newRefMapSize = 8L + (long) newLen * 2L;
        long newRefMapAddr = UnsafeUtils.unsafe.allocateMemory(newRefMapSize);
        for (long i = 0; i < newRefMapSize; i++) {
            UnsafeUtils.unsafe.putByte(newRefMapAddr + i, (byte) 0);
        }
        UnsafeUtils.unsafe.putInt(newRefMapAddr, newLen);

        if (oldRefMap != 0L) {
            int oldRefMapLen = UnsafeUtils.unsafe.getInt(oldRefMap);
            int copyLen = Math.min(oldRefMapLen, newLen);
            for (int i = 0; i < copyLen; i++) {
                short v = UnsafeUtils.unsafe.getShort(oldRefMap + 8L + (long) i * 2L);
                UnsafeUtils.unsafe.putShort(newRefMapAddr + 8L + (long) i * 2L, v);
            }
        }

        UnsafeUtils.unsafe.putLong(
                newAddr + ConstantPoolCache.RESOLVED_REFERENCES_OFFSET, newOopSlotAddr);
        UnsafeUtils.unsafe.putLong(
                newAddr + ConstantPoolCache.REFERENCE_MAP_OFFSET, newRefMapAddr);

        long cpAddr = currentCpAddress();
        UnsafeUtils.unsafe.putLong(cpAddr + ConstantPool.CACHE_OFFSET, newAddr);
        UnsafeUtils.unsafe.putLong(newAddr + ConstantPoolCache.CONSTANT_POOL_OFFSET, cpAddr);

        readFields();
    }

    @Override
    public String toString() {
        return String.format(
                "InstanceKlass[address=0x%x, name=%s, initState=%d, methods=%d, "
                        + "nonstaticFieldSize=%d, staticFieldSize=%d, itableLen=%d, miscFlags=0x%04x]",
                getAddress(), getName(), initState, methodsCount(),
                nonstaticFieldSize, staticFieldSize, itableLen, miscFlags & 0xFFFF);
    }
}
