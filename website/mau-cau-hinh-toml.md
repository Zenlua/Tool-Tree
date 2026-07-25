# Mẫu cấu hình TOML cho KR Script (Tool-Tree)

Tài liệu này mô tả đầy đủ cú pháp file cấu hình dạng TOML mà `PageConfigReader.kt`
hỗ trợ đọc song song với XML. Mỗi "loại node" tương ứng 1 bảng TOML, tên bảng =
tên loại (không cần field `type` riêng).

## 0. Quy tắc chung

- **Luôn dùng 2 ngoặc `[[ten]]`** cho mọi mục, kể cả khi hiện tại chỉ có 1 mục —
  để tránh sau này lỡ thêm mục thứ 2 cùng tên mà quên đổi ngoặc (TOML không cho
  trộn `[ten]` và `[[ten]]` cho cùng 1 khoá ở cùng vị trí, sẽ lỗi parse).
  - Sai: `[group]` rồi sau đó lại có `[[group]]` khác trong cùng file.
- Trình đọc vẫn chấp nhận dạng 1 ngoặc `[ten]` (bảng đơn) khi chắc chắn chỉ có
  đúng 1 mục loại đó, nhưng nên ưu tiên 2 ngoặc cho an toàn.
- Các mục nằm **bên trong** một `group` (con của nó) khai báo bằng đường dẫn
  lồng: `[[group.action]]`, `[[group.page]]`, `[[group.text]]` ...
- **Thứ tự hiển thị**: luôn theo đúng vị trí xuất hiện trong file (trên trước,
  dưới sau), bất kể loại mục là gì, kể cả khi các loại xen kẽ nhau. Không cần
  và không còn field `order`.
- Các loại node hợp lệ: `group | page | action | switch | picker | text | editor | resource`.

---

## 1. `[[group]]` — Nhóm

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `key` | `index`, `id` | Khoá định danh (để làm phím tắt...) |
| `title` | | Tiêu đề nhóm |
| `support` | `visible` | Script shell trả về `"1"` thì nhóm mới hiển thị |

Bên trong 1 group có thể khai báo bất kỳ node con nào: `[[group.action]]`,
`[[group.page]]`, `[[group.switch]]`, `[[group.picker]]`, `[[group.text]]`,
`[[group.editor]]`, `[[group.resource]]`.

```toml
[[group]]
title = "Nhóm 1"
support = "echo 1"   # hoặc bỏ dòng này để luôn hiển thị
```

---

## 2. Các field DÙNG CHUNG (áp dụng cho hầu hết node)

### 2.1. Field cơ bản (`main` — mọi node đều có)

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `key` | `index`, `id` | Khoá định danh duy nhất |
| `title` | | Tiêu đề hiển thị |
| `desc-sh` | | Script lấy mô tả động (ưu tiên hơn `desc`) |
| `desc` | | Mô tả tĩnh |
| `summary-sh` | | Script lấy tóm tắt động (ưu tiên hơn `summary`) |
| `summary` | | Tóm tắt tĩnh |
| `support` | `visible` | Script trả `"1"` thì node mới hiển thị |

### 2.2. Field "clickable" (page, action, switch, picker, editor)

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `lock` | `lock-state` | Script kiểm tra khoá (chưa xác thực thì không cho bấm) |
| `min-sdk` | `sdk-min` | SDK tối thiểu hỗ trợ |
| `max-sdk` | `sdk-max` | SDK tối đa hỗ trợ |
| `target-sdk` | `sdk-target` | SDK mục tiêu |
| `icon` | `icon-path` | Đường dẫn icon |
| `logo` | `logo-path` | Đường dẫn logo |
| `photo` | `photo-path` | Đường dẫn ảnh |
| `bg` | `bg-path` | Đường dẫn ảnh nền |
| `allow-shortcut` | | Cho phép tạo shortcut ngoài màn hình chính |

### 2.3. Field "runnable" (action, switch, picker — bên cạnh field clickable)

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `confirm` | | Hiện hộp thoại xác nhận trước khi chạy |
| `warn` | `warning` | Thông báo cảnh báo trước khi chạy |
| `auto-off` | `auto-close` | Tự động tắt sau khi chạy xong |
| `auto-finish` | | Tự kết thúc |
| `auto-kill` | | Tự kill tiến trình |
| `auto-restart` | | Tự khởi động lại |
| `interruptible` | `interruptable` | Cho phép ngắt giữa chừng (để trống = true) |
| `need-input` | `needs-input`, `require-input` | Cần nhập liệu qua bàn phím ảo (`read`) |
| `reload-page` | | Tải lại toàn trang sau khi chạy |
| `reload` | | `true` = tải lại toàn trang; hoặc liệt kê tên block cần cập nhật, cách nhau dấu phẩy |
| `shell` | | Câu lệnh shell (dùng cho picker khi cần) |
| `bg-task` | `background-task`, `async-task` | Chạy nền không hiện tiến trình |

---

## 3. `[[page]]` — Trang con

Kế thừa field clickable + runnable (không có `confirm`/`warn` áp dụng như action
nhưng field vẫn được đọc chung qua `runnableNodeToml`), cộng thêm:

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `config` | | Đường dẫn file cấu hình trang con (.toml/.xml) |
| `config-sh` | | Script trả về đường dẫn file cấu hình động |
| `html` | | Trang HTML online để mở |
| `before-load` | `before-read` | Script chạy trước khi đọc trang |
| `after-load` | `after-read` | Script chạy sau khi đọc trang |
| `load-ok` | `load-success` | Script chạy khi tải thành công |
| `load-fail` | `load-error` | Script chạy khi tải lỗi |
| `link` | `href` | Mở liên kết ngoài thay vì mở trang con |
| `activity` | `a`, `intent` | Mở Activity Android chỉ định thay vì mở trang con |
| `option-sh` | `option-su`, `options-sh` | Script sinh menu tuỳ chọn động cho trang |
| `handler-sh` | `handler`, `set`, `getstate`, `script` | Script xử lý chung của trang |
| `lock` | `lock-state` | Script kiểm tra khoá |

### 3.1. `[[page.options]]` — Mục trong menu của trang (tuỳ chọn)

Kế thừa field runnable, cộng thêm:

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `type` | | Loại mục menu |
| `style` | | `"fab"` = hiển thị dạng nút nổi (Floating Action Button) |
| `suffix` | | Đuôi file lọc (vd `"zip,apk,7z"`), tự suy ra `mime` nếu chưa set |
| `mime` | | Kiểu MIME lọc file |
| `path-home` | `home-path`, `pathhome` | Thư mục khởi đầu khi mở trình chọn file |
| `multiple` | | Cho phép chọn nhiều |
| `box` | `visible`, `check` | Script kiểm tra trạng thái tick chọn |
| `silent` | `hidden` | Ẩn khỏi menu hiển thị (để trống = true) |
| `link` | `href` | Mở liên kết ngoài |
| `activity` | `a`, `intent` | Mở Activity chỉ định |
| `html` | | Trang HTML online |
| `config` | | File cấu hình trang con |
| `config-sh` | | Script trả về đường dẫn cấu hình động |
| `title` | `text` | Tiêu đề mục menu |

```toml
[[page]]
title = "Xem log hệ thống"
config = "log_page.toml"
before-load = "touch /sdcard/log.txt"

  [[page.options]]
  title = "Xoá log"
  script = "rm -f /sdcard/log.txt"
  confirm = true
```

---

## 4. `[[switch]]` — Công tắc bật/tắt

Kế thừa field clickable + runnable, cộng thêm:

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `get` | `getstate` | Script lấy trạng thái hiện tại (trả `"1"`/`"true"` = bật) |
| `set` | `setstate` | Script thực thi khi đổi trạng thái (nhận biến `state` = `"1"`/`"0"`) |
| `lock` | `lock-state` | Script kiểm tra khoá |

```toml
[[switch]]
title = "Bật tiết kiệm pin"
get = "cat /sys/power/mode"
set = "echo $state > /sys/power/mode"
confirm = true
```

---

## 5. `[[picker]]` — Chọn 1/nhiều giá trị từ danh sách

Kế thừa field clickable + runnable, cộng thêm:

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `option-sh` | `options-sh`, `options-su` | Script sinh danh sách lựa chọn động (mỗi dòng `giá trị|tiêu đề`) |
| `multiple` | | Cho phép chọn nhiều |
| `separator` | | Ký tự nối các giá trị khi chọn nhiều |
| `get` | `getstate` | Script lấy giá trị hiện tại |
| `set` | `setstate` | Script thực thi khi chọn xong (nhận biến `state`) |
| `lock` | `lock-state` | Script kiểm tra khoá |

### 5.1. `[[picker.options]]` — Lựa chọn tĩnh (khi không dùng `option-sh`)

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `val` | `value` | Giá trị thực khi chọn |
| `title` | `text` | Tiêu đề hiển thị |

```toml
[[picker]]
title = "Chọn chế độ CPU"
get = "cat /sys/cpu_mode"
set = "echo $state > /sys/cpu_mode"

  [[picker.options]]
  val = "performance"
  title = "Hiệu năng cao"

  [[picker.options]]
  val = "powersave"
  title = "Tiết kiệm pin"
```

---

## 6. `[[action]]` — Hành động thực thi script

Kế thừa field clickable + runnable, cộng thêm:

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `script` | `set`, `setstate` | Nội dung script sẽ chạy |
| `lock` | `lock-state` | Script kiểm tra khoá |

```toml
[[action]]
title = "Xoá cache"
confirm = true
script = "rm -rf /cache/*"
```

### 6.1. `[[action.params]]` — Tham số nhập trước khi chạy

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `name` | | **Bắt buộc**, tên biến duy nhất (dùng trong `script` dạng `$name`) |
| `title` | | Tiêu đề hiển thị |
| `label` | | Nhãn hiển thị (nếu khác title) |
| `desc` | | Mô tả |
| `placeholder` | | Watermark/gợi ý trong ô nhập |
| `value` | | Giá trị mặc định (tĩnh) |
| `value-sh` | `value-su` | Script lấy giá trị mặc định động |
| `type` | | Loại control, xem bảng bên dưới |
| `required` | | Bắt buộc phải nhập (báo lỗi nếu để trống) |
| `readonly` | | Chỉ xem, khoá không cho sửa **và không gửi giá trị đi khi chạy action** |
| `maxlength` | | Độ dài tối đa (chỉ áp dụng ô nhập văn bản) |
| `min` / `max` | | Giá trị nhỏ nhất/lớn nhất (chỉ áp dụng `seekbar`, `int`, `number`) |
| `multiple` | | Cho phép chọn nhiều (dropdown nhiều lựa chọn, hoặc chọn nhiều file/thư mục với `type=file/folder`) |
| `separator` | | Ký tự nối khi chọn nhiều (mặc định `\n`) |
| `editable` | | Cho phép tự gõ đường dẫn (chỉ `type=file/folder`) |
| `suffix` | | Đuôi file lọc, vd `"zip,apk"` (chỉ `type=file`) |
| `mime` | | Kiểu MIME lọc file (chỉ `type=file`) |
| `path-home` | `home-path`, `pathhome` | Thư mục khởi đầu khi mở trình chọn file/thư mục |
| `options-sh` | `option-sh`, `options-su` | Script sinh danh sách lựa chọn động (mỗi dòng `giá trị|tiêu đề`) |
| `support` | `visible` | Script trả `"1"` thì param mới hiển thị |

**Bảng giá trị `type` hỗ trợ:**

| `type` | Control hiển thị |
|---|---|
| *(bỏ trống, hoặc `text`, `int`, `number`, `color`)* | Ô nhập văn bản (EditText); `int`/`number` kiểm tra là số + so `min`/`max`; `color` kiểm tra đúng định dạng mã màu |
| `bool`, `checkbox` | Checkbox — giá trị gửi đi `"1"`/`"0"` |
| `switch` | Công tắc gạt — giá trị gửi đi `"1"`/`"0"` |
| `seekbar` | Thanh trượt, dùng cùng `min`/`max` |
| `file`, `folder` | Trình chọn file/thư mục |
| `app`, `packages` | Trình chọn ứng dụng đã cài |
| Có khai báo `options`/`options-sh` (và không phải `app`/`packages`) | Dropdown chọn 1 hoặc nhiều (theo `multiple`) |

### 6.2. `[[action.params.options]]` — Lựa chọn tĩnh cho param dạng dropdown

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `val` | `value` | Giá trị thực khi chọn |
| `title` | `text` | Tiêu đề hiển thị |

### 6.3. Hệ thống phụ thuộc `depend-*` (ẩn/hiện param theo param khác)

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `depend-on` | `depend` | Tên (các) param cha điều khiển, nhiều cha nối bằng `\|` (vd `"mode\|cam"`) |
| `depend-value` | | Giá trị cần khớp cho từng cha (theo đúng thứ tự `depend-on`), trong 1 vị trí các giá trị chấp nhận (OR) cách nhau dấu phẩy (vd `"a\|b,c"`) |
| `depend-mode` | | `"show"` (mặc định, chỉ hiện khi khớp) hoặc `"hide"` (ẩn khi khớp); có thể khai riêng từng cha nối `\|` |
| `depend-logic` | `depend-priority` | Cách gộp nhiều điều kiện: `"and"` (mặc định, tất cả phải thỏa) \| `"priority"`/`"or"` (ưu tiên trái→phải) \| `"priority-rtl"`/`"or-rtl"` (ưu tiên phải→trái) \| `"xor"` (đúng 1 điều kiện) \| `"nand"` (phủ định and) |
| `depend-threshold` | | 0–100, % điều kiện tối thiểu cần thỏa khi dùng `"and"` (mặc định `-1` = 100%) |
| `depend-default` | | Trạng thái khi KHÔNG điều kiện nào thỏa: `"show"` (mặc định) hoặc `"hide"` |
| `depend-initial` | `depend-initial-state` | Trạng thái ẩn/hiện BAN ĐẦU trước khi đánh giá: `"auto"` (mặc định, theo `depend-default`) \| `"show"` \| `"hide"` |
| `depend-negate` | | `true` = đảo ngược toàn bộ điều kiện (show↔hide) |
| `depend-include-hidden` | | Mặc định `true`: param bị ẩn vẫn gửi giá trị khi chạy; đặt `"false"` để loại hẳn khỏi kết quả khi đang ẩn |
| `depend-cascade` | | Mặc định `true`: cha đang ẩn thì con cũng ẩn theo (dây chuyền); đặt `"false"` để con tự đánh giá độc lập |
| `depend-onchange` | `depend-on-change`, `depend-callback` | Script/callback gọi khi trạng thái ẩn/hiện của param THAY ĐỔI thật sự |
| `depend-readonly` | | `true`: khi điều kiện không thỏa, param KHÔNG ẩn mà chỉ bị làm mờ + khoá tương tác (giống readonly tạm thời), thay vì biến mất |

```toml
[[action]]
title = "Sao lưu dữ liệu"
script = """
mkdir -p $dest
cp -r $src/* $dest/
"""

  [[action.params]]
  name = "src"
  title = "Thư mục nguồn"
  type = "folder"
  required = true

  [[action.params]]
  name = "mode"
  title = "Chế độ"
  type = "text"
  value = "auto"

    [[action.params.options]]
    val = "auto"
    title = "Tự động"

    [[action.params.options]]
    val = "manual"
    title = "Thủ công"

  [[action.params]]
  name = "dest"
  title = "Thư mục đích (chỉ hiện khi Chế độ = Thủ công)"
  type = "folder"
  depend-on = "mode"
  depend-value = "manual"
  depend-readonly = "true"   # bị mờ + khoá thay vì biến mất khi không thỏa

  [[action.params]]
  name = "note"
  title = "Ghi chú (chỉ xem, không cho sửa/không gửi đi)"
  value = "Bản build nội bộ"
  readonly = "true"
```

---

## 7. `[[text]]` — Khối văn bản tĩnh/động

Kế thừa field `main` (không có clickable/runnable), cộng thêm nhiều dòng `rows`:

### 7.1. `[[text.rows]]` — Từng dòng nội dung

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `text` | | Nội dung tĩnh |
| `sh` | | Script sinh nội dung động (cập nhật lại theo `reload`) |
| `bold` | `b` | In đậm |
| `italic` | `i` | In nghiêng |
| `underline` | `u` | Gạch chân |
| `foreground` | `color` | Mã màu chữ |
| `bg` | `background`, `bgcolor` | Mã màu nền |
| `size` | | Cỡ chữ |
| `break` | | Xuống dòng sau đoạn này |
| `align` | | `"normal"` \| `"center"` \| `"opposite"` (căn phải, từ Android P) |
| `link` | `href` | Mở liên kết khi bấm vào |
| `activity` | `a`, `intent` | Mở Activity khi bấm vào |
| `photo` | `photo-path` | Chèn ảnh |
| `script` | `run` | Script chạy khi bấm vào dòng này |

```toml
[[text]]
title = "Thông tin thiết bị"

  [[text.rows]]
  text = "Model: "
  bold = true

  [[text.rows]]
  sh = "getprop ro.product.model"
  break = true
```

---

## 8. `[[editor]]` — Mở trình soạn thảo văn bản (TextEditorActivity)

Kế thừa field clickable, cộng thêm:

| Field | Bí danh khác | Ý nghĩa |
|---|---|---|
| `file` | `path` | Đường dẫn file cần mở |
| `wrap` | | Bật word-wrap (mặc định bật; đặt `"0"`/`"false"`/`"off"`/`"no-wrap"` để tắt) |
| `placeholder` | | Watermark khi file trống |
| `readonly` | | Chỉ xem, không cho sửa/lưu |
| `need-input` | | Cần thao tác nhập trước khi mở |
| `value` | | Nội dung mặc định (tĩnh) nếu file chưa tồn tại |
| `value-sh` | | Script sinh nội dung mặc định động |

```toml
[[editor]]
title = "Sửa file cấu hình"
file = "/sdcard/config.conf"
wrap = true
```

---

## 9. `[[resource]]` — Giải nén tài nguyên kèm theo (assets)

Có thể khai báo trực tiếp bên trong BẤT KỲ node nào (`group`, `page`, `switch`,
`picker`, `action`, `text`) — không phải bảng riêng cấp cao nhất mà là field/mảng
con của node đó:

| Field | Ý nghĩa |
|---|---|
| `resource-file` | Giải nén 1 file tài nguyên (asset) ra thiết bị |
| `resource-dir` | Giải nén 1 thư mục tài nguyên ra thiết bị |

### 9.1. `[[<node>.resources]]` — Khai báo nhiều tài nguyên cùng lúc

| Field | Ý nghĩa |
|---|---|
| `file` | Đường dẫn file asset cần giải nén |
| `dir` | Đường dẫn thư mục asset cần giải nén |

```toml
[[action]]
title = "Cài đặt script hỗ trợ"
script = "sh /data/local/tmp/helper.sh"
resource-file = "scripts/helper.sh"

  [[action.resources]]
  file = "scripts/lib.sh"

  [[action.resources]]
  dir = "scripts/modules"
```

---

## 10. Mẫu file đầy đủ (kết hợp mọi loại node)

```toml
# ============================================================
# Ví dụ file cấu hình TOML đầy đủ cho 1 trang KR Script
# ============================================================

[[group]]
title = "Hệ thống"
support = "echo 1"

  [[group.switch]]
  title = "Chế độ máy bay"
  get = "settings get global airplane_mode_on"
  set = "settings put global airplane_mode_on $state"
  confirm = true

  [[group.picker]]
  title = "Múi giờ"
  get = "getprop persist.sys.timezone"
  set = "setprop persist.sys.timezone $state"

    [[group.picker.options]]
    val = "Asia/Ho_Chi_Minh"
    title = "Việt Nam (GMT+7)"

    [[group.picker.options]]
    val = "UTC"
    title = "UTC"

  [[group.action]]
  title = "Xoá cache hệ thống"
  desc = "Giải phóng dung lượng tạm"
  confirm = true
  warn = "Thao tác này không thể hoàn tác"
  script = "rm -rf /cache/* /data/local/tmp/cache/*"

    [[group.action.params]]
    name = "keepLogs"
    title = "Giữ lại log"
    type = "bool"
    value = "0"

    [[group.action.params]]
    name = "targetDir"
    title = "Thư mục cần xoá thêm"
    type = "folder"
    depend-on = "keepLogs"
    depend-value = "0"

  [[group.page]]
  title = "Cài đặt nâng cao"
  config = "advanced.toml"

  [[group.text]]
  title = "Ghi chú"

    [[group.text.rows]]
    text = "Phiên bản công cụ: "

    [[group.text.rows]]
    sh = "echo 1.0.0"
    bold = true
    break = true

  [[group.editor]]
  title = "Sửa file build.prop"
  file = "/system/build.prop"
  confirm = true
```

---

## 11. Ghi chú kỹ thuật

- Mọi giá trị boolean (`true`/`false`/`1`/`0`) đều được so khớp không phân biệt
  hoa/thường qua hàm `tomlTruthy`, trừ vài field so khớp chuỗi trực tiếp
  (`readonly`, `wrap`, `need-input` ở `editor` — chỉ nhận đúng `"true"`/`"1"`).
- Script (`script`, `set`, `get`, ...) chạy với quyền root qua `ScriptEnvironmen`,
  biến tham số của `action.params` được truyền vào dưới dạng biến shell cùng tên
  (`$name`).
- File này chỉ mô tả **định dạng TOML**; định dạng XML tương đương vẫn được hỗ
  trợ song song và không thay đổi hành vi.
