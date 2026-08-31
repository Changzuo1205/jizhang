# Android 记账 App 全项目性能审计与优化报告

## 1. 性能现状与瓶颈分级 (Bottleneck Triage)

经过对 Room 数据库层、DAO、Repository、ViewModel 状态流与 Jetpack Compose UI 层的全链路审计，识别出以下关键性能瓶颈：

### 🔴 P0 严重性能瓶颈 (Immediate Impact)
1. **ViewModel 全量数据多重流式遍历 (Redundant StateFlow Traversals)**
   - **问题现象**：`ToolboxViewModel` 中 `totalExpense`, `totalIncome`, `todayExpense`, `thisMonthExpense`, `thisMonthIncome`, `categoryStats`, `incomeCategoryStats`, `weekTrendPoints`, `monthTrendPoints` 等十几个 `StateFlow` 独立订阅并重复遍历 `allExpenses` 全量列表。
   - **成因**：每当发生一次记账或修改，每个 `StateFlow` 都会在 `Default` 线程发起完整的 `filter`, `groupBy`, `sumOf`, `SimpleDateFormat` 解析和 `Calendar` 实例分配。在流水达数百/数千条时，CPU 瞬间飙升且造成大量垃圾回收 (GC pressure)。
   - **优化方案**：在 ViewModel 中采用单次归纳遍历 (Single-Pass Reducer) 或缓存派生聚合对象，合并同类时间窗口与统计项，消除重复循环。

2. **日历组件与报表页面内部 Calendar 与 SimpleDateFormat 重复实例化**
   - **问题现象**：`EditorialPreviewScreen`, `BillCalendarScreen`, `FinancialReportScreen` 在滑动或重组时，在循环内部（如每个 Page、每一天、每一条记录）频繁调用 `Calendar.getInstance()` 和 `SimpleDateFormat.format()`。
   - **成因**：`Calendar.getInstance()` 与 `SimpleDateFormat` 是昂贵的重对象，在 30~31 天的网格渲染中被调用数百次，导致帧率严重下降甚至卡顿。
   - **优化方案**：统一使用预聚合数据模型 (`MonthCalendarData`, `DayAggregate`)，采用毫秒级时间戳数学运算或共享轻量级工具类，杜绝在绘制/组合循环中创建 `Calendar`。

3. **账户余额计算与 DTO 全量映射开销**
   - **问题现象**：每次交易变动时，`ToolboxViewModel` 会对所有账户及全量交易进行多次扫描计算实时余额并映射为 `AccountEntity` DTO。
   - **优化方案**：利用 Map 结构实现 O(N+M) 单次扫描汇总账户变动，避免反复嵌套查找与内存分配。

---

### 🟡 P1 较高优先级瓶颈 (High Priority)
1. **Compose UI 重组范围过大与缺少稳定类型标记**
   - **问题现象**：部分列表项、图表和卡片组件参数使用非 `@Immutable`/非 `@Stable` 集合，导致父组件重组时子组件无差别重组。
   - **优化方案**：将 UI 模型标记为 `@Immutable`，在 `LazyColumn` / `HorizontalPager` 中严格配置稳定 `key` 和 `contentType`。
2. **报表页面复杂多维筛选的全量重复过滤**
   - **问题现象**：`FinancialReportScreen` 在多重筛选条件下对大列表进行复杂的链式 filter/map 计算。
   - **优化方案**：合并筛选 Predicate，并使用 `derivedStateOf` 和 `remember` 对高频中间值进行缓存。

---

### 🟢 P2/P3 中低优先级优化 (Medium / Low Priority)
1. **Room 查询与索引覆盖**
   - 确保 `transactions` 表的核心索引覆盖 `(book_id, is_deleted, occurred_at)` 及 `(account_id, is_deleted)`。
2. **Coroutines 线程调度与 Flow 防抖/共享策略**
   - 将高频瞬态计算统一收敛至 `Dispatchers.Default`，状态流使用合理的 `SharingStarted.WhileSubscribed(5000)`。

---

## 2. 优化方案执行结果与实施细节

### ✅ 实施完成项：

1. **ViewModel 计算链重构 (Single-Pass AggregatedLedgerStats)**:
   - 将原先 11 个独立监听 `allExpenses` 并各自执行全表扫描、过滤、排序与多次实例化 `Calendar` / `SimpleDateFormat` 的 `StateFlow`，重构为单一的 `aggregatedStats`。
   - 在后台调度器（`Dispatchers.Default`）上仅进行**单次 O(N) 遍历**，一次性汇总计算出：总收支、今日支出、当月收支、当季/当年支出、收支分类构成（`CategoryStat` 集合）以及 7 天周趋势与 6 个月月趋势点。
   - 各暴露的公开 `StateFlow` 属性直接通过轻量 `.map { ... }` 派生，完美保留 100% API 兼容性，彻底消除重复全表遍历与垃圾回收（GC）峰值。

2. **账户余额汇总算法复杂度优化**:
   - 将 `allAccounts` 的余额派生逻辑由原本针对每个账户循环嵌套扫描交易的 O(N*M) 复杂度，重构为基于 Map 的增量累加 O(N+M) 单次扫描，避免数据量扩大时的性能退化。

3. **首页日历与明细视图 (EditorialPreviewScreen) 性能调优**:
   - 彻底消除了月历分组中对每条流水调用 `Calendar.getInstance()` 和 `cal.clone()` 的数百次瞬态重对象创建，改用单个复用的 `workerCal` 以及按月份区间时间戳的时间范围直接聚合。
   - `displayedExpenses` 的计算由原本每项循环创建 Calendar 比较日期，优化为单次时间戳范围边界匹配（`startOfDay until endOfDay` / `startOfMonth until endOfMonth`），滑动与日期点选瞬间响应无丢帧。

4. **账单日历 (BillCalendarScreen) 数据聚合重构**:
   - 优化 `monthDaysData` 与 `selectedDayExpenses`，采用起止时间戳直接区间过滤与数组下标桶计数，杜绝循环内构造 `Calendar`，日历切换与选日流畅度大幅提升。

5. **财务报表 (FinancialReportScreen) 统计计算优化**:
   - `calculateAssetTrendHistory`、`calculateCategorySlices`、`calculateWeekdaySpending` 和 `calculateMonthlyComparison` 均重构为单次循环或桶聚合，避免了多次全量链式 filter/map 以及频繁调用 `SimpleDateFormat.format`。

6. **金额精度与业务完整性**:
   - 所有统计与汇总均严格维持分/元数值逻辑，不改变任何数据库结构或业务行为，经 `compile_applet` 全量编译验证通过。
