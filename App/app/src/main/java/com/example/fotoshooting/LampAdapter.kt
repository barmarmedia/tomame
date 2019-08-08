package com.example.fotoshooting

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.support.v4.content.ContextCompat.getSystemService
import android.view.LayoutInflater
import android.widget.Button
import java.security.AccessController.getContext


class LampAdapter(private val context : Context, private val dataSource:ArrayList<Lamp>) : BaseAdapter() {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        var lamp = getItem(position) as Lamp

        // Get title element
        val rowView = LayoutInflater.from(context)
            .inflate(R.layout.lamp_entry, parent, false)

        val titleTextView = rowView.findViewById(R.id.lampTitle) as TextView
        titleTextView.text = "Lampe "+position

        val captionTextView = rowView.findViewById(R.id.lampCaption) as TextView
        captionTextView.text = lamp.lampCaption

        rowView.findViewById<Button>(R.id.colorButton).setBackgroundColor(lamp.color)

        return rowView
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }
}