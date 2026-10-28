# MechanicianCombat Forge 1.20.1 移植指导文档

更新时间：2026-10-26
规范版本：1.0，与移植评估报告定稿同步。

## 1. 当前状态与完成标准

**当前阶段：Forge 47.4.28 / Java 17 已有两次可复现 clean build、最终哈希隔离专服双进程验收、Curios 槽和限定风弹/菜单/忠诚/存档行为通过；完整玩法与客户端联机验收仍进行中，移植尚未完成。** 用户于 2026-10-26 明确批准以官方 Curios API 替换 Accessories。Curios Forge `5.11.1+1.20.1` 作为锁定复现版本，运行时最低范围为 `[5.11.1+1.20.1,)`；其余前置也按最低版本开放，依赖锁仍固定已验证文件供复现。当前发布候选主 JAR SHA-256 为 `D7FE67FDDB9FF885E6CF325333A1E1A4A4C4A2D07F96FB945373ED0DDBAA9989`（972,241 字节），验收 JAR 及依赖哈希见 `docs/evidence/forge-47.4.28/final-build-reproducibility.txt`。隔离专服已验证 Curios back 槽、风弹服务端行为、便携/自动背包发射器单发路径、菜单边界、Loyalty 命中统计与来源标记副手回收、区块 NBT 跨进程读回。当前最终哈希的正式客户端在 Forge Early Display 因无可用主显示器而未进入模组加载；旧哈希客户端日志不计作当前通过。枪械/弹种、自然 Loyalty 返航、背包发射器自然充能/连续运行与炮塔目标切换、真实菜单 UI/恶意网络输入、动画/越肩/声音表现与双人同步仍未完成。详见最新 [验收矩阵](evidence/forge-47.4.28/acceptance-matrix.md)、[构建复现](evidence/forge-47.4.28/final-build-reproducibility.txt)与[专服验收](evidence/forge-47.4.28/final-server-acceptance.txt)。

用户已确定源目录、目标目录、Minecraft 1.20.1 和 Forge，并于 2026-10-25 批准 A 方案：使用原项目已有前置的 Forge 1.20.1 对应版本，尽量完整保留玩法；额外新依赖另行确认。

目标是保留 r349 的枪械、弹药、动作、战斗背包、发射器、自动炮塔、界面和视觉反馈。分阶段实施仅用于控制风险，不代表允许交付删减版。用户已批准 Accessories 改用 Curios；其他功能降级、操作改变或依赖替换仍需先确认。

完成须满足：可复现构建；客户端与专用服务端可启动；关键玩法、持久化及双人同步通过验收；资源、依赖、已知限制有明确记录。仅编译通过不足以宣布移植完成。

## 2. 源项目证据

参考根目录：`E:\Server_mod\MechanicianCombat-source-r349-20261025_030512`。

| 项目 | 检查结果 |
| --- | --- |
| 源平台 | Minecraft 1.21.1、Fabric、Java 21 |
| 源构建标记 | `r349-2026-10-25` |
| 源 ID / Java 包 | `tkgun` / `com.tk.gun` |
| Java 源码 | 101 个文件，16,101 个非空行（PowerShell Measure-Object -Line） |
| 主资源目录 | 229 个文件，不包含 source_assets |
| 网络消息 | `net` 包内 13 个 Java 文件 |
| Mixin 配置 | 两份配置，6 个公共项、14 个客户端项 |
| 构建形式 | PowerShell 离线编译与 Yarn 重映射；不是可直接降版本的 Forge Gradle 工程 |
| 目标目录初始状态 | 空目录 |

关键证据文件均相对参考根目录：

- `SOURCE_HANDOFF.md`：平台、前置、版权说明和 r349 炮塔待验证事项。
- `deps/SOURCES.txt`：named 编译副本的来源；内容与交接说明有差异，不能单独作为依赖锁文件。
- `mod/src/main/resources/fabric.mod.json`：实际声明的入口与必要前置。
- `docs/模组操作说明.txt`：操作和玩法基线；与代码冲突时记录差异并以实际 r349 代码核实。
- `mod/src/main/java/com/tk/gun/TkGunServer.java`：注册、网络接收、战斗状态与背包逻辑，约 3,757 个非空行。
- `TkGun.java`、`GunStateMachine.java`：状态编号、基础数值与状态迁移。
- `item/GunAmmoData.java`、`BackpackUpgradeData.java`、`BackpackFireworkData.java`：数据组件与 NBT 迁移重点。
- `client/rig/*`、`client/CombatBackpackGeoRenderer.java`：玩家骨架和背包动画。
- `client/ShoulderSurfBridge.java`：存在对前置内部字段的反射，需要逐版本核实。

源交接说明将自动炮塔锁定方向、滚转、不同玩家骨架下的位置和平滑切换列为待验证项。这些不是已证明正确的功能，移植后仍需专项验收。

## 3. 目标基线与组织方案

| 项目 | 约定 |
| --- | --- |
| Minecraft | 严格目标 1.20.1 |
| Loader | Minecraft Forge 47.x，不是 NeoForge |
| Java | JDK 17，编译目标 17 |
| 最终 Forge 目标 | 47.4.28；此前 47.4.6 仅为历史验证基线，新版构建与运行须重新实测 |
| 映射 | Mojang official 1.20.1；不混入 Yarn 命名的依赖 |
| Gradle / ForgeGradle | 8.8 / 6.0.54，已有无源码阶段配置成功证据；禁止直接拿参考构建脚本运行 |
| 名称 / ID | `MechanicianCombat` / `tkgun` |
| Java 根包 | `com.mechaniciancombat`（本文提出的组织方案） |
| 资源命名空间 | 保持 `assets/tkgun` 与 `data/tkgun` |
| JAR 命名 | `MechanicianCombat-1.20.1-forge-<移植版本>.jar` |

保留命名空间有助于资源和注册 ID 一致；**不意味着 Minecraft 1.21.1 世界可以降级到 1.20.1**。本次默认不制作跨版本世界转换器，验收使用独立 1.20.1 测试世界。

建议工程结构（部分配置与目录已提前建立；下列职责划分仍是后续实施建议，不表示模块已实现）：

```text
MechanicianCombat/
  AGENTS.md
  build.gradle
  settings.gradle
  gradle.properties
  gradlew / gradlew.bat / gradle/wrapper/
  src/main/java/com/mechaniciancombat/
    registry/       注册
    combat/         服务端状态与战斗规则
    item/ entity/ block/ menu/
    network/        消息及处理
    compat/         已批准前置的接入
    client/         按键、HUD、GUI、渲染与相机
    mixin/          经核实必须保留的注入
  src/main/resources/
    META-INF/mods.toml
    pack.mcmeta
    assets/tkgun/
    data/tkgun/
  src/test/java/    不额外引入测试库的纯逻辑检查
  docs/
    PORTING_GUIDE.md
```

保留关键业务类名以方便逐项对照；不在首次移植时大规模重写状态机。源项目及同级其他模组仅允许读取。

## 4. 已批准依赖与新增依赖规则

批准状态：**A 方案已批准**。可下载和接入原项目已有前置的 Forge 1.20.1 对应版本；下表不代表各版本 API 已验证。额外新依赖仍须确认。

| 源依赖 | 使用证据 / 风险 | 目标处理建议 |
| --- | --- | --- |
| Fabric API / Loader | 入口、注册、事件、网络和界面普遍使用 | 用 Forge 原生机制重写，不引入 Fabric 兼容层 |
| GeckoLib 4.8.2 | 多处 GeoItem、GeoRenderer、骨架动画 | 使用已核查的 Forge 1.20.1 / 4.8.4，适配该版本 API |
| Player Animation Lib | 原项目使用的前置；目标清单锁定 Forge `1.0.2-rc1+1.20` | 作为客户端前置驱动原版玩家模型；持枪身体姿势取自 GeckoLib 当前骨骼状态 |
| Accessories / Curios API | 原背包直接继承 Accessories API；用户于 2026-10-26 明确批准替换为 Curios | 使用官方 Forge `5.11.1+1.20.1`；实现 Curios `ICurioItem`、`back` 槽、装备查询和 `ICurioRenderer`，验证完成前不标记运行通过 |
| Shoulder Surfing Reloaded | 清单为必需，桥接代码有回退和反射 | 优先使用对应 Forge 版本；核对公开扩展 API，隔离到客户端 |
| Combat Roll | 清单为必需，服务端滚动事件与客户端输入 Mixin | 核对 Forge 版本事件及动作时序，保持翻滚/滑铲循环 |
| owo-lib / Cloth Config / Forge Config API Port | 交接说明列为前置 | 以选定目标前置的元数据确认是否仍必需，不机械照搬 Fabric 列表 |
| AzureLib | SOURCES 列为源 Combat Roll 依赖 | 仅在目标版本确实需要且用户批准时接入 |
| CustomSkinLoader | 源操作说明称可选 | 不作为新增必要前置；有条件时列兼容检查 |

已采用 **A：保真移植**。允许使用参考项目已经用到的前置的 Forge 1.20.1 对应版本；用户另于 2026-10-26 明确批准用 Curios API 替换 Accessories，不改变背包玩法。PlayerAnimator 是原项目已有前置，现作为客户端运行前置。当前前置及其固定验证文件见 `DEPENDENCIES.json`。除此授权外，新增替代运行库仍需确认。锁记录的 `declaredDependencies` 来自发布 API，不等于完整的 JAR 必需依赖列表，以评估报告第 4 节的元数据核查结果补充判断。

原 Accessories 对照测试及其 Mixin 阻塞作为历史证据保留。替换决定已由用户明确批准；现阶段继续完成 Curios `back` 槽数据加载、真实装备查询、客户端渲染/UI、服务端背包发射及多人同步验收，不因编译通过而宣称功能完成。

依赖批准记录：

| 日期 | 用户决定 | 覆盖范围 | 状态 |
| --- | --- | --- | --- |
| 2026-10-25 | 用户选择 A | 原项目已有前置的 Forge 1.20.1 对应版本；额外新依赖另行确认 | 已批准 |
| 2026-10-25 | 用户追加批准 | 官方 Accessories JAR 自带 Endec、MixinExtras、少量 Forgified Fabric API 模块，无需另装 Connector | 已批准 |
| 2026-10-26 | 用户要求将 Accessories 依赖改为 Curios API | Curios Forge 1.20.1；迁移背包集成并继续完整保留玩法 | 已批准；可复现构建和服务端槽集成断言通过，真实 UI/渲染待验收 |

用户随后决定参考 GitHub 已有 Forge 1.20.1 实现，在本项目内制作风弹，见 [参考研究与接入方案](风弹参考项目与实现方案.md)；配方或掉落交给整合包配置，本模组不内置配方。用户另已要求支持忠诚附魔：目标熔铁弩矢须保留忠诚并适配回收，不自动开放其他附魔。用户已确认生存模式同时支持附魔台与铁砧。撞方块消失按用户要求复刻源项目：保留 50% 消失，忠诚仅能返回未消失的弩矢；源码证据与回收约束见 [移植评估报告第 6.2 节](移植评估报告.md#62-忠诚附魔与原有消失规则)。

MixinGradle 0.7.38 已获用户批准，仅作为构建期工具生成 refmap，固定版本与校验值见依赖清单；不作为模组运行时前置。

## 5. 必须完成的 API 与资源转换

### 5.1 生命周期、注册和事件

- 两个 Fabric 初始化入口改为 Forge 模组入口及独立客户端生命周期接入。
- 物品、方块、方块实体、实体、菜单、声音、粒子及创造模式页统一使用 DeferredRegister。
- Tick、玩家事件、命令、按键、粒子工厂与渲染层分别映射到正确的 Forge 事件；避免重复注册。
- Yarn 类名转换为 Mojang official 名称后，仍必须核对方法签名与行为，不能只批量替换 import。

### 5.2 网络与联机

逐个登记以下 13 个源消息的字段、方向、接收者、触发条件，再移植为 SimpleChannel 消息：

`CombatBackpackFirePayload`、`CombatBackpackFireResultPayload`、`CombatBackpackStatusPayload`、`GunDemoPayload`、`GunFirePayload`、`GunFocusPayload`、`GunHitMarkerPayload`、`GunReloadPayload`、`GunRollPayload`、`GunSoundPayload`、`GunStatePayload`、`OpenAmmoStoragePayload`、`SwitchAmmoPayload`。

- 不沿用 1.21 的 CustomPayload / PacketCodec / RegistryByteBuf。
- 显式分配协议版本、消息 ID 和方向；协议不匹配应明确拒绝。
- 服务端接收时验证发送者、持有物、当前状态、冷却、弹药、索引范围和容器有效性。
- 音效、射线/目标、射击频率不能由客户端任意指定；广播范围须匹配实际观察者。
- 登录、开始追踪、重生、换维度和退出时同步或清理状态，避免跨玩家串状态。

### 5.3 数据与容器

- 将 DataComponentTypes.CUSTOM_DATA 转为 1.20.1 ItemStack NBT；保留 `tkgun_ammo`、`Selected`、`tkgun_mag`、`tkgun_mag_ammo`、`tkgun_mag_version` 的业务语义。
- 区分新枪与打空的枪；验证每把枪独立的 8 槽弹药环及选择槽。
- 附魔、烟花、盔甲纹饰、工具属性等组件分别适配，不得一律当作 CUSTOM_DATA。
- 两种 ScreenHandler 改为 AbstractContainerMenu 体系；核对打开参数、距离校验、Shift 点击、关闭/断线/换手行为和方块实体持久化。
- 明确每项数据由物品、玩家、世界还是临时状态持有；不得把应持久化的数据仅存静态 Map。

### 5.4 美术、渲染与 Mixin

- 初次迁移直接复用已导出的模型/动画/贴图/音频，不运行带绝对路径的源资源导出脚本。
- 逐层验证玩家 rig、武器层、盔甲层、背包层和炮塔骨骼，保留名称、坐标含义与动作阶段。
- 客户端前置只能由客户端类加载；专用服务端启动必须单独验证。
- Mixin 共 20 个配置项逐项建映射，更新包名、Java 17、目标描述符与 refmap。必须证明注入实际生效。
- ShoulderSurfBridge 的反射字段不视为稳定 API；优先已验证的公开接口，无等价接入时先报告方案。

### 5.5 数据资源

- 源 `data/*/recipe/` → 目标 `data/*/recipes/`。
- 源 `data/*/loot_table/` → 目标 `data/*/loot_tables/`。
- 源 `data/*/tags/item/` → 目标 `data/*/tags/items/`，其他标签按 1.20.1 类型核实。
- 已检查的背包染色配方使用结果 `id` 字段；1.20.1 配方应转换为结果 `item`，逐个验证而非只重命名目录。
- `damage_type` 与 `tags/damage_type` 保持该版本正确目录；不能全局把所有目录复数化。
- 1.21 的 `enchantable/trident` 标签不能作为 1.20.1 附魔可用性的充分实现，需在代码或目标机制中处理熔铁弩矢附魔约束。
- GUI sprite 元数据和新渲染调用逐项适配 1.20.1 纹理绘制接口。
- 建立 1.20.1 的 `pack.mcmeta`（pack_format 15）；补齐 mods.toml，移除 Fabric 元数据，检查 JSON 引用与大小写。

## 6. 实施阶段和验收门槛

| 阶段 | 工作与产物 | 通过条件 | 当前状态 |
| --- | --- | --- | --- |
| R0 | 源码盘点、规范、依赖决定 | 主要方案已定，剩余事项明确 | 已完成；目标版本、Curios 替代、最低版本依赖门槛、风弹及忠诚规则均已由用户确认 |
| R1 | MDK、固定版本、注册/消息清单、入口 | Java 17 构建，客户端和专服启动，侧别正确 | 当前候选多次可复现 clean build 通过，Forge 47.4.28 正式映射专服启动/停止通过；当前正式哈希在默认 Early Display 配置下 G/H 双客户端启动和加入专服通过。之前主显示器错误属于间歇性历史记录；客户端/专服基本启动门槛通过，其他 R4/R5 行为仍未全部验收 |
| R2 | 物品、数据、菜单、方块实体与资源转换 | 关键数据存取、菜单安全、资源加载通过 | 当前哈希专服验证方块标签、资源加载、菜单过滤/边界/破坏、容器 NBT 持久化；两个客户端在生产弹药环 UI 真实回环 64 枚物品，并经生产改装台 UI 点击安装背包核心/便携发射器，验证服务端槽位与 NBT 同步。一名玩家 Inventory NBT 已通过单次保存、服务端进程重启及同身份重连精确对照。附魔台/铁砧真实生存 UI、丢弃重拾/异常关闭/断线、多玩家/异常停服持久化仍未过 |
| R3 | 战斗状态、消息、弹体、伤害与背包系统 | 服务端逻辑与双人同步通过 | 最新哈希目标版专服验收通过风弹/全部弹种/手持、枪械、Curios 背包 Loyalty III 自然回收、发射器/炮塔/门控和存档重启；双客户端仅菜单行为同步通过，战斗实体/弹体/粒子/声音观察者同步仍未过 |
| R4 | 动画、HUD、相机、Curios 与翻滚前置 | 主要动作/视角/穿戴与 r349 对照通过 | 当前正式哈希在默认 `earlyWindowControl=true` 客户端启动，Curios、弹药环和改装台真实屏幕事件操作、装配状态同步与瞄准截图通过；2026-10-26 修正腰射相机耦合控制、普通玩家模型背包位置/缩放及持枪动画皮肤第二层棱边接缝，主项目 `compileJava` 通过，但这些源码修改后的真实键鼠移动、鼠标转向、背包位置和动画皮肤画面尚未实机复验。灰阶枪械画面尚未与原作者目标逐项比对；翻滚/滑铲键盘输入、音效试听及对端战斗观察同步仍未验收 |
| R5 | 完整回归、构建打包、依赖与说明 | 完成验收矩阵或记录用户认可的偏差 | Java 17 下当前源码连续 clean build 字节一致，候选 SHA-256 `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D`；目标版专服双进程行为/持久化、默认早显双客户端启动及菜单/NBT同步通过。客户端完整战斗输入、动作、音效、观察者同步、异常菜单恢复和原作者视觉对照仍缺证据，不能作为移植完成或最终发布 |

用户已明确要求继续移植。按 R1–R5 推进，每阶段更新本文状态，保留构建日志、失败原因、修改摘要和验证日期；尚未可运行的阶段产物必须明确标记。任何未决选择只阻塞相关功能，不应停止独立的移植工作。

## 7. 最小验收矩阵

| 功能 | 必测行为 |
| --- | --- |
| 构建/安装 | Java 17 clean build；检查最终 JAR 元数据；无 Fabric/Yarn 类引用或误打包游戏/第三方 JAR |
| 启动/资源 | 客户端与专服分别启动；无缺失注册、配方、贴图、声音或 Mixin 目标错误 |
| 枪械 | 腰射 6 发、专注 10 发；手动装填；射击时序、热量、过热、退出专注和输入限制 |
| 弹药环 | 8 槽；滚轮切换；空弹药无法装填；两把枪独立保存；丢弃/捡起/重登正确 |
| 弹种 | 爆破、切割、幽匿、玫瑰石英、霰弹、熔铁弩矢等逐项对照实际注册；伤害/范围/追踪/附魔/回收 |
| 忠诚 | 手持、背包、枪械三条路径保留等级与物品数据；保留撞方块 50% 消失；副手回收与充能结算一次，无复制或额外自动补满弹匣 |
| 风弹 | 自有物品与弹体；背包速度 1.6 格/tick，成功生成扣 1 个弹药和 1 次充能；专项击退/碰撞/同步检查；配方及掉落交给整合包 |
| 翻滚/滑铲 | 循环交替、冷却、快速专注窗口、动作尾帧；按键重绑定后仍生效 |
| 战斗背包 | 背部槽穿脱、颜色、升级、肩炮锁定/充能/发射、烟花免伤与 HUD |
| 自动炮塔 | r349 C3 模型、目标切换、遮挡/距离/失效目标、锁定朝向、滚转和多玩家可见性 |
| 改装台/容器 | 合成与掉落、打开权限、Shift 点击、容量与物品类型；关菜单/断线/破坏方块无复制或丢失 |
| 视角/渲染 | 第一/第三人称、越肩、皮肤与盔甲、瞄准/持枪切换、不同动作下模型位置 |
| 联机 | 至少两名玩家；观察者动作/声音/弹体同步；登录/重生/维度切换无遗留状态 |
| 异常输入 | 无枪请求射击、非法槽位、过量频率、失效菜单/目标，不越权不崩溃 |

纯规则测试使用 JDK 自带能力；需要世界语义的检查使用 Forge 1.20.1 GameTest 或实际测试世界，不引入 Bedrock GameTest。只测试重要行为，避免为文件存在或实现细节堆砌测试。

当前项目后续验收命令（按运行条件执行；Gradle 任务返回成功不等于游戏本体完成启动）：

```powershell
.\gradlew.bat --version
.\gradlew.bat clean build
.\gradlew.bat runServer
.\gradlew.bat runClient
```

如建立了数据生成器或 Forge GameTest，再执行 `runData` / `runGameTestServer`；生成资源必须纳入检查，不能靠运行空任务宣称测试通过。

## 8. 交付记录

每次提供测试包应记录：版本、源码基线、构建命令/JDK/Forge 版本、准确 JAR 路径和 SHA-256、固定前置列表、已测场景、未测场景、已知缺陷和复现步骤。

保留源作者信息。源交接说明要求第三方编译副本不得直接分发，原美术资源未经确认不可公开再分发；移植时不能因 fabric.mod.json 写有 MIT 就推定所有第三方文件与美术资源均可打包发布。

## 9. 技术参考

以下是本次核对的 Forge 官方入口；具体实现仍以锁定的 1.20.1 源码签名为准：

- [Forge 1.20.x 工程入门](https://docs.minecraftforge.net/en/1.20.x/gettingstarted/)
- [Forge 注册](https://docs.minecraftforge.net/en/1.20.x/concepts/registries/)
- [Forge 客户端/服务端区分](https://docs.minecraftforge.net/en/1.20.x/concepts/sides/)
- [Forge SimpleImpl 网络](https://docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/)

当前运行依赖为 Curios Forge `5.11.1+1.20.1`，运行时最低范围为 `[5.11.1+1.20.1,)`；精确验证文件与哈希见 [依赖锁记录](DEPENDENCIES.json)。Accessories beta46/beta45 的兼容记录仅作历史对照，不属于当前安装清单。六个第三方前置都按最低版本开放运行时范围；更高版本实际 API 兼容性尚未覆盖。

