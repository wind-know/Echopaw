package com.example.echopaw.navigation.model

interface LoadNavigationInfoCallBack<T> {
    fun onSuccess(t: T)
    fun onStart()
    fun onFailed()
    fun onFinish()
}
