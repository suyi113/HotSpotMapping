package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class Klass extends Metadata {

    private static final long LAYOUT_HELPER_OFFSET                         = findOffsetOrDefault("Klass::_layout_helper",                          8L);
    private static final long ID_OFFSET                                    = findOffsetOrDefault("Klass::_id",                                     12L);
    private static final long VTABLE_LEN_OFFSET                            = findOffsetOrDefault("Klass::_vtable_len",                             160L);
    private static final long SUPER_CHECK_OFFSET_OFFSET                    = findOffsetOrDefault("Klass::_super_check_offset",                     20L);
    private static final long NAME_OFFSET                                  = findOffsetOrDefault("Klass::_name",                                   24L);
    private static final long SECONDARY_SUPER_CACHE_OFFSET                 = findOffsetOrDefault("Klass::_secondary_super_cache",                  32L);
    private static final long SECONDARY_SUPERS_OFFSET                      = findOffsetOrDefault("Klass::_secondary_supers",                       40L);
    private static final long PRIMARY_SUPERS_OFFSET                        = findOffsetOrDefault("Klass::_primary_supers",                         48L);
    private static final long JAVA_MIRROR_OFFSET                           = findOffsetOrDefault("Klass::_java_mirror",                            112L);
    private static final long SUPER_OFFSET                                 = findOffsetOrDefault("Klass::_super",                                  120L);
    private static final long SUBKLASS_OFFSET                              = findOffsetOrDefault("Klass::_subklass",                               128L);
    private static final long NEXT_SIBLING_OFFSET                          = findOffsetOrDefault("Klass::_next_sibling",                           136L);
    private static final long NEXT_LINK_OFFSET                             = findOffsetOrDefault("Klass::_next_link",                              144L);
    private static final long CLASS_LOADER_DATA_OFFSET                     = findOffsetOrDefault("Klass::_class_loader_data",                      152L);
    private static final long MODIFIER_FLAGS_OFFSET                        = findOffsetOrDefault("Klass::_modifier_flags",                         16L);
    private static final long ACCESS_FLAGS_OFFSET                          = findOffsetOrDefault("Klass::_access_flags",                           164L);
    private static final long LAST_BIASED_LOCK_BULK_REVOCATION_TIME_OFFSET = findOffsetOrDefault("Klass::_last_biased_lock_bulk_revocation_time", 168L);
    private static final long PROTOTYPE_HEADER_OFFSET                      = findOffsetOrDefault("Klass::_prototype_header",                       176L);
    private static final long BIASED_LOCK_REVOCATION_COUNT_OFFSET          = findOffsetOrDefault("Klass::_biased_lock_revocation_count",           184L);
    private static final long SHARED_CLASS_PATH_INDEX_OFFSET               = findOffsetOrDefault("Klass::_shared_class_path_index",                188L);

    public int layoutHelper;
    public int id;
    public int vtableLen;
    public int superCheckOffset;
    public long name;
    public long secondarySuperCache;
    public long secondarySupers;
    public long[] primarySupers = new long[8];
    public long javaMirror;
    public long superKlass;
    public long subklass;
    public long nextSibling;
    public long nextLink;
    public long classLoaderData;
    public int modifierFlags;
    public int accessFlags;
    public long lastBiasedLockBulkRevocationTime;
    public long prototypeHeader;
    public int biasedLockRevocationCount;
    public short sharedClassPathIndex;

    public Klass(long address) {
        super(address);
        readFields();
    }

    public Klass(Class<?> clazz) {
        super(clazz);
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        layoutHelper = UnsafeUtils.unsafe.getInt(base + LAYOUT_HELPER_OFFSET);
        id = UnsafeUtils.unsafe.getInt(base + ID_OFFSET);
        vtableLen = UnsafeUtils.unsafe.getInt(base + VTABLE_LEN_OFFSET);
        superCheckOffset = UnsafeUtils.unsafe.getInt(base + SUPER_CHECK_OFFSET_OFFSET);
        name = UnsafeUtils.unsafe.getLong(base + NAME_OFFSET);
        secondarySuperCache = UnsafeUtils.unsafe.getLong(base + SECONDARY_SUPER_CACHE_OFFSET);
        secondarySupers = UnsafeUtils.unsafe.getLong(base + SECONDARY_SUPERS_OFFSET);
        for (int i = 0; i < 8; i++) {
            primarySupers[i] = UnsafeUtils.unsafe.getLong(base + PRIMARY_SUPERS_OFFSET + i * 8L);
        }
        javaMirror = UnsafeUtils.unsafe.getLong(base + JAVA_MIRROR_OFFSET);
        superKlass = UnsafeUtils.unsafe.getLong(base + SUPER_OFFSET);
        subklass = UnsafeUtils.unsafe.getLong(base + SUBKLASS_OFFSET);
        nextSibling = UnsafeUtils.unsafe.getLong(base + NEXT_SIBLING_OFFSET);
        nextLink = UnsafeUtils.unsafe.getLong(base + NEXT_LINK_OFFSET);
        classLoaderData = UnsafeUtils.unsafe.getLong(base + CLASS_LOADER_DATA_OFFSET);
        modifierFlags = UnsafeUtils.unsafe.getInt(base + MODIFIER_FLAGS_OFFSET);
        accessFlags = UnsafeUtils.unsafe.getInt(base + ACCESS_FLAGS_OFFSET);
        lastBiasedLockBulkRevocationTime = UnsafeUtils.unsafe.getLong(base + LAST_BIASED_LOCK_BULK_REVOCATION_TIME_OFFSET);
        prototypeHeader = UnsafeUtils.unsafe.getLong(base + PROTOTYPE_HEADER_OFFSET);
        biasedLockRevocationCount = UnsafeUtils.unsafe.getInt(base + BIASED_LOCK_REVOCATION_COUNT_OFFSET);
        sharedClassPathIndex = UnsafeUtils.unsafe.getShort(base + SHARED_CLASS_PATH_INDEX_OFFSET);
    }

    @Override
    public boolean isKlass() {
        return true;
    }

    public int id() {
        return id;
    }

    public int layoutHelper() {
        return layoutHelper;
    }

    public long name() {
        return name;
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

    public int modifierFlags() {
        return modifierFlags;
    }

    public void putModifierFlags(int accessFlags) {
        UnsafeUtils.unsafe.storeFence();
        UnsafeUtils.unsafe.putInt(getAddress() + MODIFIER_FLAGS_OFFSET,accessFlags);
        UnsafeUtils.unsafe.storeFence();
        readFields();
    }

    public int accessFlags() {
        return accessFlags;
    }

    public void putAccessFlags(int accessFlags){
        UnsafeUtils.unsafe.storeFence();
        UnsafeUtils.unsafe.putInt(getAddress() + ACCESS_FLAGS_OFFSET,accessFlags);
        UnsafeUtils.unsafe.storeFence();
        readFields();
    }

    public int vtableLength() {
        return vtableLen;
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

    public long primarySuperOfDepth(int depth) {
        if (depth < 0 || depth >= 8) {
            throw new IndexOutOfBoundsException("depth: " + depth);
        }
        return primarySupers[depth];
    }

    public long prototypeHeader() {
        return prototypeHeader;
    }

    public int biasedLockRevocationCount() {
        return biasedLockRevocationCount;
    }

    public long lastBiasedLockBulkRevocationTime() {
        return lastBiasedLockBulkRevocationTime;
    }

    public short sharedClassPathIndex() {
        return sharedClassPathIndex;
    }

    public boolean isInstanceKlass() {
        return layoutHelper > 0;
    }

    public boolean isArrayKlass() {
        return layoutHelper < 0;
    }

    public boolean isPublic() {
        return (accessFlags & 0x0001) != 0;
    }

    public boolean isFinal() {
        return (accessFlags & 0x0010) != 0;
    }

    public boolean isInterface() {
        return (accessFlags & 0x0200) != 0;
    }

    public boolean isAbstract() {
        return (accessFlags & 0x0400) != 0;
    }

    public boolean isSynthetic() {
        return (accessFlags & 0x1000) != 0;
    }

    public String getName() {
        if (name == 0) return null;
        return new Symbol(name).value;
    }

    public String getExternalName() {
        String n = getName();
        return n == null ? null : n.replace('/', '.');
    }
    @Override
    public String toString() {
        return String.format(
                "Klass[address=0x%x, id=%d, layoutHelper=%d, name=0x%x, super=0x%x, loaderData=0x%x, accessFlags=0x%x]",
                getAddress(), id, layoutHelper, name, superKlass, classLoaderData, accessFlags);
    }
}