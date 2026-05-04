-------------总大纲-------------------
模块一：大世界底层架构与环境推演系统（已完成概要，需补全参数）

ECS架构实体与组件设计细则（生灵、宗门、物品的 Component 字段详尽定义）。

全局 Tick 时间步进推进逻辑与多倍速加速（时间流速）的具体实现代码思路。

天地灵气能量守恒模型数学公式，以及“末法时代”触发的精确数值阈值。

区域网格（Grid）地图数据结构与底层寻路/灵脉迁徙判定条件。

模块二：生灵属性基底与境界成长演化系统

修士NPC底层数据字典（生命、真元、寿元、灵根资质、气运值、性格标签等具体变量类型与取值范围）。

全大境界/小境界突破判定公式与消耗配置表（升级经验指数曲线、突破成功率基础值、雷劫伤害计算公式）。

底层散众（宏观统计）与高阶老怪（效用AI）的双轨推演流转图细节。

各族群先天属性修正倍率表（例如妖族肉身免伤加成、魔族吞噬经验效率等具体浮点数系数）。

模块三：宗门阵营与社会关系拓扑推演

宗门评级考核与核心底蕴数据模型（护宗大阵耐久度计算、灵脉品阶对宗门的灵气加成公式）。

宗门阶级剥削逻辑与升仙大会/血色试炼（秘境）周期刷新的时间轴配置。

宗门间外交状态机（吞并、结盟、宿敌）的转换条件与大规模宗门战争的自动演算逻辑。

人际关系图谱构建规则（仇人、道侣、师徒的结成概率计算，以及神识寻仇追踪算法）。

模块四：天道操作台与法则因果系统

天道干预 UI 面板指令集，及其对应的底层核心数据修改接口（API）。

天命气运系统逻辑暗线（详细说明如何在代码层面实现“气运之子”坠崖不死反获神功的保底机制）。

天灾祥瑞事件池触发概率与全图 Debuff/Buff 数值核算公式。

天道本源能量（玩家造物货币）的产出剥削公式与各项天道操作的消耗定价表。

模块五：修仙百艺、物资产出与奇物唯一性系统

功法/丹方/阵法推演进度条算法，以及传承断代遗失判定的具体逻辑。

大世界坊市物价动态平衡波动逻辑（基于供需关系的简易经济学算法）。

唯一性古宝/灵宠的生成规则，与持宝人坐标暴露（全图引怪黑吃黑）的具体判定半径。

大能遗迹洞府生成器（防御阵法等级、宝库容量与探索伤亡率的随机数生成公式）。

模块六：底层战斗核算与绝对边界控制

境界战力绝对压制防破防公式（计算攻击力与护体真元减伤的底层数学模型，确保跨大境界绝对无法破防）。

斗法胜率计算器（结合法宝品阶、功法五行克制、当前境界的极简自动推演公式）。

死亡结算与能量回收逻辑（修士陨落后的爆率、储物袋掉落机制、以及自身真元反哺天地的比例）。

模块七：安卓工程落地与本地存储架构

纯本地 SQLite 数据库表结构完整设计（Player、Sect、Entity、World_Config 等 Table 的主外键关联与字段设计）。

离线挂机收益期望值算法（避免暴力穷举几十万生灵，采用宏观数学期望瞬间结算离线万年数据的方案）。

Data-Driven UI（数据流UI）的数据双向绑定与渲染解耦方案（如何避免底层几十万次高频推演卡死主线程）。




----------------------------分-------------------------------------------------------------------------------------
模块一：大世界底层架构与环境推演系统”**的详细设计。这些设计标准可直接映射为 C# 中的 `struct` 和 `class`，完美契合安卓单机性能优化的需求。

---

# 模块一：大世界底层架构与环境推演系统（详细设计案）

本模块是整款游戏的基石。在这里，我们不谈任何UI图片，只谈内存里跑着的数据结构和维持大世界运转的数学公式。

## 1. ECS架构实体与组件设计细则 (Entity-Component-System)
在动辄几十万修士的世界中，决不能给 NPC 挂载传统的 GameObject。我们必须采用 ECS 架构（或轻量级结构体数组），将数据与逻辑彻底剥离，以保证安卓设备的 CPU 缓存命中率。

*   **实体 (Entity)**：在底层仅仅是一个全局唯一的 `int entityId`。
*   **组件 (Component)**：纯数据结构体 (`struct`)。只有拥有特定属性的生灵才会被分配对应的组件。

**核心组件结构定义表（C# Struct 参考）：**

| 组件名称 (Component) | 字段定义与数据类型 | 策划备注与作用 |
| :--- | :--- | :--- |
| **`BaseInfoComponent`** | `int age` (当前年龄)<br>`int maxLifespan` (寿元上限)<br>`byte linggenType` (灵根类型枚举)<br>`short luckValue` (气运值，影响保底) | 基础生理属性。寿命耗尽（age >= maxLifespan）将直接触发逻辑抹杀。 |
| **`CultivationComponent`** | `byte majorLevel` (大境界，1炼气-10渡劫)<br>`byte minorLevel` (小境界，1-9层)<br>`long currentExp` (当前修为)<br>`long maxExp` (突破所需修为)<br>`int bottleneckId` (当前卡住的瓶颈) | 境界与修为数据。底层只处理 `currentExp` 的累加，达到 `maxExp` 后交由突破系统进行概率检定。 |
| **`TransformComponent`** | `short gridX`<br>`short gridY`<br>`byte actionState` (0闭关, 1历练, 2重伤) | 坐标组件。决定了该生灵在二维网格地图中的绝对位置，便于区域统计和遭遇战触发。 |
| **`CombatComponent`** | `int hp`<br>`int maxHp`<br>`int attackPower`<br>`int defensePower` | 战斗属性。由 `CultivationComponent` 映射而来，仅在触发战斗判定时参与计算。 |
| **`FactionComponent`** | `int sectId` (归属宗门ID，0为散修)<br>`byte position` (0杂役-5太上老祖)<br>`int contribution` (宗门贡献点) | 维系宗门金字塔剥削关系的核心数据。 |

*   **系统 (System)**：纯逻辑处理器。例如 `AgingSystem` 每个 Tick 遍历所有拥有 `BaseInfoComponent` 的实体，执行 `age++`；`MovementSystem` 处理坐标的变更。

## 2. 全局 Tick 时间步进推进逻辑与加速实现
舍弃依赖于渲染帧率的 `Update()`，大世界的时间必须是离散且绝对受控的。

*   **时间基础定义**：
    *   定义 `1 Tick = 游戏内 1 个月`。
    *   基础流速（X1）：真实时间 `1.0` 秒执行 1 次 Tick。
*   **多倍速加速（时间流速）逻辑**：
    玩家调整 X10、X100 时，底层并非加速游戏物理时间，而是**在一个真实帧内循环执行多次 Tick 计算**。这保证了哪怕在 X100 速度下，也不会发生“吞数据”或逻辑跳跃。

*   **底层架构流转图（核心代码逻辑思路）**：
    ```csharp
    // 天道总控核心引擎
    float tickTimer = 0f;
    float baseTickInterval = 1.0f; // 1秒1个Tick
    int speedMultiplier = 1;       // 玩家选择的倍速(1, 10, 100)

    void EngineUpdate(float deltaTime) {
        tickTimer += deltaTime * speedMultiplier;
        
        // 关键逻辑：累积时间足够，则消耗时间池，执行物理步进
        while(tickTimer >= baseTickInterval) {
            ExecuteOneGlobalTick(); // 执行一次宏观世界推演
            tickTimer -= baseTickInterval;
        }
    }

    void ExecuteOneGlobalTick() {
        // 严格按照优先级顺序执行 System
        WorldQiSystem.UpdateQiFlow();     // 1. 天地灵气流动与结算
        AgingSystem.UpdateLifespan();     // 2. 寿元结算(老死判定)
        CultivationSystem.GainExp();      // 3. 闭关吸纳灵气增加修为
        FactionSystem.ProcessWarfare();   // 4. 宗门战争推演
        EventTriggerSystem.CheckEvents(); // 5. 遭遇战、奇遇判定
    }
    ```

## 3. “天地灵气”能量守恒模型与末法时代阈值
这是限制高阶战力泛滥、维持修仙界残酷内耗的核心底层机制。整个世界的灵气总量绝对守恒，由天道（玩家）在创世时设定初始值。

*   **能量守恒公式**：
    $$ Total\_Qi = Active\_Qi + \sum_{i=1}^{n} (Entity\_Bound\_Qi_i) + \sum_{j=1}^{m} (Resource\_Bound\_Qi_j) $$
    *   $Total\_Qi$：世界灵气总本源（恒定值，除非玩家使用天道权限注入）。
    *   $Active\_Qi$：游离在天地间、灵脉中的可用灵气（面板直接展示的核心资源）。
    *   $Entity\_Bound\_Qi$：固化在NPC体内的修为底蕴（境界越高，锁死的灵气越多）。
    *   $Resource\_Bound\_Qi$：固化在法宝、灵草、阵法中的灵气。

*   **【末法时代】触发阈值与惩罚核算**：
    当高阶老怪过多，吸干了天地灵气，导致：
    $$ Active\_Qi < 10\% \times Total\_Qi $$
    系统自动触发【末法时代（全图 Debuff）】：
    1.  **绝地天通**：凡人生成真灵根的概率直接乘以 $0.05$（近乎无法诞生新修士）。
    2.  **修为反噬**：所有结丹期以上修士的被动灵气吸收速率变为负数（境界跌落风险）。
    3.  **杀戮法则激活**：系统强制将所有高阶 AI 的【杀人夺宝】权重拉满。修士死亡后，其体内的 $Entity\_Bound\_Qi$ 将有 $80\%$ 重新化为 $Active\_Qi$ 回归天地。**通过惨烈的同门相残、正魔大战来释放固化灵气，让系统重新恢复平衡。**

## 4. 区域网格（Grid）地图数据结构与迁徙演算
虽然没有可视化的3D山川河流，但底层必须将世界划分为一张 $N \times M$ 的网格矩阵，作为修士聚散和宗门立派的依据。

*   **网格单元 (`RegionCell`) 数据结构**：
    *   `short cellX`, `short cellY` (坐标)
    *   `long localActiveQi` (当前区块的游离灵气，修士在此打坐吸的就是这个)
    *   `int sectOwnerId` (占领此地的宗门ID，0为无主之地)
    *   `byte dangerLevel` (凶险度：决定妖兽潮爆发概率与秘境品阶)
*   **灵脉枯竭与宏观迁徙判定**：
    当一个区块的宗门无节制扩招，导致 `localActiveQi` 被抽干（降至区块容量的 20% 以下）时：
    1.  **散修逻辑**：每一 Tick，散修会检测相邻四个网格的 `localActiveQi`，若发现相邻网格灵气高于当前网格的 1.5倍，立即执行坐标变更（迁徙）。
    2.  **宗门逻辑**：宗门 AI 判定灵脉枯竭，触发【寻找新灵脉】状态。若发现高灵气网格已有其他宗门占领，且对方综合战力评估低于己方 $120\%$，则自动宣战，触发“夺脉灭门战”。

---

```

---

# 模块二：生灵属性基底与境界成长演化系统（详细设计案）

## 1. 修士NPC底层数据字典设计

为了确保十万级生灵同屏运算的效率，生灵数据必须极度精简，采用位掩码（Bitmask）与极简数据类型存储。

| 字段名称 (Variable) | 数据类型 | 取值范围与策划定义 | 核心作用说明 |
| :--- | :--- | :--- | :--- |
| `npc_id` | `uint` | $1$ ~ $4,294,967,295$ | 全局唯一标识符。 |
| `race_type` | `byte` | $0$:人, $1$:妖, $2$:魔, $3$:灵... | 族群枚举，决定先天属性成长倍率。 |
| `linggen_quality` | `float` | $0.1$ (伪) ~ $10.0$ (天灵根) | 灵根系数。直接乘以环境灵气，得出每Tick基础吸灵量。 |
| `luck_value` | `sbyte` | $-100$ (天谴) ~ $100$ (天命) | 气运值。触发奇遇、逢凶化吉概率的修正参数。 |
| `personality_tags` | `ushort` | 位掩码 (如 `1<<0`苟道, `1<<1`贪婪) | 效用AI评估行为权重时的性格补正。 |
| `combat_stats` | `struct` | `int hp`, `int max_hp`, `int atk` | 基础战斗三围，由大境界基数乘以功法补正得出。 |
| `current_lifespan`| `short` | $0$ ~ $32,767$ (单位: 年) | 结合境界决定的寿元上限，到达上限直接触发坐化/夺舍。 |

## 2. 境界成长演化：经验曲线与突破核算公式

修仙界的战力与消耗呈绝对的指数级爆炸，坚决杜绝线性成长。

### A. 升级修为需求曲线 (Experience Curve)
境界提升所需的修为储备遵循严格的指数公式。
设定基础参数：$BaseExp = 1000$（炼气一层所需修为），大境界系数 $\alpha = 10.0$，小境界系数 $\beta = 1.5$。
某一境界所需最大修为公式为：
$$Exp_{max} = BaseExp \times \alpha^{MajorLevel - 1} \times \beta^{MinorLevel - 1}$$
*(注：开发时此公式计算结果过大，应提前算好存入静态配置表 `Array[10][9]` 中，避免运行时频繁进行指数浮点运算耗费性能。)*

### B. 瓶颈突破成功率判定 (Breakthrough Probability)
到达 $Exp_{max}$ 后，进入“闭关”状态，跨大境界需进行成功率判定。
$$P_{success} = (P_{base} \times Linggen_{mod}) + Pill_{bonus} - (\gamma \times Bottleneck_{penalty})$$
*   $P_{base}$：基础成功率（如筑基为 $15\%$，结丹为 $5\%$）。
*   $Linggen_{mod}$：灵根补正（天灵根此项极高，伪灵根极低）。
*   $Pill_{bonus}$：丹药加成（例如筑基丹强行 $+20\%$）。
*   $Bottleneck_{penalty}$：连续突破失败会增加心魔惩罚（$\gamma$ 递增），必须靠特定静心丹药消除。

### C. 天劫伤害核算与防御检定 (Tribulation Damage)
元婴期及以上境界突破时强制触发天雷劫。
天雷总伤害公式：
$$D_{tribulation} = BaseDamage \times (1 + \delta \times HeavenlyDao_{modifier}) \times Race_{penalty}$$
*   $HeavenlyDao_{modifier}$：天道（玩家）在面板拉动的雷劫强度倍率。
*   $Race_{penalty}$：妖族与魔族天生受雷劫克制，此系数为 $1.5 \sim 2.0$。
*   **生存检定**：判定 `(NPC_HP + Defense_From_Items + Array_Defense) > D_tribulation`。通过则晋升并重塑肉身，失败则灰飞烟灭（或触发气运保底，肉身损毁元婴逃遁）。

## 3. 纯本地双轨推演流转逻辑 (Dual-Track AI Flow)

由于纯本地运行，不能一视同仁，必须对底层散修与高阶大能采用完全不同的算力分配。

### 轨道一：底层散修（宏观模糊逻辑推演）
针对炼气、筑基期的数万NPC，不进行单体路径寻路，而是按“Tick时间步 + 区域网格”批量结算。
1.  **收集变量**：当前区域的 `Active_Qi`，区域人口基数 `N`。
2.  **存活与突破结算**：
    *   生成随机数检定，按概率直接判定死亡比例（代表外出历练陨落、争夺资源死亡）。
    *   按区域灵气浓度推算平均修炼效率，将符合条件的底层人口批量晋升一阶。
3.  **结果输出**：UI数据流变动，不产生具体动画。

### 轨道二：高阶大能（效用AI深度推演）
针对结丹及以上的高价值NPC，每Tick遍历其效用评分（Utility Scoring），执行最高分行为。
*   **续命动机（最高优）**：
    $$Score_{survive} = \left( \frac{MaxLifespan}{MaxLifespan - CurrentAge} \right) \times 100$$
    *(当寿元所剩无几时，分母趋近于0，该项得分无限放大，强制中断一切其他行为，外出抢夺延寿灵果或寻找夺舍目标。)*
*   **杀人夺宝动机**：
    $$Score_{rob} = \left( \frac{TargetItemValue}{SelfNetWorth} \right) \times \left( \frac{SelfCombatPower}{TargetCombatPower} \right) \times GreedTag$$
    *(目标身怀重宝且战力远低于自己，且自身带有“贪婪”标签时，必定触发截杀。)*

## 4. 各族群/体质先天属性修正倍率表

底层AI的差异化演化，全靠这套极简的浮点数乘区。种族决定了下限，灵根决定了上限。

| 族群分类 | 修为获取倍率 (`Exp_mod`) | 肉身气血倍率 (`HP_mod`) | 寿元上限倍率 (`Life_mod`) | 专属隐藏逻辑 (Hardcode) |
| :--- | :--- | :--- | :--- | :--- |
| **人族** | $1.0$ (基准) | $1.0$ (基准) | $1.0$ (基准) | 突破阵法推演进度条速度 $+50\%$。极易成立宗门。 |
| **妖族** | $0.6$ (极慢) | $3.5$ (强横) | $5.0$ (极长) | 无法推演复杂功法；同阶斗法胜率基础 $+20\%$；雷劫伤害承受 $+50\%$。 |
| **魔族** | $1.5$ (极快) | $1.2$ | $1.0$ | 击杀同阶/低阶修士后，可直接掠夺其 $20\%$ 固化修为；极易走火入魔。 |
| **灵族** | $2.5$ (天骄) | $0.6$ (孱弱) | $3.0$ | 出生自带极高阵道天赋；被高阶人族遇上，必定触发高权重的“抓捕炼丹”动机。 |
| **蛮族** | $0.2$ (极度迟缓)| $5.0$ (金身) | $1.5$ | 彻底免疫心魔劫；无法使用任何神识类法宝；物理护甲极高。 |




----


本模块我们将把这种社会形态彻底数据化，用图论（Graph）和状态机（State Machine）来构建修仙界的阵营拓扑网络。

---

# 模块三：宗门阵营与社会关系拓扑推演（详细设计案）

## 1. 宗门核心底蕴与评级数据模型 (Sect Foundation Data Structure)

宗门的强弱不单纯看人数，而是看“护城河”（灵脉与大阵）和“核威慑”（高阶修士）。在底层数据库中，宗门以独立结构体形式存在。

| 字段名称 (Sect Variable) | 数据类型 | 策划定义与作用机理 |
| :--- | :--- | :--- |
| `sect_id` | `int` | 宗门唯一标识，与网格地图上的 `sectOwnerId` 绑定。 |
| `sect_tier` | `byte` | 评级（1不入流, 2三流...9顶尖）。决定其招收弟子的基础名额与灵脉占据上限。 |
| `spirit_vein_grade` | `byte` | 灵脉品阶（1-5阶）。直接决定该宗门领地的 $Local\_Active\_Qi$ 恢复速度。 |
| `array_hp` | `long` | 护宗大阵耐久。宗门战时的绝对护盾，被打破则宗门宣告覆灭。 |
| `treasury_stones` | `long` | 宗门公款（下品灵石）。用于日常大阵消耗、发下层月俸。归零则宗门分崩离析。 |
| `highest_cultivator_id` | `uint` | 最高战力（老祖）的实体ID。此ID一旦判定死亡，触发“灭门危机”全图广播。 |

*   **底蕴评估公式 (Heritage Score)**：
    相邻宗门AI在决定是否开战时，会相互计算底蕴分：
    $$Score_{sect}=VeinGrade\times 10000+Treasury\times 0.1+\sum (ElderCombatPower)$$

## 2. 阶级剥削逻辑与周期性事件时间轴 (Cyclic Harvest Mechanism)

宗门必须通过定期割底层弟子的“韭菜”来维持高层的消耗。底层采用 Tick 累加器来触发周期性全图事件。

*   **升仙大会（凡人收割期）**
    *   **触发周期**：每 120 Ticks（即游戏内 10 年）触发一次。
    *   **底层逻辑**：系统遍历该宗门管辖网格内所有年龄在 10-15 岁、且 `linggen_quality > 0` 的凡人实体。将其 `FactionComponent` 的 `sectId` 更改为该宗门，并标记为“外门弟子”。
*   **血色禁地试炼（筑基丹主药收割）**
    *   **触发周期**：每 600 Ticks（游戏内 50 年）。
    *   **强制参与**：系统强制将宗门内 80% 停留在炼气大圆满（卡瓶颈）的外门弟子，投递至特定的“高危高回报”秘境网格。
    *   **绞肉机公式**：试炼采用残绘的宏观死亡率计算。设定固定基础死亡率为 $70\%$。
    *   **产出结算**：存活下来的弟子（30%）带回主药，系统计算本轮可炼制的筑基丹总数：
        $$Pills=\lfloor(Survivors\times Yield_{avg})/Herbs_{required}\rfloor$$
    *   **残酷分配**：炼出的筑基丹，优先分配给有“核心弟子”标签（天灵根/老祖后代）的实体，剩下的才由存活弟子掷骰子（比拼 `luck_value`）抢夺。

## 3. 宗门外交状态机与宏观战争演算 (Diplomacy & War Engine)

宗门与宗门之间不存在真正的友谊，只有基于利益的状态流转（FSM）。

*   **宗门状态枚举 (Sect States)**：
    `0: 蛰伏`（和平发育），`1: 摩擦`（边境抢矿），`2: 结盟`（共同抵御强敌），`3: 灭门战`（不死不休）。
*   **状态转化触发钩子 (Event Hooks)**：
    *   *灵脉枯竭钩子*：当宗门 A 内部元婴修士增多，自身灵脉不足以支撑时，强制向相邻灵脉品阶最高的宗门 B 转化状态为 `1: 摩擦`。
    *   *趁火打劫钩子*：系统每 Tick 监听所有宗门的 `highest_cultivator_id`。如果宗门 A 的元婴老祖坐化（陨落），其周边所有宗门（评估自身老祖存活）将瞬间把对 A 的状态切为 `3: 灭门战`。
*   **宗门战争宏观演算（Lanchester's Law 变体）**：
    不进行几万人的 1v1 演算，直接进行战力对抗折损。
    *   攻方总战力：$Power_{atk}=\sum (ElderPower)+\log_{10}(Disciples)\times AvgPower$
    *   守方总防御：$Defense_{def}=ArrayHP+Power_{def}$
    *   如果 $Power_{atk} > Defense_{def}$，则判定攻城成功。
    *   **灭亡结算**：战败宗门被注销。其 `treasury_stones` 被攻方掠夺；剩余未死亡弟子，其 `sectId` 强制清零，沦为在地图上四处游荡的“散修”，大幅增加区域网格的混乱度。

## 4. 人际图谱与神识寻仇追踪算法 (Karma & Vengeance Graph)

为了节省内存，底层**只记录结丹期及以上高阶修士**的人际关系图谱。底层凡人和炮灰不配拥有复杂的社交记录。

*   **有向图关系边 (Graph Edge Structure)**：
    存储在独立的结构体列表中，包含 `Source_ID`, `Target_ID`, `Relation_Type` (1:道侣, 2:师徒, 3:血仇), `Intensity` (羁绊/仇恨值)。
*   **血仇生成触发器 (Feud Generation)**：
    当实体 A（如散修韩某）击杀了实体 B，且系统检测到 B 身上挂载着高阶老祖 C 的师徒边（`Relation_Type == 2`）。系统立刻生成一条 `C -> A` 的边，`Relation_Type = 3`，仇恨值拉满。
*   **神识追踪寻仇算法 (Tracking Algorithm)**：
    *   只要血仇边存在，老祖 C 在每 Tick 都会执行一次针对 A 的“神识索敌”检定。
    *   **索敌成功率公式**：
        $$P_{track}=(Level_c-Level_a)\times 0.2-Concealment$$
        *   $Level_c$ 和 $Level_a$ 为大境界等级。境界压制越惨，越容易被算到坐标。
        *   $Concealment$ 为被追踪者 A 身上的“敛气/欺天”法宝提供的隐蔽系数。
    *   **追踪执行**：一旦随机数落地 $P_{track}$ 成功，系统强行将老祖 C 的坐标（`gridX`, `gridY`）修改为 A 的当前坐标，并在下一 Tick 强制触发高阶斗法核算。如果 A 打不过，只能底牌尽出试图逃逸（修改坐标）。


    -----
    在《天道修仙模拟》中，玩家不再是挥舞长剑的侠客，而是高居云端的“天道系统”本身。在这个纯数据驱动的沙盒中，玩家的每一次“点击面板”，在底层代码层面实际上都是对 ECS 架构中**全局环境变量**或**特定实体组件参数**的强制重写。

本模块我们将详细拆解这套“上帝视角”的玩法逻辑，定义天道能量的剥削体系，以及气运之子的“锁血保底”机制是如何在代码中实现的。

---

# 模块四：天道操作台与法则因果系统（详细设计案）

## 1. 天道本源能量（玩家造物货币）经济模型

天道不是做慈善的，玩家干预世界需要消耗【本源能量】（`Origin_Energy`）。这种能量的本质，是修仙界生灵“逆天而行”所产生的法则红利。核心玩法就是典型的**“养蛊与收割”**。

*   **本源能量产出公式（收割机制）**：
    修仙者平时吸纳天地灵气（占用内存），只有在他们死亡（释放内存）或遭遇雷劫时，天道才能抽成。
    $$Energy\_Gain = \sum (Victim\_Tier\_Base \times Level\_Multiplier \times 0.5) + \sum (Tribulation\_Tax)$$
    *   **自然坐化/被击杀回收**：炼气期炮灰死亡仅提供 $1$ 点本源；但如果引导一场宗门大战，导致数万修士陨落，或一个吸干了半州灵气的化神期老怪被击杀，天道将一次性收割数万点本源能量。
    *   **渡劫抽成（雷劫税）**：高阶修士每次引发雷劫，无论成败，其对抗雷劫爆发出的能量都会被天道吸收 $10\%$。

*   **天道干预消耗定价表 (Action Cost Config)**：

| 干预指令类型 | 具体效果描述 | 本源能量消耗评估 | 策略意义 |
| :--- | :--- | :--- | :--- |
| **【微观·拨弄命运】** | 为指定炼气期实体强行修改 `linggen_quality` 为天灵根。 | 5,000 (昂贵) | 投入重金定向培养“气运之子”。 |
| **【宏观·局部法则】** | 锁定某区域网格的 `Danger_Level`，强行爆发【兽潮】。 | 10,000 | 清理区域内过剩的低阶散修，回收灵气。 |
| **【因果·降下重宝】** | 在某坐标生成无主【古宝】，触发全图高阶AI抢夺。 | 20,000 | 抛出诱饵，引诱高阶老怪互相残杀，收割高端本源。 |
| **【天谴·雷罚抹杀】** | 无视战力判定，直接调用抹杀API清除指定实体。 | $Target\_HP \times 10$ (极高) | 强行抹杀化神老怪成本极高，容易导致天道能量亏空。 |

## 2. 天道干预控制台与底层修改接口 (God-Mode APIs)

UI面板上的每一个滑块与按钮，都对应着底层 `WorldManager` 的直接 API 调用。

*   **灵气浓度调控 API (Regional Qi Manipulation)**：
    ```csharp
    // 玩家在UI上圈定一个范围，拉高灵气浓度
    void API_AlterRegionalQi(int gridX, int gridY, int radius, float multiplier) {
        foreach(var cell in Grid.GetCellsInRange(gridX, gridY, radius)) {
            // 强行注入游离灵气，必然导致周边门派发觉并举宗迁徙来争夺
            cell.Local_Qi *= multiplier; 
            WorldEventLogger.Log("天地异象：局部灵气喷发");
        }
    }
    ```
*   **种族法则锁 API (Race Bottleneck Lock)**：
    如果玩家发现妖族繁衍过快，可直接上锁。
    ```csharp
    // 玩家在法则面板勾选：禁止妖族突破元婴
    void API_LockRaceBottleneck(byte raceType, byte maxAllowedTier) {
        GlobalRules.SetRule(RuleType.RaceLimit, raceType, maxAllowedTier);
        // 底层突破系统中，所有达到门槛的该族实体，P_success 强制赋0，必定突破失败受内伤。
    }
    ```

## 3. 天命气运系统（“位面之子”底层保底机制）

《凡人修仙传》等修仙小说中，主角最大的特征就是“坠崖不死”、“残魂附体”。在游戏中，这是通过一种**死亡拦截钩子（Death Override Hook）**来实现的。

*   **数据结构绑定**：被玩家打上“位面之子”标签的 NPC，其 `BaseInfoComponent` 中的 `luck_value` 会被强行锁定为 $999$（普通NPC上限为 $100$）。
*   **致命伤拦截逻辑 (Fatal Blow Intercept)**：
    当战斗系统结算得出 `Damage > Entity.HP`，即将销毁该实体时，底层强制插入检定：
    ```csharp
    if (Entity.hp <= 0) {
        if (Entity.luck_value == 999) {
            TriggerMiracleEvent(Entity); // 触发锁血奇遇机制
            return; // 拦截死亡
        } else {
            ExecuteDeath(Entity); // 普通NPC直接销毁，进入资源回收
        }
    }
    ```
*   **奇遇事件池 (Miracle Event Pool)**：
    拦截死亡后，系统从保底事件池中按权重随机抽取结果补救：
    1.  **【遁入虚空】**：强行将该实体的坐标 `(gridX, gridY)` 修改为全图随机的未知秘境坐标，`HP` 锁为 1，并附加“重伤闭关”状态 10 个 Tick。
    2.  **【大能残魂苏醒】**：战斗力临时乘以 $10.0$ 持续 1 个 Tick（模拟戒指老爷爷附体反杀），反杀后该实体经脉尽断，修为倒退一个小境界。
    3.  **【天地法则反噬】**：击杀方（如果也是实体）突然遭到莫名天劫轰击，直接扣除 $50\%$ 最大生命值，被迫中断追杀。

## 4. 天灾祥瑞事件池算力模型 (Global Event System)

天灾与祥瑞无需渲染华丽的粒子特效，它们在底层本质上是对特定范围内所有实体参数的**批量乘区修正 (Multiplier Override)**。

*   **祥瑞：【天降甘霖 / 灵气复苏】**
    *   **触发参数**：全图网格 `Local_Qi` 瞬间补满。
    *   **实体级修正**：所有处于“闭关”状态的实体，在接下来的 5 个 Tick 内，`Exp_Gain_Multiplier`（经验获取倍率）$+200\%$。
    *   **生态结果**：短时间内大批修士集体突破，底层凡人灵根觉醒率暴涨，进入“修仙盛世”。

*   **天灾：【天地大劫 / 魔气灌体】**
    *   **触发参数**：随机选中 3 个顶级宗门的坐标，将其 `Danger_Level` 设为极高。
    *   **灾祸公式**：
        被卷入天灾网格内的修士，每 Tick 必须进行心性检定。
        $$P_{corrupt} = (100 - Entity.Willpower) \times \epsilon$$
        检定失败的修士，其 `FactionComponent` 强制变更为【魔化阵营】（六亲不认状态），开始疯狂攻击原同门师兄弟。
    *   **生态结果**：顶级宗门内部爆发恐怖内乱，护宗大阵从内部被攻破，宗门底蕴毁于一旦，世界阶级彻底洗牌。

---

模块四确立了玩家作为“执棋者”的操控手段，同时也用冰冷的代码解释了修仙小说中“气运”、“奇遇”的本质。


---

---
---

```
在《天道修仙模拟》的底层逻辑中，资源与功法绝不是凭空“刷”出来的。修仙界是一套极其残酷的闭环经济体：功法靠先人呕心沥血推演，宝物靠剥削底层开采，高阶机缘则是踏着无数尸骨继承而来的。

本模块将把修仙界的“生产力”与“奇物掉落”彻底数据化，构建一个极其真实的、会自我迭代甚至发生技术倒退的修仙文明推演系统。

---

# 模块五：修仙百艺、物资产出与奇物唯一性系统（详细设计案）

## 1. 功法与百艺的推演、传承与断代算法 (Tech-Tree & Inheritance)

游戏中不设全局固定的科技树。所有的丹方（如筑基丹）、阵法（如颠倒五行阵）、功法（如青元剑诀），均是以“图纸（Recipe）”形式在实体间流传。

*   **数据结构定义**：
    设立独立的 `KnowledgeRecord` 结构体，记录每一项知识的全局状态。
    *   `KnowledgeID`：知识唯一标识。
    *   `HolderCount`：当前全图掌握该知识的活体 NPC 数量。
    *   `Difficulty`：推演难度基数。

*   **自主推演进度演算 (Deduction Algorithm)**：
    高悟性（人族优势）、高境界的修士在闭关时，若无现成功法，会尝试自主推演。
    每 Tick 推演进度累加公式：
    $$Progress_{current} = Progress_{last} + \left( \frac{NPC\_Intelligence \times Realm\_Multiplier}{Difficulty} \right) \times \epsilon$$
    *$\epsilon$ 为天道设定的灵感随机浮动值（0.8 ~ 1.5）。*

*   **传承玉简与断代遗失判定 (Loss of Inheritance)**：
    这是维持修仙界底蕴平衡的核心逻辑。
    当一个掌握了某稀有功法的 NPC（如全服唯一会炼制降尘丹的宗门长老）被击杀：
    1.  **判定玉简掉落**：只有该 NPC 死前处于“非突发战死（如寿终正寝）”状态，或触发了 $10\%$ 的绝地反击小概率事件，才会将功法刻印为“玉简”掉落物。
    2.  **断代检定**：如果该 NPC 是全图最后一个掌握此知识的人（`HolderCount == 0`），且未掉落玉简，系统直接触发**【功法失传】**。该知识的 `Progress` 强制清零。
    3.  **世界倒退**：后世生灵若想再次获得降尘丹，必须重新经过漫长岁月，靠底层的推演进度条从零开始试错。这构成了极度真实的“上古功法碾压当世”的修仙背景。

## 2. 大世界坊市物价动态平衡波动逻辑 (Dynamic Market Equilibrium)

不设立死板的固定商店，所有坊市物价由 AI 修士的【供需关系】实时演算生成。

*   **局部经济圈 (Regional Economy)**：
    网格地图（Grid）中，凡是被评级为“二流以上宗门”驻地的区块，自动生成 `MarketComponent`（坊市组件）。
*   **物价波动核算公式**：
    底层通过统计该坊市辐射范围（Radius=3 网格）内的所有 NPC 状态，得出“需求值”与“供给值”。
    某物资（如筑基丹）的实时收购价计算公式：
    $$Price_{current} = BasePrice \times \left( 1 + \frac{\sum Demand_{local} - \sum Supply_{local}}{\sum Supply_{local} + 1} \right) \times Volatility$$
    *   $\sum Demand_{local}$：该区域内处于“炼气九层大圆满（卡瓶颈）”状态的 NPC 数量总和。
    *   $\sum Supply_{local}$：该区域内被炼丹师推演产出并挂售的筑基丹存量。
    *   **生态反应**：如果天道（玩家）强行扣留某区域的筑基丹主药，导致 Supply 趋近于 0，筑基丹价格将呈现指数级暴涨。底层 AI 发现无力购买，其“杀人越货”和“劫掠坊市”的权重分数将瞬间超过安全阈值，直接引发区域级暴乱。

## 3. 唯一古宝生成与持宝人坐标暴露判定 (Unique Artifacts Exposure)

像虚天鼎、掌天瓶这类太古奇宝，系统在创世时仅生成唯一 ID（`Is_Unique = true`），绝不量产。

*   **奇宝认主与封印解封**：
    古宝现世通常带有天象封印。获取古宝的 NPC，需消耗大量真元和 Tick 时间进行血炼。
*   **神物自晦与坐标暴露半径 (Aura Exposure Radius)**：
    怀璧其罪是修仙界第一法则。使用威力越大的古宝，越容易被大能察觉。
    每次持宝人动用古宝（触发战斗或突破加成），系统会计算一个“贪婪神识暴露半径 $R$”：
    $$R_{expose} = \frac{Artifact\_Power\_Rating}{Concealment\_Stat \times (Owner\_Realm\_Level + 1)}$$
    *   $Artifact\_Power\_Rating$：古宝评级分数（如玄天斩灵剑为 9999）。
    *   $Concealment\_Stat$：持宝人所学隐匿功法的等级系数。
    *   **触发截杀**：系统向半径 $R_{expose}$ 内的所有网格广播 `Artifact_Aura_Event`。如果该范围内存在高阶 NPC（境界 > 持宝人），且该 NPC 带有【贪婪/劫掠】标签，将立即把自己的 `ActionState` 切为寻仇追踪，朝暴露坐标直线移动发起夺宝战。

## 4. 大能遗迹洞府生成器 (Fallen Expert Ruin Generator)

为了让高阶修士死后的能量与财富能够流转，而不是凭空消失，系统采用“残骸转化为秘境”的生成逻辑。

*   **触发条件**：
    任何元婴期（Tier 5）及以上境界的 NPC 判定死亡时（除形神俱灭的抹杀天罚外），其死亡坐标点网格强制生成一个 `RuinEntity`（遗迹实体）。
*   **遗迹参数继承算法**：
    遗迹不是随机生成的，而是严格继承原主人生前的数据：
    1.  **防御级别（阵法）**：继承原主人自身的 `Array_Skill_Level`。如果是个阵法白痴，遗迹很快会被底层散修摸透；如果是阵法宗师，该遗迹将成为绞肉机。
    2.  **宝物池（Loot Pool）**：复制原主人死亡瞬间的 `Inventory`。但扣除 $30\%$ 在死前斗法中损毁的物资。
    3.  **残魂陷阱率**：取决于原主人死前的怨气值（被越阶强杀或走火入魔）。
*   **探索规则与消耗判定（探索扣除机制）**：
    后世 AI 发现遗迹并进入探索，每次 Tick 需要过一次判定：
    ```csharp
    // C# 遗迹探索判定核心逻辑
    void ProcessRuinExploration(Entity explorer, RuinEntity ruin) {
        if (explorer.Combat_Power < ruin.Defense_Level * 100) {
            // 战力不足，触发阵法反噬
            float deathProb = 0.5f + (ruin.Defense_Level / explorer.Realm_Level);
            if (Random.value < deathProb) {
                ExecuteDeath(explorer); // 探索者陨落，其储物袋反而成为遗迹的新养料
            } else {
                explorer.ApplyDebuff(Status.HeavyWound); // 重伤逃出
            }
        } else {
            // 破阵成功，搜刮宝物
            Item loot = ruin.LootPool.PopRandomItem();
            explorer.Inventory.Add(loot);
            ruin.Remaining_Capacity--;
            
            // 如果宝物被抽干，摧毁遗迹实体，该网格恢复为普通地形
            if (ruin.Remaining_Capacity <= 0) {
                DestroyEntity(ruin.ID);
            }
        }
    }
    ```

---

这套物资与奇物系统，确保了游戏后期的经济与功法系统不会发生通货膨胀，甚至会因为修仙界大战导致功法断代，需要天道玩家主动“散播上古玉简”来进行版本更新。

```
---
---
---



在《凡人修仙传》的残酷修仙界中，战斗从来不是花里胡哨的见招拆招，而是底蕴、境界与法宝的绝对数值倾轧。低阶修士试图依靠人海战术围杀死高阶修士，在真正的写实修仙体系中是痴人说梦。

本模块我们将构建一套极简、高效、且存在“绝对屏障”的底层战斗推演引擎，确保能在极低的 CPU 占用下，瞬间结算大世界中每 Tick 发生的数万次斗法。

---

# 模块六：底层战斗核算与绝对边界控制（详细设计案）

## 1. 境界压制与绝对防破防数学模型 (Absolute Realm Suppression)

为了彻底杜绝传统手游中“100个1级小兵砍死100级大BOSS”的数值崩坏，本游戏引入**【真元护体破防阈值】**机制。

*   **大境界压制系数 (`Realm_Gap_Modifier`)**：
    每跨越一个大境界，生命值、攻击力与防御力的基数呈 $10$ 倍级跳跃。但这还不够，必须在伤害结算底层加入强制的护甲穿透判定。
*   **最终伤害核算公式**：
    假设攻击方 A，防守方 B。
    $$ BaseDamage = Atk_a \times SkillModifier_a - Def_b $$
    
    **【绝对屏障判定】**：
    在造成伤害前，判定 A 的穿透力是否高于 B 的护体真元绝对阈值。
    $$ Penetration = Atk_a \times (1 + WeaponArmorPenetration) $$
    $$ Threshold_b = Def_b \times (1 + (RealmLevel_b - RealmLevel_a) \times 5.0) $$
    
    *   **破防失败 (反震重伤)**：如果 $Penetration < Threshold_b \times 0.2$。A 的攻击不仅强制为 $0$ 伤害，且 A 自己会受到 $Def_b \times 0.5$ 的【真元反震伤害】。这就是原著中炼气期拿法器砍结丹老怪，法器直接崩碎且自己吐血而亡的底层逻辑。
    *   **勉强破防 (刮痧)**：如果 $Threshold_b \times 0.2 \le Penetration \le Threshold_b$。造成基础伤害的 $10\%$。
    *   **正常伤害**：如果 $Penetration > Threshold_b$。依照正常伤害公式扣血。

## 2. 极简斗法推演引擎与胜率公式 (Fast Combat Resolution Engine)

因为是 ECS 挂机沙盒，不需要计算两把飞剑在空中的碰撞体积。两名修士在同一网格相遇并触发【杀人夺宝 / 寻仇】动机时，系统直接计算各自的【综合斗法战力评级 (Combat Power, CP)】并掷骰子。

*   **综合战力评分核算 (CP Calculation)**：
    $$ CP = (BaseHP \times 0.1 + BaseAtk) \times ElementModifier \times ArtifactMultiplier + ConsumableBonus $$
    *   $ElementModifier$：五行生克补正。若 A 的主修功法（如火）克制 B（如金），A 的系数为 $1.2$，B 的系数为 $0.8$。
    *   $ArtifactMultiplier$：古宝与法宝补正。如手持“平民法器”系数为 $1.0$，手持“通天灵宝残片”系数可高达 $3.0$。
    *   $ConsumableBonus$：符箓与一次性阵盘加成。

*   **胜率判定公式 (Lanchester 变体)**：
    为保留一丝“机缘与意外”，采用非线性的概率推演，而不是 CP 高的一方必胜。
    $$ P_{win\_A} = \frac{CP_a^{1.5}}{CP_a^{1.5} + CP_b^{1.5}} $$
    *当双方 CP 接近时，胜负难以预料（拼底牌与气运）；当 $CP_a$ 是 $CP_b$ 的 3 倍时（如跨大境界），$P_{win\_A}$ 将趋近于 $95\% \sim 99\%$，形成碾压。*

## 3. 战斗结算逻辑代码流转 (Combat Execution C# Snippet)

底层 System 扫到两个状态为【交战】的 Entity 时，调用的极简核心代码逻辑：

```csharp
// 斗法核算核心引擎 (脱离渲染，纯数据推演)
void ResolveCombat(Entity attacker, Entity defender) {
    // 1. 计算双方胜率概率模型
    float cpA = CalculateCombatPower(attacker, defender); 
    float cpB = CalculateCombatPower(defender, attacker);
    
    float winProbA = Mathf.Pow(cpA, 1.5f) / (Mathf.Pow(cpA, 1.5f) + Mathf.Pow(cpB, 1.5f));
    
    // 2. 气运系统暗箱操作 (天道干预保底)
    if (attacker.LuckValue == 999) winProbA = Mathf.Max(winProbA, 0.8f); // 主角光环锁最低胜率
    if (defender.LuckValue == 999) winProbA = Mathf.Min(winProbA, 0.2f); 

    // 3. 掷骰子结算
    float roll = Random.Range(0f, 1f);
    
    if (roll <= winProbA) {
        // A 胜
        ExecuteDeathAndLoot(winner: attacker, loser: defender);
        attacker.HP -= CalculateBattleLoss(cpA, cpB); // 惨胜也会扣血甚至重伤
    } else {
        // B 胜 (反杀)
        ExecuteDeathAndLoot(winner: defender, loser: attacker);
        defender.HP -= CalculateBattleLoss(cpB, cpA);
    }
}
```

## 4. 储物袋掉落与“鲸落”能量反哺机制 (Whale Fall & Loot System)

修士战死绝非仅仅是一个实体从数组中被 Remove，而是维持大世界能量守恒的最关键一环——即所谓的“一鲸落，万物生”。

*   **储物袋爆率结算 (Inventory Looting)**：
    *   战死方（Loser）的虚拟背包进入结算程序。
    *   **斗法损耗**：由于生前激烈反抗，$30\% \sim 50\%$ 的丹药、符箓在战斗中被消耗或损毁，直接从数据库抹除（维持世界物资紧缺）。
    *   **剥夺**：剩余物品打包生成一个临时的 `LootBag` 组件。胜者（Winner）根据自身储物袋剩余空间进行拾取。如果胜者空间满了，储物袋将掉落在该网格（`RegionCell`）内，被标记为【无主遗宝】，极易引发其他路过散修的二次争夺。
*   **天地反哺 (Energy Return - "Whale Fall")**：
    高阶修士体内封存着巨量的 `Bound_Qi`（固化灵气）。一旦死亡：
    $$ Returned\_Qi = Loser.Bound\_Qi \times 0.8 $$
    这 $80\%$ 的灵气直接注入当前所在网格的 $Local\_Active\_Qi$ 中。
    *   **生态表现**：如果两个元婴老怪在某个贫瘠的沙漠网格发生死战，其中一个陨落。几十年后（几个 Tick），这片沙漠会因为老怪陨落反哺的庞大灵气，直接演化出一片绿洲和一条中品灵脉，并引来凡人定居与小宗门建立。这就是最高级的沙盒写实生态演算。


---
----
-
----
-
-
--
本模块是整个游戏能否在安卓设备上流畅运行、且不被轻易破解的核心。采用自底向上的开发模式，我们在搭建表层UI和视觉表现之前，必须先将底层的数据存储、线程解耦以及离线演算逻辑彻底夯实。这不仅能避免后期重构，还能在解包逆向APK时确保核心数据逻辑的安全与独立。

---

# 模块七：安卓工程落地与本地存储架构（详细设计案）

## 1. 纯本地 SQLite 数据库与分块存储架构 (Local DB Schema)

对于一款纯单机、动辄几十万底层实体的大世界沙盒，使用原生的 `PlayerPrefs` 或单文件 JSON 会导致极慢的读写 IO，甚至在杀进程时导致坏档。必须采用 SQLite 结合分块 JSON 序列化的方案。

*   **数据库表结构设计 (Table Schema)**：

    *   `Table: World_Config` (全局配置表)
        *   `key_name` (VARCHAR): 如 `global_active_qi`, `current_tick`, `danger_level`
        *   `key_value` (VARCHAR): 对应数值，关键数据（如天道本源）在此层进行简单的位移加密（XOR），防止被内存修改器直接扫描篡改。
    *   `Table: Sect_Data` (宗门阵营表)
        *   `sect_id` (INT PRIMARY KEY)
        *   `tier`, `array_hp`, `treasury`, `leader_id`
    *   `Table: High_Tier_Entities` (高阶修士独立表：结丹期及以上)
        *   `entity_id` (INT PRIMARY KEY)
        *   `name`, `realm_level`, `sect_id`, `hp`, `bound_qi`
        *   `inventory_blob` (BLOB): 储物袋内容序列化
        *   *策划备注：高阶修士拥有完整社交关系和复杂行为树，必须独立建表，支持高频 UPDATE 操作。*
    *   `Table: Low_Tier_Chunks` (底层众生分块存储：凡人、炼气、筑基)
        *   `chunk_id` (INT PRIMARY KEY): 对应网格地图的 Region ID
        *   `entity_list_blob` (BLOB): 压缩的 JSON 字符串或 Byte 数组。
        *   *策划备注：底层炮灰不需要独立建表，直接按区域打包存入 BLOB。每次读档时，按需将主角当前视角的 Chunk 反序列化到内存数组中，极大地减少了 SQLite 的 B-Tree 索引开销。*

## 2. 离线挂机收益与沧桑演化算法 (Offline Math Expectation)

放置游戏最忌讳“上线时执行一万次 Tick 循环”，这必定会导致安卓端主线程阻塞（ANR）。我们需要采用宏观数学期望公式，在玩家上线的瞬间，直接算出几万年间的演化结果。

*   **离线时间换算**：
    计算离线真实时长 $T_{offline}$（秒）。得出缺失的 Tick 数量 $N = T_{offline} / BaseTickInterval$。
*   **众生生灭演算 (离线人口与灵气推演)**：
    不进行单体检定，直接套用宏观统计学公式。
    设离线前某网格有底层修士 $Pop_{start}$，其区域灵气浓度支持的环境容量为 $K$。经过 $N$ 个 Tick 后的存活人数 $Pop_{end}$ 采用离散逻辑斯谛增长模型的变体来瞬间结算：
    $$ Pop_{end} = \frac{K}{1 + \left(\frac{K - Pop_{start}}{Pop_{start}}\right) \times e^{-r \times N}} $$
    *(其中 $r$ 为该区域的基础繁衍与突破综合速率系数)*
*   **老怪离线闭关结算 (高阶推演)**：
    针对 SQLite 中记录的高阶修士，遍历其闭关时长。
    直接计算离线期间积累的理论修为总额 $Exp_{gain}$，加上由于灵脉衰减导致的折损系数。如果 $Exp_{gain}$ 足以触发天劫，直接在后台通过概率随机数进行一次生与死的判定，记录在“离线简报日志”中。

## 3. Data-Driven UI (数据流UI) 的读写解耦方案

原生的很多安卓单机游戏或粗糙的 Unity 项目，UI 层与逻辑层强耦合，UI 每帧都在 `Update()` 里去读取几十万个变量，导致极度卡顿。我们必须实行彻底的**读写分离**。

*   **底层 ECS 逻辑线程 (Logic Thread)**：
    完全在后台线程跑推演计算。所有的数据修改（Entity HP 减少、宗门灭亡）都在这层闭环。
*   **脏标记与事件总线 (Dirty Flag & Event Bus)**：
    UI 不主动读取底层数据。底层逻辑在发生**宏观重要变动**（如：黄枫谷被灭、大能突破化神、天道本源增加）时，抛出一个带有数据载荷的 Event。
*   **UI 渲染线程 (Main Thread)**：
    安卓主线程只负责订阅 Event 并在九宫格信息流中生成一行 Log（例如：*“【警告】检测到越国境内爆发兽潮”*），或者更新顶部栏的数值 Text。底层炮灰每秒死一千个，UI 上的柱状图只需每秒缓动（Lerp）一次总数，绝不进行逐个实体的数据绑定。

## 4. 本地大模型编排预留接口 (Local LLM Orchestration Hooks)

为了打破死板的 if-else 剧情，我们在安卓包体中封装轻量级的 HTTP 客户端模块，用于与开发者在同局域网内运行的 PC 算力节点（如 LM Studio 的本地推理 API）进行通讯。

*   **通信结构设计 (API Payload)**：
    当世界中发生极其罕见的高阶事件（如：两个元婴老怪因为争夺一件古宝同归于尽，只留下一个玉简），安卓端暂停该事件的底层默认结算，向本地 PC 发送 JSON 序列化请求：
    ```json
    {
      "event_type": "epic_battle_result",
      "entity_A": {"name": "韩老魔", "traits": ["苟道", "心狠手辣"]},
      "entity_B": {"name": "王天古", "traits": ["阵法宗师", "孤傲"]},
      "artifact": "虚天鼎",
      "prompt_directive": "根据以上两位修仙者的性格，推演一段300字的陨落前算计与功法玉简流传的剧情，要求符合凡人修仙传的残酷冷血风格，并返回最终获得玉简的第三方NPC特征。"
    }
    ```
*   **数据回调与沙盒实装**：
    PC 端的大语言模型生成结构化结果后，返回给安卓端。安卓端的解析器提取 JSON 中的新剧情文本存入 UI 的“修仙界大事记”模块，并将模型钦定的“第三方 NPC”强行写入底层 SQLite，赋予其气运值。这种自底向上的架构，使游戏不仅仅是一个数值模拟器，更是一个能够自动生成百万字正统修仙小说的引擎。
```





项目结构
D:\0-zheteng\TiandaoSimulator\app\src\main\java\com\zheteng\tiandao\
├── core/                                   # 核心引擎与全局基础设施 (最早开发)
│   ├── engine/
│   │   ├── TickEngine.kt                   # 全局 Tick 时间步进推进器 (实现多倍速与离散推演)
│   │   └── OfflineCalculator.kt            # 离线挂机收益与沧桑演化数学期望演算器
│   ├── eventbus/
│   │   ├── EventBus.kt                     # 全局事件总线 (彻底解耦底层运算与 UI 渲染)
│   │   └── SystemEvents.kt                 # 核心事件载荷数据类 (如宏观变动、大事记)
│   └── config/
│       ├── WorldConstants.kt               # 世界全局参数 (基准灵气、末法阈值、基础消耗表)
│       └── MathFormulas.kt                 # 统一数学公式库 (经验指数曲线、战斗破防模型)
├── db/                                     # 纯本地持久化层 (Room 架构，底层基石)
│   ├── TiandaoDatabase.kt                  # Room 数据库主类
│   ├── dao/
│   │   ├── SectDao.kt                      # 宗门数据高频更新接口
│   │   ├── HighTierEntityDao.kt            # 高阶修士 (结丹及以上) 独立表接口
│   │   ├── LowTierChunkDao.kt              # 底层炮灰分块打包 (BLOB) 读写接口
│   │   └── WorldConfigDao.kt               # 天道全局配置持久化接口
│   ├── entity/                             # 数据库表结构定义 (对应模块七)
│   │   ├── SectDbEntity.kt
│   │   ├── HighTierDbEntity.kt
│   │   ├── LowTierChunkDbEntity.kt
│   │   └── WorldConfigDbEntity.kt
│   └── converter/
│       └── JsonTypeConverters.kt           # 复杂对象 (如储物袋、组件列表) 的 JSON 序列化转换器
├── ecs/                                    # 纯内存推演架构 (高频核心逻辑)
│   ├── core/
│   │   └── EntityManager.kt                # 实体 ID 分发与组件生命周期管理
│   ├── component/                          # 纯数据结构体 (Struct 级轻量级类)
│   │   ├── BaseInfoComponent.kt            # 生理基底 (寿元、灵根、气运)
│   │   ├── CultivationComponent.kt         # 修为境界 (大境界、小境界、经验值)
│   │   ├── TransformComponent.kt           # 坐标与状态 (网格 XY，行动状态)
│   │   ├── CombatComponent.kt              # 战斗三围 (HP、攻、防、真元)
│   │   ├── FactionComponent.kt             # 宗门从属与剥削层级
│   │   └── InventoryComponent.kt           # 虚拟储物袋 (存储物品、法宝 ID)
│   └── system/                             # 纯逻辑处理器 (按优先级严格排队推演)
│       ├── AgingSystem.kt                  # 寿元结算与坐化判定
│       ├── CultivationSystem.kt            # 灵气吸纳与境界突破判定 (包含天劫雷罚核算)
│       ├── MovementAndGridSystem.kt        # 寻路迁徙与区域灵脉判定
│       ├── FactionAndWarSystem.kt          # 宗门外交状态机与宏观战争演算
│       └── CombatResolutionSystem.kt       # 底层极简斗法引擎与绝对屏障判定
├── world/                                  # 大世界生态与社会拓扑
│   ├── grid/
│   │   ├── GridManager.kt                  # N x M 网格矩阵管理器
│   │   └── RegionCell.kt                   # 网格区块数据 (游离灵气、凶险度、宗门占领)
│   ├── social/
│   │   ├── KarmaGraph.kt                   # 人际关系有向图网络 (道侣、师徒、血仇)
│   │   └── VengeanceSystem.kt              # 神识寻仇追踪与截杀算法
│   └── eco/
│       ├── TechTreeManager.kt              # 功法百艺自主推演与断代遗失判定
│       └── MarketManager.kt                # 坊市物价动态平衡与供需算法
├── heavenlydao/                            # 天道操作台 (玩家干预接口)
│   ├── OriginEnergyManager.kt              # 本源能量收割与消耗核算
│   ├── GodModeApi.kt                       # 天道底层修改接口 (灵气调控、法则锁、降下重宝)
│   └── MiracleEventPool.kt                 # 天灾祥瑞事件池与气运锁血保底拦截器
├── llm/                                    # 本地大模型通信模块
│   └── LocalInferenceClient.kt             # 轻量级 HTTP 客户端 (请求本地 PC 算力生成剧情)
└── ui/                                     # Data-Driven UI 层 (纯被动渲染)
├── MainActivity.kt                     # 主 Activity (绑定底层服务)
├── MainViewModel.kt                    # 持有由 EventBus 投递的聚合 UI 状态
└── panels/
├── GodControlPanel.kt              # 天道干预滑块与指令 UI
├── WorldLogPanel.kt                # 九宫格信息流大事件渲染 (流式文本)
└── SectTopologyView.kt             # 宗门势力与关系网谱视图
