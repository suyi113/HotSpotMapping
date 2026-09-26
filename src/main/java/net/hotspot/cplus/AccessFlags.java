package net.hotspot.cplus;


public class AccessFlags {

    public static final int JVM_ACC_PUBLIC        = 0x00000001;
    public static final int JVM_ACC_PRIVATE       = 0x00000002;
    public static final int JVM_ACC_PROTECTED     = 0x00000004;
    public static final int JVM_ACC_STATIC        = 0x00000008;
    public static final int JVM_ACC_FINAL         = 0x00000010;
    public static final int JVM_ACC_SYNCHRONIZED  = 0x00000020;
    public static final int JVM_ACC_SUPER         = 0x00000020;
    public static final int JVM_ACC_VOLATILE      = 0x00000040;
    public static final int JVM_ACC_TRANSIENT     = 0x00000080;
    public static final int JVM_ACC_NATIVE        = 0x00000100;
    public static final int JVM_ACC_INTERFACE     = 0x00000200;
    public static final int JVM_ACC_ABSTRACT      = 0x00000400;
    public static final int JVM_ACC_STRICT        = 0x00000800;
    public static final int JVM_ACC_SYNTHETIC     = 0x00001000;
    public static final int JVM_ACC_ANNOTATION    = 0x00002000;
    public static final int JVM_ACC_ENUM          = 0x00004000;
    public static final int JVM_ACC_MODULE        = 0x00008000;

    public static final int JVM_ACC_WRITTEN_FLAGS           = 0x00007FFF;

    public static final int JVM_ACC_MONITOR_MATCH           = 0x10000000;
    public static final int JVM_ACC_HAS_MONITOR_BYTECODES   = 0x20000000;
    public static final int JVM_ACC_HAS_LOOPS               = 0x40000000;
    public static final int JVM_ACC_LOOPS_FLAG_INIT         = 0x80000000;
    public static final int JVM_ACC_QUEUED                  = 0x01000000;
    public static final int JVM_ACC_NOT_C2_COMPILABLE       = 0x02000000;
    public static final int JVM_ACC_NOT_C1_COMPILABLE       = 0x04000000;
    public static final int JVM_ACC_NOT_C2_OSR_COMPILABLE   = 0x08000000;
    public static final int JVM_ACC_HAS_LINE_NUMBER_TABLE   = 0x00100000;
    public static final int JVM_ACC_HAS_CHECKED_EXCEPTIONS  = 0x00400000;
    public static final int JVM_ACC_HAS_JSRS                = 0x00800000;
    public static final int JVM_ACC_IS_OLD                  = 0x00010000;
    public static final int JVM_ACC_IS_OBSOLETE             = 0x00020000;
    public static final int JVM_ACC_IS_PREFIXED_NATIVE      = 0x00040000;
    public static final int JVM_ACC_ON_STACK                = 0x00080000;
    public static final int JVM_ACC_IS_DELETED              = 0x00008000;

    public static final int JVM_ACC_HAS_MIRANDA_METHODS     = 0x10000000;
    public static final int JVM_ACC_HAS_VANILLA_CONSTRUCTOR = 0x20000000;
    public static final int JVM_ACC_HAS_FINALIZER           = 0x40000000;
    public static final int JVM_ACC_IS_CLONEABLE_FAST       = 0x80000000;
    public static final int JVM_ACC_HAS_FINAL_METHOD        = 0x01000000;
    public static final int JVM_ACC_IS_SHARED_CLASS         = 0x02000000;
    public static final int JVM_ACC_IS_HIDDEN_CLASS         = 0x04000000;
    public static final int JVM_ACC_IS_VALUE_BASED_CLASS    = 0x08000000;

    public static final int JVM_ACC_HAS_LOCAL_VARIABLE_TABLE = 0x00200000;
    public static final int JVM_ACC_PROMOTED_FLAGS          = 0x00200000;

    public static final int JVM_ACC_FIELD_ACCESS_WATCHED           = 0x00002000;
    public static final int JVM_ACC_FIELD_MODIFICATION_WATCHED     = 0x00008000;
    public static final int JVM_ACC_FIELD_INTERNAL                 = 0x00000400;
    public static final int JVM_ACC_FIELD_STABLE                   = 0x00000020;
    public static final int JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE = 0x00000100;
    public static final int JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE    = 0x00000800;
    public static final int JVM_ACC_FIELD_FLAGS = 0x00008000 | JVM_ACC_WRITTEN_FLAGS;
    public static final int JVM_ACC_FIELD_INTERNAL_FLAGS =
            JVM_ACC_FIELD_ACCESS_WATCHED |
                    JVM_ACC_FIELD_MODIFICATION_WATCHED |
                    JVM_ACC_FIELD_INTERNAL |
                    JVM_ACC_FIELD_STABLE |
                    JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE;

    public enum Kind { KLASS, METHOD, FIELD }

    private int _flags;

    public AccessFlags() {
        this._flags = 0;
    }

    public AccessFlags(int flags) {
        this._flags = flags;
    }

    public static AccessFlags accessFlagsFrom(int flags) {
        return new AccessFlags(flags);
    }

    public boolean isPublic()       { return (_flags & JVM_ACC_PUBLIC)       != 0; }
    public boolean isPrivate()      { return (_flags & JVM_ACC_PRIVATE)      != 0; }
    public boolean isProtected()    { return (_flags & JVM_ACC_PROTECTED)    != 0; }
    public boolean isStatic()       { return (_flags & JVM_ACC_STATIC)       != 0; }
    public boolean isFinal()        { return (_flags & JVM_ACC_FINAL)        != 0; }
    public boolean isSynchronized() { return (_flags & JVM_ACC_SYNCHRONIZED) != 0; }
    public boolean isSuper()        { return (_flags & JVM_ACC_SUPER)        != 0; }
    public boolean isVolatile()     { return (_flags & JVM_ACC_VOLATILE)     != 0; }
    public boolean isTransient()    { return (_flags & JVM_ACC_TRANSIENT)    != 0; }
    public boolean isNative()       { return (_flags & JVM_ACC_NATIVE)       != 0; }
    public boolean isInterface()    { return (_flags & JVM_ACC_INTERFACE)    != 0; }
    public boolean isAbstract()     { return (_flags & JVM_ACC_ABSTRACT)     != 0; }
    public boolean isStrict()       { return (_flags & JVM_ACC_STRICT)       != 0; }
    public boolean isAnnotation()   { return (_flags & JVM_ACC_ANNOTATION)   != 0; }
    public boolean isEnum()         { return (_flags & JVM_ACC_ENUM)         != 0; }
    public boolean isModule()       { return (_flags & JVM_ACC_MODULE)       != 0; }

    public boolean isSynthetic()    { return (_flags & JVM_ACC_SYNTHETIC)    != 0; }

    public boolean isMonitorMatching()       { return (_flags & JVM_ACC_MONITOR_MATCH)          != 0; }
    public boolean hasMonitorBytecodes()     { return (_flags & JVM_ACC_HAS_MONITOR_BYTECODES)  != 0; }
    public boolean hasLoops()                { return (_flags & JVM_ACC_HAS_LOOPS)              != 0; }
    public boolean loopsFlagInit()           { return (_flags & JVM_ACC_LOOPS_FLAG_INIT)        != 0; }
    public boolean queuedForCompilation()    { return (_flags & JVM_ACC_QUEUED)                 != 0; }
    public boolean isNotC1Compilable()       { return (_flags & JVM_ACC_NOT_C1_COMPILABLE)      != 0; }
    public boolean isNotC2Compilable()       { return (_flags & JVM_ACC_NOT_C2_COMPILABLE)      != 0; }
    public boolean isNotC2OsrCompilable()    { return (_flags & JVM_ACC_NOT_C2_OSR_COMPILABLE)  != 0; }
    public boolean hasLinenumberTable()      { return (_flags & JVM_ACC_HAS_LINE_NUMBER_TABLE)  != 0; }
    public boolean hasCheckedExceptions()    { return (_flags & JVM_ACC_HAS_CHECKED_EXCEPTIONS) != 0; }
    public boolean hasJsrs()                 { return (_flags & JVM_ACC_HAS_JSRS)               != 0; }
    public boolean isOld()                   { return (_flags & JVM_ACC_IS_OLD)                 != 0; }
    public boolean isObsolete()              { return (_flags & JVM_ACC_IS_OBSOLETE)            != 0; }
    public boolean isDeleted()               { return (_flags & JVM_ACC_IS_DELETED)             != 0; }
    public boolean isPrefixedNative()        { return (_flags & JVM_ACC_IS_PREFIXED_NATIVE)     != 0; }

    public boolean hasMirandaMethods()       { return (_flags & JVM_ACC_HAS_MIRANDA_METHODS)    != 0; }
    public boolean hasVanillaConstructor()   { return (_flags & JVM_ACC_HAS_VANILLA_CONSTRUCTOR)!= 0; }
    public boolean hasFinalizer()            { return (_flags & JVM_ACC_HAS_FINALIZER)          != 0; }
    public boolean hasFinalMethod()          { return (_flags & JVM_ACC_HAS_FINAL_METHOD)       != 0; }
    public boolean isCloneableFast()         { return (_flags & JVM_ACC_IS_CLONEABLE_FAST)      != 0; }
    public boolean isSharedClass()           { return (_flags & JVM_ACC_IS_SHARED_CLASS)        != 0; }
    public boolean isHiddenClass()           { return (_flags & JVM_ACC_IS_HIDDEN_CLASS)        != 0; }
    public boolean isValueBasedClass()       { return (_flags & JVM_ACC_IS_VALUE_BASED_CLASS)   != 0; }

    public boolean hasLocalvariableTable()   { return (_flags & JVM_ACC_HAS_LOCAL_VARIABLE_TABLE) != 0; }
    public void setHasLocalvariableTable()   { atomicSetBits(JVM_ACC_HAS_LOCAL_VARIABLE_TABLE); }
    public void clearHasLocalvariableTable() { atomicClearBits(JVM_ACC_HAS_LOCAL_VARIABLE_TABLE); }

    public boolean isFieldAccessWatched()       { return (_flags & JVM_ACC_FIELD_ACCESS_WATCHED) != 0; }
    public boolean isFieldModificationWatched() { return (_flags & JVM_ACC_FIELD_MODIFICATION_WATCHED) != 0; }
    public boolean hasFieldInitializedFinalUpdate() { return (_flags & JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE) != 0; }
    public boolean onStack()                    { return (_flags & JVM_ACC_ON_STACK) != 0; }
    public boolean isInternal()                 { return (_flags & JVM_ACC_FIELD_INTERNAL) != 0; }
    public boolean isStable()                   { return (_flags & JVM_ACC_FIELD_STABLE) != 0; }
    public boolean fieldHasGenericSignature()   { return (_flags & JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE) != 0; }

    public int getFlags() {
        return _flags & JVM_ACC_WRITTEN_FLAGS;
    }

    public void addPromotedFlags(int flags) {
        _flags |= (flags & JVM_ACC_PROMOTED_FLAGS);
    }

    public void setFieldFlags(int flags) {
        _flags = flags & JVM_ACC_FIELD_FLAGS;
    }

    public void setFlags(int flags) {
        _flags = flags & JVM_ACC_WRITTEN_FLAGS;
    }

    public void setQueuedForCompilation()   { atomicSetBits(JVM_ACC_QUEUED); }
    public void clearQueuedForCompilation() { atomicClearBits(JVM_ACC_QUEUED); }

    public void atomicSetBits(int bits)   { _flags |= bits; }
    public void atomicClearBits(int bits) { _flags &= ~bits; }

    public void setIsSynthetic()              { atomicSetBits(JVM_ACC_SYNTHETIC); }

    public void setMonitorMatching()          { atomicSetBits(JVM_ACC_MONITOR_MATCH); }
    public void setHasMonitorBytecodes()      { atomicSetBits(JVM_ACC_HAS_MONITOR_BYTECODES); }
    public void setHasLoops()                 { atomicSetBits(JVM_ACC_HAS_LOOPS); }
    public void setLoopsFlagInit()            { atomicSetBits(JVM_ACC_LOOPS_FLAG_INIT); }
    public void setNotC1Compilable()          { atomicSetBits(JVM_ACC_NOT_C1_COMPILABLE); }
    public void setNotC2Compilable()          { atomicSetBits(JVM_ACC_NOT_C2_COMPILABLE); }
    public void setNotC2OsrCompilable()       { atomicSetBits(JVM_ACC_NOT_C2_OSR_COMPILABLE); }
    public void setHasLinenumberTable()       { atomicSetBits(JVM_ACC_HAS_LINE_NUMBER_TABLE); }
    public void setHasCheckedExceptions()     { atomicSetBits(JVM_ACC_HAS_CHECKED_EXCEPTIONS); }
    public void setHasJsrs()                  { atomicSetBits(JVM_ACC_HAS_JSRS); }
    public void setIsOld()                    { atomicSetBits(JVM_ACC_IS_OLD); }
    public void setIsObsolete()               { atomicSetBits(JVM_ACC_IS_OBSOLETE); }
    public void setIsDeleted()                { atomicSetBits(JVM_ACC_IS_DELETED); }
    public void setIsPrefixedNative()         { atomicSetBits(JVM_ACC_IS_PREFIXED_NATIVE); }

    public void clearNotC1Compilable()        { atomicClearBits(JVM_ACC_NOT_C1_COMPILABLE); }
    public void clearNotC2Compilable()        { atomicClearBits(JVM_ACC_NOT_C2_COMPILABLE); }
    public void clearNotC2OsrCompilable()     { atomicClearBits(JVM_ACC_NOT_C2_OSR_COMPILABLE); }

    public void setHasVanillaConstructor()    { atomicSetBits(JVM_ACC_HAS_VANILLA_CONSTRUCTOR); }
    public void setHasFinalizer()             { atomicSetBits(JVM_ACC_HAS_FINALIZER); }
    public void setHasFinalMethod()           { atomicSetBits(JVM_ACC_HAS_FINAL_METHOD); }
    public void setIsCloneableFast()          { atomicSetBits(JVM_ACC_IS_CLONEABLE_FAST); }
    public void setHasMirandaMethods()        { atomicSetBits(JVM_ACC_HAS_MIRANDA_METHODS); }
    public void setIsSharedClass()            { atomicSetBits(JVM_ACC_IS_SHARED_CLASS); }
    public void setIsHiddenClass()            { atomicSetBits(JVM_ACC_IS_HIDDEN_CLASS); }
    public void setIsValueBasedClass()        { atomicSetBits(JVM_ACC_IS_VALUE_BASED_CLASS); }

    public void setIsFieldAccessWatched(boolean value) {
        if (value) atomicSetBits(JVM_ACC_FIELD_ACCESS_WATCHED);
        else       atomicClearBits(JVM_ACC_FIELD_ACCESS_WATCHED);
    }

    public void setIsFieldModificationWatched(boolean value) {
        if (value) atomicSetBits(JVM_ACC_FIELD_MODIFICATION_WATCHED);
        else       atomicClearBits(JVM_ACC_FIELD_MODIFICATION_WATCHED);
    }

    public void setHasFieldInitializedFinalUpdate(boolean value) {
        if (value) atomicSetBits(JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE);
        else       atomicClearBits(JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE);
    }

    public void setFieldHasGenericSignature() {
        atomicSetBits(JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE);
    }

    public void setOnStack(boolean value) {
        if (value) atomicSetBits(JVM_ACC_ON_STACK);
        else       atomicClearBits(JVM_ACC_ON_STACK);
    }

    public short asShort() { return (short) _flags; }
    public int   asInt()   { return _flags; }

    @Override
    public String toString() {
        return toString(Kind.KLASS);
    }

    public String toString(Kind kind) {
        StringBuilder sb = new StringBuilder();
        switch (kind) {
            case KLASS:    appendKlass(sb);    break;
            case METHOD:   appendMethod(sb);   break;
            case FIELD:    appendField(sb);    break;
        }
        return sb.toString().trim();
    }

    private void appendKlass(StringBuilder sb) {
        if (isPublic())    sb.append("public ");
        if (isPrivate())   sb.append("private ");
        if (isProtected()) sb.append("protected ");
        if (isFinal())     sb.append("final ");
        if (isAbstract() && !isInterface()) sb.append("abstract ");

        if (isAnnotation()) {
            sb.append("@interface ");
        } else if (isInterface()) {
            sb.append("interface ");
        } else if (isEnum()) {
            sb.append("enum ");
        } else {
            sb.append("class ");
        }

        if (isSynthetic()) sb.append("/* synthetic */ ");
        if (isSharedClass()) sb.append("/* shared */ ");
        if (isHiddenClass()) sb.append("/* hidden */ ");
        if (isValueBasedClass()) sb.append("/* value-based */ ");
    }

    private void appendMethod(StringBuilder sb) {
        if (isPublic())       sb.append("public ");
        if (isPrivate())      sb.append("private ");
        if (isProtected())    sb.append("protected ");
        if (isStatic())       sb.append("static ");
        if (isFinal())        sb.append("final ");
        if (isSynchronized()) sb.append("synchronized ");
        if (isNative())       sb.append("native ");
        if (isAbstract())     sb.append("abstract ");
        if (isStrict())       sb.append("strictfp ");

        if (isSynthetic()) sb.append("/* synthetic */ ");
    }

    private void appendField(StringBuilder sb) {
        if (isPublic())    sb.append("public ");
        if (isPrivate())   sb.append("private ");
        if (isProtected()) sb.append("protected ");
        if (isStatic())    sb.append("static ");
        if (isFinal())     sb.append("final ");
        if (isVolatile())  sb.append("volatile ");
        if (isTransient()) sb.append("transient ");

        if (isEnum())      sb.append("/* enum */ ");
        if (isSynthetic()) sb.append("/* synthetic */ ");
        if (isStable())    sb.append("/* stable */ ");
        if (isInternal())  sb.append("/* internal */ ");
    }
}