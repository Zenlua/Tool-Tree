package com.tool.tree.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.omarea.krscript.ui.ActionListFragment

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragmentList = ArrayList<ActionListFragment>()
    private val fragmentTitles = ArrayList<String>()

    // Thêm Fragment mới vào danh sách
    fun addFragment(fragment: ActionListFragment, title: String) {
        fragmentList.add(fragment)
        fragmentTitles.add(title)
        notifyItemInserted(fragmentList.size - 1)
    }

    // Thay thế Fragment tại vị trí cụ thể
    fun replaceFragment(position: Int, fragment: ActionListFragment) {
        if (position in 0 until fragmentList.size) {
            fragmentList[position] = fragment
            // Sử dụng notifyItemChanged để ViewPager2 biết cần nạp lại Fragment tại vị trí này
            notifyItemChanged(position)
        }
    }

    // Lấy Fragment tại vị trí
    fun getFragment(position: Int): ActionListFragment? =
        fragmentList.getOrNull(position)

    // Lấy tiêu đề tab
    fun getTitle(position: Int): String =
        fragmentTitles.getOrElse(position) { "" }

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment {
        // Trả về Fragment từ danh sách
        return fragmentList[position]
    }

    // Để replaceFragment hoạt động chính xác với ViewPager2, bạn nên override thêm hàm này
    override fun getItemId(position: Int): Long {
        // Tạo một ID duy nhất cho mỗi instance của Fragment dựa trên hashCode
        return fragmentList[position].hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return fragmentList.any { it.hashCode().toLong() == itemId }
    }
}
