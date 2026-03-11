# Kakathic

if [ "$LANGUAGE-$COUNTRY" == "vi-VN" ];then
# Group / title
TITLE_CHANGE_PROJECT="Thay đổi dự án"
SUMMARY_CURRENT="Hiện tại"
NOTE_PATCH="Lưu ý: Sau khi thực hiện các tính năng được bật sẽ được áp dụng vào dự án và không thể tắt tính năng đó nữa, giải mã lại toàn bộ phân vùng nếu muốn thay đổi các tính năng khác"

# Action titles
TITLE_FAST_PATCH="Vá nhanh cơ bản"
TITLE_REMOVE_APP="Xoá app rác"
TITLE_KEYBOARD="Tùy chỉnh bàn phím"
TITLE_SYSTEM_PATCH="Bản vá hệ thống"
TITLE_DEX2OAT="Dex2oat toàn bộ"

# Labels
LABEL_SELECT="Lựa chọn"
LABEL_DELETE_APP="Xoá app rác"

# Prop
TITLE_PROP="Thay đổi ro.control_privapp_permissions"
DESC_PROP="enforce: nếu thiếu quyền có thể bị bootloop, log: thông báo lỗi vào logcat và không cấp quyền, disable: tự động cấp quyền còn thiếu cho app"

# Basic patch labels
LABEL_DISABLE_OTA="Tắt update ota"
LABEL_CRYPTO="Thêm ro.crypto.state=encrypted"
LABEL_RW_ROM="Vá RW rom erofs"
LABEL_HOME_POCO="Thay đổi HOME poco"
DESC_HOME_POCO="Lưu ý chỉ thay đổi: com.mi.android.globallauncher thành com.miui.home ở prop, apk cần thay đổi thủ công"

# Keyboard
DESC_IME_APP="Ứng dụng bàn phím"
DESC_COLOR_LIGHT="Mã màu nền sáng"
DESC_COLOR_DARK="Mã màu nền tối"
DESC_DIMEN="Điều chỉnh dimen chiều rộng"

# System patch
LABEL_FIX_NOTI="Sửa lỗi thông báo chậm CN"
LABEL_FIX_FPS="Mở khóa fps Max"
LABEL_FIX_WINDOW="Tối đa 6 cửa sổ nhỏ"
LABEL_FIX_THEME="Sửa lỗi reset theme"
LABEL_FIX_GLOBAL="Tính năng global rom CN"
LABEL_FIX_ERROR="Xoá hộp thoại lỗi vân tay khi khởi động"
LABEL_FIX_IME="Bàn phím nâng cao"
LABEL_FIX_FWKO="Thêm Kaorios Toolbox"
LABEL_FIX_SCREEN="Mở khóa giới hạn chụp ảnh màn hình"
LABEL_FIX_APKSIGN="Bỏ qua xác minh chữ ký"
LABEL_FIX_APPVAULT="Bẻ khóa giao dịch Appvault"
LABEL_FIX_THEMES="Bẻ khóa giao dịch Theme"
LABEL_FIX_WEATHER="Hiện aqi ở thời tiết CN"
LABEL_FIX_JOYOSE="Mod ứng dụng Joyose"
LABEL_FIX_MAP="Xoá map china ở thư viện CN"

# Dex2oat
LABEL_OAT_FW="Tạo oat framework, service"
DESC_SECONTEXT="Nhập đường dẫn lớp cho ứng dụng. Một số ứng dụng yêu cầu đường dẫn lớp; bạn có thể kiểm tra tệp odex cũ để xem đường dẫn lớp"

# Shell
NOT_FOUND_TEXT="Không tìm thấy"
NO_VALUE_TEXT="Không có giá trị nào !"
CREATING_OAT_TEXT="Đang tạo oat"
SEARCHING_AND_DELETING="Đang tìm kiếm và xoá..."
RW_ROM_TEXT_1="Cảnh báo: Không tìm thấy vendor_boot, nếu nó đã được vá rw có thể bỏ qua !"
RW_ROM_TEXT_2="Cảnh báo: Không tìm thấy mi_ext, không copy được file có thể lỗi tính năng, nếu đã được copy có thể bỏ qua !"
RW_ROM_TEXT_3="Cảnh báo: Không tìm thấy phân vùng chứa ext4 cần phải thêm thủ công vào:"
patch_text="Đã vá"
patch_text1="Đang vá"
fi
