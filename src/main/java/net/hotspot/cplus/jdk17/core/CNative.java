package net.hotspot.cplus.jdk17.core;

import net.hotspot.cplus.jdk17.klass.Klass;

/**
 * 计算任意 C 层字段/符号的运行期地址。
 *
 * <p>静态字段地址：{@link #symbol}（findNative → PE 导出 → VMStructs 静态字段）。
 * <p>实例字段偏移：{@link #instanceFieldOffset}（VMStructs）。
 * <p>找不到时：{@link #scanValue}（按特征值扫 jvm.dll 数据区）、
 * {@link #findRipReferences}（反查引用该地址的指令）、{@link #findFieldByValue}（已知值反查字段偏移）。
 */
public final class CNative {

    /** 名字 → 地址：导出符号 / 静态字段；找不到返回 0。 */
    public static long symbol(String name) {
        long a = Shellcode.findNative(name);
        if (a != 0) return a;
        a = Shellcode.symbol(name);
        if (a != 0) return a;
        try {
            long s = VMStructsHelper.findStaticAddress(name);
            return s > 0 ? s : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 实例字段偏移。 */
    public static long instanceFieldOffset(String typeAndField) {
        return VMStructsHelper.findOffset(typeAndField);
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

    /** 在 jvm.dll 镜像里按 qword 步进找 value（特征值扫描），返回前 limit 个地址。 */
    public static long[] scanValue(long value, int limit) {
        long base = moduleBase();
        if (base == 0) return new long[0];
        int size = sizeOfImage(base);
        return scanValueIn(base, base + size, value, limit);
    }

    public static long[] scanValueIn(long start, long end, long value, int limit) {
        long[] out = new long[Math.max(1, limit)];
        int n = 0;
        for (long p = align(start); p + 8 <= end; p += 8) {
            if (UnsafeUtils.unsafe.getLong(p) == value) {
                out[n++] = p;
                if (n == out.length) break;
            }
        }
        long[] r = new long[n];
        System.arraycopy(out, 0, r, 0, n);
        return r;
    }

    /**
     * 反查调用链：扫描 jvm.dll 的代码，找 RIP 相对寻址到 target 的指令，返回指令起始地址。
     * 覆盖常见编码：mov/movzx/movsx [rip+disp]、mov [rip+disp], reg 等。
     */
    public static long[] findRipReferences(long target, int limit) {
        long base = moduleBase();
        if (base == 0 || target == 0) return new long[0];
        int size = sizeOfImage(base);
        long[] out = new long[Math.max(1, limit)];
        int n = 0;
        for (long p = base; p < base + size - 8; p++) {
            int b = UnsafeUtils.unsafe.getByte(p) & 0xFF;
            if (b == 0x8B || b == 0x89 || b == 0x8A || b == 0x88) {
                if ((UnsafeUtils.unsafe.getByte(p + 1) & 0xC7) != 0x05) continue;
                long insn = p;
                if (isRex(p - 1)) insn = p - 1;
                int disp = UnsafeUtils.unsafe.getInt(p + 2);
                if (insn + (insn == p ? 6 : 7) + disp == target) {
                    out[n++] = insn;
                    if (n == out.length) break;
                }
            } else if (b == 0x0F) {
                int op2 = UnsafeUtils.unsafe.getByte(p + 1) & 0xFF;
                if (op2 != 0xB6 && op2 != 0xB7 && op2 != 0xBE && op2 != 0xBF) continue;
                if ((UnsafeUtils.unsafe.getByte(p + 2) & 0xC7) != 0x05) continue;
                long insn = p;
                if (isRex(p - 1)) insn = p - 1;
                int disp = UnsafeUtils.unsafe.getInt(p + 3);
                if (insn + (insn == p ? 7 : 8) + disp == target) {
                    out[n++] = insn;
                    if (n == out.length) break;
                }
            }
        }
        long[] r = new long[n];
        System.arraycopy(out, 0, r, 0, n);
        return r;
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
        Resolution r = resolveJniEnvironment(thread);
        return r.kind == Kind.PROBE_HIT ? r.address : 0;
    }

    public enum Kind {
        EXPORT, STATIC_FIELD, INSTANCE_FIELD, SCAN_HIT, PROBE_HIT, NOT_FOUND
    }

    /** 扫描候选字段时的特征判据：addr 是字段地址，value 是它当前的 qword 值。 */
    public interface FieldProbe {
        boolean test(long addr, long value);
    }

    /** 一次解析的结果：地址 / 偏移 / 证据（反查到的引用指令、扫描命中点）。 */
    public static final class Resolution {
        public final String name;
        public final Kind kind;
        public final long address;
        public final long offset;
        public final long[] evidence;

        Resolution(String name, Kind kind, long address, long offset, long[] evidence) {
            this.name = name;
            this.kind = kind;
            this.address = address;
            this.offset = offset;
            this.evidence = evidence;
        }

        /** 实例字段在给定对象上的地址；静态/导出直接返回 address。 */
        public long addressOn(long base) {
            return kind == Kind.INSTANCE_FIELD ? base + offset : address;
        }

        @Override
        public String toString() {
            return "Resolution[" + name + ", " + kind
                    + (kind == Kind.INSTANCE_FIELD ? ", offset=" + offset
                    : ", address=0x" + Long.toHexString(address))
                    + ", evidence=" + evidence.length + "]";
        }
    }

    /** 只按名字找：导出符号 → VMStructs 静态字段 → VMStructs 实例字段偏移。 */
    public static Resolution resolve(String name) {
        return resolve(name, 0, 0, 0);
    }

    /**
     * 用**特征判据**在 host 对象/区间里扫字段：适用于 VMStructs 没收录、
     * 也没有导出符号的字段（例如 JavaThread::_jni_environment）。
     * 对命中的地址还会反查引用它的指令作为证据。
     */
    public static Resolution resolveByProbe(String name, long host, int range,
                                            int step, FieldProbe probe) {
        for (int off = 0; off + 8 <= range; off += step) {
            long addr = host + off;
            long value = UnsafeUtils.unsafe.getLong(addr);
            if (probe.test(addr, value)) {
                return new Resolution(name, Kind.PROBE_HIT, addr, off,
                        findRipReferences(addr, 8));
            }
        }
        return new Resolution(name, Kind.NOT_FOUND, 0, -1, new long[0]);
    }

    /** JavaThread::_jni_environment 的判据：字段值指向 jvm.dll 里的 JNINativeInterface_ 表。 */
    public static Resolution resolveJniEnvironment(long thread) {
        return resolveByProbe("JavaThread::_jni_environment", thread, 4096, 8,
                (addr, value) -> {
                    long base = moduleBase();
                    if (base == 0 || value < base) return false;
                    int size = sizeOfImage(base);
                    if (value >= base + size) return false;
                    if (UnsafeUtils.unsafe.getLong(value) != 0
                            || UnsafeUtils.unsafe.getLong(value + 8) != 0
                            || UnsafeUtils.unsafe.getLong(value + 16) != 0
                            || UnsafeUtils.unsafe.getLong(value + 24) != 0) return false;
                    return inJvm(base, size, UnsafeUtils.unsafe.getLong(value + 32))
                            && inJvm(base, size, UnsafeUtils.unsafe.getLong(value + 40));
                });
    }

    /**
     * 综合解析：导出符号 → 静态字段 → 实例字段偏移 → 特征值扫描 jvm.dll/指定区间，
     * 并对每个扫描候选反查引用它的指令（调用链），取证据最多者。
     *
     * @param value 已知特征值（为 0 时不扫描）
     * @param base  扫描区间起点（为 0 时扫整个 jvm.dll 镜像）
     * @param range 扫描区间长度
     */
    public static Resolution resolve(String name, long value, long base, long range) {
        long a = Shellcode.findNative(name);
        if (a != 0) return new Resolution(name, Kind.EXPORT, a, -1, new long[0]);
        a = Shellcode.symbol(name);
        if (a != 0) return new Resolution(name, Kind.EXPORT, a, -1, new long[0]);

        try {
            long s = VMStructsHelper.findStaticAddress(name);
            if (s > 0) {
                return new Resolution(name, Kind.STATIC_FIELD, s, -1,
                        findRipReferences(s, 8));
            }
        } catch (Throwable ignored) {
            // 不是静态字段，继续
        }
        try {
            long off = VMStructsHelper.findOffset(name);
            if (off >= 0) {
                return new Resolution(name, Kind.INSTANCE_FIELD, -1, off, new long[0]);
            }
        } catch (Throwable ignored) {
            // 不是实例字段，继续
        }

        long[] hits = new long[0];
        if (value != 0) {
            hits = range > 0 ? scanValueIn(base, base + range, value, 16)
                    : scanValue(value, 16);
        }
        long best = 0;
        long[] bestRefs = new long[0];
        for (long h : hits) {
            long[] refs = findRipReferences(h, 8);
            if (refs.length > bestRefs.length) {
                best = h;
                bestRefs = refs;
            }
        }
        if (best == 0 && hits.length > 0) {
            best = hits[0];
        }
        if (best != 0) {
            return new Resolution(name, Kind.SCAN_HIT, best, -1, bestRefs);
        }
        return new Resolution(name, Kind.NOT_FOUND, 0, -1, new long[0]);
    }

    public static long moduleBase() {
        return Shellcode.moduleBase(Shellcode.findNative("gHotSpotVMStructs"));
    }

    public static int sizeOfImage(long base) {
        int pe = UnsafeUtils.unsafe.getInt(base + 0x3C);
        return UnsafeUtils.unsafe.getInt(base + pe + 24 + 56);
    }

    private static boolean isRex(long addr) {
        int b = UnsafeUtils.unsafe.getByte(addr) & 0xFF;
        return b >= 0x40 && b <= 0x4F;
    }

    private static long align(long v) {
        return (v + 7) & ~7L;
    }

    private static boolean inJvm(long base, int size, long p) {
        return p > base && p < base + size;
    }

    private CNative() {
    }
}
