package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ClassLoaderData {

    private static final long HOLDER_OFF            = findOffset(0L,   "ClassLoaderData::_holder");
    private static final long CLASS_LOADER_OFF      = findOffset(8L,   "ClassLoaderData::_class_loader");
    private static final long METASPACE_OFF         = findOffset(16L,  "ClassLoaderData::_metaspace");
    private static final long METASPACE_LOCK_OFF    = findOffset(24L,  "ClassLoaderData::_metaspace_lock");
    private static final long UNLOADING_OFF         = findOffset(32L,  "ClassLoaderData::_unloading");
    private static final long HAS_MIRROR_HOLDER_OFF = findOffset(33L,  "ClassLoaderData::_has_class_mirror_holder");
    private static final long MODIFIED_OOPS_OFF     = findOffset(34L,  "ClassLoaderData::_modified_oops");
    private static final long KEEP_ALIVE_OFF        = findOffset(36L,  "ClassLoaderData::_keep_alive");
    private static final long CLAIM_OFF             = findOffset(40L,  "ClassLoaderData::_claim");
    private static final long HANDLES_OFF           = findOffset(48L,  "ClassLoaderData::_handles");
    private static final long KLASSES_OFF           = findOffset(56L,  "ClassLoaderData::_klasses");
    private static final long PACKAGES_OFF          = findOffset(64L,  "ClassLoaderData::_packages");
    private static final long MODULES_OFF           = findOffset(72L,  "ClassLoaderData::_modules");
    private static final long UNNAMED_MODULE_OFF    = findOffset(80L,  "ClassLoaderData::_unnamed_module");
    private static final long DICTIONARY_OFF        = findOffset(88L,  "ClassLoaderData::_dictionary");
    private static final long JMETHOD_IDS_OFF       = findOffset(96L,  "ClassLoaderData::_jmethod_ids");
    private static final long DEALLOCATE_LIST_OFF   = findOffset(104L, "ClassLoaderData::_deallocate_list");
    private static final long NEXT_OFF              = findOffset(112L, "ClassLoaderData::_next");
    private static final long CLASS_LOADER_KLASS_OFF= findOffset(120L, "ClassLoaderData::_class_loader_klass");
    private static final long NAME_OFF              = findOffset(128L, "ClassLoaderData::_name");
    private static final long NAME_AND_ID_OFF       = findOffset(136L, "ClassLoaderData::_name_and_id");

    private static final boolean HAS_MIRROR_HOLDER_FIELD =
            VMStructsHelper.hasField("ClassLoaderData::_has_class_mirror_holder");
    private static final boolean HAS_UNLOADING_FIELD =
            VMStructsHelper.hasField("ClassLoaderData::_unloading");
    private static final boolean HAS_NAME_FIELD =
            VMStructsHelper.hasField("ClassLoaderData::_name");

    private final long address;

    public final long holderHandle;
    public final long classLoaderHandle;
    public final long metaspace;
    public final long metaspaceLock;
    public final boolean unloading;
    public final boolean hasClassMirrorHolder;
    public final boolean modifiedOops;
    public final int  keepAlive;
    public final int  claim;
    public final long handlesHead;
    public final long klasses;
    public final long packages;
    public final long modules;
    public final long unnamedModule;
    public final long dictionary;
    public final long jmethodIds;
    public final long deallocateList;
    public final long next;
    public final long classLoaderKlass;
    public final long name;
    public final long nameAndId;

    public ClassLoaderData(long address) {
        if (address == 0) throw new IllegalArgumentException("address is null");
        this.address = address;

        this.holderHandle         = UnsafeUtils.unsafe.getLong(address + HOLDER_OFF);
        this.classLoaderHandle    = UnsafeUtils.unsafe.getLong(address + CLASS_LOADER_OFF);
        this.metaspace            = UnsafeUtils.unsafe.getLong(address + METASPACE_OFF);
        this.metaspaceLock        = UnsafeUtils.unsafe.getLong(address + METASPACE_LOCK_OFF);
        this.unloading            = HAS_UNLOADING_FIELD
                && UnsafeUtils.unsafe.getByte(address + UNLOADING_OFF) != 0;
        this.hasClassMirrorHolder = HAS_MIRROR_HOLDER_FIELD
                && UnsafeUtils.unsafe.getByte(address + HAS_MIRROR_HOLDER_OFF) != 0;
        this.modifiedOops         = UnsafeUtils.unsafe.getByte(address + MODIFIED_OOPS_OFF) != 0;
        this.keepAlive            = UnsafeUtils.unsafe.getInt (address + KEEP_ALIVE_OFF);
        this.claim                = UnsafeUtils.unsafe.getInt (address + CLAIM_OFF);
        this.handlesHead          = UnsafeUtils.unsafe.getLong(address + HANDLES_OFF);
        this.klasses              = UnsafeUtils.unsafe.getLong(address + KLASSES_OFF);
        this.packages             = UnsafeUtils.unsafe.getLong(address + PACKAGES_OFF);
        this.modules              = UnsafeUtils.unsafe.getLong(address + MODULES_OFF);
        this.unnamedModule        = UnsafeUtils.unsafe.getLong(address + UNNAMED_MODULE_OFF);
        this.dictionary           = UnsafeUtils.unsafe.getLong(address + DICTIONARY_OFF);
        this.jmethodIds           = UnsafeUtils.unsafe.getLong(address + JMETHOD_IDS_OFF);
        this.deallocateList       = UnsafeUtils.unsafe.getLong(address + DEALLOCATE_LIST_OFF);
        this.next                 = UnsafeUtils.unsafe.getLong(address + NEXT_OFF);
        this.classLoaderKlass     = UnsafeUtils.unsafe.getLong(address + CLASS_LOADER_KLASS_OFF);
        this.name                 = HAS_NAME_FIELD
                ? UnsafeUtils.unsafe.getLong(address + NAME_OFF) : 0L;
        this.nameAndId            = UnsafeUtils.unsafe.getLong(address + NAME_AND_ID_OFF);
    }

    public long getAddress() { return address; }

    public ClassLoaderData next() {
        return next == 0 ? null : new ClassLoaderData(next);
    }

    public long classLoaderOop() {
        if (classLoaderHandle == 0) return 0;
        return UnsafeUtils.unsafe.getLong(classLoaderHandle);
    }

    public Object classLoaderJavaObject() {
        long oop = classLoaderOop();
        return oop == 0 ? null : UnsafeUtils.oopToJavaObject(oop);
    }

    public boolean isBootstrap() {
        return classLoaderOop() == 0;
    }

    public long holderOop() {
        if (holderHandle == 0) return 0;
        return UnsafeUtils.unsafe.getLong(holderHandle);
    }


    public Dictionary getDictionary() {
        return dictionary == 0 ? null : new Dictionary(dictionary);
    }

    public InstanceKlass firstKlass() {
        return klasses == 0 ? null : new InstanceKlass(klasses);
    }

    public List<InstanceKlass> allKlasses() {
        List<InstanceKlass> out = new ArrayList<>();
        long nextLinkOff = VMStructsHelper.findOffsetOrDefault("Klass::_next_link", -1L);
        if (nextLinkOff < 0) return out;
        long cur = klasses;
        Set<Long> seen = new HashSet<>();
        while (cur != 0) {
            if (!seen.add(cur)) break;
            out.add(new InstanceKlass(cur));
            cur = UnsafeUtils.unsafe.getLong(cur + nextLinkOff);
        }
        return out;
    }

    public boolean isAnonymous() {
        return hasClassMirrorHolder;
    }

    public String loaderName() {
        if (name == 0) return null;
        return new Symbol(name).value;
    }

    public String nameAndId() {
        if (nameAndId == 0) return null;
        return new Symbol(nameAndId).value;
    }


    public static void dumpOffsets() {
        System.out.printf(
                "ClassLoaderData offsets (fallback, product layout):%n" +
                        "  _holder=%d  _class_loader=%d  _metaspace=%d  _metaspace_lock=%d%n" +
                        "  _unloading=%d  _has_class_mirror_holder=%d  _modified_oops=%d%n" +
                        "  _keep_alive=%d  _claim=%d  _handles=%d%n" +
                        "  _klasses=%d  _packages=%d  _modules=%d  _unnamed_module=%d%n" +
                        "  _dictionary=%d  _jmethod_ids=%d  _deallocate_list=%d%n" +
                        "  _next=%d  _class_loader_klass=%d  _name=%d  _name_and_id=%d%n" +
                        "  hasField: mirrorHolder=%b  unloading=%b  name=%b%n",
                HOLDER_OFF, CLASS_LOADER_OFF, METASPACE_OFF, METASPACE_LOCK_OFF,
                UNLOADING_OFF, HAS_MIRROR_HOLDER_OFF, MODIFIED_OOPS_OFF,
                KEEP_ALIVE_OFF, CLAIM_OFF, HANDLES_OFF,
                KLASSES_OFF, PACKAGES_OFF, MODULES_OFF, UNNAMED_MODULE_OFF,
                DICTIONARY_OFF, JMETHOD_IDS_OFF, DEALLOCATE_LIST_OFF,
                NEXT_OFF, CLASS_LOADER_KLASS_OFF, NAME_OFF, NAME_AND_ID_OFF,
                HAS_MIRROR_HOLDER_FIELD, HAS_UNLOADING_FIELD, HAS_NAME_FIELD);
    }

    private static long findOffset(long fallback, String name) {
        try {
            long v = VMStructsHelper.findOffset(name);
            if (v >= 0) return v;
        } catch (Throwable ignored) { }
        return fallback;
    }

    @Override
    public String toString() {
        return String.format(
                "ClassLoaderData[address=0x%x, next=0x%x, classLoaderHandle=0x%x, " +
                        "dictionary=0x%x, metaspace=0x%x, unloading=%b, " +
                        "hasClassMirrorHolder=%b, klasses=0x%x, name=%s]",
                address, next, classLoaderHandle,
                dictionary, metaspace, unloading,
                hasClassMirrorHolder, klasses,
                name == 0 ? "null" : new Symbol(name).value);
    }
}