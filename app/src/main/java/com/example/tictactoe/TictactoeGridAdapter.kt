package com.example.tictactoe

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class TictactoeGridAdapter(
    private val ctx: Context,
    private val labels:List<String>,
    private val clickable:Boolean,
    ): BaseAdapter() {
    private var inflater:LayoutInflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var fields:Array<View?> = arrayOfNulls(labels.size)


    override fun getCount(): Int {
        return labels.size
    }

    override fun getItem(p0: Int): Any {
        return 0
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        if (fields[p0] == null){
            fields[p0] = inflater.inflate(R.layout.tictactoe_field, null)
        }
        val textView:TextView = fields[p0]!!.findViewById<TextView>(R.id.textView)
        textView.text = labels[p0]
        textView.isClickable = !clickable
        return fields[p0]!!
    }
    private fun setClickabilityOfAll(clickable:Boolean){
        for (view:View? in fields){
            val textView:TextView? = view?.findViewById<TextView>(R.id.textView)
            textView?.isClickable = !clickable
        }
    }
    fun makeAllClickable(){
        setClickabilityOfAll(true)
    }
    fun makeAllUnclickable(){
        setClickabilityOfAll(false)
    }
}