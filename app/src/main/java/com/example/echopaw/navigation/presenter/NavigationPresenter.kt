package com.example.echopaw.navigation.presenter

import com.example.echopaw.navigation.contract.INavigationContract.INavigationModel
import com.example.echopaw.navigation.contract.INavigationContract.INavigationPresenter
import com.example.echopaw.navigation.contract.INavigationContract.INavigationView
import com.example.echopaw.navigation.model.LoadNavigationInfoCallBack
import com.example.echopaw.navigation.model.NavigationInfo


class NavigationPresenter(
    private val model: INavigationModel<String>,
    private val view: INavigationView
) : INavigationPresenter,
    LoadNavigationInfoCallBack<List<NavigationInfo>> {

    override fun getNavigationInfo(info: String?) {
        // 如果 info 可能为 null，可以在这里做默认值处理
        model.execute(info ?: "", this)
    }

    override fun onSuccess(navigationInfos: List<NavigationInfo>) {
        view.showNavigationInfomation(navigationInfos)
    }

    override fun onStart() {
        // 可选：在开始加载时做 UI 提示
    }

    override fun onFailed() {
        // 可选：失败时的 UI 处理
    }

    override fun onFinish() {
        // 可选：结束时的收尾操作
    }
}
