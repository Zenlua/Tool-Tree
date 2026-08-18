# Add-on
id=view_source
name="View source code"
author=Kakathic
description="Information and source code"
version=1.0
versionCode=100
root=false

# default
google_text="Currently using a translation tool"
view_source_text_1="Checking for updates..."
view_source_text_2="Update complete."
view_source_text_3="Checksum mismatch, please try reloading!"
view_source_text_4="Using the latest version."
view_source_text_5="Input file"
view_source_text_6="Import file"
view_source_text_7="Do not decompile source code"
view_source_text_8="Do not decode resources"
view_source_text_9="Options"
view_source_text_10="Please note that in most cases, jadx cannot decompile 100% of the source code, so errors will occur."
view_source_text_11="Dex to Java Decompiler"
view_source_text_12="Save to"
view_source_text_13="Decompilation error!"

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Xem mã nguồn"
    description="Thông tin và mã nguồn"
    view_source_text_1="Kiểm tra cập nhật..."
    view_source_text_2="Cập nhật xong."
    view_source_text_3="Không khớp sum, hãy thử tải lại !"
    view_source_text_4="Đang dùng phiên bản mới nhất."
    view_source_text_5="Tệp đầu vào"
    view_source_text_6="Nhập tệp tin"
    view_source_text_7="Không dịch ngược mã nguồn"
    view_source_text_8="Không giải mã tài nguyên"
    view_source_text_9="Tùy chọn"
    view_source_text_10="Xin lưu ý rằng trong hầu hết các trường hợp, jadx không thể dịch ngược toàn bộ 100% mã nguồn, do đó sẽ xảy ra lỗi."
    view_source_text_11="Trình dịch ngược Dex sang Java"
    view_source_text_12="Lưu ở"
    view_source_text_13="Lỗi dịch ngược !"
    ;;
  *)
    ;;
esac
