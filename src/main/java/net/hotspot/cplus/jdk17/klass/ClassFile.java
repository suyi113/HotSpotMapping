package net.hotspot.cplus.jdk17.klass;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ClassFile {

    public static final class MethodInfo {
        public final String name;
        public final String descriptor;
        public final int accessFlags;
        public final int maxStack;
        public final int maxLocals;
        public final byte[] code;

        MethodInfo(String name, String descriptor, int accessFlags,
                   int maxStack, int maxLocals, byte[] code) {
            this.name = name;
            this.descriptor = descriptor;
            this.accessFlags = accessFlags;
            this.maxStack = maxStack;
            this.maxLocals = maxLocals;
            this.code = code;
        }
    }

    public final int constantPoolCount;
    public final int accessFlags;
    public final List<MethodInfo> methods;

    private ClassFile(int constantPoolCount, int accessFlags, List<MethodInfo> methods) {
        this.constantPoolCount = constantPoolCount;
        this.accessFlags = accessFlags;
        this.methods = methods;
    }

    public MethodInfo find(String name, String descriptor) {
        for (MethodInfo m : methods) {
            if (m.name.equals(name) && m.descriptor.equals(descriptor)) {
                return m;
            }
        }
        return null;
    }

    public static ClassFile parse(byte[] data) {
        Reader r = new Reader(data);
        if (r.u4() != 0xCAFEBABE) {
            throw new IllegalArgumentException("not a class file");
        }
        r.u2();
        r.u2();

        int cpCount = r.u2();
        String[] utf8 = new String[cpCount];
        for (int i = 1; i < cpCount; i++) {
            int tag = r.u1();
            switch (tag) {
                case 1:
                    utf8[i] = r.utf8();
                    break;
                case 3:
                case 4:
                    r.skip(4);
                    break;
                case 5:
                case 6:
                    r.skip(8);
                    i++;
                    break;
                case 7:
                case 8:
                case 16:
                case 19:
                case 20:
                    r.skip(2);
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    r.skip(4);
                    break;
                case 15:
                    r.skip(3);
                    break;
                default:
                    throw new IllegalArgumentException("bad constant pool tag " + tag);
            }
        }

        int access = r.u2();
        r.u2();
        r.u2();
        int interfaces = r.u2();
        r.skip(2 * interfaces);

        int fields = r.u2();
        for (int i = 0; i < fields; i++) {
            r.skip(6);
            skipAttributes(r, utf8);
        }

        int methodCount = r.u2();
        List<MethodInfo> list = new ArrayList<>();
        for (int i = 0; i < methodCount; i++) {
            int flags = r.u2();
            String name = utf8[r.u2()];
            String desc = utf8[r.u2()];
            int attrCount = r.u2();
            int maxStack = 0;
            int maxLocals = 0;
            byte[] code = null;
            for (int a = 0; a < attrCount; a++) {
                String an = utf8[r.u2()];
                int len = r.u4();
                int end = r.pos + len;
                if ("Code".equals(an)) {
                    maxStack = r.u2();
                    maxLocals = r.u2();
                    int codeLen = r.u4();
                    code = r.bytes(codeLen);
                }
                r.pos = end;
            }
            list.add(new MethodInfo(name, desc, flags, maxStack, maxLocals, code));
        }
        return new ClassFile(cpCount, access, list);
    }

    private static void skipAttributes(Reader r, String[] utf8) {
        int count = r.u2();
        for (int i = 0; i < count; i++) {
            r.u2();
            int len = r.u4();
            r.skip(len);
        }
    }

    private static final class Reader {
        private final byte[] data;
        private int pos;

        Reader(byte[] data) {
            this.data = data;
        }

        int u1() {
            return data[pos++] & 0xFF;
        }

        int u2() {
            int v = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            return v;
        }

        int u4() {
            int v = ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
                    | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
            pos += 4;
            return v;
        }

        void skip(int n) {
            pos += n;
        }

        byte[] bytes(int n) {
            byte[] out = new byte[n];
            System.arraycopy(data, pos, out, 0, n);
            pos += n;
            return out;
        }

        String utf8() {
            int len = u2();
            String s = new String(data, pos, len, StandardCharsets.UTF_8);
            pos += len;
            return s;
        }
    }

    private ClassFile() {
        throw new AssertionError();
    }
}
