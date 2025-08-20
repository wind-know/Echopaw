package com.example.echopaw.message

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.echopaw.R
import com.example.echopaw.message.RecyclerAdapter.MyViewHolder

class RecyclerAdapter(private val dataList: List<Message>?) :
    RecyclerView.Adapter<MyViewHolder>() {
    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var ivPicture: ImageView =
            view.findViewById(R.id.iv_picture)
        var timeTextView: TextView = view.findViewById(R.id.tv_time)
        var locationTextView: TextView =
            view.findViewById(R.id.tv_location)
        var tagTextView: TextView = view.findViewById(R.id.tv_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = dataList!![position]

        holder.ivPicture.setImageResource(message.imageId)
        holder.timeTextView.text = message.time
        holder.locationTextView.text = message.location
        holder.tagTextView.text = message.tag

        val bgDrawable = holder.tagTextView.background.mutate()
        if (bgDrawable is GradientDrawable) {
            val colorResId = when (message.tag) {
                "#神秘" -> R.color.tag_mystery
                "#快乐" -> R.color.tag_happy
                "#期待" -> R.color.tag_expect
                else -> R.color.tag_default
            }

            val color = ContextCompat.getColor(holder.itemView.context, colorResId)
            bgDrawable.setColor(color)
        }
    }

    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }
}
