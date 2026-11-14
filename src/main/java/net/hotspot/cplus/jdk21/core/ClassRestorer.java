package net.hotspot.cplus.jdk21.core;

import net.hotspot.cplus.jdk21.klass.ClassFile;
import net.hotspot.cplus.jdk21.klass.ConstMethod;
import net.hotspot.cplus.jdk21.klass.InstanceKlass;
import net.hotspot.cplus.jdk21.klass.Method;

import java.io.ByteArrayOutputStream;

/**
 * 把 class 文件里的方法元数据写回运行期 ConstMethod：
 * 异常表、LineNumberTable（压缩格式）、LocalVariableTable、StackMapTable、注解、访问标志。
 */
final class ClassRestorer {

    private static final int HAS_LNT      = 0x0001;
    private static final int HAS_CHECKED  = 0x0002;
    private static final int HAS_LVT      = 0x0004;
    private static final int HAS_ET       = 0x0008;
    private static final int HAS_GENERIC  = 0x0010;
    private static final int HAS_PARAMS   = 0x0020;
    private static final int HAS_M_ANNO   = 0x0080;
    private static final int HAS_P_ANNO   = 0x0100;
    private static final int HAS_T_ANNO   = 0x0200;
    private static final int HAS_D_ANNO   = 0x0400;

    private static final long CM_HEADER = ConstMethod.CONST_METHOD_HEADER_SIZE;
    private static final long CM_FLAGS  = 28L;

    static void restoreMethod(InstanceKlass ik, Method m, ClassFile.MethodInfo mi) {
        ConstMethod cm = new ConstMethod(m.constMethod);
        long base = cm.getAddress();
        int words = cm.constMethodSize;
        long end = base + words * 8L;
        int flags = UnsafeUtils.unsafe.getInt(base + CM_FLAGS);

        int annCount = (flags & HAS_M_ANNO) != 0 ? 1 : 0;
        annCount += (flags & HAS_P_ANNO) != 0 ? 1 : 0;
        annCount += (flags & HAS_T_ANNO) != 0 ? 1 : 0;
        annCount += (flags & HAS_D_ANNO) != 0 ? 1 : 0;
        long tail = end - 8L * annCount;

        byte[] checked = null;
        byte[] params = null;
        int generic = 0;
        long p = tail;
        if ((flags & HAS_GENERIC) != 0) {
            generic = u2(p - 2);
            p -= 2;
        }
        if ((flags & HAS_PARAMS) != 0) {
            int len = u2(p - 2);
            int n = 2 + len * 4;
            params = bytes(p - n, n);
            p -= n;
        }
        if ((flags & HAS_CHECKED) != 0) {
            int len = u2(p - 2);
            int n = 2 + len * 2;
            checked = bytes(p - n, n);
            p -= n;
        }

        int[] et = mi.exceptionTable == null ? new int[0] : mi.exceptionTable;
        java.util.List<ClassFile.LocalVar> lvtList = mi.localVars;
        byte[] lnt = compressLineNumbers(mi.lineNumbers);
        int codeSize = mi.code == null ? 0 : mi.code.length;

        long need = CM_HEADER + codeSize + lnt.length;
        long etBytes = (et.length / 4) * 8L + 2L;
        long lvtBytes = lvtList.size() * 12L + 2L;
        need += et.length > 0 ? etBytes : 0;
        need += lvtList.isEmpty() ? 0 : lvtBytes;
        need += (checked == null ? 0 : checked.length);
        need += (params == null ? 0 : params.length);
        need += ((flags & HAS_GENERIC) != 0) ? 2 : 0;
        need += 8L * annCount;
        if (need > words * 8L) {
            throw new IllegalStateException("constMethod too small to restore metadata: need "
                    + need + " > " + (words * 8L));
        }

        // 访问标志：保留 JVM 内部高位，恢复 class 文件里的 u2
        long af = m.getAddress() + Method.accessFlagsFieldOffset();
        int acc = UnsafeUtils.unsafe.getInt(af);
        UnsafeUtils.unsafe.putInt(af, (acc & ~0xFFFF) | (mi.accessFlags & 0xFFFF));

        // LineNumberTable 紧跟在 code 后面（代码本身已由 putCodeAndResize 写入重写后的字节码）
        long lntAddr = base + CM_HEADER + codeSize;
        for (int i = 0; i < lnt.length; i++) {
            UnsafeUtils.unsafe.putByte(lntAddr + i, lnt[i]);
        }

        // 从尾部往前写 LVT / ET / checked / params / generic
        long q = tail;
        if ((flags & HAS_GENERIC) != 0) {
            q -= 2;
            putU2(q, generic);
        }
        if (params != null) {
            q -= params.length;
            putBytes(q, params);
        }
        if (checked != null) {
            q -= checked.length;
            putBytes(q, checked);
        }
        int etLen = et.length / 4;
        if (etLen > 0) {
            q -= etBytes;
            long e = q;
            for (int i = 0; i < etLen * 4; i++) {
                putU2(e, et[i]);
                e += 2;
            }
            putU2(e, etLen);
        }
        int lvtLen = lvtList.size();
        if (lvtLen > 0) {
            q -= lvtBytes;
            long e = q;
            for (ClassFile.LocalVar v : lvtList) {
                putU2(e, v.startPc);
                putU2(e + 2, v.length);
                putU2(e + 4, v.nameIndex);
                putU2(e + 6, v.descriptorIndex);
                putU2(e + 8, v.signatureIndex);
                putU2(e + 10, v.slot);
                e += 12;
            }
            putU2(e, lvtLen);
        }

        // flags：只改 LNT/LVT/ET/方法注解 这四个存在位
        int newFlags = flags & ~(HAS_LNT | HAS_LVT | HAS_ET | HAS_M_ANNO);
        if (mi.lineNumbers != null && mi.lineNumbers.length > 0) newFlags |= HAS_LNT;
        if (lvtLen > 0) newFlags |= HAS_LVT;
        if (etLen > 0) newFlags |= HAS_ET;
        if (mi.annotations != null && mi.annotations.length > 0) newFlags |= HAS_M_ANNO;
        UnsafeUtils.unsafe.putInt(base + CM_FLAGS, newFlags);

        // StackMapTable
        if (cm.stackmapData != 0) {
            int cap = UnsafeUtils.unsafe.getInt(cm.stackmapData);
            byte[] sm = mi.stackMap == null ? new byte[0] : mi.stackMap;
            if (sm.length > cap) {
                throw new IllegalStateException("stackmap too large: " + sm.length + " > " + cap);
            }
            UnsafeUtils.unsafe.putInt(cm.stackmapData, sm.length);
            for (int i = 0; i < sm.length; i++) {
                UnsafeUtils.unsafe.putByte(cm.stackmapData + 4 + i, sm[i]);
            }
        }

        // 方法注解（RuntimeVisibleAnnotations）：指针在 ConstMethod 末尾
        if ((newFlags & HAS_M_ANNO) != 0) {
            long arr = UnsafeUtils.unsafe.getLong(end - 8);
            byte[] an = mi.annotations;
            int cap = arr == 0 ? 0 : UnsafeUtils.unsafe.getInt(arr);
            if (arr == 0 || an.length > cap) {
                throw new IllegalStateException("annotation array too small: "
                        + (arr == 0 ? "null" : cap) + " < " + an.length);
            }
            UnsafeUtils.unsafe.putInt(arr, an.length);
            for (int i = 0; i < an.length; i++) {
                UnsafeUtils.unsafe.putByte(arr + 4 + i, an[i]);
            }
        }
    }

    static byte[] compressLineNumbers(int[] pairs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bci = 0;
        int line = 0;
        if (pairs != null) {
            for (int i = 0; i < pairs.length; i += 2) {
                int bd = pairs[i] - bci;
                int ld = pairs[i + 1] - line;
                bci = pairs[i];
                line = pairs[i + 1];
                if (bd >= 0 && bd < 32 && ld >= 0 && ld < 8) {
                    int v = (bd << 3) | ld;
                    if (v != 0 && v != 0xFF) {
                        out.write(v);
                        continue;
                    }
                }
                out.write(0xFF);
                writeSigned(out, bd);
                writeSigned(out, ld);
            }
        }
        out.write(0);
        return out.toByteArray();
    }

    private static void writeSigned(ByteArrayOutputStream out, int v) {
        writeInt(out, (v << 1) ^ (v >> 31));
    }

    /** HotSpot CompressedWriteStream: UNSIGNED5 (Pack200) 编码。 */
    private static void writeInt(ByteArrayOutputStream out, int value) {
        long sum = value & 0xFFFFFFFFL;
        for (int i = 0; ; i++) {
            if (sum < 192 || i == 4) {
                out.write((int) sum);
                return;
            }
            sum -= 192;
            int b = (int) (192 + (sum % 64));
            sum >>= 6;
            out.write(b);
        }
    }

    private static void putU2(long addr, int v) {
        UnsafeUtils.unsafe.putShort(addr, (short) v);
    }

    private static int u2(long addr) {
        return UnsafeUtils.unsafe.getShort(addr) & 0xFFFF;
    }

    private static byte[] bytes(long addr, int n) {
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            out[i] = UnsafeUtils.unsafe.getByte(addr + i);
        }
        return out;
    }

    private static void putBytes(long addr, byte[] b) {
        for (int i = 0; i < b.length; i++) {
            UnsafeUtils.unsafe.putByte(addr + i, b[i]);
        }
    }

    private ClassRestorer() {
    }
}
