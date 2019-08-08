package com.example.fotoshooting

import android.graphics.Color
import android.os.Build
import android.support.annotation.RequiresApi

public class Lamp constructor(var lampCaption:String = "", var color:Int = Color.RED) {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun toString(): String {
        return Color.valueOf(color).toString()
    }
}