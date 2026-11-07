package net.hotspot.cplus.jdk21.klass;

public class FieldInfo {

    public static final int FF_INITIALIZED = 1 << 0;
    public static final int FF_INJECTED    = 1 << 1;
    public static final int FF_GENERIC     = 1 << 2;
    public static final int FF_STABLE      = 1 << 3;
    public static final int FF_CONTENDED   = 1 << 4;

    public int index;

    public int nameIndex;

    public int signatureIndex;

    public int offset;

    public int accessFlags;

    public int fieldFlags;

    public int initializerIndex;

    public int genericSignatureIndex;

    public int contentionGroup;

    public boolean isInitialized()  { return (fieldFlags & FF_INITIALIZED) != 0; }
    public boolean isInjected()     { return (fieldFlags & FF_INJECTED) != 0; }
    public boolean isGeneric()      { return (fieldFlags & FF_GENERIC) != 0; }
    public boolean isStable()       { return (fieldFlags & FF_STABLE) != 0; }
    public boolean isContended()    { return (fieldFlags & FF_CONTENDED) != 0; }
    public boolean isOffsetSet()    { return offset != 0; }

    public boolean isStatic()       { return (accessFlags & AccessFlags.JVM_ACC_STATIC) != 0; }
    public boolean isFinal()        { return (accessFlags & AccessFlags.JVM_ACC_FINAL) != 0; }

    public String name(ConstantPool cp) {
        if (cp == null || nameIndex == 0) return null;
        long sym = cp.symbolAt(nameIndex);
        return sym == 0 ? null : new Symbol(sym).value;
    }

    public String signature(ConstantPool cp) {
        if (cp == null || signatureIndex == 0) return null;
        long sym = cp.symbolAt(signatureIndex);
        return sym == 0 ? null : new Symbol(sym).value;
    }

    @Override
    public String toString() {
        return String.format(
                "FieldInfo[index=%d, nameIndex=%d, sigIndex=%d, offset=%d, accessFlags=0x%04x, "
                        + "fieldFlags=0x%02x, initval=%d, genericSig=%d, contendedGroup=%d]",
                index, nameIndex, signatureIndex, offset, accessFlags & 0xFFFF,
                fieldFlags, initializerIndex, genericSignatureIndex, contentionGroup);
    }
}
