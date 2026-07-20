# Tài liệu tham khảo XML cấu hình Tool-Tree (KR Script Framework)

> Tài liệu này được rút ra trực tiếp từ mã nguồn (`PageConfigReader.kt` và các model trong
> `com.omarea.krscript.model`), liệt kê **toàn bộ** thẻ và thuộc tính XML mà ứng dụng hiện đang
> đọc/hiểu được. Attribute nào không có trong danh sách dưới đây thì sẽ bị parser bỏ qua (không
> gây lỗi, nhưng cũng không có tác dụng gì).

---

## 1. Cấu trúc chung của 1 file cấu hình

Một file cấu hình gồm nhiều **node** (nút) xếp cạnh nhau hoặc lồng trong `<group>`. Có 7 loại
node cấp cao nhất:

| Thẻ | Ý nghĩa |
|---|---|
| `<page>` | Trang con (mở ra 1 danh sách/màn hình khác) |
| `<action>` | Một hành động, khi bấm sẽ chạy script (có thể có `<param>` để hỏi người dùng nhập liệu trước) |
| `<switch>` | Công tắc bật/tắt (on/off) |
| `<picker>` | Ô chọn giá trị (đơn hoặc đa) từ danh sách, không cần vào màn hình param riêng như action |
| `<text>` | Khối văn bản hiển thị thuần (có thể có nhiều `<slice>` định dạng khác nhau) |
| `<editor>` | Mục mở trình soạn thảo văn bản để xem/sửa 1 file |
| `<group>` | Nhóm các node trên lại thành 1 khối có tiêu đề |
| `<resource>` | Không hiển thị, chỉ dùng để giải nén tài nguyên (asset) ra bộ nhớ |

**Lưu ý về thẻ gốc:** nếu file mô tả *nguyên 1 trang* (nhiều mục con), toàn bộ nội dung phải được
bọc trong 1 cặp `<page> ... </page>` ngoài cùng — bản thân cặp `<page>` ngoài cùng này chỉ đóng
vai trò gói (không được xử lý như 1 trang con thật sự). Nếu file chỉ định nghĩa **đúng 1**
`<action>`/`<switch>`/`<picker>` duy nhất thì có thể dùng thẳng thẻ đó làm gốc, không cần bọc
`<page>`.

```xml
<page>
    <title>Trang ví dụ</title>

    <group>
        <title>Nhóm 1</title>
        <action> ... </action>
        <switch> ... </switch>
    </group>

    <action> ... </action>
    <page> ... </page>   <!-- 1 trang con lồng bên trong -->
</page>
```

---

## 2. Thuộc tính dùng chung

### 2.1. Nhóm cơ bản (áp dụng cho MỌI node: page/action/switch/picker/text/editor/group)

| Thuộc tính | Ghi chú |
|---|---|
| `key` / `index` / `id` | Định danh duy nhất của node (dùng khi tạo shortcut ra màn hình chính) |
| `title` | Tiêu đề hiển thị |
| `desc` | Mô tả (text tĩnh, đặt trong thẻ `<desc>` con nếu cần nội dung dài) |
| `desc-sh` | Mô tả lấy từ kết quả shell (chạy 1 lần lúc load) |
| `summary` | Tóm tắt ngắn |
| `summary-sh` | Tóm tắt lấy từ kết quả shell |
| `support` / `visible` | Chạy shell, nếu kết quả trả về khác `"1"` thì **ẩn hẳn** node này (không load) |

Cách khác để khai báo desc/summary là dùng thẻ con:

```xml
<desc su="cat /proc/version">Text tĩnh nếu không có "su"</desc>
<!-- "su"/"sh"/"desc-sh" tương đương nhau -->
<summary sh="get_state.sh">Tóm tắt</summary>
```

### 2.2. Nhóm "clickable" (page / action / switch / picker / editor)

| Thuộc tính | Ghi chú |
|---|---|
| `lock` / `lock-state` / `locked` | Giá trị tĩnh `"1"`/`"true"`/`"locked"` = luôn khoá. Có thể dùng thẻ con `<lock>`/`<lock-state>` chứa script để tính khoá động |
| `min-sdk` / `sdk-min` | SDK tối thiểu yêu cầu |
| `max-sdk` / `sdk-max` | SDK tối đa |
| `target-sdk` / `sdk-target` | SDK mục tiêu |
| `icon` / `icon-path` | Icon hiển thị trong danh sách |
| `logo` / `logo-path` | Icon dùng khi tạo shortcut ra màn hình chính |
| `photo` / `photo-path` | Ảnh minh hoạ |
| `bg` / `bg-path` | Ảnh nền |
| `allow-shortcut` | Cho phép tạo shortcut hay không (`allow`/`true`/`1`) — mặc định: cho phép nếu `key` bắt đầu bằng `@` |

**Quy tắc đường dẫn** (icon/logo/photo/bg, và `file` của `<editor>`, `config` của `<page>`):
- Bắt đầu bằng `/` → đường dẫn tuyệt đối trên máy (đọc qua quyền root nếu cần).
- Bắt đầu bằng `file:///android_asset/...` → tài nguyên đóng gói trong assets của app.
- Đường dẫn tương đối khác → được tính tương đối theo thư mục chứa file cấu hình hiện tại, nếu
  không thấy thì thử tìm trong thư mục riêng (private dir) của ứng dụng.

### 2.3. Nhóm "runnable" (action / switch / picker — các node có thể chạy script)

| Thuộc tính | Ghi chú |
|---|---|
| `confirm` | `"true"`/`"1"` → hỏi xác nhận trước khi chạy |
| `warn` / `warning` | Nội dung cảnh báo hiển thị khi xác nhận |
| `auto-off` / `auto-close` | Tự đóng màn hình log sau khi chạy xong |
| `auto-finish` | Tự đóng luôn Activity sau khi chạy xong |
| `auto-kill` | Tự kill tiến trình sau khi chạy xong |
| `auto-restart` | Tự khởi động lại (app) sau khi chạy xong |
| `interruptible` / `interruptable` | Cho phép người dùng dừng giữa chừng hay không (mặc định: có) |
| `need-input` / `needs-input` / `require-input` | Script có dùng lệnh `read` để nhận nhập bàn phím lúc chạy hay không |
| `reload-page` | `"true"`/`"1"` → tải lại toàn bộ trang sau khi chạy xong |
| `reload` | `"reload"`/`"true"`/`"1"` → như `reload-page`; hoặc 1 chuỗi các `id` cách nhau bằng dấu phẩy → chỉ tải lại những khối đó |
| `shell` | Chế độ chạy: mặc định hiện dialog log; `bg-task`/`async-task`/`background`/`background-task`/`true`/`1` → chạy nền không hiện dialog |
| `bg-task` / `background-task` / `async-task` | Cách khác để bật chế độ chạy nền (tương đương giá trị trên của `shell`) |

---

## 3. `<action>` — hành động có thể có tham số

```xml
<action key="@my_action" title="Ví dụ" confirm="true" warning="Thao tác này không thể hoàn tác!"
        reload="true">
    <title>Ví dụ hành động</title>
    <desc>Mô tả ngắn hiển thị dưới tiêu đề</desc>

    <param name="mode" label="Chế độ" type="list" value="a"
           option-sh="echo -e 'a|A\nb|B'"/>

    <!-- script chạy khi bấm, đọc được các biến $name của từng <param> -->
    <set>
        echo "mode=$mode"
    </set>
</action>
```

Con của `<action>`:
- `<title>`, `<desc>`, `<summary>` (như phần 2.1)
- `<script>` / `<set>` / `<setstate>` — nội dung script sẽ chạy khi người dùng bấm vào action.
  Có thể tham chiếu tới biến `$tên_param` của từng `<param>`.
- `<lock>` / `<lock-state>` — script tính trạng thái khoá động.
- `<param ...>` — xem chi tiết ở mục 5.

---

## 4. `<switch>` — công tắc bật/tắt

```xml
<switch title="Chế độ tiết kiệm pin" confirm="false">
    <get>cat /sys/power/state 2>/dev/null | grep -q on && echo 1 || echo 0</get>
    <set>echo "$1" > /sys/power/state</set>
</switch>
```

Con của `<switch>`: `<title>`, `<desc>`, `<summary>`, `<resource>`,
`<lock>`/`<lock-state>`, và:

| Thẻ | Ghi chú |
|---|---|
| `<get>` / `<getstate>` | Script trả về `1`/`true` = đang bật, ngược lại = đang tắt |
| `<set>` / `<setstate>` | Script chạy khi người dùng gạt công tắc |

---

## 5. `<picker>` — chọn giá trị nhanh (không mở màn hình param riêng)

```xml
<picker title="Độ sáng màn hình" name="brightness" multiple="false" separator=",">
    <option val="low">Thấp</option>
    <option val="mid">Vừa</option>
    <option val="high">Cao</option>

    <getstate>settings get system screen_brightness_mode</getstate>
    <setstate>settings put system screen_brightness_mode "$1"</setstate>
</picker>
```

Thuộc tính riêng trên thẻ `<picker>`:

| Thuộc tính | Ghi chú |
|---|---|
| `option-sh` / `options-sh` / `options-su` | Script trả về danh sách option động, mỗi dòng `value|title` |
| `multiple` | Cho chọn nhiều giá trị |
| `separator` | Ký tự nối các giá trị đã chọn khi `multiple` (mặc định: xuống dòng `\n`) |

Con của `<picker>`: `<title>`, `<desc>`, `<summary>`, `<option val="">Tiêu đề</option>` (khai báo
tĩnh, dùng khi không dùng `option-sh`), `<getstate>`/`<get>`, `<setstate>`/`<set>`, `<resource>`,
`<lock>`/`<lock-state>`.

---

## 6. `<text>` — khối văn bản

```xml
<text>
    <title>Ghi chú</title>
    <slice bold="true" color="#FF0000" break="true">Dòng chữ đỏ, in đậm</slice>
    <slice link="https://example.com">Bấm vào đây</slice>
    <slice sh="date">Nội dung động lấy từ shell</slice>
    <slice run="reboot">Bấm để chạy script</slice>
</text>
```

Mỗi `<slice>` (đoạn văn bản) hỗ trợ các thuộc tính:

| Thuộc tính | Ghi chú |
|---|---|
| `bold` / `b` | In đậm |
| `italic` / `i` | In nghiêng |
| `underline` / `u` | Gạch chân |
| `foreground` / `color` | Màu chữ (mã hex, vd `#FF0000`) |
| `bg` / `background` / `bgcolor` | Màu nền |
| `size` | Cỡ chữ |
| `break` | Xuống dòng sau đoạn này |
| `align` | `opposite` \| `center` \| `normal` |
| `link` / `href` | Bấm vào để mở link |
| `activity` / `a` / `intent` | Bấm vào để mở 1 Activity |
| `photo` / `photo-path` | Ảnh minh hoạ cho đoạn này |
| `script` / `run` | Bấm vào để chạy script |
| `sh` | Nội dung đoạn lấy động từ kết quả shell |

---

## 7. `<editor>` — mở trình soạn thảo văn bản

```xml
<editor title="Sửa file cấu hình" file="/sdcard/config.txt" wrap="true"/>
```

| Thuộc tính | Ghi chú |
|---|---|
| `file` / `path` | **Bắt buộc.** Đường dẫn file cần mở để soạn thảo. Nếu file chưa tồn tại, sẽ tạo mới khi lưu |
| `wrap` | `"0"`/`"false"`/`"off"`/`"no-wrap"` → tắt ngắt dòng mặc định; ngược lại mặc định bật ngắt dòng |

---

## 8. `<group>` — nhóm nhiều node lại

```xml
<group key="grp1" title="Nhóm cài đặt">
    <action>...</action>
    <switch>...</switch>
    <page>...</page>
</group>
```

Thuộc tính: `key`/`index`/`id`, `title`, `support`/`visible`. Nếu `support`/`visible` trả về khác
`"1"` thì **toàn bộ node bên trong group cũng bị bỏ qua**.

---

## 9. `<page>` — trang con

```xml
<page title="Cài đặt nâng cao" config="advanced.xml">
    <option title="Làm mới" type="refresh"/>
    <option title="Tạo file mới" type="file" suffix="txt" config-sh="echo new.xml"/>
</page>
```

Thuộc tính riêng của `<page>`:

| Thuộc tính | Ghi chú |
|---|---|
| `config` | Đường dẫn tới file XML khác chứa nội dung trang con |
| `config-sh` | Script trả về đường dẫn file cấu hình (động) |
| `html` | Trang con là 1 trang web (WebView) |
| `link` / `href` | Bấm vào mở link thay vì mở trang |
| `activity` / `a` / `intent` | Bấm vào mở Activity thay vì mở trang |
| `before-load` / `before-read` | Script chạy trước khi đọc file cấu hình trang con |
| `after-load` / `after-read` | Script chạy sau khi đọc xong |
| `load-ok` / `load-success` | Script chạy khi tải trang con thành công |
| `load-fail` / `load-error` | Script chạy khi tải trang con thất bại |
| `option-sh` / `option-su` / `options-sh` | Script sinh động các mục menu (thay cho khai báo `<option>` tĩnh) |
| `handler-sh` / `handler` / `set` / `getstate` / `script` | Script xử lý khi bấm menu/nút nổi (FAB) |

Con của `<page>`: `<title>`, `<desc>`, `<summary>`, `<resource>`, `<html>`, `<config>`,
`<handler-sh>`/`<handler>`/`<set>`/`<getstate>`/`<script>`, `<lock>`/`<lock-state>`, và các mục
menu `<option>` / `<page-option>` / `<menu>` / `<menu-item>`:

```xml
<option title="Xuất file" type="file" suffix="zip" style="fab"
        box="echo 1" silent="false" link="https://..."/>
```

| Thuộc tính của `<option>` (menu-item) | Ghi chú |
|---|---|
| `type` | Loại hành vi đặc biệt: `finish` (đóng trang), `refresh` (tải lại trang), `file` (chọn file trước khi chạy)... |
| `style="fab"` | Hiển thị dạng nút nổi (Floating Action Button) |
| `suffix` / `mime` | Loại file cho phép chọn (khi `type="file"`) |
| `box` / `visible` / `check` | Script xác định trạng thái tick (checkbox), chạy lại mỗi lần mở menu |
| `silent` / `hidden` | Chạy ngầm, không hiện dialog log |
| `link` / `href`, `activity`/`a`/`intent`, `html`, `config`, `config-sh` | Cho phép menu item mở như 1 trang thay vì chạy script (ưu tiên: link > activity > html/config-sh/config) |
| + toàn bộ thuộc tính "runnable" ở mục 2.3 | vì menu-item cũng có thể chạy script như action |

---

## 10. `<param>` — tham số nhập liệu của `<action>`

```xml
<param name="ten_bien" label="Nhãn hiển thị" title="Tiêu đề phụ" desc="Mô tả"
       type="text" value="mặc định" required="true" readonly="false"
       placeholder="Gợi ý" maxlength="20"/>
```

### 10.1. Thuộc tính chung cho mọi `<param>`

| Thuộc tính | Ghi chú |
|---|---|
| `name` | **Bắt buộc, phải duy nhất.** Tên biến `$name` dùng trong script của action |
| `label` | Nhãn hiển thị cạnh ô nhập |
| `title` | Tiêu đề phụ phía trên |
| `desc` | Mô tả |
| `value` | Giá trị mặc định (tĩnh) |
| `value-sh` / `value-su` | Giá trị mặc định lấy động từ shell |
| `type` | Loại input — xem bảng 10.2 |
| `placeholder` | Gợi ý (hint) hiển thị trong ô trống |
| `required` | `"true"`/`"1"`/`"required"` → bắt buộc nhập, không được để trống |
| `readonly` | `"readonly"`/`"true"`/`"1"` → chỉ đọc, không cho sửa |
| `maxlength` | Số ký tự tối đa (dùng cho input text) |
| `min` / `max` | Giới hạn giá trị (dùng cho `type="int"`/`"number"`/`"seekbar"`) |
| `support` / `visible` | Script điều kiện hiển thị — nếu khác `"1"` thì **bỏ hẳn** param này (không tạo dòng) |
| `multiple` | `"multiple"`/`"true"`/`"1"` → cho chọn nhiều giá trị (dùng với `type="list"`/`"app"`/`"packages"`) |
| `separator` | Ký tự nối nhiều giá trị đã chọn (mặc định `\n`) |
| `editable` | Cho phép gõ tay giá trị (dùng với `type="file"`/`"folder"`) |
| `suffix` | Đuôi file cho phép chọn (dùng với `type="file"`), cũng tự suy ra `mime` nếu chưa khai báo |
| `mime` | Kiểu MIME cho phép chọn (dùng với `type="file"`) |
| `depend-on` / `depend` | Tên (các) param điều khiển hiện/ẩn dòng này — xem mục 10.4 |
| `depend-value` | (Các) giá trị cần khớp để hiện/ẩn — xem mục 10.4 |
| `depend-mode` | `show` (mặc định) hoặc `hide` — xem mục 10.4 |

### 10.2. Các `type` được hỗ trợ

| `type` | Widget hiển thị | Ghi chú |
|---|---|---|
| *(không khai báo, mặc định)* | Ô nhập văn bản | |
| `int`, `number` | Ô nhập số | Kiểm tra `min`/`max` |
| `color` | Ô nhập mã màu hex + nút bảng màu | |
| `bool`, `checkbox` | Checkbox | `value`/`value-sh` = `"1"`/`"true"` → tích sẵn |
| `switch` | Công tắc gạt | như trên |
| `seekbar` | Thanh trượt + nút `+`/`−` | dùng `min`/`max` |
| `file` | Chọn 1 file | dùng `suffix`/`mime`, `editable` |
| `folder` | Chọn 1 thư mục | dùng `editable` |
| `app`, `packages` | Chọn 1 (hoặc nhiều, nếu `multiple`) ứng dụng đã cài | có thể giới hạn danh sách app cho chọn bằng `option-sh` (trả về `packageName|Tên hiển thị`) |
| `list` *(có `option-sh`/`options-sh` hoặc `<option>` con)* | Dropdown/Spinner (≤6 lựa chọn) hoặc dialog chọn (>6 lựa chọn); nếu `multiple="true"` → dialog chọn nhiều | |

### 10.3. Khai báo danh sách lựa chọn (dùng cho `type="list"` hoặc bộ lọc app)

**Cách 1 — động qua shell** (mỗi dòng dạng `value|title`):
```xml
<param name="mode" type="list" option-sh="echo -e 'a|A\nb|B (so)\nc|C'"/>
```

**Cách 2 — khai báo tĩnh bằng thẻ `<option>` con:**
```xml
<param name="mode" type="list">
    <option val="a">A</option>
    <option val="b">B (so)</option>
    <option val="c">C</option>
</param>
```
(`val`/`value` là giá trị thực; nếu bỏ trống thì lấy luôn nội dung chữ làm giá trị)

### 10.4. `depend-on` / `depend-value` / `depend-mode` — ẩn/hiện theo giá trị param khác

**Cú pháp cơ bản (1 param cha):**
```xml
<param name="cam" type="list" option-sh="..." depend-on="mode" depend-value="b" depend-mode="show"/>
```
→ chỉ hiện dòng `cam` khi `mode` đang chọn giá trị `b`. `depend-mode="hide"` thì đảo ngược lại
(ẩn khi khớp, hiện khi không khớp).

`depend-value` có thể liệt kê **nhiều giá trị chấp nhận** (nối bằng dấu phẩy, so khớp kiểu OR):
```xml
depend-value="b,c"   <!-- khớp khi giá trị = b HOẶC c -->
```

**Nhiều param cha cùng lúc** (nối bằng dấu `|`, mỗi vị trí tương ứng 1 param cha, tất cả phải
cùng đúng — AND):
```xml
<param name="xom" ... depend-on="mode|cam" depend-value="a|b" depend-mode="show|hide"/>
<!-- hiện khi: mode = a  VÀ  cam khác b -->

<param name="zum" ... depend-on="mode|cam|xom" depend-value="a|b|b,c" depend-mode="show|hide|show"/>
<!-- hiện khi: mode = a  VÀ  cam khác b  VÀ  xom thuộc {b, c} -->
```

**So khớp mở rộng theo option** — `depend-value` không chỉ so với `value` thực tế của lựa chọn
đang chọn ở param cha, mà còn khớp được với **title** hiển thị của option đó, và với **phần văn
bản nằm trong dấu ngoặc `()`** của title (cả có ngoặc lẫn không ngoặc):
```xml
<param name="mode" type="list" option-sh="echo -e 'a|A (so)\nb|B\nc|C'"/>
<param name="ex" ... depend-on="mode" depend-value="a,A,(so)"/>
<!-- khớp khi giá trị đang chọn ở "mode" là "a" (value), HOẶC "A" (title),
     HOẶC "(so)"/"so" (phần trong ngoặc của title) -->
```

Cơ chế này áp dụng cho **mọi type** có thể làm param cha: `list` (đơn/đa), `bool`/`checkbox`,
`switch`, `seekbar`, `file`/`folder`, `app`/`packages`, `color`, và input số/văn bản mặc định —
tất cả đều cập nhật ẩn/hiện ngay lập tức khi giá trị thay đổi (không cần đóng dialog/màn hình
lại).

---

## 11. Cú pháp `@string/ten_string`

Ở bất kỳ đâu nhận text tĩnh (`title`, `desc`, nội dung `<slice>`, nội dung `<option>`...), có thể
tham chiếu tới 1 string resource đã khai báo trong `res/values*/strings.xml` thay vì gõ cứng:

```xml
<title>@string/app_name</title>
```

Nếu không tìm thấy resource tương ứng, chuỗi gốc `@string/...` sẽ được giữ nguyên (không lỗi).

---

## 12. `<resource>` — giải nén tài nguyên đóng gói sẵn

```xml
<resource file="scripts/backup.sh"/>
<resource dir="scripts/"/>
```
Dùng để giải nén 1 file hoặc cả thư mục từ assets ra bộ nhớ máy trước khi sử dụng (ví dụ 1 script
shell cần thực thi). Có thể đặt ở bất kỳ đâu bên trong `page`/`action`/`switch`/`picker`/`text`.

---

## 13. Ví dụ tổng hợp đầy đủ

```xml
<page>
    <title>Trang demo</title>

    <group title="Thông tin">
        <text>
            <title>Giới thiệu</title>
            <slice bold="true" color="#2196F3">Ứng dụng demo cấu hình KR Script</slice>
            <slice sh="date" break="true">Thời gian hiện tại:</slice>
        </text>
    </group>

    <group title="Tuỳ chọn">
        <switch title="Bật chế độ tối">
            <get>settings get system ui_night_mode</get>
            <set>settings put system ui_night_mode "$1"</set>
        </switch>

        <picker title="Độ ưu tiên" name="priority" multiple="false">
            <option val="low">Thấp</option>
            <option val="high">Cao</option>
            <getstate>echo low</getstate>
            <setstate>echo "Đã chọn: $1"</setstate>
        </picker>
    </group>

    <group title="Hành động">
        <action title="Chạy tác vụ" confirm="true" warning="Chắc chắn chứ?">
            <param name="mode" label="Chọn chế độ" type="list" value="a"
                   option-sh="echo -e 'a|A\nb|B'"/>

            <param name="cam" label="CAM" type="list"
                   option-sh="echo -e 'a|A\nb|B'"
                   depend-on="mode" depend-value="b" depend-mode="show"/>

            <param name="level" label="Mức độ" type="seekbar" value="5" min="0" max="10"/>

            <param name="target_app" label="Ứng dụng" type="app"/>

            <param name="advanced_hint" label="Chỉ hiện khi nâng cao" type="text"
                   depend-on="mode|level" depend-value="b|7,8,9,10" depend-mode="show|show"/>

            <set>
                echo "mode=$mode cam=$cam level=$level app=$target_app"
            </set>
        </action>

        <editor title="Sửa file log" file="/sdcard/log.txt"/>
    </group>
</page>
```