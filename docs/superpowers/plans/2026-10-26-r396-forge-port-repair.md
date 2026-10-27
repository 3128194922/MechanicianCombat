# MechanicianCombat r396 Forge 移植与修复 Plan

> **执行说明：** 当前仅为方案，未修改实现代码。后续执行使用 `superpowers:executing-plans`，按任务逐项实现、验证并记录证据。

**Goal：** 将主项目补齐到最新 r396 的功能和资源基线，使用内置越肩相机，解决移动方向、瞄准锁死和弹药背景问题，完整接入铸造傀儡及关联玩法。

**Architecture：** 按 r396 的独立相机 yaw/pitch、鼠标输入、身体朝向与动作状态的职责移植；Forge 事件和必要的客户端 Mixin 承担 1.20.1 接入。游戏结果仍由服务端决定，GeckoLib 负责模型和动画；已批准的 Curios 背包集成继续保留。

**Tech Stack：** Minecraft 1.20.1 / Forge 47.4.28 / Java 17；主项目现有 GeckoLib、Curios、Cloth Config；Combat Roll 按 r396 作为可选集成。

**Spec：** 本文第 1 节及用户 2026-10-26 的最终指定：“移植内容实现以最新 r396 为准，内置越肩相机”。此决定覆盖此前 r349 + Shoulder Surfing 4.21.0/5.2.0 路线。

## 1. 范围与约束

- 唯一可修改项目：`E:\Server_mod\MechanicianCombat`。
- 最新只读参考：`E:\Server_mod\MechanicianCombat-r396-source\MechanicianCombat-source-r396`。
- r349 只用于历史差异追踪，不再作为功能完成标准。
- 最终目标固定为 Minecraft 1.20.1 / Forge 47.4.28 / Java 17，不引入 Fabric 兼容加载层。
- Java 根包保留 `com.mechaniciancombat`，禁止包含 `example`；模组和资源 ID 保留 `tkgun`。
- 不新增未批准的第三方依赖，不将 1.21 的 owo-lib JAR 直接装入 Forge 1.20.1。内置相机源码本身不依赖 owo API；目标依赖按实际调用与传递依赖核查。
- Curios 替代 Accessories 的既有批准继续有效；保留已实现的风弹、忠诚附魔及存档语义。
- 参考目录及原始 bbmodel 只读，不运行参考目录内会生成文件的脚本，不覆盖式重跑旧迁移工具。
- 客户端、专服、多人、画面效果分别验收；编译或脚本通过不能代替真实键鼠操作与视觉验收。
- 安装用户游戏目录、升级已有存档等操作不在本轮 Plan 内；验证目录全部位于主项目内。

## 2. 已核实的事实与未确定项

| 项目 | 代码核查结论 | 处理方向 |
| --- | --- | --- |
| 参考版本 | r349 将 Shoulder Surfing 列为必需；r396 的 `SOURCE_HANDOFF_r396.md` 和 `fabric.mod.json` 已将其改为可选 | 按 r396 实施内置相机，以不安装 Shoulder Surfing 为主要验收环境 |
| 内置相机 | r396 新增 `FallbackShoulderCamera` 及相机、鼠标、跑步输入三个 Mixin；当前主项目缺少这些实现 | 整条输入与相机链路移植，不能只改相机位置 |
| WASD | Shoulder Surfing 4.21.0 确实会按相机/身体角度变换输入并修改身体角度；当前又有自身角度控制 | 属于优先排查链路；尚未实机证明是本次唯一根因，不直接断言“按键映射坏了” |
| 瞄准锁死 | 当前存在相机采样、身体转向、耦合策略与动作限制的交互 | 先记录输入与角度时序，再用 r396 的相机独立状态恢复控制权 |
| 弹药背景 | 原实现调用 GUI 精灵接口；主项目把 `tkgun:ammo_info` 直接传入普通 `blit`，实际 PNG 在 `textures/gui/sprites/ammo_info.png` | 路径错误且缺少九宫格适配；PNG 与 r349 文件哈希一致，问题不在贴图缺失 |
| 九宫格 | `ammo_info.png.mcmeta` 声明 96×32、边框 8 像素 | 1.20.1 使用真实纹理路径及等价九宫格绘制，不照搬 1.21 GUI 精灵接口 |
| 傀儡 | r396 有 `MoltenGolemEntity`、GeoModel、Renderer、粒子、模型、动画、声音，以及服务端召唤天线流程；主项目缺失 | 实体、获取/召唤、AI、伤害、同步、保存与表现一起移植 |
| 其他增量 | r396 同时修改背包状态、消息、弹体、准星、模型动画，新增冷却重置及实体移除工具 | 按完整差异表逐项闭环，不能用“Java 文件复制齐了”当作完成依据 |
| 文档准确性 | r396 包内旧 `SOURCE_HANDOFF.md` 仍写 r349 | 以 r396 专用交接说明、VERSION、实际源码和元数据交叉核实 |

本轮未运行客户端复现，用户实际加载的主模组及相机前置版本也尚未采集。联网文档查询连接失败；上述事实来自本地源码、元数据及现有 4.21.0 JAR 的接口/字节码。实施前仍需核对目标版实际 API，不能照搬 Yarn/intermediary 方法名。

## 3. 必须重点回归的五类条件

1. 普通移动、瞄准移动、疾跑转向不能共用错误的输入覆盖分支；相反按键和斜向输入要有明确结果。
2. 换枪、菜单、死亡、重生、断线和换维度后，相机及输入状态不能残留。
3. 贴墙、低帧率、改变 GUI 缩放时，相机过渡、准星和 HUD 仍可用。
4. 傀儡攻击和落地伤害不能重复结算；主人和傀儡身份、冷却与攻击阶段跨保存后不能混乱。
5. 未安装可选模组时客户端和专服都能启动；可选集成的类和 Mixin 不得强制解析不存在的前置。

## 4. 执行顺序与任务

所有源码路径均以 `E:\Server_mod\MechanicianCombat\src\main` 为根；测试位于同项目 `src\test\java\com\mechaniciancombat`；证据写入 `docs\evidence\r396-port`。

### P0：锁定 r396 差异与问题复现

**文件：** `docs/PORTING_GUIDE.md`、`docs/PROGRESS.md`、`docs/DEPENDENCIES.json`、新增 `docs/evidence/r396-port/feature-matrix.md`。

- [ ] 保存当前主项目源码/构建配置/资源及现有候选 JAR 的哈希清单，保留现有改动。
- [ ] 以 r396 为基准制作 Java、注册 ID、数据字段、消息、资源、行为差异表；Java 包名和映射差异归一化后审查内容，不直接覆盖 Forge 适配。
- [ ] 登记 r396 新增和同名修改内容，特别是 `GunStatePayload`、`CombatBackpackStatusPayload`、弹体命中规则、背包改装容器和模型动画。
- [ ] 在主项目独立验证目录复现用户问题；记录实际加载 JAR 哈希、Forge/Java/前置版本和相机配置。
- [ ] 诊断按键原始值、最终前后/左右输入、相机 yaw/pitch、实体 yaw、身体 yaw、位移与所选动画，区分“位置始终向前”和“动画始终向前”。

**通过标准：** 每个 r396 功能都有主项目对应项与状态；每个已报告问题有复现步骤，无法复现则明确缺少的条件，不编造根因。

### P1：移植内置相机，建立独立鼠标视角

**新增：** `java/com/mechaniciancombat/client/FallbackShoulderCamera.java`，`mixin/FallbackShoulderCameraMixin.java`、`mixin/FallbackShoulderMouseMixin.java`、`mixin/FallbackShoulderRunMixin.java`；按加载需要增加专用可选集成选择器。

**修改：** `client/TkGunClient.java`、`client/GunCamera.java`、`client/ShoulderCouplingState.java`、`resources/tkgun.mixins.json`、`build.gradle`、`resources/META-INF/mods.toml`、依赖清单。

**接口：** 将 r396 的 `tick(Minecraft)`、`turn(LocalPlayer,double,double)`、`steerRunningInput(Minecraft,KeyboardInput)`、`computePose(BlockGetter,Entity,float)`、`leave(Minecraft)` 和相机角度读取接口映射到 Mojang 1.20.1；具体类型和签名以本地目标类核实为准。

- [ ] 先加入相机角度/激活退出的回归用例：鼠标水平增量改变相机 yaw；pitch 限制 ±90°；非本地玩家和非持枪状态不接管。
- [ ] 核对 1.20.1 `Camera.setup`、相机位置/旋转方法、`Entity.turn`、`KeyboardInput.tick(boolean,float)` 的实际签名、侧别和 refmap，再接入 Mixin。
- [ ] 复用 r396 相机参数：普通距离/侧偏/高度 `3.2/0.68/0.18`，瞄准 `2.35/0.52/0.12`，动作 `3.55/0.58/0.32`；过渡常数 `0.10 s`、碰撞余量 `0.15`。
- [ ] 内置相机独立保存 yaw/pitch；本模组控制身体跟随和瞄准，不将身体转角反向覆盖相机角度。核对逻辑 tick 与渲染采样顺序，防止读取旧相机角度后持续回写锁死。
- [ ] 把 Shoulder Surfing 从必需前置移除，内置相机的主要验收环境不安装它。保留 r396 的可选集成边界；安装该前置时按 r396 交接规则交接控制权，禁止两套相机同时运行。
- [ ] 检查实际引用后解除 Player Animation Lib 的必需依赖；Combat Roll 按 r396 设为可选，硬 API 引用隔离到桥接类，未安装时跳过专属 Mixin。不得通过整体关闭 Mixin 严格检查掩盖问题。
- [ ] 测试换物品、开关界面、第一/第三人称切换、死亡/重生、断线重连、换维度时的状态清理和原视角恢复；空手与其他玩家不受影响。

**通过标准：** 未安装 Shoulder Surfing/Player Animation Lib/Combat Roll 的客户端和专服可启动；持枪鼠标可连续左右、上下旋转；右键瞄准时视角不锁死；相机位置、过渡和碰撞与 r396 对照。

### P2：按 r396 恢复移动与瞄准规则

**修改：** `client/TkGunClient.java`、`client/GunCamera.java`、`client/GunClientState.java`、`client/ShoulderCouplingState.java`、相机跑步 Mixin、`mixin/FocusMovementLockMixin.java`；按差异审查同步 `GunStateMachine.java` 与相关动作限制。

**接口：** P1 产生的相机独立角度和运行状态是输入；实际位移用于动画采样，动画不得反向决定普通移动向量。

- [ ] 建立失败用例：普通 W/S/A/D 四方向、相反按键抵消、斜向输入；记录身体和相机不同时的实际位移与动画。
- [ ] 普通腰射/瞄准移动保留原始前后左右含义，四方向动画由真实位移与身体基向量选取。
- [ ] 按 r396 保留疾跑/加速阶段特例：A/D 用于身体转向，侧向输入清零，偏转目标为相机左右最多 `45°`，每 tick 转向最多 `4°`。此规则只作用于跑步/加速分支，不能污染普通 A/D 横移。
- [ ] 专注、翻滚、滑铲、过热的移动与右键限制按 r396 状态和时序移植；不要为修 WASD 全局取消这些玩法限制。
- [ ] 核对相机、身体、枪口 locator、准星射线、实际弹着点的数据方向；迁移 r396 准星与命中反馈，隐藏重复原版准星。
- [ ] 对照 r396 验证瞄准扇区与动作锁，不把有意保留的扇区限制误删；相机自身可自由转向。
- [ ] 真键鼠验证站立、普通移动、右键移动、疾跑、翻滚/滑铲恢复；两名玩家分别操作，观察者检查身体、枪械和弹道方向。

**通过标准：** 普通 WASD 不再全部表现为 W；普通横移和疾跑转向各自符合 r396；持枪进入/退出瞄准及动作状态后没有永久锁角或转向抖动。

### P3：恢复弹药贴图背景与 r396 HUD

**修改：** `client/GunHud.java`、`client/GunReticle.java`、`client/CombatBackpackHud.java`；按需新增 `client/GuiNineSlice.java`，保留现有 PNG。

- [ ] 将弹药背景资源改为真实路径 `tkgun:textures/gui/sprites/ammo_info.png`。
- [ ] 核查 Forge 1.20.1 的九宫格绘制接口；用其或主项目内的小型绘制方法等价实现源图 `96×32`、边框 `8`，不依赖 1.21 的 GUI 精灵元数据解释机制，不新增 UI 库。
- [ ] 校核 UV、真实纹理尺寸、透明混合与文字层级，不使用当前 `256×256` 假定值裁错图片。
- [ ] 同步 r396 的弹着点/命中准星、背包/召唤天线 HUD 和语言资源。
- [ ] 在多种 GUI 缩放、分辨率、长弹药名称、空弹匣、装填、热量/专注状态下截图对照；边框完整，文字不越界。

**通过标准：** 使用原素材显示完整背景、九宫格边框与正确文字；无缺失纹理提示；视觉对照通过后才标记完成。

### P4：完整移植铸造傀儡与召唤天线

**新增：** `entity/MoltenGolemEntity.java`、`CombatTargeting.java`、`client/MoltenGolemGeoModel.java`、`client/entity/MoltenGolemEntityRenderer.java`、`client/GolemEnergyParticle.java` 及 r396 对应资源。

**修改：** `registry/ModRegistries.java`、`TkGunServer.java`、`TkGunParticles.java`、`client/TkGunClient.java`、背包客户端状态/渲染/HUD、背包部件/升级数据/改装菜单、相关消息及弹体命中规则。

- [ ] 先加入实体注册、属性、状态同步和 NBT 读写的失败用例，再实现 Forge 注册与 `SynchedEntityData`；保留 `molten_golem` ID、尺寸 `1.4×2.7`、最大生命 `400`。
- [ ] 迁移 r396 召唤天线类型值 `3`、部件安装/卸下、背包改装菜单、目标标记、施法、生成及已有本主人傀儡的重定位流程；通过 Curios 实际装备背包触发，刷怪蛋只作为另一入口。
- [ ] 保留召唤准备 `20 tick`、施法 `60 tick`、长冷却 `1200 tick`、对应分支短冷却 `300 tick`，具体触发分支逐条对照源代码，不自行重新解释数值。
- [ ] 实体保留落地 `70 tick`，跟随主人，三种攻击与技能冷却；攻击时长/伤害帧分别 `20/8`、`40/24`、`58/38 tick`，技能二/三冷却 `100/400 tick`。
- [ ] 伤害、范围、击退、落地爆发、免伤、主人绑定与目标过滤逐项按 r396；伤害仅服务端结算，不依赖客户端 locator 作为权威数据。
- [ ] 保留并适配 Owner、SummonTicks、Atk2Cooldown、Atk3Cooldown、AttackElapsed、AttackDamageDone、AttackKind、VisualState 的存档语义；验证攻击中保存读回不重复结算。
- [ ] 迁移模型/动画/贴图、`locator_right` 粒子采样、四个声音和对应注册；客户端视觉采样不进入专服逻辑。
- [ ] 用独立世界验证：天线召唤、无目标回退、目标消失、重复召唤/重定位、主人离线、区块卸载/读回、两位主人隔离、三种攻击、同模组弹药/炮塔的傀儡过滤，以及双客户端动画/声音同步。

**通过标准：** 生存玩法链路能从背包安装天线走到召唤和战斗；生成、攻击、保存与多人观察均通过。仅 `/summon` 成功不算完整傀儡移植。

### P5：闭合 r396 其余差异与整体验收

**修改：** P0 差异表涉及的同名业务类、资源和元数据；新增 `item/BackpackCooldownResetItem.java`、`item/EntityRemovalToolItem.java`；按 r396 适配 `CombatRollBridge.java`。

- [ ] 移植冷却重置工具、实体移除工具及其真实源行为、权限/使用条件、注册和资源。
- [ ] 逐项合并 r396 的枪械状态、消息字段、弹体命中/爆炸粒子、背包炮塔/召唤动画与目标排除规则；补齐差异表，不遗漏“文件名相同但内容已变”的代码。
- [ ] 消息仍采用现有 Forge `SimpleChannel`；字段变化同步编码/解码/处理并更新协议兼容规则，明确拒绝不兼容客户端。保留服务端输入校验与客户端类隔离。
- [ ] 不装可选前置验收基础玩法；另装现有已批准 Combat Roll 对照翻滚/滑铲。Shoulder Surfing 只作可选兼容检查，不作为内置相机完成条件。
- [ ] 在 Java 17 下执行主项目 `gradlew.bat clean build verifyPortContracts`，运行适用的 GameTest 和现有网络契约检查；检查资源引用、Mixin refmap 和专服类加载。
- [ ] 使用本次构建的同一哈希 JAR，完成专服启动、独立客户端启动、真实键鼠操作、双人联机、存档重启与资源截图/听音验收。
- [ ] 回归 Curios 背包、风弹、忠诚弩矢、装填与弹药菜单、背包改装、自动炮塔和异常断线；已有通过证据仅能作历史对照。
- [ ] 更新 `AGENTS.md`、`PORTING_GUIDE.md`、`PROGRESS.md`、安装说明、依赖清单和验收矩阵，以 r396 和实际结果统一文档。
- [ ] 交付带 Minecraft/Forge/移植版本标识的 JAR、SHA-256、必需/可选前置清单和未通过项。仅全部必达项通过后标记完成；其余分别注明未运行、失败或阻塞。

**通过标准：** P0 差异表每一项都有实现与验收结论；不能将 r396 资源存在、构建通过或旧版运行截图替代本次玩法验收。

## 5. 阶段交付与停止条件

- 第一阶段交付：P0–P3，先恢复玩家可移动、可旋转瞄准、可见弹药背景，保留真实操作和画面对照证据。
- 第二阶段交付：P4，傀儡与召唤天线完整玩法闭环。
- 第三阶段交付：P5，完成 r396 其余增量、回归、文档和最终候选包。
- 若发现 1.20.1 无等价 API，或必须新增依赖、改变玩法或删除 r396 功能，停止该项实施，说明具体障碍并给用户多个方案；可继续其他无依赖任务。
- 当前计划不自动删除可选前置文件、不改参考项目、不安装到用户现用游戏目录。

## 6. 计划自检

- 已覆盖用户提出的 WASD、第三人称差异、弹药背景、傀儡缺失与瞄准锁死问题。
- 已以最新 r396 和内置相机覆盖此前路线；包含疾跑 A/D 转向这一源项目有意设计，未误写成所有状态都横移。
- 已将五类重点回归条件分配到 P1/P2/P3/P4/P5，并区分静态证据与真实运行证据。
- 已把同名类和网络/资源差异纳入 P0/P5，保留 Forge 现有 Curios、风弹和忠诚适配。
- 本轮只新增本计划文件；P0–P5 的实现和运行验证均未执行。
