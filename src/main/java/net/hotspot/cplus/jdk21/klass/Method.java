package net.hotspot.cplus.jdk21.klass;

import net.hotspot.cplus.jdk21.codecache.CodeBuilder;
import net.hotspot.cplus.jdk21.codecache.CompiledMethod;
import net.hotspot.cplus.jdk21.codecache.NMethod;
import net.hotspot.cplus.jdk21.core.Metadata;
import net.hotspot.cplus.jdk21.core.VMStructsHelper;

import net.hotspot.cplus.jdk21.core.UnsafeUtils;

import static net.hotspot.cplus.jdk21.core.VMStructsHelper.findOffsetOrDefault;

public class Method extends Metadata {

    private static final long CONST_METHOD_OFFSET           = findOffsetOrDefault("Method::_constMethod",            8L);
    private static final long METHOD_DATA_OFFSET            = findOffsetOrDefault("Method::_method_data",            16L);
    private static final long METHOD_COUNTERS_OFFSET        = findOffsetOrDefault("Method::_method_counters",        24L);
    private static final long ADAPTER_OFFSET                = findOffsetOrDefault("Method::_adapter",                32L);
    private static final long ACCESS_FLAGS_OFFSET           = findOffsetOrDefault("Method::_access_flags",           40L);
    private static final long VTABLE_INDEX_OFFSET           = findOffsetOrDefault("Method::_vtable_index",           44L);
    private static final long FLAGS_OFFSET                  = findOffsetOrDefault("Method::_flags",                  48L);
    private static final long INTRINSIC_ID_OFFSET           = findOffsetOrDefault("Method::_intrinsic_id",           52L);
    private static final long I2I_ENTRY_OFFSET              = findOffsetOrDefault("Method::_i2i_entry",              56L);
    private static final long FROM_COMPILED_ENTRY_OFFSET    = findOffsetOrDefault("Method::_from_compiled_entry",    64L);
    private static final long CODE_OFFSET                   = findOffsetOrDefault("Method::_code",                   72L);
    private static final long FROM_INTERPRETED_ENTRY_OFFSET = findOffsetOrDefault("Method::_from_interpreted_entry", 80L);

    public static final int NONVIRTUAL_VTABLE_INDEX = -2;

    public long  constMethod;
    public long  methodData;
    public long  methodCounters;
    public long  adapter;
    public int   accessFlags;
    public int   vtableIndex;
    public int   flags;
    public short intrinsicId;
    public long  i2iEntry;
    public long  fromCompiledEntry;
    public long  code;
    public long  fromInterpretedEntry;

    public Method(long address) {
        super(address);
        if (address == 0) return;
        readFields();
    }

    public void readFields() {
        long base = getAddress();
        if (base == 0) return;
        constMethod          = UnsafeUtils.unsafe.getLong(base + CONST_METHOD_OFFSET);
        methodData           = UnsafeUtils.unsafe.getLong(base + METHOD_DATA_OFFSET);
        methodCounters       = UnsafeUtils.unsafe.getLong(base + METHOD_COUNTERS_OFFSET);
        adapter              = UnsafeUtils.unsafe.getLong(base + ADAPTER_OFFSET);
        accessFlags          = UnsafeUtils.unsafe.getInt(base + ACCESS_FLAGS_OFFSET);
        vtableIndex          = UnsafeUtils.unsafe.getInt(base + VTABLE_INDEX_OFFSET);
        flags                = UnsafeUtils.unsafe.getInt(base + FLAGS_OFFSET);
        intrinsicId          = UnsafeUtils.unsafe.getShort(base + INTRINSIC_ID_OFFSET);
        i2iEntry             = UnsafeUtils.unsafe.getLong(base + I2I_ENTRY_OFFSET);
        fromCompiledEntry    = UnsafeUtils.unsafe.getLong(base + FROM_COMPILED_ENTRY_OFFSET);
        code                 = UnsafeUtils.unsafe.getLong(base + CODE_OFFSET);
        fromInterpretedEntry = UnsafeUtils.unsafe.getLong(base + FROM_INTERPRETED_ENTRY_OFFSET);
    }

    public void refresh() {
        readFields();
    }

    @Override
    public boolean isMethod() {
        return true;
    }

    public boolean isStatic() {
        return (accessFlags & AccessFlags.JVM_ACC_STATIC) != 0;
    }

    public boolean isNative() {
        return (accessFlags & AccessFlags.JVM_ACC_NATIVE) != 0;
    }

    public boolean isAbstract() {
        return (accessFlags & AccessFlags.JVM_ACC_ABSTRACT) != 0;
    }

    public boolean isCompiled() {
        return code != 0;
    }

    public ConstMethod constMethod() {
        return constMethod == 0 ? null : new ConstMethod(constMethod);
    }

    public String name() {
        if (constMethod == 0) return null;
        ConstMethod cm = new ConstMethod(constMethod);
        if (cm.constants == 0 || cm.nameIndex == 0) return null;
        ConstantPool cp = new ConstantPool(cm.constants);
        long symbolAddr = cp.symbolAt(cm.nameIndex & 0xFFFF);
        if (symbolAddr == 0) return null;
        return new Symbol(symbolAddr).value;
    }

    public String signature() {
        if (constMethod == 0) return null;
        ConstMethod cm = new ConstMethod(constMethod);
        if (cm.constants == 0 || cm.signatureIndex == 0) return null;
        ConstantPool cp = new ConstantPool(cm.constants);
        long symbolAddr = cp.symbolAt(cm.signatureIndex & 0xFFFF);
        if (symbolAddr == 0) return null;
        return new Symbol(symbolAddr).value;
    }

    public AccessFlags accessFlags() {
        return new AccessFlags(accessFlags);
    }

    /**
     * 让方法回到解释执行：清掉 {@code _code}，把入口指回解释器。
     *
     * <p>只改字节码（{@link ConstMethod#putCodeAndResize}）对已经 JIT 编译的方法无效，
     * 因为调用走的是 nmethod；先调用本方法再改字节码，后续调用才会执行新字节码。
     *
     * <p>做法与 jdk17 版本一致：{@code _from_compiled_entry} 用 adapter 的 c2i 入口
     * （编译代码调用进来时做帧转换），{@code _from_interpreted_entry} 指回 i2i 入口。
     */
    public void forceInterpreted() {
        long base = getAddress();
        if (base == 0) return;

        long i2iEntry = UnsafeUtils.unsafe.getLong(base + I2I_ENTRY_OFFSET);
        long adapter  = UnsafeUtils.unsafe.getLong(base + ADAPTER_OFFSET);

        long c2iEntry = i2iEntry;
        if (adapter != 0) {
            long candidate = UnsafeUtils.unsafe.getLong(adapter + 32L);
            if (candidate != 0) {
                c2iEntry = candidate;
            }
        }

        UnsafeUtils.unsafe.storeFence();
        UnsafeUtils.unsafe.putLong(base + CODE_OFFSET, 0L);
        UnsafeUtils.unsafe.putLong(base + FROM_COMPILED_ENTRY_OFFSET, c2iEntry);
        UnsafeUtils.unsafe.putLong(base + FROM_INTERPRETED_ENTRY_OFFSET, i2iEntry);
        UnsafeUtils.unsafe.storeFence();

        readFields();
    }

    // MethodFlags（methodFlags.hpp，JDK 21）
    private static final int FLAG_IS_NOT_C2_COMPILABLE     = 1 << 8;
    private static final int FLAG_IS_NOT_C1_COMPILABLE     = 1 << 9;
    private static final int FLAG_IS_NOT_C2_OSR_COMPILABLE = 1 << 10;
    private static final int FLAG_DONT_INLINE              = 1 << 12;

    /**
     * 禁止该方法再被 C1/C2 编译，并禁止内联。
     *
     * <p>替换过字节码的方法不能再交给编译器：新字节码里的常量池索引按解释器的
     * cache index 约定写入，编译器重新解析时可能访问到非法的 CP 条目而崩溃
     * （实测 C1 在编译调用方、内联该方法时会 EXCEPTION_ACCESS_VIOLATION）。
     */
    public void setNotCompilable() {
        long base = getAddress();
        if (base == 0) return;

        int flags = UnsafeUtils.unsafe.getInt(base + FLAGS_OFFSET);
        flags |= FLAG_DONT_INLINE
                | FLAG_IS_NOT_C1_COMPILABLE
                | FLAG_IS_NOT_C2_COMPILABLE
                | FLAG_IS_NOT_C2_OSR_COMPILABLE;
        UnsafeUtils.unsafe.putInt(base + FLAGS_OFFSET, flags);
        readFields();
    }

    public boolean isDontInline() {
        return (flags & FLAG_DONT_INLINE) != 0;
    }

    public boolean isNotCompilable() {
        return (flags & (FLAG_IS_NOT_C1_COMPILABLE | FLAG_IS_NOT_C2_COMPILABLE)) != 0;
    }

    public static long accessFlagsFieldOffset() { return ACCESS_FLAGS_OFFSET; }
    public static long intrinsicIdFieldOffset() { return INTRINSIC_ID_OFFSET; }
    public static long flagsFieldOffset()       { return FLAGS_OFFSET; }

    /**
     * native 方法的函数指针内嵌在 Method 结构之后（Method::native_function_addr()）。
     * 偏移 = align_up(sizeof(Method), wordSize)。
     */
    public static long nativeFunctionOffset() {
        long size = VMStructsHelper.findSize("Method");
        return (size + 7L) & ~7L;
    }

    public long nativeFunctionAddress() {
        return getAddress() + nativeFunctionOffset();
    }

    public long nativeFunction() {
        return UnsafeUtils.unsafe.getLong(nativeFunctionAddress());
    }

    public void setNativeFunction(long fn) {
        UnsafeUtils.unsafe.putLong(nativeFunctionAddress(), fn);
    }

    /**
     * 清除 setNotCompilable() 设置的「禁止编译/内联」标记，
     * 让方法重新可以交给 C1/C2 编译（类还原时使用）。
     */
    public void clearNotCompilable() {
        long base = getAddress();
        int f = UnsafeUtils.unsafe.getInt(base + FLAGS_OFFSET);
        f &= ~(FLAG_IS_NOT_C2_COMPILABLE | FLAG_IS_NOT_C1_COMPILABLE
                | FLAG_IS_NOT_C2_OSR_COMPILABLE | FLAG_DONT_INLINE);
        UnsafeUtils.unsafe.putInt(base + FLAGS_OFFSET, f);
        readFields();
    }

    @Override
    public String toString() {
        return String.format(
                "Method[address=0x%x, constMethod=0x%x, accessFlags=0x%x (%s), vtableIndex=%d, intrinsicId=%d, code=0x%x]",
                getAddress(), constMethod, accessFlags,
                accessFlags().toString(AccessFlags.Kind.METHOD),
                vtableIndex, intrinsicId, code);
    }
}
