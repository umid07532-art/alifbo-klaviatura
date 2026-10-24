package uz.alifbo.kb

import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout

class AlifboIME : InputMethodService() {

    private var shift = false
    private var lastConv: Pair<String, String>? = null // (almashgan, asl)
    private val letterButtons = mutableListOf<Button>()

    private fun dp(v: Int) = (resources.displayMetrics.density * v).toInt()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shift = false
        lastConv = null
        refresh()
    }

    override fun onCreateInputView(): View {
        letterButtons.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF20262B.toInt())
            setPadding(dp(3), dp(6), dp(3), dp(6))
        }

        // Tez harflar qatori
        root.addView(row().apply {
            listOf("Ö", "Ğ", "Ş", "Ç").forEach { ch -> addView(key(ch) { put(ch) }) }
        })
        // Raqamlar
        root.addView(row().apply {
            "1234567890".forEach { d -> addView(key(d.toString()) { put(d.toString()) }) }
        })
        // Harflar
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        rows.forEachIndexed { i, r ->
            root.addView(row().apply {
                if (i == 2) addView(key("⇧", 1.5f) { shift = !shift; refresh() })
                r.forEach { c ->
                    val b = key(c.toString()) { typeLetter(c) }
                    letterButtons.add(b)
                    addView(b)
                }
                if (i == 2) addView(key("⌫", 1.5f) { backspace() })
            })
        }
        // Pastki qator
        root.addView(row().apply {
            addView(key("'") { typeApostrophe() })
            addView(key(",") { put(",") })
            addView(key("␣", 5f) { put(" ") })
            addView(key(".") { put(".") })
            addView(key("⏎", 1.5f) { enter() })
        })
        return root
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        gravity = Gravity.CENTER
    }

    private fun key(label: String, weight: Float = 1f, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 18f
        isAllCaps = false
        setTextColor(0xFFE8EBEE.toInt())
        setBackgroundColor(0xFF343C43.toInt())
        layoutParams = LinearLayout.LayoutParams(0, dp(46), weight).apply {
            setMargins(dp(2), dp(3), dp(2), dp(3))
        }
        setOnClickListener { action() }
    }

    private fun refresh() {
        letterButtons.forEach { b ->
            val t = b.text.toString()
            b.text = if (shift) t.uppercase() else t.lowercase()
        }
    }

    private fun put(s: String) {
        lastConv = null
        currentInputConnection?.commitText(s, 1)
    }

    private fun typeLetter(c: Char) {
        val ic = currentInputConnection ?: return
        val ch = if (shift) c.uppercaseChar() else c
        if (shift) { shift = false; refresh() }

        // s+h -> ş, c+h -> ç
        if (ch.lowercaseChar() == 'h') {
            val prev = ic.getTextBeforeCursor(1, 0)?.toString()
            val conv = when (prev) {
                "s" -> "ş"; "S" -> "Ş"; "c" -> "ç"; "C" -> "Ç"; else -> null
            }
            if (conv != null) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(conv, 1)
                lastConv = conv to (prev + ch)
                return
            }
        }
        lastConv = null
        ic.commitText(ch.toString(), 1)
    }

    private fun typeApostrophe() {
        val ic = currentInputConnection ?: return
        val prev = ic.getTextBeforeCursor(1, 0)?.toString()
        val conv = when (prev) {
            "o" -> "ö"; "O" -> "Ö"; "g" -> "ğ"; "G" -> "Ğ"; else -> null
        }
        if (conv != null) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(conv, 1)
            lastConv = conv to (prev + "'")
        } else {
            lastConv = null
            ic.commitText("’", 1) // tutuq belgisi
        }
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        val lc = lastConv
        // Almashgan harfdan keyin ⌫ -> asl holatiga qaytaradi (masalan ş -> sh)
        if (lc != null && ic.getTextBeforeCursor(1, 0)?.toString() == lc.first) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(lc.second, 1)
            lastConv = null
            return
        }
        lastConv = null
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    private fun enter() {
        lastConv = null
        val info = currentInputEditorInfo
        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noAction = ((info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }
}
