# Forge 1.20.1 移植候选包说明

## 当前状态

目标版本为 Minecraft 1.20.1 / Forge 47.4.28 / Java 17。用户已批准以官方 Curios API 替换 Accessories；迁移源码已通过两次可复现的 JDK 17 clean build，并在隔离正式映射专服完成 Curios `back` 槽/标签/装备查询断言及风弹服务端行为复验。开发客户端完成模组初始化与资源加载，但真实 UI/背包渲染未验收。当前 JAR 仍是未完成全部玩法验收的移植候选。详见 [移植进展](PROGRESS.md) 与 [目标版验收矩阵](evidence/forge-47.4.28/acceptance-matrix.md)。

## 构建产物

- JAR：dist/MechanicianCombat-1.20.1-forge-0.1.0-porting.jar
- Minecraft：1.20.1
- Loader：Minecraft Forge 47.4.28
- Java：17
- SHA-256：`D7FE67FDDB9FF885E6CF325333A1E1A4A4C4A2D07F96FB945373ED0DDBAA9989`（2026-10-26 交付候选；两次目标版 clean build 字节一致，正式映射专服已用该哈希复验）
- 大小：972,241 字节

## 前置与最低版本

运行时依赖由 src/main/resources/META-INF/mods.toml 按最低版本范围检查，最低版本及以上可通过版本门槛；更高版本的实际兼容性仍需测试。docs/DEPENDENCIES.json 保存当前构建和验收使用的准确文件、版本及哈希，供可复现构建，不表示运行时只接受该精确版本。

| 前置 | 最低版本 | 当前验证/锁定版本 |
| --- | --- | --- |
| Curios API | 5.11.1+1.20.1 | 5.11.1+1.20.1 |
| GeckoLib | 4.8.4 | 4.8.4 |
| Player Animator | 1.0.2-rc1+1.20 | 1.0.2-rc1+1.20-forge |
| Shoulder Surfing（客户端） | 1.20.1-4.21.0 | 1.20.1-4.21.0+forge |
| Combat Roll | 1.3.3+1.20.1 | 1.3.3+1.20.1-forge |
| Cloth Config | 11.1.136 | 11.1.136+forge |

该 Mod JAR 不内置以上外部模组。客户端与服务端应安装兼容版本；下载来源和文件哈希以 [依赖锁文件](DEPENDENCIES.json) 为准。

## 获取与整合包

移植提供 tkgun:wind_charge 物品 ID，不包含风弹配方或掉落；整合包可按需配置获取方式。忠诚附魔只对熔铁弩矢开放，支持附魔台和铁砧，不扩展其他附魔。

## 已知验证边界

完整构建、专服断言范围和失败项目见验收矩阵。当前未完成客户端主菜单与真实 UI、完整枪械和弹种、忠诚返航拾取、背包发射器、炮塔、动作/动画/声音、菜单安全、存档重启读回和目标版双人同步验收。


