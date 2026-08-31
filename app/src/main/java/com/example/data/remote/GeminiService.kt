package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class GeminiModel(val modelId: String, val displayName: String, val shortBadge: String, val description: String) {
    FLASH_3_5("gemini-3.5-flash", "Gemini 3.5 Flash", "3.5 Flash", "推荐 · 快速平衡与通用智能"),
    PRO_3_1("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "3.1 Pro", "进阶 · 复杂理财与深度推理分析"),
    FLASH_LITE_3_1("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash-Lite", "3.1 Lite", "极速 · 毫秒级快速问答与小助手")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val modelUsed: String? = null
)

data class BotRole(
    val id: String,
    val title: String,
    val subtitle: String,
    val systemInstruction: String,
    val starterPrompts: List<String>
)

object BotRoles {
    val FINANCIAL_ADVISOR = BotRole(
        id = "financial_advisor",
        title = "理财与预算顾问",
        subtitle = "消费规划 · 50/30/20 法则 · 储蓄策略",
        systemInstruction = """
            你是一位专业、温暖且富有洞察力的个人财务与理财预算顾问。
            你的职责是：
            1. 协助用户分析日常支出与收入结构，识别不必要的消费黑洞。
            2. 提供实用的 50/30/20 预算法则、信封攒钱法、阶梯储蓄法等财务管理建议。
            3. 耐心解答用户的记账疑惑，给出温和但有执行力的省钱与理财方案。
            4. 保持中文回答结构清晰，适度使用小标题、要点列表与生动的表情符号，语言亲切务实。
            5. 在用户提供账目背景数据时，结合具体数字进行针对性分析。
        """.trimIndent(),
        starterPrompts = listOf(
            "如何用 50/30/20 法则合理分配月薪？",
            "有哪些适合年轻人的无痛日常攒钱小技巧？",
            "月光族应该如何建立第一笔 3-6 个月应急备用金？",
            "怎样戒掉每天外卖和冲动网购的消费习惯？"
        )
    )

    val LEDGER_ANALYST = BotRole(
        id = "ledger_analyst",
        title = "账本数据分析师",
        subtitle = "财务诊断 · 消费洞察 · 资产健康度",
        systemInstruction = """
            你是一位严谨、敏锐的账本数据分析专家。
            你的职责是：
            1. 擅长从收支结构、固定开销占比、资产负债比等维度评估财务健康状况。
            2. 指出消费趋势异常，评估负债率与现金流安全性。
            3. 输出条理分明的数据透视、风险提示与优化优先级（高/中/低）。
            4. 采用客观、专业的分析视角，给出可量化的改进指标。
        """.trimIndent(),
        starterPrompts = listOf(
            "帮我诊断一下：餐饮占总支出 45% 是否过高？",
            "如何计算个人的资产负债率与现金流健康度？",
            "怎样设计一份年度财务目标与收支追踪表？",
            "固定支出占比太高，应该优先削减哪些项目？"
        )
    )

    val MINIMALIST_COACH = BotRole(
        id = "minimalist_coach",
        title = "极简生活教练",
        subtitle = "理性消费 · 断舍离 · 记账心法",
        systemInstruction = """
            你是一位倡导极简主义与理性消费的生活哲学教练。
            你的职责是：
            1. 帮助用户在买买买之前进行心理冷却（如 72 小时心动法则、每物单次使用成本计算）。
            2. 引导用户区分「需要」与「想要」，降低物欲与精神内耗。
            3. 鼓励长期、轻松、不焦虑的极简记账习惯。
            4. 语言温和平静、充满治愈力量。
        """.trimIndent(),
        starterPrompts = listOf(
            "总觉得记账繁琐坚持不下去，有什么极简心法？",
            "下单前如何判断一件物品是「需要」还是「想要」？",
            "如何克服打折促销时的囤货焦虑？",
            "分享一个 72 小时消费冷却清单的实践方法"
        )
    )

    val ALL_ROLES = listOf(FINANCIAL_ADVISOR, LEDGER_ANALYST, MINIMALIST_COACH)
}

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    suspend fun sendChat(
        history: List<ChatMessage>,
        systemInstruction: String,
        model: GeminiModel
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }

            if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    Exception("未检测到有效的 Gemini API Key。请在 AI Studio 的 Secrets 面板中配置 GEMINI_API_KEY 即可正常对话。")
                )
            }

            val requestJson = JSONObject()

            // System Instruction
            if (systemInstruction.isNotBlank()) {
                val sysInstructionObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPart = JSONObject().put("text", systemInstruction)
                sysPartsArray.put(sysPart)
                sysInstructionObj.put("parts", sysPartsArray)
                requestJson.put("systemInstruction", sysInstructionObj)
            }

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", genConfig)

            // Contents array (Multi-turn conversation history)
            val contentsArray = JSONArray()
            history.forEach { msg ->
                if (!msg.isError && msg.content.isNotBlank()) {
                    val turnObj = JSONObject()
                    turnObj.put("role", if (msg.role == "user") "user" else "model")
                    val partsArray = JSONArray()
                    val partObj = JSONObject().put("text", msg.content)
                    partsArray.put(partObj)
                    turnObj.put("parts", partsArray)
                    contentsArray.put(turnObj)
                }
            }
            requestJson.put("contents", contentsArray)

            val url = "$BASE_URL${model.modelId}:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    val err = errJson.optJSONObject("error")
                    err?.optString("message") ?: "HTTP ${response.code}: $responseBody"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCand = candidates.getJSONObject(0)
                val contentObj = firstCand.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val replyText = parts.getJSONObject(0).optString("text", "")
                    if (replyText.isNotBlank()) {
                        return@withContext Result.success(replyText)
                    }
                }
            }
            Result.failure(Exception("Gemini 未返回有效文本回复"))
        } catch (e: Exception) {
            Log.e("GeminiService", "API Request failed", e)
            Result.failure(e)
        }
    }
}
