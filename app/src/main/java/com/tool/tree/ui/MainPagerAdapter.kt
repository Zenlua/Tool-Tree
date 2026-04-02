package com.tool.tree.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragments = mutableListOf<Fragment>()
    private val titles = mutableListOf<String>()

    fun addFragment(fragment: Fragment, title: String) {
        fragments.add(fragment)
        titles.add(title)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun getTitle(position: Int): String {
        return titles[position]
    }

    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun getTitleIndex(title: String): Int {
        return titles.indexOf(title)
    }

    fun clear() {
        fragments.clear()
        titles.clear()
    }
}