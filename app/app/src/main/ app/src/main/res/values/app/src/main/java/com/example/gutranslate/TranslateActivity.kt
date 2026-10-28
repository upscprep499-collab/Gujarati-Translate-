package com.example.gutranslate

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class TranslateActivity : Activity() {

    private lateinit var wordView: TextView
    private lateinit var resultView: TextView
    private lateinit var altView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        wordView = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }
        resultView = TextView(this).apply {
            textSize = 24f
            setPadding(0, pad / 2, 0, pad / 2)
            setTextIsSelectable(true)
        }
        altView = TextView(this).apply { textSize = 14f }
        root.addView(wordView)
        root.addView(resultView)
        root.addView(altView)

        val selected = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()?.trim()

        if (intent.action == Intent.ACTION_PROCESS_TEXT && !selected.isNullOrEmpty()) {
            translate(selected)
        } else {
            val input = EditText(this).apply { hint = "English word lakho" }
            val button = Button(this).apply { text = "Translate" }
            button.setOnClickListener {
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) translate(t)
            }
            val help = TextView(this).apply {
                text = "Koi pan app ma (PDF, ebook, Chrome) English word select karo, " +
                        "pachhi menu ma \"Gujarati ma jovo\" par tap karo."
                textSize = 13f
                visibility = View.VISIBLE
            }
            root.addView(input, 0)
            root.addView(button, 1)
            root.addView(help)
        }
        setContentView(root)
    }

    private fun translate(text: String) {
        wordView.text = text
        resultView.text = "Translating…"
        altView.text = ""
        thread {
            try {
                val url = URL(
                    "https://translate.googleapis.com/translate_a/single" +
                            "?client=gtx&sl=en&tl=gu&dt=t&dt=bd&q=" +
                            URLEncoder.encode(text, "UTF-8")
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(body)

                val sentences = arr.getJSONArray(0)
                val main = StringBuilder()
                for (i in 0 until sentences.length()) {
                    main.append(sentences.getJSONArray(i).getString(0))
                }

                val alts = StringBuilder()
                if (!arr.isNull(1)) {
                    val groups = arr.getJSONArray(1)
                    for (i in 0 until groups.length()) {
                        val g = groups.getJSONArray(i)
                        val words = g.getJSONArray(1)
                        val list = (0 until minOf(words.length(), 5))
                            .joinToString(", ") { words.getString(it) }
                        alts.append(g.getString(0)).append(": ").append(list).append("\n")
                    }
                }
                runOnUiThread {
                    resultView.text = main.toString()
                    altView.text = alts.toString().trim()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    resultView.text = "Translation na thai shaki. Internet check karo."
                }
            }
        }
    }
}
