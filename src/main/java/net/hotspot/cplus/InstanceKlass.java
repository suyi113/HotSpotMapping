
package net.hotspot.cplus;



import net.hotspot.cplus.Utils.UnsafeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;


public class InstanceKlass extends Klass {
    private static final long ANNOTATIONS_OFFSET                  = findOffsetOrDefault("InstanceKlass::_annotations",                  200L);
    private static final long PACKAGE_ENTRY_OFFSET                = findOffsetOrDefault("InstanceKlass::_package_entry",                208L);
    private static final long ARRAY_KLASSES_OFFSET                = findOffsetOrDefault("InstanceKlass::_array_klasses",                216L);
    private static final long CONSTANTS_OFFSET                    = findOffsetOrDefault("InstanceKlass::_constants",                    224L);
    private static final long INNER_CLASSES_OFFSET                = findOffsetOrDefault("InstanceKlass::_inner_classes",                232L);
    private static final long NEST_MEMBERS_OFFSET                 = findOffsetOrDefault("InstanceKlass::_nest_members",                 240L);
    private static final long NEST_HOST_OFFSET                    = findOffsetOrDefault("InstanceKlass::_nest_host",                    248L);
    private static final long PERMITTED_SUBCLASSES_OFFSET         = findOffsetOrDefault("InstanceKlass::_permitted_subclasses",         256L);
    private static final long RECORD_COMPONENTS_OFFSET            = findOffsetOrDefault("InstanceKlass::_record_components",            264L);
    private static final long SOURCE_DEBUG_EXTENSION_OFFSET       = findOffsetOrDefault("InstanceKlass::_source_debug_extension",       272L);
    private static final long NONSTATIC_FIELD_SIZE_OFFSET         = findOffsetOrDefault("InstanceKlass::_nonstatic_field_size",         280L);
    private static final long STATIC_FIELD_SIZE_OFFSET            = findOffsetOrDefault("InstanceKlass::_static_field_size",            284L);
    private static final long NONSTATIC_OOP_MAP_SIZE_OFFSET       = findOffsetOrDefault("InstanceKlass::_nonstatic_oop_map_size",       288L);
    private static final long ITABLE_LEN_OFFSET                   = findOffsetOrDefault("InstanceKlass::_itable_len",                   292L);
    private static final long NEST_HOST_INDEX_OFFSET              = findOffsetOrDefault("InstanceKlass::_nest_host_index",              296L);
    private static final long THIS_CLASS_INDEX_OFFSET             = findOffsetOrDefault("InstanceKlass::_this_class_index",             298L);
    private static final long STATIC_OOP_FIELD_COUNT_OFFSET       = findOffsetOrDefault("InstanceKlass::_static_oop_field_count",       300L);
    private static final long JAVA_FIELDS_COUNT_OFFSET            = findOffsetOrDefault("InstanceKlass::_java_fields_count",            302L);
    private static final long IDNUM_ALLOCATED_COUNT_OFFSET        = findOffsetOrDefault("InstanceKlass::_idnum_allocated_count",        304L);
    private static final long IS_MARKED_DEPENDENT_OFFSET          = findOffsetOrDefault("InstanceKlass::_is_marked_dependent",          306L);
    private static final long INIT_STATE_OFFSET                   = findOffsetOrDefault("InstanceKlass::_init_state",                   307L);
    private static final long REFERENCE_TYPE_OFFSET               = findOffsetOrDefault("InstanceKlass::_reference_type",               308L);
    private static final long KIND_OFFSET                         = findOffsetOrDefault("InstanceKlass::_kind",                         309L);
    private static final long MISC_FLAGS_OFFSET                   = findOffsetOrDefault("InstanceKlass::_misc_flags",                   310L);
    private static final long INIT_THREAD_OFFSET                  = findOffsetOrDefault("InstanceKlass::_init_thread",                  312L);
    private static final long OOP_MAP_CACHE_OFFSET                = findOffsetOrDefault("InstanceKlass::_oop_map_cache",                320L);
    private static final long JNI_IDS_OFFSET                      = findOffsetOrDefault("InstanceKlass::_jni_ids",                      328L);
    private static final long METHODS_JMETHOD_IDS_OFFSET          = findOffsetOrDefault("InstanceKlass::_methods_jmethod_ids",          336L);
    private static final long DEP_CONTEXT_OFFSET                  = findOffsetOrDefault("InstanceKlass::_dep_context",                  344L);
    private static final long DEP_CONTEXT_LAST_CLEANED_OFFSET     = findOffsetOrDefault("InstanceKlass::_dep_context_last_cleaned",     352L);
    private static final long OSR_NMETHODS_HEAD_OFFSET            = findOffsetOrDefault("InstanceKlass::_osr_nmethods_head",            360L);
    private static final long BREAKPOINTS_OFFSET                  = findOffsetOrDefault("InstanceKlass::_breakpoints",                  368L);
    private static final long PREVIOUS_VERSIONS_OFFSET            = findOffsetOrDefault("InstanceKlass::_previous_versions",            376L);
    private static final long CACHED_CLASS_FILE_OFFSET            = findOffsetOrDefault("InstanceKlass::_cached_class_file",            384L);
    private static final long JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET = findOffsetOrDefault("InstanceKlass::_jvmti_cached_class_field_map", 392L);
    private static final long METHODS_OFFSET                      = findOffsetOrDefault("InstanceKlass::_methods",                      400L);
    private static final long DEFAULT_METHODS_OFFSET              = findOffsetOrDefault("InstanceKlass::_default_methods",              408L);
    private static final long LOCAL_INTERFACES_OFFSET             = findOffsetOrDefault("InstanceKlass::_local_interfaces",             416L);
    private static final long TRANSITIVE_INTERFACES_OFFSET        = findOffsetOrDefault("InstanceKlass::_transitive_interfaces",        424L);
    private static final long METHOD_ORDERING_OFFSET              = findOffsetOrDefault("InstanceKlass::_method_ordering",              432L);
    private static final long DEFAULT_VTABLE_INDICES_OFFSET       = findOffsetOrDefault("InstanceKlass::_default_vtable_indices",       440L);
    private static final long FIELDS_OFFSET                       = findOffsetOrDefault("InstanceKlass::_fields",                       448L);    private static final int INIT_STATE_ALLOCATED            = 0;
    private static final int INIT_STATE_LOADED               = 1;
    private static final int INIT_STATE_LINKED               = 2;
    private static final int INIT_STATE_BEING_INITIALIZED    = 3;
    private static final int INIT_STATE_FULLY_INITIALIZED    = 4;
    private static final int INIT_STATE_INITIALIZATION_ERROR = 5;

    private static final int KIND_OTHER        = 0;
    private static final int KIND_REFERENCE    = 1;
    private static final int KIND_CLASS_LOADER = 2;
    private static final int KIND_MIRROR       = 3;

    private static final int MISC_REWRITTEN                           = 1 << 0;
    private static final int MISC_HAS_NONSTATIC_FIELDS                = 1 << 1;
    private static final int MISC_SHOULD_VERIFY_CLASS                 = 1 << 2;
    private static final int MISC_UNUSED                              = 1 << 3;
    private static final int MISC_IS_CONTENDED                        = 1 << 4;
    private static final int MISC_HAS_NONSTATIC_CONCRETE_METHODS      = 1 << 5;
    private static final int MISC_DECLARES_NONSTATIC_CONCRETE_METHODS = 1 << 6;
    private static final int MISC_HAS_BEEN_REDEFINED                  = 1 << 7;
    private static final int MISC_SHARED_LOADING_FAILED               = 1 << 8;
    private static final int MISC_IS_SCRATCH_CLASS                    = 1 << 9;
    private static final int MISC_IS_SHARED_BOOT_CLASS                = 1 << 10;
    private static final int MISC_IS_SHARED_PLATFORM_CLASS            = 1 << 11;
    private static final int MISC_IS_SHARED_APP_CLASS                 = 1 << 12;
    private static final int MISC_HAS_RESOLVED_METHODS                = 1 << 13;
    private static final int MISC_IS_BEING_REDEFINED                  = 1 << 14;
    private static final int MISC_HAS_CONTENDED_ANNOTATIONS           = 1 << 15;

    public static final int BC_GETSTATIC = 0xB2;
    public static final int BC_PUTSTATIC = 0xB3;
    public static final int BC_GETFIELD  = 0xB4;
    public static final int BC_PUTFIELD  = 0xB5;

    public static final int TOS_STATE_SHIFT      = 28;
    public static final int IS_FIELD_ENTRY_SHIFT = 26;
    public static final int IS_FINAL_SHIFT       = 22;
    public static final int IS_VOLATILE_SHIFT    = 21;
    public static final int FIELD_INDEX_MASK     = 0xFFFF;

    public static final int TOS_BTOS = 0;
    public static final int TOS_ZTOS = 1;
    public static final int TOS_CTOS = 2;
    public static final int TOS_STOS = 3;
    public static final int TOS_ITOS = 4;
    public static final int TOS_LTOS = 5;
    public static final int TOS_FTOS = 6;
    public static final int TOS_DTOS = 7;
    public static final int TOS_ATOS = 8;
    public static final int TOS_VTOS = 9;

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
    public int nonstaticFieldSize;
    public int staticFieldSize;
    public int nonstaticOopMapSize;
    public int itableLen;
    public short nestHostIndex;
    public short thisClassIndex;
    public short staticOopFieldCount;
    public short javaFieldsCount;
    public short idnumAllocatedCount;
    public boolean isMarkedDependent;
    public byte initState;
    public byte referenceType;
    public byte kind;
    public short miscFlags;
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
    public long fields;

    private long expandedCpAddr   = 0L;
    private long expandedTagsAddr = 0L;
    private int  expandedLength   = 0;
    private final Map<String, Long> symbolCache = new HashMap<>();

    private static final ConcurrentHashMap<Long, long[]> EXPANDED = new ConcurrentHashMap<>();
    private static final List<Object> PINNED = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Long, Integer> CACHE_NEXT = new ConcurrentHashMap<>();

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

    private long expandedResolvedKlassesAddr   = 0L;
    private int  expandedResolvedKlassesLength = 0;

    private static final long OOP_ARRAY_LENGTH_OFFSET = 12L;
    private static final long OOP_ARRAY_DATA_OFFSET   = 16L;
    private static final int  OOP_SHIFT               = 3;

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

    private void readFields() {
        long base = getAddress();
        annotations = UnsafeUtils.unsafe.getLong(base + ANNOTATIONS_OFFSET);
        packageEntry = UnsafeUtils.unsafe.getLong(base + PACKAGE_ENTRY_OFFSET);
        arrayKlasses = UnsafeUtils.unsafe.getLong(base + ARRAY_KLASSES_OFFSET);
        constants = UnsafeUtils.unsafe.getLong(base + CONSTANTS_OFFSET);
        innerClasses = UnsafeUtils.unsafe.getLong(base + INNER_CLASSES_OFFSET);
        nestMembers = UnsafeUtils.unsafe.getLong(base + NEST_MEMBERS_OFFSET);
        nestHost = UnsafeUtils.unsafe.getLong(base + NEST_HOST_OFFSET);
        permittedSubclasses = UnsafeUtils.unsafe.getLong(base + PERMITTED_SUBCLASSES_OFFSET);
        recordComponents = UnsafeUtils.unsafe.getLong(base + RECORD_COMPONENTS_OFFSET);
        sourceDebugExtension = UnsafeUtils.unsafe.getLong(base + SOURCE_DEBUG_EXTENSION_OFFSET);
        nonstaticFieldSize = UnsafeUtils.unsafe.getInt(base + NONSTATIC_FIELD_SIZE_OFFSET);
        staticFieldSize = UnsafeUtils.unsafe.getInt(base + STATIC_FIELD_SIZE_OFFSET);
        nonstaticOopMapSize = UnsafeUtils.unsafe.getInt(base + NONSTATIC_OOP_MAP_SIZE_OFFSET);
        itableLen = UnsafeUtils.unsafe.getInt(base + ITABLE_LEN_OFFSET);
        nestHostIndex = UnsafeUtils.unsafe.getShort(base + NEST_HOST_INDEX_OFFSET);
        thisClassIndex = UnsafeUtils.unsafe.getShort(base + THIS_CLASS_INDEX_OFFSET);
        staticOopFieldCount = UnsafeUtils.unsafe.getShort(base + STATIC_OOP_FIELD_COUNT_OFFSET);
        javaFieldsCount = UnsafeUtils.unsafe.getShort(base + JAVA_FIELDS_COUNT_OFFSET);
        idnumAllocatedCount = UnsafeUtils.unsafe.getShort(base + IDNUM_ALLOCATED_COUNT_OFFSET);
        isMarkedDependent = UnsafeUtils.unsafe.getByte(base + IS_MARKED_DEPENDENT_OFFSET) != 0;
        initState = UnsafeUtils.unsafe.getByte(base + INIT_STATE_OFFSET);
        referenceType = UnsafeUtils.unsafe.getByte(base + REFERENCE_TYPE_OFFSET);
        kind = UnsafeUtils.unsafe.getByte(base + KIND_OFFSET);
        miscFlags = UnsafeUtils.unsafe.getShort(base + MISC_FLAGS_OFFSET);
        initThread = UnsafeUtils.unsafe.getLong(base + INIT_THREAD_OFFSET);
        oopMapCache = UnsafeUtils.unsafe.getLong(base + OOP_MAP_CACHE_OFFSET);
        jniIds = UnsafeUtils.unsafe.getLong(base + JNI_IDS_OFFSET);
        methodsJmethodIds = UnsafeUtils.unsafe.getLong(base + METHODS_JMETHOD_IDS_OFFSET);
        depContext = UnsafeUtils.unsafe.getLong(base + DEP_CONTEXT_OFFSET);
        depContextLastCleaned = UnsafeUtils.unsafe.getLong(base + DEP_CONTEXT_LAST_CLEANED_OFFSET);
        osrNmethodsHead = UnsafeUtils.unsafe.getLong(base + OSR_NMETHODS_HEAD_OFFSET);
        breakpoints = UnsafeUtils.unsafe.getLong(base + BREAKPOINTS_OFFSET);
        previousVersions = UnsafeUtils.unsafe.getLong(base + PREVIOUS_VERSIONS_OFFSET);
        cachedClassFile = UnsafeUtils.unsafe.getLong(base + CACHED_CLASS_FILE_OFFSET);
        jvmtiCachedClassFieldMap = UnsafeUtils.unsafe.getLong(base + JVMTI_CACHED_CLASS_FIELD_MAP_OFFSET);
        methods = UnsafeUtils.unsafe.getLong(base + METHODS_OFFSET);
        defaultMethods = UnsafeUtils.unsafe.getLong(base + DEFAULT_METHODS_OFFSET);
        localInterfaces = UnsafeUtils.unsafe.getLong(base + LOCAL_INTERFACES_OFFSET);
        transitiveInterfaces = UnsafeUtils.unsafe.getLong(base + TRANSITIVE_INTERFACES_OFFSET);
        methodOrdering = UnsafeUtils.unsafe.getLong(base + METHOD_ORDERING_OFFSET);
        defaultVtableIndices = UnsafeUtils.unsafe.getLong(base + DEFAULT_VTABLE_INDICES_OFFSET);
        fields = UnsafeUtils.unsafe.getLong(base + FIELDS_OFFSET);
    }

    @Override
    public boolean isInstanceKlass() { return true; }

    public long annotations() { return annotations; }
    public long packageEntry() { return packageEntry; }
    public long arrayKlasses() { return arrayKlasses; }
    public long constants() { return constants; }
    public long innerClasses() { return innerClasses; }
    public long nestMembers() { return nestMembers; }
    public long nestHost() { return nestHost; }
    public long permittedSubclasses() { return permittedSubclasses; }
    public long recordComponents() { return recordComponents; }
    public long sourceDebugExtension() { return sourceDebugExtension; }
    public int nonstaticFieldSize() { return nonstaticFieldSize; }
    public int staticFieldSize() { return staticFieldSize; }
    public int nonstaticOopMapSize() { return nonstaticOopMapSize; }
    public int itableLen() { return itableLen; }
    public short nestHostIndex() { return nestHostIndex; }
    public short thisClassIndex() { return thisClassIndex; }
    public short staticOopFieldCount() { return staticOopFieldCount; }
    public short javaFieldsCount() { return javaFieldsCount; }
    public short idnumAllocatedCount() { return idnumAllocatedCount; }
    public boolean isMarkedDependent() { return isMarkedDependent; }
    public byte initState() { return initState; }
    public byte referenceType() { return referenceType; }
    public byte kind() { return kind; }
    public short miscFlags() { return miscFlags; }
    public long initThread() { return initThread; }
    public long methods() { return methods; }
    public long defaultMethods() { return defaultMethods; }
    public long localInterfaces() { return localInterfaces; }
    public long transitiveInterfaces() { return transitiveInterfaces; }
    public long methodOrdering() { return methodOrdering; }
    public long defaultVtableIndices() { return defaultVtableIndices; }
    public long fields() { return fields; }

    public boolean isLoaded() { return initState >= INIT_STATE_LOADED; }
    public boolean isLinked() { return initState >= INIT_STATE_LINKED; }
    public boolean isInitialized() { return initState == INIT_STATE_FULLY_INITIALIZED; }
    public boolean isNotInitialized() { return initState < INIT_STATE_BEING_INITIALIZED; }
    public boolean isBeingInitialized() { return initState == INIT_STATE_BEING_INITIALIZED; }
    public boolean isInErrorState() { return initState == INIT_STATE_INITIALIZATION_ERROR; }
    public boolean isRewritten() { return (miscFlags & MISC_REWRITTEN) != 0; }
    public boolean hasNonstaticFields() { return (miscFlags & MISC_HAS_NONSTATIC_FIELDS) != 0; }
    public boolean shouldVerifyClass() { return (miscFlags & MISC_SHOULD_VERIFY_CLASS) != 0; }
    public boolean isContended() { return (miscFlags & MISC_IS_CONTENDED) != 0; }
    public boolean hasNonstaticConcreteMethods() { return (miscFlags & MISC_HAS_NONSTATIC_CONCRETE_METHODS) != 0; }
    public boolean declaresNonstaticConcreteMethods() { return (miscFlags & MISC_DECLARES_NONSTATIC_CONCRETE_METHODS) != 0; }
    public boolean hasBeenRedefined() { return (miscFlags & MISC_HAS_BEEN_REDEFINED) != 0; }
    public boolean sharedLoadingFailed() { return (miscFlags & MISC_SHARED_LOADING_FAILED) != 0; }
    public boolean isScratchClass() { return (miscFlags & MISC_IS_SCRATCH_CLASS) != 0; }
    public boolean isSharedBootClass() { return (miscFlags & MISC_IS_SHARED_BOOT_CLASS) != 0; }
    public boolean isSharedPlatformClass() { return (miscFlags & MISC_IS_SHARED_PLATFORM_CLASS) != 0; }
    public boolean isSharedAppClass() { return (miscFlags & MISC_IS_SHARED_APP_CLASS) != 0; }

    public boolean isSharedUnregisteredClass() {
        int sharedLoaderTypeBits = MISC_IS_SHARED_BOOT_CLASS | MISC_IS_SHARED_PLATFORM_CLASS | MISC_IS_SHARED_APP_CLASS;
        return (miscFlags & sharedLoaderTypeBits) == 0;
    }

    public boolean hasResolvedMethods() { return (miscFlags & MISC_HAS_RESOLVED_METHODS) != 0; }
    public boolean isBeingRedefined() { return (miscFlags & MISC_IS_BEING_REDEFINED) != 0; }
    public boolean hasContendedAnnotations() { return (miscFlags & MISC_HAS_CONTENDED_ANNOTATIONS) != 0; }
    public boolean isOtherInstanceKlass() { return kind == KIND_OTHER; }
    public boolean isReferenceInstanceKlass() { return kind == KIND_REFERENCE; }
    public boolean isMirrorInstanceKlass() { return kind == KIND_MIRROR; }
    public boolean isClassLoaderInstanceKlass() { return kind == KIND_CLASS_LOADER; }

    public MethodArray methodsArray() {
        if (methods == 0) return new MethodArray(0);
        return new MethodArray(methods);
    }

    public Method[] getMethods() {
        return methodsArray().toMethods();
    }

    public FieldArray fieldsArray() {
        if (fields == 0) return new FieldArray(0);
        return new FieldArray(fields);
    }

    public FieldInfo[] getFields() {
        return fieldsArray().toFields();
    }

    public long javaMirrorOopAddress() {
        if (javaMirror == 0) return 0;
        return UnsafeUtils.unsafe.getLong(javaMirror);
    }

    public String getName() {
        if (name == 0) return null;
        return new Symbol(name).value;
    }

    public String getExternalName() {
        String n = getName();
        return n == null ? null : n.replace('/', '.');
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
            throw new IllegalStateException("Can't find string in JVM symbol table:: " + s
                    + "（Please add the classes that referenced it into donors.）");
        }
        int idx = expandedLength;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_UTF8);
        UnsafeUtils.unsafe.putLong(expandedCpAddr + HEADER_SIZE + idx * 8L, symAddr);
        System.out.printf("[CP] Utf8   #%d sym=0x%x \"%s\"%n",
                idx, symAddr, s.length() > 60 ? s.substring(0, 60) : s);
        bumpLength();
        readFields();
        return idx;
    }

    private int appendResolvedKlass(long klassAddr) {
        if (expandedResolvedKlassesAddr == 0L) {
            long oldAddr = UnsafeUtils.unsafe.getLong(expandedCpAddr + 40L);
            int  oldLen  = (oldAddr == 0) ? 0 : UnsafeUtils.unsafe.getInt(oldAddr);

            int newLen  = oldLen + 32;
            long newSize = 8L + (long) newLen * 8L;
            long newAddr = UnsafeUtils.unsafe.allocateMemory(newSize);
            for (long i = 0; i < newSize; i++) {
                UnsafeUtils.unsafe.putByte(newAddr + i, (byte) 0);
            }
            UnsafeUtils.unsafe.putInt(newAddr, newLen);
            for (int i = 0; i < oldLen; i++) {
                long v = UnsafeUtils.unsafe.getLong(oldAddr + 8L + i * 8L);
                UnsafeUtils.unsafe.putLong(newAddr + 8L + i * 8L, v);
            }
            UnsafeUtils.unsafe.putLong(expandedCpAddr + 40L, newAddr);

            expandedResolvedKlassesAddr = newAddr;
            expandedResolvedKlassesLength = oldLen;
        }

        int idx = expandedResolvedKlassesLength;
        UnsafeUtils.unsafe.putLong(expandedResolvedKlassesAddr + 8L + idx * 8L, klassAddr);
        expandedResolvedKlassesLength++;
        UnsafeUtils.unsafe.putInt(expandedResolvedKlassesAddr, expandedResolvedKlassesLength);
        publishExpandedState();
        System.out.printf("[CP] rklass idx=%d klass=0x%x rkAddr=0x%x len=%d%n",
                idx, klassAddr, expandedResolvedKlassesAddr, expandedResolvedKlassesLength);
        return idx;
    }

    public int addClass(String className, Class<?>... donors) {
        long klassAddr = findKlassByDonors(className, donors);
        if (klassAddr == 0) {
            throw new IllegalStateException("Class not found " + className + "，Please pass its Class in donors");
        }
        ensureCpCapacity(1);

        int existing = findExistingClassByKlass(klassAddr);
        if (existing > 0) return existing;

        int nameIdx = addUtf8(className, donors);
        int rkIndex = appendResolvedKlass(klassAddr);

        int idx = expandedLength;
        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_CLASS);
        UnsafeUtils.unsafe.putShort(base,      (short) nameIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) rkIndex);
        System.out.printf("[CP] Class  #%d nameIdx=%d rkIdx=%d klass=0x%x bytes=[%02x%02x %02x%02x] \"%s\"%n",
                idx, nameIdx, rkIndex, klassAddr,
                UnsafeUtils.unsafe.getByte(base) & 0xFF,
                UnsafeUtils.unsafe.getByte(base + 1) & 0xFF,
                UnsafeUtils.unsafe.getByte(base + 2) & 0xFF,
                UnsafeUtils.unsafe.getByte(base + 3) & 0xFF,
                className);
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
            if (tag == TAG_CLASS || tag == 100) {
                long slot = (cp != null)
                        ? cp.slotAt(i)
                        : UnsafeUtils.unsafe.getLong(expandedCpAddr + HEADER_SIZE + i * 8L);
                int rkIdx = (int) (slot & 0xFFFF);
                long k = UnsafeUtils.unsafe.getLong(resolvedKlassesAddr + 8L + rkIdx * 8L);
                if (k == klassAddr) {
                    System.out.printf("[CP] findClass hit i=%d rkIdx=%d tag=%d slot=0x%x%n",
                            i, rkIdx, tag, slot);
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
        System.out.printf("[CP] NAT    #%d nameIdx=%d descIdx=%d name=\"%s\" desc=\"%s\"%n",
                idx, nameIdx, descIdx, name, desc);
        bumpLength();
        readFields();
        return idx;
    }

    public int addFieldref(String owner, String name, String desc, Class<?>... donors) {
        int classIdx = addClass(owner, donors);
        int natIdx   = addNameAndType(name, desc, donors);
        ensureCpCapacity(1);
        int existing = findExistingRef(TAG_FIELDREF, classIdx, natIdx);
        if (existing > 0) {
            System.out.printf("[CP] Field  #%d (reuse) classIdx=%d natIdx=%d %s.%s%s%n",
                    existing, classIdx, natIdx, owner, name, desc);
            return existing;
        }

        int idx = expandedLength;
        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_FIELDREF);
        UnsafeUtils.unsafe.putShort(base,      (short) classIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) natIdx);
        System.out.printf("[CP] Field  #%d classIdx=%d natIdx=%d %s.%s%s%n",
                idx, classIdx, natIdx, owner, name, desc);
        bumpLength();
        readFields();
        return idx;
    }

    public int addMethodref(String owner, String name, String desc, Class<?>... donors) {
        int classIdx = addClass(owner, donors);
        int natIdx   = addNameAndType(name, desc, donors);
        ensureCpCapacity(1);
        int existing = findExistingRef(TAG_METHODREF, classIdx, natIdx);
        if (existing > 0) {
            System.out.printf("[CP] Method #%d (reuse) classIdx=%d natIdx=%d %s.%s%s%n",
                    existing, classIdx, natIdx, owner, name, desc);
            return existing;
        }

        int idx = expandedLength;
        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_METHODREF);
        UnsafeUtils.unsafe.putShort(base,      (short) classIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) natIdx);
        System.out.printf("[CP] Method #%d classIdx=%d natIdx=%d %s.%s%s%n",
                idx, classIdx, natIdx, owner, name, desc);
        bumpLength();
        readFields();
        return idx;
    }

    public int addInterfaceMethodref(String owner, String name, String desc, Class<?>... donors) {
        int classIdx = addClass(owner, donors);
        int natIdx   = addNameAndType(name, desc, donors);
        ensureCpCapacity(1);
        int existing = findExistingRef(TAG_INTERFACEMETHODREF, classIdx, natIdx);
        if (existing > 0) {
            System.out.printf("[CP] Iface  #%d (reuse) classIdx=%d natIdx=%d %s.%s%s%n",
                    existing, classIdx, natIdx, owner, name, desc);
            return existing;
        }

        int idx = expandedLength;
        long base = expandedCpAddr + HEADER_SIZE + idx * 8L;
        UnsafeUtils.unsafe.putByte(expandedTagsAddr + TAGS_HEADER + idx, (byte) TAG_INTERFACEMETHODREF);
        UnsafeUtils.unsafe.putShort(base,      (short) classIdx);
        UnsafeUtils.unsafe.putShort(base + 2L, (short) natIdx);
        System.out.printf("[CP] Iface  #%d classIdx=%d natIdx=%d %s.%s%s%n",
                idx, classIdx, natIdx, owner, name, desc);
        bumpLength();
        readFields();
        return idx;
    }

    public int addCacheEntry(int cpIndex) {
        ConstantPool cp = new ConstantPool(currentCpAddress());
        ConstantPoolCache cpc = cp.getCache();
        if (cpc == null) {
            throw new IllegalStateException("ConstantPoolCache is null");
        }

        ConstantPoolCache finalCpc = cpc;
        int idx = CACHE_NEXT.computeIfAbsent(getAddress(), k -> finalCpc.length);
        System.out.printf("[CP] addCacheEntry: start idx=%d cpc.length=%d%n",
                idx, cpc.length);

        if (idx >= cpc.length) {
            int need = idx - cpc.length + 32;
            ensureCpCacheCapacity(need);
            cpc = new ConstantPool(currentCpAddress()).getCache();
            System.out.printf("[CP] addCacheEntry: after grow cpc.length=%d%n", cpc.length);
        }

        long addr = cpc.entryAddr(idx);
        for (int i = 0; i < (int) ConstantPoolCache.ENTRY_SIZE; i++) {
            UnsafeUtils.unsafe.putByte(addr + i, (byte) 0);
        }
        UnsafeUtils.unsafe.putShort(
                addr + ConstantPoolCache.ENTRY_INDICES_OFFSET,
                (short) (cpIndex & 0xFFFF));

        int readBack = UnsafeUtils.unsafe.getShort(
                addr + ConstantPoolCache.ENTRY_INDICES_OFFSET) & 0xFFFF;
        System.out.printf("[CP] addCacheEntry: idx=%d cpIdx=%d (readBack=%d) next=%d%n",
                idx, cpIndex, readBack, idx + 1);

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
        System.out.printf("[CP] addResolvedFieldEntry: start idx=%d cpc.length=%d%n",
                idx, cpc.length);

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
        UnsafeUtils.unsafe.putInt(
                addr + ConstantPoolCache.ENTRY_FLAGS_OFFSET, flags);

        int getCode = isStatic ? BC_GETSTATIC : BC_GETFIELD;
        int putCode = isFinal ? 0 : (isStatic ? BC_PUTSTATIC : BC_PUTFIELD);
        int indices = ((putCode & 0xFF) << 24)
                | ((getCode & 0xFF) << 16)
                | (cpIndex & 0xFFFF);
        UnsafeUtils.unsafe.putInt(
                addr + ConstantPoolCache.ENTRY_INDICES_OFFSET, indices);

        int rbI = UnsafeUtils.unsafe.getInt(addr + ConstantPoolCache.ENTRY_INDICES_OFFSET);
        long rbF1 = UnsafeUtils.unsafe.getAddress(addr + ConstantPoolCache.ENTRY_F1_OFFSET);
        long rbF2 = UnsafeUtils.unsafe.getLong(addr + ConstantPoolCache.ENTRY_F2_OFFSET);
        int rbFl = UnsafeUtils.unsafe.getInt(addr + ConstantPoolCache.ENTRY_FLAGS_OFFSET);
        System.out.printf(
                "[CP] addResolvedFieldEntry: idx=%d cpIdx=%d%n" +
                        "     indices=0x%08x (bc1=0x%02x bc2=0x%02x)%n" +
                        "     f1=0x%x f2=%d flags=0x%08x%n",
                idx, cpIndex,
                rbI, (rbI >> 16) & 0xFF, (rbI >> 24) & 0xFF,
                rbF1, rbF2, rbFl);

        CACHE_NEXT.put(getAddress(), idx + 1);
        long p = cpc.entryAddr(idx);
        System.out.printf("[FIELD] idx=%d%n", idx);
        System.out.printf("  ind  = %016x%n", UnsafeUtils.unsafe.getLong(p));
        System.out.printf("  f1   = %016x%n", UnsafeUtils.unsafe.getLong(p + 8));
        System.out.printf("  f2   = %016x (%d)%n",
                UnsafeUtils.unsafe.getLong(p + 16), UnsafeUtils.unsafe.getLong(p + 16));
        System.out.printf("  flags= %016x%n", UnsafeUtils.unsafe.getLong(p + 24));
        return idx;
    }

    public void commitCpChanges() {
        if (expandedCpAddr == 0L) {
            System.out.println("[CP] commit: no expansion, skip");
            return;
        }

        ConstantPool oldCp = new ConstantPool(constants);
        ConstantPoolCache cpc = oldCp.getCache();
        System.out.printf("[CP] commit: oldCp=0x%x oldLen=%d newCp=0x%x newLen=%d cpc=0x%x cpcLen=%d%n",
                oldCp.getAddress(), oldCp.length,
                expandedCpAddr, expandedLength,
                cpc == null ? 0L : cpc.getAddress(),
                cpc == null ? -1 : cpc.length);

        UnsafeUtils.unsafe.putLong(getAddress() + CONSTANTS_OFFSET, expandedCpAddr);
        this.constants = expandedCpAddr;

        int rewritten = 0;
        for (Method m : methodsArray().toMethods()) {
            if (m.constMethod != 0) {
                UnsafeUtils.unsafe.putLong(m.constMethod + 8L, expandedCpAddr);
                rewritten++;
            }
        }
        System.out.printf("[CP] commit: rewrote %d ConstMethod.constants%n", rewritten);

        ConstantPoolCache newCpc = new ConstantPool(currentCpAddress()).getCache();
        if (newCpc != null) {
            UnsafeUtils.unsafe.putLong(newCpc.getAddress() + 8L, expandedCpAddr);
            System.out.printf("[CP] commit: new cpc.constant_pool -> 0x%x%n", expandedCpAddr);
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
                    } catch (Throwable ignored) {}
                }
            }
            if (tag == 5 || tag == 6) i++;
        }
        return -1;
    }

    private int findExistingNat(int nameIdx, int descIdx) {
        return findTwoShortRef(TAG_NAMEANDTYPE, nameIdx, descIdx);
    }

    private int findExistingRef(int tag, int classIdx, int natIdx) {
        return findTwoShortRef(tag, classIdx, natIdx);
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
            UnsafeUtils.unsafe.putByte(newCpAddr + i,
                    UnsafeUtils.unsafe.getByte(oldCpAddr + i));
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

        System.out.printf(
                "[CP] doFirstExpand: full-copy 0x%x -> 0x%x header=%d len %d -> %d vtable=0x%x tags=0x%x%n",
                oldCpAddr, newCpAddr, copyHeader, oldLength, newLength, cpVtable, newTagsAddr);
        readFields();
    }
    private int findTwoShortRef(int wantTag, int a, int b) {
        long tagsData = expandedTagsAddr + TAGS_HEADER;
        for (int i = 1; i < expandedLength; i++) {
            int tag = UnsafeUtils.unsafe.getByte(tagsData + i) & 0xFF;
            if (tag == wantTag) {
                long base = expandedCpAddr + HEADER_SIZE + i * 8L;
                int v1 = UnsafeUtils.unsafe.getShort(base) & 0xFFFF;
                if (b < 0) {
                    if (v1 == a) return i;
                } else {
                    int v2 = UnsafeUtils.unsafe.getShort(base + 2) & 0xFFFF;
                    if (v1 == a && v2 == b) return i;
                }
            }
            if (tag == 5 || tag == 6) i++;
        }
        return -1;
    }

    private long resolveSymbol(String s, Class<?>... donors) {
        Long cached = symbolCache.get(s);
        if (cached != null) return cached;

        long sym = findSymbolInPool(s, new ConstantPool(constants));
        if (sym != 0) { symbolCache.put(s, sym); return sym; }

        for (Class<?> c : donors) {
            if (c == null) continue;
            try {
                ConstantPool cp = new ConstantPool(new InstanceKlass(c).constants);
                sym = findSymbolInPool(s, cp);
                if (sym != 0) { symbolCache.put(s, sym); return sym; }
            } catch (Throwable ignored) {}
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
                    } catch (Throwable ignored) {}
                }
            }
            if (tag == 5 || tag == 6) i++;
        }
        return 0;
    }

    public long osrNmethodsHead() { return osrNmethodsHead; }

    public List<NMethod> osrNMethods() {
        List<NMethod> out = new java.util.ArrayList<>();
        long head = osrNmethodsHead;
        while (head != 0) {
            NMethod nm = new NMethod(head);
            out.add(nm);
            head = nm.osrLink();
        }
        return out;
    }

    public NMethod osrNMethodFor(Method target) {
        long head = osrNmethodsHead;
        long targetAddr = target.getAddress();
        while (head != 0) {
            NMethod nm = new NMethod(head);
            if (nm.method == targetAddr) return nm;
            head = nm.osrLink();
        }
        return null;
    }
    
    public void ensureCpCacheCapacity(int extraEntries) {
        ConstantPool cp = new ConstantPool(currentCpAddress());
        ConstantPoolCache cpc = cp.getCache();
        if (cpc == null) {
            System.out.println("[CP] ensureCpc: cpc==null, skip");
            return;
        }

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
            UnsafeUtils.unsafe.putByte(newAddr + i,
                    UnsafeUtils.unsafe.getByte(oldAddr + i));
        }
        long cpcVtable = UnsafeUtils.unsafe.getAddress(oldAddr);
        UnsafeUtils.unsafe.putAddress(newAddr, cpcVtable);

        UnsafeUtils.unsafe.putInt(newAddr + 0L, newLen);
        UnsafeUtils.unsafe.putAddress(newAddr, cpcVtable);

        long oldEntriesBytes = (long) oldLen * ConstantPoolCache.ENTRY_SIZE;
        for (long i = 0; i < oldEntriesBytes; i++) {
            UnsafeUtils.unsafe.putByte(
                    newAddr + headerSize + i,
                    UnsafeUtils.unsafe.getByte(oldAddr + headerSize + i));
        }
        Object[] newResolvedArr = new Object[newLen];
        PINNED.add(newResolvedArr);
        long newResolvedOop = OopDesc.addressOf(newResolvedArr);

        long oldResolvedOop = UnsafeUtils.unsafe.getLong(oldAddr + 16L);
        if (oldResolvedOop != 0L) {
            int oldArrLen = UnsafeUtils.unsafe.getInt(oldResolvedOop + OOP_ARRAY_LENGTH_OFFSET);
            int copyLen = Math.min(oldArrLen, newLen);
            long oldElems = oldResolvedOop + OOP_ARRAY_DATA_OFFSET;
            long newElems = newResolvedOop + OOP_ARRAY_DATA_OFFSET;
            for (int i = 0; i < copyLen; i++) {
                int narrow = UnsafeUtils.unsafe.getInt(oldElems + (long) i * 4L);
                UnsafeUtils.unsafe.putInt(newElems + (long) i * 4L, narrow);
            }
        }
        UnsafeUtils.unsafe.putLong(newAddr + 16L, newResolvedOop);

        short[] newRefMapArr = new short[newLen];
        PINNED.add(newRefMapArr);
        long newRefMapOop = OopDesc.addressOf(newRefMapArr);

        long oldRefMapOop = UnsafeUtils.unsafe.getLong(oldAddr + 24L);
        if (oldRefMapOop != 0L) {
            int oldMapLen = UnsafeUtils.unsafe.getInt(oldRefMapOop + OOP_ARRAY_LENGTH_OFFSET);
            int copyLen = Math.min(oldMapLen, newLen);
            long oldElems = oldRefMapOop + OOP_ARRAY_DATA_OFFSET;
            long newElems = newRefMapOop + OOP_ARRAY_DATA_OFFSET;
            for (int i = 0; i < copyLen; i++) {
                short v = UnsafeUtils.unsafe.getShort(oldElems + (long) i * 2L);
                UnsafeUtils.unsafe.putShort(newElems + (long) i * 2L, v);
            }
        }
        UnsafeUtils.unsafe.putLong(newAddr + 24L, newRefMapOop);

        long cpAddr = currentCpAddress();
        UnsafeUtils.unsafe.putLong(cpAddr + 16L, newAddr);
        UnsafeUtils.unsafe.putLong(newAddr + 8L, cpAddr);

        UnsafeUtils.unsafe.putAddress(newAddr, cpcVtable);

        System.out.printf(
                "[CP] ensureCpc: full-copy 0x%x -> 0x%x %d -> %d vtable=0x%x resolved=0x%x refMap=0x%x%n",
                oldAddr, newAddr, oldLen, newLen, cpcVtable, newResolvedOop, newRefMapOop);
        readFields();
    }

    @Override
    public String toString() {
        return String.format(
                "InstanceKlass[address=0x%x, id=%d, layoutHelper=%d, name=0x%x, super=0x%x, loaderData=0x%x, " +
                        "accessFlags=0x%x, modifierFlags=0x%x ,initState=%d, kind=%d, miscFlags=0x%x, constants=0x%x, methods=0x%x, fields=0x%x]",
                getAddress(), id(), layoutHelper(), name(), superKlass(), classLoaderData(),
                accessFlags(), modifierFlags(), initState, kind, miscFlags, constants, methods, fields);
    }
}