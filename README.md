# fart12-lite


## 简介

FART（ART 环境下基于主动调用的自动化脱壳方案）的 Android 12 移植版本，去除了 traceMethod、RegisterNative 打印等调试功能，只保留核心脱壳能力。通过配置文件精准控制脱壳目标。

## 核心脱壳流程

入口（Java 层）：
- `FFaarrtt.java` 的 `fartthread()` — 整个脱壳的触发入口，等待 60 秒后调用 `fart()`
- `fart()` → `fartWithClassLoader()` — 遍历所有 dex 的所有类，逐个调用 `loadClassAndInvoke()`
- `loadClassAndInvoke()` — 对每个类的每个方法调用 `DexFile.fartextMethodCode()`

核心 dump（Native 层）：
- `art_method.cc` 的 `dumpArtMethod()` — 核心函数，dump 单个方法的字节码到 bin 文件，同时 dump 整个 dex
- `art_method.cc` 的 `fartextInvoke()` — 设置魔数 111111 然后触发方法执行

主动调用拦截（解释器层）：
- `interpreter_switch_impl-inl.h` — 检测到 `regvalue==111111` 后，调用 `dumpArtMethod()` 然后 break

调用链：

```
FFaarrtt.fartthread()
  → fart() → fartWithClassLoader() → loadClassAndInvoke()
    → DexFile.fartextMethodCode() [JNI]
      → fartextInvoke() [设置 111111]
        → interpreter Execute [检测 111111，强制走 switch 解释器]
          → interpreter_switch_impl [拦截，调用 dumpArtMethod()]
            → dumpArtMethod() [dump dex + bin 到文件]
```

## 修改的文件

| 文件 | 说明 |
|------|------|
| `art/runtime/art_method.cc` | 核心脱壳逻辑、主动调用、dex dump |
| `art/runtime/art_method.h` | 新增方法声明 |
| `art/runtime/interpreter/interpreter.cc` | 强制解释执行 |
| `art/runtime/interpreter/interpreter_switch_impl-inl.h` | 脱壳指令拦截 |
| `art/runtime/native/dalvik_system_DexFile.cc` | setMikRomConfig JNI 注册 |
| `art/runtime/native/java_lang_reflect_Method.cc` | fartextMethodCode JNI 注册 |
| `art/runtime/Android.bp` | 编译配置（-Wno-error） |
| `art/dex2oat/dex2oat.cc` | 禁用 dex2oat 优化 |
| `frameworks/base/core/java/android/app/ActivityThread.java` | 注入 initConfig / fartthread |
| `frameworks/base/core/java/cn/zskkk/FFaarrtt.java` | 主控逻辑 |
| `frameworks/base/core/java/cn/zskkk/PackageItem.java` | 配置数据结构 |
| `libcore/dalvik/src/main/java/dalvik/system/DexFile.java` | 新增 native 方法声明 |

每个文件中修改的代码块由 `//add` 开始，`//add end` 结束。

## 编译

1. 拉取 AOSP 12 源码
2. 将本仓库中的文件替换到对应位置（建议手动对比，不要整个文件覆盖）
3. 在 `build/soong/scripts/check_boot_jars/package_allowed_list.txt` 中添加 `cn\.zskkk`
4. 编译：`m -j$(nproc)`

## 配置文件

脱壳通过 `/data/local/tmp/f1rt.config` 控制，格式为 JSON：

```json
{
  "enabled": true,
  "packageName": "com.xxx.xxx",
  "appName": "xxx",
  "isTuoke": true,
  "isDeep": false
}
```

### 字段说明

| 字段 | 说明 |
|------|------|
| `enabled` | 是否启用，填 `true` |
| `packageName` | 目标 APP 包名 |
| `appName` | 备注名，随意填写，勿用中文 |
| `isTuoke` | 是否执行脱壳 |
| `isDeep` | 是否深层主动调用（针对二代抽取壳） |

## 脱壳产物

dump 出的文件保存在 `/data/data/<packageName>/zskkk/` 目录下：

- `*_dexfile.dex` — dump 出的 dex 文件
- `*_classlist.txt` — 对应的类列表
- `*_deep_dexfile.dex` — 深层调用 dump 的 dex（isDeep=true 时）
- `*_ins_*.bin` — 方法指令 bin 文件
- `*_dexfile_repair.dex` — 修复后的 dex

## 参考

- 原版 FART：[hanbinglengyue/FART](https://github.com/hanbinglengyue/FART)
- FartExt（Android 10）：[dqzg12300/FartExt](https://github.com/dqzg12300/FartExt)
