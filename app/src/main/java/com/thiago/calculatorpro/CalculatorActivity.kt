package com.thiago.calculatorpro

import android.app.Activity
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.DecimalFormat
import kotlin.math.*

class CalculatorActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var expressionView: TextView
    private lateinit var resultView: TextView
    private lateinit var keys: GridLayout

    private var expression = ""
    private var answer = 0.0
    private var memory = 0.0
    private var scientific = true
    private var lightTheme = false
    private val history = mutableListOf<String>()

    private val purple = Color.rgb(165, 90, 255)
    private val dark = Color.rgb(15, 14, 23)
    private val panel = Color.rgb(29, 27, 40)
    private val white = Color.WHITE
    private val formatter = DecimalFormat("0.##########")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun colorBackground(): Int = if (lightTheme)
        Color.rgb(245, 242, 250) else dark

    private fun colorPanel(): Int = if (lightTheme)
        Color.WHITE else panel

    private fun colorText(): Int = if (lightTheme)
        Color.rgb(30, 25, 40) else white

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 20, 18, 16)
            setBackgroundColor(colorBackground())
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(content)

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        heading.addView(TextView(this).apply {
            text = "THIAGO"
            textSize = 25f
            setTextColor(purple)
            setTypeface(null, 1)
        }, LinearLayout.LayoutParams(0, -2, 1f))

        heading.addView(makeButton("HISTORY") {
            showHistory()
        }, LinearLayout.LayoutParams(-2, 48))

        heading.addView(makeButton("☼") {
            lightTheme = !lightTheme
            buildScreen()
        }, LinearLayout.LayoutParams(55, 48))

        content.addView(heading)

        content.addView(TextView(this).apply {
            text = "CALCULATOR PRO"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(if (lightTheme) Color.GRAY else Color.LTGRAY)
        })

        val display = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.END
            setPadding(18, 20, 18, 20)
            setBackgroundColor(colorPanel())
        }

        expressionView = TextView(this).apply {
            text = expression
            textSize = 20f
            gravity = Gravity.END
            setTextColor(if (lightTheme) Color.DKGRAY else Color.LTGRAY)
            maxLines = 2
        }

        resultView = TextView(this).apply {
            text = formatter.format(answer)
            textSize = 38f
            gravity = Gravity.END
            setTextColor(purple)
            setTypeface(null, 1)
            setOnLongClickListener {
                copyResult()
                true
            }
        }

        display.addView(expressionView)
        display.addView(resultView)

        content.addView(display, LinearLayout.LayoutParams(
            -1, 175
        ).apply {
            setMargins(0, 22, 0, 18)
        })

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        modeRow.addView(makeButton(
            if (scientific) "SCIENTIFIC ON" else "SCIENTIFIC OFF"
        ) {
            scientific = !scientific
            buildScreen()
        }, LinearLayout.LayoutParams(0, 48, 1f))

        modeRow.addView(makeButton("⌫") {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
                updateDisplay()
            }
        }, LinearLayout.LayoutParams(65, 48))

        modeRow.addView(makeButton("AC") {
            expression = ""
            answer = 0.0
            updateDisplay()
        }, LinearLayout.LayoutParams(65, 48))

        content.addView(modeRow)

        keys = GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = false
        }

        val normalKeys = listOf(
            "MC", "MR", "M+", "M-",
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "−",
            "0", ".", "(", ")",
            "√", "x²", "^", "+",
            "sin", "cos", "tan", "log",
            "ln", "π", "e", "="
        )

        val simpleKeys = listOf(
            "MC", "MR", "M+", "M-",
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "−",
            "0", ".", "(", ")",
            "√", "x²", "^", "+",
            "C", "±", "%", "="
        )

        val buttonLabels = if (scientific) normalKeys else simpleKeys

        for (label in buttonLabels) {
            val button = makeButton(label) {
                handleKey(label)
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 59
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            keys.addView(button, params)
        }

        content.addView(keys)

        content.addView(TextView(this).apply {
            text = "Hold the result to copy it"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, 14, 0, 4)
        })

        root.addView(scroll, LinearLayout.LayoutParams(
            -1, 0, 1f
        ))

        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = if (label.length > 5) 11f else 17f
            isAllCaps = false
            setTextColor(
                if (label in listOf("=", "+", "−", "×", "÷", "^"))
                    Color.WHITE else colorText()
            )
            setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    if (label in listOf("=", "+", "−", "×", "÷", "^"))
                        purple else colorPanel()
                )
            )
            setPadding(2, 0, 2, 0)
            setOnClickListener { action() }
        }
    }

    private fun handleKey(key: String) {
        when (key) {
            "AC", "C" -> expression = ""
            "=" -> calculate()
            "MC" -> memory = 0.0
            "MR" -> expression += formatter.format(memory)
            "M+" -> memory += currentValue()
            "M-" -> memory -= currentValue()
            "√" -> expression += "sqrt("
            "x²" -> expression += "^2"
            "sin", "cos", "tan", "log", "ln" -> expression += "$key("
            "π" -> expression += "pi"
            "e" -> expression += "e"
            "±" -> expression = if (expression.startsWith("-"))
                expression.drop(1) else "-($expression)"
            "%" -> expression += "/100"
            "×" -> expression += "*"
            "÷" -> expression += "/"
            "−" -> expression += "-"
            else -> expression += key
        }
        updateDisplay()
    }

    private fun currentValue(): Double {
        return try {
            evaluate(expression)
        } catch (_: Exception) {
            answer
        }
    }

    private fun calculate() {
        try {
            val original = expression
            val value = evaluate(expression)
            if (!value.isFinite()) throw IllegalArgumentException()
            answer = value
            history.add("$original = ${formatter.format(value)}")
            if (history.size > 100) history.removeAt(0)
            expression = formatter.format(value)
            updateDisplay()
        } catch (_: Exception) {
            resultView.text = "Invalid expression"
        }
    }

    private fun updateDisplay() {
        expressionView.text = expression
        if (expression.isBlank()) {
            resultView.text = "0"
            return
        }

        try {
            val value = evaluate(expression)
            if (value.isFinite()) {
                resultView.text = formatter.format(value)
            } else {
                resultView.text = "Error"
            }
        } catch (_: Exception) {
            resultView.text = " "
        }
    }

    private fun copyResult() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Calculator result", resultView.text)
        )
        Toast.makeText(this, "Result copied!", Toast.LENGTH_SHORT).show()
    }

    private fun showHistory() {
        val options = arrayOf("View history", "Clear history")
        AlertDialog.Builder(this)
            .setTitle("Calculation History")
            .setItems(options) { _, which ->
                if (which == 0) {
                    AlertDialog.Builder(this)
                        .setTitle("History")
                        .setMessage(
                            if (history.isEmpty()) "No calculations yet"
                            else history.asReversed().joinToString("\n\n")
                        )
                        .setPositiveButton("Close", null)
                        .show()
                } else {
                    history.clear()
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun evaluate(input: String): Double {
        return ExpressionParser(input).parse()
    }

    private inner class ExpressionParser(private val source: String) {
        private var pos = 0

        fun parse(): Double {
            val value = expression()
            skipSpaces()
            if (pos != source.length) error("Unexpected input")
            return value
        }

        private fun skipSpaces() {
            while (pos < source.length && source[pos].isWhitespace()) pos++
        }

        private fun eat(c: Char): Boolean {
            skipSpaces()
            if (pos < source.length && source[pos] == c) {
                pos++
                return true
            }
            return false
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                value = when {
                    eat('+') -> value + term()
                    eat('-') -> value - term()
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = power()
            while (true) {
                value = when {
                    eat('*') -> value * power()
                    eat('/') -> value / power()
                    else -> return value
                }
            }
        }

        private fun power(): Double {
            var value = unary()
            if (eat('^')) value = value.pow(power())
            return value
        }

        private fun unary(): Double {
            if (eat('+')) return unary()
            if (eat('-')) return -unary()
            return primary()
        }

        private fun primary(): Double {
            skipSpaces()

            if (eat('(')) {
                val value = expression()
                if (!eat(')')) error("Missing )")
                return value
            }

            if (pos < source.length && source[pos].isDigit() ||
                pos < source.length && source[pos] == '.') {
                val start = pos
                while (pos < source.length &&
                    (source[pos].isDigit() || source[pos] == '.')) pos++
                return source.substring(start, pos).toDouble()
            }

            if (pos < source.length && source[pos].isLetter()) {
                val start = pos
                while (pos < source.length && source[pos].isLetter()) pos++
                val name = source.substring(start, pos).lowercase()

                return when (name) {
                    "pi" -> PI
                    "e" -> E
                    "sqrt" -> sqrt(primaryArgument())
                    "sin" -> sin(Math.toRadians(primaryArgument()))
                    "cos" -> cos(Math.toRadians(primaryArgument()))
                    "tan" -> tan(Math.toRadians(primaryArgument()))
                    "log" -> log10(primaryArgument())
                    "ln" -> ln(primaryArgument())
                    else -> error("Unknown function")
                }
            }

            error("Unexpected character")
        }

        private fun primaryArgument(): Double {
            return if (eat('(')) {
                val value = expression()
                if (!eat(')')) error("Missing )")
                value
            } else {
                primary()
            }
        }
    }
}
