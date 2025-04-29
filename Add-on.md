
Định dạng tệp add-on:

- Là một file nén dạng .zip hoặc .7z

- Sau khi nén xong, đổi phần mở rộng thành .add


Cấu trúc bên trong tệp add-on:

```
file.add
└── (nội dung sau khi giải nén)
    ├── addon.prop  # Thông tin mô tả add-on (ví dụ: tên, phiên bản, tác giả...)
    ├── icon.png  # Icon đại diện cho add-on (hiển thị trong giao diện người dùng)
    ├── menu.sh|menu.xml  # Script để hiển thị
    ├── index.sh|index.xml  # Là tệp con của menu chỉ tới 
    └── addon/  # Thư mục chứa các tệp hỗ trợ hoặc nội dung chính của add-on
```
