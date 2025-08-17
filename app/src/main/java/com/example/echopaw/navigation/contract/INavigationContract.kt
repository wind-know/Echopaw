package com.example.echopaw.navigation.contract

import com.example.echopaw.navigation.base.BaseView
import com.example.echopaw.navigation.model.LoadNavigationInfoCallBack
import com.example.echopaw.navigation.model.NavigationInfo


interface INavigationContract {
    interface INavigationModel<T> {
        fun execute(data: String?, callBack: LoadNavigationInfoCallBack<List<NavigationInfo>>?)
    }

    interface INavigationPresenter {
        fun getNavigationInfo(info: String?)
    }

    interface INavigationView : BaseView<INavigationPresenter?> {
        fun showNavigationInfomation(navigationInfos: List<NavigationInfo?>?)
        fun showError()
    }
}
