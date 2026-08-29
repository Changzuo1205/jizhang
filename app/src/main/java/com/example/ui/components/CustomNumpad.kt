package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppBackgroundConfig

/**
 * 专业记账 5 列计算器集成键盘
 * 布局：
 * [ 1 ] [ 2 ] [ 3 ] [ + ] [ ⌫ ]
 * [ 4 ] [ 5 ] [ 6 ] [ - ] [再记]
 * [ 7 ] [ 8 ] [ 9 ] [ × ] [   ]
 * [ C ] [ 0 ] [ . ] [ = ] [保存]
 */
@Composable
fun AccountingNumpad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onSaveAndNext: (() -> Unit)? = null,
    confirmColor: Color = Color(0xFFF97316),
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current

    val keyBg = if (bgConfig.isLight) Color(0xFFF1F5F9).copy(alpha = 0.92f) else Color(0xFF1E2330).copy(alpha = 0.75f)
    val opKeyBg = if (bgConfig.isLight) Color(0xFFE2E8F0).copy(alpha = 0.92f) else Color(0xFF282F40).copy(alpha = 0.85f)
    val textColor = bgConfig.textPrimary
    val textMuted = bgConfig.textSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: 1, 2, 3, +, Backspace
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumpadKey("1", textColor, keyBg, Modifier.weight(1f)) {
                handleInput("1", expression, onExpressionChange, onConfirm)
            }
            NumpadKey("2", textColor, keyBg, Modifier.weight(1f)) {
                handleInput("2", expression, onExpressionChange, onConfirm)
            }
            NumpadKey("3", textColor, keyBg, Modifier.weight(1f)) {
                handleInput("3", expression, onExpressionChange, onConfirm)
            }
            NumpadKey("+", confirmColor, opKeyBg, Modifier.weight(1f), fontSize = 20.sp) {
                handleInput("+", expression, onExpressionChange, onConfirm)
            }
            // Backspace key
            NumpadIconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                tint = textMuted,
                bgColor = opKeyBg,
                modifier = Modifier.weight(1f)
            ) {
                handleInput("⌫", expression, onExpressionChange, onConfirm)
            }
        }

        // Row 2: 4, 5, 6, -, 再记
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumpadKey("4", textColor, keyBg, Modifier.weight(1f)) {
                handleInput("4", expression, onExpressionChange, onConfirm)
            }
            NumpadKey("5", textColor, keyBg, Modifier.weight(1f)) {
                handleInput("5", expression, onExpressionChange, onConfirm)
            }
            NumpadKey("6", textColor, keyBg, Modifier.weight(1f)) {
                handleInput("6", expression, onExpressionChange, onConfirm)
            }
            NumpadKey("-", confirmColor, opKeyBg, Modifier.weight(1f), fontSize = 20.sp) {
                handleInput("-", expression, onExpressionChange, onConfirm)
            }
            NumpadKey(
                text = "再记",
                textColor = if (bgConfig.isLight) Color(0xFF334155) else Color(0xFFCBD5E1),
                bgColor = opKeyBg,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ) {
                if (hasOperator(expression) && canEvaluate(expression)) {
                    val evaluated = evaluateExpression(expression)
                    onExpressionChange(evaluated)
                }
                onSaveAndNext?.invoke() ?: onConfirm()
            }
        }

        // Row 3 & 4 combined for the 2-row Save button
        Row(
            modifier = Modifier.fillMaxWidth().weight(2f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left 4x2 grid (Numbers 7,8,9, ×, C,0,., =)
            Column(
                modifier = Modifier.weight(4f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 3 left: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NumpadKey("7", textColor, keyBg, Modifier.weight(1f)) {
                        handleInput("7", expression, onExpressionChange, onConfirm)
                    }
                    NumpadKey("8", textColor, keyBg, Modifier.weight(1f)) {
                        handleInput("8", expression, onExpressionChange, onConfirm)
                    }
                    NumpadKey("9", textColor, keyBg, Modifier.weight(1f)) {
                        handleInput("9", expression, onExpressionChange, onConfirm)
                    }
                    NumpadKey("×", confirmColor, opKeyBg, Modifier.weight(1f), fontSize = 18.sp) {
                        handleInput("×", expression, onExpressionChange, onConfirm)
                    }
                }

                // Row 4 left: C, 0, ., =
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NumpadKey("C", textMuted, opKeyBg, Modifier.weight(1f), fontSize = 15.sp) {
                        handleInput("C", expression, onExpressionChange, onConfirm)
                    }
                    NumpadKey("0", textColor, keyBg, Modifier.weight(1f)) {
                        handleInput("0", expression, onExpressionChange, onConfirm)
                    }
                    NumpadKey(".", textColor, keyBg, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold) {
                        handleInput(".", expression, onExpressionChange, onConfirm)
                    }
                    NumpadKey("=", confirmColor, opKeyBg, Modifier.weight(1f), fontSize = 18.sp) {
                        handleInput("=", expression, onExpressionChange, onConfirm)
                    }
                }
            }

            // Right 1x2 column: Save Button spanning 2 rows
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                confirmColor,
                                confirmColor.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White)
                    ) {
                        if (hasOperator(expression) && canEvaluate(expression)) {
                            val evaluated = evaluateExpression(expression)
                            onExpressionChange(evaluated)
                        }
                        onConfirm()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保 存",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun NumpadKey(
    text: String,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 19.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(11.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

@Composable
private fun NumpadIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(11.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "退格",
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

private fun hasOperator(expr: String): Boolean {
    return expr.contains("+") || expr.contains("-") || expr.contains("×") || expr.contains("*") || expr.contains("÷") || expr.contains("/")
}

private fun canEvaluate(expr: String): Boolean {
    if (expr.isEmpty()) return false
    val lastChar = expr.last()
    return lastChar.isDigit() || lastChar == '.'
}

private fun handleInput(
    input: String,
    expr: String,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    when (input) {
        "C" -> onChange("")
        "⌫" -> if (expr.isNotEmpty()) onChange(expr.dropLast(1))
        "=" -> {
            if (hasOperator(expr) && canEvaluate(expr)) {
                val result = evaluateExpression(expr)
                onChange(result)
            } else {
                onConfirm()
            }
        }
        "+", "-", "×", "÷" -> {
            val op = if (input == "×") "*" else if (input == "÷") "/" else input
            if (expr.isNotEmpty()) {
                val lastChar = expr.last()
                if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/' || lastChar == '×' || lastChar == '÷') {
                    onChange(expr.dropLast(1) + op)
                } else {
                    if (hasOperator(expr)) {
                        val evaluated = evaluateExpression(expr)
                        onChange(evaluated + op)
                    } else {
                        onChange(expr + op)
                    }
                }
            } else if (input == "-") {
                onChange("-")
            }
        }
        "." -> {
            val currentNumber = expr.split(Regex("[+\\-*/×÷]")).lastOrNull() ?: ""
            if (!currentNumber.contains(".")) {
                if (currentNumber.isEmpty()) {
                    onChange(expr + "0.")
                } else {
                    onChange(expr + ".")
                }
            }
        }
        else -> { // Digits 0-9
            val currentNumber = expr.split(Regex("[+\\-*/×÷]")).lastOrNull() ?: ""
            if (currentNumber == "0" && input != ".") {
                // Replace leading single 0
                val prefix = expr.dropLast(1)
                onChange(prefix + input)
            } else if (currentNumber.contains(".") && currentNumber.substringAfter(".").length >= 2) {
                // Keep max 2 decimals per number segment
                return
            } else {
                onChange(expr + input)
            }
        }
    }
}

fun evaluateExpression(expr: String): String {
    try {
        val normalized = expr.replace("×", "*").replace("÷", "/")
        // Simple token evaluation for left-to-right or standard arithmetic
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        for (ch in normalized) {
            if (ch in listOf('+', '-', '*', '/')) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current = StringBuilder()
                }
                tokens.add(ch.toString())
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }

        if (tokens.isEmpty()) return expr

        // First pass: * and /
        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if ((t == "*" || t == "/") && pass1.isNotEmpty() && i + 1 < tokens.size) {
                val prev = pass1.removeAt(pass1.size - 1).toDoubleOrNull() ?: 0.0
                val next = tokens[i + 1].toDoubleOrNull() ?: 0.0
                val res = if (t == "*") prev * next else if (next != 0.0) prev / next else 0.0
                pass1.add(res.toString())
                i += 2
            } else {
                pass1.add(t)
                i++
            }
        }

        // Second pass: + and -
        var result = pass1.firstOrNull()?.toDoubleOrNull() ?: 0.0
        var j = 1
        while (j < pass1.size) {
            val op = pass1[j]
            val next = if (j + 1 < pass1.size) pass1[j + 1].toDoubleOrNull() ?: 0.0 else 0.0
            if (op == "+") result += next
            else if (op == "-") result -= next
            j += 2
        }

        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", result).replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
        }
    } catch (e: Exception) {
        return expr
    }
}

// Retain compatibility with existing CustomNumpad callers
@Composable
fun CustomNumpad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    confirmColor: Color = Color(0xFF6366F1),
    modifier: Modifier = Modifier
) {
    AccountingNumpad(
        expression = expression,
        onExpressionChange = onExpressionChange,
        onConfirm = onConfirm,
        confirmColor = confirmColor,
        modifier = modifier
    )
}
