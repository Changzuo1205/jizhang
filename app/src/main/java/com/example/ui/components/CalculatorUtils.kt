package com.example.ui.components

import java.text.DecimalFormat

data class DaySummary(
    val dayNumber: Int,
    val dateTimestamp: Long = 0L,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val isToday: Boolean = false,
    val isCurrentMonth: Boolean = true
)

fun hasOperator(expr: String): Boolean {
    return expr.contains("+") || expr.contains("-") || expr.contains("×") || expr.contains("÷") || expr.contains("*") || expr.contains("/")
}

fun canEvaluate(expr: String): Boolean {
    if (!hasOperator(expr)) return false
    val lastChar = expr.lastOrNull() ?: return false
    return lastChar.isDigit() || lastChar == '.'
}

fun evaluateExpression(expr: String): String {
    try {
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        val tokens = mutableListOf<String>()
        var currentNum = StringBuilder()
        for (ch in sanitized) {
            if (ch in "+-*/") {
                if (currentNum.isNotEmpty()) {
                    tokens.add(currentNum.toString())
                    currentNum = StringBuilder()
                }
                tokens.add(ch.toString())
            } else {
                currentNum.append(ch)
            }
        }
        if (currentNum.isNotEmpty()) {
            tokens.add(currentNum.toString())
        }

        if (tokens.isEmpty()) return expr

        // Pass 1: * and /
        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/") {
                val prev = pass1.removeAt(pass1.lastIndex).toDoubleOrNull() ?: 0.0
                val next = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: 1.0
                val res = if (token == "*") prev * next else if (next != 0.0) prev / next else 0.0
                pass1.add(res.toString())
                i += 2
            } else {
                pass1.add(token)
                i++
            }
        }

        // Pass 2: + and -
        var result = pass1.firstOrNull()?.toDoubleOrNull() ?: 0.0
        var j = 1
        while (j < pass1.size) {
            val op = pass1[j]
            val nextVal = pass1.getOrNull(j + 1)?.toDoubleOrNull() ?: 0.0
            if (op == "+") {
                result += nextVal
            } else if (op == "-") {
                result -= nextVal
            }
            j += 2
        }

        val df = DecimalFormat("#.##")
        return df.format(result)
    } catch (e: Exception) {
        return expr
    }
}

fun handleInput(
    input: String,
    currentExpr: String,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit = {}
) {
    when (input) {
        "C" -> onExpressionChange("0")
        "DEL", "⌫" -> {
            if (currentExpr.length <= 1) {
                onExpressionChange("0")
            } else {
                onExpressionChange(currentExpr.dropLast(1))
            }
        }
        "=" -> {
            if (hasOperator(currentExpr) && canEvaluate(currentExpr)) {
                onExpressionChange(evaluateExpression(currentExpr))
            }
        }
        "+", "-", "×", "÷" -> {
            if (currentExpr.isEmpty() || currentExpr == "0") {
                if (input == "-") onExpressionChange("-")
            } else {
                val last = currentExpr.last()
                if (last in "+-×÷") {
                    onExpressionChange(currentExpr.dropLast(1) + input)
                } else {
                    onExpressionChange(currentExpr + input)
                }
            }
        }
        "." -> {
            val lastPart = currentExpr.split("+", "-", "×", "÷").lastOrNull() ?: ""
            if (!lastPart.contains(".")) {
                onExpressionChange(if (currentExpr == "0" || currentExpr.isEmpty()) "0." else "$currentExpr.")
            }
        }
        else -> {
            // Numbers
            if (currentExpr == "0") {
                onExpressionChange(input)
            } else {
                onExpressionChange(currentExpr + input)
            }
        }
    }
}
