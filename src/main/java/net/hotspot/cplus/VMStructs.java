
package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;


import java.nio.charset.StandardCharsets;

public class VMStructs {

    private VMStructs() {}

    private static String readCString(long addr) {
        if (addr == 0) return null;
        int len = 0;
        while (UnsafeUtils.unsafe.getByte(addr + len) != 0) len++;
        if (len == 0) return "";
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = UnsafeUtils.unsafe.getByte(addr + i);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static class VMStructEntry {
        public static final int SIZE               = 48;
        public static final int TYPE_NAME_OFFSET   = 0;
        public static final int FIELD_NAME_OFFSET  = 8;
        public static final int TYPE_STRING_OFFSET = 16;
        public static final int IS_STATIC_OFFSET   = 24;
        public static final int OFFSET_OFFSET      = 32;
        public static final int ADDRESS_OFFSET     = 40;

        public final long   address;
        public final String typeName;
        public final String fieldName;
        public final String typeString;
        public final int    isStatic;
        public final long   offset;
        public final long   fieldAddress;

        public VMStructEntry(long address) {
            this.address      = address;
            this.typeName     = readCString(UnsafeUtils.unsafe.getLong(address + TYPE_NAME_OFFSET));
            this.fieldName    = readCString(UnsafeUtils.unsafe.getLong(address + FIELD_NAME_OFFSET));
            this.typeString   = readCString(UnsafeUtils.unsafe.getLong(address + TYPE_STRING_OFFSET));
            this.isStatic     = UnsafeUtils.unsafe.getInt(address + IS_STATIC_OFFSET);
            this.offset       = UnsafeUtils.unsafe.getLong(address + OFFSET_OFFSET);
            this.fieldAddress = UnsafeUtils.unsafe.getLong(address + ADDRESS_OFFSET);
        }

        public boolean isStaticField() { return isStatic != 0; }
        public boolean isLastEntry()   { return fieldName == null; }

        @Override
        public String toString() {
            if (isLastEntry()) return "VMStructEntry[LAST]";
            if (isStaticField()) {
                return String.format("VMStructEntry[%s::%s %s static addr=0x%x]",
                        typeName, fieldName, typeString, fieldAddress);
            }
            return String.format("VMStructEntry[%s::%s %s offset=%d]",
                    typeName, fieldName, typeString, offset);
        }
    }

    public static class VMTypeEntry {
        public static final int SIZE                   = 40;
        public static final int TYPE_NAME_OFFSET       = 0;
        public static final int SUPERCLASS_NAME_OFFSET = 8;
        public static final int IS_OOP_TYPE_OFFSET     = 16;
        public static final int IS_INTEGER_TYPE_OFFSET = 20;
        public static final int IS_UNSIGNED_OFFSET     = 24;
        public static final int SIZE_OFFSET            = 32;

        public final long   address;
        public final String typeName;
        public final String superclassName;
        public final int    isOopType;
        public final int    isIntegerType;
        public final int    isUnsigned;
        public final long   size;

        public VMTypeEntry(long address) {
            this.address        = address;
            this.typeName       = readCString(UnsafeUtils.unsafe.getLong(address + TYPE_NAME_OFFSET));
            this.superclassName = readCString(UnsafeUtils.unsafe.getLong(address + SUPERCLASS_NAME_OFFSET));
            this.isOopType      = UnsafeUtils.unsafe.getInt(address + IS_OOP_TYPE_OFFSET);
            this.isIntegerType  = UnsafeUtils.unsafe.getInt(address + IS_INTEGER_TYPE_OFFSET);
            this.isUnsigned     = UnsafeUtils.unsafe.getInt(address + IS_UNSIGNED_OFFSET);
            this.size           = UnsafeUtils.unsafe.getLong(address + SIZE_OFFSET);
        }

        public boolean isOop()       { return isOopType != 0; }
        public boolean isInteger()   { return isIntegerType != 0; }
        public boolean isUnsigned()  { return isUnsigned != 0; }
        public boolean isLastEntry() { return typeName == null; }

        @Override
        public String toString() {
            if (isLastEntry()) return "VMTypeEntry[LAST]";
            return String.format("VMTypeEntry[%s : %s size=%d isOop=%d isInt=%d isUnsigned=%d]",
                    typeName, superclassName == null ? "null" : superclassName,
                    size, isOopType, isIntegerType, isUnsigned);
        }
    }

    public static class VMIntConstantEntry {
        public static final int SIZE         = 16;
        public static final int NAME_OFFSET  = 0;
        public static final int VALUE_OFFSET = 8;

        public final long   address;
        public final String name;
        public final int    value;

        public VMIntConstantEntry(long address) {
            this.address = address;
            this.name    = readCString(UnsafeUtils.unsafe.getLong(address + NAME_OFFSET));
            this.value   = UnsafeUtils.unsafe.getInt(address + VALUE_OFFSET);
        }

        public boolean isLastEntry() { return name == null; }

        @Override
        public String toString() {
            if (isLastEntry()) return "VMIntConstantEntry[LAST]";
            return String.format("VMIntConstantEntry[%s = %d]", name, value);
        }
    }

    public static class VMLongConstantEntry {
        public static final int SIZE         = 16;
        public static final int NAME_OFFSET  = 0;
        public static final int VALUE_OFFSET = 8;

        public final long   address;
        public final String name;
        public final long   value;

        public VMLongConstantEntry(long address) {
            this.address = address;
            this.name    = readCString(UnsafeUtils.unsafe.getLong(address + NAME_OFFSET));
            this.value   = UnsafeUtils.unsafe.getLong(address + VALUE_OFFSET);
        }

        public boolean isLastEntry() { return name == null; }

        @Override
        public String toString() {
            if (isLastEntry()) return "VMLongConstantEntry[LAST]";
            return String.format("VMLongConstantEntry[%s = %d]", name, value);
        }
    }

    public static class VMAddressEntry {
        public static final int SIZE         = 16;
        public static final int NAME_OFFSET  = 0;
        public static final int VALUE_OFFSET = 8;

        public final long   address;
        public final String name;
        public final long   value;

        public VMAddressEntry(long address) {
            this.address = address;
            this.name    = readCString(UnsafeUtils.unsafe.getLong(address + NAME_OFFSET));
            this.value   = UnsafeUtils.unsafe.getLong(address + VALUE_OFFSET);
        }

        public boolean isLastEntry() { return name == null; }

        @Override
        public String toString() {
            if (isLastEntry()) return "VMAddressEntry[LAST]";
            return String.format("VMAddressEntry[%s = 0x%x]", name, value);
        }
    }
}