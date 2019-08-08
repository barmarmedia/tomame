package com.example.fotoshooting

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class LampEntry @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes), View.OnClickListener {

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.lamp_entry, this, true)

        orientation = HORIZONTAL

        var a : TypedArray = context.obtainStyledAttributes(attrs,
            R.styleable.LampEntry, 0, 0)

        var titleText:String = a.getString(R.styleable.LampEntry_titleText)
        a.recycle()

        var titleTextView = findViewById<TextView>(R.id.lampTitle)
        titleTextView.text = titleText

        var captionTextView = findViewById<TextView>(R.id.lampCaption)
        captionTextView.text = a.getString(R.styleable.LampEntry_captionText)
        a.recycle()

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.colorButton -> {
                Log.d("adsf", "asdf")
            }
        }
    }
}