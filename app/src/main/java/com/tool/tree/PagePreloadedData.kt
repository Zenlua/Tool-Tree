package com.tool.tree

import com.omarea.krscript.model.ActionNode
import com.omarea.krscript.model.NodeInfoBase
import com.omarea.krscript.model.PageMenuOption
import java.io.Serializable

/**
 * Kết quả build 1 trang (items + [[menu]]/[[fab]]) đã được TIỀN TẢI ("preload") NGOÀI trang
 * cha, TRƯỚC khi mở Activity mới - xem OpenPageHelper.openPage()/preloadThenOpen().
 *
 * CHỈ áp dụng cho mục process = false (trang build 1 lần rồi hiện trọn vẹn, không hiện dần
 * từng mục). Với process = true (progressive), OpenPageHelper vẫn mở trang NGAY như cũ và để
 * ActionPage tự tải + hiện dần bên trong - đúng mục đích của progressive load nên KHÔNG gắn
 * PagePreloadedData cho trường hợp này.
 *
 * Gắn kèm vào Intent mở ActionPage qua extra "preloadedItems". ActionPage.onCreate() đọc thấy
 * extra này thì bỏ qua hẳn checkPageLockThenLoad()/loadPageConfig() (đã chạy xong ở
 * OpenPageHelper rồi, kể cả bước kiểm tra khoá + beforeRead/afterRead/loadSuccess), chỉ còn
 * việc hiện items ra danh sách ngay lập tức - xem ActionPage.applyPreloadedData().
 */
class PagePreloadedData(
    val items: ArrayList<NodeInfoBase>,
    val menuOptions: ArrayList<PageMenuOption>?,
    val headerActions: ArrayList<ActionNode>?,
    val autoShowActions: ArrayList<ActionNode>?
) : Serializable
