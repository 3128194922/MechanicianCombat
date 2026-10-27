# MechanicianCombat：Forge 47.4.28 移植现状与后续行动报告

日期：2026-10-26；依据：当前项目文件、既有原始日志和官方资料。

**当前结论：继续完成移植验收，目标为 Minecraft 1.20.1 / Forge 47.4.28 / Java 17。**

**最新状态更新（2026-10-26）：** 当前正式候选主 JAR SHA-256 `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D`（980,699 字节），Curios API 5.11.1+1.20.1，Forge 47.4.28 / Java 17.0.14。显式使用 JDK 17 与 7890 代理的两次 clean build 主包及验收包哈希一致；目标版专服 prepare/保存/第二进程 verify 通过。风弹、全部弹种、三路径 Loyalty III 自然返航与 50% 撞墙消失、枪械门控与背包发射器/炮塔、菜单安全、服务端持久化通过验收；G/H 双客户端通过弹药环真实 UI 64 枚取放回环、改装台 UI 安装背包核心/便携发射器并验证服务端 NBT 同步，且保存了专注动画帧，最新一轮使用默认早显配置复验。一名玩家的物品栏 NBT 已通过正常离服保存、专服进程重启及同身份重连对照。原双端动作脚本结束截图只证明主循环到达 tick 1525，没有逐项证明翻滚键、滑铲或射击生效。当前正式哈希本轮在默认 earlyWindowControl=true 下已完成 G/H 双客户端启动与联机；此前 GLFW 主显示器失败为间歇性历史结果。灰白几何、真人输入质量、观察者同步、音效播放和菜单异常关闭/断线仍未验，玩家持久化也仅有单人单次样本，项目继续标记未完成。详见 `docs/evidence/forge-47.4.28/acceptance-matrix.md`、`default-window-client-retest.md`、`final-server-acceptance.txt`、`final-multiplayer/final-multiplayer-acceptance.txt`、`player-relog/result.md`、`table-ui-final-reproducibility.txt`。

### 后续实施补记（2026-10-25）

用户随后明确授权恢复实施，并固定最终目标为 Forge 47.4.28 / Java 17。构建版本和主/测试 `mods.toml` 已更新；镐挖掘标签迁至 `tags/blocks`，且在目标版正式映射服数据加载后通过标签成员检查。当前源码/资源基线见 `docs/evidence/forge-47.4.28/baseline-sha256.txt`。目标版 userdev 现已完整获取、校验并用于 clean build；目标版实际验收及限制以本报告最新状态更新和验收矩阵为准。

此前“只写报告、不直接实施”阶段限制已被用户本轮明确撤销；本次授权包括恢复实际修改、构建和验证。技术排查按用户指定的 Forge 47.4.28 目标、P0→P5 顺序持续进行，不把旧版验证结果升级为目标版通过。

> 状态核对：旧计划和旧报告中的“未下载/未构建/未启动”只代表其记录时间点；以 `PROGRESS.md` 与 `docs/evidence/forge-47.4.28/` 中后续目标版证据为准。

**Goal：** 完整保留 r349 及已批准的风弹、忠诚功能，交付经过 Forge 47.4.28 客户端、专服与双人玩法验收的移植包。

**Architecture：** 沿用现有 `com.mechaniciancombat` 工程及 `tkgun` 注册 ID，修复和验证现有实现。使用 Forge 注册、事件、SimpleChannel 与 1.20.1 NBT；服务端决定战斗和物品变化，客户端负责输入与表现。

**Tech Stack：** Minecraft 1.20.1、Forge 47.4.28、JDK 17、Mojang official 1.20.1；先保留 Gradle 8.8、ForgeGradle 6.0.54、MixinGradle 0.7.38。现有六个前置见下表，兼容性以目标环境实测决定。

**Spec：** [AGENTS.md](../AGENTS.md)、[移植指导](PORTING_GUIDE.md)、[移植评估报告](移植评估报告.md)、[风弹专项方案](风弹参考项目与实现方案.md)、[进展记录](PROGRESS.md)。

## 1. 约束与已确定的决定

- 所有修改、构建和隔离验收均在 `E:\Server_mod\MechanicianCombat` 内；源参考目录及其他项目只读，不重跑覆盖式迁移。
- 根包保持 `com.mechaniciancombat`，包路径不出现 `example`；名称 MechanicianCombat，模组与资源 ID 保持 `tkgun`。
- 不删减枪械、弹种、背包、改装台、发射器、炮塔、动作、界面、动画和音效，不擅改操作与数值。
- 沿用原项目前置的 Forge 对应版本及已经批准的官方内嵌模块；经用户批准，饰品 API 使用 Curios 替代 Accessories；不引入 Connector 或其他替代运行库，不改写第三方 JAR。
- 自有 `tkgun:wind_charge`，无风弹模组前置；配方和掉落由整合包提供，不额外扩展试炼内容。
- 熔铁弩矢支持忠诚，生存获取入口为附魔台和铁砧；不自动开放其他附魔。保留撞方块后 50% 消失，只有未销毁弹体可以返航。
- 背包发射风弹速度为 1.6 格/tick，成功生成后消耗一个副手风弹及一次充能；失败不能扣除。手持投掷是独立路径，不混为背包规格。
- 不把 1.21.1 世界降级作为目标；使用独立 1.20.1 测试世界。
- 当前目录未发现 Git 仓库，后续用主项目内的文件清单、校验值和证据目录保存基线，不假设能提交或创建工作树。

## 2. 当前移植已到什么程度

### 2.1 前期只读核查结果（历史基线）

| 项目 | 当前事实 | 解释 |
| --- | --- | --- |
| 主源码 | 113 个 Java 文件 | 已有完整工程，不从空项目重做 |
| 资源 JSON | 85 个 | 数量和既有记录一致；解析成功不证明路径与游戏语义正确 |
| 构建基线 | Forge 47.4.6 / Java 17 | 配置尚未升级至 47.4.28 |
| Forge 元数据 | 主包 `[47.4.6,48)`；测试包 `[47.4.6,)` | 升级时生产包与测试包需要同步检查 |
| 主候选包 | `build/libs/MechanicianCombat-1.20.1-forge-0.1.0-porting.jar` | 本轮直接复算 SHA-256，与文档一致 |
| 主包大小 | 966,487 字节 | 与既有证据一致 |
| 主包 SHA-256 | `0617C4E50DD627B0AEE7704CBE211787444339E177D8096E3DCCCC5F5F710290` | 只标识现有 47.4.6 候选包，不能沿用于未来新包 |
| 测试包 | 同目录的 `-gametests.jar`，10,839 字节 | 测试模组不作为正式交付依赖 |
| 网络 | `ModNetwork` 已有 0–12 共 13 个固定消息 ID、方向和协议检查 | 不是“消息尚未移植”；仍需行为与异常请求验收 |
| 手持风弹 | 当前实现速度 1.5、冷却 10 tick | 属于当前实现及参考观察值，尚需对照原版核实，不列为用户额外确认的数值决定 |
| 我方 Mixin | 当前配置 5 个公共项、14 个客户端项，共 19 个 | 源配置 20 项中另一个行为改用 Forge 事件，不以数量不同直接判定漏移植 |

旧的 `238 errors / 31 warnings` 是更早的编译失败状态，已被后续构建与候选包证据取代，不能继续当作当前结论。

### 2.2 现有证据能证明什么

以下全部属于 **Forge 47.4.6** 的已有证据，本轮未重新执行：

| 检查 | 既有结果 | 不能据此推定 |
| --- | --- | --- |
| JDK 17 clean build | `clean-build-gametest-contract.log` 有 `BUILD SUCCESSFUL`；现有合同入口调用背包转移与弹药环检查 | Gradle `test` 成功不等于完整游戏测试或 UI 通过 |
| 正式映射专服 | 当前主哈希进入 `Done`，正常保存退出 | 无 Mixin 问题、全部玩法通过 |
| 双客户端 | 保留日志记录两名玩家同时进入当前主哈希服务器 | 两客户端副本哈希均已独立确认、远端动作和音效正确 |
| 风弹服务端世界行为 | 正式映射测试模组验证生存使用 2→1、生成一个弹体、命中猪并击退 | 真实客户端按键/网络路径、背包充能结算、远端画面/声音通过 |
| 忠诚兼容 API | 附魔台钩子接受忠诚，铁砧附魔书钩子接受忠诚 III，拒绝锋利 | 实际 UI 可得到结果、返航拾取与持久化正确 |
| 开发 GameTest | 测试代码编译成功；userdev 启动在第三方 Mixin 处失败 | `runGameTestServer` 已执行世界断言 |
| 枪械、忠诚返航等历史演示 | 更早 JAR 曾有部分实测记录 | 旧哈希结果可直接算作当前包或 47.4.28 的验收 |

证据入口：[正式映射验收说明](evidence/2026-10-25/production-mapped-acceptance.md)、[原始运行日志](evidence/2026-10-25/production-mapped-acceptance.log)、[双客户端说明](evidence/2026-10-25/current-hash-0617C4E5-multiplayer-evidence.md)、[客户端显示环境阻断](evidence/2026-10-25/current-hash-client-wind-demo-blocked.md)。

**阶段判断：R0 已具备；R1–R4 已有大量实现和部分验证，严格门槛仍未全部通过；R5 尚未完成。** 不给没有统一统计依据的“完成百分比”。

## 3. Forge 47.4.28 的升级安排

Forge 官方下载页已列出 Minecraft 1.20.1 的 47.4.28，并给出 Gradle 坐标 `1.20.1-47.4.28`。[官方版本页](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)

官方变更日志显示，47.4.6 之后涉及握手包处理、EventBus、注册对象、客户端早期窗口及部分 Mixin 兼容修复。这些改动要求重新检查网络、事件与客户端启动；日志没有证明 Accessories 的既有三个问题会随升级消失。47.4.17 的侧别清理调整针对客户端，不能据此放行专服中的客户端 Mixin 目标。[官方变更日志](https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.28/forge-1.20.1-47.4.28-changelog.txt)

后续使用的唯一验收版本固定为 **47.4.28**；Java 保持 17。Forge 官方 1.20.1 开发文档要求 JDK 17，不因底层 ASM 支持更高 Java 而更改本项目编译目标。[官方开发说明](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/)

| 文件/位置 | 后续计划变更 | 本轮状态 |
| --- | --- | --- |
| `gradle.properties` | `forge_version=47.4.28`，保留 MC 1.20.1、Java 17 | 未修改 |
| `build.gradle` | 沿用变量化 Forge 坐标，检查新 userdev、Mixin refmap 和重映射产物；先不顺带升级插件 | 未修改 |
| `src/main/resources/META-INF/mods.toml` | 将 Forge 最低范围提高到 `[47.4.28,48)`，沿用现有范围形式 | 未修改；只对实测 47.4.28 作兼容声明 |
| `src/test/resources/META-INF/mods.toml` | 同步 Forge 最低版本至 47.4.28，避免测试包仍指向旧基线 | 未修改 |
| `docs/DEPENDENCIES.json` | 只有实际选定并验证不同官方前置版本后才更新锁值 | 未修改 |
| AGENTS、指导、评估、旧计划、安装说明和进展 | 实施时更新“当前状态/目标”，47.4.6 的历史证据保留原版本和原哈希 | 未修改；本报告单独记录新目标 |
| 验收环境与证据 | 新建 `validation/forge-47.4.28/` 和 `docs/evidence/forge-47.4.28/` | 本轮不创建运行环境 |

先对现有插件组合进行目标版本解析和构建，只有明确失败且定位到工具兼容性时再调整固定工具版本。禁止用新 MDK 覆盖现有源码；47.4.28 的新依赖未缓存时不能坚持离线模式并把解析失败当作代码错误。

## 4. 前期阻塞记录及尚存遗漏

本节 4.1 和 4.2 的 Accessories / 标签路径描述记录自 Curios 切换前，属于历史诊断；当前状态与剩余验收以报告顶部及 `docs/evidence/forge-47.4.28/` 为准。

### 4.1 Accessories 兼容性问题（已由用户批准切换 Curios，历史记录）

现有正式服日志记录：

1. `accessories-common.mixins.json:LevelRendererAccessor` 在专服查询客户端 `LevelRenderer`，被 Forge 拒载。服务器仍能启动，但未达到项目“无侧别/Mixin 目标错误”的要求。
2. 内嵌 Object Builder 的 `VillagerTrades` 局部变量表注入不匹配，另有内嵌 Mixin 元数据诊断。不可仅以“非致命”作为通过依据。
3. ForgeGradle userdev 中，内嵌 Fabric API 的 `EntityMixin` 找不到 `f_19853_` 字段，开发客户端与开发 GameTest 被阻断。它与正式映射服能够启动是两种环境结果，不相互矛盾。

已读原始日志分别见 `production-mapped-acceptance.log` 与 `gametest-server-beta46.log`。这些问题属于当前官方前置组合，不能简单认定为我方客户端类泄漏，也不能通过关闭 required、降低 require、屏蔽日志或改第三方 JAR 解决。

以下排查顺序为当时 beta46 排障计划，现已被用户明确批准的 Curios 替换决定取代：

- 第一轮：保持 beta46 和其余五个前置，只升级 Forge 到 47.4.28；同时记录 userdev 与正式映射的实际 Forge/Mixin/refmap 环境。
- 第二轮：若 userdev 仍失败，核对前置重映射、refmap remapping 及 ForgeGradle 启动参数；仅采用有目标版本依据的主项目构建接线修正，不伪造第三方映射。
- 第三轮：若 beta46 仍无法达到门槛，在主项目 `validation/` 子目录对官方 beta45 作隔离对照；先核对发布物、加载器、嵌套依赖和 API，再测试。beta45 只是候选，没有已成功证据。
- beta48/beta49 已有问题记录，不直接当作“更新就更好”的默认升级。其他官方候选只有在元数据和相关修复证据足够时加入对照；每次只改变一个前置版本。
- 仅当候选通过正式客户端、专服、背部槽/渲染、菜单及严格 Mixin 门槛后，才更新主项目依赖锁、构建坐标和 mods.toml 中的真实版本字符串。
- 若官方候选及主项目合法接入方式都不能满足条件，出具最小复现及“等待上游修复 / 经批准另作适配设计”方案。此时如实报告未完成，不能降级验收或静默替换饰品系统。

### 4.2 方块标签路径遗漏（已修正并在目标版服务端加载验证）

历史错误路径为：`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`，内容包含 `tkgun:backpack_modification_table`。现已修正到 `tags/blocks`，并在 Forge 47.4.28 正式映射服务器确认改装台属于 `minecraft:mineable/pickaxe`。

Forge 1.20.1 的方块标签目录应是 `tags/blocks`；单纯解析 JSON 无法发现此类语义错误。实际手持镐挖掘动画、速度和掉落尚未观察。[官方标签目录规则](https://docs.minecraftforge.net/en/1.20.1/resources/server/tags/)

### 4.3 客户端显示环境与验收缺口

旧的正式客户端启动尝试在 GLFW 显示器初始化失败；本次 Curios 开发客户端已越过此前模组 Mixin 错误并完成初始化/资源加载，但 UI 观测环境仍未提供可枚举的 Minecraft 窗口。真实 UI、渲染和交互继续记为未测，不能把环境日志当作画面验收。

仍缺实际附魔 UI、改装台交互防复制、完整弹种、背包/炮塔、远端动作/声音、重生换维度清理等验收。风弹手持代码还应检查 `addFreshEntity` 返回失败时的扣物/冷却分支；这属于静态检查提出的失败路径风险，本轮没有声称已经复现丢物。

### 4.4 文档状态存在滞后

- 指导文档依赖表仍提到 Accessories beta49，而实际构建和锁文件为 beta46。
- 指导和旧计划部分摘要称风弹扣物/命中仍无当前哈希证据，但后续正式映射日志已经验证服务端方法与碰撞；缺的是客户端输入、背包结算及观察者表现，不能继续笼统写“风弹未验收”。
- 旧任务勾选未能完整区分“实现已有、编译通过、运行未验收”。不应重新移植已有 13 个消息或把现有注册/NBT 全部当成未实现。
- 旧源码迁移清单的逐文件状态不能覆盖更新的专项运行证据。后续用“功能、环境、Forge 版本、主包哈希、结果、证据、限制”统一登记。

## 5. 依赖基线

当前锁定版本来自项目 `DEPENDENCIES.json`，本轮没有替换或重新确认它们对 47.4.28 的兼容性。

| 前置 | 当前固定版本 | 后续安排 |
| --- | --- | --- |
| Accessories | `1.0.0-beta46+1.20.1` | 按第 4.1 节排查，必要时官方候选隔离对照 |
| GeckoLib | `4.8.4` | 保持并重验动画、Molang 和渲染 |
| Player Animator | `1.0.2-rc1+1.20-forge` | 保持并重验玩家动作 |
| Shoulder Surfing | `1.20.1-4.21.0+forge` | 客户端安装；重验越肩、瞄准和桥接 |
| Combat Roll | `1.3.3+1.20.1-forge` | 保持并重验事件、按键重绑定、翻滚/滑铲 |
| Cloth Config | `11.1.136+forge` | 保持，满足已有前置需要 |

Accessories 文件名中的 `neoforge` 不能单独作为平台判断；已批准的该发布物使用事实以其元数据和现有 Forge 运行日志为据。候选版本仍逐个核查。官方内嵌模块不拆改，不把参考工程的其他运行库一并搬入。

## 6. 接下来的行动与通过条件

以下均为未来任务，本轮没有执行。每个任务只修复核查或实测暴露的问题，不同时重写无关模块。

### P0：保存当前基线并对齐记录（对应 R0/R1）

涉及：`validation/baseline-47.4.6/`、`docs/PROGRESS.md`、`docs/SOURCE_MIGRATION.json`、上述现行文档。

- [ ] 保存现有主 JAR、依赖锁、源码/配置校验值和原始证据清单；放在 `build/` 外，避免再被 `clean` 删除。
- [ ] 保留历史日志及失败记录；将最新用户要求、47.4.28 目标与第 4.4 节滞后内容同步到工作规范及文档。
- [ ] 将功能登记为“已有实现/构建证据/运行证据/缺项”，不单凭旧 checkbox 重做。

通过条件：能恢复当前 47.4.6 候选状态，所有当前结论可定位到版本和哈希。

### P1：升级 Forge 并重新建立构建、启动基线（对应 R1）

涉及：第 3 节配置文件、现有合同测试、网络核对脚本、`validation/forge-47.4.28/`。

- [ ] 按第 3 节更新生产/测试版本声明；检查 JDK 17 和 Gradle 8.8，按需解析官方 Forge 47.4.28 依赖。
- [ ] 执行一次 `clean build`、`verifyPortContracts` 及网络静态合同核对，检查 reobf JAR、refmap、入口和最终元数据；静态合同不能替代网络行为测试。
- [ ] 分别启动 47.4.28 正式专服、正式客户端和 userdev 客户端/GameTest，记录成功或完整失败点。
- [ ] 逐项处理 Accessories 问题，按第 4.1 节对照候选；只在验证成功后更新依赖锁。

通过条件：目标正式客户端/专服可启动；我方类无侧别泄漏，Mixin 门槛通过；开发测试能够执行，或其明确阻断有等价正式映射测试路径且不掩盖正式环境问题。开发 GameTest 未执行仍必须标注未通过。

### P2：资源、NBT、菜单与存档（对应 R2）

涉及：`src/main/resources/data/`、`item/GunAmmoData.java`、`item/Backpack*`、`screen/AmmoStorageScreenHandler.java`、`screen/BackpackModificationTableScreenHandler.java`、`block/BackpackModificationTableBlockEntity.java`、现有合同测试。

- [ ] 修正方块标签目录；核对配方、掉落、标签、伤害类型的目标版本路径和字段，在世界中检查资源真正被加载。
- [ ] 验证新枪与打空枪、8 槽弹药环、两把枪独立数据、选中槽、枪械弹匣、背包颜色/升级/烟花数据；丢弃捡起、重登和服务器重启后保持。
- [ ] 在实际菜单验证 Shift 点击、交换背包、拖动物品、换手、关闭、断线、离开距离和破坏改装台；检查材料/模块总量，不能复制或吞物。
- [ ] 缺陷先建立能复现行为的最小检查，再做主项目修复；纯规则继续用 JDK，世界与菜单语义用 Forge 测试或实际世界，不新增测试库。

通过条件：资源有游戏内生效证据，物品总量守恒、权限有效、持久化正确；纯规则和真实菜单都通过。

### P3：战斗、弹体、风弹和忠诚（对应 R3）

涉及：`GunStateMachine.java`、`TkGunServer.java`、`net/`、`entity/`、`item/AmmoItems.java`、`item/WindChargeItem.java`、`item/BackpackLauncherRules.java`、`mixin/BackpackLoyaltyPickupMixin.java`、现有风弹测试。

- [ ] 枪械对照 r349 验证腰射 6 发、专注 10 发、手动装填、热量、过热、状态限制与射击节奏；全部实际注册弹种逐一验证伤害、范围和回收。
- [ ] 测试 13 个消息的有效路径及无枪开火、非法槽位/方向、重复请求、过高频率、无效菜单和越权目标；由服务端控制伤害、库存、冷却及广播。
- [ ] 风弹分别覆盖真实手持输入、背包发射、生成失败、实体/方块碰撞、一次性爆发、范围边界、遮挡和同步；检查背包成功只扣 1 弹/1 充能、失败不扣。
- [ ] 按风弹专项方案对照 Java 1.21.1 玩家风弹，逐项检查直接命中伤害、格挡/反弹、相关方块交互、击退抗性、自身助推及落伤保护。当前生成/击退测试不证明这些行为完整，碰撞时一次 `fallDistance` 清零也不能替代完整落伤保护；差异须定位并适配后验收。
- [ ] 忠诚覆盖手持/枪械/背包三条路径、附魔台与铁砧实际操作、等级/NBT 保留、返航拾取、副手优先和背包充能只结算一次。
- [ ] 核对 50% 消失的随机判定代码，并分别验证存活返航与消失分支；单次成功或失败都不证明概率正确。覆盖穿透、满背包、主人死亡/离线和跨维度边界。

通过条件：当前 47.4.28 同一候选哈希的三条发射路径、风弹专项行为、资源消耗及主要异常输入通过；无重复伤害、复制、额外补满弹匣或重复充能。

### P4：动画、前置与双人表现（对应 R4）

涉及：`client/`、`client/rig/`、`CombatRollHook.java`、`mixin/`、`tkgun.mixins.json`、必要动画资源。

- [ ] 在有效桌面显示环境启动两个正式客户端，分别复算主包和必要依赖哈希，与服务器一致；由实际客户端触发动作，另一客户端观察。
- [ ] 对照第一/第三人称、越肩、瞄准切换、普通/细臂皮肤及盔甲，核对持枪、装填、翻滚/滑铲、背包、肩炮和炮塔位置/朝向。
- [ ] 验证 Accessories 穿脱、按键重绑定、Combat Roll 交替与冷却、越肩桥接、HUD、弹药提示框和声音；实际打开提示框触发尚缺证据的 `GunTooltipBackgroundMixin`。
- [ ] 对照源 20 项行为：当前 19 项 Mixin 均有实际目标与触发证据，另一个烟花自伤行为的 Forge 事件路径实际触发验证。
- [ ] 炮塔覆盖目标切换、遮挡、距离、失效目标、滚转、平滑切换和远端可见性；覆盖登录/退出、死亡重生、换维度及重新追踪后的状态清理。

通过条件：双方看到正确动作、弹体、锁定目标和声音；源项目本身标为待验证的炮塔问题也逐项记录，不把原版不确定状态当作移植正确证明。

### P5：最终包与交付（对应 R5）

涉及：`docs/INSTALLATION.md`、`docs/PROGRESS.md`、指导/评估/工作规范、依赖锁、最终验收矩阵及发布候选包。

- [ ] 最后代码变更后重跑受影响测试，执行 Java 17 clean build；同一最终 JAR 再完成正式客户端/专服和关键联机回归。重复构建同源码的哈希一致性需保留日志。
- [ ] 检查最终 JAR 只包含目标生产类/资源，无误打包 Fabric/Yarn 运行接口、第三方 JAR或临时 `tkguntests`；批准前置自身内嵌模块单独登记。
- [ ] 归档最终 SHA-256、字节数、Forge 47.4.28、Java/工具版本、完整前置及哈希、命令/结果、已知问题、操作说明与功能验收表。
- [ ] 核对原作者署名、许可及实质参考代码来源；美术公开分发权限单独核查，不将“候选包验收”自动等同于获准公开分发。

通过条件：所有强制玩法和兼容性门槛有最终版本证据。仍有强制未测项时只交付候选测试包，不宣布完整移植完成；任何削减范围或降低标准另行说明。

## 7. 后续命令与证据规则

下面仅是未来执行清单，本轮未运行：

```powershell
# 工作目录：E:\Server_mod\MechanicianCombat
.\gradlew.bat --version
.\gradlew.bat clean build --no-daemon --console=plain
.\gradlew.bat verifyPortContracts --no-daemon --console=plain
powershell -NoProfile -File .\tools\verify-network-contract.ps1
.\gradlew.bat runGameTestServer --no-daemon --console=plain
.\gradlew.bat runClient --no-daemon --console=plain
.\gradlew.bat runServer --no-daemon --console=plain
```

`build` 已通过 `check` 接入合同检查；只有单独定位合同问题时才重复执行 `verifyPortContracts`。客户端与服务端启动分别操作，任务返回成功不能替代进入世界的日志/画面。userdev GameTest 被阻断时按现有 `gameTestModJar` 与重映射机制在正式隔离服执行测试，但明确记录它不是 userdev 测试通过。

隔离验收只绑定本机回环地址，独立存档、配置、端口；EULA 沿用既有授权范围，仅在隔离目录处理。运行目录和证据放在 `build/` 外，新旧版本不共用真实服务器目录。缓存或证书/网络失败保留原错，不把关闭证书检查作为默认步骤。

每份结果至少包含：日期、源码/配置清单、Forge/Java、主包及前置哈希、映射环境、触发动作、服务端结果、客户端/观察者结果、原始日志、未覆盖范围。换 Forge、前置或生产 JAR 后，受影响的通过项必须重验；即使主包哈希相同，47.4.6 的运行结果也不升级为 47.4.28 的证据。

## 8. 本轮交付与下一步入口

本轮早期状态以当时记录为准；后续恢复实施与验收补记见文末。不要从头批量转换源码，也不把再次构建成功当作完成。

【前端变前台，后端变后厨，python送到家，Java炒米粉】

## 2026-10-26 实施恢复后的最新验收补记

- 新增的验收逻辑已覆盖手持、枪械、Curios 背包 Loyalty III 弹体的真实石墙碰撞、50% 方块命中消失重试、存活弹自然回飞和正常 owner/playerTouch 拾取；背包来源登记、一次回收/一次充能恢复、非冷却充能与冷却恢复均通过 Forge 47.4.28 正式映射隔离专服运行。
- 该验收模组修正过具体类继承方法在混淆 GameTest JAR 中的符号链接错误，验收脚本的旧文案断言也已同步；没有通过掩盖 Mixin/运行时错误来得到 PASS。
- Forge 47.4.28 / Java 17 prepare、正常保存关闭、第二进程读取世界 verify 全部 PASS。枪械弹药环 8 槽数据/选中槽、枪械弹匣与背包核心/自动发射器 NBT 通过跨进程保存读回。生产 JAR SHA-256 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`，973,544 B；验收 JAR SHA-256 `B066BEB2D8B3E8644EF5FF52C1D7EBFBC017AA0CD686AFAB31D0AC1BE4C120A6`，44,889 B。
- 证据：`docs/evidence/forge-47.4.28/final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`。状态已同步到 `docs/evidence/forge-47.4.28/acceptance-matrix.md`、`docs/PROGRESS.md` 和 `dist/README-交付候选.md`。
- 移植仍未完成：枪械灰白几何根因与渲染验收、观察者端模型/弹体/粒子/声音同步、手动菜单与关闭/断线安全、客户端完整输入及动画矩阵、开发 `runGameTestServer` 阻断项仍未解决/验收。此产物仍是候选，不能标记为最终交付。

### 正式源码恢复与目标版复建

- 两轮临时骨骼跳过诊断结束后，已撤除 `BowgunGunLayer` 临时代码并恢复联机验证脚本。诊断只排除了 case/change/弹匣/locator 组与 `barrel` 本体；两端画面中的 U 形灰白几何仍在，剩余根因未定位。证据：`docs/evidence/forge-47.4.28/render-remaining-bones-diagnostic/result.md`、`render-barrel-bone-diagnostic/`。
- 恢复正式源码后再次执行 Java 17 `clean build gameTestModJar` 成功，主 JAR 与 dist 哈希均为 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`，测试 JAR 为 `B066BEB2D8B3E8644EF5FF52C1D7EBFBC017AA0CD686AFAB31D0AC1BE4C120A6`。服务器/双客户端 PASS 证据均绑定相同主哈希；专服验收测试 JAR 与当前重建哈希相同。
- 7890 代理 JVM 参数下缓存离线构建成功；不带该配置的 ForgeGradle 环境连接检查在 TLS 阶段等待，期间没有删除依赖或跳过 Mixin/兼容性检查。




## 2026-10-26 后续验收补记

手持 Loyalty 已从传送式拾取断言升级为真实发射、方块命中、随机消失重投、自然回飞和正常 `playerTouch` 拾取；目标版隔离专服两进程验收通过。生产主 JAR 未变，哈希仍为 `E168EDD1EBAFF3E5E0BCE7B17E5F7C808408D1BC332F177AA8BA592861A25B1A`。GLFW 当前探针已检测到一个显示器，一客户端完成弹药 UI/动画截图，第二客户端握手阶段断开，且截图仍见灰白枪械几何。完整移植交付仍未达成。详细范围与证据见 `docs/evidence/forge-47.4.28/acceptance-matrix.md` 和 `docs/evidence/forge-47.4.28/final-multiplayer/result.md`。

## 双客户端复验更新（2026-10-26）

针对双开登录握手超时，先以 H 单客户端连接确认目录与依赖无差异，再在两套隔离客户端临时降低图形负载并恢复原配置。正式 E168 主 JAR 的 G/H 两客户端随后同时加入同一目标版隔离专服，弹药环生产菜单与 Focus_shooting 截图均通过。截图仍有灰白几何与 H 朝地视角；对方观察视角同步、弹体与音效仍未验，完整移植状态仍为未完成。证据见验收矩阵及 `docs/evidence/forge-47.4.28/final-multiplayer/`。

## 最终复建记录（2026-10-26）

手持 Loyalty 自然回收验收器定稿后执行 `clean build gameTestModJar` 通过；紧接着以该构建进行 Forge 47.4.28 / Java 17 隔离专服 prepare、保存退出与第二进程 verify，结果 PASS。正式主包哈希仍与 dist 完全一致：`E168EDD1EBAFF3E5E0BCE7B17E5F7C808408D1BC332F177AA8BA592861A25B1A`。此处只确认构建与服务器范围，不代表完整客户端视觉/远端同步验收完成。

## 2026-10-26 后续继续：渲染矩阵与目标版复验

- 修复 `BowgunRigRenderer` 胸部步行 bob 额外平移未配对 PoseStack push/pop 的同级骨骼矩阵泄漏；执行 `clean build gameTestModJar`，Forge 47.4.28 / Java 17 正式映射隔离专服 prepare 与第二进程存档 verify 均通过。
- 新生产 JAR SHA-256：`2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`（973,544 B）；同哈希 G/H 双客户端通过 Forge 握手加入同服、打开生产弹药环并记录 Focus_shooting 截图。
- 胸部 bob 矩阵修复后，Focus_shooting 截图中的灰白枪械几何仍可复现；这项修复不构成视觉问题修复。自动演示脚本在后续仰角阶段重设相机，H 的某些晚期截图不能代表稳定手持视角；当前 UI/动画截图只能证明场景到达和渲染帧保存。
- 修正专服验收 PowerShell 对 Java 空退出码的误判，以及双客户端脚本在 Windows PowerShell 5.1 的 `Join-Path` 调用错误。原始输出与 PASS 结果见 `docs/evidence/forge-47.4.28/final-server-acceptance.txt` 和 `docs/evidence/forge-47.4.28/final-multiplayer/final-multiplayer-acceptance.txt`。
- 移植仍未完成：枪械异常根因、观察者端模型/弹体/粒子/声音同步、真实菜单手动/关闭/断线安全、枪械与背包自然 Loyalty 返还、全玩法矩阵与最终交付验收尚缺。

## 当前状态更正（2026-10-26，优先于本文此前状态段落）

最近一次正式源码 `clean build gameTestModJar` 已成功，日志为 `docs/evidence/forge-47.4.28/final-clean-build-official-source.log`。主 JAR 和 `dist/` 的 SHA-256 均为 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`；GameTest JAR SHA-256 为 `B066BEB2D8B3E8644EF5FF52C1D7EBFBC017AA0CD686AFAB31D0AC1BE4C120A6`。同哈希目标版隔离专服两进程验收与双客户端共服/UI/动画到达证据通过，详细范围以 `docs/evidence/forge-47.4.28/acceptance-matrix.md` 为准；两客户端并未完成观察者侧同步验收。

灰白枪械几何仍在，临时逐骨骼染色诊断两次被 GLFW 主显示器初始化错误阻断，未取得诊断帧/骨骼日志，根因未定位。真实菜单手动操作、断线/关闭安全、听感、翻滚滑铲完整动作及观察者模型/弹体/粒子/声音同步等仍无足够证据。故当前状态为**移植未完成**；JAR 仍属验收候选。不要把旧 Forge、旧哈希或开发 GameTest 阻塞与目标版专服 GameTest 模组验收混为一谈。

## 当前最新推进（2026-10-26）

围绕截图中的灰白枪械几何，读取当前模型/皮肤层/枪械层、r349 同名 rig 渲染器和 GeckoLib 4.8.4 的 `GeoRenderer.renderRecursively` 字节码。测试包没有盔甲，皮肤第二层和已做过的多组枪件跳过均未移除灰块；现有证据只能限定在被接管的 rig 渲染链，不能证明具体骨骼。再次逐骨骼着色时，尽管 GLFW 探针报告两个显示器，Forge Early Display 仍报 `Can't find a primary monitor`，没有生成诊断帧。诊断代码和 JVM 参数均已恢复；最新正式源码 `clean build gameTestModJar` 成功，主 JAR 哈希 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`，与同哈希服务器和双客户端历史验收产物完全一致。灰块、真实手动 UI、安全关闭/断线、观察者同步、听感和剩余动作矩阵仍未验证，项目保持未完成状态。

### 2026-10-26 续作验收补记

正式候选在隔离双客户端复跑中通过同服加入、生产弹药环菜单打开和两端 Focus_shooting 画面捕获；当前哈希为 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`。针对仍存在的灰白几何，临时枪械层逐骨骼染色检查显示 gun-layer 枪体已变色、异常形状未变色；因此该视觉缺陷继续阻止完整交付，下一步重点转向主 rig 身体层/远端玩家渲染，不再把之前的早显卡初始化失败当成当前复测结论。临时代码已撤除并 clean 重建正式 JAR。详情见 `docs/evidence/forge-47.4.28/acceptance-matrix.md` 与 `render-bone-tint-diagnostic/result.md`。

### 身体层诊断复试补记（2026-10-26）

枪件骨骼染色显示灰白形状不是枪械层直接绘制后，下一轮尝试对主 rig 六个身体骨骼着色。Forge Early Display 再次间歇性报 `glfwGetPrimaryMonitor failed`，客户端未加载模组，故身体层来源仍未能证实。已撤除本次诊断并 clean build，正式产物和全部验证安装目录重新对齐到 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`。记录：`docs/evidence/forge-47.4.28/render-body-tint-diagnostic/result.md`。目标仍为完成迁移；模型异常、Observer 同步和真实 UI/动作行为矩阵待继续完成。

### 灰白模型排查追加（2026-10-26）

正式双客户端再次通过同服、弹药环 UI 与 Focus_shooting 截图。之后的隔离测试显示：只隐藏 BowgunRigRenderer 的六个主体骨骼会让异常的大块几何显著减少，但留下零散部分；说明身体层有贡献，且其他渲染层可能重叠。隐藏身体是诊断手段，会丢失正常玩家模型，未作为修复。单独关闭第二皮肤层时原异常主体仍存在；组合关闭身体和第二层因 Early Display 主显示器错误未完成。代码恢复并再次 clean build；正式产物与 dist/专服/G/H 安装目录哈希为 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`。根因、视觉修复及完整验收仍未完成。证据：`docs/evidence/forge-47.4.28/render-hide-body-diagnostic/result.md`、`render-hide-body-and-skinlayer-diagnostic/result.md`。

### 2026-10-26：越肩透明度 A/B 复测（未进入客户端）

为判断 Shoulder Surfing 的 `adjust_player_transparency` 是否造成枪械灰白几何，在隔离客户端将其暂设为 false；Forge 47.4.28 / Java 17.0.14 隔离专服与正式 JAR 均正常启动，但客户端再次在 Early Display 因 `glfwGetPrimaryMonitor failed` 退出，未连接、未加载模组、无可比较截图。本次对透明度影响**无结论**。仅停止该次隔离 javaw，客户端配置已恢复原哈希 `6A877C2741058D03468FFBD0BCA0BE4AA732986B2BB655A50CB981616BBE7015`，验收脚本证据路径已恢复；服务器属性、验收 JAR 及正式 JAR 均复核还原。记录见 `docs/evidence/forge-47.4.28/render-shoulder-transparency-off/result.md`。正式 JAR 未变（SHA-256 `2AC7A0D07F28CE4661D6474DEA19AAD714D33CE6271D741578377C9BB395B1D`）。项目继续标记为移植未完成。

### 灰白主体静态来源排除（2026-10-26）

复核验收脚本和正式代码：`/tkgun kit` 仅写入武器槽和 64 枚弩矢，不给玩家穿戴盔甲；`BowgunArmorLayer` 仅在对应 EquipmentSlot 真有 ArmorItem 时绘制。因此验收截图里的白灰主体不能由该验收命令授予的盔甲解释。`BowgunRigModel` 主体贴图动态读取当前玩家皮肤，`BowgunRigRenderer.renderCubesOfBone` 绘制六个玩家身体骨骼；此前只跳过这六根骨骼的成功截图对照显示灰白大块明显减少但仍有残片。这把排查优先级收敛到主体几何/玩家皮肤贴图与剩余 rig 绘制路径，但还没有根因结论。关闭 Shoulder Surfing 透明度的客户端 A/B 被 Forge Early Display 阻断，不能据此排除其影响。源码：`src/main/java/com/mechaniciancombat/client/rig/BowgunRigRenderer.java`、`BowgunRigModel.java`、`BowgunArmorLayer.java`、`src/main/java/com/mechaniciancombat/TkGunCommand.java`。

### 2026-10-26：自动背包发射间隔和切换验收

- **验收实现**：扩展隔离正式映射服务端验收器，让 Curios AUTO 背包面对两个可见目标，断言首目标是最近者；在首发后模拟每个服务端 tick，确保 20 tick 间隔内不多发；恰到间隔时第二发只扣一枚烟花与一格充能并轮换目标；停止输入不追加发射。
- **构建**：`gradlew --offline gameTestModJar --no-daemon --console=plain` 成功（Java 17；代理端口 7890）。验收 JAR SHA-256 `91AB976A20AEB6838903246D5F91AAF7A037CB5522DD9CCA24E90F446D335ADA`，45,530 字节。正式生产 JAR 未改，仍是 `2AC7A0D07F28CE4661D6474DEA19AAD714D33CE6271D741578377C9BB395B1D`。
- **运行**：更新后的 GameTest 验收器在 Forge 47.4.28 / Java 17.0.14 正式映射隔离专服完成 prepare、保存退出、第二进程 verify；两轮日志均有新的 interval/target-rotation PASS，最终汇总 `status=PASS`。脚本运行后恢复原服务端配置和验收 JAR 文件位置。
- **证据**：`auto-backpack-interval-build.log`、`auto-backpack-interval-server-runner.log`、`final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`。
- **边界**：这是专服生产逻辑验收，不证明真实客户端 UI/动画、目标遮挡切换、冷却归零及观察者同步；完整移植仍未完成。

### 自动背包炮塔遮挡与定时切换复验（2026-10-26）

- **验收器构建**：再次执行 `gradlew --offline gameTestModJar --no-daemon --console=plain` 成功。更新测试 JAR SHA-256 `95A62DD70202DE5AA7F017392559125F7DA092DD98ACEFA2085D3BC408A54488`（45,862 字节）；生产 JAR 不变，SHA-256 `2AC7A0D07F28CE4661D6474DEA19AAD714D33CE6271D741578377C9BB395B1D`（973,544 字节）。
- **Forge 47.4.28 / Java 17.0.14 专服**：更新后的验收器 prepare、保存退出、verify 第二进程三阶段通过。AUTO 模式用两只猪测最近目标、精确 20 tick 间隔、两次命中后轮换；随后以方块遮住侧目标，确认它不在冷却间隔内多发，按下次攻击时刻改打无遮挡目标；三发合计仅消耗三枚烟花、三格充能，停止无重复弹体。跨进程枪械与背包 NBT verify 仍通过。最终汇总 `status=PASS`，正式 JAR hash未变。
- **证据**：`docs/evidence/forge-47.4.28/auto-backpack-occlusion-build.log`、`auto-backpack-occlusion-server-runner.log`、`final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`。
- **边界**：目标死亡/消失、AUTO 冷却与完整充能周期、便携模式从零充能自然恢复、客户端表现/观察者同步仍未测；该进度不等同完整移植验收。

### 便携发射器自然充能与冷却闭环验收（2026-10-26）

- **验收器**：去掉原先把便携发射器电量直接写为 3 的测试捷径；从新装 0 格状态逐 tick 推进到各个截止点，断言每 40 tick 恰好增加一格并最终停在 3 格。目标标记不消耗弹药/能量；三次连续有效发射逐次扣一枚风弹和一格能量；第三次发射后 100 tick 冷却期间保持 0 格，截止点恢复到满格。
- **构建**：`gradlew --offline gameTestModJar --no-daemon --console=plain` 成功；新版验收 JAR SHA-256 `C2B096A7DF011FBE046BF93B97607C5C17151ED3700B8D81D352A59B5852725D`（46,647 字节）。正式 JAR 未修改，SHA-256 `2AC7A0D07F28CE4661D6474DEA19AAD714D33CE6271D741578377C9BB395B1D`。
- **运行**：Forge 47.4.28 / Java 17.0.14 正式映射专服 prepare、保存退出、verify 第二进程全部通过；最终 `status=PASS`。prepare 和 verify 两轮都记录便携的 0→3 自然充能、三发扣费、100 tick 冷却与满格恢复，以及上轮新增 AUTO 间隔/旋转/遮挡路径。专服配置已复原。
- **证据**：`portable-backpack-cycle-build.log`、`portable-backpack-cycle-server-runner.log`、`final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`。
- **边界**：客户端实际菜单/动画、观察者同步、AUTO 死亡目标及其自然冷却循环仍未测，移植仍未完成。
