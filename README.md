# ftbqauto — FTB 任务自动化

FTB Quests 的附属模组，让**物品提交任务**和**奖励领取**能自动化。

- MC 1.21.1 / NeoForge 21.1.228 / Java 21
- 对应 FTB Quests `2101.1.33`

## 三个内容

**两个方块绑的都是「任务」（Quest）** —— 就是你任务地图上看到的那个节点。
子任务（Task）和奖励（Reward）是它的内部结构，不用你分别去绑。

### 任务提交器（Quest Submitter）

27 格容器，能被漏斗/管道存取。绑定一个任务后，**红石上升沿**
（信号从无到有的瞬间）把箱内所有能提交的物品一次性提交掉，
覆盖该任务下的**所有**物品子任务。提交多少扣多少，剩下的留在箱里。

正面是一整块**纯黑屏幕**，上方渲染任务标题、中间渲染任务图标（对标 FTB 任务显示器）。
标题过长会自动缩小以免溢出方块。

### 奖励领取器（Reward Claimer）

27 格容器。绑定任务后每 2 秒检查一次：任务完成且有没领的奖励就自动领掉。

| 奖励类型 | 去向 |
|---|---|
| 物品（`ItemReward`） | 进方块自己的库存 |
| 随机奖励 / 战利品箱 / 整表（`RandomReward` / `LootReward` / `AllTableReward`） | 先把表摇成具体奖励，再逐个按下面的规则处理 —— 物品进方块，其余给玩家 |
| 经验、命令、阶段、进度等 | 发给绑定玩家 |
| **选择型（`ChoiceReward`）** | **不自动领** —— 必须你自己在任务界面点选 |

- 玩家离线时非物品奖励留着不动，等他上线再领；物品奖励离线也能领
- 先确认库存塞得下才标记"已领取"，塞不下就跳过，不会吞奖励
- 正面同样是纯黑屏幕 + 任务标题 + 图标

### FTB 任务绑定器（Quest Binder）

| 操作 | 效果 |
|---|---|
| 右键空气 | 打开 **FTB 原版的任务搜索界面**（就是任务界面里 Ctrl+F 那个），选中即绑定到本物品 |
| 右键提交器/领取器 | 把物品上绑的任务写到方块上 |
| 潜行右键空气 | 清空绑定 |

绑定器还没绑东西时，直接右键方块会打开搜索界面，选完直接绑上去，省一步。

绑定会跟着方块掉落物走，拆了重放不用重新绑。

## 绑定归属：玩家 vs 队伍

这块 FTB 自己的设计就是分裂的，模组照搬：

| | 归属 |
|---|---|
| **绑定身份** | **玩家** —— 存玩家 UUID + 名字，谁拿绑定器右键方块就算谁的 |
| **任务进度**（提交器） | **队伍** —— FTB 的进度本来就是队伍级的，运行期把玩家 UUID 解析成他所在的队伍 |
| **奖励领取状态**（领取器） | **玩家** —— FTB 的 `claimedRewards` 按 `(玩家, 奖励)` 记，同队每人可各领一份 |

实际效果：提交器往**队伍进度**里交，领取器只替**绑定玩家**领他自己那份。
绑定玩家退队/换队，方块跟着走。

玩家的名字和 UUID 一起存在方块里（而不是运行时查档案），因为玩家离线时
客户端查不到别人的名字；名字会随方块实体同步到客户端，拆了重放也不丢。

**绑定器本身不记名** —— 它只是一把钥匙，归属在"右键方块"那一刻决定。

## 服务端与兼容性

- **零 mixin**：整个模组没有一处字节码修改，全部是对 FTB 公开 API 的正常调用，
  不存在覆盖/劫持 FTB 原本逻辑的可能。
- **服务端安全**：服务端用到的 FTB 类（`TeamData` / `Reward` / `ItemTask` /
  `RewardTable` 等）全是双端的。`QuestObjectBase.getTitle()` 自带
  `isServerSide()` 分支；`getIcon()` 是 `@Environment(CLIENT)`，只在渲染器里调。
  客户端类全部有门禁（`Dist.CLIENT` 订阅 + `level.isClientSide` 分支）。
- 选择型奖励显式跳过，留给你在 FTB 界面手动选；领取器领过的奖励不会重复领。

## 物流与联动

- **容器兼容主流物流**：两个方块都注册了 NeoForge 的
  `Capabilities.ItemHandler.BLOCK`，机械动力的漏斗/溜槽、其他物流模组、
  原版漏斗都能正常进出。六面均可存取。
- **玉（Jade）联动**：看向方块会多显示一行"绑定任务：xxx"；没绑就显示"未绑定任务"。
  没装 Jade 也不会报错（插件类由 Jade 自己扫描加载）。
- **物品提示**：两个方块物品都带用法说明（悬停可见）。

## 用法

1. 拿绑定器右键空气 → 搜索任务 → 点击选中
2. 拿绑定器右键方块 → 绑定完成（容器标题会显示绑了什么）
3. 空手右键方块 → 打开容器界面
4. 提交器接红石脉冲；领取器放那儿自动跑

## 构建

```bash
gradlew build           # 产物在 build/libs/
```

想构建完直接丢进整合包，用 `-Pdeploy_dir` 指定 mods 目录：

```bash
gradlew deployToPack -Pdeploy_dir="<整合包>/mods"
```

（路径不写死在仓库里，也可以放进本机的 `~/.gradle/gradle.properties`。）

需要联网拉依赖。如果你的网络需要代理，把 `systemProp.http(s).proxyHost/Port`
写进 `~/.gradle/gradle.properties` —— 不要写进项目里的 `gradle.properties`，
那会让没有代理的人构建失败。

贴图是程序化生成的，改了 `tools/gen_textures.js` 后跑：

```bash
node tools/gen_textures.js
```

## 实现要点（改代码前值得先看）

- **提交**用的是 `ItemTask.insert(TeamData, ItemStack, boolean)`。
  **不要**用 `ItemTask.submitTask(...)` —— 那个扣的是**玩家背包**，方块场景用不了。
- **领奖**走 `Reward.automatedClaimPre/Post`（FTB 给"方块自动领奖"预留的钩子，
  它自家的战利品箱开启器就走这条）。`automatedClaimPre` 只往 `items` 里塞东西、
  没有副作用，`automatedClaimPost` 才是真正发放 —— 所以可以"先算一遍，
  塞不下就整条放弃"，不会丢奖励。
- **`RandomReward` / `LootReward` / `AllTableReward` 的 `automatedClaimPre` 是硬编码返回
  `false` 的**。它们本身不是奖励，而是"抽奖容器"。看到 `false` 不能直接跳过、
  也不能图省事回退到 `data.claimReward(player, ...)` —— 那样 `RandomReward.claim()`
  会把摇出来的物品**直接塞进玩家背包**。正确做法是拿 `getTable()` 自己摇
  （`generateWeightedRandomRewards`），把具体奖励逐个按同样规则展开，
  最后标记的是**父奖励**（因为 `quest.getRewards()` 里只有它，子奖励标记了也没用）。
- **`ChoiceReward.claim()` 是个空方法**。如果把它标记成已领取，奖励会凭空消失。
  所以代码里显式跳过 `RewardTypes.CHOICE`。
- **队伍归属**：绑定存的是玩家 UUID，队伍在运行时解析，所以玩家离线方块也能工作。
- **容器持久化必须自己写**。继承 `BaseContainerBlockEntity` **不会**自动获得物品存档 ——
  它的 `saveAdditional`/`loadAdditional` 只管 lockKey 和 CustomName。
  每个容器子类都得自己调 `ContainerHelper.saveAllItems/loadAllItems`
  （原版 `DispenserBlockEntity` 就是这么写的）。
  漏掉的话方块里的东西永远不会写进 NBT，**区块一存一读就没了**。
  `loadAllItems` 是按槽位覆盖、不清空，所以加载前要先把列表换成新的空表。
- **物流兼容不能只靠 `WorldlyContainer`**。光实现 `WorldlyContainer` 只够原版漏斗用；
  机械动力的漏斗/溜槽和其他物流模组走的是 NeoForge 的
  `Capabilities.ItemHandler.BLOCK`，不注册这个能力它们会认为方块里没东西。
  见 `registry/ModCapabilities.java`，用 `InvWrapper` 包一层即可。
- **搜索界面**复用了 `dev.ftb.mods.ftbquests.client.gui.SelectQuestObjectScreen`。
  它的右上角 X **只触发回调、不关界面**（FTB 拿它当配置弹窗），
  所以回调里必须自己调 `closeGui(true)`。
- **正面渲染**参考 FTB 的 `TaskScreenRenderer`：把原点挪到方块中心，
  绕 Y 轴转到正面朝外，再沿 +Z 推出去。这样局部 +X 恒为观察者右手边，
  直接按 GUI 的方式画即可，不用翻转。尺寸常量都在 `BoundBlockRenderer` 顶部。
- 正面贴图是纯黑的（`gen_textures.js` 里的 `blackFace()`），只当屏幕底板用；
  `_front_on` 目前和 `_front` 一样，是留给以后加"通电/刚领到"视觉提示的钩子，
  blockstate 已经把两种状态分好了，改那一张图即可。

## 已知注意事项

- `SelectQuestObjectScreen` 等 GUI 类**不在** FTB 的稳定 API 包
  （`dev.ftb.mods.ftbquests.api`）里。当前版本可用，但**升级 FTB 大版本后需要重新验证**。
- FTB 的许可证是 `All Rights Reserved`，所以贴图全部自绘，没有复用它的资源。
- 搜索框按**任务标题**过滤（FTB 原版行为），不是按物品名。
- 方块正面的图标只支持物品类图标；任务用的是自定义图片图标时正面不显示任何东西
  （纯黑屏幕）。
