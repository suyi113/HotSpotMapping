package net.hotspot.cplus.jdk21.core;

import net.hotspot.cplus.jdk21.classloading.ClassLoaderData;
import net.hotspot.cplus.jdk21.codecache.CodeBuilder;
import net.hotspot.cplus.jdk21.klass.Klass;

import net.hotspot.cplus.jdk21.threads.JavaThread;
import net.hotspot.cplus.jdk21.core.OopDesc;
import net.hotspot.cplus.jdk21.threads.ThreadsList;
import net.hotspot.cplus.jdk21.threads.ThreadsSMRSupport;
import net.hotspot.cplus.jdk21.core.VMStructsHelper;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnsafeUtils {

    public static Unsafe unsafe;

    private static final Object[] HOLDER = new Object[1];

    static {
        try {
            Field usf = Unsafe.class.getDeclaredField("theUnsafe");
            usf.setAccessible(true);
            unsafe = (Unsafe) usf.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private UnsafeUtils() {
    }

    public static long getKlassLong(Class<?> clazz) {
        return unsafe.getLong(clazz, 16);
    }

    public static void registerJvmDll() {
        String javaHome = System.getProperty("java.home");
        String jvmDll = javaHome + "\\bin\\server\\jvm.dll";
        java.io.File dll = new java.io.File(jvmDll);
        if (!dll.exists()) {
            System.err.println("[HotSpotMapping] jvm.dll not found: " + jvmDll);
            return;
        }
        try {
            System.load(jvmDll);
            System.out.println("[HotSpotMapping] jvm.dll loaded explicitly");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[HotSpotMapping] jvm.dll already loaded (expected), skipping. reason=" + e.getMessage());
        } catch (Throwable t) {
            System.err.println("[HotSpotMapping] registerJvmDll failed:");
            t.printStackTrace();
        }
    }

    public static long staticLongValue(String typeAndField, long fallback) {
        try {
            long addr = VMStructsHelper.findStaticAddress(typeAndField);
            if (addr <= 0) return fallback;
            return unsafe.getLong(addr);
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static int staticIntValue(String typeAndField, int fallback) {
        try {
            long addr = VMStructsHelper.findStaticAddress(typeAndField);
            if (addr <= 0) return fallback;
            return unsafe.getInt(addr);
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static Object oopToJavaObject(long oop) {
        if (oop == 0) return null;

        long holderOop = OopDesc.addressOf(HOLDER);
        long elemAddr = holderOop + unsafe.arrayBaseOffset(Object[].class);

        if (OopDesc.USE_COMPRESSED_OOPS) {
            int narrow = (int) ((oop - OopDesc.HEAP_BASE) >>> OopDesc.HEAP_SHIFT);
            unsafe.putInt(elemAddr, narrow);
        } else {
            unsafe.putLong(elemAddr, oop);
        }

        Object result = HOLDER[0];
        HOLDER[0] = null;
        return result;
    }

    public static List<Class<?>> getAllLoadedClass() {
        long cldHeadAddr = VMStructsHelper.findStaticAddress("ClassLoaderDataGraph::_head");
        long cldPtr = unsafe.getLong(cldHeadAddr);

        long cldKlassesOff = VMStructsHelper.findOffset("ClassLoaderData::_klasses");
        long cldNextOff    = VMStructsHelper.findOffset("ClassLoaderData::_next");
        long kNextLinkOff  = VMStructsHelper.findOffset("Klass::_next_link");
        long kMirrorOff    = VMStructsHelper.findOffset("Klass::_java_mirror");

        List<Class<?>> out = new ArrayList<>();
        Set<Long> seenCld = new HashSet<>();

        while (cldPtr != 0) {
            if (!seenCld.add(cldPtr)) break;

            long klassPtr = unsafe.getLong(cldPtr + cldKlassesOff);
            Set<Long> localChain = new HashSet<>();
            while (klassPtr != 0) {
                if (!localChain.add(klassPtr)) break;

                long oopHandleAddr = unsafe.getLong(klassPtr + kMirrorOff);
                if (oopHandleAddr != 0) {
                    long mirrorOop = unsafe.getLong(oopHandleAddr);
                    if (mirrorOop != 0) {
                        Object o = oopToJavaObject(mirrorOop);
                        if (o instanceof Class<?> c) {
                            out.add(c);
                        }
                    }
                }
                klassPtr = unsafe.getLong(klassPtr + kNextLinkOff);
            }
            cldPtr = unsafe.getLong(cldPtr + cldNextOff);
        }
        return out;
    }

    public static List<Thread> getAllThreads() {
        List<Thread> out = new ArrayList<>();
        long listAddr = ThreadsSMRSupport.javaThreadList();
        if (listAddr == 0) return out;

        ThreadsList list = new ThreadsList(listAddr);
        for (int i = 0; i < list.length; i++) {
            JavaThread jt = list.threadAt(i);
            if (jt == null) continue;
            Object o = jt.getThreadObject();
            if (o instanceof Thread t) {
                out.add(t);
            }
        }
        return out;
    }

    public static void setAccessible(Field field, boolean flag) {
        try {
            unsafe.putBoolean(field, 12, flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAccessible(Constructor<?> constructor, boolean flag) {
        try {
            unsafe.putBoolean(constructor, 12, flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAccessible(Method method, boolean flag) {
        try {
            unsafe.putBoolean(method, 12, flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
