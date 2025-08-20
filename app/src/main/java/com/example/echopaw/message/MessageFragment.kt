// MessageFragment.java
package com.example.echopaw.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.echopaw.R
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

import java.util.List

class MessageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        val drawable = requireContext().getDrawable(R.drawable.divider)
        if (drawable != null) {
            dividerItemDecoration.setDrawable(drawable)
        }
        recyclerView.addItemDecoration(dividerItemDecoration)

        val dataList = List.of(
            Message(R.drawable.pic1, "2025.04.27 11:11", "中国•浙江省•杭州市", "#神秘"),
            Message(R.drawable.pic2, "2025.03.10 08:15", "中国•浙江省•杭州市", "#快乐"),
            Message(R.drawable.pic3, "2025.02.02 11:11", "中国•浙江省•杭州市", "#快乐"),
            Message(R.drawable.pic4, "2025.02.10 11:11", "中国•浙江省•杭州市", "#神秘"),
            Message(R.drawable.pic5, "2025.01.10 08:15", "中国•浙江省•杭州市", "#神秘"),
            Message(R.drawable.pic6, "2025.12.27 11:11", "中国•浙江省•杭州市", "#期待"),
            Message(R.drawable.pic1, "2025.11.20 12:11", "中国•浙江省•杭州市", "#神秘"),
            Message(R.drawable.pic2, "2025.05.10 08:14", "中国•浙江省•杭州市", "#期待"),
            Message(R.drawable.pic3, "2025.02.18 09:29", "中国•浙江省•杭州市", "#神秘"),
            Message(R.drawable.pic4, "2025.07.01 08:21", "中国•浙江省•杭州市", "#快乐"),
            Message(R.drawable.pic5, "2025.11.22 15:30", "中国•浙江省•杭州市", "#神秘")
        )
        recyclerView.adapter = RecyclerAdapter(dataList)
        OverScrollDecoratorHelper.setUpOverScroll(
            recyclerView,
            OverScrollDecoratorHelper.ORIENTATION_VERTICAL
        )

        return view
    }
}