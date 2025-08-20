package com.example.echopaw.navigation.model

import com.example.echopaw.callback.MapFragment
import com.example.echopaw.home.HomeFragment
import com.example.echopaw.message.MessageFragment
import com.example.echopaw.mine.MineFragment
import com.example.echopaw.navigation.contract.INavigationContract.INavigationModel

class NavigationModel : INavigationModel<String> {
    override fun execute(data: String?, callBack: LoadNavigationInfoCallBack<List<NavigationInfo>>?) {
        val list = mutableListOf<NavigationInfo>()

        val homeFragment = HomeFragment()
        list.add(NavigationInfo("首页", homeFragment))

        val callbackFragment = MapFragment()
        list.add(NavigationInfo("回应", callbackFragment))
//
        val messageFragment = MessageFragment()
        list.add(NavigationInfo("消息", messageFragment))

        val mineFragment = MineFragment()
        list.add(NavigationInfo("我的", mineFragment))

        callBack?.onSuccess(list)
    }
}
