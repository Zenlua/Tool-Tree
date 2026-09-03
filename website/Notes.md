# Notes - Đổi cấu trúc load TOML (bỏ dot-notation, thêm marker [[toml]])

## Thay đổi
1. **PageConfigSh.kt** — `looksLikeInlineToml()`: nhận diện inline TOML qua marker
   `[[toml]]` **hoặc** `[[group]]` ở dòng 1/2 (giữ `[[group]]` như cũ, thêm `[[toml]]` làm
   marker tuỳ chọn cho trường hợp nội dung không mở đầu bằng group, vd chỉ có `[[action]]`
   đứng lẻ ở gốc).
2. **PageConfigReader.kt**:
   - `tomlNodeTypeOrder`: giữ nguyên `"group"` (KHÔNG đổi thành "toml").
   - `tomlChildren()`: viết lại. Trước đây con của group dùng TOML dot-notation
     (`[[group.action]]` = bảng lồng thật trong `[[group]]`), đọc bằng đệ quy. Giờ tất cả
     loại con (`action`, `text`, `switch`, `picker`, `page`, `download`, `editor`, `resource`,
     `menu`, `fab`) là **mảng phẳng ở gốc tài liệu** — gán vào `[[group]]` gần nhất phía trước
     theo **thứ tự dòng**, không còn lồng dấu chấm. Mục đứng trước `[[group]]` đầu tiên (nếu
     có) được thêm thẳng vào kết quả, không thuộc group nào.
   - `tomlBuildNode()`: bỏ nhánh `"group"` (đã xử lý riêng trong `tomlChildren()`), các nhánh
     còn lại giữ nguyên logic cũ 100%.
   - `"toml"` KHÔNG phải type được parser xử lý — chỉ là marker nhận diện ở PageConfigSh.kt;
     nếu vô tình xuất hiện trong tài liệu, parser bỏ qua vô hại.

## Cú pháp TOML mới
```toml
[[toml]]              # marker tuỳ chọn, chỉ để PageConfigSh.kt nhận diện inline TOML

[[group]]
title = "Nhóm A"

[[action]]
title = "Hành động 1"

[[action]]
title = "Hành động 2"

[[group]]
title = "Nhóm B"

[[page]]
title = "Trang con"
```
- `action.params`, `text.rows`, `menu.items`, `page.rows`,... (nesting nội bộ trong 1 node)
  **không đổi**, vẫn dùng dot-notation như cũ, chỉ bớt tiền tố `group.` (VD:
  `[[group.action.params]]` → `[[action.params]]`).
- Group không `support` (support=false) → toàn bộ mục con phía sau (tới `[[group]]` kế tiếp)
  bị bỏ qua, giống hành vi cũ.
- Không còn hỗ trợ group lồng trong group (`[[group.group]]`) — trước đây có trong code
  nhưng không thấy dùng ở bất kỳ file config thực tế nào trong repo.

## Phạm vi — QUAN TRỌNG
- Chỉ sửa parser, KHÔNG cập nhật cú pháp cũ trong các file config thực tế
  (`assets/home/etc/tool-tree.bash`, `tool-tree.bash.bak`, `assets/home/bin/menuadd`,
  `assets/home/etc/error.toml`, `website/toml.html`). Các file này dùng `[[group.action]]`
  kiểu dot-notation cũ → **sẽ không load được nữa** cho tới khi migrate ở task riêng
  (bỏ tiền tố `group.` trong các dòng `[[group.xxx]]` lồng, đổi thành `[[xxx]]` phẳng).
- Không hỗ trợ song song 2 cú pháp children (dot-notation cũ bị bỏ hẳn cho action/text/...);
  riêng `[[group]]` chính nó thì không đổi tên/cú pháp.

## Sửa lại so với bản trước (do hiểu sai yêu cầu ban đầu)
- Bản đầu: đổi hẳn `[[group]]` → `[[toml]]` làm container — SAI.
- Bản này: `[[group]]` giữ nguyên; `[[toml]]` chỉ là marker nhận diện đầu file, tuỳ chọn.

## Đã dọn
- Không còn code/comment tham chiếu cách hiểu sai (container "toml") trong 2 file đã sửa.

## Cần kiểm tra thêm
- Chưa build được (repo không có `gradlew` trong zip) — đã soát logic thủ công, cần bạn build
  thử trên máy để chắc chắn không lỗi biên dịch/runtime trước khi dùng.
- Cần migrate các file config thực tế (bash/toml/docs) sang cú pháp mới (bỏ tiền tố `group.`)
  ở task tiếp theo.
