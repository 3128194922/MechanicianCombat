# 移植执行记录

## 当前状态（2026-10-26，Forge 47.4.28）

> **最新交付基线**：Minecraft 1.20.1 / Forge 47.4.28 / Java 17.0.14；Curios API 5.11.1+1.20.1；正式 JAR `dist/MechanicianCombat-1.20.1-forge-0.1.0-porting.jar`，SHA-256 `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D`，980,699 B。Java 17 下两次 `clean build gameTestModJar` 的主包与验收包哈希一致；ForgeGradle 连通性探测通过本机 7890 代理；专服两进程行为与存档重启验收 PASS。G/H 同哈希双客户端各自通过生产弹药环 64 枚 UI 回环、真实改装台核心/便携发射器点击装配与服务端 NBT 同步，且保存了专注动画帧。两端到达 demo tick 结束标记，但这不能证明翻滚/滑铲/射击实际生效。另有一名玩家的 Inventory NBT 经保存离服、专服进程重启及同身份重新加入后逐字一致。默认 Forge Early Display 在本机主显示器初始化失败；枪械灰白几何、真人输入质量、观察者同步、音效播放及菜单异常关闭/断线恢复仍未验证，因此**移植未完成**。

- **2026-10-26 用户实测问题修复（仅编译验证）：** 腰射转向不再在 ShoulderSurfing 耦合回调已注册时由 `GunCamera` 重复写玩家 yaw；普通玩家模型 Curios 背包改为居中、1 倍缩放并置于身体后侧。`compileJava` 通过。上述 JAR 哈希对应的发布候选尚未包含本轮源码修改；真实键鼠移动/转向及背包外观仍需客户端验收，不能据编译结果标记为运行通过。
- **2026-10-26 持枪动画皮肤第二层接缝修复（仅编译验证）：** 根因为第二层表面逐面沿法线外移，导致棱边顶点位置不一致。改为按整体包围盒沿 XYZ 扩张，确保相邻面共享相同棱边；UV 与原外扩规格保留。`compileJava` 通过；动画画面仍需客户端复验，当前交付 JAR 尚未包含本次及前述源码修改。
- **2026-10-26 ForgeGradle 证书预检离线入口：** 常规在线构建在应用 ForgeGradle 时可能因 `maven.minecraftforge.net` 证书预检失败。新增 `tools/build-offline.ps1`，仅在该脚本运行期间临时跳过 ForgeGradle 预检并强制 `--offline`，结束时恢复环境变量；`.\tools\build-offline.ps1 -Task compileJava` 已通过。在线证书信任问题未修复，普通 Gradle/IDE 构建行为不变。
>
> 本版修复了隔离点击验收只触发 `mouseClicked` 的不完整模拟；现在依次执行原版 click/drag/release。详细哈希、运行方式和限制见 `docs/evidence/forge-47.4.28/acceptance-matrix.md`、`default-window-client-retest.md`。

- **2026-10-26 越肩视角复修（编译通过，客户端待验）：** 用户实测上一轮“持枪始终解耦 + 输入 tick 同步 yaw”仍无法转动视角。对照 r349 后确认目标实现是腰射由 ShoulderSurfing 耦合相机/身体，瞄准及翻滚等动作才解耦；已恢复该策略，并仅在 ShoulderSurfing 回调注册失败时由 GunCamera 兜底同步 yaw。持枪时额外跳过本地玩家名字牌绘制，其他玩家名字牌保留。`compileJava` 通过；真人鼠标转向、WASD 与名字牌显示尚未复验；发布 JAR 未重打包/同步，不能标记运行验证通过。
- **灰色模型的第一轮受控排查：** 用隔离的两客户端复现同一画面后，加入 -Dtkgun.skinlayer=false 关闭玩家皮肤第二层；两端灰白几何仍原样出现，排除 BowgunSkinLayer 第二层 UV 为根因，但具体来源未定，视觉验收仍不通过。记录与截图见 docs/evidence/forge-47.4.28/skinlayer-diagnostic/result.md。
- **灰色模型排查推进：** 两客户端关闭整个 BowgunGunLayer 后灰白块消失，但枪械模型也消失；分别跳过 stock、bone 未去掉异常。故问题归到枪械渲染层范围，具体骨骼/矩阵/动画仍未确定。诊断代码已完全撤销并核对源码哈希；恢复源码后 clean build gameTestModJar 成功，正式 JAR 与 dist SHA-256 复核为 E168EDD1…61A25B1A。结果及限制见 docs/evidence/forge-47.4.28/gunlayer-diagnostic/result.md。

- **当前交付候选（2026-10-26）：** 用户明确批准将 Accessories 改为 Curios API。背包物品实现为 `Item + ICurioItem`，装备查询使用 Curios `back` slot，18 个背包变体已加入 `curios:back` 标签，玩家实体分配 `back` slot，客户端 renderer 接入 Curios API。最终源码使用 Java 17 / Forge 47.4.28 连续两次 `clean build gameTestModJar` 字节一致；主 JAR SHA-256 `E168EDD1EBAFF3E5E0BCE7B17E5F7C808408D1BC332F177AA8BA592861A25B1A`，973,459 B；正式映射验收 JAR SHA-256 `816065217E728F6BA45A023B1E43FB3E8AEDBB96F7705DBC28044E8F829D4DAA`，42,489 B。正式映射专服两个独立进程通过风弹、各弹种服务端命中、生产开火/装填/扳机、Curios 背包、Loyalty III 拾取入口、菜单边界、改装台破坏掉落与存档重启。E2 哈希两份客户端在新建和平测试世界及新离线身份下完成双端菜单与专注动画阶段：两端都实际呈现弹药环 UI 并生成 Focus_shooting 截图。此前失败已定位为复用世界内旧玩家处于 DeathScreen；E2 的服务端日志也记录两次菜单成功打开。E2 两次 clean build 与专服双进程验收通过，`dist/` 已同步。完整移植尚未完成：双视角远端同步、截图中角色/摄像机表现、手动 UI、音效试听等仍待验。证据见 `multiplayer-client-acceptance.txt`、`multiplayer-ForgePortTestC/`、`multiplayer-ForgePortTestD/`、`multiplayer-server.log`。证据见 `final-build-reproducibility.txt`、`final-server-acceptance.txt`。
- Accessories beta46/beta45 的构建与 Mixin 记录现为历史结果；不得把其旧 JAR/日志当成当前 Curios 组合通过证据。
- **资源字符集缺陷修复：** 最终 JAR 原先因 Gradle 在 Java 17 Windows 使用系统默认字符集，打包后的 META-INF/mods.toml 中文描述无效 UTF-8。processResources 现显式设置 filteringCharset 为 UTF-8 并在构建时断言完整描述；修复后的主 JAR 两次 clean build 一致，严格 UTF-8 解码通过，Forge 47.4.28 专服双进程验收重新通过。证据见 docs/evidence/forge-47.4.28/resource-metadata-utf8.txt。
- **最终哈希客户端启动与模组初始化通过（2026-10-26，限定范围）：** 在 Windows Forms/独立 GLFW 探针确认主显示器可见后重新启动，Forge Early Display 成功创建窗口并进入 `forgeclient`；Curios、tkgun 初始化，21/21 声音资源自检、61 个 GeckoLib 动画片段、Shoulder Surfing 的 Curios 回调及 OpenAL 声音引擎均有日志。SHA-256 为 `F8C63544CFCA61A438CE39FC502A045B978FD475D80CA715F63A4CA40B55FE96`。本次显示器启动错误未复现；但计算机控制接口没有枚举 Java 游戏窗口，未检查主菜单、实际 UI、画面、动画、声音播放或游戏行为。日志见 `docs/evidence/forge-47.4.28/final-client-runtime-acceptance.txt`。
- **双客户端隔离服验收（2026-10-26，E4 本地阶段通过）：** 新世界、新离线 UUID、和平难度、平整高空平台下 G/H 两端均加入同一专服；服务端确认 AmmoStorageScreenHandler 打开，客户端实际弹药环 UI 截图和 Focus_shooting 阶段均有证据。手动键鼠操作、对方观察视角实体/弹体同步、声音未测；动画截图仍出现块状灰模型，视觉验收未通过。E1/E2 死亡界面测试污染及排除过程已留档。证据见 `multiplayer-client-acceptance.txt`、`multiplayer-ForgePortTestG/H/`。

- 目标版六个第三方前置在运行时 metadata 使用最低版本及以上范围；`DEPENDENCIES.json` 仍锁定可复现的已验证文件。最终 JAR 在 Forge 47.4.28 隔离专服 Curios 槽/标签/装备查询、风弹服务端行为、弹药环 Shift 点击存储、菜单边界与改装台有效性均通过；镐挖掘标签、21/21 声音资源加载已有目标版证据。
- **菜单服务端补验通过（限定范围）：** 最终验收器确认弹药环拒绝负值/越界 quick-move，Shift 点击只把合法弩矢合入环形仓并正确写入枪械 NBT；改装台只接受指定背包/核心/发射器，越界 quick-move 被拒绝，方块被移除后菜单失效。对应防护补入 `AmmoStorageScreenHandler.quickMoveStack`。真实客户端点击、关闭/断线/破坏及异常 C2S 包仍未验收。
- **改装台破坏与物品归还补验通过（限定范围，主 JAR 未变）：** 最终哈希专服在打开菜单时移除改装台，确认菜单失效、方块实体库存清空，背包/核心/发射器各掉落一件；再次调用清理未产生重复物品。prepare 与 verify 两次进程均通过。该测试不覆盖客户端关闭/断线 UI 同步。结果见 `final-server-prepare.log`、`final-server-verify.log`；验收包新哈希 `816065217E728F6BA45A023B1E43FB3E8AEDBB96F7705DBC28044E8F829D4DAA`。
- **忠诚附魔方块命中补验通过（限定范围）：** 最终哈希正式映射专服两次启动，各对 256 个带 Loyalty III 的熔铁弩矢直接执行方块命中处理，分别 124/256 与 127/256 消失，共 251/512（49.0%）；存活者停留且物品栈保留 Loyalty III。该取样验证命中处理的 50% 消失分支，不等于自然飞行碰撞；背包来源标记的副手回收入口另已单独通过。记录见 `docs/evidence/forge-47.4.28/final-server-prepare.log` 与 `final-server-verify.log`。
- **忠诚背包副手回收与充能补验通过（限定范围）：** 最终哈希专服两次启动均通过：装备含核心和发射器的 Curios 背包，为带 Loyalty III 的熔铁弩矢设定背包来源标记，再实际调用 `ThrownTrident.tryPickup` 的 Mixin 路径；副手得到一枚附魔数据完整的弩矢，背包总数为一；正常状态补 1 格，冷却状态清冷却并恢复满格。自然返航时间、真实发射循环、多次回收以及枪械/手持路径或副手已占用分支仍未覆盖。记录见 `backpack-loyalty-acceptance-compile.log`、`final-server-prepare.log`、`final-server-verify.log`。
- **手持及枪械 Loyalty III 回收入口新增通过（限定范围，主 JAR F8C6…FE96）：** 隔离专服运行生产 `MoltenTridentItem.use/releaseUsing`，待首个 server tick 后确认熔铁三叉戟生成并保留 Loyalty III；调用原版 `ThrownTrident.tryPickup` 后确认恰好返还一枚附魔弩矢。枪械发射路径同样构造 Loyalty III 弹体并通过原版 owner pickup 恰好返还一枚。该检查对弹体执行回收前传送，不代表自然返航飞行/真实碰撞回收；枪械自然弹药扣除队列、背包飞行返航和持久化仍未验。双进程结果见 `final-server-prepare.log`、`final-server-verify.log`。
- **本轮新增弹种与真实射击专服验收（2026-10-26）：** 正式映射验收器通过生产命中分支断言：标准 hitscan；爆破弹直击与范围；切割弹三段直击与范围；幽匿弹直击/音爆且直击目标只结算一次；玫瑰石英弹 5 点基础直击（僵尸护甲后有效伤害 4.92）且无溅射；破甲弹产生 8 个霰弹视觉弹丸并命中锥形伤害；枪械熔铁弩矢弹体保留 Loyalty III。另由正式服务端执行实际 `fireOnce`，确认服务端选中目标、创建视觉弹体并在弹体完成轨迹后施加爆破伤害。仍不覆盖客户端按键、枪械状态机/弹药扣除、自然 Loyalty 返航/拾取或玩家命中 UI。最新验收 JAR SHA-256 `DB78AAA56AFD911098DE37C91165DAEAA8A424179FB6AF6E16D7AF668F6C44AC`（37,971 B）；双次 clean build 与最终主 JAR 双进程专服验收通过。证据：`final-build-reproducibility.txt`、`final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`。
- **2026-10-26 最终哈希跨重启通过（限定范围）：** 在隔离专服 `acceptance-final-20261026` 第一次启动写入枪械弹药环、选择槽、弹匣及背包升级 NBT，保存退出后第二进程重启从磁盘读回并逐字段通过。结果见 `final-server-prepare-result.txt`、`final-server-verify-result.txt` 及对应日志。玩家背包、丢弃/重拾和客户端菜单交互仍未覆盖。
- Accessories beta46/beta45 专服 Mixin 诊断及开发客户端 `EntityMixin` 错误现在均为历史记录。切换 Curios 后 `runClient` 越过该阻断并记录 Curios 与越肩兼容初始化，tkgun 21/21 声音资源自检成功；本次没有可观察的 Minecraft 窗口，未把真实 UI/渲染记为通过。
- 开发 GameTest 断言已编译，但开发运行被 JPMS split-package 启动错误阻断；正式映射专服的隔离验收器通过了对应服务器行为子集。两者不混为一项。正式客户端安装器已在隔离目录成功安装 Forge 47.4.28；最新最终哈希客户端已进入模组初始化，完整范围见 `final-client-runtime-acceptance.txt`，但真实游戏 UI 和行为未验。
- 枪械腰射/专注状态机、射击输入和弹药扣除、装填/过热、菜单恶意输入/关闭断线及真实 UI、忠诚手持/枪械/背包自然返航拾取与真实飞行撞方块、背包发射器自然充能/连续发射、自动炮台连续运行/目标切换、翻滚/滑铲、动作/越肩/声音画面和双人同步仍未验证；所有弹种命中效果分支本轮有正式映射专服断言，便携/自动发射器单发和启停、50% Loyalty 命中统计及区块箱样本 NBT 跨重启已通过。**本项目尚未满足完整交付标准。
- **r349 文件覆盖清点：** 101/101 个源 Java 文件均映射到同名目标或已记录的 Curios/Forge 适配替代；229 个源资源均按原路径、1.20.1 目录规范、Forge 元数据或 Curios tag 找到对应项。该结果仅为文件结构覆盖，不折算玩法测试通过。逐项说明见 `docs/evidence/forge-47.4.28/source-coverage-audit.md`。
- 完整逐项范围与日志索引：`docs/evidence/forge-47.4.28/acceptance-matrix.md`、`server-acceptance.md`、`accessories-beta45-comparison.md`。

## 2026-10-25 恢复实施：Forge 47.4.28

- 按用户明确授权恢复实施，最终目标固定为 Minecraft 1.20.1 / Forge 47.4.28 / Java 17。旧版 Forge 47.4.6 产物及证据仅保留为历史对照。
- 在任何改动前保存主项目当前源码/资源 SHA-256 清单及旧候选 JAR 校验值：`docs/evidence/forge-47.4.28/baseline-sha256.txt`。
- 已更新 `gradle.properties`、主/测试 `mods.toml` 至 Forge 47.4.28，并修正镐挖掘方块标签路径到 `data/minecraft/tags/blocks/mineable/pickaxe.json`。
- 初期 47.4.28 的 POM/userdev 下载和 ForgeGradle 连通性探测曾失败，导致首次构建未进入 `compileJava`；后续通过本机已授权的 7890 代理取得并校验官方 userdev/universal，之后的目标版 clean build 与正式映射专服结果见本文件顶部及最新证据目录。初期失败日志仍保留作历史诊断，不能覆盖后续通过结果。
- 所有运行目录与证据位于主项目内的 `validation/forge-47.4.28/` 与 `docs/evidence/forge-47.4.28/`；使用新建的本机回环测试世界，没有触及真实服务器或存档。

### 中途排查记录（其时状态；当前结果见文档顶部）

> 本节记录当时的中间状态。后续已完成目标版构建及正式映射隔离服检查；以顶部摘要和 forge-47.4.28 证据目录为准。

- 已新增 GameTest：风弹命中石按钮后应使其进入按下状态。因 Forge 47.4.28 userdev 下载尚未完成，此测试还没有编译或观察 RED 失败；当前测试源不能标记为已运行。
- 已增加服务端风爆方块交互实现：按钮、拉杆、非铁门/活板门、栅栏门、铃、点燃的蜡烛/蜡烛蛋糕，以及装饰陶罐、紫颂花；当前实现尚未编译或在世界中验证。
- 已新增落伤高度跟踪：记录风爆高度、在 Forge `LivingFallEvent` 中扣除爆点上方的坠落距离，并在着地或 600 tick 过期后清除；标记保存在实体 persistent data。已补“高度以下保护/单次消耗”测试源，尚未运行。
- 已补弹体击退分支：把风爆速度叠加到半径内的 `Projectile`，并标记服务端速度同步；已新增雪雪球击退 GameTest 源，尚未运行。
- 新增正式映射服务端验收分支覆盖按钮、风弹命中击退等行为，现已在 47.4.28 正式映射服实跑；精确结果和仍未覆盖的客户端/菜单/多玩家场景见 `server-acceptance.md`。
- 使用 Java 17 `javac` 对本次改动的生产类和验收类作了旧缓存诊断，基于 Forge 47.4.6 官方映射类编译退出码 0（一个既有 FML 加载上下文弃用警告）；这不算 47.4.28 Gradle 编译，且没有运行测试。记录：`docs/evidence/forge-47.4.28/source-compile-diagnostic-47.4.6.txt`。
- Forge Maven 默认路由反复重置连接；改用该官方域名的备用 Cloudflare 地址后，100 KB 范围请求成功，继续完成剩余片段。任何片段仍需通过整包 SHA-1 才会进入 Gradle 缓存。
- 找到并修正手持风弹的失败生成分支：`addFreshEntity` 返回 false 时现在会保留物品且不触发冷却/音效；已新增 Forge 实体加入事件拦截的 GameTest，目标版尚未编译/运行。
- ForgeGradle 线程栈证实前一阻塞是官方启动连通性探测运行在 daemon 内；将已授权的探测开关传给 daemon 后已越过该探测，并进入官方 `universal@jar` 下载。该产物尚未到位，仍未进入源码编译。
- 用户提供本机代理端口 7890 后，官方 Forge 47.4.28 universal JAR 已经代理下载并通过 SHA-1 校验（2,517,355 B）；与 userdev 一并放入 `validation/forge-47.4.28/local-maven/`，目标 Gradle 构建可继续。
- Maven userdev 分段缓存目前有前三段完整，第四段与第五段仍下载中；官方 SHA-1 为 `2b4c3b220ed894d566e0b121cbd5f6c0675fa35a`，尚未组装校验，不可作为构建依赖使用。
- 开发 GameTest 启动配置仍因测试模组与主模组重复导出包而不能执行 GameTest；不要把该开发任务标为通过。对应服务器行为已由独立正式映射验收器实际覆盖。

计划：docs/superpowers/plans/2026-10-25-forge-port.md

## 历史执行结果（Forge 47.4.6；不计入目标版验收）

### 风弹世界行为 GameTest 验证尝试

- 为验证当前 Forge 1.20.1 服务端手持使用入口，新增 `src/test/java/com/mechaniciancombat/gametest/WindChargeGameTests.java`：以生存 FakePlayer 使用两枚风弹，检查恰好消耗一枚并生成一个服务端 `WindChargeEntity`。`build.gradle` 为 `gameTestServer` 提供该测试源集及启用命名空间。
- Gradle 8.8 / JDK 17 离线 `clean build` 成功，`compileTestJava`、`test`、`verifyPortContracts` 均通过；日志：`docs/evidence/2026-10-25/clean-build-gametest-contract.log`。本次只为该命令使用 ForgeGradle 官方错误提示中的 `-Dnet.minecraftforge.gradle.check.certs=false`，绕过受限网络下的仓库连通性预检；依赖仍使用本地缓存。
- `runGameTestServer` 已成功进入 Forge 47.4.6 / Minecraft 1.20.1，但在测试类执行前被 Accessories beta46 内嵌的 `fabric-entity-events-v1.mixins.json:EntityMixin` 阻断：`@Shadow f_19853_` 在官方映射开发目标 `Entity` 上找不到。完整复现日志：`docs/evidence/2026-10-25/gametest-server-beta46.log`。因此测试代码已编译但其世界断言**未运行**；不应把 GameTest 或风弹扣物/生成验收标为通过。没有修改第三方 JAR。
- clean build 后主 JAR 哈希仍为 `0617C4E50DD627B0AEE7704CBE211787444339E177D8096E3DCCCC5F5F710290`（966,487 字节）；本次改动只有 GameTest 构建接线、测试源与证据文档，不改变生产逻辑。之前 `build/runtime-47.4.6` 下的隔离测试安装目录属于 Gradle `clean` 清理范围，目录副本不再存在；已有原始运行日志和哈希 sidecar 保留。

### 正式映射隔离服风弹与忠诚兼容验收

- 重新安装 Forge 47.4.6 正式专服到 `build/runtime-gametest-production/server-install-clean`。安装器 SHA-256 与既有锁值一致；服务端只绑定 `127.0.0.1:25569`，`online-mode=false`，只在该隔离目录接受 EULA。六个已批准前置与当前主 JAR 均从项目锁定文件/本地缓存安装；未接触真实服务器。
- 由于 `runGameTestServer` 的 userdev 映射在 Accessories beta46 内嵌 Fabric API Mixin 处失败，另将只含 `com.mechaniciancombat.gametest` 的测试类打成独立 `-gametests.jar` 并按正式映射重映射。它不会打进发布 JAR，也没有新增运行时依赖。
- Forge 47.4.6 正式映射专服启动进入 `Done` 后，临时测试模组断言通过：熔铁弩矢可在附魔台应用 Loyalty、可接受 Loyalty III 附魔书供铁砧使用，并拒绝 Sharpness；生存玩家用两枚风弹调用服务端使用入口后恰好扣一枚、生成一个实体；弹体命中猪并产生击退（最大速度平方 `1.6000000047683716`）。服务端随后正常保存并关闭。
- 完整运行日志：`docs/evidence/2026-10-25/production-mapped-acceptance.log`；测试步骤和严格范围：`docs/evidence/2026-10-25/production-mapped-acceptance.md`。这弥补了当前哈希风弹服务端消耗、生成、命中和击退证据，并验证了附魔 API/书籍兼容入口；仍**不等同**实际客户端按键/附魔台和铁砧 UI 点击、Loyalty 投掷返航拾取或跨玩家渲染同步验收。
- 正式服仍出现 Accessories beta46 内嵌 Object Builder 的 VillagerTrades LVT Mixin 错误，以及客户端 `LevelRenderer` Mixin 目标缺失警告；服务器可以进入 `Done`，但严格“无 Mixin 目标错误”门槛仍未通过。beta45 隔离验证结果见 `docs/evidence/forge-47.4.28/accessories-beta45-comparison.md`；主依赖保持 beta46。

### 当前哈希双客户端补测

- 主 JAR `0617C4E50DD627B0AEE7704CBE211787444339E177D8096E3DCCCC5F5F710290` 已在当前 Forge 47.4.6 专服安装目录复核。
- 当前专服日志记录 `PlayerIota` 与 `PlayerTheta` 成功登录，并由服务器报告两名玩家同时在线；两台 Forge 47.4.6 客户端分别加载 `tkgun` 并连接 `127.0.0.1:25568`。
- 当前哈希专服日志可查询到 `tkgun:wind_charge` 实体及坐标。不能从保留日志确认此实体由客户端使用动作生成，也不能确认弹药扣减、命中/击退和观察者的画面/声音同步。
- 证据与严格边界见 `docs/evidence/2026-10-25/current-hash-0617C4E5-multiplayer-evidence.md`。本次更新替代此前“当前哈希双客户端完全未重跑”的表述；不代表最小验收矩阵已通过。

### 当前哈希客户端风弹输入尝试（2026-10-25）

- 再次运行 Gradle 8.8 / JDK 17 `build --offline --no-daemon --console=plain` 成功（`test`、`verifyPortContracts`、重映射 JAR 均通过），日志：`docs/evidence/2026-10-25/completion-build.log`。运行环境的 Java `user.home` 指向只读 `C:\`，因此只在主项目 `.gradle` 中建立隔离 Gradle 缓存；最新主 JAR 哈希未变。
- 重新启动当前哈希隔离专服 `127.0.0.1:25569`，确认进入 `Done` 后停止并保存。EULA 与离线模式只用于该目录，未触碰真实服务器。
- 通过官方 profile、缓存客户端库和当前 `-Dtkgun.windDemo=1` harness 试启动客户端。Forge Early Display 在隐藏与普通窗口两次启动均因 `glfwGetPrimaryMonitor failed / Failed to locate a primary monitor` 于加载模组前退出；没有进入连接阶段，因此本轮**没有**实际客户端风弹输入或扣物结果。这是显示环境阻断，不是模组玩法失败证据。
- 完整边界见 `docs/evidence/2026-10-25/current-hash-client-wind-demo-blocked.md`。当前哈希双人同时登录和正式映射服务端风弹碰撞验收仍按各自证据记录，不能替代客户端输入、GUI、返航拾取与观察者同步。

### Accessories Mixin 侧别诊断（Forge 47.4.6 历史；目标版更新见顶部）

- 最新 Forge 47.4.6 专服 `debug.log` 显示目标来自 `accessories-common.mixins.json:LevelRendererAccessor`；Forge `RuntimeDistCleaner` 拒绝加载客户端 `LevelRenderer` 后，Mixin 报告目标不存在。该条属于 beta46 第三方 JAR，不是 `tkgun.mixins.json`。
- 直接读取本地 beta46 JAR 的 Mixin JSON，确认 `LevelRendererAccessor` 被列在通用 `mixins` 而非 `client` 列表。对主项目共用 Java 包的扫描只发现 `BowgunItem` 在 `DistExecutor` 客户端分支里引用客户端类；最新专服 debug 日志没有 `com/mechaniciancombat/client` 加载尝试。
- 本地 beta48 与 beta49 JAR 也把该 accessor 放在通用 `mixins` 列表；两者还包含已导致 Mixin 0.8.5 启动失败的 `ContainerMixin`。此处为 beta45 下载前的历史状态。后续隔离对照及其结果见 `docs/evidence/forge-47.4.28/accessories-beta45-comparison.md`；主依赖仍固定 beta46。

- **当前主产物：** Forge 47.4.6 / Java 17 JAR SHA-256 为 `0617C4E50DD627B0AEE7704CBE211787444339E177D8096E3DCCCC5F5F710290`，966,487 字节。最新源码加入服务端弹药环方向校验、8 槽索引规则及拒绝零/负数弹药消费。RED 阶段分别确认缺失规则时编译失败；实现后 `verifyPortContracts` 通过。合同覆盖无效选择值归一化、首尾切换、方向 ±1 限制与消费量正值要求。
- **当前哈希构建证据：** `clean build --offline --no-daemon --console=plain` 成功，含 `test` 与 `verifyPortContracts`；完整日志见 `docs/evidence/2026-10-25/clean-build-final.log`。本轮通过 JDK 17 直接重跑背包转移与弹药环合同类成功；Gradle wrapper 的二次运行未能启动，因为当前沙箱将 lock 目录解析到不可写的 `C:\.gradle`。当前哈希随后在 Forge 47.4.6 / Java 17 授权隔离专服 `127.0.0.1:25568` 加载并进入 `Done (15.232s)`；启动日志见 `docs/evidence/2026-10-25/latest-hash-0617C4E5-47.4.6-server.log`，证据说明见同名 `.md`。Accessories beta46 的 LevelRenderer 客户端目标被 Forge 拒载，且内嵌 Fabric API/Object Builder Mixin 记录非致命诊断；专服仍正常保存并关闭，但严格“无 Mixin 目标错误”门槛未通过。
- **前一哈希客户端补充检查（`E7CB2FDA...`）：** Forge 47.4.6 / Java 17 隔离客户端完成模组加载、tkgun 客户端注册及纹理图集创建，并向回环专服发起 Quick Play 连接；服务端收到 `PlayerTheta` 登录请求后记录通用 `Disconnected`，未观察到成功进入世界。移除 Quick Play 后，Forge early display 因环境找不到主显示器而无法打开主菜单。该日志只能证明客户端初始化及连接请求；记录见 `docs/evidence/2026-10-25/previous-hash-E7CB2FDA-47.4.6-client-evidence.md`。
- **前一哈希 `91208F9A923B67AC9663EF53F9C9946FCA8165A63E4A8A25F289257E0F345955` 的运行证据：** Forge 47.4.6 / Java 17 正式隔离客户端启动，两客户端同时登录；风弹客户端 `useItem=SUCCESS`，服务端确认扣物和生成弹体。日志存于 `docs/evidence/2026-10-25/`。这些玩法结果不自动外推到当前哈希。
- **前一哈希风弹运行边界：** 首次演示客户端日志 `docs/evidence/2026-10-25/wind-final-91208-r2.out.log` 记录 `useItem=SUCCESS`；服务端日志 `docs/evidence/2026-10-25/exact-hash-current-47.4.6-server.log` 记录风弹消耗和 `tkgun:wind_charge` 实体生成，位置约 `[0.5, 61.52, 8.0]`。当时目标猪偏离弹道，后续重试中目标猪未移动且自动演示器没有再次触发。客户端重连日志保存在 `docs/evidence/2026-10-25/exact-hash-current-theta-client.log` 与 `docs/evidence/2026-10-25/exact-hash-current-iota-client.log`；两客户端同时在线由服务端日志记录。风弹命中/击退及跨客户端观察尚未通过。
- **此前精确哈希运行验收（旧哈希 `554314F0...10F18BF8`）：** Forge 47.4.6 / Java 17 专服在 `127.0.0.1:25568` 本机回环运行；`PlayerTheta` 与 `PlayerIota` 同时登录，服务端 `/list` 返回两人。枪械装填、专注射击、`spawn=true` 弹体生成、过热收尾及 Loyalty III 生存投掷返航通过；枪械弹药 NBT 与 Loyalty III 物品经服务端重启读回。该运行验收已在当时记录，但临时服务器日志与截图随后被清理，最新哈希未重跑风弹回环验收。
- **忠诚投掷与存档（旧哈希验收）：** 生存模式隔离专服原版 `/enchant` 成功给 `tkgun:bolt` 写入 Loyalty III。使用原版按键投掷门控演示两次：第一次弩矢数量 3→2、之后弹体消失；第二次投掷后数量保持 2，表明存活弹体返还。与实现中的 50% 方块命中消失分支一致。另在专服重启后验证 Loyalty III 仍保存在 PlayerIota 物品、枪械弹仓 NBT 保存在 PlayerTheta 物品。此前运行日志和截图曾保存在 `build/` 临时目录，后续 `clean` 已移除；此记录不替代附魔台/铁砧 UI 操作，UI 仍未通过实际点击验收。
- Accessories 已固定为官方 `1.0.0-beta46+1.20.1`（Modrinth ID `WUQZDiqP`）；官方文件 SHA-512/SHA-256 与本地 JAR 及 [依赖锁](DEPENDENCIES.json) 一致。Modrinth 声明支持 Forge/NeoForge 1.20.1 与客户端/服务端。
- 对旧哈希 JAR 的正式客户端/专服日志核对 Mixin：目标配置 19 项中 18 项已观察到实际目标转换（含 Combat Roll 输入注入）；`GunTooltipBackgroundMixin` 的 `TooltipRenderUtil` 未在启动/连接流程加载，因此该项尚未观察到应用。源项目另有烟花自伤 Mixin，目标已迁移为 Forge `LivingHurtEvent` 处理器；该事件行为尚未实际触发验收。Mixin 逐文件状态见 `docs/SOURCE_MIGRATION.json`。
- 资源引用静态核验覆盖模型、贴图与声音共 84 个引用，缺失 0；85 个 JSON 均能解析。该项不替代实际纹理/动画/界面观察。
- ForgeGradle `runClient` 开发客户端仍因 Accessories beta46 内嵌 Fabric API 的 `EntityMixin` 缺少 `Entity.level` refmap 映射而失败；当前最新哈希另有 Forge 47.4.6 正式映射隔离客户端记录：两名客户端成功连入当前哈希专服，具体证据见本次联机补测 sidecar。旧哈希 `554314F0...10F18BF8` 的枪械、忠诚投掷演示仍是历史证据。正式客户端验证中修复了我方枪械提示框注入签名、瞄准鼠标注入签名、实体渲染器取值顺序及 GeckoLib 4.8.4 不兼容的动画表达式；48 处表达式替换后，客户端动画解析错误为 0。已观察到持枪、重装、专注射击、摄像机瞄准、枪弹生成和 Loyalty III 存活弩矢返航；其余玩法/视角场景和完整操作矩阵仍需逐项验收。
- 正式启动发现并修复了四个移植问题：Mixin 配置未声明生成的 refmap；火箭重力注入目标所有者需与正式字节码的 `AbstractArrow` 一致；自伤保护改用 Forge `LivingHurtEvent`；工作台的 `facing` 属性加入自有方块状态定义。主 JAR 的同配置正式服务端启动通过。
- Gradle 8.8、JDK 17、ForgeGradle 6.0.54：最新 `clean build --offline --no-daemon --console=plain` 成功（含 `test`、`verifyPortContracts`）。`tools/verify-network-contract.ps1` 通过，13 个网络消息合同一致。85 个 JSON 解析通过，113 个 Java 源文件与迁移清单一致；未发现 Fabric 入口元数据或 `example` 包。
- `build.gradle` 已禁用归档时间戳并固定 ZIP 条目顺序。当前主 JAR：`build/libs/MechanicianCombat-1.20.1-forge-0.1.0-porting.jar`，966,487 字节，SHA-256 `0617C4E50DD627B0AEE7704CBE211787444339E177D8096E3DCCCC5F5F710290`。当前 clean build 日志见 `docs/evidence/2026-10-25/clean-build-final.log`。
- 专服日志中仍有 Accessories 所带 Fabric Object Builder 的一个非致命 LVT 捕获错误告警，服务端随后完成世界加载并进入 `Done`。尚未逐项验证该前置的村民交易增强功能。
- 本轮在旧哈希客户端观察到风弹验收分支确实启动，但仅用 `KeyMapping.setDown` 点按没有导致服务端消耗风弹；测试分支随后改用正式 `MultiPlayerGameMode.useItem` 入口并通过编译，因临时客户端运行目录被 `clean` 删除，改后的投掷、消耗和击退尚未重跑验证。
- 专服 EULA 曾按用户授权仅在 `build/runtime-47.4.6` 隔离目录设为 `true`。当前哈希专服启动、双客户端登录和风弹实体存在均有日志证据，测试使用的仅是隔离目录；未触碰真实服务器。风弹使用扣物与命中击退仍未在当前哈希验证。附魔台/铁砧 UI、Loyalty 实际返航拾取、完整弹种/背包/炮塔玩法矩阵、菜单交互安全、已批准前置联动、提示框背景 Mixin、烟花自伤行为和其他玩家客户端上的动作/音效/弹体可见性仍需逐项验收；当前不能宣布完整玩法发布验收完成。

下方记录保留 beta48 开发映射阶段的调查过程；其中“当前配置 beta48/Forge 服务端未验证”等表述已被本节最新证据取代。

## 早期评估阶段历史记录（已由上方最新执行结果取代）

### Accessories 启动错误方案分析（早期状态，已由上方正式服务端测试更新）

- 本地 beta48/beta49 JAR 均包含 `ContainerMixin.class`；beta46 不包含。三个版本都内嵌同一 `fabric-entity-events-v1-1.6.0+6274ab9d77.jar`，所以 beta46 避开了 ContainerMixin 错误，但仍暴露 FAPI `EntityMixin` 的映射问题。
- beta46 的失败字段为 `Entity.f_19853_`。本工程 Forge 映射将其映射为开发环境名称 `Entity.level`，而内嵌 FAPI 的 refmap 没有该 Shadow 字段映射，所以 ForgeGradle 开发服找不到字段。Forge 文档说明生产环境使用 SRG 名称、开发包通过 `reobf*` 转换；因此“beta46 仅在开发环境失败、生产服务端可匹配 SRG 字段”是有依据的推断，但还没有正式服务端运行证据。参考：[ForgeGradle 6 配置与生产 SRG 说明](https://docs.minecraftforge.net/en/fg-6.x/configuration/)。
- 当前 beta48 不可作为已验证兼容方案；beta45 官方页面声明支持 Forge 1.20.1/服务端，但尚无 JAR 检查或本地运行证据证明它修复此问题，且降级可能带来 API 差异。修补或重打包第三方 Accessories/FAPI 会违反本项目“不未经同意修改其他模组”的约束；替换为 Curios 则属于新依赖与饰品系统变更，需另获批准。
- 当时最保真候选是再次用 beta46 做生产映射服务端验证；此前 Forge Maven 连接超时尚未获取正式专服证据。该方案后来按用户授权执行并已在 Forge 47.4.6 专服通过，见本文件顶部最新执行记录。

- 用户批准 A 方案。AGENTS.md 和指导文档同步更新。
- 已发现本机 JDK 17、Gradle 8.8 缓存和 ForgeGradle 6.0.54；待实际构建验证。
- Modrinth 官方 API 可访问（普通受限环境 SSL 连接失败，获准联网环境成功）。
- 已找到 Accessories、GeckoLib、Player Animator、Shoulder Surfing 4.21.0、Combat Roll 和 Cloth Config 的目标版本。接下来核实 JAR 元数据。
- Ruling: 使用指定空目标目录直接执行，不另建 worktree — 用户明确指定主项目路径，未要求 Git 初始化 — 无提交级回滚，改动均留在目标目录。
- 上述为最初记录；随后提前建立了构建配置、源码转换稿、资源转换和网络适配草稿。不是完成品。

## 早期范围纠正阶段历史（后续用户已要求恢复实施）

- 用户要求优先完成移植报告，不继续实现；已停止产品代码修改与构建。
- 六个目标前置已核对官方 SHA-512 和 JAR 元数据。用户追加批准 Accessories 内置模块。
- 无源码时的 Gradle 配置检查成功（compileJava NO-SOURCE）；不能作为功能构建通过证据。
- 最近一次有源码编译失败：238 errors、31 warnings；日志后另有未编译草稿变更，不能把这个数量当作当前错误数。
- 未进行客户端、专服或双人验证，无已验证的目标 JAR。
- 当时仅修订文档并形成 docs/移植评估报告.md；当时记录的风弹与附魔方向已由后续用户决定更新，见下文及主报告。
- 现存草稿保留原状，后续仅在用户明确要求实施时恢复。原定计划不自动继续执行。

## 风弹参考研究

- 用户选择本项目内实现风弹，并要求查找 GitHub Forge 1.20.1 参考。
- 已只读检查 Just Trial Spawners（68d4314cfbbf55aa804d898fc944601d2224c87d）与 Wind Charges Backport+（c0e995e293eb6f65430a3901b2b03b4425bc8f7b）的核心源码、版本与许可。
- 主要参考前者，记录其范围查询、tick 和原版行为完整性等需要复核的点；后者授权标注冲突且物理实现不同，仅作对照。
- 新增 docs/风弹参考项目与实现方案.md，并同步已确定方向。未修改 src、构建配置或运行构建，未安装参考模组。

## 忠诚附魔需求确认

- 用户要求“附魔忠诚移植”，已记录目标熔铁弩矢支持忠诚，不扩大到其他附魔。
- 只读核查物品、弹体、背包忠诚拾取和服务端发射路径：源代码多处清除附魔；弹体撞方块有 50% 消失；背包成功返还副手还会恢复充能，冷却期为直接恢复满充能。
- 主报告、指导文档和项目规范已同步。用户随后要求撞方块消失按原项目完全复刻：保留 50% 消失，未消失弩矢才能忠诚返回；同时确定风弹配方或掉落交给整合包，本模组不内置配方。
- 未修改产品代码、未运行构建或游戏。上述是需求与源码证据，不是功能已实现或通过验证。

## 报告定稿（1.0）

- 按用户要求最终完善报告，汇总 D01–D07 已确认决定，统一主报告与指导文档的 R0–R5 阶段编号。
- 修正“前置待选择”“Forge 版本候选”等过时表述；说明依赖锁中的 API 依赖数组不能替代 JAR 元数据。
- 补齐恢复实施步骤、风险处理、验收证据和发布清单；明确忠诚生存附魔入口尚须在实施该功能前选择。
- 同步指导文档、风弹专项方案及旧实施草案；报告定稿不自动恢复代码实施。
- 定稿检查：五份本轮文档的本地文件链接均有效；六个依赖 JAR 的 SHA-256 与锁记录一致；已清除本轮检索的过时决策/阶段表述。前后文件校验确认 src 与三份主构建配置未变。未运行构建、启动游戏或宣称运行验收通过。



- **生产枪械装填事务新增目标版专服验收（2026-10-26）：** 对生产 TkGunServer.reload 路径，正式映射隔离服断言从弹药环精确扣除 6 枚熔铁弩矢并将弹种/弹匣状态写入枪械 NBT；只有 5 枚时不消耗、不启动部分装填。两次 clean build 后主 JAR 哈希保持 D7FE…9989，验收 JAR 当前哈希 $testHash（37,971 B）；专服 prepare 与重启后 verify 均运行通过。该检查验证服务端装填交易，不替代客户端输入、视觉动画及开火节奏完整 UI 流程。



- **生产枪械扳机门控新增验收（2026-10-26）：** 目标正式映射专服实际调用生产 BowgunItem.use 与 TkGunServer.tryFire，确认无举枪、同 tick 重复、过热三种输入被服务端拒绝；合法举枪只入队一次、弹匣 NBT 从 4 更新到 5。和此前腰射装填原子性测试分开覆盖，均在 prepare 和存档重启后的 verify 两次通过。当前验收 JAR SHA-256 $testHash（37,971 B）；主发布 JAR 未变，仍为 D7FE…9989。证据见 inal-server-prepare.log、inal-server-verify.log 和 inal-server-acceptance.txt。该范围不包含客户端按键或网络端到端验证。

- **本轮最终复现与服务器验收（2026-10-26）：** 连续两次 clean build 后，主 JAR SHA-256 `F8C63544CFCA61A438CE39FC502A045B978FD475D80CA715F63A4CA40B55FE96`（972,776 B），验收 JAR `8DB9AA1666305DFECD0BF65B23F3EC735B040ED0EA6E2B585E48FD5EDD711D88`（41,549 B）；双进程 Forge 47.4.28 专服测试、保存与重启复验均通过。更新后的 `dist/`、正式服务器/客户端安装副本与两次 clean build 输出主 JAR 哈希逐字节一致。最新客户端用此哈希实际启动，图形驱动到 OpenGL 4.6 初始化成功，但没有主显示器，停在 Forge Early Display。客户端 UI/声音/动画、菜单 C2S/断线流程、自然 Loyalty 飞行、炮塔连续运行和双人同步仍未验收。

### 后续诊断与当前候选核对（2026-10-26）

- 正式源码 `BowgunGunLayer.java` 已恢复至无诊断开关的基线哈希 `D449551CC9C5471DBF0273B7C359C2A80987216C39A02E82C4BAFC472A872AAD`。效果组隔离 JAR `E36E5DDA…7C0535` 在 Forge 47.4.28 / Java 17 单客户端真实连接隔离专服，并到达弹药环 UI 与瞄准动画截图；跳过 fire/smoke/dash 全组后灰白几何仍存在，不能视为修复。gun 根骨试验无效，因为几何文件中该骨没有方块。
- 一次 effects 旧试验误用了 stock+bone JAR 且开关不被识别，已明确标为无效，不纳入通过证据。隔离测试安装目录的正式同名 JAR 已恢复为 `E168EDD1EBAFF3E5E0BCE7B17E5F7C808408D1BC332F177AA8BA592861A25B1A`。
- 当前正式源码不含临时诊断属性。上次 clean build 的 E168 正式 JAR 已从 `dist/` 逐字节恢复到 `build/libs`，但这是复制恢复，不是本次诊断后的重新 clean build；必须重新执行目标版 clean build 才能更新正式候选和交付哈希。当前项目仍未完成完整客户端/专服/双人玩法验收。






- **诊断后正式复建/专服复验/客户端复试（2026-10-26，更新前文“复制恢复”状态）：** 临时诊断代码已撤除，之后 `clean build gameTestModJar` 再次通过；`build/libs` 与 `dist/` 当前 SHA-256 均为 `E168EDD1EBAFF3E5E0BCE7B17E5F7C808408D1BC332F177AA8BA592861A25B1A`（973,459 B）。同哈希正式映射隔离专服 prepare、保存退出、重启 verify 均通过，详见 `final-server-acceptance.txt`。本轮双客户端复测两种安装目录均在 Forge Early Display 遇到 `Can't find a primary monitor`，未进入握手；隔离服务端已启动但没有玩家加入，故本轮双客户端未通过。当前窗口清单没有 Minecraft 窗口；已有同哈希历史 UI/动画阶段证据仍保留，但远端同步未测，见 `docs/evidence/forge-47.4.28/final-multiplayer/result.md`。

## 自然 Loyalty 回收与双客户端再验（2026-10-26）

- **已实现（仅验收器）：** 将手掷熔铁矢测试从“设置 noPhysics 并传送到 owner”改为正式 `MoltenTridentItem.use/releaseUsing` 发射、真实方块碰撞、保留 50% 消失分支（消失即按生产路径重投）、真实 Loyalty 回飞距离变化和 `playerTouch` 正常拾取。最终断言为 Loyalty III、恰好一枚、弹体移除。枪械/背包路径尚仍是入口级捷径测试，未据此宣称自然回收通过。
- **编译通过：** Forge 47.4.28、Java 17 下 `clean build gameTestModJar`/后续 `build gameTestModJar` 成功；生产 JAR 未改动，SHA-256 `E168EDD1EBAFF3E5E0BCE7B17E5F7C808408D1BC332F177AA8BA592861A25B1A`，973,459 B；测试 JAR SHA-256 `0967ABB69363D49A196DE4462E7A3B6F2A12B90BF7F8E1181D36BE8A52D8324D`，43,644 B。
- **专服运行通过：** 新测试 JAR 在目标版隔离专服 prepare/保存退出/verify 二次启动均通过；手掷 Loyalty 自然飞回和服务器已有玩法/NBT 检查通过。证据：`final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`。
- **客户端部分通过、双人仍失败：** GLFW 3.3.1 / Java 17 探针于 12:04:47 得到 `glfwInit=true`、`monitorCount=1`、非零 primary monitor。正式主 JAR 的 ForgePortTestG 加入专服，生产弹药环 UI 和 Focus_shooting 截图记录成功；灰白枪械几何仍在。ForgePortTestH 在 vanilla handshake 初始化后约 30 秒断开，未加入；本次双客户端结果为部分失败，不能说同步通过。证据位于 `docs/evidence/forge-47.4.28/final-multiplayer/current-attempt-G/` 及 `final-multiplayer/result.md`。
- **交付状态仍未完成：** 第二客户端握手原因未查明；观察者视角实体/弹体/音效同步、手动菜单操作、声音试听、枪械/背包 Loyalty 自然回收、枪械模型异常、自动炮塔连续行为等仍缺实际验证。

## 正式双客户端低负载复验补记（2026-10-26）

- 第一次 G→H 测试在双开环境中 H 登录超时；单独启动 H 随后成功，证明依赖组合/客户端目录可加载并连接。为排除图形负载因素，第二次双客户端测试临时把两套隔离 `options.txt` 调整到 render/simulation distance 4、FPS 30、mipmap 0、entity distance 0.5，并在结果生成后按备份恢复原选项。
- 第二次同哈希双客户端验收实际通过：ForgePortTestG、ForgePortTestH 同时加入 Forge 47.4.28 专服；两端均通过生产菜单 C2S 打开弹药环并记录 Focus_shooting 截图。详细结果为 `final-multiplayer/final-multiplayer-acceptance.txt`，完整日志及截图在 `final-ForgePortTestG/`、`final-ForgePortTestH/`。
- 截图可见图形异常：G 仍出现灰白枪械/玩家几何，H 的动画画面视角朝地面。双客户端连接及各自菜单/动画状态已验证；角色模型/摄像机正确性、对方观察视角下实体与弹体、声音及逐包同步仍未验证。不得将本项写成完整双人玩法通过。

## 最终复建与服务器复验（2026-10-26）

当前验收器最后修改后再次执行 `clean build gameTestModJar` 成功，并马上使用该构建输出运行 `run-final-server-acceptance.ps1`，两进程 prepare/保存/重启 verify 再次 PASS。主 JAR `build/libs` 与 `dist` 哈希均为 `E168EDD1EBAFF3E5E0BCE7B17E5F7C808408D1BC332F177AA8BA592861A25B1A`（973,459 B），GameTest JAR 为 `0967ABB69363D49A196DE4462E7A3B6F2A12B90BF7F8E1181D36BE8A52D8324D`（43,644 B）。此项确认构建与服务器验收，不代表尚未完成的真实视觉和观察者同步矩阵通过。

## 2026-10-26 后续继续：渲染矩阵与目标版复验

- 修复 `BowgunRigRenderer` 胸部步行 bob 额外平移未配对 PoseStack push/pop 的同级骨骼矩阵泄漏；执行 `clean build gameTestModJar`，Forge 47.4.28 / Java 17 正式映射隔离专服 prepare 与第二进程存档 verify 均通过。
- 新生产 JAR SHA-256：`2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`（973,544 B）；同哈希 G/H 双客户端通过 Forge 握手加入同服、打开生产弹药环并记录 Focus_shooting 截图。
- 胸部 bob 矩阵修复后，Focus_shooting 截图中的灰白枪械几何仍可复现；这项修复不构成视觉问题修复。自动演示脚本在后续仰角阶段重设相机，H 的某些晚期截图不能代表稳定手持视角；当前 UI/动画截图只能证明场景到达和渲染帧保存。
- 修正专服验收 PowerShell 对 Java 空退出码的误判，以及双客户端脚本在 Windows PowerShell 5.1 的 `Join-Path` 调用错误。原始输出与 PASS 结果见 `docs/evidence/forge-47.4.28/final-server-acceptance.txt` 和 `docs/evidence/forge-47.4.28/final-multiplayer/final-multiplayer-acceptance.txt`。
- 移植仍未完成：枪械异常根因、观察者端模型/弹体/粒子/声音同步、真实菜单手动/关闭/断线安全、枪械与背包自然 Loyalty 返还、全玩法矩阵与最终交付验收尚缺。

## 2026-10-26 后续继续：三路径自然忠诚回收与跨进程验收

- **已实现（验收器）**：将枪械和 Curios 背包 Loyalty 验收从传送到玩家的捷径改为真实墙面碰撞；按 50% 消失规则重试，存活弹体自然回飞并经正常 owner/playerTouch 拾取。断言背包来源标记只结算一次且只恢复一格充能。手持、枪械和背包三路径均在正式映射隔离专服通过。
- **已修复（验收器）**：将 GameTest 调用通过 Minecraft `Entity` 基类 API 发出，避免重混淆后对 `MoltenTridentEntity` 的开发态继承方法符号链接失败；将验收脚本旧日志断言改为与当前 PASS 文案一致。
- **构建/运行**：更新后的测试 JAR SHA-256 `B066BEB2D8B3E8644EF5FF52C1D7EBFBC017AA0CD686AFAB31D0AC1BE4C120A6`（44,889 B）。Forge 47.4.28 / Java 17 prepare、保存退出、第二进程 verify 均通过；枪械弹药环、选中槽、弹匣和背包升级 NBT 从磁盘重新读回通过。主生产 JAR未变，仍为 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`（973,544 B）。
- 详细 PASS 与完整服务端日志：`docs/evidence/forge-47.4.28/final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`。验收矩阵及交付候选说明已同步。
- **仍未完成**：枪械灰白几何根因、观察者端模型/弹体/粒子/声音同步、真实客户端手动 UI/关闭断线安全、完整输入/动作矩阵和开发 `runGameTestServer` 仍未通过；因此不能宣称移植完成。

### 正式源码恢复与最终复建（2026-10-26）

- 两项临时枪械骨骼跳过诊断结束后，已从 `BowgunGunLayer` 撤除诊断分支，并恢复 `run-final-multiplayer.ps1` 正式证据目录及无跳过参数。
- 主项目执行 `clean build gameTestModJar` 成功，日志 `docs/evidence/forge-47.4.28/final-clean-build-restored-production-proxy7890.log`。通过代理 JVM 参数（7890）后的离线构建完成；主 JAR 与 dist 哈希相同，测试 JAR 哈希也与专服 PASS 证据一致。
- 两种临时视觉隔离都没有消除 U 形灰块：跳过 case/change/弹匣/locator 组后仍在；单独跳过 barrel 后枪管断开而灰块仍在。G/H 两端证据分别在 `docs/evidence/forge-47.4.28/render-remaining-bones-diagnostic/` 和 `render-barrel-bone-diagnostic/`。根因未定、视觉项仍未通过。

## 2026-10-26 继续验收状态（当前权威记录）

- **构建**：正式源码下 `clean build gameTestModJar` 成功（`docs/evidence/forge-47.4.28/final-clean-build-official-source.log`）；主产物与 `dist/` SHA-256 均为 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`（973,544 字节），GameTest JAR 为 `B066BEB2D8B3E8644EF5FF52C1D7EBFBC017AA0CD686AFAB31D0AC1BE4C120A6`（44,889 字节）。
- **目标版服务器**：上述主哈希及测试哈希绑定的 Forge 47.4.28 / Java 17 隔离服两进程 prepare、保存退出、重启 verify 通过；风弹、枪械和各弹种关键路径、三种 Loyalty 自然撞墙/消失重试/回飞/拾取、菜单边界与方块破坏清理、枪械及背包跨进程 NBT 通过。记录见 `final-server-acceptance.txt`。
- **目标版双客户端**：G/H 同时连接同一隔离服、各自打开生产弹药环菜单并记录 Focus_shooting 画面通过，主哈希同上。观察者侧人物模型、弹体、粒子和声音同步，以及真实手动 UI 操作仍未通过。
- **未完成**：Focus 截图持续出现灰白枪械几何。两次逐骨骼染色尝试均在 Forge Early Display 被 `glfwGetPrimaryMonitor failed` 阻断，无诊断图/骨骼日志，根因未定位；不能声称修复。翻滚/滑铲完整输入、菜单关闭/断线、听感、观察者同步与全验收矩阵仍缺证据。因此项目状态仍为“移植未完成”，当前 JAR 是目标版验收候选而非完成交付。
- 本次临时诊断源码与启动参数已撤除；`BowgunGunLayer.class` 可执行字节码及行号表复建后与正式验收 JAR 一致。未触碰项目外源码、其他模组或真实存档。

## 2026-10-26 后续：Forge 窗口诊断与正式源码恢复

- 继续定位枪械灰白几何：检查已知装备，测试玩家只持枪和弹药，没有盔甲，因此截图中该几何不能归因于本模组盔甲层；关闭皮肤第二层及多个枪件组的旧对照也未消除它。查看 GeckoLib 4.8.4 实际 `GeoRenderer.renderRecursively` 字节码确认每个骨骼先渲染主方块，再调用每个 layer，随后递归并配对 PoseStack；胸部 bob 的额外矩阵也有 push/pop 配对。现有证据仍不足以确定是哪一根骨骼/哪一层。
- 为锁定来源重新构建了临时逐骨骼染色诊断 JAR。尽管独立 GLFW 探针报告两个 monitor 和非零 primary，Forge Early Display 仍报 `Can't find a primary monitor`；客户端窗口未创建，染色日志/截图为零，因此该探针不能证明 Forge 客户端可启动，诊断无结论。日志/说明见 `docs/evidence/forge-47.4.28/render-bone-tint-diagnostic/`。
- 已结束隔离客户端进程、恢复正式 `BowgunGunLayer` 与 `run-final-multiplayer.ps1`，随后再次执行正式源码 `clean build gameTestModJar` 成功；`build/libs` 与 `dist/` SHA-256 均为 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`，GameTest JAR 为 `B066BEB2D8B3E8644EF5FF52C1D7EBFBC017AA0CD686AFAB31D0AC1BE4C120A6`。当前无诊断 tint 开关残留。
- **仍在推进且未完成**：灰白几何根因、Forge 真实客户端视觉与 UI 操作、观察者同步、完整滚动/滑铲/射击矩阵。此前同哈希服务器和双客户端通过范围继续有效；本次显示器失败不覆盖那些通过证据，也不能把本次算作视觉通过。

### 本次续作更新（2026-10-26）

正式版完成 clean build 与 G/H 双客户端同服复测，两人均进入 Forge 47.4.28 / Java 17.0.14 隔离专服；两端的生产弹药环网络菜单检查和 Focus_shooting 实际画面记录通过。随后用独立临时染色构建复测灰白几何：枪械层骨骼均按预期变色，而异常几何保持灰色；因此枪械层的这些骨骼并非异常的直接来源。临时代码已移除，正式源码和 build、dist、服务端及两个客户端安装 JAR 哈希恢复一致。最新构建及验收记录见 `final-clean-build-official-source.log` 与 `final-multiplayer/final-multiplayer-acceptance.txt`；渲染诊断记录见 `render-bone-tint-diagnostic/result.md`。模型异常及远端同步/真实 UI/完整动作验收未解决，故仍不能宣称完成。

### 2026-10-26 身体层复查

枪械层染色的异常形状不变色后，改为给玩家主 rig 的六个身体骨骼染色；本轮 Forge Early Display 再次间歇性无法取得 primary monitor，身体诊断没进入 mod 加载且无结果。此前同环境正式版和枪械层诊断版已能双人启动，因此这里记录为运行环境间歇阻塞，不记作产品失败或修复。临时代码清理后 clean build 成功，正式产物哈希恢复；完整证据见验收矩阵和 `render-body-tint-diagnostic/result.md`。

### 本回合后续进度（2026-10-26）

身体层的显式隐藏对照使灰白主体明显减少但留下残片，确认主 rig 六个身体骨骼是异常画面的一个来源；该临时处理会丢失玩家身体，不可作为产品修复。枪械层逐骨骼染色对照中异常图案不变色。组合隐藏身体和第二层测试再次被间歇性 Early Display primary-monitor 错误阻断。恢复源码后再次 clean build 成功，正式 JAR 哈希对齐 dist 及三个测试安装目录。视觉根因还未完全收敛，正式观察者同步和完整手动 UI/动作矩阵也未验，移植未完成。详见验收矩阵及 `render-hide-body-diagnostic/result.md`。

### 组合诊断重试

第二次“隐藏身体 + 关闭第二皮肤层”测试仍未越过 Forge Early Display（primary monitor 为 0），因此没有组合渲染结论。临时代码和启动参数已清除；最终 clean build 成功，官方 JAR 哈希同步至 dist、专服及两客户端目录。记录在验收矩阵和 `render-hide-body-and-skinlayer-diagnostic/result.md`。

### 2026-10-26：越肩透明度 A/B 复测（未进入客户端）

为判断 Shoulder Surfing 的 `adjust_player_transparency` 是否造成枪械灰白几何，在隔离客户端将其暂设为 false；Forge 47.4.28 / Java 17.0.14 隔离专服与正式 JAR 均正常启动，但客户端再次在 Early Display 因 `glfwGetPrimaryMonitor failed` 退出，未连接、未加载模组、无可比较截图。本次对透明度影响**无结论**。仅停止该次隔离 javaw，客户端配置已恢复原哈希 `6A877C2741058D03468FFBD0BCA0BE4AA732986B2BB655A50CB981616BBE7015`，验收脚本证据路径已恢复；服务器属性、验收 JAR 及正式 JAR 均复核还原。记录见 `docs/evidence/forge-47.4.28/render-shoulder-transparency-off/result.md`。正式 JAR 未变（SHA-256 `2AC7A0D07F28CE4661D6474DEA19AAD714D33CE6271D741578377C9BB395B1D`）。项目继续标记为移植未完成。

### 灰白主体静态来源排除（2026-10-26）

复核验收脚本和正式代码：`/tkgun kit` 仅写入武器槽和 64 枚弩矢，不给玩家穿戴盔甲；`BowgunArmorLayer` 仅在对应 EquipmentSlot 真有 ArmorItem 时绘制。因此验收截图里的白灰主体不能由该验收命令授予的盔甲解释。`BowgunRigModel` 主体贴图动态读取当前玩家皮肤，`BowgunRigRenderer.renderCubesOfBone` 绘制六个玩家身体骨骼；此前只跳过这六根骨骼的成功截图对照显示灰白大块明显减少但仍有残片。这把排查优先级收敛到主体几何/玩家皮肤贴图与剩余 rig 绘制路径，但还没有根因结论。关闭 Shoulder Surfing 透明度的客户端 A/B 被 Forge Early Display 阻断，不能据此排除其影响。源码：`src/main/java/com/mechaniciancombat/client/rig/BowgunRigRenderer.java`、`BowgunRigModel.java`、`BowgunArmorLayer.java`、`src/main/java/com/mechaniciancombat/TkGunCommand.java`。

### Forge Early Display 故障环境复核（2026-10-26）

在越肩透明度 A/B 失败后检查本机显示状态：Windows Forms 看到 `WinDisc` 主屏幕 1644×768，但 Forge/LWJGL Early Display 仍认为没有 primary monitor。测试客户端启动 JSON 虽带 `-Dfml.earlyprogresswindow=false`，实际日志仍进入 `fmlearlywindow` 后失败，故该参数没有绕过此初始化路径。此结果不能证明是项目渲染缺陷或环境缺屏；A/B 与画面仍未测。记录见 `docs/evidence/forge-47.4.28/render-shoulder-transparency-off/display-enumeration.txt`。

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

### 2026-10-26 续验：干净构建与目标版专服回归

- **构建**：当前正式源码执行 `clean build gameTestModJar --offline --no-daemon`，Gradle 8.8 / Java 17 构建成功（21 个任务执行）；日志 `docs/evidence/forge-47.4.28/continuation-clean-build.log`。生产包 `build/libs` 与 `dist` 均为 973,544 字节，SHA-256 `2AC7A0D07F28CE4661D6474FDEA19AAD714D33CE6271D741578377C9BB395B1D`。
- **专服回归**：目标版 Forge 47.4.28 / Java 17 正式映射专服验收再次 PASS；prepare 后保存并停止，再由第二进程载入世界 verify。包括风弹、全部弹种/生产开火门控、手持/枪械/Curios 背包三路径 Loyalty III 真实撞墙及 50% 方块碰撞消失、三类菜单边界与方块破坏掉落、AUTO/便携背包发射定时/扣费/冷却、Curios 槽、持久化 NBT。汇总 `docs/evidence/forge-47.4.28/final-server-acceptance.txt`，原始运行日志 `continuation-final-server-runner.log`。
- **客户端状态**：此前同生产哈希 G/H 双客户端连接并渲染生产弹药 UI 的证据仍有效；客户端真实手动槽操作、完整 UI 生命周期、远端观察者实体/弹体/粒子/声音同步以及可接受的枪械几何仍未通过。当前复查没有把截图阶段等同于客户端完整验收，移植状态仍为未完成。

### 2026-10-26：弹药环真实屏幕点击回环（已实现/编译，客户端运行待测）

- **实现**：隔离验收参数 `-Dtkgun.demoUiTransfer=1` 开启后，客户端在正式弹药环屏幕中用屏幕 `mouseClicked` 命中测试执行：从热栏取起 64 枚弩矢、放入环槽、取出、放回原热栏格；逐阶段断言物品 ID/数量、光标空态和环槽空态，并在完成前保留 UI 截图。默认关闭，不改变正常玩法。双客户端验收脚本现要求 G/H 两端均有 `PASS ammo_storage_click_roundtrip count=64`，并清空隔离实例旧 `tkgun_*.png` 截图，防止历史图像混入；现在会为服务端和两客户端逐一安装/校验同一主 JAR。
- **测试优先**：新证据检查器在实现前因缺少 `PASS ammo_storage_click_roundtrip` 正确失败（RED）。实现后的 `compileJava`、`clean build gameTestModJar` 均通过；目标版专服 prepare/存档停机/第二进程 verify 再次 PASS，主 JAR SHA-256 为 `410F78A5BEEE35B218B27D74C52DF081FA267218A7F6820609E4835A533D3265`。
- **运行阻塞**：双客户端验收在 Forge Early Display 前退出，错误仍为 `Can't find a primary monitor`。同一时间直接用目标 Java 17 与隔离 LWJGL 3.3.1 探针读取到 `glfwInit=true, monitorCount=0, primaryMonitor=0`；新 UI 回环尚未运行，证据检查器仍正确报缺少客户端 PASS 标记。不得把源码编译或服务器通过写成客户端点击通过。
- **产物**：`build/libs/` 与 `dist/` 已同步到上述 975,296 字节候选 JAR。构建/运行记录：`ui-click-transfer-compile.log`、`ui-click-transfer-clean-build.log`、`ui-click-transfer-server-runner.log`、`ui-click-transfer-multiplayer-runner.log`、`glfw-current-probe-continued.log`、`ui-click-transfer-runtime-blocked.log`。源码改动位于 `GunDebugHarness.java`、`AmmoStorageScreen.java`；验收脚本为 `run-final-multiplayer.ps1` 与 `verify-client-ui-transfer-evidence.ps1`。
- **截图证据校正**：复核发现 `render-hide-body-diagnostic` 中 `tkgun_1515_Focus_shooting.png` 与较早正式截图字节相同（均 12:07），不得引用为新对照；该诊断报告实际列出的 `tkgun_315_Focus_shooting.png` 时间为 13:56，属于后续运行截图，分辨率与正式对照不同，仅保留定性观察、不作像素级因果结论。

移植仍未完成：目前新增的真实菜单点击路径需要在 GLFW 能枚举到主显示器后重跑；枪械视觉、观察者同步、完整客户端战斗/动画和声音验收也仍待完成。

### 2026-10-26：弹药环客户端真实点击回环修正与 Forge 47.4.28 复验

- **诊断结论**：上一版隔离验收只调用 `mouseClicked`。取物点击会直接提交，但光标携带物品后，Minecraft 1.20.1 容器进入 QUICK_CRAFT 手势；必须继续经过拖动/释放才能结束点击。旧日志证明服务端只收到取起弩矢的 PICKUP，未收到向弹药环放入的请求，故问题在验收输入模拟，不是先前判定为服务器拒绝。
- **改动**：验收屏幕辅助方法现在在点击后补齐原版拖动与释放回调；加入由 JVM 参数显式开启的点击前后追踪，仅供隔离验收使用，默认关闭。
- **构建**：`clean build gameTestModJar --offline --no-daemon` 在 Gradle 8.8 / Java 17 成功。正式 JAR 977,489 字节，SHA-256 `4FD8253FC983DA218E80C80A13A9F2D6BB57D6DF6971512AE737B3F07DB15445`；`dist`、隔离服务端和两个客户端均同步同一哈希。日志：`ui-click-roundtrip-fix-build.log`。
- **双客户端实测**：Forge 47.4.28 / Java 17.0.14 隔离专服同时接入 ForgePortTestG/H。两端均经生产弹药环 UI 完成 64 枚弩矢取起、入环、取出、放回原热栏格，物品数守恒；专服记录 QUICK_CRAFT 完整阶段。`status=PASS_TWO_CLIENTS_CONNECTED_AMMO_UI_CLICK_ROUNDTRIP_AND_FOCUS_ANIMATION_RECORDED`。结果：`final-multiplayer/final-multiplayer-acceptance.txt`；客户端原始日志与截图位于 `final-multiplayer/ForgePortTestG/`、`ForgePortTestH/`；运行器日志：`ui-click-roundtrip-fix-multiplayer-runner.log`。
- **专服实测**：相同 JAR 在 Forge 47.4.28 / Java 17.0.14 完成 GameTest prepare、保存停服、第二进程 verify，最终 `status=PASS`。涵盖风弹、全部弹种、手持/枪械/Curios 背包 Loyalty III 与方块命中 50% 消失、菜单过滤/边界/破坏清理、枪械与背包持久化、AUTO/便携发射时序及资源、目标切换/遮挡、开火门控和背包自然充能。记录：`final-server-acceptance.txt`、`final-server-prepare.log`、`final-server-verify.log`；运行器日志：`ui-click-roundtrip-fix-server-runner.log`。
- **边界**：双客户端运行仍需 `-HeadlessEarlyWindowProbe`（隔离实例将 Forge `earlyWindowControl` 临时置 false，退出时恢复），不代表默认 Forge Early Display 客户端启动通过。真实人工 UI、改装台完整交互、观察者侧实体/弹体/粒子/声音同步、实际听感、翻滚/滑铲动作、枪械灰白几何根因仍未完成。移植状态继续为**未完成**。

### 2026-10-26：生产改装台 UI 双客户端实操与最终 Java 17 产物

- **UI 验收器修正**：新加的改装台路径第一轮因测试器读取错菜单槽位失败，保留记录；确认 `BackpackModificationTableScreenHandler` 的玩家快捷栏起始索引为 32 后，改用槽 34 检查快捷栏第 3 格。产品菜单逻辑未因该诊断修改。
- **双客户端实际界面**：同一 Forge 47.4.28 隔离专服中，ForgePortTestG/H 各自通过生产方块交互打开 `BackpackModificationTableScreen`，以真实屏幕 click/drag/release 放入背包、核心、便携发射器；客户端核实服务端同步槽位与 `BackpackUpgradeData` NBT，截图记录安装前后界面。与弹药环 64 枚点击回环、Focus_shooting 同轮均通过。结果 `final-multiplayer/final-multiplayer-acceptance.txt`，客户端日志/截图在 `final-multiplayer/final-ForgePortTestG/`、`final-ForgePortTestH/`；脚本输出 `table-ui-final-multiplayer-runner.log`。
- **Java 17 可复现构建**：之前一次 Gradle 配置因 ForgeGradle HTTPS 连通检查无代理而等待；线程栈定位到 `EnvironmentChecks.testServerConnection` 后，使用 JDK 17、`127.0.0.1:7890` HTTP/HTTPS 代理重跑成功。连续两次 `clean build gameTestModJar --offline --no-daemon` 主/验收包哈希一致，主包 `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D`（980,699 B），验收包 `C2B096A7DF011FBE046BF93B97607C5C17151ED3700B8D81D352A59B5852725D`（46,647 B）。复现记录 `table-ui-final-reproducibility.txt`。
- **最终目标版专服复验**：相同主包哈希在 Forge 47.4.28 / Java 17.0.14 两进程验收通过；见 `final-server-acceptance.txt` 与 `table-ui-final-server-runner.log`。dist、专服、两个客户端安装包哈希一致；最终静态检查覆盖 87 个 JSON、Curios/方块标签、最低依赖范围、包名及 13 条网络契约。
- **尚未完成**：默认早显客户端仍因运行环境无法枚举主显示器而在模组初始化前退出；真实人工操作/异常关闭断线与玩家背包重登持久化、观察者端战斗模型/弹体/击退/粒子/声音同步、实听音效、完整翻滚滑铲与枪械灰白几何根因仍未验证。不得宣称完整移植。

### 默认 Forge Early Display 客户端启动复测（2026-10-26，新哈希）

- 主 JAR：`4FD8253FC983DA218E80C80A13A9F2D6BB57D6DF6971512AE737B3F07DB15445`，Forge 47.4.28 / Java 17.0.14。未启用隔离早显绕过参数，直接使用默认 `earlyWindowControl=true`。
- 结果：客户端在模组初始化前退出/挂起，Early Display 报 `Can't find a primary monitor` / `glfwGetPrimaryMonitor failed`；专服正常启动但该次无客户端加入。失败不能归因于本 mod，也不能作为默认客户端通过。隔离测试窗口进程已停止；运行器 finally 已恢复两份客户端 `config/fml.toml`、服务器 `server.properties` 和验收器 JAR。
- 证据：`ui-click-roundtrip-fix-default-window-runner.log`、`validation/forge-47.4.28/client-skin-off/final-multiplayer-client.err.log`、该客户端 `logs/latest.log`、`final-multiplayer-server.log`。之前同哈希 `earlyWindowControl=false` 双客户端 UI/动画/菜单回环 PASS 仍独立有效。

### 2026-10-26：双客户端 demo 结束标记复核

- **结果**：同一正式哈希 `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D` 在 Forge 47.4.28 / Java 17.0.14 隔离专服上，ForgePortTestG/H 均加入成功；两端分别通过弹药环 64 枚点击回环、改装台背包/核心/便携发射器 UI 安装及服务端槽位/NBT 同步，并保存专注动画截图。两端还生成新 `screenshots/tkgun_t1525_none.png`，证明 demo 主循环到达 tick 1525。复核端侧日志后确认该标记没有逐项验证翻滚键、滑铲或射击生效，验收器已更名为 `demoEndMarker=True`。
- **证据**：`docs/evidence/forge-47.4.28/final-multiplayer/final-multiplayer-acceptance.txt`、`final-multiplayer-server.log`、逐端 `final-ForgePortTestG/` 与 `final-ForgePortTestH/` 截图/日志。
- **验收边界**：tick 结束截图只证明 demo 循环进度，不证明动作执行。物理键盘、翻滚/滑铲/射击实际响应与动作质量均未验。截图仍有灰白角色/枪械几何；观察者端战斗模型/弹体/粒子/击退/音效同步、人工可用性和菜单异常关闭/断线恢复仍未通过；因此总体仍是**移植未完成**。

### 2026-10-26：玩家 Inventory NBT 重登持久化

- 在 `acceptance-final-multiplayer-20261026` 项目隔离世界中，以 Forge 47.4.28 / Java 17.0.14 和同一正式主 JAR 启动两次独立专服进程。ForgePortTestG 正常加入，记录服务端 `Inventory` NBT，执行保存与断开，专服正常关闭；第二进程重启后同一身份重新加入，再次读取 Inventory。
- 两次 Inventory NBT 完全一致：包含带弹匣数据的 `tkgun:bowgun`、64 枚 `tkgun:bolt`、便携发射器和核心。原始 NBT、两端客户端/服务端日志及可重跑脚本见 `docs/evidence/forge-47.4.28/player-relog/` 与 `validation/forge-47.4.28/verify-player-relog.ps1`。验收后服务器属性、JVM 参数、客户端早显配置和 GameTest JAR 均已复原。
- **范围**：这是单玩家、单次正常保存/重启/重连验证；多人长期存档、异常断电、死亡/换维度、异常菜单断线仍未测，不据此扩大结论。

### 2026-10-26：真实输入工具窗口发现受阻

- 复跑 Forge 47.4.28 双客户端隔离验收，生产 UI 回环、改装台 NBT 同步和动画截图均 PASS；新的 runner 输出保持“仅 demo end marker”，不把 tick 到达解释为翻滚/滑铲/射击成功。
- 尝试人工键盘输入时，Computer Use 窗口清单没有暴露正在运行的 Java Minecraft 窗口；未向游戏发出按键，真实动作输入仍未验。记录：`docs/evidence/forge-47.4.28/final-multiplayer/result.md`。
- 当前 `build/libs` 与 `dist` 均为 SHA-256 `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D` / 980,699 字节；复跑 `verifyPortContracts --offline` 通过，日志 `continued-verifyPortContracts-20261026.log`。主 JAR 未因本次文档校正改变。

### 2026-10-26：清除残留渲染诊断代码并复验正式产物

- **源码与构建输入复核**：前一轮的 `BowgunRigRenderer` 文件备份实际仍包含逐骨骼 tint 诊断分支；因此第一次 clean build 产出的 `5ABBDA48…` 诊断 JAR 与 `dist` 不同。已从生产源码移除该临时分支，运行期渲染逻辑恢复到正式候选；不改玩法或资源。
- **构建**：JDK 17、Gradle 8.8、7890 代理下再次执行 `clean build gameTestModJar --offline --no-daemon --console=plain`，`BUILD SUCCESSFUL`。正式 JAR `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D`（980,699 B），验收 JAR `C2B096A7DF011FBE046BF93B97607C5C17151ED3700B8D81D352A59B5852725D`（46,647 B）；构建 JAR、dist、隔离服务端和 G/H 客户端副本均已核对为同一正式哈希。构建日志：`docs/evidence/forge-47.4.28/continued-clean-build-restored-jdk17-20261026.log` 与 `continued-clean-build-repeat-jdk17-20261026.log`，两次相邻正式源码 clean build 产物哈希相同。
- **正式专服**：当前正式哈希再次完成 prepare、正常保存停服、第二进程 verify，`status=PASS`。证据：`continued-final-server-runner-20261026.log`、`final-server-acceptance.txt` 及 prepare/verify 日志。
- **双客户端**：G/H 以同一正式哈希接入 Forge 47.4.28 / Java 17.0.14 隔离服；两端生产弹药环 64 枚点击回环、改装台安装背包/核心/便携发射器与服务端 NBT 同步、瞄准动画截图均 PASS。`demoEndMarker=True` 仅表示循环到达指定 tick，不证明翻滚/滑铲/射击输入。运行日志：`continued-final-multiplayer-runner-20261026.log`；逐端证据与截图在 `final-multiplayer/`。
- **渲染隔离对照**：临时诊断 JAR `5ABBDA485EF982A199C956885BC0559126188AB81B7CAA6AAAF57313E8DAE69D`（非交付）逐一染色六个身体骨骼，双客户端截图显示身体纹理层确实呈现对应颜色；同画面灰阶高耸部分保持灰色。当前 `bowgun_gun.png` 为灰阶金属贴图，故剩余灰阶形状更符合自有枪械材质的表现；这只是资产/截图对照推断，尚未与原作者目标画面或完整第三人称视角逐项比对，故不把视觉项改记为通过。正式源码无诊断分支，正式 JAR 与上述临时包明确区分。结果：`docs/evidence/forge-47.4.28/render-body-tint-probe-clean-world/`。
- **未通过/未测**：当前正式哈希已在默认 Forge Early Display 下完成 G/H 双客户端启动与联机（见下方续验补记）；此前 `glfwGetPrimaryMonitor failed` 仍作为间歇性历史阻断保留。真实键盘动作、远端观察者的实体/粒子/击退/声音同步、实听音效、人工可用性与异常菜单断线恢复仍需验收。总体继续标记为移植未完成。

### 2026-10-26：当前正式哈希默认早显双客户端通过

- **默认启动**：在未加 `-HeadlessEarlyWindowProbe` 的情况下，以 `earlyWindowControl=true` 默认配置启动 Forge 47.4.28 / Java 17.0.14 隔离专服与 ForgePortTestG/H。两名客户端均成功启动并加入，主哈希均为 `015048C12297DC4E68A22B2A085386823B02B5FE369E0255E41B3941672B7A8D`。
- **客户端阶段**：G/H 均通过生产弹药环 UI 打开、64 枚点击回环/数量守恒、改装台背包核心/便携发射器安装及服务端 NBT 同步、瞄准动画截图。结果 `PASS_TWO_CLIENTS_CONNECTED_PRODUCTION_UI_ROUNDTRIPS_BACKPACK_TABLE_SYNC_AND_DEMO_END_MARKER_RECORDED`。tick marker 不证明翻滚/滑铲/射击实际输入；观察者端实体/击退/粒子/声音同步未测。
- **证据**：`continued-final-default-window-runner-20261026.log`，端侧日志 `validation/forge-47.4.28/client-skin-off/logs/debug.log`、`client2-skin-off/logs/debug.log` 与更新后的 `final-multiplayer/` 截图/验收结果。运行后 `earlyWindowControl=true` 与服务器属性/JVM 参数均恢复。
- **状态校正**：默认早显之前出现的 GLFW 失败不是稳定重现；当前正式哈希本轮默认配置双端通过启动与联机。移植仍未完成，待测项见验收矩阵。
