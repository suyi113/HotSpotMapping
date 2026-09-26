package net.hotspot.cplus;


import net.hotspot.cplus.Utils.UnsafeUtils;

public class CiObject extends CiBaseObject {
    protected static final long HANDLE_OFF = 16L;
    protected static final long KLASS_OFF  = 24L;

    public CiObject(long address) { super(address); }

    public long handleSlot() { return UnsafeUtils.unsafe.getLong(address + HANDLE_OFF); }

    public long oopAddr() {
        long slot = handleSlot();
        if (slot == 0) return 0;
        if (OopDesc.USE_COMPRESSED_OOPS) {
            int narrow = UnsafeUtils.unsafe.getInt(slot);
            return OopDesc.HEAP_BASE + ((long)(narrow & 0xFFFFFFFFL) << 3);
        } else {
            return UnsafeUtils.unsafe.getLong(slot);
        }
    }

    public Object toJavaObject() {
        long oop = oopAddr();
        return oop == 0 ? null : UnsafeUtils.oopToJavaObject(oop);
    }

    public CiKlass klass() {
        long p = UnsafeUtils.unsafe.getLong(address + KLASS_OFF);
        return p == 0 ? null : new CiKlass(p);
    }
}