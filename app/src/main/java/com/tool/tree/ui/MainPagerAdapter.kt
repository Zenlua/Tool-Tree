package com.tool.tree.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragments = ArrayList<Fragment>()
    private val titles = ArrayList<String>()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun addFragment(fragment: Fragment, title: String) {
        fragments.add(fragment)
        titles.add(title)
        notifyItemInserted(fragments.size - 1)
    }

    fun updateFragment(position: Int, fragment: Fragment) {
        if (position in fragments.indices) {
            fragments[position] = fragment
            notifyItemChanged(position)
        }
    }

    fun getFragment(position: Int): Fragment? {
        return fragments.getOrNull(position)
    }

    fun getTitle(position: Int): String = titles.getOrElse(position) { "" }
}