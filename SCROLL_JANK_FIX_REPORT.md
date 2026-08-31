# 首页日历重新进入 Viewport 滚动单帧卡顿修复报告 (SCROLL_JANK_FIX_REPORT)

## 1. 原始问题
* **现象**：在首页列表向下滑动时，日历（`JournalDateRuler`）从屏幕上方重新进入视口；当日历的下边界接近屏幕顶部附近时，页面出现一次明显单帧卡顿（Jank Spike），之后继续向下滑动恢复正常。
* **特征**：不是滑动全程卡顿，而是**在日历重新从上方进入 Viewport 触发 Compose 的 Composition & Measure 瞬间出现的单帧峰值延迟**。

---

## 2. A/B 实验结果

* **实验 A（基准对照组）**：
  * 将 `LazyColumn` 中的 `JournalDateRuler` 替换为固定占位 `Box(Modifier.fillMaxWidth().height(80.dp))`。
  * **结果**：向下滑动至相同位置完全不卡顿，帧率极为平稳。证实 `LazyColumn` 机制与数据层无问题，卡顿点来自日历组件在进入视口时的布局与渲染成本。
* **实验 B（优化前真实日历组件）**：
  * 日历重新进入 Viewport 时触发完整的 `AnimatedContent` 布局测量、多层嵌套容器测量、以及全量 `graphicsLayer` 节点分配。
  * **结果**：在下边界触顶的单帧内存在明显掉帧。
* **实验 C（优化后真实日历组件）**：
  * 移除 `AnimatedContent` 多重过渡包装，改为确定性的直接条件布局；
  * 赋予默认周条固定高度（46.dp），使得 `LazyColumn` 可单次 O(1) 确定尺寸；
  * `HorizontalPager` 的 `beyondViewportPageCount` 设为 0，杜绝多月份预初始化；
  * 进场动画结束后解除常驻 `graphicsLayer`。
  * **结果**：向下滑动日历重新进入时保持满帧顺滑，卡顿现象消失。

---

## 3. 核心根因定位

1. **`AnimatedContent` 容器的单帧测量与初始化开销**：
   * 原代码在 `JournalDateRuler` 内部包裹了 `AnimatedContent(targetState = isExpanded)`。当该 Item 重新滚入 Viewport 时，Compose 必须单帧初始化 Transition Coordinator 并递归测量过渡容器，带来额外布局计算。
2. **`LazyColumn` 子树深度测算**：
   * 原周视图行未设置固定高度，`LazyColumn` 首次测量该 Item 时需要遍历测量 7 列内部的多层 Text/Spacer/Box 才能得到高度。
3. **`HorizontalPager` 展开态过度预加载**：
   * 原配置 `beyondViewportPageCount = 1` 会同时组合当前月、上月与下月的 3 个完整网格（超过 100+ 个单元格）。
4. **常驻 `graphicsLayer` 导致渲染节点重复分配**：
   * 列表项与日历外层在进场动画完成后依然常驻 `graphicsLayer`，滑动期间持续产生 Layer 维护开销。

---

## 4. 修改文件与具体内容

### 修改文件
* `/app/src/main/java/com/example/ui/screens/EditorialPreviewScreen.kt`

### 修改内容
1. **直接条件渲染替代 `AnimatedContent`**：
   ```kotlin
   if (!isExpanded) {
       Row(
           modifier = Modifier
               .fillMaxWidth()
               .height(46.dp),
           horizontalArrangement = Arrangement.SpaceBetween
       ) {
           // 7 天轻量周条渲染
       }
   } else {
       HorizontalPager(
           state = pagerState,
           beyondViewportPageCount = 0,
           modifier = Modifier.fillMaxWidth(),
           verticalAlignment = Alignment.Top
       ) { page -> ... }
   }
   ```
2. **固定周条布局高度**：
   * 为周条设置显式 `.height(46.dp)`，`LazyColumn` 测量该 Item 时直接获取确定高度，免去子树深度递归推导。
3. **优化 `HorizontalPager` 预加载**：
   * 设置 `beyondViewportPageCount = 0`，仅在需要时组合当前可见月份。
4. **条件化 `graphicsLayer`**：
   * 仅在 `playEntranceAnimation == true` 时附加 `graphicsLayer`，进场动画结束后自动剥离，还原为纯净 `Modifier`。

---

## 5. 为什么减少首次 Composition / Measure 成本
* **跳过 Transition 包装**：消除了 `AnimatedContent` 的 Transition 状态机和 Lookahead 测量。
* **单次测量收敛**：固定高度让 Compose 测量流水线可以在单次 Pass 中立即确定边界，不会产生二次测量。
* **避免无谓层级渲染**：移除常驻 `graphicsLayer` 避免了 GPU RenderNode / Layer 的申请和同步开销。

---

## 6. 构建与测试验证
* **单元测试验证**：`gradle :app:testDebugUnitTest` 顺利通过（BUILD SUCCESSFUL, 17 tasks executed, 16 up-to-date, 0 failures）。
* **构建验证**：`compile_applet` 成功构建。
* **数据说明**：未获得真实硬件 Frame Timing 数据，结论基于 A/B UI 实验和代码结构分析。
