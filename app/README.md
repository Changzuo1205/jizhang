# 极光记账 (Aurora Expense Tracker)

一款基于 **Kotlin + Jetpack Compose** 构建的现代化、高性能、拟物玻璃拟态（Glassmorphism）风格的个人财务与资产记账 Android 应用。

---

## 🌟 核心特性

### 1. 现代化记账与资金流转
- **双向收支记录**：支持支出、收入类型，支持一级分类与二级细分分类联动。
- **智能时段感知**：选择“餐饮”分类时，系统根据当前时间自动推荐对应子分类（早 / 中 / 晚 / 宵夜）。
- **个性化九宫格数字键盘**：内置四则运算加减号、一键切换账户、备注快捷输入与回车即时保存。
- **原子级资产联动**：每一笔交易实时扣减或增加关联资产账户（微信、支付宝、银行卡、基金等）余额。
- **平账与漏记款机制**：账户余额校准时支持一键生成“漏记款”冲平差额记录，确保历史总账与当前实际资产严格平衡。

### 2. 预算管理与智能预警
- **多周期预算**：支持**月度**、**季度**、**年度**预算设定与切换。
- **实时进度指示**：动态展示当期已用百分比、本月结余、剩余日均可支配额度。
- **超支警戒高亮**：支出接近（≥80%）或超支（>100%）时自动触发呼吸灯光效与告警色彩。

### 3. 多维统计与图表分析
- **收支占比饼图**：原生 Canvas 绘制的交互式环形扇区饼图，支持点击切片联动高亮与排行透视。
- **收支趋势折线图**：支持近 7 天、本月各周、近半年收支对比折线与柱状走势。
- **账单日历视图**：以月份日历网格呈现每天的收支标记，支持按日精准钻取与筛选明细。

### 4. 极致视觉与个性化定制
- **极光玻璃拟态 (Glassmorphism)**：半透明磨砂玻璃卡片、高光流动边框与发光强调色体系。
- **全动态壁纸方案**：内置 5 种背景方案（深邃星空、极光幻境、清新浅色、赛博霓虹等）。
- **沉浸式 Edge-to-Edge**：系统状态栏、手势导航栏根据背景明暗自动反转对比度。
- **无级字体缩放**：支持小、标准、大、超大四档排版缩放，全界面自适应布局。

---

## 🏗️ 技术架构与选型

- **开发语言**：100% Kotlin
- **UI 框架**：Jetpack Compose (全 Material Design 3 规范)
- **架构模式**：MVVM (Model-View-ViewModel) + 单向数据流 (Unidirectional Data Flow)
- **数据持久化**：Jetpack Room 数据库 (搭配 KSP 代码生成)
- **并发与流式响应**：Kotlin Coroutines + Flow / StateFlow
- **图片与矢量绘制**：Coil Compose + Compose Canvas 原生矢量渲染
- **构建工具**：Gradle (Kotlin DSL - `build.gradle.kts`) + Version Catalog (`libs.versions.toml`)

---

## 📂 项目结构说明

```
app/src/main/
├── assets/
│   └── initial_expenses.json         # 初始预置历史收支账目（800+ 笔真实历史记录）
├── java/com/example/
│   ├── MainActivity.kt               # 应用主入口，处理沉浸式状态栏与主题注入
│   ├── data/
│   │   ├── local/                    # 本地持久化层 (Room DB, Entity, DAO)
│   │   │   ├── ExpenseEntity.kt      # 记账明细实体类 (expenses 表)
│   │   │   ├── AccountEntity.kt      # 资产账户实体类 (accounts 表)
│   │   │   ├── ExpenseDao.kt         # 账目 CRUD 及聚合统计 DAO
│   │   │   ├── AccountDao.kt         # 账户 CRUD 及余额增减 DAO
│   │   │   ├── DailyToolboxDatabase.kt # Room 数据库单例及数据自动预填充
│   │   │   └── CategoryManager.kt    # 分类管理器（预置分类、时段推荐、自定义扩展）
│   │   └── repository/
│   │       └── ToolboxRepository.kt  # 数据仓库层，协调交易与账户余额的原子更新
│   └── ui/
│       ├── components/               # 通用 UI 组件库
│       │   ├── GlassComponents.kt    # 玻璃拟态卡片、高光边框、发光 Chip
│       │   ├── CustomNumpad.kt       # 记账专用九宫格键盘
│       │   ├── BottomNavBar.kt       # 悬浮玻璃态底部导航栏
│       │   ├── MonthCalendarView.kt  # 账单日历月份网格
│       │   ├── ExpenseAddEditDialog.kt # 记账录入与编辑浮层
│       │   ├── BudgetProgressBarCard.kt # 预算进度与预警卡片
│       │   └── AnimatedNumberText.kt # 数字滚动渐变动效
│       ├── screens/                  # 核心功能页面
│       │   ├── MainScreen.kt         # 根容器页面，管理四大 Tab 与二级全屏子页面切换
│       │   ├── ExpenseScreen.kt      # 记账主页（汇总卡片、明细列表、筛选器、快捷录入）
│       │   ├── AccountsScreen.kt     # 资产账户管理（净资产/负债统计、账户卡片、余额校准）
│       │   ├── ReportsScreen.kt      # 统计报表（饼图占比分析、周/月趋势折线、排行榜）
│       │   ├── BillCalendarScreen.kt # 账单日历全屏视图（按日查看明细与月度透视）
│       │   ├── BudgetSettingsScreen.kt# 预算配置全屏页面（月度/季度/年度限额与分配）
│       │   └── MineScreen.kt         # 我的页面（主题切换、壁纸选择、字体缩放、数据导入导出）
│       ├── theme/                    # 视觉主题与设计系统
│       │   ├── ThemeConfig.kt        # 5大背景壁纸、6大主题色、字体缩放模型
│       │   ├── Color.kt              # 赛博极光色系与发光调色板
│       │   ├── Theme.kt              # CompositionLocal 注入与 MaterialTheme 配置
│       │   └── Type.kt               # 排版与字体规格
```

---

## 🚀 构建与运行环境

- **Android Studio**：Ladybug (2024.2.1) 或更高版本
- **Compile SDK**：36
- **Target SDK**：36
- **Min SDK**：24 (Android 7.0+)
- **JDK 版本**：Java 11

### 编译指令
```bash
# 运行单元测试
gradle :app:testDebugUnitTest

# 构建 Debug APK
gradle :app:assembleDebug
```

---

## 📝 核心业务流程接手说明

1. **新增记账流程**：
   - 用户在 `ExpenseAddEditDialog` 中录入金额并点击完成；
   - 调用 `ToolboxViewModel.addExpense(expense)`；
   - 由 `ToolboxRepository` 原子性地写入 `expenses` 表，并根据支出/收入类型更新对应 `accounts` 表的余额字段；
   - `Flow<List<ExpenseEntity>>` 自动发射最新数据，首页明细、统计图表及预算卡片实时刷新。

2. **账户平账机制**：
   - 用户在账户详情中校准余额时，勾选“生成漏记款记录”；
   - `ToolboxRepository.updateAccountWithDiscrepancy` 计算新旧余额差额，自动生成一条类型为“漏记款”的明细，确保资产变化有据可查。

---

## 📄 开源许可证

本项目遵循 [MIT License](LICENSE) 开源协议。
