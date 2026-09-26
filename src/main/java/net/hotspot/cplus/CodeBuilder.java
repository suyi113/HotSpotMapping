
package net.hotspot.cplus;



import net.hotspot.cplus.Utils.Opcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeBuilder {

    public static final int T_BOOLEAN = Opcode.T_BOOLEAN;
    public static final int T_CHAR    = Opcode.T_CHAR;
    public static final int T_FLOAT   = Opcode.T_FLOAT;
    public static final int T_DOUBLE  = Opcode.T_DOUBLE;
    public static final int T_BYTE    = Opcode.T_BYTE;
    public static final int T_SHORT   = Opcode.T_SHORT;
    public static final int T_INT     = Opcode.T_INT;
    public static final int T_LONG    = Opcode.T_LONG;

    private byte[] buffer = new byte[256];
    private int length = 0;

    private final Map<Integer, Integer> labelPositions = new HashMap<>();
    private final List<JumpFixup> fixups = new ArrayList<>();
    private int nextLabelId = 0;

    private static class JumpFixup {
        final int opcodePos;
        final int operandPos;
        final int labelId;
        final boolean wide;
        JumpFixup(int opcodePos, int operandPos, int labelId, boolean wide) {
            this.opcodePos  = opcodePos;
            this.operandPos = operandPos;
            this.labelId    = labelId;
            this.wide       = wide;
        }
    }

    public static class Label {
        final int id;
        Label(int id) { this.id = id; }
    }

    private void ensure(int extra) {
        if (length + extra > buffer.length) {
            int newCap = Math.max(buffer.length * 2, length + extra);
            buffer = Arrays.copyOf(buffer, newCap);
        }
    }

    private void emit(int b) {
        ensure(1);
        buffer[length++] = (byte) (b & 0xFF);
    }

    private void emit2(int v) {
        ensure(2);
        buffer[length++] = (byte) ((v >>> 8) & 0xFF);
        buffer[length++] = (byte) (v & 0xFF);
    }

    private void emit2LE(int v) {
        ensure(2);
        buffer[length++] = (byte) (v & 0xFF);
        buffer[length++] = (byte) ((v >>> 8) & 0xFF);
    }

    private void emit4(int v) {
        ensure(4);
        buffer[length++] = (byte) ((v >>> 24) & 0xFF);
        buffer[length++] = (byte) ((v >>> 16) & 0xFF);
        buffer[length++] = (byte) ((v >>> 8) & 0xFF);
        buffer[length++] = (byte) (v & 0xFF);
    }

    private void patch2(int pos, int v) {
        buffer[pos]     = (byte) ((v >>> 8) & 0xFF);
        buffer[pos + 1] = (byte) (v & 0xFF);
    }

    private void patch2LE(int pos, int v) {
        buffer[pos]     = (byte) (v & 0xFF);
        buffer[pos + 1] = (byte) ((v >>> 8) & 0xFF);
    }

    private void patch4(int pos, int v) {
        buffer[pos]     = (byte) ((v >>> 24) & 0xFF);
        buffer[pos + 1] = (byte) ((v >>> 16) & 0xFF);
        buffer[pos + 2] = (byte) ((v >>> 8) & 0xFF);
        buffer[pos + 3] = (byte) (v & 0xFF);
    }

    public Label newLabel() {
        return new Label(nextLabelId++);
    }

    public CodeBuilder mark(Label label) {
        labelPositions.put(label.id, length);
        return this;
    }

    private void resolveJump(int opcodePos, int operandPos, Label label, boolean wide) {
        Integer target = labelPositions.get(label.id);
        if (target != null) {
            int offset = target - opcodePos;
            if (wide) patch4(operandPos, offset);
            else      patch2(operandPos, offset);
        } else {
            fixups.add(new JumpFixup(opcodePos, operandPos, label.id, wide));
        }
    }

    private CodeBuilder emitJump1(int opcode, Label L) {
        int opcodePos = length;
        emit(opcode);
        int operandPos = length;
        emit2(0);
        resolveJump(opcodePos, operandPos, L, false);
        return this;
    }

    public CodeBuilder raw(byte[] bytes) {
        ensure(bytes.length);
        System.arraycopy(bytes, 0, buffer, length, bytes.length);
        length += bytes.length;
        return this;
    }

    public CodeBuilder raw(int opcode) {
        emit(opcode);
        return this;
    }

    public CodeBuilder opcode(int op) {
        emit(op);
        return this;
    }

    public int size() {
        return length;
    }

    public byte[] build() {
        for (JumpFixup f : fixups) {
            Integer target = labelPositions.get(f.labelId);
            if (target == null) {
                throw new IllegalStateException("Label " + f.labelId + " Unmarked");
            }
            int offset = target - f.opcodePos;
            if (f.wide) patch4(f.operandPos, offset);
            else        patch2(f.operandPos, offset);
        }
        return Arrays.copyOf(buffer, length);
    }

    public byte[] build(InstanceKlass ik) {
        return build();
    }


    public int utf8(InstanceKlass ik, String s) {
        return ik.addUtf8(s);
    }

    public int classref(InstanceKlass ik, String className) {
        return ik.addClass(className);
    }

    public int classref(InstanceKlass ik, Class<?> c) {
        return ik.addClass(c.getName().replace('.', '/'), c);
    }

    public int fieldref(InstanceKlass ik, String owner, String name, String desc) {
        return ik.addFieldref(owner, name, desc);
    }

    public int fieldref(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return ik.addFieldref(owner.getName().replace('.', '/'), name, desc, owner);
    }

    public int methodref(InstanceKlass ik, String owner, String name, String desc) {
        return ik.addMethodref(owner, name, desc);
    }

    public int methodref(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return ik.addMethodref(owner.getName().replace('.', '/'), name, desc, owner);
    }

    public int interfaceMethodref(InstanceKlass ik, String owner, String name, String desc) {
        return ik.addInterfaceMethodref(owner, name, desc);
    }

    public int interfaceMethodref(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return ik.addInterfaceMethodref(owner.getName().replace('.', '/'), name, desc, owner);
    }

    public int nameAndType(InstanceKlass ik, String name, String desc) {
        return ik.addNameAndType(name, desc);
    }


    public CodeBuilder nop()          { emit(Opcode.NOP); return this; }
    public CodeBuilder aconst_null()  { emit(Opcode.ACONST_NULL); return this; }

    public CodeBuilder iconst(int n) {
        if (n < -1 || n > 5) throw new IllegalArgumentException("iconst out of range [-1,5]: " + n);
        emit(Opcode.ICONST_0 + n);
        return this;
    }

    public CodeBuilder lconst(int n) {
        if (n < 0 || n > 1) throw new IllegalArgumentException("lconst out of range [0,1]: " + n);
        emit(Opcode.LCONST_0 + n);
        return this;
    }

    public CodeBuilder fconst(int n) {
        if (n < 0 || n > 2) throw new IllegalArgumentException("fconst out of range [0,2]: " + n);
        emit(Opcode.FCONST_0 + n);
        return this;
    }

    public CodeBuilder dconst(int n) {
        if (n < 0 || n > 1) throw new IllegalArgumentException("dconst out of range [0,1]: " + n);
        emit(Opcode.DCONST_0 + n);
        return this;
    }

    public CodeBuilder bipush(int b) {
        emit(Opcode.BIPUSH);
        emit(b);
        return this;
    }

    public CodeBuilder sipush(int s) {
        emit(Opcode.SIPUSH);
        emit2(s);
        return this;
    }

    public CodeBuilder ldc(int index) {
        if (index >= 0 && index <= 255) {
            emit(Opcode.LDC);
            emit(index);
        } else {
            emit(Opcode.LDC_W);
            emit2LE(index);
        }
        return this;
    }

    public CodeBuilder ldc_w(int index) {
        emit(Opcode.LDC_W);
        emit2LE(index);
        return this;
    }

    public CodeBuilder ldc2_w(int index) {
        emit(Opcode.LDC2_W);
        emit2LE(index);
        return this;
    }


    public CodeBuilder iload(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.ILOAD_0 + n);
        else { emit(Opcode.ILOAD); emit(n); }
        return this;
    }

    public CodeBuilder lload(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.LLOAD_0 + n);
        else { emit(Opcode.LLOAD); emit(n); }
        return this;
    }

    public CodeBuilder fload(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.FLOAD_0 + n);
        else { emit(Opcode.FLOAD); emit(n); }
        return this;
    }

    public CodeBuilder dload(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.DLOAD_0 + n);
        else { emit(Opcode.DLOAD); emit(n); }
        return this;
    }

    public CodeBuilder aload(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.ALOAD_0 + n);
        else { emit(Opcode.ALOAD); emit(n); }
        return this;
    }


    public CodeBuilder iaload()  { emit(Opcode.IALOAD);  return this; }
    public CodeBuilder laload()  { emit(Opcode.LALOAD);  return this; }
    public CodeBuilder faload()  { emit(Opcode.FALOAD);  return this; }
    public CodeBuilder daload()  { emit(Opcode.DALOAD);  return this; }
    public CodeBuilder aaload()  { emit(Opcode.AALOAD);  return this; }
    public CodeBuilder baload()  { emit(Opcode.BALOAD);  return this; }
    public CodeBuilder caload()  { emit(Opcode.CALOAD);  return this; }
    public CodeBuilder saload()  { emit(Opcode.SALOAD);  return this; }


    public CodeBuilder istore(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.ISTORE_0 + n);
        else { emit(Opcode.ISTORE); emit(n); }
        return this;
    }

    public CodeBuilder lstore(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.LSTORE_0 + n);
        else { emit(Opcode.LSTORE); emit(n); }
        return this;
    }

    public CodeBuilder fstore(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.FSTORE_0 + n);
        else { emit(Opcode.FSTORE); emit(n); }
        return this;
    }

    public CodeBuilder dstore(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.DSTORE_0 + n);
        else { emit(Opcode.DSTORE); emit(n); }
        return this;
    }

    public CodeBuilder astore(int n) {
        if (n >= 0 && n <= 3) emit(Opcode.ASTORE_0 + n);
        else { emit(Opcode.ASTORE); emit(n); }
        return this;
    }


    public CodeBuilder iastore() { emit(Opcode.IASTORE); return this; }
    public CodeBuilder lastore() { emit(Opcode.LASTORE); return this; }
    public CodeBuilder fastore() { emit(Opcode.FASTORE); return this; }
    public CodeBuilder dastore() { emit(Opcode.DASTORE); return this; }
    public CodeBuilder aastore() { emit(Opcode.AASTORE); return this; }
    public CodeBuilder bastore() { emit(Opcode.BASTORE); return this; }
    public CodeBuilder castore() { emit(Opcode.CASTORE); return this; }
    public CodeBuilder sastore() { emit(Opcode.SASTORE); return this; }


    public CodeBuilder pop()     { emit(Opcode.POP);     return this; }
    public CodeBuilder pop2()    { emit(Opcode.POP2);    return this; }
    public CodeBuilder dup()     { emit(Opcode.DUP);     return this; }
    public CodeBuilder dup_x1()  { emit(Opcode.DUP_X1);  return this; }
    public CodeBuilder dup_x2()  { emit(Opcode.DUP_X2);  return this; }
    public CodeBuilder dup2()    { emit(Opcode.DUP2);    return this; }
    public CodeBuilder dup2_x1() { emit(Opcode.DUP2_X1); return this; }
    public CodeBuilder dup2_x2() { emit(Opcode.DUP2_X2); return this; }
    public CodeBuilder swap()    { emit(Opcode.SWAP);    return this; }


    public CodeBuilder iadd() { emit(Opcode.IADD); return this; }
    public CodeBuilder ladd() { emit(Opcode.LADD); return this; }
    public CodeBuilder fadd() { emit(Opcode.FADD); return this; }
    public CodeBuilder dadd() { emit(Opcode.DADD); return this; }

    public CodeBuilder isub() { emit(Opcode.ISUB); return this; }
    public CodeBuilder lsub() { emit(Opcode.LSUB); return this; }
    public CodeBuilder fsub() { emit(Opcode.FSUB); return this; }
    public CodeBuilder dsub() { emit(Opcode.DSUB); return this; }

    public CodeBuilder imul() { emit(Opcode.IMUL); return this; }
    public CodeBuilder lmul() { emit(Opcode.LMUL); return this; }
    public CodeBuilder fmul() { emit(Opcode.FMUL); return this; }
    public CodeBuilder dmul() { emit(Opcode.DMUL); return this; }

    public CodeBuilder idiv() { emit(Opcode.IDIV); return this; }
    public CodeBuilder ldiv() { emit(Opcode.LDIV); return this; }
    public CodeBuilder fdiv() { emit(Opcode.FDIV); return this; }
    public CodeBuilder ddiv() { emit(Opcode.DDIV); return this; }

    public CodeBuilder irem() { emit(Opcode.IREM); return this; }
    public CodeBuilder lrem() { emit(Opcode.LREM); return this; }
    public CodeBuilder frem() { emit(Opcode.FREM); return this; }
    public CodeBuilder drem() { emit(Opcode.DREM); return this; }

    public CodeBuilder ineg() { emit(Opcode.INEG); return this; }
    public CodeBuilder lneg() { emit(Opcode.LNEG); return this; }
    public CodeBuilder fneg() { emit(Opcode.FNEG); return this; }
    public CodeBuilder dneg() { emit(Opcode.DNEG); return this; }

    public CodeBuilder ishl()  { emit(Opcode.ISHL);  return this; }
    public CodeBuilder lshl()  { emit(Opcode.LSHL);  return this; }
    public CodeBuilder ishr()  { emit(Opcode.ISHR);  return this; }
    public CodeBuilder lshr()  { emit(Opcode.LSHR);  return this; }
    public CodeBuilder iushr() { emit(Opcode.IUSHR); return this; }
    public CodeBuilder lushr() { emit(Opcode.LUSHR); return this; }
    public CodeBuilder iand()  { emit(Opcode.IAND);  return this; }
    public CodeBuilder land()  { emit(Opcode.LAND);  return this; }
    public CodeBuilder ior()   { emit(Opcode.IOR);   return this; }
    public CodeBuilder lor()   { emit(Opcode.LOR);   return this; }
    public CodeBuilder ixor()  { emit(Opcode.IXOR);  return this; }
    public CodeBuilder lxor()  { emit(Opcode.LXOR);  return this; }

    public CodeBuilder iinc(int index, int amount) {
        emit(Opcode.IINC);
        emit(index);
        emit(amount);
        return this;
    }


    public CodeBuilder i2l() { emit(Opcode.I2L); return this; }
    public CodeBuilder i2f() { emit(Opcode.I2F); return this; }
    public CodeBuilder i2d() { emit(Opcode.I2D); return this; }
    public CodeBuilder l2i() { emit(Opcode.L2I); return this; }
    public CodeBuilder l2f() { emit(Opcode.L2F); return this; }
    public CodeBuilder l2d() { emit(Opcode.L2D); return this; }
    public CodeBuilder f2i() { emit(Opcode.F2I); return this; }
    public CodeBuilder f2l() { emit(Opcode.F2L); return this; }
    public CodeBuilder f2d() { emit(Opcode.F2D); return this; }
    public CodeBuilder d2i() { emit(Opcode.D2I); return this; }
    public CodeBuilder d2l() { emit(Opcode.D2L); return this; }
    public CodeBuilder d2f() { emit(Opcode.D2F); return this; }
    public CodeBuilder i2b() { emit(Opcode.I2B); return this; }
    public CodeBuilder i2c() { emit(Opcode.I2C); return this; }
    public CodeBuilder i2s() { emit(Opcode.I2S); return this; }


    public CodeBuilder lcmp()  { emit(Opcode.LCMP);  return this; }
    public CodeBuilder fcmpl() { emit(Opcode.FCMPL); return this; }
    public CodeBuilder fcmpg() { emit(Opcode.FCMPG); return this; }
    public CodeBuilder dcmpl() { emit(Opcode.DCMPL); return this; }
    public CodeBuilder dcmpg() { emit(Opcode.DCMPG); return this; }


    public CodeBuilder ifeq(Label L) { return emitJump1(Opcode.IFEQ, L); }
    public CodeBuilder ifne(Label L) { return emitJump1(Opcode.IFNE, L); }
    public CodeBuilder iflt(Label L) { return emitJump1(Opcode.IFLT, L); }
    public CodeBuilder ifge(Label L) { return emitJump1(Opcode.IFGE, L); }
    public CodeBuilder ifgt(Label L) { return emitJump1(Opcode.IFGT, L); }
    public CodeBuilder ifle(Label L) { return emitJump1(Opcode.IFLE, L); }

    public CodeBuilder if_icmpeq(Label L) { return emitJump1(Opcode.IF_ICMPEQ, L); }
    public CodeBuilder if_icmpne(Label L) { return emitJump1(Opcode.IF_ICMPNE, L); }
    public CodeBuilder if_icmplt(Label L) { return emitJump1(Opcode.IF_ICMPLT, L); }
    public CodeBuilder if_icmpge(Label L) { return emitJump1(Opcode.IF_ICMPGE, L); }
    public CodeBuilder if_icmpgt(Label L) { return emitJump1(Opcode.IF_ICMPGT, L); }
    public CodeBuilder if_icmple(Label L) { return emitJump1(Opcode.IF_ICMPLE, L); }

    public CodeBuilder if_acmpeq(Label L) { return emitJump1(Opcode.IF_ACMPEQ, L); }
    public CodeBuilder if_acmpne(Label L) { return emitJump1(Opcode.IF_ACMPNE, L); }

    public CodeBuilder goto_(Label L) { return emitJump1(Opcode.GOTO, L); }

    public CodeBuilder goto_w(Label L) {
        int opcodePos = length;
        emit(Opcode.GOTO_W);
        int operandPos = length;
        emit4(0);
        resolveJump(opcodePos, operandPos, L, true);
        return this;
    }

    public CodeBuilder ifnull(Label L)    { return emitJump1(Opcode.IFNULL, L); }
    public CodeBuilder ifnonnull(Label L) { return emitJump1(Opcode.IFNONNULL, L); }


    public CodeBuilder ireturn() { emit(Opcode.IRETURN); return this; }
    public CodeBuilder lreturn() { emit(Opcode.LRETURN); return this; }
    public CodeBuilder freturn() { emit(Opcode.FRETURN); return this; }
    public CodeBuilder dreturn() { emit(Opcode.DRETURN); return this; }
    public CodeBuilder areturn() { emit(Opcode.ARETURN); return this; }
    public CodeBuilder return_() { emit(Opcode.RETURN);  return this; }


    public CodeBuilder getstatic(int idx) {
        emit(Opcode.GETSTATIC); emit2LE(idx);
        return this;
    }
    public CodeBuilder putstatic(int idx) {
        emit(Opcode.PUTSTATIC); emit2LE(idx);
        return this;
    }
    public CodeBuilder getfield(int idx) {
        emit(Opcode.GETFIELD); emit2LE(idx);
        return this;
    }
    public CodeBuilder putfield(int idx) {
        emit(Opcode.PUTFIELD); emit2LE(idx);
        return this;
    }

    public CodeBuilder getstatic(InstanceKlass ik, String owner, String name, String desc) {
        return getstatic(ik.addFieldref(owner, name, desc));
    }
    public CodeBuilder putstatic(InstanceKlass ik, String owner, String name, String desc) {
        return putstatic(ik.addFieldref(owner, name, desc));
    }
    public CodeBuilder getfield(InstanceKlass ik, String owner, String name, String desc) {
        return getfield(ik.addFieldref(owner, name, desc));
    }
    public CodeBuilder putfield(InstanceKlass ik, String owner, String name, String desc) {
        return putfield(ik.addFieldref(owner, name, desc));
    }

    public CodeBuilder getstatic(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return getstatic(ik.addFieldref(owner.getName().replace('.', '/'), name, desc, owner));
    }
    public CodeBuilder putstatic(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return putstatic(ik.addFieldref(owner.getName().replace('.', '/'), name, desc, owner));
    }
    public CodeBuilder getfield(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return getfield(ik.addFieldref(owner.getName().replace('.', '/'), name, desc, owner));
    }
    public CodeBuilder putfield(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return putfield(ik.addFieldref(owner.getName().replace('.', '/'), name, desc, owner));
    }


    public CodeBuilder invokevirtual(int idx) {
        emit(Opcode.INVOKEVIRTUAL); emit2LE(idx);
        return this;
    }
    public CodeBuilder invokespecial(int idx) {
        emit(Opcode.INVOKESPECIAL); emit2LE(idx);
        return this;
    }
    public CodeBuilder invokestatic(int idx) {
        emit(Opcode.INVOKESTATIC); emit2LE(idx);
        return this;
    }
    public CodeBuilder invokeinterface(int idx, int count) {
        emit(Opcode.INVOKEINTERFACE); emit2LE(idx); emit(count); emit(0);
        return this;
    }
    public CodeBuilder invokedynamic(int idx) {
        emit(Opcode.INVOKEDYNAMIC); emit2LE(idx); emit(0); emit(0);
        return this;
    }

    public CodeBuilder invokevirtual(InstanceKlass ik, String owner, String name, String desc) {
        return invokevirtual(ik.addMethodref(owner, name, desc));
    }
    public CodeBuilder invokespecial(InstanceKlass ik, String owner, String name, String desc) {
        return invokespecial(ik.addMethodref(owner, name, desc));
    }
    public CodeBuilder invokestatic(InstanceKlass ik, String owner, String name, String desc) {
        return invokestatic(ik.addMethodref(owner, name, desc));
    }
    public CodeBuilder invokeinterface(InstanceKlass ik, String owner, String name, String desc, int count) {
        return invokeinterface(ik.addInterfaceMethodref(owner, name, desc), count);
    }

    public CodeBuilder invokevirtual(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return invokevirtual(ik.addMethodref(owner.getName().replace('.', '/'), name, desc, owner));
    }
    public CodeBuilder invokespecial(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return invokespecial(ik.addMethodref(owner.getName().replace('.', '/'), name, desc, owner));
    }
    public CodeBuilder invokestatic(InstanceKlass ik, Class<?> owner, String name, String desc) {
        return invokestatic(ik.addMethodref(owner.getName().replace('.', '/'), name, desc, owner));
    }
    public CodeBuilder invokeinterface(InstanceKlass ik, Class<?> owner, String name, String desc, int count) {
        return invokeinterface(ik.addInterfaceMethodref(owner.getName().replace('.', '/'), name, desc, owner), count);
    }


    public CodeBuilder new_(int classIdx) {
        emit(Opcode.NEW); emit2(classIdx);
        return this;
    }
    public CodeBuilder new_(InstanceKlass ik, String className) {
        return new_(ik.addClass(className));
    }
    public CodeBuilder new_(InstanceKlass ik, Class<?> c) {
        return new_(ik.addClass(c.getName().replace('.', '/'), c));
    }

    public CodeBuilder newarray(int atype) {
        emit(Opcode.NEWARRAY); emit(atype);
        return this;
    }
    public CodeBuilder newarrayBoolean() { return newarray(T_BOOLEAN); }
    public CodeBuilder newarrayChar()    { return newarray(T_CHAR); }
    public CodeBuilder newarrayFloat()   { return newarray(T_FLOAT); }
    public CodeBuilder newarrayDouble()  { return newarray(T_DOUBLE); }
    public CodeBuilder newarrayByte()    { return newarray(T_BYTE); }
    public CodeBuilder newarrayShort()   { return newarray(T_SHORT); }
    public CodeBuilder newarrayInt()     { return newarray(T_INT); }
    public CodeBuilder newarrayLong()    { return newarray(T_LONG); }

    public CodeBuilder anewarray(int classIdx) {
        emit(Opcode.ANEWARRAY); emit2(classIdx);
        return this;
    }
    public CodeBuilder anewarray(InstanceKlass ik, String className) {
        return anewarray(ik.addClass(className));
    }
    public CodeBuilder anewarray(InstanceKlass ik, Class<?> c) {
        return anewarray(ik.addClass(c.getName().replace('.', '/'), c));
    }

    public CodeBuilder arraylength() { emit(Opcode.ARRAYLENGTH); return this; }
    public CodeBuilder athrow()      { emit(Opcode.ATHROW);      return this; }

    public CodeBuilder checkcast(int classIdx) {
        emit(Opcode.CHECKCAST); emit2(classIdx);
        return this;
    }
    public CodeBuilder checkcast(InstanceKlass ik, String className) {
        return checkcast(ik.addClass(className));
    }
    public CodeBuilder checkcast(InstanceKlass ik, Class<?> c) {
        return checkcast(ik.addClass(c.getName().replace('.', '/'), c));
    }

    public CodeBuilder instanceof_(int classIdx) {
        emit(Opcode.INSTANCEOF); emit2(classIdx);
        return this;
    }
    public CodeBuilder instanceof_(InstanceKlass ik, String className) {
        return instanceof_(ik.addClass(className));
    }
    public CodeBuilder instanceof_(InstanceKlass ik, Class<?> c) {
        return instanceof_(ik.addClass(c.getName().replace('.', '/'), c));
    }

    public CodeBuilder monitorenter() { emit(Opcode.MONITORENTER); return this; }
    public CodeBuilder monitorexit()  { emit(Opcode.MONITOREXIT);  return this; }

    public CodeBuilder multianewarray(int classIdx, int dims) {
        emit(Opcode.MULTIANEWARRAY); emit2(classIdx); emit(dims);
        return this;
    }
    public CodeBuilder multianewarray(InstanceKlass ik, String className, int dims) {
        return multianewarray(ik.addClass(className), dims);
    }
    public CodeBuilder multianewarray(InstanceKlass ik, Class<?> c, int dims) {
        return multianewarray(ik.addClass(c.getName().replace('.', '/'), c), dims);
    }
}