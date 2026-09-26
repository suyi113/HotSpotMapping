package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiInstanceKlass extends CiKlass {

    private static final long LOADER_OFF                       = 40L;
    private static final long PROTECTION_DOMAIN_OFF            = 48L;
    private static final long INIT_STATE_OFF                   = 56L;
    private static final long IS_SHARED_OFF                    = 60L;
    private static final long HAS_FINALIZER_OFF                = 61L;
    private static final long HAS_SUBKLASS_OFF                 = 64L;
    private static final long HAS_NONSTATIC_FIELDS_OFF         = 68L;
    private static final long HAS_NONSTATIC_CONCRETE_METHODS_OFF = 69L;
    private static final long IS_HIDDEN_OFF                    = 70L;
    private static final long IS_RECORD_OFF                    = 71L;
    private static final long FLAGS_OFF                        = 72L;
    private static final long NONSTATIC_FIELD_SIZE_OFF         = 76L;
    private static final long NONSTATIC_OOP_MAP_SIZE_OFF       = 80L;
    private static final long SUPER_OFF                        = 88L;
    private static final long JAVA_MIRROR_OFF                  = 96L;
    private static final long FIELD_CACHE_OFF                  = 104L;
    private static final long NONSTATIC_FIELDS_OFF             = 112L;
    private static final long HAS_INJECTED_FIELDS_OFF          = 120L;
    private static final long IMPLEMENTOR_OFF                  = 128L;

    public static final int STATE_ALLOCATED            = 0;
    public static final int STATE_LOADED               = 1;
    public static final int STATE_LINKED               = 2;
    public static final int STATE_BEING_INITIALIZED    = 3;
    public static final int STATE_FULLY_INITIALIZED    = 4;
    public static final int STATE_INITIALIZATION_ERROR = 5;

    public static final int SUBKLASS_UNKNOWN = 0;
    public static final int SUBKLASS_FALSE   = 1;
    public static final int SUBKLASS_TRUE    = 2;

    public CiInstanceKlass(long address) { super(address); }

    public long instanceKlassAddr() { return metadataAddr(); }

    public InstanceKlass instanceKlass() {
        long p = metadataAddr();
        return p == 0 ? null : new InstanceKlass(p);
    }

    public int initState() {
        return UnsafeUtils.unsafe.getInt(address + INIT_STATE_OFF);
    }

    public boolean isInitialized()     { return initState() == STATE_FULLY_INITIALIZED; }
    public boolean isNotInitialized()  { return initState() <  STATE_BEING_INITIALIZED; }
    public boolean isBeingInitialized(){ return initState() == STATE_BEING_INITIALIZED; }
    public boolean isLinked()          { return initState() >= STATE_LINKED; }
    public boolean isInErrorState()    { return initState() == STATE_INITIALIZATION_ERROR; }

    public boolean isShared() {
        return UnsafeUtils.unsafe.getByte(address + IS_SHARED_OFF) != 0;
    }
    public boolean hasFinalizer() {
        return UnsafeUtils.unsafe.getByte(address + HAS_FINALIZER_OFF) != 0;
    }
    public boolean hasNonstaticFields() {
        return UnsafeUtils.unsafe.getByte(address + HAS_NONSTATIC_FIELDS_OFF) != 0;
    }
    public boolean hasNonstaticConcreteMethods() {
        return UnsafeUtils.unsafe.getByte(address + HAS_NONSTATIC_CONCRETE_METHODS_OFF) != 0;
    }
    public boolean isHidden() {
        return UnsafeUtils.unsafe.getByte(address + IS_HIDDEN_OFF) != 0;
    }
    public boolean isRecord() {
        return UnsafeUtils.unsafe.getByte(address + IS_RECORD_OFF) != 0;
    }

    public int hasSubklassRaw() {
        return UnsafeUtils.unsafe.getInt(address + HAS_SUBKLASS_OFF);
    }
    public boolean hasSubklass() {
        int v = hasSubklassRaw();
        if (v == SUBKLASS_TRUE) return true;
        if (isFinal()) return false;
        return true;
    }

    public int nonstaticFieldSize() {
        return UnsafeUtils.unsafe.getInt(address + NONSTATIC_FIELD_SIZE_OFF);
    }
    public int nonstaticOopMapSize() {
        return UnsafeUtils.unsafe.getInt(address + NONSTATIC_OOP_MAP_SIZE_OFF);
    }

    public int flags() {
        return UnsafeUtils.unsafe.getInt(address + FLAGS_OFF);
    }
    public boolean isPublic()    { return (flags() & 0x0001) != 0; }
    public boolean isFinal()     { return (flags() & 0x0010) != 0; }
    public boolean isSuper()     { return (flags() & 0x0020) != 0; }
    public boolean isInterface() { return (flags() & 0x0200) != 0; }
    public boolean isAbstract()  { return (flags() & 0x0400) != 0; }

    public CiInstanceKlass superKlass() {
        long p = UnsafeUtils.unsafe.getLong(address + SUPER_OFF);
        return p == 0 ? null : new CiInstanceKlass(p);
    }

    public CiInstance javaMirror() {
        long p = UnsafeUtils.unsafe.getLong(address + JAVA_MIRROR_OFF);
        return p == 0 ? null : new CiInstance(p);
    }

    public CiConstantPoolCache fieldCache() {
        long p = UnsafeUtils.unsafe.getLong(address + FIELD_CACHE_OFF);
        return p == 0 ? null : new CiConstantPoolCache(p);
    }

    public long nonstaticFieldsAddr() {
        return UnsafeUtils.unsafe.getLong(address + NONSTATIC_FIELDS_OFF);
    }

    public int hasInjectedFieldsRaw() {
        return UnsafeUtils.unsafe.getInt(address + HAS_INJECTED_FIELDS_OFF);
    }

    public boolean hasInjectedFields() {
        return hasInjectedFieldsRaw() > 0;
    }

    public CiInstanceKlass implementor() {
        long p = UnsafeUtils.unsafe.getLong(address + IMPLEMENTOR_OFF);
        return p == 0 ? null : new CiInstanceKlass(p);
    }

    public int nofImplementors() {
        CiInstanceKlass impl = implementor();
        if (impl == null) return 0;
        if (impl.getAddress() != address) return 1;
        return 2;
    }

    @Override
    public String toString() {
        CiSymbol n = name();
        return String.format(
                "CiInstanceKlass[addr=0x%x, name=%s, state=%d, " +
                        "isShared=%b, hasFinalizer=%b, nonstaticFieldSize=%d, " +
                        "super=%s, implementor=%s]",
                address,
                n == null ? "?" : n.value(),
                initState(),
                isShared(), hasFinalizer(), nonstaticFieldSize(),
                superKlass() == null ? "null"
                        : (superKlass().name() == null ? "?" : superKlass().name().value()),
                implementor() == null ? "null" : "0x" + Long.toHexString(implementor().getAddress()));
    }
}