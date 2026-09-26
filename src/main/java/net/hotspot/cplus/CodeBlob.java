
package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;

public class CodeBlob {

    private static final long TYPE_OFFSET                  = findOffsetOrDefault("CodeBlob::_type",                  0L);
    private static final long SIZE_OFFSET                  = findOffsetOrDefault("CodeBlob::_size",                  12L);
    private static final long HEADER_SIZE_OFFSET           = findOffsetOrDefault("CodeBlob::_header_size",           16L);
    private static final long FRAME_COMPLETE_OFFSET_OFFSET = findOffsetOrDefault("CodeBlob::_frame_complete_offset", 20L);
    private static final long DATA_OFFSET_OFFSET           = findOffsetOrDefault("CodeBlob::_data_offset",           24L);
    private static final long FRAME_SIZE_OFFSET            = findOffsetOrDefault("CodeBlob::_frame_size",            28L);
    private static final long CODE_BEGIN_OFFSET            = findOffsetOrDefault("CodeBlob::_code_begin",            32L);
    private static final long CODE_END_OFFSET              = findOffsetOrDefault("CodeBlob::_code_end",              40L);
    private static final long CONTENT_BEGIN_OFFSET         = findOffsetOrDefault("CodeBlob::_content_begin",         48L);
    private static final long DATA_END_OFFSET              = findOffsetOrDefault("CodeBlob::_data_end",              56L);
    private static final long OOP_MAPS_OFFSET              = findOffsetOrDefault("CodeBlob::_oop_maps",              80L);
    private static final long NAME_OFFSET                  = findOffsetOrDefault("CodeBlob::_name",                  96L);

    private final long address;

    public int  type;
    public int  size;
    public int  headerSize;
    public int  frameCompleteOffset;
    public int  dataOffset;
    public int  frameSize;
    public long codeBegin;
    public long codeEnd;
    public long contentBegin;
    public long dataEnd;
    public long oopMaps;
    public long name;

    public CodeBlob(long address) {
        this.address = address;
        if (address == 0) return;
        readFields();
    }

    private void readFields() {
        long base = address;
        type                = UnsafeUtils.unsafe.getInt(base + TYPE_OFFSET);
        size                = UnsafeUtils.unsafe.getInt(base + SIZE_OFFSET);
        headerSize          = UnsafeUtils.unsafe.getInt(base + HEADER_SIZE_OFFSET);
        frameCompleteOffset = UnsafeUtils.unsafe.getInt(base + FRAME_COMPLETE_OFFSET_OFFSET);
        dataOffset          = UnsafeUtils.unsafe.getInt(base + DATA_OFFSET_OFFSET);
        frameSize           = UnsafeUtils.unsafe.getInt(base + FRAME_SIZE_OFFSET);
        codeBegin           = UnsafeUtils.unsafe.getLong(base + CODE_BEGIN_OFFSET);
        codeEnd             = UnsafeUtils.unsafe.getLong(base + CODE_END_OFFSET);
        contentBegin        = UnsafeUtils.unsafe.getLong(base + CONTENT_BEGIN_OFFSET);
        dataEnd             = UnsafeUtils.unsafe.getLong(base + DATA_END_OFFSET);
        oopMaps             = UnsafeUtils.unsafe.getLong(base + OOP_MAPS_OFFSET);
        name                = UnsafeUtils.unsafe.getLong(base + NAME_OFFSET);
    }

    public long getAddress() {
        return address;
    }

    public long relocationBegin() {
        return address + headerSize;
    }

    public long relocationEnd() {
        return codeBegin;
    }

    public int relocationSize() {
        return (int) (relocationEnd() - relocationBegin());
    }

    public int contentSize() {
        return (int) (contentBegin == 0 ? 0 : (codeEnd - contentBegin));
    }

    public int codeSize() {
        return (int) (codeEnd - codeBegin);
    }

    public boolean contains(long addr) {
        return contentBegin <= addr && addr < codeEnd;
    }

    public boolean codeContains(long addr) {
        return codeBegin <= addr && addr < codeEnd;
    }

    public boolean blobContains(long addr) {
        return address <= addr && addr < dataEnd;
    }

    public boolean isNmethod() {
        return false;
    }

    public boolean isRuntimeStub() {
        return false;
    }

    @Override
    public String toString() {
        return String.format(
                "CodeBlob[address=0x%x, name=0x%x, size=%d, headerSize=%d, " +
                        "codeBegin=0x%x, codeEnd=0x%x, dataEnd=0x%x, frameSize=%d]",
                address, name, size, headerSize,
                codeBegin, codeEnd, dataEnd, frameSize);
    }
}