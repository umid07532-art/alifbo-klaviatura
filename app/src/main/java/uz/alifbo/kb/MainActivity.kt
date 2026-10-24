package uz.alifbo.kb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 20).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "1) Klaviaturani yoqing\n2) Uni tanlang\n3) Pastdagi maydonda sinab ko'ring: o', g', sh, ch"
            textSize = 16f
        })
        root.addView(Button(this).apply {
            text = "1) Klaviaturani yoqish"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "2) Klaviaturani tanlash"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })
        root.addView(EditText(this).apply { hint = "Shu yerda sinab ko'ring" })
        setContentView(root)
    }
}
