package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

/**
 * Java mapping of HotSpot's oops/methodData.hpp DataLayout.
 *
 * class DataLayout {                    // no vptr; CHeap-like value type
 *   union { u8 _bits; struct { u1 _tag; u1 _flags; u2 _bci; u4 _traps; } _struct; } _header;
 *   intptr_t _cells[1];
 * };
 *
 * Layout on LP64:
 *   +0  u1  _tag
 *   +1  u1  _flags
 *   +2  u2  _bci
 *   +4  u4  _traps
 *   +8  _cells[0]
 *
 * header_size_in_cells() = 1  =>  8 bytes
 * cell_size              = 8
 */
public final class DataLayout {

    private static final long TAG_OFFSET   = findOffsetOrDefault("DataLayout::_header._struct._tag",    0L);
    private static final long FLAGS_OFFSET = findOffsetOrDefault("DataLayout::_header._struct._flags",  1L);
    private static final long BCI_OFFSET   = findOffsetOrDefault("DataLayout::_header._struct._bci",    2L);
    private static final long TRAPS_OFFSET = findOffsetOrDefault("DataLayout::_header._struct._traps",  4L);
    private static final long CELLS_OFFSET = findOffsetOrDefault("DataLayout::_cells[0]",               8L);

    /** Header occupies 1 cell on LP64. */
    public static final int HEADER_CELLS = 1;

    public int  tag;
    public int  flags;
    public int  bci;
    public long traps;

    private final long address;

    public DataLayout(long address) {
        if (address == 0) {
            throw new IllegalArgumentException("DataLayout: address == 0");
        }
        this.address = address;
        readFields();
    }

    public final void readFields() {
        long base = address;
        tag   = UnsafeUtils.unsafe.getByte (base + TAG_OFFSET)   & 0xFF;
        flags = UnsafeUtils.unsafe.getByte (base + FLAGS_OFFSET) & 0xFF;
        bci   = UnsafeUtils.unsafe.getShort(base + BCI_OFFSET)   & 0xFFFF;
        traps = UnsafeUtils.unsafe.getInt  (base + TRAPS_OFFSET) & 0xFFFFFFFFL;
    }

    public long getAddress() { return address; }

    // ---------- cells ----------
    public long cellAt(int index) {
        return UnsafeUtils.unsafe.getLong(address + CELLS_OFFSET + index * 8L);
    }

    public void setCellAt(int index, long v) {
        UnsafeUtils.unsafe.putLong(address + CELLS_OFFSET + index * 8L, v);
    }

    public int  intAt(int index)             { return (int) cellAt(index); }
    public void setIntAt(int index, int v)   { setCellAt(index, v); }

    public int sizeInBytes() {
        return sizeInBytesFor(MethodData.cellCountFor(this));
    }

    public static int sizeInBytesFor(int cellCount) {
        return (HEADER_CELLS + cellCount) * 8;
    }

    // ---------- header writers ----------
    public void setTag(int v) {
        tag = v;
        UnsafeUtils.unsafe.putByte(address + TAG_OFFSET, (byte) v);
    }
    public void setFlags(int v) {
        flags = v;
        UnsafeUtils.unsafe.putByte(address + FLAGS_OFFSET, (byte) v);
    }
    public void setBci(int v) {
        bci = v;
        UnsafeUtils.unsafe.putShort(address + BCI_OFFSET, (short) v);
    }
    public void setTraps(long v) {
        traps = v;
        UnsafeUtils.unsafe.putInt(address + TRAPS_OFFSET, (int) v);
    }

    // ---------- tag predicates ----------
    public boolean isBitData()             { return tag == MethodData.TAG_BIT_DATA; }
    public boolean isCounterData()         { return tag == MethodData.TAG_COUNTER_DATA; }
    public boolean isJumpData()            { return tag == MethodData.TAG_JUMP_DATA; }
    public boolean isReceiverTypeData()    { return tag == MethodData.TAG_RECEIVER_TYPE_DATA; }
    public boolean isVirtualCallData()     { return tag == MethodData.TAG_VIRTUAL_CALL_DATA; }
    public boolean isRetData()             { return tag == MethodData.TAG_RET_DATA; }
    public boolean isBranchData()          { return tag == MethodData.TAG_BRANCH_DATA; }
    public boolean isMultiBranchData()     { return tag == MethodData.TAG_MULTI_BRANCH_DATA; }
    public boolean isArgInfoData()         { return tag == MethodData.TAG_ARG_INFO_DATA; }
    public boolean isCallTypeData()        { return tag == MethodData.TAG_CALL_TYPE_DATA; }
    public boolean isVirtualCallTypeData() { return tag == MethodData.TAG_VIRTUAL_CALL_TYPE_DATA; }
    public boolean isParametersTypeData()  { return tag == MethodData.TAG_PARAMETERS_TYPE_DATA; }
    public boolean isSpeculativeTrapData() { return tag == MethodData.TAG_SPECULATIVE_TRAP_DATA; }

    // ---------- common accessors ----------
    public int  counterCount() { return intAt(0); }
    public boolean nullSeen()  { return (flags & 0x01) != 0; }

    // ---------- JumpData ----------
    // taken_off_set = 0, displacement_off_set = 1
    public int jumpTaken()        { return intAt(0); }
    public int jumpDisplacement() { return intAt(1); }

    // ---------- BranchData ----------
    // taken = 0, displacement = 1, not_taken = 2
    public int branchTaken()        { return intAt(0); }
    public int branchDisplacement() { return intAt(1); }
    public int branchNotTaken()     { return intAt(2); }

    public int  receiverRowLimit()       { return 2; }   // TypeProfileWidth = 2
    public int  nonprofiledCount()       { return intAt(1); }
    public long receiverAt(int row)      { return cellAt(2 + row * 2); }
    public int  receiverCountAt(int row) { return intAt (3 + row * 2); }
    public int retBciAt(int row)          { return intAt(1 + row * 3); }
    public int retCountAt(int row)        { return intAt(2 + row * 3); }
    public int retDisplacementAt(int row) { return intAt(3 + row * 3); }
    public int retRowLimit()              { return 2; }   // BciProfileWidth = 2

    public int arrayLen() { return intAt(0); }

    public int multiBranchDefaultCount()   { return intAt(1); }
    public int multiBranchDefaultDisp()    { return intAt(2); }
    public int multiBranchNumCases()       { return (arrayLen() - 2) / 2; }
    public int multiBranchCaseCount(int i) { return intAt(3 + i * 2); }
    public int multiBranchCaseDisp(int i)  { return intAt(4 + i * 2); }

    public int argInfoNumArgs()      { return intAt(0); }
    public int argInfoModified(int i){ return intAt(1 + i); }

    public int  paramsNum()             { return intAt(0) / 2; }
    public int  paramsStackSlot(int i)  { return intAt (1 + i * 2); }
    public long paramsType(int i)       { return cellAt(2 + i * 2); }

    public int callTypeExtraCells()        { return intAt(1); }
    public int virtualCallTypeExtraCells() { return intAt(5); }

    public long speculativeMethod() { return cellAt(0); }

    @Override
    public String toString() {
        return String.format(
                "DataLayout[address=0x%x, tag=%d (%s), bci=%d, flags=0x%02X, traps=0x%X]",
                address, tag, MethodData.tagName(tag), bci, flags, traps);
    }
}