/**
 * Java 映射 HotSpot classfile/vmClasses.hpp 的 vmClasses。
 *
 *   class vmClasses : AllStatic {
 *     static InstanceKlass* _klasses[];       // 未指定长度，槽位连续排列
 *     static InstanceKlass* _box_klasses[];   // 未注册到 VMStructs，无法直接访问
 *   };
 *
 * VM_CLASS_AT(name) = _klasses[static_cast<int>(VM_CLASS_ID(name))]
 * vmStructs.cpp 只显式注册了 8 个元素，但都在同一连续数组里。
 * 用 Object 槽位作为基址（它在 vmClassID::FIRST，索引 0），其余可以推算。
 *
 * 已注册的 8 个槽位（Temurin 17 实测）:
 *   vmClasses::_klasses[static_cast<int>(vmClassID::Object_klass_knum)]       idx=0
 *   vmClasses::_klasses[static_cast<int>(vmClassID::String_klass_knum)]       idx=1
 *   vmClasses::_klasses[static_cast<int>(vmClassID::Class_klass_knum)]        idx=2
 *   vmClasses::_klasses[static_cast<int>(vmClassID::ClassLoader_klass_knum)]  idx=4
 *   vmClasses::_klasses[static_cast<int>(vmClassID::System_klass_knum)]       idx=6
 *   vmClasses::_klasses[static_cast<int>(vmClassID::Thread_klass_knum)]       idx=34
 *   vmClasses::_klasses[static_cast<int>(vmClassID::ThreadGroup_klass_knum)]  idx=35
 *   vmClasses::_klasses[static_cast<int>(vmClassID::MethodHandle_klass_knum)] idx=52
 */
package net.hotspot.cplus;

import net.hotspot.cplus.Utils.UnsafeUtils;

public final class VmClasses {

    private VmClasses() {}

    private static final long SLOT_OBJECT       = find("Object");
    private static final long SLOT_STRING       = find("String");
    private static final long SLOT_CLASS        = find("Class");
    private static final long SLOT_CLASSLOADER  = find("ClassLoader");
    private static final long SLOT_SYSTEM       = find("System");
    private static final long SLOT_THREAD       = find("Thread");
    private static final long SLOT_THREADGROUP  = find("ThreadGroup");
    private static final long SLOT_METHODHANDLE = find("MethodHandle");

    public static final long BASE = SLOT_OBJECT;

    public static final int MAX_SLOTS = 256;

    private static final int ELEM_SIZE = 8;

    private static long find(String name) {
        String key = "vmClasses::_klasses[static_cast<int>(vmClassID::"
                + name + "_klass_knum)]";
        try {
            long v = VMStructsHelper.findStaticAddress(key);
            return v > 0 ? v : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static int slotIndex(long slotAddr) {
        if (BASE == 0 || slotAddr == 0) return -1;
        long diff = slotAddr - BASE;
        if (diff < 0 || (diff & (ELEM_SIZE - 1)) != 0) return -1;
        return (int) (diff / ELEM_SIZE);
    }

    public static long slotAddr(int index) {
        if (BASE == 0 || index < 0 || index >= MAX_SLOTS) return 0;
        return BASE + (long) index * ELEM_SIZE;
    }

    public static long slotAddrByName(String name) {
        return find(name);
    }


    public static InstanceKlass klassAt(int index) {
        long a = slotAddr(index);
        if (a == 0) return null;
        long k = UnsafeUtils.unsafe.getLong(a);
        return k == 0 ? null : new InstanceKlass(k);
    }

    public static long rawKlassAt(int index) {
        long a = slotAddr(index);
        return a == 0 ? 0 : UnsafeUtils.unsafe.getLong(a);
    }

    public static void clearSlot(int index) {
        long a = slotAddr(index);
        if (a == 0) return;
        long old = UnsafeUtils.unsafe.getLong(a);
        UnsafeUtils.unsafe.putLong(a, 0L);
        System.out.printf("[VmClasses] clear slot %d @ 0x%x (was 0x%x)%n",
                index, a, old);
    }

    public static boolean clearByName(String name) {
        long a = find(name);
        if (a == 0) {
            System.err.printf("[VmClasses] 没有注册 %s 的槽位%n", name);
            return false;
        }
        long old = UnsafeUtils.unsafe.getLong(a);
        UnsafeUtils.unsafe.putLong(a, 0L);
        System.out.printf("[VmClasses] cleared %s slot=%d @ 0x%x (was 0x%x)%n",
                name, slotIndex(a), a, old);
        return true;
    }


    public static void dumpRegistrations() {
        System.out.printf("[VmClasses] BASE = 0x%x%n", BASE);
        System.out.printf("[VmClasses] 8 个已注册槽位:%n");
        dumpSlot("Object",       SLOT_OBJECT);
        dumpSlot("String",       SLOT_STRING);
        dumpSlot("Class",        SLOT_CLASS);
        dumpSlot("ClassLoader",  SLOT_CLASSLOADER);
        dumpSlot("System",       SLOT_SYSTEM);
        dumpSlot("Thread",       SLOT_THREAD);
        dumpSlot("ThreadGroup",  SLOT_THREADGROUP);
        dumpSlot("MethodHandle", SLOT_METHODHANDLE);
    }

    private static void dumpSlot(String name, long addr) {
        if (addr == 0) {
            System.out.printf("[VmClasses]   %-13s <未注册>%n", name);
            return;
        }
        int idx = slotIndex(addr);
        long k = UnsafeUtils.unsafe.getLong(addr);
        String kn = "?";
        if (k != 0) {
            try { kn = new InstanceKlass(k).getExternalName(); }
            catch (Throwable ignored) { }
        }
        System.out.printf("[VmClasses]   %-13s slot=%-3d @ 0x%x  ->  0x%x  %s%n",
                name, idx, addr, k, kn);
    }

    public static void dumpAll() {
        if (BASE == 0) {
            System.err.println("[VmClasses] BASE==0, 无法遍历");
            return;
        }
        System.out.printf("[VmClasses] 数组遍历，BASE=0x%x%n", BASE);
        int nonEmpty = 0;
        for (int i = 0; i < MAX_SLOTS; i++) {
            long k = rawKlassAt(i);
            if (k == 0) continue;
            if ((k & 7L) != 0 || k < 0x10000L) {
                System.out.printf("[VmClasses]   [%3d] 0x%016x  非法指针, 停止遍历%n", i, k);
                break;
            }
            String n;
            try { n = new InstanceKlass(k).getExternalName(); }
            catch (Throwable t) { n = "<bad>"; }
            System.out.printf("[VmClasses]   [%3d] 0x%x  %s%n", i, k, n);
            nonEmpty++;
        }
        System.out.printf("[VmClasses] 共 %d 个非空槽位%n", nonEmpty);
    }

    @Override
    public String toString() {
        return String.format("VmClasses[BASE=0x%x]", BASE);
    }
}