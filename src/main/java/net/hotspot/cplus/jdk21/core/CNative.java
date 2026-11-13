package net.hotspot.cplus.jdk21.core;

import net.hotspot.cplus.jdk21.klass.Klass;

/**
 * 计算任意 C 层字段/符号的运行期地址。
 *
 * <p>四种手段：
 * <ol>
 *   <li>VMStructs 偏移：{@link #fieldOffset} / {@link #fieldAddress}；</li>
 *   <li>符号解析：{@link #symbol}（findNative 优先，失败再走 PE 导出表）；</li>
 *   <li>内存扫描：{@link #findFieldByValue} / {@link #findPointer}（已知值反查字段偏移）；</li>
 *   <li>指针链：{@link #addressOf} / {@link #deref}（沿 G1CollectedHeap::_g1h 这类链走）。</li>
 * </ol>
 */
public final class CNative {

    public static long symbol(String name) {
        long a = Shellcode.findNative(name);
        if (a != 0) return a;
        return Shellcode.symbol(name);
    }

    public static long fieldOffset(String typeAndField) {
        return VMStructsHelper.findOffset(typeAndField);
    }

    public static long fieldAddress(long base, String typeAndField) {
        return base + fieldOffset(typeAndField);
    }

    public static int fieldInt(long base, String typeAndField) {
        return UnsafeUtils.unsafe.getInt(fieldAddress(base, typeAndField));
    }

    public static long fieldLong(long base, String typeAndField) {
        return UnsafeUtils.unsafe.getLong(fieldAddress(base, typeAndField));
    }

    public static void setFieldInt(long base, String typeAndField, int value) {
        UnsafeUtils.unsafe.putInt(fieldAddress(base, typeAndField), value);
    }

    public static void setFieldLong(long base, String typeAndField, long value) {
        UnsafeUtils.unsafe.putLong(fieldAddress(base, typeAndField), value);
    }

    /** 在 [base, base+range) 里找第一个等于 value 的 qword，返回偏移；找不到返回 -1。 */
    public static int findFieldByValue(long base, long value, int range) {
        for (int off = 0; off + 8 <= range; off += 8) {
            if (UnsafeUtils.unsafe.getLong(base + off) == value) {
                return off;
            }
        }
        return -1;
    }

    /** 在 [start, end) 里按 qword 步进找指针 value，返回地址；找不到返回 0。 */
    public static long findPointer(long start, long end, long value) {
        for (long p = start; p + 8 <= end; p += 8) {
            if (UnsafeUtils.unsafe.getLong(p) == value) {
                return p;
            }
        }
        return 0;
    }

    /** 沿指针链取值：p = base；对每个 offset 执行 p = *(long*)(p + offset)。 */
    public static long deref(long base, int... offsets) {
        long p = base;
        for (int off : offsets) {
            p = UnsafeUtils.unsafe.getLong(p + off);
        }
        return p;
    }

    /** 沿指针链求地址：前 n-1 个 offset 解引用，最后一个 offset 只做加法。 */
    public static long addressOf(long base, int... offsets) {
        long p = base;
        for (int i = 0; i < offsets.length - 1; i++) {
            p = UnsafeUtils.unsafe.getLong(p + offsets[i]);
        }
        return p + offsets[offsets.length - 1];
    }

    /**
     * 扫描 JavaThread，找到 VMStructs 里没有的 {@code _jni_environment} 字段地址。
     * 判据：字段值指向 jvm.dll 里的 JNINativeInterface_ 表（前 4 项为 NULL，
     * 后面几项是 jvm.dll 内的 JNI 函数指针）。
     */
    public static long jniEnvironment(long thread) {
        long base = Shellcode.moduleBase(Shellcode.findNative("gHotSpotVMStructs"));
        if (base == 0) return 0;
        int pe = UnsafeUtils.unsafe.getInt(base + 0x3C);
        int size = UnsafeUtils.unsafe.getInt(base + pe + 24 + 56);
        for (int off = 0; off < 4096; off += 8) {
            long v = UnsafeUtils.unsafe.getLong(thread + off);
            if (v < base || v >= base + size) continue;
            if (UnsafeUtils.unsafe.getLong(v) != 0
                    || UnsafeUtils.unsafe.getLong(v + 8) != 0
                    || UnsafeUtils.unsafe.getLong(v + 16) != 0
                    || UnsafeUtils.unsafe.getLong(v + 24) != 0) continue;
            if (inJvm(base, size, UnsafeUtils.unsafe.getLong(v + 32))
                    && inJvm(base, size, UnsafeUtils.unsafe.getLong(v + 40))
                    && inJvm(base, size, UnsafeUtils.unsafe.getLong(v + 48))) {
                return thread + off;
            }
        }
        return 0;
    }

    private static boolean inJvm(long base, int size, long p) {
        return p > base && p < base + size;
    }

    private CNative() {
    }
}
