package com.tool.tree.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.omarea.krscript.ui.ActionListFragment

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragmentList = ArrayList<ActionListFragment>()
    private val fragmentTitles = ArrayList<String>()
    // Sử dụng ID ổn định thay vì hashCode của Fragment
    private val fragmentIds = ArrayList<Long>()
    private var nextId = 0L

    fun addFragment(fragment: ActionListFragment, title: String) {
        fragmentList.add(fragment)
        fragmentTitles.add(title)
        fragmentIds.add(nextId++)
        notifyItemInserted(fragmentList.size - 1)
    }

    fun replaceFragment(position: Int, fragment: ActionListFragment) {
        if (position in 0 until fragmentList.size) {
            fragmentList[position] = fragment
            // Khi thay thế, ta cấp một ID mới cho vị trí này để ViewPager2 tạo lại UI
            fragmentIds[position] = nextId++
            notifyItemChanged(position)
        }
    }

    fun getFragment(position: Int): ActionListFragment? = fragmentList.getOrNull(position)

    fun getTitle(position: Int): String = fragmentTitles.getOrElse(position) { "" }

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]

    override fun getItemId(position: Int): Long = fragmentIds[position]

    override fun containsItem(itemId: Long): Boolean = fragmentIds.contains(itemId)
}
