package com.tool.tree.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * SwipeRefreshLayout mặc định chỉ kiểm tra CHÍNH view con trực tiếp (mTarget) để quyết định
 * child có đang cuộn được lên hay không (canChildScrollUp) - dùng để biết có nên bắt đầu kéo
 * hiệu ứng refresh hay để nguyên cho view con tự xử lý cuộn.
 *
 * Ở ActionPage, view con trực tiếp của layout này là 1 FrameLayout (main_list) chứa
 * ActionListFragment - mà nội dung thật sự cuộn được (ScrollView id=kr_content) lại nằm SÂU
 * bên trong fragment, không phải là view con trực tiếp. FrameLayout không tự có khả năng cuộn
 * nên canChildScrollUp() mặc định luôn trả về false -> kéo xuống bất kỳ lúc nào (kể cả khi
 * đang cuộn dở danh sách) đều bị hiểu nhầm là "đã ở đầu trang" và kích hoạt refresh, làm hỏng
 * thao tác cuộn bình thường.
 *
 * Lớp này duyệt xuống cây view để tìm ra view thật sự đang cuộn được (hiện tại là ScrollView
 * kr_content của ActionListFragment; ListView/GridView cũng được hỗ trợ sẵn) rồi hỏi thẳng view
 * đó xem có đang cuộn lên được không.
 */
class PullRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    override fun canChildScrollUp(): Boolean {
        val scrollableChild = findScrollableView(this) ?: return super.canChildScrollUp()
        return ViewCompat.canScrollVertically(scrollableChild, -1)
    }

    private fun findScrollableView(root: View): View? {
        if (root !== this && (root is android.widget.ScrollView || root is android.widget.AbsListView)) {
            return root
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findScrollableView(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
}
