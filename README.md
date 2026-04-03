# Delta Force Mod - 三角洲行动兵力系统

## 简介
这是一个为 Minecraft Forge 1.20.1 制作的模组，添加了类似三角洲行动的兵力占领系统。

## 功能
- 兵力系统：GTI攻方120兵力，HAAVK守方无限兵力
- 队伍管理：分配玩家到 GTI 或 HAAVK
- 据点系统：自定义创建据点区域
- 占点系统：顺序占领据点（1区、2区、3区...）
- 阵营独立BossBar：GTI看红色进攻条，HAAVK看蓝色防守条
- 数据持久化：自动保存游戏数据

## 命令
- `/deltaforcesystem help` - 查看帮助
- `/deltaforcesystem start` - 开始游戏
- `/deltaforcesystem team set <玩家> GTI/HAAVK` - 分配队伍
- `/deltaforcesystem stronghold set <名称>` - 创建据点
- `/deltaforcesystem stronghold order <数字> <据点1> [据点2]...` - 设置占点顺序

## API
其他模组可以通过 `com.deltaforce.deltaforcemod.api.DeltaForceAPI` 调用本模组功能。

## 依赖
- Minecraft: 1.20.1
- Forge: 47.4.18