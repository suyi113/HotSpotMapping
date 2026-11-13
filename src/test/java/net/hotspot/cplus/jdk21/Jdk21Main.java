package net.hotspot.cplus.jdk21;

import net.hotspot.cplus.jdk21.classloading.ClassLoaderData;
import net.hotspot.cplus.jdk21.codecache.CodeBuilder;
import net.hotspot.cplus.jdk21.codecache.NMethod;
import net.hotspot.cplus.jdk21.core.CNative;
import net.hotspot.cplus.jdk21.core.OopDesc;
import net.hotspot.cplus.jdk21.core.Shellcode;
import net.hotspot.cplus.jdk21.core.VMStructs;
import net.hotspot.cplus.jdk21.core.VMStructsHelper;
import net.hotspot.cplus.jdk21.gc.G1CollectedHeap;
import net.hotspot.cplus.jdk21.gc.HeapRegion;
import net.hotspot.cplus.jdk21.gc.HeapRegionType;
import net.hotspot.cplus.jdk21.gc.HeapScanner;
import net.hotspot.cplus.jdk21.klass.ConstMethod;
import net.hotspot.cplus.jdk21.klass.ConstantPool;
import net.hotspot.cplus.jdk21.klass.FieldInfo;
import net.hotspot.cplus.jdk21.klass.InstanceKlass;
import net.hotspot.cplus.jdk21.klass.Klass;
import net.hotspot.cplus.jdk21.klass.Method;
import net.hotspot.cplus.jdk21.klass.Symbol;
import net.hotspot.cplus.jdk21.threads.JavaThread;
import net.hotspot.cplus.jdk21.threads.ThreadsList;
import net.hotspot.cplus.jdk21.threads.ThreadsSMRSupport;

import com.sun.management.HotSpotDiagnosticMXBean;
import net.hotspot.cplus.jdk21.core.UnsafeUtils;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Jdk21Main {

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        setupConsole();

        section("环境");
        environment();

        section("VMStructs");
        vmStructs();

        section("Klass 基本字段");
        klassBasics();

        section("Klass kind 分类");
        klassKinds();

        section("Symbol 名字解码");
        symbolDecoding();

        section("layout_helper 解析");
        layoutHelper();

        section("父类链");
        superChain();

        section("InstanceKlass / Method / ConstMethod");
        instanceKlassChecks();

        section("字段（fieldinfo stream）");
        fieldChecks();

        section("OopDesc");
        oopChecks();

        section("类加载（ClassLoaderData）");
        classLoaderChecks();

        section("堆扫描（G1）");
        heapChecks();

        section("Code cache（nmethod）");
        codeCacheChecks();

        section("线程（JavaThread / ThreadsList）");
        threadChecks();

        section("常量池扩展（CodeBuilder 前置）");
        cpExpansionChecks();

        section("字节码修改（真执行）");
        bytecodePatchChecks();

        section("反优化（编译后再替换）");
        deoptPatchChecks();

        section("UnsafeUtils 工具方法");
        utilsChecks();

        section("类还原（restoreClass）");
        restoreChecks();

        section("Shellcode 调用本地函数");
        shellcodeChecks();

        section("C 层字段寻址");
        cNativeChecks();

        section("扫描 VMStructs 缺失字段");
        scanMissingFieldChecks();

        section("修改 JDK 内部类（sun.misc.Unsafe）");
        unsafePatchChecks();

        section("异常路径");
        errorPaths();

        summary();
    }

    private static void environment() {
        System.out.println("  java.version = " + System.getProperty("java.version"));
        System.out.println("  java.vm.name = " + System.getProperty("java.vm.name"));
        System.out.println("  java.vm.info = " + System.getProperty("java.vm.info"));
        System.out.println("  file.encoding = " + System.getProperty("file.encoding")
                + ", native.encoding = " + System.getProperty("native.encoding")
                + ", stdout.encoding = " + System.getProperty("stdout.encoding"));
        System.out.println("  实际输出编码 = " + System.getProperty("jdk21.console.charset"));

        HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        System.out.println("  UseCompressedOops          = " + vmOption(bean, "UseCompressedOops"));
        System.out.println("  UseCompressedClassPointers = " + vmOption(bean, "UseCompressedClassPointers"));
        System.out.println("  Unsafe                     = " + (UnsafeUtils.unsafe != null));

        check("JDK 版本为 21.x", System.getProperty("java.version").startsWith("21"),
                System.getProperty("java.version"));
        check("Unsafe 可用", UnsafeUtils.unsafe != null);
    }

    private static void vmStructs() {
        int fields;
        int types;
        try {
            fields = VMStructsHelper.fieldCount();
            types = VMStructsHelper.typeCount();
        } catch (Throwable t) {
            check("VMStructs 可用", false, t.toString());
            return;
        }

        check("VMStructs 可用", fields > 0 && types > 0, "fields=" + fields + ", types=" + types);

        System.out.println("  Klass 类型大小 = " + VMStructsHelper.findSize("Klass") + " 字节");
        for (String name : new String[]{
                "Klass::_layout_helper", "Klass::_name", "Klass::_super",
                "Klass::_java_mirror", "Klass::_vtable_len", "Klass::_access_flags"}) {
            Long off = offsetOrNull(name);
            System.out.println("  " + name + " -> " + (off == null ? "未导出" : off));
        }
    }

    private static void klassBasics() {
        Klass string = new Klass(String.class);
        check("String: 地址非 0", string.getAddress() != 0);
        check("String: name 字段非 0", string.name() != 0);
        check("String: javaMirror 非 0", string.javaMirror() != 0);
        check("String: classLoaderData 非 0", string.classLoaderData() != 0);
        check("String: vtableLen > 0", string.vtableLength() > 0, "vtableLen=" + string.vtableLength());
        check("String: accessFlags 非 0", string.accessFlags() != 0,
                String.format("0x%x", string.accessFlags()));
        check("toString 形如 Klass[...]", string.toString().startsWith("Klass["));
        check("isKlass()", string.isKlass());

        Klass again = new Klass(string.getAddress());
        check("按地址重建后 name 一致", again.name() == string.name());
        check("按地址重建后 kind 一致", again.kind() == string.kind());
    }

    private static void klassKinds() {

        checkKlass(String.class, "java/lang/String", Klass.INSTANCE_KLASS_KIND, "java/lang/Object");
        checkKlass(Object.class, "java/lang/Object", Klass.INSTANCE_KLASS_KIND, null);
        checkKlass(Runnable.class, "java/lang/Runnable", Klass.INSTANCE_KLASS_KIND, "java/lang/Object");
        checkKlass(Thread.class, "java/lang/Thread", Klass.INSTANCE_KLASS_KIND, "java/lang/Object");
        checkKlass(Class.class, "java/lang/Class", Klass.INSTANCE_MIRROR_KLASS_KIND, "java/lang/Object");
        checkKlass(int[].class, "[I", Klass.TYPE_ARRAY_KLASS_KIND, "java/lang/Object");
        checkKlass(long[].class, "[J", Klass.TYPE_ARRAY_KLASS_KIND, "java/lang/Object");
        checkKlass(String[].class, "[Ljava/lang/String;", Klass.OBJ_ARRAY_KLASS_KIND, "[Ljava/lang/Object;");

        Klass self = new Klass(Jdk21Main.class);
        check("Jdk21Main: kind=InstanceKlass", self.kind() == Klass.INSTANCE_KLASS_KIND, self.kindName());
        check("Jdk21Main: name", "net/hotspot/cplus/jdk21/Jdk21Main".equals(self.getName()), self.getName());
        check("Jdk21Main: 非共享类", !self.isShared() || self.sharedClassPathIndex() >= 0,
                "shared=" + self.isShared() + ", pathIndex=" + self.sharedClassPathIndex());
        System.out.println("  " + self);
    }

    private static void checkKlass(Class<?> clazz, String expectedName, int expectedKind, String expectedSuper) {
        String label = clazz.getSimpleName().isEmpty() ? clazz.getName() : clazz.getSimpleName();
        Klass k = new Klass(clazz);

        check(label + ": kind=" + expectedKind, k.kind() == expectedKind, k.kindName());
        check(label + ": name=" + expectedName, expectedName.equals(k.getName()), k.getName());
        check(label + ": 地址=0x" + Long.toHexString(k.getAddress()), k.getAddress() != 0);

        boolean instance = expectedKind <= Klass.INSTANCE_STACK_CHUNK_KLASS_KIND;
        check(label + ": layout_helper 与 kind 一致",
                k.layoutHelperIsInstance() == instance,
                "kind=" + k.kindName() + ", layoutHelper=" + k.layoutHelper());

        if (expectedSuper == null) {
            check(label + ": 无父类", k.superKlass() == 0, String.valueOf(k.superKlass()));
        } else {
            check(label + ": 父类=" + expectedSuper, expectedSuper.equals(superName(k)), superName(k));
        }

        check(label + ": vtableLen > 0", k.vtableLength() > 0, "vtableLen=" + k.vtableLength());
    }

    private static String superName(Klass k) {
        long sup = k.superKlass();
        return sup == 0 ? null : new Klass(sup).getName();
    }

    private static void symbolDecoding() {
        Klass string = new Klass(String.class);
        Symbol symbol = new Symbol(string.name());

        check("Symbol.value", "java/lang/String".equals(symbol.value), String.valueOf(symbol.value));
        check("Symbol.refcount > 0", symbol.refcount() > 0, "refcount=" + symbol.refcount());
        check("Symbol.utf8Length = 16", symbol.utf8Length() == 16, "length=" + symbol.utf8Length());
        check("Symbol.toString 含 value", symbol.toString().contains("java/lang/String"));

        check("Klass.getExternalName", "java.lang.String".equals(string.getExternalName()),
                string.getExternalName());
    }

    private static void layoutHelper() {
        Klass intArray = new Klass(int[].class);
        check("int[]: isTypeArray", intArray.layoutHelperIsTypeArray());
        check("int[]: !isObjArray", !intArray.layoutHelperIsObjArray());
        check("int[]: headerSize=16", intArray.layoutHelperHeaderSize() == 16,
                "headerSize=" + intArray.layoutHelperHeaderSize());
        check("int[]: elementType=T_INT(10)", intArray.layoutHelperElementType() == 10,
                "elementType=" + intArray.layoutHelperElementType());
        check("int[]: log2ElementSize=2", intArray.layoutHelperLog2ElementSize() == 2,
                "log2=" + intArray.layoutHelperLog2ElementSize());

        Klass longArray = new Klass(long[].class);
        check("long[]: elementType=T_LONG(11)", longArray.layoutHelperElementType() == 11,
                "elementType=" + longArray.layoutHelperElementType());
        check("long[]: log2ElementSize=3", longArray.layoutHelperLog2ElementSize() == 3,
                "log2=" + longArray.layoutHelperLog2ElementSize());

        Klass byteArray = new Klass(byte[].class);
        check("byte[]: elementType=T_BYTE(8)", byteArray.layoutHelperElementType() == 8,
                "elementType=" + byteArray.layoutHelperElementType());
        check("byte[]: log2ElementSize=0", byteArray.layoutHelperLog2ElementSize() == 0,
                "log2=" + byteArray.layoutHelperLog2ElementSize());

        Klass objArray = new Klass(String[].class);
        check("String[]: isObjArray", objArray.layoutHelperIsObjArray());
        check("String[]: elementType=T_OBJECT(12)", objArray.layoutHelperElementType() == 12,
                "elementType=" + objArray.layoutHelperElementType());

        int expectedOopLog2 = Boolean.parseBoolean(vmOption(
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class), "UseCompressedOops"))
                ? 2 : 3;
        check("String[]: log2ElementSize=" + expectedOopLog2,
                objArray.layoutHelperLog2ElementSize() == expectedOopLog2,
                "log2=" + objArray.layoutHelperLog2ElementSize());

        Klass string = new Klass(String.class);
        check("String: 实例 layout_helper > 0", string.layoutHelperIsInstance());
        check("String: layoutHelperSizeInBytes == layoutHelper（无 slow path）",
                string.layoutHelperNeedsSlowPath()
                        || string.layoutHelperSizeInBytes() == string.layoutHelper(),
                "layoutHelper=" + string.layoutHelper()
                        + ", sizeInBytes=" + string.layoutHelperSizeInBytes());

        check("instanceLayoutHelper(24,false) = 0xc0",
                Klass.instanceLayoutHelper(24, false) == 0xc0,
                Integer.toHexString(Klass.instanceLayoutHelper(24, false)));
    }

    private static void superChain() {
        List<String> chain = new ArrayList<>();
        Klass k = new Klass(Jdk21Main.class);
        while (k != null && chain.size() < 64) {
            String name = k.getName();
            chain.add(name);
            long sup = k.superKlass();
            k = sup == 0 ? null : new Klass(sup);
        }

        check("父类链以 java/lang/Object 结束",
                !chain.isEmpty() && "java/lang/Object".equals(chain.get(chain.size() - 1)),
                String.join(" -> ", chain));
        check("父类链无环", chain.size() < 64, "depth=" + chain.size());

        Klass string = new Klass(String.class);
        int depth = string.superDepth();
        check("String: superDepth=1", depth == 1, "depth=" + depth);
        check("String: primary_supers[depth] == String",
                depth < Klass.PRIMARY_SUPER_LIMIT
                        && new Klass(string.primarySuperOfDepth(depth)).getName().equals("java/lang/String"),
                "depth=" + depth);
        check("String: canBePrimarySuper", string.canBePrimarySuper());
    }

    private static void instanceKlassChecks() {
        InstanceKlass ik = new InstanceKlass(String.class);
        check("String: InstanceKlass 构造", ik.getAddress() != 0);
        check("String: 名字", "java/lang/String".equals(ik.getName()), ik.getName());
        check("String: initState=fully_initialized",
                ik.initState() == InstanceKlass.INIT_STATE_FULLY_INITIALIZED, "state=" + ik.initState());
        check("String: constants 非 0", ik.constants() != 0);
        check("String: thisClassIndex > 0", (ik.thisClassIndex() & 0xFFFF) > 0,
                "index=" + (ik.thisClassIndex() & 0xFFFF));
        check("String: fieldinfoStream 非 0", ik.fieldinfoStream() != 0);
        check("String: 未被重定义", !ik.hasBeenRedefined());

        Set<String> expected = reflectionSignatures(String.class);
        Set<String> actual = new HashSet<>();
        for (Method m : ik.getMethods()) {
            String name = m.name();
            String sig = m.signature();
            if (name != null && sig != null) {
                actual.add(name + sig);
            }
        }
        check("String: 反射的每个方法/构造器都能找到", actual.containsAll(expected),
                "反射=" + expected.size() + ", 内存=" + actual.size());
        check("String: 多出的只有 <clinit>", actual.size() - expected.size() <= 1,
                "差 " + (actual.size() - expected.size()));

        Method length = ik.findMethod("length", "()I");
        check("String.length() 找到", length != null);
        if (length != null) {
            ConstMethod cm = new ConstMethod(length.constMethod);
            check("String.length: 常量池非 0", cm.constants != 0);
            check("String.length: codeSize > 0", (cm.codeSize & 0xFFFF) > 0,
                    "codeSize=" + (cm.codeSize & 0xFFFF));
            check("String.length: 容量足够",
                    cm.constMethodSizeInBytes() >= ConstMethod.CONST_METHOD_HEADER_SIZE + (cm.codeSize & 0xFFFF),
                    "size=" + cm.constMethodSizeInBytes() + "B");
            check("String.length: 非 native/abstract", !length.isNative() && !length.isAbstract());
        }

        Method charAt = ik.findMethod("charAt", "(I)C");
        check("String.charAt(I)C 找到", charAt != null);

        InstanceKlass self = new InstanceKlass(Jdk21Main.class);
        Method main = self.findMethod("main", "([Ljava/lang/String;)V");
        check("Jdk21Main.main 找到", main != null);
        if (main != null) {
            ConstMethod cm = new ConstMethod(main.constMethod);
            byte[] code = cm.code();
            check("Jdk21Main.main 字节码非空", code.length > 0, "length=" + code.length);
            check("Jdk21Main.main 首字节是合法 opcode", code.length > 0 && isKnownOpcode(code[0]),
                    code.length == 0 ? "空" : String.format("0x%02x", code[0] & 0xFF));
        }

        System.out.println("  " + ik);
    }

    private static boolean isKnownOpcode(byte b) {
        int op = b & 0xFF;
        return op >= 0x00 && op <= 0xC9;
    }

    private static Set<String> reflectionSignatures(Class<?> clazz) {
        Set<String> out = new HashSet<>();
        for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
            out.add(m.getName() + MethodType.methodType(m.getReturnType(), m.getParameterTypes())
                    .toMethodDescriptorString());
        }
        for (java.lang.reflect.Constructor<?> c : clazz.getDeclaredConstructors()) {
            out.add("<init>" + MethodType.methodType(void.class, c.getParameterTypes())
                    .toMethodDescriptorString());
        }
        return out;
    }

    private static void fieldChecks() {
        InstanceKlass ik = new InstanceKlass(Sample.class);
        check("Sample: 字段数 = 5", ik.fieldCount() == 5, "count=" + ik.fieldCount());
        check("Sample: 无注入字段", ik.numInjectedFields() == 0, "injected=" + ik.numInjectedFields());

        ConstantPool cp = ik.getConstantPool();
        Set<String> expected = new HashSet<>();
        for (java.lang.reflect.Field f : Sample.class.getDeclaredFields()) {
            expected.add(f.getName() + ":" + typeDescriptor(f.getType())
                    + ":" + java.lang.reflect.Modifier.isStatic(f.getModifiers()));
        }
        Set<String> actual = new HashSet<>();
        for (FieldInfo fi : ik.getFields()) {
            String n = fi.name(cp);
            String s = fi.signature(cp);
            if (n != null && s != null) {
                actual.add(n + ":" + s + ":" + fi.isStatic());
            }
        }
        check("Sample: 字段名/描述符/静态标记与反射一致", actual.equals(expected),
                "内存=" + actual + ", 反射=" + expected);

        FieldInfo intField = ik.findField("intField", "I");
        check("Sample.intField 找到", intField != null);
        if (intField != null) {
            check("Sample.intField 偏移已布局", intField.isOffsetSet(), "offset=" + intField.offset);
            check("Sample.intField 非静态", !intField.isStatic());
            check("Sample.intField 非 final", !intField.isFinal());
        }
        FieldInfo staticInt = ik.findField("staticInt", "I");
        check("Sample.staticInt 找到且是静态", staticInt != null && staticInt.isStatic());

        FieldInfo stringField = ik.findField("stringField", "Ljava/lang/String;");
        check("Sample.stringField 描述符正确", stringField != null);

        InstanceKlass cls = new InstanceKlass(Class.class);
        check("java.lang.Class: 有 JVM 注入字段", cls.numInjectedFields() > 0,
                "java=" + cls.numJavaFields() + ", injected=" + cls.numInjectedFields());

        System.out.println("  Sample fields = " + actual);
        System.out.println("  java.lang.Class: fields=" + cls.fieldCount()
                + " (java=" + cls.numJavaFields() + ", injected=" + cls.numInjectedFields() + ")");
    }

    private static String typeDescriptor(Class<?> type) {

        return MethodType.methodType(type).toMethodDescriptorString().substring(2);
    }

    private static class Sample {
        int intField;
        long longField;
        String stringField;
        static int staticInt;
        static String staticString;
    }

    private static void oopChecks() {
        Klass selfKlass = new Klass(Jdk21Main.class);
        long handle = selfKlass.javaMirror();
        check("Jdk21Main: javaMirror 句柄非 0", handle != 0);
        if (handle == 0) return;

        long mirrorOop = UnsafeUtils.unsafe.getLong(handle);
        check("Jdk21Main: 镜像 oop 非 0", mirrorOop != 0, "0x" + Long.toHexString(mirrorOop));
        if (mirrorOop == 0) return;

        OopDesc oop = OopDesc.of(mirrorOop);
        check("OopDesc: mark 非 0", oop.mark != 0, String.format("0x%x", oop.mark));
        check("OopDesc: 未锁定（mark 低位 = 01）", oop.isUnlocked(), String.format("mark=0x%x", oop.mark));
        check("OopDesc: klass 指向 java.lang.Class 的 Klass",
                oop.klass == new Klass(Class.class).getAddress(),
                String.format("0x%x vs 0x%x", oop.klass, new Klass(Class.class).getAddress()));
        check("OopDesc.getInstanceKlass 名字", "java/lang/Class".equals(oop.getInstanceKlass().getName()),
                oop.getInstanceKlass().getName());
        check("OopDesc.fieldBase = address + 12（压缩类指针）",
                oop.fieldBase() == oop.getAddress() + 12, "fieldBase=" + oop.fieldBase());

        Object probe = new Object();
        long probeAddr = OopDesc.addressOf(probe);
        OopDesc probeOop = OopDesc.of(probeAddr);
        check("addressOf/of 往返：klass 正确",
                probeOop.klass == new Klass(probe.getClass()).getAddress(),
                String.format("0x%x", probeOop.klass));

        Object back = UnsafeUtils.oopToJavaObject(mirrorOop);
        check("oopToJavaObject 还原成 Jdk21Main.class", back == Jdk21Main.class, String.valueOf(back));

        System.out.println("  " + oop);
    }

    private static void classLoaderChecks() {
        List<Class<?>> loaded = UnsafeUtils.getAllLoadedClass();
        check("已加载类数量 > 100", loaded.size() > 100, "count=" + loaded.size());
        check("包含 java.lang.String", loaded.contains(String.class));
        check("包含 Jdk21Main", loaded.contains(Jdk21Main.class));

        long head = VMStructsHelper.findStaticAddress("ClassLoaderDataGraph::_head");
        check("ClassLoaderDataGraph::_head 可解析", head > 0);

        List<ClassLoaderData> clds = new ArrayList<>();
        long ptr = UnsafeUtils.unsafe.getLong(head);
        Set<Long> seen = new HashSet<>();
        while (ptr != 0 && seen.add(ptr) && clds.size() < 500) {
            ClassLoaderData cld = new ClassLoaderData(ptr);
            clds.add(cld);
            ptr = cld.next;
        }
        check("ClassLoaderData 数量 > 0", clds.size() > 0, "count=" + clds.size());

        int totalKlasses = 0;
        ClassLoaderData bootstrap = null;
        for (ClassLoaderData cld : clds) {
            List<InstanceKlass> ks = cld.allKlasses();
            totalKlasses += ks.size();
            for (InstanceKlass k : ks) {
                if ("java/lang/Object".equals(k.getName())) {
                    bootstrap = cld;
                }
            }
        }
        check("遍历到的 Klass 总数 > 100", totalKlasses > 100, "count=" + totalKlasses);
        check("找到包含 java/lang/Object 的 ClassLoaderData", bootstrap != null);
        if (bootstrap != null) {
            List<InstanceKlass> ks = bootstrap.allKlasses();
            check("该 CLD 里有 java/lang/String",
                    ks.stream().anyMatch(k -> "java/lang/String".equals(k.getName())),
                    "klasses=" + ks.size());
            System.out.println("  " + bootstrap);
        }

        ClassLoader appLoader = Jdk21Main.class.getClassLoader();
        boolean foundApp = false;
        for (ClassLoaderData cld : clds) {
            if (cld.classLoaderJavaObject() == appLoader) {
                foundApp = cld.allKlasses().stream()
                        .anyMatch(k -> "net/hotspot/cplus/jdk21/Jdk21Main".equals(k.getName()));
            }
        }
        check("应用类加载器的 CLD 里有 Jdk21Main", foundApp);

        System.out.println("  ClassLoaderData=" + clds.size() + " 个, Klass 总数=" + totalKlasses);
    }

    private static void heapChecks() {
        G1CollectedHeap g1 = G1CollectedHeap.current();
        check("G1CollectedHeap 地址非 0", g1.getAddress() != 0);

        List<HeapRegion> regions = g1.hrm().allRegions();
        check("HeapRegion 数量 > 0", regions.size() > 0, "regions=" + regions.size());

        int free = 0, young = 0, old = 0, humongous = 0, other = 0;
        long used = 0;
        for (HeapRegion r : regions) {
            HeapRegionType t = r.type();
            if (t.isFree()) free++;
            else if (t.isYoung()) young++;
            else if (t.isOld()) old++;
            else if (t.isHumongous()) humongous++;
            else other++;
            used += r.used();
        }
        check("存在非空闲 region", regions.size() - free > 0, "free=" + free);
        check("已用字节 > 0", used > 0, "used=" + used + "B");

        Set<Long> klasses = HeapScanner.collectAllKlassAddrs();
        check("HeapScanner: 收集到的 Klass > 100", klasses.size() > 100, "count=" + klasses.size());

        System.out.printf("  regions: total=%d free=%d young=%d old=%d humongous=%d other=%d used=%dMB%n",
                regions.size(), free, young, old, humongous, other, used / (1024 * 1024));
        System.out.println("  " + g1);
    }

    private static void codeCacheChecks() {

        long acc = 0;
        for (int i = 0; i < 300_000; i++) {
            acc += hot(i);
        }
        check("hot() 被执行（防优化）", acc != 0, "acc=" + acc);

        InstanceKlass ik = new InstanceKlass(Jdk21Main.class);
        Method hot = ik.findMethod("hot", "(I)I");
        check("找到 hot(I)I", hot != null);
        if (hot == null) return;

        check("hot() 已被 JIT 编译", hot.code != 0,
                "code=0x" + Long.toHexString(hot.code));
        if (hot.code == 0) return;

        NMethod nm = new NMethod(hot.code);
        check("NMethod 地址 == Method.code", nm.getAddress() == hot.code);
        check("isNmethod()", nm.isNmethod());
        check("state 合法", nm.state >= NMethod.IN_USE && nm.state <= NMethod.ZOMBIE, nm.stateName());
        check("state 为 in_use", nm.isInUse(), nm.stateName());
        check("compLevel 在 0..4", nm.compLevel >= 0 && nm.compLevel <= 4, "compLevel=" + nm.compLevel);
        check("size > 0", nm.size > 0, "size=" + nm.size);
        check("codeBegin < codeEnd", nm.codeBegin != 0 && nm.codeBegin < nm.codeEnd,
                String.format("0x%x..0x%x", nm.codeBegin, nm.codeEnd));
        check("codeSize 与区间一致", nm.codeSize() == (int) (nm.codeEnd - nm.codeBegin),
                "codeSize=" + nm.codeSize());
        check("entryPoint 落在 code 区间内",
                nm.entryPoint >= nm.codeBegin && nm.entryPoint <= nm.codeEnd,
                String.format("entry=0x%x, code=0x%x..0x%x", nm.entryPoint, nm.codeBegin, nm.codeEnd));
        check("verifiedEntryPoint 落在 code 区间内",
                nm.verifiedEntryPoint >= nm.codeBegin && nm.verifiedEntryPoint <= nm.codeEnd,
                String.format("0x%x", nm.verifiedEntryPoint));
        check("nmethod 关联的 Method 是 hot",
                nm.getMethod() != null && "hot".equals(nm.getMethod().name()),
                nm.getMethod() == null ? "null" : nm.getMethod().name());
        check("frameSize >= 0", nm.frameSize >= 0, "frameSize=" + nm.frameSize);

        System.out.println("  " + nm);
    }

    private static int hot(int x) {
        int s = 0;
        for (int i = 0; i < 8; i++) {
            s += (x * i) ^ (x >>> 3);
        }
        return s + Integer.bitCount(x);
    }

    private static void threadChecks() {
        long listAddr = ThreadsSMRSupport.javaThreadList();
        check("javaThreadList 非 0", listAddr != 0);
        if (listAddr == 0) return;

        ThreadsList list = new ThreadsList(listAddr);
        check("ThreadsList.length > 0", list.length > 0, "count=" + list.length);

        List<Thread> threads = UnsafeUtils.getAllThreads();
        check("getAllThreads 非空", !threads.isEmpty(), "count=" + threads.size());
        check("包含当前线程", threads.contains(Thread.currentThread()),
                "threads=" + threads.stream().map(Thread::getName).toList());

        int nonNull = 0;
        for (int i = 0; i < list.length; i++) {
            if (list.threadAt(i) != null) nonNull++;
        }
        check("threadAt 全部可读", nonNull == list.length, "read=" + nonNull + "/" + list.length);

        System.out.println("  " + list + " -> " + threads.stream().map(Thread::getName).toList());
    }

    private static void cpExpansionChecks() {
        Class<?> scratch = defineScratchClass();
        check("替身类已定义（独立 ClassLoader）", scratch != null && scratch != Jdk21Main.class);
        if (scratch == null) return;

        InstanceKlass ik = new InstanceKlass(scratch);
        check("替身类名字", "net/hotspot/cplus/jdk21/Jdk21Main".equals(ik.getName()), ik.getName());

        int oldLen = ik.getConstantPool().length;
        check("原常量池长度 > 0", oldLen > 0, "len=" + oldLen);

        int utf8Idx = ik.addUtf8("java/util/ArrayList", java.util.ArrayList.class);

        check("addUtf8 返回有效下标", utf8Idx > 0, "idx=" + utf8Idx + ", oldLen=" + oldLen);

        ConstantPool expanded = new ConstantPool(ik.currentCpAddress());
        long sym = expanded.symbolAt(utf8Idx);
        check("新 Utf8 可解析", sym != 0 && "java/util/ArrayList".equals(new Symbol(sym).value),
                sym == 0 ? "sym=0" : new Symbol(sym).value);

        int classIdx = ik.addClass("java/util/ArrayList", java.util.ArrayList.class);
        check("addClass 返回更新下标", classIdx > utf8Idx, "idx=" + classIdx);

        long oldCpAddr = ik.constants;
        ik.commitCpChanges();
        check("commit 后 constants 指向扩展池", ik.constants != oldCpAddr,
                String.format("0x%x -> 0x%x", oldCpAddr, ik.constants));
        check("commit 后常量池变长", ik.getConstantPool().length > oldLen,
                oldLen + " -> " + ik.getConstantPool().length);

        ConstantPool after = new ConstantPool(ik.constants);
        long symAfter = after.symbolAt(utf8Idx);
        check("commit 后新 Utf8 仍可解析",
                symAfter != 0 && "java/util/ArrayList".equals(new Symbol(symAfter).value));

        CodeBuilder cb = new CodeBuilder();
        cb.iconst(1);
        cb.ireturn();
        byte[] code = cb.build(ik);
        check("CodeBuilder 生成字节码", code.length > 0, "len=" + code.length);
        check("CodeBuilder 产物为 iconst_1;ireturn", code.length == 2
                && code[0] == 0x04 && code[1] == (byte) 0xAC,
                String.format("%02x %02x", code[0] & 0xFF, code.length > 1 ? code[1] & 0xFF : 0));
    }

    private static Class<?> defineScratchClass() {
        try (java.io.InputStream in = Jdk21Main.class.getResourceAsStream("Jdk21Main.class")) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            ClassLoader loader = new ClassLoader(Jdk21Main.class.getClassLoader()) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (Jdk21Main.class.getName().equals(name)) {
                        Class<?> c = findLoadedClass(name);
                        if (c == null) {
                            c = defineClass(name, bytes, 0, bytes.length);
                        }
                        if (resolve) resolveClass(c);
                        return c;
                    }
                    return super.loadClass(name, resolve);
                }
            };
            return loader.loadClass(Jdk21Main.class.getName());
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static void bytecodePatchChecks() {
        check("修改前 target(10) == 11", PatchTarget.target(10) == 11, "actual=" + PatchTarget.target(10));

        InstanceKlass ik = new InstanceKlass(PatchTarget.class);
        Method target = ik.findMethod("target", "(I)I");
        check("找到 target(I)I", target != null);
        if (target == null) return;
        check("target 尚未编译", target.code == 0, "code=0x" + Long.toHexString(target.code));

        int oldLen = ik.getConstantPool().length;
        check("替换前常量池不含 ExternalHelper",
                !cpHasClass(ik.getConstantPool(), "net/hotspot/cplus/jdk21/ExternalHelper"));
        CodeBuilder cb = new CodeBuilder();
        cb.iload(0);
        cb.invokestatic(ik, ExternalHelper.class, "twice", "(I)I");
        cb.ireturn();
        byte[] code = cb.build(ik);
        check("生成 iload_0/invokestatic/ireturn", code.length == 5,
                "len=" + code.length + ", bytes=" + hex(code));
        check("扩展后常量池含 ExternalHelper",
                cpHasClass(new ConstantPool(ik.currentCpAddress()),
                        "net/hotspot/cplus/jdk21/ExternalHelper"));

        ConstMethod cm = new ConstMethod(target.constMethod);
        int capacity = (int) (cm.constMethodSizeInBytes() - ConstMethod.CONST_METHOD_HEADER_SIZE);
        check("新代码放得下（按 ConstMethod 容量）", code.length <= capacity,
                "new=" + code.length + ", capacity=" + capacity);

        cm.putCodeAndResize(code);
        ik.commitCpChanges();
        check("commit 后常量池变长", ik.getConstantPool().length > oldLen,
                oldLen + " -> " + ik.getConstantPool().length);

        ConstantPool afterCp = new ConstantPool(ik.constants);
        int cacheIdx = (code[2] & 0xFF) | ((code[3] & 0xFF) << 8);
        int cpIdx = afterCp.getCache().entryConstantPoolIndex(cacheIdx);
        int classIdx = (int) (afterCp.slotAt(cpIdx) & 0xFFFF);
        check("invokestatic 指向 ExternalHelper",
                "net/hotspot/cplus/jdk21/ExternalHelper".equals(cpClassName(afterCp, classIdx)),
                "cacheIdx=" + cacheIdx + ", cpIdx=" + cpIdx + ", " + cpClassName(afterCp, classIdx));

        int r = PatchTarget.target(10);
        check("修改后 target(10) == 20", r == 20, "actual=" + r);
        check("未改动的方法不受影响", PatchTarget.untouched(1) == 101,
                "actual=" + PatchTarget.untouched(1));

        // sipush 的操作数不是常量池索引，必须按大端写
        Method untouched = ik.findMethod("untouched", "(I)I");
        check("找到 untouched(I)I", untouched != null);
        if (untouched != null) {
            CodeBuilder cb2 = new CodeBuilder();
            cb2.sipush(1000);
            cb2.ireturn();
            byte[] code2 = cb2.build(ik);
            check("sipush 1000 编码为大端 11 03 e8 ac",
                    code2.length == 4 && code2[1] == 0x03 && code2[2] == (byte) 0xE8,
                    "bytes=" + hex(code2));

            ConstMethod cm2 = new ConstMethod(untouched.constMethod);
            cm2.putCodeAndResize(code2);
            check("修改后 untouched(0) == 1000", PatchTarget.untouched(0) == 1000,
                    "actual=" + PatchTarget.untouched(0));
        }

        // 超出 ConstMethod 容量的字节码必须被拒绝，而不是写穿内存
        CodeBuilder cb3 = new CodeBuilder();
        for (int i = 0; i < 400; i++) {
            cb3.nop();
        }
        byte[] big = cb3.build();
        try {
            new ConstMethod(target.constMethod).putCodeAndResize(big);
            check("超长字节码被拒绝", false, "没有抛异常");
        } catch (IllegalArgumentException e) {
            check("超长字节码被拒绝", true, e.getMessage());
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    private static String cpClassName(ConstantPool cp, int classIdx) {
        long slot = cp.slotAt(classIdx);
        int nameIdx = (int) ((slot >>> 16) & 0xFFFF);
        long sym = cp.symbolAt(nameIdx);
        return sym == 0 ? null : new Symbol(sym).value;
    }

    private static boolean cpHasClass(ConstantPool cp, String className) {
        for (int i = 1; i < cp.length; i++) {
            int tag = UnsafeUtils.unsafe.getByte(cp.tags + 4 + i) & 0xFF;
            if (tag == 7 && className.equals(cpClassName(cp, i))) {
                return true;
            }
            if (tag == 5 || tag == 6) i++;
        }
        return false;
    }

    private static void deoptPatchChecks() {
        long acc = 0;
        for (int i = 0; i < 300_000; i++) {
            acc += PatchTarget.compiled(i);
        }
        check("compiled() 已执行", acc != 0, "acc=" + acc);

        InstanceKlass ik = new InstanceKlass(PatchTarget.class);
        Method m = ik.findMethod("compiled", "(I)I");
        check("找到 compiled(I)I", m != null);
        if (m == null) return;

        check("compiled() 已被 JIT 编译", m.code != 0,
                "code=0x" + Long.toHexString(m.code));
        check("替换前 compiled(10) == 289", PatchTarget.compiled(10) == 289,
                "actual=" + PatchTarget.compiled(10));

        m.forceInterpreted();
        check("forceInterpreted 后 code == 0", m.code == 0,
                "code=0x" + Long.toHexString(m.code));

        CodeBuilder cb = new CodeBuilder();
        cb.iload(0);
        cb.invokestatic(ik, ExternalHelper.class, "twice", "(I)I");
        cb.ireturn();
        ConstMethod cm = new ConstMethod(m.constMethod);
        cm.putCodeAndResize(cb.build(ik));
        ik.commitCpChanges();
        m.setNotCompilable();
        check("setNotCompilable 生效", m.isNotCompilable() && m.isDontInline(),
                "flags=0x" + Integer.toHexString(m.flags));

        int r = PatchTarget.compiled(10);
        check("反优化 + 替换后 compiled(10) == 20", r == 20, "actual=" + r);

        long sum = 0;
        for (int i = 0; i < 300_000; i++) {
            sum += PatchTarget.compiled(i);
        }
        check("30 万次热调用不崩溃", sum != 0, "sum=" + sum);
        check("热调用后仍执行新字节码", PatchTarget.compiled(10) == 20,
                "actual=" + PatchTarget.compiled(10));
        check("方法没有被重新编译", m.code == 0, "code=0x" + Long.toHexString(m.code));

        System.out.println("  " + m);
    }

    private static void unsafePatchChecks() {
        Class<?> unsafeClass;
        try {
            unsafeClass = Class.forName("sun.misc.Unsafe");
        } catch (Throwable t) {
            check("加载 sun.misc.Unsafe", false, t.toString());
            return;
        }
        check("加载 sun.misc.Unsafe", true);

        boolean threw = false;
        try {
            sun.misc.Unsafe.getUnsafe();
        } catch (SecurityException e) {
            threw = true;
        }
        check("原 getUnsafe() 抛 SecurityException", threw);

        InstanceKlass ik = new InstanceKlass(unsafeClass);
        Method m = ik.findMethod("getUnsafe", "()Lsun/misc/Unsafe;");
        check("找到 Unsafe.getUnsafe", m != null);
        if (m == null) return;

        m.forceInterpreted();
        PatchPrint.printed = false;

        CodeBuilder cb = new CodeBuilder();
        cb.invokestatic(ik, PatchPrint.class, "print", "()V");
        cb.aconst_null();
        cb.areturn();
        byte[] code = cb.build(ik);
        check("生成 print()+aconst_null+areturn", code.length == 5, "len=" + code.length);

        new ConstMethod(m.constMethod).putCodeAndResize(code);
        ik.commitCpChanges();
        m.setNotCompilable();

        Object r = sun.misc.Unsafe.getUnsafe();
        check("patch 后 getUnsafe() 返回 null", r == null, String.valueOf(r));
        check("patch 后确实调用了 PatchPrint.print", PatchPrint.printed);

        try {
            long mem2 = UnsafeUtils.unsafe.allocateMemory(16);
            UnsafeUtils.unsafe.freeMemory(mem2);
            check("patch 后 Unsafe 其他方法仍可用", true);
        } catch (Throwable t) {
            check("patch 后 Unsafe 其他方法仍可用", false, t.toString());
        }
        check("Unsafe.getUnsafe 已被标记为不可编译", m.isNotCompilable() && m.isDontInline(),
                "flags=0x" + Integer.toHexString(m.flags));

        System.out.println("  " + m);
    }

    private static void utilsChecks() {
        String s = "abc";
        check("addressOf 非 0", UnsafeUtils.addressOf(s) != 0,
                "0x" + Long.toHexString(UnsafeUtils.addressOf(s)));
        check("klassAddressOf == Klass(String).address",
                UnsafeUtils.klassAddressOf(s) == new Klass(String.class).getAddress(),
                "0x" + Long.toHexString(UnsafeUtils.klassAddressOf(s)));
        check("instanceKlassOf 名字", "java/lang/String".equals(
                UnsafeUtils.instanceKlassOf(s).getName()), UnsafeUtils.classNameOf(s));
        check("classNameOf(int[])", "[I".equals(UnsafeUtils.classNameOf(new int[0])),
                UnsafeUtils.classNameOf(new int[0]));

        check("sizeOf(new Object()) == 16", UnsafeUtils.sizeOf(new Object()) == 16,
                "size=" + UnsafeUtils.sizeOf(new Object()));
        check("sizeOf(\"abc\") == 24", UnsafeUtils.sizeOf("abc") == 24,
                "size=" + UnsafeUtils.sizeOf("abc"));
        check("sizeOf(new int[4]) == 32", UnsafeUtils.sizeOf(new int[4]) == 32,
                "size=" + UnsafeUtils.sizeOf(new int[4]));

        long mem = UnsafeUtils.unsafe.allocateMemory(16);
        try {
            UnsafeUtils.write(mem, new byte[]{1, 2, 3, 4});
            byte[] back = UnsafeUtils.read(mem, 4);
            check("read/write 往返", back[0] == 1 && back[1] == 2
                    && back[2] == 3 && back[3] == 4, java.util.Arrays.toString(back));
        } finally {
            UnsafeUtils.unsafe.freeMemory(mem);
        }

        check("findLoadedClass(java/lang/String)", UnsafeUtils.findLoadedClass("java/lang/String") == String.class);
        check("getLoadedClasses(java/lang/Str) 包含 String",
                UnsafeUtils.getLoadedClasses("java/lang/Str").contains(String.class));

        check("vmOption(UseCompressedOops) 一致",
                String.valueOf(OopDesc.USE_COMPRESSED_OOPS).equals(UnsafeUtils.vmOption("UseCompressedOops")),
                UnsafeUtils.vmOption("UseCompressedOops"));
        check("heapUsed > 0", UnsafeUtils.heapUsed() > 0, "used=" + UnsafeUtils.heapUsed());
        check("heapCommitted > heapUsed", UnsafeUtils.heapCommitted() >= UnsafeUtils.heapUsed());
        check("metaspaceUsed > 0", UnsafeUtils.metaspaceUsed() > 0, "used=" + UnsafeUtils.metaspaceUsed());

        long cur = UnsafeUtils.currentThreadAddress();
        check("currentThreadAddress 非 0", cur != 0, "0x" + Long.toHexString(cur));
        check("threadAddress(currentThread) 一致", cur == UnsafeUtils.threadAddress(Thread.currentThread()));

        InstanceKlass ik = new InstanceKlass(PatchTarget.class);
        Method spare = ik.findMethod("spare", "(I)I");
        check("找到 spare(I)I", spare != null);
        if (spare != null) {
            check("spare 原行为", PatchTarget.spare(0) == 5, "actual=" + PatchTarget.spare(0));
            CodeBuilder cb = new CodeBuilder();
            cb.bipush(9);
            cb.ireturn();
            UnsafeUtils.patchMethod(ik, spare, cb.build(ik));
            check("patchMethod 后 spare(0) == 9", PatchTarget.spare(0) == 9,
                    "actual=" + PatchTarget.spare(0));
            check("patchMethod 后不可编译", spare.isNotCompilable() && spare.isDontInline());
        }
    }

    private static void restoreChecks() {
        check("还原前 target(10) == 20", PatchTarget.target(10) == 20,
                "actual=" + PatchTarget.target(10));

        int n = UnsafeUtils.restoreClass(PatchTarget.class);
        check("restoreClass 恢复的方法数 > 0", n > 0, "n=" + n);
        check("还原后 target(10) == 11", PatchTarget.target(10) == 11,
                "actual=" + PatchTarget.target(10));
        check("还原后 compiled(10) == 289", PatchTarget.compiled(10) == 289,
                "actual=" + PatchTarget.compiled(10));
        check("还原后 spare(0) == 5", PatchTarget.spare(0) == 5,
                "actual=" + PatchTarget.spare(0));

        InstanceKlass ik = new InstanceKlass(PatchTarget.class);
        check("还原后常量池不含 ExternalHelper",
                !cpHasClass(ik.getConstantPool(),
                        "net/hotspot/cplus/jdk21/ExternalHelper"),
                "cpLen=" + ik.getConstantPool().length);
    }

    private static void shellcodeChecks() {
        long pidFn = Shellcode.kernel32("GetCurrentProcessId");
        check("解析 kernel32!GetCurrentProcessId", pidFn != 0,
                "0x" + Long.toHexString(pidFn));

        long original = 0;
        for (int i = 0; i < 200000; i++) {
            original = ShellTarget.shell();
        }
        check("shell() 已被 JIT 编译",
                new InstanceKlass(ShellTarget.class).findMethod("shell", "()J").code != 0);
        check("shell() 原始返回值不是 pid", original != ProcessHandle.current().pid());

        InstanceKlass ik = new InstanceKlass(ShellTarget.class);
        Method m = ik.findMethod("shell", "()J");
        Shellcode.write(m, Shellcode.call(pidFn));

        long got = callShellOnce();
        long pid = ProcessHandle.current().pid();
        check("shellcode 调用 GetCurrentProcessId 得到 pid", got == pid,
                "got=" + got + ", pid=" + pid + ", original=" + original);

        catalog();
    }

    private static void catalog() {
        long ticks = runStub(Shellcode.kernel32("GetTickCount64"));
        check("kernel32!GetTickCount64 > 0", ticks > 0, "ticks=" + ticks);

        long tid = runStub(Shellcode.kernel32("GetCurrentThreadId"));
        check("kernel32!GetCurrentThreadId > 0", tid > 0, "tid=" + tid);

        long feature = runStub(Shellcode.kernel32("IsProcessorFeaturePresent"), 17);
        check("kernel32!IsProcessorFeaturePresent(17) 是 0/1",
                feature == 0 || feature == 1, "ret=" + feature);

        String[] jvmSymbols = {"JVM_GC", "JVM_ActiveProcessorCount", "JVM_CurrentTimeMillis",
                "JVM_TotalMemory", "JVM_FreeMemory", "JVM_MaxMemory"};
        for (String s : jvmSymbols) {
            long addr = Shellcode.symbol(s);
            check("解析 jvm!" + s, addr != 0, "0x" + Long.toHexString(addr));
        }
        String locked = Shellcode.exportOf(Shellcode.symbol("JVM_GC"));
        check("反查地址锁定 JVM_GC", "JVM_GC".equals(locked), String.valueOf(locked));
    }

    private static long runStub(long fn, long... args) {
        InstanceKlass ik = new InstanceKlass(ShellTarget.class);
        Method m = ik.findMethod("shell", "()J");
        byte[] code;
        if (args.length == 0) {
            code = Shellcode.call(fn);
        } else if (args.length == 1) {
            code = Shellcode.call(fn, args[0]);
        } else {
            code = Shellcode.call(fn, args[0], args[1]);
        }
        Shellcode.write(m, code);
        return callShellOnce();
    }

    private static long callShellOnce() {
        return ShellTarget.shell();
    }

    private static void cNativeChecks() {
        check("symbol(gHotSpotVMStructs) 非 0",
                CNative.symbol("gHotSpotVMStructs") != 0);
        check("symbol(JVM_GC) 非 0", CNative.symbol("JVM_GC") != 0);

        long thread = UnsafeUtils.currentThreadAddress();
        long anchorOff = CNative.fieldOffset("JavaThread::_anchor");
        check("JavaThread::_anchor 偏移 > 0", anchorOff > 0, "off=" + anchorOff);
        check("fieldAddress = base + offset",
                CNative.fieldAddress(thread, "JavaThread::_anchor") == thread + anchorOff);
        check("fieldInt(thread, _thread_state) 合理",
                CNative.fieldInt(thread, "JavaThread::_thread_state") >= 0,
                "state=" + CNative.fieldInt(thread, "JavaThread::_thread_state"));

        long mem = UnsafeUtils.unsafe.allocateMemory(64);
        try {
            UnsafeUtils.unsafe.putLong(mem + 24, 0x1122334455667788L);
            check("findFieldByValue 命中 +24",
                    CNative.findFieldByValue(mem, 0x1122334455667788L, 64) == 24);
            check("findPointer 命中",
                    CNative.findPointer(mem, mem + 64, 0x1122334455667788L) == mem + 24);

            long inner = UnsafeUtils.unsafe.allocateMemory(32);
            try {
                UnsafeUtils.unsafe.putLong(inner + 8, 0xABCDL);
                UnsafeUtils.unsafe.putLong(mem + 16, inner);
                check("deref 指针链 (16,8)",
                        CNative.deref(mem, 16, 8) == 0xABCDL);
                check("addressOf 指针链 (16,8)",
                        CNative.addressOf(mem, 16, 8) == inner + 8);
            } finally {
                UnsafeUtils.unsafe.freeMemory(inner);
            }
        } finally {
            UnsafeUtils.unsafe.freeMemory(mem);
        }
    }

    private static void scanMissingFieldChecks() {
        boolean missing = false;
        try {
            VMStructsHelper.findOffset("JavaThread::_jni_environment");
        } catch (Throwable t) {
            missing = true;
        }
        check("VMStructs 不含 JavaThread::_jni_environment", missing);

        long thread = UnsafeUtils.currentThreadAddress();
        long env = CNative.jniEnvironment(thread);
        check("扫描到 _jni_environment 字段", env != 0,
                "off=" + (env - thread) + ", env=0x" + Long.toHexString(env));

        long table = env == 0 ? 0 : UnsafeUtils.unsafe.getLong(env);
        long getVersion = table == 0 ? 0 : UnsafeUtils.unsafe.getLong(table + 32);
        check("JNIEnv 表指针非 0", table != 0, "table=0x" + Long.toHexString(table));
        check("GetVersion 函数指针非 0", getVersion != 0,
                "fn=0x" + Long.toHexString(getVersion));

        Thread other = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
        }, "scan-probe");
        other.start();
        try {
            long otherAddr = 0;
            for (int i = 0; i < 200 && otherAddr == 0; i++) {
                otherAddr = UnsafeUtils.threadAddress(other);
                if (otherAddr == 0) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            long otherEnv = otherAddr == 0 ? 0 : CNative.jniEnvironment(otherAddr);
            check("另一线程扫描到同一偏移",
                    otherEnv != 0 && (otherEnv - otherAddr) == (env - thread),
                    "off=" + (otherEnv - otherAddr) + " vs " + (env - thread));
        } finally {
            other.interrupt();
        }
    }

    private static void errorPaths() {        try {
            new Klass(0L);
            check("地址 0 抛 IllegalArgumentException", false, "没有抛异常");
        } catch (IllegalArgumentException e) {
            check("地址 0 抛 IllegalArgumentException", true, e.getMessage());
        }

        try {
            new Klass(String.class).primarySuperOfDepth(8);
            check("primarySuperOfDepth(8) 越界", false, "没有抛异常");
        } catch (IndexOutOfBoundsException e) {
            check("primarySuperOfDepth(8) 越界", true, e.getMessage());
        }
    }

    private static void setupConsole() {
        Charset charset = resolveConsoleCharset();
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, charset));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, charset));
        System.setProperty("jdk21.console.charset", charset.name());
    }

    private static Charset resolveConsoleCharset() {
        String override = System.getProperty("jdk21.console.encoding");
        if (override != null && !override.isEmpty()) {
            try {
                return Charset.forName(override);
            } catch (Exception ignored) {

            }
        }
        if (System.console() != null) {
            String nativeEncoding = System.getProperty("native.encoding");
            if (nativeEncoding == null) {
                nativeEncoding = System.getProperty("sun.jnu.encoding");
            }
            if (nativeEncoding != null) {
                try {
                    return Charset.forName(nativeEncoding);
                } catch (Exception ignored) {

                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }

    private static void check(String label, boolean ok) {
        check(label, ok, "");
    }

    private static void check(String label, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            failures.add(label + (detail == null || detail.isEmpty() ? "" : " -> " + detail));
        }
        System.out.printf("  [%s] %s%s%n",
                ok ? "PASS" : "FAIL", label,
                detail == null || detail.isEmpty() ? "" : "   (" + detail + ")");
    }

    private static void summary() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("通过: " + passed + "    失败: " + failed);
        if (failed == 0) {
            System.out.println("结果: PASS");
        } else {
            System.out.println("结果: FAIL");
            for (String f : failures) {
                System.out.println("  - " + f);
            }
        }
        System.out.println("==================================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static String vmOption(HotSpotDiagnosticMXBean bean, String name) {
        try {
            return bean.getVMOption(name).getValue();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static Long offsetOrNull(String typeAndField) {
        try {
            return VMStructsHelper.findOffset(typeAndField);
        } catch (Throwable t) {
            return null;
        }
    }

    private Jdk21Main() {
    }
}
