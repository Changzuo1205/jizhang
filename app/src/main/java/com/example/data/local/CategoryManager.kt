package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.components.GlowAmber
import com.example.ui.components.GlowCyan
import com.example.ui.components.GlowEmerald
import com.example.ui.components.GlowPink
import com.example.ui.components.GlowViolet
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * 记账分类数据模型
 *
 * @property name 分类名称（如："餐饮"、"交通"、"购物"）
 * @property type 分类所属类型："EXPENSE" (支出) 或 "INCOME" (收入)
 * @property defaultSubcategories 该分类下的默认二级子分类列表
 * @property isCustom 是否为用户自定义添加的分类
 */
data class CategoryItem(
    val name: String,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val defaultSubcategories: List<String> = emptyList(),
    val isCustom: Boolean = false
)

/**
 * 记账分类管理器 (单例)
 *
 * 集中管理系统预置的一级分类、二级子分类、用户自定义分类扩展、
 * 智能餐饮按时段推荐（早/中/晚/夜宵）、记忆上次选择，以及分类图标与发光色彩映射。
 */
object CategoryManager {
    private const val PREFS_NAME = "category_preferences"
    private const val KEY_CUSTOM_CATEGORIES = "custom_categories_v1"
    private const val KEY_CUSTOM_SUBCATEGORIES = "custom_subcategories_v1"
    private const val KEY_LAST_SELECTED_SUBCAT = "last_selected_subcategories_v1"
    private const val KEY_LAST_INCOME_CATEGORY = "last_income_category_v1"

    // 1. 系统预置支出一级分类及二级分类
    val DEFAULT_EXPENSE_CATEGORIES = listOf(
        CategoryItem(
            name = "餐饮",
            type = "EXPENSE",
            defaultSubcategories = listOf("早餐", "午餐", "晚餐", "夜宵", "零食", "饮料水果", "买菜原料", "油盐酱醋", "餐饮其他", "其他")
        ),
        CategoryItem(
            name = "交通",
            type = "EXPENSE",
            defaultSubcategories = listOf("打车", "公交", "加油", "停车费", "地铁", "火车", "长途汽车", "飞机", "自行车", "船舶", "保养维修", "过路过桥", "罚款赔偿", "车款车贷", "车险", "驾照费用", "交通其他", "其他")
        ),
        CategoryItem(
            name = "购物",
            type = "EXPENSE",
            defaultSubcategories = listOf("服饰鞋包", "家居百货", "宝宝用品", "化妆护肤", "烟酒", "电子数码", "文具玩具", "报刊书籍", "珠宝首饰", "家具家纺", "保健用品", "电器", "摄影文印", "购物其他", "其他")
        ),
        CategoryItem(
            name = "娱乐",
            type = "EXPENSE",
            defaultSubcategories = listOf("旅游度假", "电影", "网游电玩", "麻将棋牌", "洗浴足浴", "运动健身", "花鸟宠物", "聚会玩乐", "茶酒咖啡", "卡拉OK", "歌舞演出", "电视", "娱乐其他", "其他")
        ),
        CategoryItem(
            name = "医教",
            type = "EXPENSE",
            defaultSubcategories = listOf("医疗药品", "挂号门诊", "养生保健", "住院费", "养老院", "学杂教材", "培训考试", "幼儿教育", "学费", "家教补习", "出国留学", "助学贷款", "医教其他", "其他")
        ),
        CategoryItem(
            name = "居家",
            type = "EXPENSE",
            defaultSubcategories = listOf("手机电话", "水电燃气", "生活费", "美发美容", "住宿房租", "材料建材", "房款房贷", "快递邮政", "电脑宽带", "家政服务", "物业", "税费手续费", "保险费", "消费贷款", "婚庆摄影", "漏记款", "生活其他", "其他")
        ),
        CategoryItem(
            name = "投资",
            type = "EXPENSE",
            defaultSubcategories = listOf("利息支出", "保险", "出资", "基金", "股票", "P2P", "余额宝", "理财产品", "投资贷款", "银行存款", "证券期货", "外汇", "贵金属", "收藏品", "投资其他", "其他")
        ),
        CategoryItem(
            name = "人情",
            type = "EXPENSE",
            defaultSubcategories = listOf("礼金红包", "物品", "孝敬", "请客", "给予", "代付款", "慈善捐款", "人情其他", "其他")
        ),
        CategoryItem(
            name = "生意",
            type = "EXPENSE",
            defaultSubcategories = listOf("进货采购", "人工支出", "材料辅料", "办公费用", "交通运输", "工程付款", "运营费", "会务费", "营销广告", "店面租金", "注册登记", "生意其他", "其他")
        ),
        CategoryItem(
            name = "资金流转",
            type = "EXPENSE",
            defaultSubcategories = listOf("应收款", "报销款", "公积金", "销售款", "退款返款", "其他")
        ),
        CategoryItem(
            name = "其他",
            type = "EXPENSE",
            defaultSubcategories = listOf("其他")
        )
    )

    // 2. 系统预置收入一级分类及二级分类 (收入不区分层级，一级与二级完全相同)
    val DEFAULT_INCOME_CATEGORIES = listOf(
        CategoryItem(name = "工资薪水", type = "INCOME", defaultSubcategories = listOf("工资薪水")),
        CategoryItem(name = "利息", type = "INCOME", defaultSubcategories = listOf("利息")),
        CategoryItem(name = "兼职外快", type = "INCOME", defaultSubcategories = listOf("兼职外快")),
        CategoryItem(name = "营业收入", type = "INCOME", defaultSubcategories = listOf("营业收入")),
        CategoryItem(name = "红包", type = "INCOME", defaultSubcategories = listOf("红包")),
        CategoryItem(name = "销售款", type = "INCOME", defaultSubcategories = listOf("销售款")),
        CategoryItem(name = "退款返款", type = "INCOME", defaultSubcategories = listOf("退款返款")),
        CategoryItem(name = "报销款", type = "INCOME", defaultSubcategories = listOf("报销款")),
        CategoryItem(name = "福利补贴", type = "INCOME", defaultSubcategories = listOf("福利补贴")),
        CategoryItem(name = "余额宝", type = "INCOME", defaultSubcategories = listOf("余额宝")),
        CategoryItem(name = "应收款", type = "INCOME", defaultSubcategories = listOf("应收款")),
        CategoryItem(name = "生活费", type = "INCOME", defaultSubcategories = listOf("生活费")),
        CategoryItem(name = "基金", type = "INCOME", defaultSubcategories = listOf("基金")),
        CategoryItem(name = "礼金", type = "INCOME", defaultSubcategories = listOf("礼金")),
        CategoryItem(name = "分红股票", type = "INCOME", defaultSubcategories = listOf("分红股票")),
        CategoryItem(name = "公积金", type = "INCOME", defaultSubcategories = listOf("公积金")),
        CategoryItem(name = "赔付款", type = "INCOME", defaultSubcategories = listOf("赔付款")),
        CategoryItem(name = "漏记款", type = "INCOME", defaultSubcategories = listOf("漏记款")),
        CategoryItem(name = "其他", type = "INCOME", defaultSubcategories = listOf("其他"))
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 根据当前具体时间，智能推荐餐饮二级分类：
     * - 04:00 - 10:00 -> 早餐
     * - 10:00 - 16:30 -> 午餐
     * - 16:30 - 20:30 -> 晚餐
     * - 20:30 - 次日04:00 -> 宵夜
     */
    fun getTimeBasedDiningSubcategory(timestamp: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val minutesFromMidnight = hour * 60 + minute

        return when {
            minutesFromMidnight in (4 * 60) until (10 * 60) -> "早餐"
            minutesFromMidnight in (10 * 60) until (16 * 60 + 30) -> "午餐"
            minutesFromMidnight in (16 * 60 + 30) until (20 * 60 + 30) -> "晚餐"
            else -> "宵夜"
        }
    }

    /**
     * 获取指定类型（支出/收入）下的全部一级分类列表（含用户自定义分类），并保证“其他”始终固定置底
     */
    fun getCategories(context: Context, type: String): List<CategoryItem> {
        val defaultList = if (type == "EXPENSE") DEFAULT_EXPENSE_CATEGORIES else DEFAULT_INCOME_CATEGORIES
        val customList = getCustomCategories(context, type)
        
        val defaultWithoutOther = defaultList.filter { it.name != "其他" }
        val customWithoutOther = customList.filter { it.name != "其他" }
        val otherCategory = defaultList.find { it.name == "其他" } ?: customList.find { it.name == "其他" } ?: CategoryItem(
            name = "其他",
            type = type,
            defaultSubcategories = listOf("其他")
        )

        return defaultWithoutOther + customWithoutOther + listOf(otherCategory)
    }

    /**
     * 获取指定分类下的全部二级细分列表（合并预置与自定义扩展），保证“其他”置底
     * 收入类型统一不区分层级，一级与二级分类保持相同。
     */
    fun getSubcategories(context: Context, categoryName: String, type: String): List<String> {
        if (type == "INCOME") {
            return listOf(categoryName)
        }
        val allCats = getCategories(context, type)
        val matched = allCats.find { it.name == categoryName }
        val baseSubs = matched?.defaultSubcategories ?: listOf("其他")

        // 合并用户针对该分类扩展的自定义子分类
        val customSubsMap = getCustomSubcategoriesMap(context)
        val extraSubs = customSubsMap[categoryName] ?: emptyList()

        val combined = LinkedHashSet<String>()
        val baseWithoutOther = baseSubs.filter { it != "其他" }
        val extraWithoutOther = extraSubs.filter { it != "其他" }

        combined.addAll(baseWithoutOther)
        combined.addAll(extraWithoutOther)
        combined.add("其他")
        return combined.toList()
    }

    /**
     * 智能计算默认选中的二级分类：
     * - 收入类型直接返回一级分类同名
     * - 餐饮分类：新建时按当前时间段智能推荐（早餐/午餐/晚餐/夜宵）
     * - 其余分类：优先使用用户上次选择的历史偏好，若无则回退至该分类的首个二级项
     */
    fun getDefaultSubcategory(
        context: Context,
        categoryName: String,
        type: String,
        isFreshCreation: Boolean = true,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        if (type == "INCOME") {
            return categoryName
        }
        val subcategories = getSubcategories(context, categoryName, type)
        if (subcategories.isEmpty()) return "其他"

        if (categoryName == "餐饮" && type == "EXPENSE") {
            if (isFreshCreation) {
                val timeMeal = getTimeBasedDiningSubcategory(timestamp)
                if (subcategories.contains(timeMeal)) return timeMeal
            }
        }

        // 检查是否有最近的历史偏好记录
        val lastSelected = getLastSelectedSubcategory(context, categoryName)
        if (lastSelected.isNotBlank() && subcategories.contains(lastSelected)) {
            return lastSelected
        }

        if (categoryName == "餐饮" && type == "EXPENSE") {
            val timeMeal = getTimeBasedDiningSubcategory(timestamp)
            if (subcategories.contains(timeMeal)) return timeMeal
        }

        return subcategories.first()
    }

    /**
     * 保存用户对特定分类最后选中的二级子分类
     */
    fun saveLastSelectedSubcategory(context: Context, categoryName: String, subcategory: String) {
        if (categoryName.isBlank() || subcategory.isBlank()) return
        val prefs = getPrefs(context)
        try {
            val raw = prefs.getString(KEY_LAST_SELECTED_SUBCAT, "{}") ?: "{}"
            val json = JSONObject(raw)
            json.put(categoryName, subcategory)
            prefs.edit().putString(KEY_LAST_SELECTED_SUBCAT, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 读取用户对特定分类最后选中的二级子分类
     */
    fun getLastSelectedSubcategory(context: Context, categoryName: String): String {
        val prefs = getPrefs(context)
        return try {
            val raw = prefs.getString(KEY_LAST_SELECTED_SUBCAT, "{}") ?: "{}"
            val json = JSONObject(raw)
            json.optString(categoryName, "")
        } catch (e: Exception) {
            ""
        }
    }

    /** 保存用户最后选择的收入分类 */
    fun saveLastIncomeCategory(context: Context, categoryName: String) {
        if (categoryName.isBlank()) return
        getPrefs(context).edit().putString(KEY_LAST_INCOME_CATEGORY, categoryName).apply()
    }

    /** 读取用户最后选择的收入分类 */
    fun getLastIncomeCategory(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_INCOME_CATEGORY, "") ?: ""
    }

    /**
     * 为已有或自定义分类动态追加自定义二级分类
     */
    fun addCustomSubcategory(context: Context, categoryName: String, newSubcategory: String) {
        val cleanName = newSubcategory.trim()
        if (cleanName.isBlank()) return
        val prefs = getPrefs(context)
        try {
            val raw = prefs.getString(KEY_CUSTOM_SUBCATEGORIES, "{}") ?: "{}"
            val json = JSONObject(raw)
            val array = json.optJSONArray(categoryName) ?: JSONArray()
            var exists = false
            for (i in 0 until array.length()) {
                if (array.getString(i) == cleanName) {
                    exists = true
                    break
                }
            }
            if (!exists) {
                array.put(cleanName)
                json.put(categoryName, array)
                prefs.edit().putString(KEY_CUSTOM_SUBCATEGORIES, json.toString()).apply()
            }
            // 自动设为当前分类的首选偏好
            saveLastSelectedSubcategory(context, categoryName, cleanName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 读取全部「自定义二级细分」映射（分类名 → 细分列表，JSON 原始口径）。
     * Phase 2 起由 [com.example.data.local.DatabaseSeeder] 的导入分支消费；
     * 注意键不区分收支类型，导入时需对两端父级做归并。
     */
    fun getCustomSubcategoriesMap(context: Context): Map<String, List<String>> {
        val prefs = getPrefs(context)
        val result = mutableMapOf<String, List<String>>()
        try {
            val raw = prefs.getString(KEY_CUSTOM_SUBCATEGORIES, "{}") ?: "{}"
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val array = json.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                result[key] = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * 添加自定义一级大分类
     */
    fun addCustomCategory(context: Context, name: String, type: String, subcategories: List<String>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val prefs = getPrefs(context)
        try {
            val raw = prefs.getString(KEY_CUSTOM_CATEGORIES, "[]") ?: "[]"
            val array = JSONArray(raw)
            val subsArray = JSONArray()
            val list = if (subcategories.isEmpty()) listOf("其他") else subcategories
            list.forEach { subsArray.put(it) }

            val obj = JSONObject().apply {
                put("name", cleanName)
                put("type", type)
                put("subcategories", subsArray)
            }
            array.put(obj)
            prefs.edit().putString(KEY_CUSTOM_CATEGORIES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 读取指定类型下的全部用户自定义一级分类（JSON 原始口径，供 v11 迁移导入复用）。
     * Phase 2 起由 [com.example.data.local.DatabaseSeeder] 的导入分支消费。
     */
    fun getCustomCategories(context: Context, type: String): List<CategoryItem> {
        val prefs = getPrefs(context)
        val list = mutableListOf<CategoryItem>()
        try {
            val raw = prefs.getString(KEY_CUSTOM_CATEGORIES, "[]") ?: "[]"
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catType = obj.optString("type", "EXPENSE")
                if (catType == type) {
                    val name = obj.getString("name")
                    val subsArray = obj.optJSONArray("subcategories") ?: JSONArray()
                    val subsList = mutableListOf<String>()
                    for (j in 0 until subsArray.length()) {
                        subsList.add(subsArray.getString(j))
                    }
                    if (subsList.isEmpty()) subsList.add("其他")
                    list.add(CategoryItem(name = name, type = type, defaultSubcategories = subsList, isCustom = true))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * 根据分类名称智能匹配 Material Design 图标矢量资源
     */
    fun getCategoryIcon(category: String): ImageVector {
        return when {
            category.contains("餐饮") || category.contains("早餐") || category.contains("午餐") || category.contains("晚餐") || category.contains("宵夜") || category.contains("美食") -> Icons.Default.Restaurant
            category.contains("交通") || category.contains("打车") || category.contains("公交") || category.contains("地铁") || category.contains("高铁") || category.contains("飞机") -> Icons.Default.DirectionsCar
            category.contains("购物") || category.contains("百货") || category.contains("衣服") || category.contains("数码") -> Icons.Default.ShoppingBag
            category.contains("娱乐") || category.contains("电影") || category.contains("游戏") || category.contains("聚会") -> Icons.Default.SportsEsports
            category.contains("医教") || category.contains("医疗") || category.contains("药品") || category.contains("挂号") || category.contains("学费") -> Icons.Default.MedicalServices
            category.contains("居家") || category.contains("水电") || category.contains("房租") || category.contains("宽带") || category.contains("生活费") -> Icons.Default.Home
            category.contains("分红股票") || category.contains("分红") || category.contains("股票") -> Icons.Default.TrendingUp
            category.contains("投资") || category.contains("基金") || category.contains("利息") -> Icons.Default.TrendingUp
            category.contains("人情") || category.contains("礼金") || category.contains("红包") || category.contains("孝敬") -> Icons.Default.CardGiftcard
            category.contains("工资") || category.contains("薪") -> Icons.Default.Work
            category.contains("奖金") -> Icons.Default.MonetizationOn
            category.contains("兼职") || category.contains("外快") -> Icons.Default.Assignment
            category.contains("营业") || category.contains("应收款") || category.contains("销售") || category.contains("余额宝") -> Icons.Default.AccountBalance
            category.contains("退款") || category.contains("返款") || category.contains("报销") || category.contains("公积金") || category.contains("福利") || category.contains("补贴") -> Icons.Default.Payment
            category.contains("赔付") || category.contains("赔偿") -> Icons.Default.Healing
            category.contains("漏记") || category.contains("校准") -> Icons.Default.Bookmark
            category.contains("转账") || category.contains("余额调整") -> Icons.Default.MoreHoriz
            else -> Icons.Default.Category
        }
    }

    /**
     * 根据分类名称智能匹配赛博朋克/极光玻璃风格的发光主题色彩 (Glow Color)
     */
    fun getCategoryGlowColor(category: String): Color {
        return when {
            category.contains("餐饮") -> GlowAmber
            category.contains("交通") -> GlowCyan
            category.contains("购物") -> GlowPink
            category.contains("娱乐") -> GlowViolet
            category.contains("医教") -> Color(0xFFF87171)
            category.contains("居家") || category.contains("生活费") -> Color(0xFF38BDF8)
            category.contains("投资") || category.contains("利息") -> Color(0xFF34D399)
            category.contains("基金") -> Color(0xFF3B82F6)
            category.contains("人情") || category.contains("礼金") || category.contains("红包") -> Color(0xFFF59E0B)
            category.contains("工资") || category.contains("薪水") -> GlowEmerald
            category.contains("奖金") -> Color(0xFFFBBF24)
            category.contains("兼职") || category.contains("外快") -> Color(0xFF818CF8)
            category.contains("营业") -> Color(0xFF0EA5E9)
            category.contains("销售") -> Color(0xFF10B981)
            category.contains("退款") || category.contains("返款") -> Color(0xFF14B8A6)
            category.contains("报销") || category.contains("公积金") -> Color(0xFF6366F1)
            category.contains("福利") || category.contains("补贴") -> Color(0xFFFBBF24)
            category.contains("余额宝") -> Color(0xFF0284C7)
            category.contains("应收款") || category.contains("应收") -> Color(0xFF059669)
            category.contains("分红股票") || category.contains("分红") || category.contains("股票") -> Color(0xFFEC4899)
            category.contains("赔付") || category.contains("赔偿") -> Color(0xFFA855F7)
            category.contains("转账") || category.contains("余额调整") -> Color(0xFF8B5CF6)
            category.contains("漏记") || category.contains("校准") -> Color(0xFFF97316)
            else -> Color(0xFF9333EA)
        }
    }
}

