package com.tool.tree.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragments = mutableListOf<Fragment>()
    private val titles = mutableListOf<String>()
    private val ids = mutableListOf<Long>()

    private var nextId = 0L

    /**
     * Thêm tab mới
     */
    fun addFragment(fragment: Fragment, title: String) {
        fragments.add(fragment)
        titles.add(title)
        ids.add(nextId++) // đảm bảo ID không trùng
    }

    /**
     * Tổng số tab
     */
    override fun getItemCount(): Int = fragments.size

    /**
     * Tạo Fragment theo vị trí
     */
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    /**
     * Lấy title tab
     */
    fun getTitle(position: Int): String {
        return titles[position]
    }

    /**
     * Lấy fragment hiện tại (để updateData)
     */
    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }

    /**
     * Tìm vị trí tab theo title
     */
    fun getTitleIndex(title: String): Int {
        return titles.indexOf(title)
    }

    /**
     * ID ổn định cho ViewPager2 (tránh recreate fragment)
     */
    override fun getItemId(position: Int): Long {
        return ids[position]
    }

    /**
     * Kiểm tra fragment còn tồn tại
     */
    override fun containsItem(itemId: Long): Boolean {
        return ids.contains(itemId)
    }

    /**
     * Xoá toàn bộ tab (nếu cần reload lại toàn bộ)
     */
    fun clear() {
        fragments.clear()
        titles.clear()
        ids.clear()
        nextId = 0L
    }
}