package com.rooster.rooster

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment

class PopTime(val tgt: String) : DialogFragment(){


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        var myView = inflater!!.inflate(R.layout.pop_time,container,false)

        var pickTimeDone = myView.findViewById(R.id.pickTimeDone) as Button
        var tp1 = myView.findViewById(R.id.tp1) as TimePicker

        pickTimeDone.setOnClickListener {
            //Code here
            val sa= activity as SettingsActivity
            if(Build.VERSION.SDK_INT>=23) {
                sa.setTime(tp1.hour, tp1.minute, tgt)
            }else{
                sa.setTime(tp1.currentHour, tp1.currentMinute, tgt)
            }

            this.dismiss()
        }


        return myView
    }


}
