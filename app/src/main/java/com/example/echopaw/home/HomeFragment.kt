package com.example.echopaw.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.echopaw.R


class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val buttonOpenRecord = view.findViewById<Button>(R.id.btn_open_record)
        buttonOpenRecord.setOnClickListener {
            // 跳转到 RecordActivity
            val intent = Intent(requireContext(), RecordActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
