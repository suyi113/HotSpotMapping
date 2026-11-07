package net.hotspot.cplus.jdk21.klass;

import net.hotspot.cplus.jdk21.classloading.ClassLoaderData;
import net.hotspot.cplus.jdk21.core.Metadata;
import net.hotspot.cplus.jdk21.core.MetaspaceObj;
import net.hotspot.cplus.jdk21.core.VMStructs;
import net.hotspot.cplus.jdk21.core.VMStructsHelper;

import net.hotspot.cplus.jdk21.core.UnsafeUtils;
import sun.misc.Unsafe;

import static net.hotspot.cplus.jdk21.core.VMStructsHelper.findOffsetOrDefault;

public class Klass extends Metadata {

    public static final int INSTANCE_KLASS_KIND              = 0;
    public static final int INSTANCE_REF_KLASS_KIND          = 1;
    public static final int INSTANCE_MIRROR_KLASS_KIND       = 2;
    public static final int INSTANCE_CLASS_LOADER_KLASS_KIND = 3;
    public static final int INSTANCE_STACK_CHUNK_KLASS_KIND  = 4;
    public static final int TYPE_ARRAY_KLASS_KIND            = 5;
    public static final int OBJ_ARRAY_KLASS_KIND             = 6;
    public static final int UNKNOWN_KLASS_KIND               = 7;

    public static final int KLASS_KIND_COUNT                 = OBJ_ARRAY_KLASS_KIND + 1;

    public static final int PRIMARY_SUPER_LIMIT              = 8;

    private static final String[] KIND_NAMES = {
            "InstanceKlass",
            "InstanceRefKlass",
            "InstanceMirrorKlass",
            "InstanceClassLoaderKlass",
            "InstanceStackChunkKlass",
            "TypeArrayKlass",
            "ObjArrayKlass",
            "UnknownKlass"
    };

    public static final int JVM_ACC_PUBLIC                = 0x00000001;
    public static final int JVM_ACC_PRIVATE               = 0x00000002;
    public static final int JVM_ACC_PROTECTED             = 0x00000004;
    public static final int JVM_ACC_STATIC                = 0x00000008;
    public static final int JVM_ACC_FINAL                 = 0x00000010;
    public static final int JVM_ACC_SUPER                 = 0x00000020;
    public static final int JVM_ACC_SYNCHRONIZED          = 0x00000020;
    public static final int JVM_ACC_VOLATILE              = 0x00000040;
    public static final int JVM_ACC_TRANSIENT             = 0x00000080;
    public static final int JVM_ACC_NATIVE                = 0x00000100;
    public static final int JVM_ACC_INTERFACE             = 0x00000200;
    public static final int JVM_ACC_ABSTRACT              = 0x00000400;
    public static final int JVM_ACC_STRICT                = 0x00000800;
    public static final int JVM_ACC_SYNTHETIC             = 0x00001000;
    public static final int JVM_ACC_ANNOTATION            = 0x00002000;
    public static final int JVM_ACC_ENUM                  = 0x00004000;
    public static final int JVM_ACC_MODULE                = 0x00008000;
    public static final int JVM_ACC_HAS_FINALIZER         = 0x40000000;
    public static final int JVM_ACC_IS_CLONEABLE_FAST     = 0x80000000;
    public static final int JVM_ACC_IS_HIDDEN_CLASS       = 0x04000000;
    public static final int JVM_ACC_IS_VALUE_BASED_CLASS  = 0x08000000;

    public static final int SHARED_CLASS_IS_SHARED                  = 1 << 0;
    public static final int SHARED_ARCHIVED_LAMBDA_PROXY_AVAILABLE  = 1 << 1;
    public static final int SHARED_HAS_VALUE_BASED_ANNOTATION       = 1 << 2;
    public static final int SHARED_VERIFIED_AT_DUMP_TIME            = 1 << 3;
    public static final int SHARED_HAS_ARCHIVED_ENUM_OBJS           = 1 << 4;
    public static final int SHARED_IS_GENERATED_SHARED_CLASS        = 1 << 5;

    public static final int LH_NEUTRAL_VALUE          = 0;
    public static final int LH_INSTANCE_SLOW_PATH_BIT = 0x01;
    public static final int LH_LOG2_ELEMENT_SIZE_SHIFT = 0;
    public static final int LH_LOG2_ELEMENT_SIZE_MASK  = 63;
    public static final int LH_ELEMENT_TYPE_SHIFT      = 8;
    public static final int LH_ELEMENT_TYPE_MASK       = 0xFF;
    public static final int LH_HEADER_SIZE_SHIFT       = 16;
    public static final int LH_HEADER_SIZE_MASK        = 0xFF;
    public static final int LH_ARRAY_TAG_SHIFT         = 30;
    public static final int LH_ARRAY_TAG_OBJ_VALUE     = 2;
    public static final int LH_ARRAY_TAG_TYPE_VALUE    = 3;

    private static final long LAYOUT_HELPER_OFFSET           = findOffsetOrDefault("Klass::_layout_helper",            8L);
    private static final long KIND_OFFSET                    = findOffsetOrDefault("Klass::_kind",                     12L);
    private static final long MODIFIER_FLAGS_OFFSET          = findOffsetOrDefault("Klass::_modifier_flags",           16L);
    private static final long SUPER_CHECK_OFFSET_OFFSET      = findOffsetOrDefault("Klass::_super_check_offset",       20L);
    private static final long NAME_OFFSET                    = findOffsetOrDefault("Klass::_name",                     24L);
    private static final long SECONDARY_SUPER_CACHE_OFFSET   = findOffsetOrDefault("Klass::_secondary_super_cache",    32L);
    private static final long SECONDARY_SUPERS_OFFSET        = findOffsetOrDefault("Klass::_secondary_supers",         40L);
    private static final long PRIMARY_SUPERS_OFFSET          = findOffsetOrDefault("Klass::_primary_supers",           48L);
    private static final long JAVA_MIRROR_OFFSET             = findOffsetOrDefault("Klass::_java_mirror",              112L);
    private static final long SUPER_OFFSET                   = findOffsetOrDefault("Klass::_super",                    120L);
    private static final long SUBKLASS_OFFSET                = findOffsetOrDefault("Klass::_subklass",                 128L);
    private static final long NEXT_SIBLING_OFFSET            = findOffsetOrDefault("Klass::_next_sibling",             136L);
    private static final long NEXT_LINK_OFFSET               = findOffsetOrDefault("Klass::_next_link",                144L);
    private static final long CLASS_LOADER_DATA_OFFSET       = findOffsetOrDefault("Klass::_class_loader_data",        152L);
    private static final long VTABLE_LEN_OFFSET              = findOffsetOrDefault("Klass::_vtable_len",               160L);
    private static final long ACCESS_FLAGS_OFFSET            = findOffsetOrDefault("Klass::_access_flags",             164L);
    private static final long TRACE_ID_OFFSET                = findOffsetOrDefault("Klass::_trace_id",                 168L);
    private static final long SHARED_CLASS_PATH_INDEX_OFFSET = findOffsetOrDefault("Klass::_shared_class_path_index",  176L);
    private static final long SHARED_CLASS_FLAGS_OFFSET      = findOffsetOrDefault("Klass::_shared_class_flags",       178L);
    private static final long ARCHIVED_MIRROR_INDEX_OFFSET   = findOffsetOrDefault("Klass::_archived_mirror_index",    180L);

    public int layoutHelper;

    public int kind;

    public int modifierFlags;

    public int superCheckOffset;

    public long name;

    public long secondarySuperCache;

    public long secondarySupers;

    public long[] primarySupers = new long[PRIMARY_SUPER_LIMIT];

    public long javaMirror;

    public long superKlass;

    public long subklass;

    public long nextSibling;

    public long nextLink;

    public long classLoaderData;

    public int vtableLen;

    public int accessFlags;

    public long traceId;

    public short sharedClassPathIndex;

    public short sharedClassFlags;

    public int archivedMirrorIndex;

    public Klass(long address) {
        super(address);
        readFields();
    }

    public Klass(Class<?> clazz) {
        super(clazz);
        readFields();
    }

    private void readFields() {
        final long base = getAddress();
        final Unsafe u = UnsafeUtils.unsafe;

        layoutHelper = u.getInt(base + LAYOUT_HELPER_OFFSET);
        kind = u.getInt(base + KIND_OFFSET);
        modifierFlags = u.getInt(base + MODIFIER_FLAGS_OFFSET);
        superCheckOffset = u.getInt(base + SUPER_CHECK_OFFSET_OFFSET);
        name = u.getLong(base + NAME_OFFSET);
        secondarySuperCache = u.getLong(base + SECONDARY_SUPER_CACHE_OFFSET);
        secondarySupers = u.getLong(base + SECONDARY_SUPERS_OFFSET);
        for (int i = 0; i < PRIMARY_SUPER_LIMIT; i++) {
            primarySupers[i] = u.getLong(base + PRIMARY_SUPERS_OFFSET + i * 8L);
        }
        javaMirror = u.getLong(base + JAVA_MIRROR_OFFSET);
        superKlass = u.getLong(base + SUPER_OFFSET);
        subklass = u.getLong(base + SUBKLASS_OFFSET);
        nextSibling = u.getLong(base + NEXT_SIBLING_OFFSET);
        nextLink = u.getLong(base + NEXT_LINK_OFFSET);
        classLoaderData = u.getLong(base + CLASS_LOADER_DATA_OFFSET);
        vtableLen = u.getInt(base + VTABLE_LEN_OFFSET);
        accessFlags = u.getInt(base + ACCESS_FLAGS_OFFSET);
        traceId = u.getLong(base + TRACE_ID_OFFSET);
        sharedClassPathIndex = u.getShort(base + SHARED_CLASS_PATH_INDEX_OFFSET);
        sharedClassFlags = u.getShort(base + SHARED_CLASS_FLAGS_OFFSET);
        archivedMirrorIndex = u.getInt(base + ARCHIVED_MIRROR_INDEX_OFFSET);
    }

    public void refresh() {
        readFields();
    }

    private void writeInt(long offset, int value) {
        UnsafeUtils.unsafe.storeFence();
        UnsafeUtils.unsafe.putInt(getAddress() + offset, value);
        UnsafeUtils.unsafe.storeFence();
        readFields();
    }

    public int kind() {
        return kind;
    }

    public String kindName() {
        int k = kind;
        return (k >= 0 && k < KIND_NAMES.length) ? KIND_NAMES[k] : ("Unknown(" + k + ")");
    }

    public boolean isInstanceKlass() {
        return kind <= INSTANCE_STACK_CHUNK_KLASS_KIND;
    }

    public boolean isOtherInstanceKlass() {
        return kind == INSTANCE_KLASS_KIND;
    }

    public boolean isReferenceInstanceKlass() {
        return kind == INSTANCE_REF_KLASS_KIND;
    }

    public boolean isMirrorInstanceKlass() {
        return kind == INSTANCE_MIRROR_KLASS_KIND;
    }

    public boolean isClassLoaderInstanceKlass() {
        return kind == INSTANCE_CLASS_LOADER_KLASS_KIND;
    }

    public boolean isStackChunkInstanceKlass() {
        return kind == INSTANCE_STACK_CHUNK_KLASS_KIND;
    }

    public boolean isArrayKlass() {
        return kind >= TYPE_ARRAY_KLASS_KIND;
    }

    public boolean isTypeArrayKlass() {
        return kind == TYPE_ARRAY_KLASS_KIND;
    }

    public boolean isObjArrayKlass() {
        return kind == OBJ_ARRAY_KLASS_KIND;
    }

    public int layoutHelper() {
        return layoutHelper;
    }

    public void putLayoutHelper(int value) {
        writeInt(LAYOUT_HELPER_OFFSET, value);
    }

    public boolean layoutHelperIsInstance() {
        return layoutHelper > LH_NEUTRAL_VALUE;
    }

    public boolean layoutHelperIsArray() {
        return layoutHelper < LH_NEUTRAL_VALUE;
    }

    public boolean layoutHelperIsTypeArray() {
        return layoutHelperIsArray()
                && ((layoutHelper >>> LH_ARRAY_TAG_SHIFT) & 0x3) == LH_ARRAY_TAG_TYPE_VALUE;
    }

    public boolean layoutHelperIsObjArray() {
        return layoutHelperIsArray()
                && ((layoutHelper >>> LH_ARRAY_TAG_SHIFT) & 0x3) == LH_ARRAY_TAG_OBJ_VALUE;
    }

    public int layoutHelperSizeInBytes() {
        return layoutHelper & ~LH_INSTANCE_SLOW_PATH_BIT;
    }

    public boolean layoutHelperNeedsSlowPath() {
        return (layoutHelper & LH_INSTANCE_SLOW_PATH_BIT) != 0;
    }

    public int layoutHelperLog2ElementSize() {
        return (layoutHelper >>> LH_LOG2_ELEMENT_SIZE_SHIFT) & LH_LOG2_ELEMENT_SIZE_MASK;
    }

    public int layoutHelperElementType() {
        return (layoutHelper >>> LH_ELEMENT_TYPE_SHIFT) & LH_ELEMENT_TYPE_MASK;
    }

    public int layoutHelperHeaderSize() {
        return (layoutHelper >>> LH_HEADER_SIZE_SHIFT) & LH_HEADER_SIZE_MASK;
    }

    public static int instanceLayoutHelper(int size, boolean slowPath) {
        return (size << 3) | (slowPath ? LH_INSTANCE_SLOW_PATH_BIT : 0);
    }

    public static int arrayLayoutHelper(int tag, int headerSize, int elementType, int log2ElementSize) {
        return (tag << LH_ARRAY_TAG_SHIFT)
                | (headerSize << LH_HEADER_SIZE_SHIFT)
                | (elementType << LH_ELEMENT_TYPE_SHIFT)
                | (log2ElementSize << LH_LOG2_ELEMENT_SIZE_SHIFT);
    }

    public long name() {
        return name;
    }

    public String getName() {
        long sym = name;
        if (sym == 0) {
            return null;
        }
        return new Symbol(sym).value;
    }

    public String getExternalName() {
        String n = getName();
        return n == null ? null : n.replace('/', '.');
    }

    public long superKlass() {
        return superKlass;
    }

    public long subklass() {
        return subklass;
    }

    public long nextSibling() {
        return nextSibling;
    }

    public long nextLink() {
        return nextLink;
    }

    public long classLoaderData() {
        return classLoaderData;
    }

    public long javaMirror() {
        return javaMirror;
    }

    public int superCheckOffset() {
        return superCheckOffset;
    }

    public long secondarySuperCache() {
        return secondarySuperCache;
    }

    public long secondarySupers() {
        return secondarySupers;
    }

    public boolean canBePrimarySuper() {
        return superCheckOffset != (int) SECONDARY_SUPER_CACHE_OFFSET;
    }

    public int superDepth() {
        if (!canBePrimarySuper()) {
            return PRIMARY_SUPER_LIMIT;
        }
        int depth = (superCheckOffset - (int) PRIMARY_SUPERS_OFFSET) / 8;
        return (depth < 0 || depth >= PRIMARY_SUPER_LIMIT) ? PRIMARY_SUPER_LIMIT : depth;
    }

    public long primarySuperOfDepth(int depth) {
        if (depth < 0 || depth >= PRIMARY_SUPER_LIMIT) {
            throw new IndexOutOfBoundsException("depth: " + depth);
        }
        return primarySupers[depth];
    }

    public int vtableLength() {
        return vtableLen;
    }

    public int modifierFlags() {
        return modifierFlags;
    }

    public void putModifierFlags(int flags) {
        writeInt(MODIFIER_FLAGS_OFFSET, flags);
    }

    public int accessFlags() {
        return accessFlags;
    }

    public void putAccessFlags(int flags) {
        writeInt(ACCESS_FLAGS_OFFSET, flags);
    }

    public boolean isPublic() {
        return (accessFlags & JVM_ACC_PUBLIC) != 0;
    }

    public boolean isPrivate() {
        return (accessFlags & JVM_ACC_PRIVATE) != 0;
    }

    public boolean isProtected() {
        return (accessFlags & JVM_ACC_PROTECTED) != 0;
    }

    public boolean isStatic() {
        return (accessFlags & JVM_ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (accessFlags & JVM_ACC_FINAL) != 0;
    }

    public boolean isSuper() {
        return (accessFlags & JVM_ACC_SUPER) != 0;
    }

    public boolean isInterface() {
        return (accessFlags & JVM_ACC_INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return (accessFlags & JVM_ACC_ABSTRACT) != 0;
    }

    public boolean isSynthetic() {
        return (accessFlags & JVM_ACC_SYNTHETIC) != 0;
    }

    public boolean isAnnotation() {
        return (accessFlags & JVM_ACC_ANNOTATION) != 0;
    }

    public boolean isEnum() {
        return (accessFlags & JVM_ACC_ENUM) != 0;
    }

    public boolean hasFinalizer() {
        return (accessFlags & JVM_ACC_HAS_FINALIZER) != 0;
    }

    public boolean isCloneableFast() {
        return (accessFlags & JVM_ACC_IS_CLONEABLE_FAST) != 0;
    }

    public boolean isHidden() {
        return (accessFlags & JVM_ACC_IS_HIDDEN_CLASS) != 0;
    }

    public boolean isValueBased() {
        return (accessFlags & JVM_ACC_IS_VALUE_BASED_CLASS) != 0;
    }

    public long traceId() {
        return traceId;
    }

    public short sharedClassPathIndex() {
        return sharedClassPathIndex;
    }

    public short sharedClassFlags() {
        return sharedClassFlags;
    }

    public boolean isShared() {
        return (sharedClassFlags & SHARED_CLASS_IS_SHARED) != 0;
    }

    public boolean lambdaProxyIsAvailable() {
        return (sharedClassFlags & SHARED_ARCHIVED_LAMBDA_PROXY_AVAILABLE) != 0;
    }

    public boolean hasValueBasedClassAnnotation() {
        return (sharedClassFlags & SHARED_HAS_VALUE_BASED_ANNOTATION) != 0;
    }

    public boolean verifiedAtDumpTime() {
        return (sharedClassFlags & SHARED_VERIFIED_AT_DUMP_TIME) != 0;
    }

    public boolean hasArchivedEnumObjs() {
        return (sharedClassFlags & SHARED_HAS_ARCHIVED_ENUM_OBJS) != 0;
    }

    public boolean isGeneratedSharedClass() {
        return (sharedClassFlags & SHARED_IS_GENERATED_SHARED_CLASS) != 0;
    }

    public int archivedMirrorIndex() {
        return archivedMirrorIndex;
    }

    public boolean hasArchivedMirrorIndex() {
        return archivedMirrorIndex >= 0;
    }

    @Override
    public boolean isKlass() {
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "Klass[address=0x%x, kind=%s, layoutHelper=%d, name=0x%x, super=0x%x, "
                        + "loaderData=0x%x, vtableLen=%d, accessFlags=0x%x]",
                getAddress(), kindName(), layoutHelper, name, superKlass,
                classLoaderData, vtableLen, accessFlags);
    }
}
