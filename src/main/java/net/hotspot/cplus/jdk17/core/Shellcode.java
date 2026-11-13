package net.hotspot.cplus.jdk17.core;

import net.hotspot.cplus.jdk17.codecache.NMethod;
import net.hotspot.cplus.jdk17.klass.InstanceKlass;
import net.hotspot.cplus.jdk17.klass.Method;

/**
 * 用 shellcode 在 Java 层调用本地（C++）方法。
 *
 * <p>Java 侧的做法：
 * <ol>
 *   <li>把目标 C++ 函数的地址解析出来（走 jvm.dll 的 PE 导出表）；</li>
 *   <li>把一小段 x64 机器码写进一个已编译 Java 方法的 entry（code cache 可写）；</li>
 *   <li>从 Java 调用这个方法，入口就跳到 shellcode，shellcode 再 call 目标函数。</li>
 * </ol>
 */
public final class Shellcode {

    /** 解析 jvm.dll 导出符号，失败返回 0。 */
    public static long symbol(String name) {
        long anchor = findNative("gHotSpotVMStructs");
        if (anchor == 0) return 0;
        long base = moduleBase(anchor);
        if (base == 0) return 0;
        return export(base, name);
    }

    /** 从 jvm.dll 的导入表里解析某个 DLL 的函数地址（例如 kernel32!GetCurrentProcessId）。 */
    public static long imported(long moduleBase, String dllName, String funcName) {
        int pe = UnsafeUtils.unsafe.getInt(moduleBase + 0x3C);
        int opt = pe + 24;
        int magic = UnsafeUtils.unsafe.getShort(moduleBase + opt) & 0xFFFF;
        int dd = opt + (magic == 0x20B ? 112 : 96);
        int importRva = UnsafeUtils.unsafe.getInt(moduleBase + dd + 8);
        if (importRva == 0) return 0;

        long desc = moduleBase + importRva;
        for (int i = 0; i < 256; i++) {
            long d = desc + 20L * i;
            int nameRva = UnsafeUtils.unsafe.getInt(d + 12);
            int firstThunk = UnsafeUtils.unsafe.getInt(d + 16);
            int oft = UnsafeUtils.unsafe.getInt(d);
            if (nameRva == 0 && firstThunk == 0 && oft == 0) break;
            String dll = cString(moduleBase + nameRva);
            if (!dll.equalsIgnoreCase(dllName)) continue;

            long names = moduleBase + (oft != 0 ? oft : firstThunk);
            long iat = moduleBase + firstThunk;
            for (int k = 0; k < 4096; k++) {
                long entry = UnsafeUtils.unsafe.getLong(names + 8L * k);
                if (entry == 0) break;
                if ((entry >>> 63) != 0) continue;
                String fn = cString(moduleBase + (int) entry + 2);
                if (fn.equals(funcName)) {
                    return UnsafeUtils.unsafe.getLong(iat + 8L * k);
                }
            }
            return 0;
        }
        return 0;
    }

    public static long kernel32(String funcName) {
        long anchor = findNative("gHotSpotVMStructs");
        if (anchor == 0) return 0;
        long base = moduleBase(anchor);
        if (base == 0) return 0;
        return imported(base, "kernel32.dll", funcName);
    }

    /** 生成「call target，返回值留在 rax」的 x64 stub（无参）。 */
    public static byte[] call(long target) {
        return stub(target, new long[0]);
    }

    /** 带 1 个参数（rcx）的 stub。 */
    public static byte[] call(long target, long a0) {
        return stub(target, new long[]{a0});
    }

    /** 带 2 个参数（rcx, rdx）的 stub。 */
    public static byte[] call(long target, long a0, long a1) {
        return stub(target, new long[]{a0, a1});
    }

    /** 调用 JVM_* 入口：第一个参数是 JNIEnv*（rdi），第二个参数置空（rsi）。 */
    public static byte[] callJni(long target, long env) {
        byte[] b = new byte[37];
        int i = 0;
        b[i++] = 0x55;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xE5;
        b[i++] = 0x48;
        b[i++] = (byte) 0x83;
        b[i++] = (byte) 0xE4;
        b[i++] = (byte) 0xF0;
        b[i++] = 0x48;
        b[i++] = (byte) 0xBF;
        for (int j = 0; j < 8; j++) {
            b[i++] = (byte) (env >>> (8 * j));
        }
        b[i++] = 0x31;
        b[i++] = (byte) 0xF6;
        b[i++] = 0x48;
        b[i++] = (byte) 0xB8;
        for (int j = 0; j < 8; j++) {
            b[i++] = (byte) (target >>> (8 * j));
        }
        b[i++] = (byte) 0xFF;
        b[i++] = (byte) 0xD0;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xEC;
        b[i++] = 0x5D;
        b[i++] = (byte) 0xC3;
        return b;
    }

    /**
     * 调用 JVM_* 入口前把 JavaThread 状态切成 _thread_in_native(3)，
     * 调用后恢复 _thread_in_Java(7)，避免 ThreadInVMfromNative 把线程状态留在 native。
     */
    public static byte[] callJvm(long target, long thread, int stateOffset) {
        byte[] b = new byte[55];
        int i = 0;
        b[i++] = 0x55;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xE5;
        b[i++] = 0x48;
        b[i++] = (byte) 0x83;
        b[i++] = (byte) 0xE4;
        b[i++] = (byte) 0xF0;
        b[i++] = 0x48;
        b[i++] = (byte) 0xBF;
        for (int j = 0; j < 8; j++) {
            b[i++] = (byte) (thread >>> (8 * j));
        }
        b[i++] = (byte) 0xC7;
        b[i++] = (byte) 0x87;
        putInt(b, i, stateOffset);
        i += 4;
        putInt(b, i, 3);
        i += 4;
        b[i++] = 0x48;
        b[i++] = (byte) 0xB8;
        for (int j = 0; j < 8; j++) {
            b[i++] = (byte) (target >>> (8 * j));
        }
        b[i++] = (byte) 0xFF;
        b[i++] = (byte) 0xD0;
        b[i++] = (byte) 0xC7;
        b[i++] = (byte) 0x87;
        putInt(b, i, stateOffset);
        i += 4;
        putInt(b, i, 7);
        i += 4;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xEC;
        b[i++] = 0x5D;
        b[i++] = (byte) 0xC3;
        return b;
    }

    /** 反查：给定函数地址，返回它在 jvm.dll 导出表里的名字（锁定地址用）。 */
    public static String exportOf(long addr) {
        long base = moduleBase(findNative("gHotSpotVMStructs"));
        if (base == 0 || addr == 0) return null;
        int pe = UnsafeUtils.unsafe.getInt(base + 0x3C);
        int opt = pe + 24;
        int magic = UnsafeUtils.unsafe.getShort(base + opt) & 0xFFFF;
        int dd = opt + (magic == 0x20B ? 112 : 96);
        int exportRva = UnsafeUtils.unsafe.getInt(base + dd);
        if (exportRva == 0) return null;
        long dir = base + exportRva;
        int numNames = UnsafeUtils.unsafe.getInt(dir + 24);
        int addrNames = UnsafeUtils.unsafe.getInt(dir + 32);
        int addrOrds = UnsafeUtils.unsafe.getInt(dir + 36);
        int addrFuncs = UnsafeUtils.unsafe.getInt(dir + 28);
        for (int i = 0; i < numNames; i++) {
            int nameRva = UnsafeUtils.unsafe.getInt(base + addrNames + 4L * i);
            int ord = UnsafeUtils.unsafe.getShort(base + addrOrds + 2L * i) & 0xFFFF;
            int funcRva = UnsafeUtils.unsafe.getInt(base + addrFuncs + 4L * ord);
            if (base + funcRva == addr) {
                return cString(base + nameRva);
            }
        }
        return null;
    }

    /**
     * 构造一个「完整」的 native 帧再调用 JVM_*：
     * 1. 保存 JavaThread._anchor(sp/fp/pc) 和 _thread_state；
     * 2. 用调用方的编译帧（rbp/rbp+8）填 _anchor，让 GC 能正常遍历；
     * 3. 线程状态切成 _thread_in_native(3)，调用目标函数；
     * 4. 恢复 _anchor 和状态。
     */
    public static byte[] callJvmAnchored(long target, long thread, long scratch,
                                         int anchorOffset, int stateOffset) {
        Buf b = new Buf(320);
        // prologue: push rbp; mov rbp,rsp; push rbx; push r12; and rsp,-16
        b.b(0x55);
        b.b(0x48); b.b(0x89); b.b(0xE5);
        b.b(0x53);
        b.b(0x41); b.b(0x54);
        b.b(0x48); b.b(0x83); b.b(0xE4); b.b(0xF0);
        // rbx = thread, r12 = scratch
        b.b(0x48); b.b(0xBB); b.l(thread);
        b.b(0x49); b.b(0xBC); b.l(scratch);
        // save anchor sp/fp/pc
        b.b(0x48); b.b(0x8B); b.b(0x83); b.i(anchorOffset);          // mov rax,[rbx+off]
        b.b(0x49); b.b(0x89); b.b(0x84); b.b(0x24); b.i(0);          // mov [r12+0],rax
        b.b(0x48); b.b(0x8B); b.b(0x83); b.i(anchorOffset + 8);
        b.b(0x49); b.b(0x89); b.b(0x84); b.b(0x24); b.i(8);
        b.b(0x48); b.b(0x8B); b.b(0x83); b.i(anchorOffset + 16);
        b.b(0x49); b.b(0x89); b.b(0x84); b.b(0x24); b.i(16);
        // save state
        b.b(0x8B); b.b(0x83); b.i(stateOffset);                      // mov eax,[rbx+off]
        b.b(0x41); b.b(0x89); b.b(0x84); b.b(0x24); b.i(24);         // mov [r12+24],eax
        // new anchor: sp = rbp+8, fp = rbp, pc = [rbp+8]
        b.b(0x48); b.b(0x8D); b.b(0x45); b.b(0x08);                  // lea rax,[rbp+8]
        b.b(0x48); b.b(0x89); b.b(0x83); b.i(anchorOffset);          // mov [rbx+off],rax
        b.b(0x48); b.b(0x89); b.b(0xAB); b.i(anchorOffset + 8);      // mov [rbx+off+8],rbp
        b.b(0x48); b.b(0x8B); b.b(0x45); b.b(0x08);                  // mov rax,[rbp+8]
        b.b(0x48); b.b(0x89); b.b(0x83); b.i(anchorOffset + 16);
        // state = 3
        b.b(0xC7); b.b(0x83); b.i(stateOffset); b.i(3);
        // call target
        b.b(0x48); b.b(0xB8); b.l(target);
        b.b(0xFF); b.b(0xD0);
        // restore state
        b.b(0x41); b.b(0x8B); b.b(0x84); b.b(0x24); b.i(24);         // mov eax,[r12+24]
        b.b(0x89); b.b(0x83); b.i(stateOffset);                      // mov [rbx+off],eax
        // restore anchor
        b.b(0x49); b.b(0x8B); b.b(0x84); b.b(0x24); b.i(0);
        b.b(0x48); b.b(0x89); b.b(0x83); b.i(anchorOffset);
        b.b(0x49); b.b(0x8B); b.b(0x84); b.b(0x24); b.i(8);
        b.b(0x48); b.b(0x89); b.b(0x83); b.i(anchorOffset + 8);
        b.b(0x49); b.b(0x8B); b.b(0x84); b.b(0x24); b.i(16);
        b.b(0x48); b.b(0x89); b.b(0x83); b.i(anchorOffset + 16);
        // epilogue
        b.b(0x48); b.b(0x8D); b.b(0x65); b.b(0xF0);                  // lea rsp,[rbp-16]
        b.b(0x41); b.b(0x5C);                                        // pop r12
        b.b(0x5B);                                                   // pop rbx
        b.b(0x5D);                                                   // pop rbp
        b.b(0xC3);                                                   // ret
        return b.toBytes();
    }

    private static final class Buf {
        private final byte[] data;
        private int pos;

        Buf(int cap) {
            data = new byte[cap];
        }

        void b(int v) {
            data[pos++] = (byte) v;
        }

        void i(int v) {
            for (int k = 0; k < 4; k++) {
                data[pos++] = (byte) (v >>> (8 * k));
            }
        }

        void l(long v) {
            for (int k = 0; k < 8; k++) {
                data[pos++] = (byte) (v >>> (8 * k));
            }
        }

        byte[] toBytes() {
            byte[] out = new byte[pos];
            System.arraycopy(data, 0, out, 0, pos);
            return out;
        }
    }

    private static void putInt(byte[] b, int pos, int v) {
        b[pos] = (byte) v;
        b[pos + 1] = (byte) (v >>> 8);
        b[pos + 2] = (byte) (v >>> 16);
        b[pos + 3] = (byte) (v >>> 24);
    }

    private static void putLong(byte[] b, int pos, long v) {
        for (int j = 0; j < 8; j++) {
            b[pos + j] = (byte) (v >>> (8 * j));
        }
    }

    /**
     * 带 JNIEnv 并切换线程状态的 JVM_* 调用：
     * rdi = env，调用前把 JavaThread 状态置为 _thread_in_native(3)，返回后恢复 _thread_in_Java(7)。
     */
    public static byte[] callJvmEnv(long target, long env, long thread, int stateOffset) {
        byte[] b = new byte[79];
        int i = 0;
        b[i++] = 0x55;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xE5;
        b[i++] = 0x48;
        b[i++] = (byte) 0x83;
        b[i++] = (byte) 0xE4;
        b[i++] = (byte) 0xF0;
        b[i++] = 0x49;
        b[i++] = (byte) 0xB8;
        putLong(b, i, thread);
        i += 8;
        b[i++] = 0x41;
        b[i++] = (byte) 0xC7;
        b[i++] = (byte) 0x80;
        putInt(b, i, stateOffset);
        i += 4;
        putInt(b, i, 3);
        i += 4;
        b[i++] = 0x48;
        b[i++] = (byte) 0xBF;
        putLong(b, i, env);
        i += 8;
        b[i++] = 0x31;
        b[i++] = (byte) 0xF6;
        b[i++] = 0x48;
        b[i++] = (byte) 0xB8;
        putLong(b, i, target);
        i += 8;
        b[i++] = (byte) 0xFF;
        b[i++] = (byte) 0xD0;
        b[i++] = 0x49;
        b[i++] = (byte) 0xB8;
        putLong(b, i, thread);
        i += 8;
        b[i++] = 0x41;
        b[i++] = (byte) 0xC7;
        b[i++] = (byte) 0x80;
        putInt(b, i, stateOffset);
        i += 4;
        putInt(b, i, 7);
        i += 4;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xEC;
        b[i++] = 0x5D;
        b[i++] = (byte) 0xC3;
        return b;
    }

    /**
     * 手动扫描 JavaThread 对象，定位 JNIEnv*（&_jni_environment）：
     * 该字段指向 jvm.dll 里的 JNINativeInterface_ 函数表。
     */
    public static long jniEnv(long thread) {
        long base = moduleBase(findNative("gHotSpotVMStructs"));
        if (base == 0) return 0;
        int pe = UnsafeUtils.unsafe.getInt(base + 0x3C);
        int opt = pe + 24;
        int sizeOfImage = UnsafeUtils.unsafe.getInt(base + opt + 56);
        for (int off = 0; off < 4096; off += 8) {
            long v = UnsafeUtils.unsafe.getLong(thread + off);
            if (v < base || v >= base + sizeOfImage) continue;
            if (UnsafeUtils.unsafe.getLong(v) != 0
                    || UnsafeUtils.unsafe.getLong(v + 8) != 0
                    || UnsafeUtils.unsafe.getLong(v + 16) != 0
                    || UnsafeUtils.unsafe.getLong(v + 24) != 0) continue;
            long f1 = UnsafeUtils.unsafe.getLong(v + 32);
            long f2 = UnsafeUtils.unsafe.getLong(v + 40);
            long f3 = UnsafeUtils.unsafe.getLong(v + 48);
            if (f1 > base && f1 < base + sizeOfImage
                    && f2 > base && f2 < base + sizeOfImage
                    && f3 > base && f3 < base + sizeOfImage) {
                return thread + off;
            }
        }
        return 0;
    }

    /**
     * 劫持一个 native 方法的 native wrapper：把 wrapper 里 call native_func 的
     * rel32 目标改成 shellcode。这样调用该方法时，JVM 会自己建立真正的 native 帧
     * （anchor + _thread_in_native），shellcode 的 rdi 就是合法的 JNIEnv*，
     * 可以直接调 JVM_* 入口。
     *
     * @return 被改写的 call 指令地址；失败返回 0
     */
    public static long hookNativeWrapper(Class<?> holder, String name, String sig, long shellcode) {
        InstanceKlass ik = new InstanceKlass(holder);
        net.hotspot.cplus.jdk17.klass.Method m = null;
        for (net.hotspot.cplus.jdk17.klass.Method cand : ik.methodsArray().toMethods()) {
            if (name.equals(cand.name()) && sig.equals(cand.signature())) {
                m = cand;
                break;
            }
        }
        if (m == null || m.code == 0) return 0;
        NMethod nm = new NMethod(m.code);
        long base = moduleBase(findNative("gHotSpotVMStructs"));
        if (base == 0) return 0;
        int pe = UnsafeUtils.unsafe.getInt(base + 0x3C);
        int sizeOfImage = UnsafeUtils.unsafe.getInt(base + pe + 24 + 56);

        long entry = nm.entryPoint;
        for (long p = entry; p < entry + 512; p++) {
            if ((UnsafeUtils.unsafe.getByte(p) & 0xFF) != 0xE8) continue;
            int rel = UnsafeUtils.unsafe.getInt(p + 1);
            long target = p + 5 + rel;
            if (target <= base || target >= base + sizeOfImage) continue;
            long newRel = shellcode - (p + 5);
            if (newRel != (int) newRel) return 0;
            UnsafeUtils.unsafe.putInt(p + 1, (int) newRel);
            return p;
        }
        return 0;
    }

    private static byte[] stub(long target, long[] args) {
        byte[] b = new byte[25 + 10 * args.length];
        int i = 0;
        b[i++] = 0x55;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xE5;
        b[i++] = 0x48;
        b[i++] = (byte) 0x83;
        b[i++] = (byte) 0xE4;
        b[i++] = (byte) 0xF0;
        int[] regs = {0xB9, 0xBA};
        for (int k = 0; k < args.length; k++) {
            b[i++] = 0x48;
            b[i++] = (byte) regs[k];
            for (int j = 0; j < 8; j++) {
                b[i++] = (byte) (args[k] >>> (8 * j));
            }
        }
        b[i++] = 0x48;
        b[i++] = (byte) 0xB8;
        for (int j = 0; j < 8; j++) {
            b[i++] = (byte) (target >>> (8 * j));
        }
        b[i++] = (byte) 0xFF;
        b[i++] = (byte) 0xD0;
        b[i++] = 0x48;
        b[i++] = (byte) 0x89;
        b[i++] = (byte) 0xEC;
        b[i++] = 0x5D;
        b[i++] = (byte) 0xC3;
        return b;
    }

    /** 把机器码写到已编译方法的入口。 */
    public static void write(Method m, byte[] code) {
        if (m.code == 0) {
            throw new IllegalStateException("method is not compiled: " + m);
        }
        NMethod nm = new NMethod(m.code);
        long verified = nm.verifiedEntryPoint;
        long entry = nm.entryPoint;
        if (verified != 0) writeAt(verified, code);
        if (entry != 0 && entry != verified) writeAt(entry, code);
    }

    private static void writeAt(long addr, byte[] code) {
        for (int i = 0; i < code.length; i++) {
            UnsafeUtils.unsafe.putByte(addr + i, code[i]);
        }
    }

    public static long findNative(String name) {
        try {
            UnsafeUtils.registerJvmDll();
            java.lang.reflect.Method fn = ClassLoader.class.getDeclaredMethod(
                    "findNative", ClassLoader.class, String.class);
            UnsafeUtils.setAccessible(fn, true);
            Object r = fn.invoke(null, Shellcode.class.getClassLoader(), name);
            return r instanceof Long ? (Long) r : 0L;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static long moduleBase(long address) {
        long p = address & ~0xFFFL;
        for (long d = 0; d < (1L << 29); d += 0x1000) {
            long cand = p - d;
            if (UnsafeUtils.unsafe.getShort(cand) == 0x5A4D) {
                int pe = UnsafeUtils.unsafe.getInt(cand + 0x3C);
                if (pe > 0 && pe < 0x1000 && UnsafeUtils.unsafe.getInt(cand + pe) == 0x00004550) {
                    return cand;
                }
            }
        }
        return 0;
    }

    public static long export(long base, String name) {
        int pe = UnsafeUtils.unsafe.getInt(base + 0x3C);
        int opt = pe + 24;
        int magic = UnsafeUtils.unsafe.getShort(base + opt) & 0xFFFF;
        int dd = opt + (magic == 0x20B ? 112 : 96);
        int exportRva = UnsafeUtils.unsafe.getInt(base + dd);
        if (exportRva == 0) return 0;

        long dir = base + exportRva;
        int numNames = UnsafeUtils.unsafe.getInt(dir + 24);
        int addrNames = UnsafeUtils.unsafe.getInt(dir + 32);
        int addrOrds = UnsafeUtils.unsafe.getInt(dir + 36);
        int addrFuncs = UnsafeUtils.unsafe.getInt(dir + 28);

        for (int i = 0; i < numNames; i++) {
            int nameRva = UnsafeUtils.unsafe.getInt(base + addrNames + 4L * i);
            String n = cString(base + nameRva);
            if (!name.equals(n)) continue;
            int ord = UnsafeUtils.unsafe.getShort(base + addrOrds + 2L * i) & 0xFFFF;
            int funcRva = UnsafeUtils.unsafe.getInt(base + addrFuncs + 4L * ord);
            return base + funcRva;
        }
        return 0;
    }

    private static String cString(long addr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            int c = UnsafeUtils.unsafe.getByte(addr + i) & 0xFF;
            if (c == 0) break;
            sb.append((char) c);
        }
        return sb.toString();
    }

    private Shellcode() {
    }
}
