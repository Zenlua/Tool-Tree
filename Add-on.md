
Định dạng tệp add-on:

- Là một file nén dạng .zip hoặc .7z

- Sau khi nén xong, đổi tên đuôi thành file.add

Cấu trúc bên trong tệp add-on:

```
file.add
└── (nội dung bên trong tệp)
    ├── addon.prop
    ├── icon.png (256×256)
    ├── menu.sh|menu.xml
    ├── index.sh|index.xml
    └── addon/
```

Nội dung tệp addon.prop

```
id=test
name=Thử nghiệm
author=Kakathic
description=Thử nghiệm add-on
version=1.0
versionCode=100
web=https://github.com/Zenlua/Add-on
```

Hình ảnh icon

- icon.png là tệp hình ảnh có thể 100x100 ~ 500x500

Nội dung bên trong menu.sh, menu.xml

- [Xem chi tiết](https://github.com/helloklf/kr-scripts/blob/master/docs/Page.md)

- Chủ yếu dùng page để chuyển vào index

Nội dung bên trong index.sh, index.xml

- [Xem chi tiết](https://github.com/helloklf/kr-scripts)

- Rất nhiều thứ khó nói chỉ có thể tự tìm hiểu




