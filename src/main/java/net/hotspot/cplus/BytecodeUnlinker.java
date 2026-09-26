package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class BytecodeUnlinker {

    private static final boolean DEBUG = true;

    private static int fastToStandard(int op) {
        switch (op) {
            case 0xCB: case 0xCC: case 0xCD: case 0xCE:
            case 0xCF: case 0xD0: case 0xD1: case 0xD2:
                return 0xB4;
            case 0xD3: case 0xD4: case 0xD5: case 0xD6:
            case 0xD7: case 0xD8: case 0xD9: case 0xDA:
            case 0xDB:
                return 0xB5;
            case 0xDC: case 0xDD: case 0xDE: case 0xDF:
                return 0x2A;
            case 0xE6:
                return 0x12;
            case 0xE7:
                return 0x13;
            case 0xF6:
                return 0xB6;
            case 0xF7:
                return 0xB7;
            case 0xF8:
                return 0xB8;
            case 0xF9:
                return 0xB9;
            default:
                return op;
        }
    }

    public static byte[] unlink(byte[] code, ConstantPoolCache cpc) {
        byte[] out = code.clone();
        int i = 0;
        while (i < out.length) {
            int origOp = out[i] & 0xFF;
            boolean wasFastAldc  = (origOp == 0xE6);
            boolean wasFastAldcW = (origOp == 0xE7);
            int op = fastToStandard(origOp);
            if (op != origOp) out[i] = (byte) op;

            switch (op) {
                case 0xB2: case 0xB3: case 0xB4: case 0xB5:
                case 0xB6: case 0xB7: case 0xB8: {
                    int cacheIdx = (out[i + 1] & 0xFF) | ((out[i + 2] & 0xFF) << 8);
                    int cpIdx    = cpc.entryConstantPoolIndex(cacheIdx);
                    if (DEBUG) {
                        System.out.printf("[UNLINK] bci=%d op=0x%02x cacheIdx=%d cpIdx=%d%n",
                                i, op, cacheIdx, cpIdx);
                    }
                    out[i + 1] = (byte) (cpIdx & 0xFF);
                    out[i + 2] = (byte) ((cpIdx >>> 8) & 0xFF);
                    i += 3;
                    break;
                }
                case 0xB9: {
                    int cacheIdx = (out[i + 1] & 0xFF) | ((out[i + 2] & 0xFF) << 8);
                    int cpIdx    = cpc.entryConstantPoolIndex(cacheIdx);
                    if (DEBUG) {
                        System.out.printf("[UNLINK] bci=%d invokeinterface cacheIdx=%d cpIdx=%d%n",
                                i, cacheIdx, cpIdx);
                    }
                    out[i + 1] = (byte) (cpIdx & 0xFF);
                    out[i + 2] = (byte) ((cpIdx >>> 8) & 0xFF);
                    i += 5;
                    break;
                }
                case 0xBA: {
                    if (DEBUG) {
                        System.out.printf("[UNLINK] bci=%d invokedynamic %n", i);
                    }
                    i += 5;
                    break;
                }
                case 0x12: {
                    if (wasFastAldc) {
                        int refIdx = out[i + 1] & 0xFF;
                        int cpIdx  = readReferenceMap(cpc, refIdx);
                        if (DEBUG) {
                            System.out.printf("[UNLINK] bci=%d ldc refIdx=%d cpIdx=%d%n",
                                    i, refIdx, cpIdx);
                        }
                        out[i + 1] = (byte) cpIdx;
                    }
                    i += 2;
                    break;
                }
                case 0x13: case 0x14: {
                    if (wasFastAldcW) {
                        int refIdx = (out[i + 1] & 0xFF) | ((out[i + 2] & 0xFF) << 8);
                        int cpIdx  = readReferenceMap(cpc, refIdx);
                        if (DEBUG) {
                            System.out.printf("[UNLINK] bci=%d ldc_w refIdx=%d cpIdx=%d%n",
                                    i, refIdx, cpIdx);
                        }
                        out[i + 1] = (byte) (cpIdx & 0xFF);
                        out[i + 2] = (byte) ((cpIdx >>> 8) & 0xFF);
                    }
                    i += 3;
                    break;
                }
                case 0xAA: {
                    int pad  = (4 - ((i + 1) % 4)) % 4;
                    int base = i + 1 + pad;
                    int low  = be32(out, base + 4);
                    int high = be32(out, base + 8);
                    i += 1 + pad + 12 + (high - low + 1) * 4;
                    break;
                }
                case 0xAB: {
                    int pad    = (4 - ((i + 1) % 4)) % 4;
                    int base   = i + 1 + pad;
                    int npairs = be32(out, base + 4);
                    i += 1 + pad + 8 + npairs * 8;
                    break;
                }
                case 0xC4: {
                    int wop = out[i + 1] & 0xFF;
                    i += (wop == 0x84) ? 6 : 4;
                    break;
                }
                default:
                    i += opcodeLength(op);
            }
        }
        return out;
    }

    private static int readReferenceMap(ConstantPoolCache cpc, int refIdx) {
        long rm = cpc.referenceMap;
        if (rm == 0) return refIdx;
        return UnsafeUtils.unsafe.getShort(rm + 4L + refIdx * 2L) & 0xFFFF;
    }

    private static int be32(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24)
                | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8)
                | (b[off + 3] & 0xFF);
    }

    private static int opcodeLength(int op) {
        switch (op) {
            case 0x10: return 2;
            case 0x11: return 3;
            case 0x12: return 2;
            case 0x13: return 3;
            case 0x14: return 3;
            case 0x15: case 0x16: case 0x17: case 0x18: case 0x19: return 2;
            case 0x36: case 0x37: case 0x38: case 0x39: case 0x3A: return 2;
            case 0x84: return 3;
            case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E:
            case 0x9F: case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4:
            case 0xA5: case 0xA6: case 0xA7: case 0xA8: return 3;
            case 0xA9: return 2;
            case 0xB2: case 0xB3: case 0xB4: case 0xB5:
            case 0xB6: case 0xB7: case 0xB8: return 3;
            case 0xB9: case 0xBA: return 5;
            case 0xBB: return 3;
            case 0xBC: return 2;
            case 0xBD: return 3;
            case 0xC0: case 0xC1: return 3;
            case 0xC5: return 4;
            case 0xC6: case 0xC7: return 3;
            case 0xC8: case 0xC9: return 5;
            default:   return 1;
        }
    }
    public static java.util.List<Integer> scanFreturns(byte[] code) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        int i = 0;
        while (i < code.length) {
            if ((code[i] & 0xFF) == 0xAE) out.add(i);
            i += instructionLength(code, i);
        }
        return out;
    }

    public static int instructionLength(byte[] code, int i) {
        int op = code[i] & 0xFF;
        switch (op) {
            case 0x10: return 2;
            case 0x11: return 3;
            case 0x12: return 2;
            case 0x13: case 0x14: return 3;
            case 0x15: case 0x16: case 0x17: case 0x18: case 0x19: return 2;
            case 0x36: case 0x37: case 0x38: case 0x39: case 0x3A: return 2;
            case 0x84: return 3;
            case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E:
            case 0x9F: case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4:
            case 0xA5: case 0xA6: case 0xA7: case 0xA8: return 3;
            case 0xA9: return 2;
            case 0xB2: case 0xB3: case 0xB4: case 0xB5:
            case 0xB6: case 0xB7: case 0xB8: return 3;
            case 0xB9: case 0xBA: return 5;
            case 0xBB: return 3;
            case 0xBC: return 2;
            case 0xBD: return 3;
            case 0xC0: case 0xC1: return 3;
            case 0xC5: return 4;
            case 0xC6: case 0xC7: return 3;
            case 0xC8: case 0xC9: return 5;
            default: return 1;
        }
    }
}