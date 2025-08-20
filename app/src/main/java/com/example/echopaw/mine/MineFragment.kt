package com.example.echopaw.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.echopaw.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.List

class MineFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mine, container, false)

        val fragmentList = List.of<Fragment>(
            ViewPagerFragment(),
            ViewPagerFragment(),
            ViewPagerFragment(),
            ViewPagerFragment(),
            ViewPagerFragment()
        )

        val adapter = PagerAdapter(
            requireActivity(),
            fragmentList
        )
        val viewPager2 = view.findViewById<ViewPager2>(R.id.viewPager2)
        viewPager2.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        TabLayoutMediator(
            tabLayout, viewPager2
        ) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.setText("全部记录")
                1 -> tab.setText("时间线")
                2 -> tab.setText("地图视图")
                3 -> tab.setText("我的收藏")
                4 -> tab.setText("回应记录")
                else -> tab.setText("未知")
            }
        }.attach()

        return view
    }
}