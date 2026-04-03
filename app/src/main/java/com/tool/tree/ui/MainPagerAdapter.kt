package com.tool.tree.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragmentList = ArrayList<ActionListFragment>()
    private val fragmentTitles = ArrayList<String>()

    fun addFragment(fragment: ActionListFragment, title: String) {
        fragmentList.add(fragment)
        fragmentTitles.add(title)
        notifyDataSetChanged()
    }

    fun updateFragment(position: Int, fragment: ActionListFragment) {
        if (position < fragmentList.size) {
            fragmentList[position] = fragment
            notifyItemChanged(position)
        }
    }

    fun getFragment(position: Int): ActionListFragment? =
        fragmentList.getOrNull(position)

    fun getTitle(position: Int): String =
        fragmentTitles.getOrElse(position) { "" }

    override fun getItemCount(): Int = fragmentList.size
    override fun createFragment(position: Int): androidx.fragment.app.Fragment =
        fragmentList[position]
}