package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppBackgroundConfig

@Composable
fun CustomNumpad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    confirmColor: Color = Color(0xFF6366F1),
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    
    val buttons = listOf(
        listOf("7", "8", "9", "⌫"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "C", "=") // Or "OK" based on expression
    )

    Column(modifier = modifier.fillMaxWidth().padding(4.dp)) {
        for (row in buttons) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                for (btn in row) {
                    val isAction = btn in listOf("⌫", "+", "-", "C", "=")
                    val isConfirm = btn == "="
                    
                    // Show "OK" if it's "=" but expression doesn't end with operator and doesn't contain operator to evaluate, wait, let's keep it simple: = always evaluates, if evaluated it's OK
                    val displayBtn = if (isConfirm) {
                        if (canEvaluate(expression) && hasOperator(expression)) "=" else "OK"
                    } else btn
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isConfirm && displayBtn == "OK") confirmColor 
                                else if (isConfirm) confirmColor.copy(alpha = 0.7f)
                                else if (bgConfig.isLight) Color.White else Color(0xFF2A2A2A)
                            )
                            .clickable {
                                handleNumpadInput(btn, expression, onExpressionChange, onConfirm)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayBtn,
                            fontSize = 22.sp,
                            fontWeight = if (isConfirm) FontWeight.Bold else FontWeight.Medium,
                            color = if (isConfirm) Color.White 
                                    else if (isAction) confirmColor 
                                    else bgConfig.textPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun hasOperator(expr: String): Boolean {
    return expr.contains("+") || expr.contains("-") || expr.contains("*") || expr.contains("/")
}

private fun canEvaluate(expr: String): Boolean {
    if (expr.isEmpty()) return false
    val lastChar = expr.last()
    return lastChar.isDigit() || lastChar == '.'
}

private fun handleNumpadInput(
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
        "+", "-" -> {
            if (expr.isNotEmpty()) {
                val lastChar = expr.last()
                if (lastChar == '+' || lastChar == '-') {
                    onChange(expr.dropLast(1) + input)
                } else {
                    if (hasOperator(expr)) {
                        val evaluated = evaluateExpression(expr)
                        onChange(evaluated + input)
                    } else {
                        onChange(expr + input)
                    }
                }
            } else if (input == "-") {
                onChange("-")
            }
        }
        "." -> {
            // Basic check to prevent multiple dots in current number segment
            val currentNumber = expr.split(Regex("[+\\-]")).lastOrNull() ?: ""
            if (!currentNumber.contains(".")) {
                onChange(expr + input)
            }
        }
        else -> { // Digits
            onChange(expr + input)
        }
    }
}

private fun evaluateExpression(expr: String): String {
    try {
        var result = 0.0
        val tokens = expr.split(Regex("(?<=[+\\-])|(?=[+\\-])"))
        
        var currentOp = "+"
        for (token in tokens) {
            val t = token.trim()
            if (t == "+" || t == "-") {
                currentOp = t
            } else if (t.isNotEmpty()) {
                val value = t.toDoubleOrNull() ?: 0.0
                if (currentOp == "+") result += value
                else if (currentOp == "-") result -= value
            }
        }
        // Format to 2 decimal places if needed, or remove trailing .0
        val formatted = if (result % 1 == 0.0) {
            result.toLong().toString()
        } else {
            String.format("%.2f", result).replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
        }
        return formatted
    } catch (e: Exception) {
        return expr
    }
}
