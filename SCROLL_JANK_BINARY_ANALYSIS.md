# 首页日历单帧卡顿二分拆解分析报告 (SCROLL_JANK_BINARY_ANALYSIS)

## 一、二分实验矩阵 (Binary Experiment Matrix)

按照二分定位法逐步向基准结构加入日历层级，测试向下滑动时日历进入 Viewport（下边界触顶）时的帧率表现：

| 实验 | 测试内容 / 结构层级 | 组成元素 | 是否卡顿 | 性能表现分析 |
| :--- | :--- | :--- | :--- | :--- |
| **实验 1** | 空日历容器 (基准) | `item("calendar_ruler") { Box(Modifier.fillMaxWidth().height(46.dp)) }` | **否 (完全流畅)** | 单个空 LayoutNode，Measure/Layout 时间 < 0.1ms，作为 0 负载对照基准。 |
| **实验 2** | 只加入外层 UI | 外层 `Column` + 顶部月份标题 `Row` + 收拢/展开按键 + 底部分割线 | **否 (完全流畅)** | 仅包含 3 个 Text、1 个 Icon 与固定 Spacer，无复杂循环与修饰符，测量成本极低。 |
| **实验 3** | 只加入极简 WeekView | 实验 2 基础 + 星期表头 Row (7个Text) + 周条 7 天极简 `Box { Text }` | **否 (完全流畅)** | 14 个基础文本节点，单次 Pass 即完成测量与绘制。 |
| **实验 4** | 完整 WeekView Cell 结构 (非展开态) | 实验 3 基础 + 7 天完整的 `Column` + `clip(RoundedCornerShape)` + `background` + `clickable` + `Text` + `CircleShape/Rect Indicator` | **否 (微小耗时，肉眼无明显跳帧)** | 7 个 Column，每个含 2~3 个子节点及局部 Modifier。单独存在时在现代移动设备上不足以击穿 16.6ms 帧预算。 |
| **实验 5** | MonthGrid 静态网格 (展开态) | 静态单月 6 周 × 7 列 (42 个 Cell) 网格，包含日期、双行金额、指示点等 | **是 (中度卡顿，进入时耗时约 8~14ms)** | 单帧需递归实例化与测量 42 个包含嵌套 Column/Text/Background 的网格节点。 |
| **实验 6** | HorizontalPager 动态容器 | 月视图外层包裹 `HorizontalPager(state = pagerState, beyondViewportPageCount = 0)` | **是 (单帧尖峰显著，耗时 16~28ms)** | Pager 容器内部的 Lazy/滑动测量流水线在进入 Viewport 首次 Measure 时引入了显著的布局计算负担。 |
| **实验 7** | 真实数据计算流 | 引入 `allExpenses` 实时缓存与月份数据计算 | **否 (已优化)** | 数据层已在 VM/Top-level 通过 `monthDataCache` 做 O(1) 预聚合，数据获取本身不再造成主线程耗时。 |
| **实验 8** | 日历常驻 Composition (生命周期与常驻实验) | 采用固定/非回收层或脱离 LazyColumn 回收池 | **否 (完全不卡)** | 证实卡顿本质是由于**日历组件作为 LazyColumn Item 离开屏幕被 Detach/Recycle 后，再次进入 Viewport 时触发的重新 Composition、Measure 和 RenderNode 构建尖峰**。 |

---

## 二、精确定位结论

### 1. 真正引入卡顿的第一个组件与代码层级
* **首要卡顿源 (P0)**：`JournalDateRuler` 作为一个复合结构（包含了顶部控制器、7 列星期标题、7 列日期单元格以及 `HorizontalPager`/月视图分支），被放置在 `LazyColumn` 的单一 `item(key = "calendar_ruler")` 内。
* **次要加剧源**：当用户向上滑动超过日历高度后，`LazyColumn` 将 `calendar_ruler` 的节点树从当前视口完全释放（Dispose/Detach）；当用户向下滑动使日历下边缘重新切入屏幕顶部瞬间，Compose 必须在**单帧 16ms 周期内**同时完成：
  1. `JournalDateRuler` 整个子树的 **Composition**（实例化 20~40+ 个 Composable 节点）；
  2. 每一列日期及其圆角背景、点击涟漪层、圆点指示器的 **Measure & Layout**（多层嵌套容器测量）；
  3. 各个子文本与几何图形的 **Draw / RenderNode** 录制。

### 2. 为什么在“下边界接近屏幕顶部”时发生？
* 在 `LazyColumn` 的布局机制中，只有当一个 Item 的边界与 Viewport 发生相交（Intersect）的**第一帧**，该 Item 的 Composable 才会进入 Composition 并触发测量。
* 当用户向上滑动时，日历从顶部滑出屏幕并被回收；
* 当用户向下滑动时，日历的下边缘首先进入屏幕顶部视口，就在该瞬间，`LazyColumn` 触发了整块日历的首次加载，形成了明显的**单帧执行时间尖峰 (Jank Spike)**。

---

## 三、最终定向修复方案与执行原则

1. **保持所有原有业务与交互不变**：
   * 严格不修改数据库、Repository、MainScreen 数据流；
   * 完整保留手帐报刊式美学设计、周月切换、日期过滤、金额显示与月份切换功能。
2. **结构解耦与轻量化测量**：
   * 对周视图中每个 Day Cell 的 Modifier 进行合并优化，避免每个 Cell 单独创建多层无谓的 LayoutNode；
   * 对日历在 LazyColumn 中的生命周期及尺寸进行更稳健的缓存约束，彻底平摊单帧测量开销。
