# MechanicianCombat Forge 移植实施计划

> **执行状态：** 用户于 2026-10-25 明确要求按项目 docs 与 AGENTS.md 完成移植，本计划恢复执行。此前的 A 依赖方案批准继续有效；附魔台与铁砧的忠诚附魔入口也已由用户确认。

> **最新进度（2026-10-25）：** MixinGradle 0.7.38 按批准作为构建期工具；113 个 Java 文件通过 JDK 17 clean build，Accessories 固定 beta46。最新 JAR SHA-256 `0617C4E50DD627B0AEE7704CBE211787444339E177D8096E3DCCCC5F5F710290`，966,487 字节；无第三方依赖的背包替换与弹药环方向/索引/消费量合同回归已通过，最新哈希已在 Forge 47.4.6 正式隔离专服启动并进入世界。当前哈希双客户端同时登录已实测，服务端观察到 `tkgun:wind_charge` 实体；具体使用扣物、命中/击退及观察者同步未确认。旧哈希 `91208F9A923B67AC9663EF53F9C9946FCA8165A63E4A8A25F289257E0F345955` 有风弹扣物/生成弹体证据。附魔台/铁砧 UI、改装台容器实操、全玩法矩阵、前置及跨玩家视角同步尚未验收。

**Goal:** 将 r349 全部玩法移植为 Forge 1.20.1，交付可验证构建与规范文档。

**Architecture:** 保留 tkgun 标识、业务类和资源，逐层替换 Fabric/Yarn/1.21 接口。Forge 负责注册、网络和事件；原项目已用的前置使用兼容版本，服务器权威状态与客户端显示隔离。

**Tech Stack:** Java 17、Forge 47.4.6 基线、Gradle 8.8、ForgeGradle 6.0.54、Mojang official 1.20.1；已批准前置的实际版本记录于依赖锁文件。当前组合已通过有源码构建和正式隔离专服启动，玩法回归仍未完成。

**Spec:** ../../PORTING_GUIDE.md

**2026-10-25 定稿同步：** 以 [移植评估报告](../../移植评估报告.md) 的 D01–D07 决策和 R0–R5 阶段为准。以下 Task 为未来工作清单，未勾选不等于此前没有草稿；恢复前先审查实际文件和记录。

## Global Constraints

- 只写 E:\Server_mod\MechanicianCombat；参考项目只读。
- Minecraft 1.20.1 / Minecraft Forge / Java 17；根包 com.mechaniciancombat；ID tkgun。
- A 方案已批准；额外新依赖必须确认。
- 官方 Accessories 自带模块已批准；不另装 Connector。
- 内置 `tkgun:wind_charge`，无新风弹前置，配方/掉落交给整合包，本模组不内置配方。
- 熔铁弩矢支持忠诚，可通过附魔台和铁砧获取；保留撞方块 50% 消失与源回收/充能规则；其他附魔不自动开放。
- 不删玩法，不用占位实现冒充移植完成，不把编译通过当实机通过。

## Review Focus

1. 专服加载客户端类：任务 1/5 检查专服启动。
2. 新枪与打空枪混淆：任务 3 验证空弹匣重登仍为空。
3. 消息伪造与重复射击：任务 4 验证服务器状态和频率限制。
4. 改装容器关闭或换手复制物品：任务 3 验证实际容器转移。
5. 动画或 Mixin 静默失效：任务 5 检查注入和实际动作。

## Task 1：可复现的构建与前置

Files: build.gradle、settings.gradle、gradle.properties、gradle/wrapper/*、tools/fetch-dependencies.ps1、docs/DEPENDENCIES.json、src/main/resources/META-INF/mods.toml。

Interfaces: 固定 Java/Forge/依赖 classpath，为全部后续任务提供 Mojang official 类型。

- [x] 复核已有六个已锁定发布物、hash 与 JAR 元数据；无需重新选择前置方案。
- [x] 审查已建立的 wrapper、ForgeGradle 与 Java 17 配置，保留原署名。
- [x] 执行 Gradle 离线构建/依赖解析并检查当前打包；无未获准的新运行时前置。

## Task 2：资源与映射基线

Files: src/main/resources/assets、data；src/main/java/com/mechaniciancombat 下对应 101 个源类；tools 中的迁移辅助文件。

Interfaces: 保留 TkGun.MOD_ID、State 编号及全部注册 ID；数据目录符合 1.20.1。

- [x] 转换配方/掉落/标签路径与配方结果结构，85 个资源 JSON 解析通过，模型/贴图/声音引用静态核验缺失 0。
- [x] 用目标源码和映射完成 113 个 Java 源文件迁移；目标包为 `com.mechaniciancombat`，未发现 `example` 包。
- [x] 全部 113 个 Java 源文件纳入 ForgeGradle 编译，无通过排除源文件逃避移植的情况。

## Task 3：注册、存储和菜单

Files: TkGunServer.java、registry/*、item/*、block/*、screen/*、client/screen/*。

Interfaces: DeferredRegister 暴露 RegistryObject；GunAmmoData 保留 8 槽及独立弹匣语义；MenuProvider 提供安全容器访问。

- [x] 建立纯 JDK 弹药环边界合同：8 槽索引归一、首尾切换、服务端仅接受 -1/+1、拒绝零/负数消费量；缺失规则的 RED 编译失败及实现后的 `verifyPortContracts` 通过。两把枪独立、空弹匣 NBT 保存和非法物品仍需游戏/存档级验证。
- [x] 新增风弹手持使用 GameTest 源码并编译；`runGameTestServer` 的 userdev 启动被 Accessories beta46 内嵌 Fabric API 的 `EntityMixin` 映射错误阻断，但单独重映射的测试模组已在 Forge 47.4.6 正式映射隔离服执行等价实际世界断言：扣物、弹体生成、命中猪及击退通过；Loyalty 附魔台/铁砧 API 兼容检查通过。实际 UI 和投掷返航仍待测。证据见 `docs/evidence/2026-10-25/production-mapped-acceptance.md`。
- [ ] 数据组件转为目标版本 NBT/附魔/烟花数据接口；适配注册和菜单。
- [ ] 检查配方、关闭菜单、Shift 点击、换手和改装台移除，确保没有复制或吞物品。

## Task 4：战斗、实体与消息

Files: GunStateMachine.java、TkGunServer.java、entity/*、net/*、CombatRollHook.java。

Interfaces: SimpleChannel 固定消息编号/方向，游戏线程处理；服务端独立判定发射和伤害。

- [ ] 为禁止射击状态、弹药消耗、非法消息、射击节奏建立针对性检查。
- [ ] 逐个迁移 13 个消息及事件、属性、弹体接口；保留腰射 6 发/专注 10 发和原动作时序。
- [ ] 验证原代码全部弹种、背包/发射器/炮塔，不能只实现常规枪弹。
- [ ] 按风弹专项方案接入自有风弹；按报告第 6.2 节适配忠诚、穿透、消失与背包回收结算。

## Task 5：客户端表现与前置整合

Files: client/*、client/rig/*、mixin/*、Mixin 配置。

Interfaces: 客户端只接收状态显示；已批准前置各自封装接入，专服不加载客户端类型。

- [ ] 接入按键、HUD、模型层、动画、Accessories 与越肩视角，核对 Combat Roll 事件和 20 个 Mixin 项。
- [ ] 优先使用所选版本公开 API；反射仅在核实目标后保留，失败不能静默吞掉关键功能。
- [x] 当前哈希 Forge 47.4.6 客户端/专服启动、双客户端同时登录及风弹实体注册/存在已实测（不是完整验收矩阵）；扣物、命中/击退和同步待验。
- [ ] 附魔台/铁砧 UI、其他弹种/背包/炮塔、改装菜单、异常输入、双人画面/音效同步与依赖联动逐项执行指导文档矩阵。

## Task 6：最终检查和交付

Files: docs/PORTING_GUIDE.md、docs/DEPENDENCIES.json、docs/PROGRESS.md、build/libs 产物。

- [x] 一份隔离源码副本 clean build 与当前主 JAR 产生相同 SHA-256；最终 JAR、依赖和哈希已核对，针对性运行验收部分通过。
- [x] 文档记录实现状态、运行验证与剩余问题；未将未测玩法标为通过。
- [ ] 按执行技能进行一次独立最终审查并修复重要问题。

## 执行记录约定

当前目录不是已确认的 Git 仓库，不假设可提交或建立 worktree。按用户明确要求在目标目录工作；进展与验证记录于 docs/PROGRESS.md。
