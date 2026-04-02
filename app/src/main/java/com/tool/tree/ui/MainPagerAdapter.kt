package com.tool.tree.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragments = mutableListOf<Fragment>()
    private val titles = mutableListOf<String>()

    /**
     * Thêm 1 tab
     */
    fun addFragment(fragment: Fragment, title: String) {
        fragments.add(fragment)
        titles.add(title)
    }

    /**
     * Tổng số tab
     */
    override fun getItemCount(): Int {
        return fragments.size
    }

    /**
     * Trả về Fragment theo vị trí
     */
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    /**
     * Lấy title cho TabLayout
     */
    fun getTitle(position: Int): String {
        return titles[position]
    }

    /**
     * (Optional) Lấy fragment nếu cần xử lý ngoài
     */
    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }

    /**
     * (Optional) Xoá toàn bộ tab (dùng khi reload)
     */
    fun clear() {
        fragments.clear()
        titles.clear()
    }
}