package net.hotspot.cplus;




import net.hotspot.cplus.Utils.UnsafeUtils;

import java.nio.charset.StandardCharsets;

import static net.hotspot.cplus.VMStructsHelper.findOffsetOrDefault;


public class Symbol extends MetaspaceObj {

    private static final long HASH_AND_REFCOUNT_OFFSET = findOffsetOrDefault("Symbol::_refcount",        0L);
    private static final long LENGTH_OFFSET            = findOffsetOrDefault("Symbol::_length",           4L);
    private static final long BODY_OFFSET              = findOffsetOrDefault("Symbol::_body",             6L);

    public int  hashAndRefcount;
    public short length;
    public String value;

    public Symbol(long address) {
        super(address);
        if (address == 0) {
            this.value = null;
            return;
        }
        readFields();
    }

    public Symbol(Class<?> clazz) {
        super(clazz);
        if (getAddress() == 0) {
            this.value = null;
            return;
        }
        readFields();
    }

    private void readFields() {
        long base = getAddress();
        if (base == 0) return;
        hashAndRefcount = UnsafeUtils.unsafe.getInt(base + HASH_AND_REFCOUNT_OFFSET);
        length          = UnsafeUtils.unsafe.getShort(base + LENGTH_OFFSET);

        int len = length & 0xFFFF;
        if (len == 0) {
            value = "";
            return;
        }
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = UnsafeUtils.unsafe.getByte(base + BODY_OFFSET + i);
        }
        value = new String(bytes, StandardCharsets.UTF_8);
    }

    public int refcount() {
        return hashAndRefcount & 0xFFFF;
    }

    public short hash() {
        return (short) (hashAndRefcount >>> 16);
    }

    public int utf8Length() {
        return length & 0xFFFF;
    }

    @Override
    public String toString() {
        return "Symbol[address=0x" + Long.toHexString(getAddress())
                + ", refcount=" + refcount()
                + ", length=" + utf8Length()
                + ", value=\"" + value + "\"]";
    }
}