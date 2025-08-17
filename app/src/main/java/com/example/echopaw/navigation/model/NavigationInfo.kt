package com.example.echopaw.navigation.model

import androidx.fragment.app.Fragment
import java.util.Objects

class NavigationInfo(var fragmentName: String, var fragment: Fragment) {
    override fun toString(): String {
        return "NavigationInfo{" +
                "fragmentName='" + fragmentName + '\'' +
                ", fragment=" + fragment +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as NavigationInfo
        return fragmentName == that.fragmentName && fragment == that.fragment
    }

    override fun hashCode(): Int {
        return Objects.hash(fragmentName, fragment)
    }
}
