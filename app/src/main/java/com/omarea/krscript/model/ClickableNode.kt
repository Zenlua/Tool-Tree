package com.omarea.krscript.model

open class ClickableNode(currentPageConfigPath: String) : NodeInfoBase(currentPageConfigPath) {
    // 功能图标路径（列表中）
    var iconPath = ""
    // Nếu > 0: icon là hoạt ảnh (gif-style), số khung hình cần nạp (icon_1.png, icon_2.png, ...)
    var iconGifNum: Int = 0
    // Thời gian hiển thị mỗi khung hình (mili giây)
    var iconGifTime: Int = 300
    // true (mặc định): tự chạy hoạt ảnh; false: chỉ hiện khung đầu, bấm vào icon để phát/tạm dừng
    var iconGifAutoplay: Boolean = true
    // Số vòng lặp tối đa (<=0: lặp vô hạn, mặc định)
    var iconGifLoopCount: Int = 0

    // 功能图标路径（桌面快捷）
    var logoPath = ""
    var photoPath = ""
    // Nếu true: hiện ảnh (photoPath) đúng kích thước thật, căn giữa, không kéo dãn full chiều ngang
    var photoRealSize: Boolean = false
    // Nếu > 0: photo là hoạt ảnh (gif-style), số khung hình cần nạp (photo_1.png, photo_2.png, ...)
    var photoGifNum: Int = 0
    // Thời gian hiển thị mỗi khung hình (mili giây)
    var photoGifTime: Int = 300
    // true (mặc định): tự chạy hoạt ảnh; false: chỉ hiện khung đầu, bấm vào ảnh để phát/tạm dừng
    var photoGifAutoplay: Boolean = true
    // Số vòng lặp tối đa (<=0: lặp vô hạn, mặc định)
    var photoGifLoopCount: Int = 0
    var bgPath = ""

    // 是否允许添加快捷方式（非false，且具有key则默认允许）
    var allowShortcut:Boolean? = null

    // 是否锁定
    var locked: Boolean = false
    // 锁定状态获取（脚本）
    var lockShell: String = ""

    // 此功能的Android SDK版本要求
    var targetSdkVersion = 0
    var minSdkVersion = 0
    var maxSdkVersion = 100
}
