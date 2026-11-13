# HotSpotMapping

用纯 Java 直接读写 HotSpot 内部数据结构的小型实验项目：通过 `sun.misc.Unsafe` 访问本进程的元空间/堆内存，通过 `gHotSpotVMStructs`（VMStructs）在运行时解析字段偏移，从而在 Java 层镜像 `Klass`、`Method`、`ConstantPool`、`nmethod`、G1 堆等 JVM 内部对象。

支持两条并行的实现：

| 目录 | 目标 JDK | 文件数 | 说明 |
|---|---|---|---|
| `net.hotspot.cplus.jdk17` | JDK 17 | 70 | 完整实现，含 `FieldArray`（17 的字段数组布局） |
| `net.hotspot.cplus.jdk21` | JDK 21 | 69 | 按 21 的布局重写；字段改用 `fieldinfo_stream` 解码，无 `FieldArray` |

两边子包结构完全一致：

```
net.hotspot.cplus.jdkXX
├─ core/           VMStructs、VMStructsHelper、UnsafeUtils、OopDesc、MetaspaceObj、Metadata、DataLayout
├─ klass/          Klass、InstanceKlass、Symbol、AccessFlags、ConstantPool(+Cache)、Method、ConstMethod、MethodArray、FieldInfo、MethodData
├─ codecache/      CodeBlob、CompiledMethod、NMethod、CodeBuilder、BytecodeUnlinker、JvmtiExport、Opcode
├─ gc/             CollectedHeap、G1CollectedHeap、G1HeapRegionTable、HeapRegion、HeapRegionManager、HeapRegionType、HeapScanner
├─ classloading/   ClassLoaderData、Dictionary、DictionaryEntry、VmClasses
├─ threads/        JavaThread、ThreadsList、ThreadsSMRSupport、CompilerThread、CompileTask
├─ ci/             Ci* 与 ciEnv（C++ 编译器接口的镜像）
├─ base/           各种内存数组封装（CArray、ObjectArray、IntArray …）
└─ agent/          AgentMain（Premain / Agent-Class）
```

## 原理

1. **定位符号表**：`ClassLoader.findNative("gHotSpotVMStructs")` 拿到 VMStructs 数组首地址；
2. **解析结构**：按 `VMStructEntry` 的固定布局遍历，得到 `Type::field -> offset` 映射（见 `VMStructsHelper`）；
3. **映射对象**：每个镜像类在构造时把目标地址上的字段一次性读出来，提供 getter/setter；
4. **兜底**：VMStructs 里查不到的私有字段用硬编码默认偏移（分别按 JDK 17 / JDK 21.0.5 校准）。

压缩指针的 base/shift 也从 VMStructs 读取（`CompressedOops::_narrow_oop._base/_shift`、`CompressedKlassPointers::_narrow_klass._base/_shift`），不依赖 `-Dhotspot.heapBase`。

## 能力

- 类元数据：名字、父类、`kind`、vtable、访问标志、初始化状态、`misc_flags`
- 方法：名字/签名、访问标志、`ConstMethod` 字节码、`nmethod`（状态、编译级别、入口地址）
- 字段：JDK 17 走 `Array<u2>`，JDK 21 走 `UNSIGNED5` 编码流，均可按名字+描述符查询
- 常量池：符号解析、常量池扩展与提交（`addUtf8/addClass/addMethodref/...` + `commitCpChanges`）
- 对象：对象头 mark word、类指针、按偏移读写实例字段、`oop ↔ Java 引用` 互转
- 类加载：遍历 `ClassLoaderDataGraph`，枚举所有已加载类、按 loader 分组
- 堆：G1 region 列表与类型、全堆遍历找出某类的所有实例（`HeapScanner`）
- 线程：`ThreadsList` / `JavaThread`，拿到所有 `Thread` 对象
- 字节码替换：`CodeBuilder` 生成字节码 + `ConstMethod.putCodeAndResize` 写回

## 快速开始

### 环境

- 构建：JDK 17（Gradle 用）
- 运行：`jdk17` 包用 JDK 17；`jdk21` 包用 JDK 21（偏移按 21 校准，混跑会读错）

### 构建

```bash
./gradlew jar          # Windows: gradlew.bat jar
```

产物：`build/libs/HotSpotMapping-1.0-SNAPSHOT.jar`

### 自检

`src/test/java/net/hotspot/cplus/jdk21/Jdk21Main.java` 是一个可执行的冒烟测试，覆盖 Klass / InstanceKlass / Method / 字段 / OopDesc / 类加载 / 堆扫描 / nmethod / 线程 / 常量池扩展 / 字节码替换 / 反优化 / 修改 JDK 内部类（`sun.misc.Unsafe`）/ UnsafeUtils 工具方法 / 类还原 / Shellcode 调用本地函数 / C 层字段寻址（含扫描 VMStructs 缺失字段），共 **260 项检查**：

```bash
./gradlew testClasses
java -cp "build/classes/java/main;build/classes/java/test" net.hotspot.cplus.jdk21.Jdk21Main

# jdk17 那一份（需用 JDK 17 运行）
java -cp "build/classes/java/main;build/classes/java/test" net.hotspot.cplus.jdk17.Jdk17PatchMain
```

样例输出见仓库根目录的 `jdk21-check.log`。

### 作为 Agent 挂载

jar 的 manifest 已声明：

```
Premain-Class: net.hotspot.cplus.jdk17.agent.AgentMain
Agent-Class:   net.hotspot.cplus.jdk17.agent.AgentMain
Can-Redefine-Classes: true
Can-Retransform-Classes: true
```

```bash
java -javaagent:build/libs/HotSpotMapping-1.0-SNAPSHOT.jar -jar your-app.jar
```

## 用法示例

```java
import net.hotspot.cplus.jdk21.core.*;
import net.hotspot.cplus.jdk21.klass.*;
import net.hotspot.cplus.jdk21.codecache.*;
import net.hotspot.cplus.jdk21.gc.*;

// 类元数据
Klass k = new Klass(String.class);
System.out.println(k.getName());          // java/lang/String
System.out.println(k.kindName());         // InstanceKlass

// 方法
InstanceKlass ik = new InstanceKlass(String.class);
Method len = ik.findMethod("length", "()I");
ConstMethod cm = new ConstMethod(len.constMethod);
byte[] code = cm.code();                  // 字节码

// 字段
FieldInfo[] fields = new InstanceKlass(MyClass.class).getFields();
ConstantPool cp = new InstanceKlass(MyClass.class).getConstantPool();
for (FieldInfo f : fields) {
    System.out.println(f.name(cp) + " " + f.signature(cp) + " @" + f.offset);
}

// 对象
Object o = "hello";
OopDesc oop = OopDesc.of(o);
System.out.println(oop.getInstanceKlass().getName());
Object back = UnsafeUtils.oopToJavaObject(OopDesc.addressOf(o));

// 已加载的类 / 线程
List<Class<?>> all = UnsafeUtils.getAllLoadedClass();
List<Thread> threads = UnsafeUtils.getAllThreads();

// 堆扫描：找出某类的所有实例
List<Object> strings = HeapScanner.getAllInstances(String.class);

// nmethod
Method m = ik.findMethod("length", "()I");
if (m.code != 0) {
    NMethod nm = new NMethod(m.code);
    System.out.println(nm.stateName() + " compLevel=" + nm.compLevel());
}
```

### UnsafeUtils 常用工具

| 方法 | 说明 |
|---|---|
| `addressOf(o)` / `klassAddressOf(o)` | 对象 → oop 地址 / Klass 地址 |
| `instanceKlassOf(o)` / `classNameOf(o)` | 对象 → `InstanceKlass` / 内部类名 |
| `sizeOf(o)` | 按 `layout_helper` 计算实例或数组大小 |
| `read(addr, len)` / `write(addr, bytes)` | 任意地址的字节读写 |
| `findLoadedClass(name)` / `getLoadedClasses(prefix)` | 在已加载类中按全名/前缀查找 |
| `threadAddress(t)` / `currentThreadAddress()` | `Thread` → `JavaThread` 地址 |
| `vmOption(name)` | 读取任意 VM 参数 |
| `heapUsed()` / `heapCommitted()` / `metaspaceUsed()` | 堆与元空间用量 |
| `patchMethod(ik, m, code)` | 反优化 + 写字节码 + 提交常量池 + 禁止再编译，一步到位 |

### Hook 反射访问检查（jdk17 / jdk21）

```java
UnsafeUtils.hookCheckAccess();            // AccessibleObject.checkAccess 变成空实现
UnsafeUtils.hookCheckCanSetAccessible();  // 所有 checkCanSetAccessible 重载直接放行
```

如果目标方法已经被 JIT 编译、调用点已解析或内联，只改字节码对已编译的调用方无效。
这两个 hook 会先打补丁并 `setNotCompilable()`，然后调用 `deoptimizeCallers(target)`：

- 扫描所有已加载类的已编译方法字节码，把 invoke 指令解析到 `(name, signature)`；
- 凡是调用（因而可能内联）了目标方法的 nmethod，一律 `forceInterpreted()` 丢弃，退回解释器；
- 再对「调用了这些方法的方法」做传递闭包，直到没有新的调用方为止；
- 最后 `freezeTarget()` 轮询清除「补丁前就已排队、补丁后才安装」的旧 nmethod。

等价于 retransform 触发反优化的效果，但不需要 `-javaagent`，也不重新定义类；
`deoptimizeCallers` 返回被反优化的方法数，可以作为工具方法单独调用。
jdk21 的 `checkCanSetAccessible` 有三个重载，hook 会把它们全部补丁化。

### 类还原（restoreClass）

```java
int n = UnsafeUtils.restoreClass(MyClass.class);
```

从 class 文件重新解析方法字节码，把运行时被 agent / 本库改过的 `ConstMethod` 覆盖回去：

- 还原 `code`、`max_stack`、`max_locals`；
- 清除 `not-compilable` / `dont_inline` 标记，让方法重新可以被 JIT 编译；
- 常量池长度收缩回 class 文件里的条目数，隐藏后来追加的条目；
- 对所有被还原方法的调用方做传递反优化（等价于 retransform 的反优化效果），
  下一次调用会重新解析、走还原后的字节码。

限制：按「同名同签名」匹配方法。class 文件里没有的运行时方法不会被删除，
class 文件里有但运行时没有的方法也不会凭空创建（那需要重建 `InstanceKlass`）。

### C 层字段寻址（CNative）

```java
CNative.symbol("gHotSpotVMStructs");                      // findNative 优先，失败走 PE 导出表
CNative.fieldOffset("JavaThread::_anchor");               // VMStructs 偏移（实测 920）
CNative.fieldAddress(thread, "JavaThread::_anchor");      // base + offset
CNative.fieldInt(thread, "JavaThread::_thread_state");    // 读字段
CNative.findFieldByValue(base, 0x1122334455667788L, 64);  // 已知值反查字段偏移
CNative.findPointer(start, end, value);                   // 找指向某值的指针
CNative.deref(base, 16, 8);                               // 指针链取值
CNative.addressOf(base, 16, 8);                           // 指针链求地址（最后一段只加偏移）
```

四种手段：**VMStructs 偏移**、**符号解析**（findNative / PE 导出表）、**内存扫描**（已知值反查字段）、
**指针链跟踪**（如 `Klass::_java_mirror` → handle → oop）。

**验证「VMStructs 里没有的字段」也能找到**：`JavaThread::_jni_environment` 不在 VMStructs 里
（`findOffset` 抛异常），`CNative.jniEnvironment(thread)` 用扫描找它——判据是 JavaThread 里某个
字段指向 jvm.dll 的 `JNINativeInterface_` 表（前 4 项为 NULL，后面是 jvm.dll 内的 JNI 函数指针）。
实测：jdk21 偏移 **952**、jdk17 偏移 **688**；另一个线程扫描得到同一偏移；表指针和
`GetVersion` 函数指针都非 0。

### Shellcode 调用本地函数

```java
long pidFn = Shellcode.kernel32("GetCurrentProcessId");   // 解析 kernel32 函数地址
InstanceKlass ik = new InstanceKlass(ShellTarget.class);
Method m = ik.findMethod("shell", "()J");
Shellcode.write(m, Shellcode.call(pidFn));                 // 把 stub 写进已编译方法的 entry
long pid = ShellTarget.shell();                            // 实际执行 shellcode
```

- `moduleBase` / `export`：从 `gHotSpotVMStructs` 地址反查 jvm.dll 的 PE 基址，解析导出表；
- `kernel32(name)`：解析 jvm.dll 的导入表，拿到 kernel32 函数地址；
- `call(target)`：生成 `push rbp; mov rbp,rsp; and rsp,-16; mov rax,target; call rax; ...; ret` 的 x64 stub；
- `write(m, code)`：写到 nmethod 的 verified entry（code cache 在 Windows 上可写）。

注意：改的是已编译方法的机器码，所以调用要通过 compiled entry（例如从一个没预热过的
解释执行方法里调用），并且之后不要再依赖该方法的原有行为。JDK 17 / 21 都在 Windows x64
上验证通过（返回值就是 `ProcessHandle.current().pid()`）。

#### 地址锁定与 JVM 入口

- `exportOf(addr)`：拿地址反查 jvm.dll 导出名，用来确认解析出来的函数确实是目标
  （测试里断言 `exportOf(symbol("JVM_GC")) == "JVM_GC"`）；
- `jniEnv(thread)`：`JavaThread::_jni_environment` 在 VMStructs 里没有导出，改为手动扫描
  JavaThread 对象，找「指向 jvm.dll 里 JNINativeInterface_ 函数表」的字段，得到 `JNIEnv*`；
- `callJvmEnv(target, env, thread, stateOffset)`：调用前把线程状态切成 `_thread_in_native(3)`，
  返回后恢复 `_thread_in_Java(7)`。

`JVM_*` 是 `JVM_ENTRY` 宏，`ThreadInVMfromNative` 假定调用方来自真正的 native 帧
（见 [javaThread.hpp](https://android.googlesource.com/toolchain/jdk/jdk21/+/1deb1e2e52ca68a3ba34da1af83fb7d3b4eef077/src/hotspot/share/runtime/javaThread.hpp)）。
实测 `JVM_CurrentTimeMillis` 曾成功返回当前时间，但 `JVM_GC` 会 safepoint、在假帧上
栈遍历而崩；所以 **catalog 只对 JVM 符号做解析/反查，不把 JVM_* 调用列为稳定用例**，
Windows API 才是稳定可调用的。

#### 伪造 native 帧的尝试（实验）

从 JDK 源码看（[javaFrameAnchor.hpp](https://raw.githubusercontent.com/openjdk/jdk21u/master/src/hotspot/share/runtime/javaFrameAnchor.hpp)、
[sharedRuntime_x86_64.cpp](https://raw.githubusercontent.com/jatin-bhateja/jdk/f82741e0c47f5e9cd658dbd18ee2e69dd7be8cd6/src/hotspot/cpu/x86/sharedRuntime_x86_64.cpp)）：

- `JavaFrameAnchor` 的布局是 `_last_Java_sp`(+0)、`_last_Java_pc`(+8)、CPU 头里的 `_last_Java_fp`(+16)；
- native wrapper 在调 native 前执行 `set_last_Java_frame(rsp, noreg, the_pc)`，
  `the_pc` 是 **wrapper 自己的代码地址**，并且之前 `oop_maps->add_gc_map(the_pc - start, map)`
  登记过 oopmap；随后把 `_thread_state` 设为 `_thread_in_native`，再 `call native_func`。

也就是说，anchor 指向的是 **带 oopmap 的 wrapper 代码**，不是调用方 Java 帧。`callJvmAnchored`
按 `{sp, pc, fp}` 正确布局塞了调用方编译帧（`sp = rbp+8`、`pc = [rbp+8]`、`fp = rbp`），
但我们的 shellcode 在 Java nmethod 里，没有对应的 oopmap；而且 209 字节的 stub 会越过
`ShellTarget.shell` nmethod 的代码区、踩到相邻的 nmethod，实测仍在 code cache 里崩。

结论：要真正复刻，必须让 shellcode 跑在**有 oopmap 的 native wrapper** 里。
`hookNativeWrapper(holder, name, sig, shellcode)` 已写好雏形：定位 native wrapper 里
`E8 rel32` 的 `call native_func` 并改写到 shellcode；但 JDK 21 里 native 方法的
`Method::_code` 为 0（wrapper 不在 `_code` 里），需要从 `Method::from_compiled_entry`
或 `CodeCache` 遍历里找 wrapper，是下一步。
`callJvmAnchored`、`callJvmEnv`、`hookNativeWrapper` 都保留为实验 API。

#### 能调用哪些函数（测试里的 catalog）

**Windows（kernel32 等，通过 jvm.dll 导入表解析，可直接调用）：**

| 函数 | 验证方式 |
|---|---|
| `GetCurrentProcessId()` | 等于 `ProcessHandle.current().pid()` |
| `GetCurrentThreadId()` | 非 0 |
| `GetTickCount64()` | 非 0 |
| `IsProcessorFeaturePresent(17)` | 返回 0/1（带参数 stub：rcx） |

**JVM 导出（`JVM_*`）：** `symbol()` 能解析 `JVM_GC`、`JVM_ActiveProcessorCount`、
`JVM_CurrentTimeMillis`、`JVM_TotalMemory`、`JVM_FreeMemory`、`JVM_MaxMemory` 等地址
（测试里逐个断言非 0）。但 `JVM_*` 是 `JVM_ENTRY` 宏，需要合法的 `JNIEnv`/`JavaThread`
状态，从裸 shellcode 直接 `call` 会崩（实测 `JVM_GC`、`JVM_ActiveProcessorCount`），
所以 catalog 只**解析**它们，不直接调用；Windows API 才作为调用用例。

### 替换方法字节码

```java
InstanceKlass ik = new InstanceKlass(PatchTarget.class);
Method target = ik.findMethod("target", "(I)I");

target.forceInterpreted();          // 若方法已被 JIT 编译，先把入口切回解释器

CodeBuilder cb = new CodeBuilder();
cb.iload(0);
cb.invokestatic(ik, ExternalHelper.class, "twice", "(I)I");   // 调外部类的方法，会自动扩展常量池
cb.ireturn();

ConstMethod cm = new ConstMethod(target.constMethod);
cm.putCodeAndResize(cb.build(ik));   // 超出 ConstMethod 容量会抛 IllegalArgumentException
ik.commitCpChanges();                // 切换 Klass/ConstMethod/ConstantPoolCache 的指针
target.setNotCompilable();           // 必须：禁止 C1/C2 再编译和内联该方法
```

注意：

- 对**已 JIT 编译**的方法，先调用 `Method.forceInterpreted()` 把入口切回解释器，再替换字节码，否则调用仍走 nmethod；
- 替换后**必须**调用 `Method.setNotCompilable()`（置 `dont_inline` + not-c1/c2/OSR-compilable）。否则 C1 编译调用方、内联这个被改过的方法时会读坏 CP 条目，直接 `EXCEPTION_ACCESS_VIOLATION`；实测 30 万次热调用即可复现；
- 常量池扩展要求目标类**已经链接**（`ConstantPoolCache` 非空），否则 `addMethodref` 会抛 `CPC is null`；
- 常量池索引与字节码里的 cache index 一致是建立在「新条目按同一索引追加」的前提上的，属于实验性做法。

更激进的例子：把 `sun.misc.Unsafe.getUnsafe()`（几乎没有调用方的静态方法）改写成
「调用自己的打印方法 + 返回 null」。调用 `Unsafe.getUnsafe()` 会打印
`[PATCHED] patched method was called` 并返回 null——说明常量池扩展和提交对
CDS 共享的 JDK 类同样有效（见 `Jdk21Main.unsafePatchChecks`）。

## 版本差异备忘

| 项目 | JDK 17 | JDK 21 |
|---|---|---|
| `Klass` 大小 | 200 | 184（去掉偏向锁字段、新增 `_trace_id`/`_shared_class_flags`） |
| `InstanceKlass` | 字段数组 `_fields` | `_fieldinfo_stream`（UNSIGNED5 编码）+ `_fields_status` |
| `ConstMethod` 头部 | 48 | 56（`_flags` 变 u4、新增 `_num_stack_arg_slots`） |
| `CompiledMethod` | 含 `_bitfields`/`_pc_desc_cache`/`_gc_data` | 已移除，反优化状态改由 `nmethod::_state` 表示 |
| 压缩类指针 shift | — | 运行时探测（本机为 0，硬编码 `<<3` 会算错） |

## 限制

- **实验性质**：直接改写 JVM 元空间内存，任何偏移/生命周期判断错误都可能导致进程崩溃（仓库里的 `hs_err_pid*.log` 就是痕迹）；
- 必须在**目标 JVM 进程内部**运行，不能跨进程；
- 部分结构依赖具体构建/版本，VMStructs 查不到时会退回默认偏移，跨小版本可能不准；
- `ci*` 相关类只做到「结构与字段映射」，没有运行时验证（需要编译线程上下文）。

## 目录之外

- `jdk21-check.log`：一次完整自检的输出样例；
- `all-classes.txt` / `all-objects.txt`：早期调试时导出的类/对象清单，可忽略或删除。

## License

暂未指定。
