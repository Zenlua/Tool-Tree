# Hướng dẫn sử dụng thuộc tính `depend-*` trong KR Script

Tài liệu này giải thích chi tiết cách dùng từng thuộc tính `depend-*` để điều khiển ẩn/hiện param theo giá trị của (các) param khác, dựa theo logic hiện có trong `ActionParamsLayoutRender.kt` + `PageConfigReader.kt`.

## Cơ chế chung

Mỗi `<param>` có thể khai báo phụ thuộc vào 1 hoặc nhiều `<param>` khác (gọi là **param cha**). Khi giá trị param cha thay đổi, hàm `evaluateDependencies()` chạy lại, tính `shouldShow` cho từng param (có tính cả cascade — xem mục 10) rồi set `View.VISIBLE` / `View.GONE`.

---

## 1. `depend-on` (hoặc `depend`)

**Mục đích:** khai báo (các) param cha mà param này phụ thuộc vào. Nhiều param cha nối bằng `|`.

```xml
<param name="mode" type="select" title="Chế độ" option-sh="echo -e 'basic\nadvanced'" />

<param name="advanced_opt" type="input" title="Tùy chọn nâng cao"
       depend-on="mode" depend-value="advanced" />
```
→ `advanced_opt` chỉ hiện khi `mode = advanced`.

Nhiều cha:
```xml
depend-on="mode|target_fs"
```
→ phụ thuộc đồng thời `mode` và `target_fs` (cách kết hợp do `depend-logic` quyết định).

---

## 2. `depend-value`

**Mục đích:** giá trị cần khớp ở param cha tương ứng. Vị trí trong `depend-value` khớp theo đúng thứ tự khai báo ở `depend-on` (cách nhau bởi `|`). Trong mỗi vị trí, nhiều giá trị chấp nhận (OR) cách nhau bởi dấu phẩy.

```xml
depend-on="mode|target_fs"
depend-value="advanced|EXT4,F2FS"
```
→ điều kiện 1: `mode` khớp khi = `advanced`.
→ điều kiện 2: `target_fs` khớp khi = `EXT4` **hoặc** `F2FS`.

Giá trị so khớp không chỉ khớp `value` thực tế mà còn khớp `title` hiển thị, hoặc phần trong ngoặc `()` của title (ví dụ option có title `"A (so)"` thì `depend-value="A"` hoặc `depend-value="so"` hoặc `depend-value="(so)"` đều khớp được).

---

## 3. `depend-mode`

**Mục đích:** quy định khi khớp `depend-value` thì **hiện** hay **ẩn**. Mặc định `"show"`. Có thể khai riêng cho từng param cha, nối bằng `|`.

```xml
depend-on="mode"
depend-value="basic"
depend-mode="hide"
```
→ Khi `mode = basic` thì **ẩn** param này (ngược lại với mặc định).

Nhiều cha, mode khác nhau:
```xml
depend-on="mode|target_fs"
depend-value="basic|EROFS"
depend-mode="hide|show"
```
→ cha 1 (`mode=basic`) → ẩn khi khớp. Cha 2 (`target_fs=EROFS`) → hiện khi khớp.

---

## 4. `depend-logic`

**Mục đích:** cách kết hợp nhiều điều kiện khi có ≥2 param cha. Có 5 chế độ.

### `and` (mặc định)
Tất cả điều kiện phải cùng "muốn hiện" (`wantShow`) thì mới hiện.
```xml
<param name="wifi" type="switch" title="WiFi" />
<param name="battery_saver" type="switch" title="Tiết kiệm pin" />

<param name="wifi_boost" type="switch" title="Tăng tốc WiFi"
       depend-on="wifi|battery_saver"
       depend-value="1|0"
       depend-mode="show|show"
       depend-logic="and" />
```
→ Chỉ hiện khi WiFi bật **và** tiết kiệm pin tắt.

### `priority` (hoặc `or`) — ưu tiên trái → phải
Xét từng điều kiện theo thứ tự khai báo; điều kiện nào **khớp trước** quyết định luôn kết quả (không xét tiếp).
```xml
<param name="target_fs" type="select" title="Định dạng" option-sh="..." />
<param name="images" type="select" title="Loại ảnh" option-sh="..." />

<param name="erofs_notice" type="input" title="Cảnh báo EROFS"
       depend-on="target_fs|images"
       depend-value="EXT4,F2FS|(erofs)"
       depend-mode="hide|show"
       depend-logic="priority" />
```
→ Nếu `target_fs = EXT4/F2FS` → **ẩn ngay** (không xét `images` nữa).
→ Nếu `target_fs` không khớp → xét tiếp `images`; nếu `images = (erofs)` → **hiện**.
→ Nếu không điều kiện nào khớp → dùng `depend-default`.

### `priority-rtl` (hoặc `or-rtl`)
Giống `priority` nhưng xét từ phải sang trái (điều kiện cuối cùng trong `depend-on` được xét trước).
```xml
depend-on="a|b|c"
depend-value="1|2|3"
depend-logic="priority-rtl"
```
→ Xét `c` trước, rồi `b`, rồi `a`.

### `xor`
Chỉ hiện khi **đúng một** điều kiện khớp (không hơn, không kém).
```xml
<param name="use_manual" type="switch" title="Nhập tay" />
<param name="use_auto" type="switch" title="Tự động" />

<param name="conflict_warning" type="input" title="⚠ Xung đột cấu hình"
       depend-on="use_manual|use_auto"
       depend-value="1|1"
       depend-logic="xor" />
```
→ Nếu **cả hai** hoặc **không cái nào** bật → không hiện cảnh báo. Chỉ hiện khi đúng 1 trong 2 bật.

### `nand`
Phủ định của `and` — hiện khi **không phải tất cả** điều kiện đều thỏa (ít nhất 1 cái không thỏa thì hiện).
```xml
depend-on="a|b"
depend-value="1|1"
depend-logic="nand"
```
→ Hiện trừ khi cả `a=1` và `b=1` cùng lúc.

---

## 5. `depend-default`

**Mục đích:** kết quả khi **không có điều kiện nào khớp** (chỉ có ý nghĩa rõ với `priority`/`priority-rtl`, hoặc khi param cha không tìm thấy). Giá trị: `"show"` (mặc định) hoặc `"hide"`.

```xml
<param name="advanced_toggle" type="input" title="Tuỳ chọn hiếm gặp"
       depend-on="mode"
       depend-value="expert"
       depend-logic="priority"
       depend-default="hide" />
```
→ Chỉ hiện khi `mode = expert`; mọi trường hợp khác (kể cả không xác định được `mode`) → **ẩn** thay vì mặc định hiện như trước.

---

## 6. `depend-initial` (hoặc `depend-initial-state`)

**Mục đích:** trạng thái ẩn/hiện **ngay lúc mở dialog**, trước khi kịp đánh giá điều kiện thật (tránh nhấp nháy khi load). Giá trị: `"auto"` (mặc định — dựa theo `depend-default`), `"show"`, `"hide"`.

```xml
<param name="big_form" type="input" title="Form phức tạp"
       depend-on="enable_big_form"
       depend-value="1"
       depend-initial="hide" />
```
→ Ngay khi dialog vừa mở, `big_form` bị ẩn tức thì (không đợi 1 nhịp `evaluateDependencies()` chạy xong mới ẩn), tránh hiện rồi biến mất gây giật màn hình.

---

## 7. `depend-negate`

**Mục đích:** đảo ngược toàn bộ kết quả cuối cùng của khối điều kiện (áp dụng sau khi đã tính `and`/`nand`/`xor`). Giá trị: `"true"`/`"1"`/`"negate"`.

```xml
<param name="admin_mode" type="switch" title="Admin" />

<param name="user_only_hint" type="input" title="Chỉ dành cho người dùng thường"
       depend-on="admin_mode"
       depend-value="1"
       depend-negate="true" />
```
→ Bình thường `depend-value="1"` nghĩa là hiện khi `admin_mode=1`. Có `depend-negate="true"` → đảo lại: hiện khi `admin_mode != 1`.

Kết hợp với `xor` để bắt "cả 2 cùng bật":
```xml
depend-on="use_manual|use_auto"
depend-value="1|1"
depend-logic="xor"
depend-negate="true"
```
→ `xor` gốc chỉ đúng khi *đúng một* cái bật → `negate` đảo lại → hiện khi **cả hai cùng bật hoặc cùng tắt** (loại trừ trường hợp chỉ 1 cái bật).

---

## 8. `depend-threshold`

**Mục đích:** chỉ áp dụng cho `depend-logic="and"` (mặc định) — quy định **% tối thiểu** số điều kiện cần thỏa thay vì bắt buộc 100%. Giá trị: số nguyên 0–100 (mặc định `-1` = 100%).

```xml
<param name="cond_a" type="switch" title="Điều kiện A" />
<param name="cond_b" type="switch" title="Điều kiện B" />
<param name="cond_c" type="switch" title="Điều kiện C" />

<param name="need_2_of_3" type="input" title="Cần ít nhất 2/3 điều kiện"
       depend-on="cond_a|cond_b|cond_c"
       depend-value="1|1|1"
       depend-threshold="67" />
```
→ 3 điều kiện, ngưỡng 67% ≈ tối thiểu 2/3 → chỉ cần 2 trong 3 công tắc bật là hiện, không cần cả 3.

---

## 9. `depend-include-hidden`

**Mục đích:** khi param bị **ẩn**, mặc định nó vẫn được đưa vào kết quả `readParamsValue()` nếu có giá trị (không bắt buộc `required`). Thuộc tính này hiện chỉ ảnh hưởng tới việc bỏ qua kiểm tra `required` khi ẩn — không phải "loại bỏ khỏi params". Giá trị: `"true"`/`"1"`.

```xml
<param name="hidden_debug_flag" type="input" title="Debug flag" value="0"
       depend-on="dev_mode"
       depend-value="1"
       depend-include-hidden="true" />
```
→ Dùng khi bạn muốn 1 giá trị mặc định (`value="0"`) vẫn được gửi lên script kể cả khi param đang ẩn (ví dụ 1 cờ ẩn luôn đi kèm mọi lần chạy, người dùng chỉ chỉnh nó khi bật `dev_mode`).

> ⚠️ Nếu muốn **loại hẳn giá trị của param ẩn khỏi kết quả**, cần sửa thêm đoạn `readParamsValue()` (chưa áp dụng trong bản hiện tại).

---

## 10. `depend-cascade` — cha ẩn thì con ẩn theo *(mới)*

**Mặc định: BẬT SẴN (`true`)** — không cần khai báo gì cũng đã có hiệu lực.

**Mục đích:** nếu **bất kỳ param cha nào** trong `depend-on` đang **bị ẩn** (do chính `depend-on` của nó, hoặc do một chuỗi phụ thuộc nhiều cấp phía trên), param con này **tự động ẩn theo luôn**, bất kể giá trị hiện tại của cha có khớp `depend-value` hay không.

Việc tính toán chạy qua **nhiều lượt (fixed-point)** cho tới khi trạng thái ổn định, nên hoạt động đúng dù param cha được khai báo **trước hay sau** param con trong file XML, và lan truyền đúng qua **chuỗi nhiều cấp** (con của con của con...).

### Ví dụ: chuỗi phụ thuộc 3 cấp

```xml
<param name="mode" type="select" title="Chế độ"
       option-sh="echo -e 'basic\nadvanced'" />

<!-- Cấp 2: chỉ hiện khi mode = advanced -->
<param name="target_fs" type="select" title="Định dạng đích"
       option-sh="echo -e 'EXT4\nF2FS\nEROFS'"
       depend-on="mode" depend-value="advanced" />

<!-- Cấp 3: phụ thuộc vào target_fs (là param cấp 2, có thể đang ẩn) -->
<param name="erofs_readonly_hint" type="input" title="⚠ EROFS chỉ đọc"
       depend-on="target_fs" depend-value="EROFS" />
```

- Khi `mode = basic` → `target_fs` bị ẩn (không khớp `advanced`).
- Vì `depend-cascade` mặc định bật, `erofs_readonly_hint` **cũng tự ẩn theo `target_fs`**, dù giá trị đang lưu trong `target_fs` (widget đã bị ẩn, người dùng không sửa được nữa) tình cờ vẫn là `EROFS` từ trước đó. Nếu không có cascade, `erofs_readonly_hint` sẽ hiện ra một cách vô lý dù `target_fs` (cha trực tiếp của nó) đang không hiển thị trên màn hình.
- Chỉ khi `mode = advanced` (→ `target_fs` hiện) **và** người dùng chọn `target_fs = EROFS`, thì `erofs_readonly_hint` mới thực sự hiện.

### Tắt cascade cho 1 param cụ thể

Trường hợp hiếm gặp — bạn muốn param con vẫn tự đánh giá theo **giá trị** của cha ngay cả khi hàng chứa cha đang ẩn khỏi màn hình:

```xml
<param name="always_evaluate" type="input" title="Luôn tự xét theo giá trị cha"
       depend-on="target_fs" depend-value="EROFS"
       depend-cascade="false" />
```
→ `always_evaluate` sẽ hiện khi `target_fs` **có giá trị** `EROFS`, kể cả khi bản thân dòng `target_fs` đang bị ẩn khỏi giao diện.

---

## 11. `depend-onchange` (hoặc `depend-on-change`, `depend-callback`) — đã thực thi *(mới)*

**Mục đích:** chạy 1 đoạn shell script mỗi khi trạng thái ẩn/hiện của param này **thực sự đổi** (từ hiện → ẩn hoặc ẩn → hiện). Script chạy trên **luồng nền riêng** (không chạy trên UI thread, tránh treo giao diện khi lệnh root mất thời gian).

Script nhận được 2 biến môi trường:

| Biến | Ý nghĩa |
|---|---|
| `$PARAM_NAME` | Tên param vừa đổi trạng thái |
| `$PARAM_VISIBLE` | `1` nếu param vừa chuyển sang **hiện**, `0` nếu vừa chuyển sang **ẩn** |

### Ví dụ 1: ghi log mỗi khi 1 param đổi trạng thái

```xml
<param name="mode" type="select" title="Chế độ"
       option-sh="echo -e 'basic\nadvanced'" />

<param name="advanced_opt" type="input" title="Tùy chọn nâng cao"
       depend-on="mode" depend-value="advanced"
       depend-onchange="log -t krscript &quot;advanced_opt doi trang thai: visible=$PARAM_VISIBLE&quot;" />
```
→ Mỗi lần `advanced_opt` chuyển ẩn ↔ hiện (do người dùng đổi `mode`), dòng log được ghi ra logcat.

### Ví dụ 2: tự dọn dẹp giá trị khi param bị ẩn đi

```xml
<param name="temp_folder" type="folder" title="Thư mục tạm" value=""
       depend-on="use_temp_folder" depend-value="1"
       depend-onchange="if [ &quot;$PARAM_VISIBLE&quot; = &quot;0&quot; ]; then rm -rf /sdcard/krscript_temp; fi" />
```
→ Khi `temp_folder` bị ẩn đi (người dùng tắt `use_temp_folder`), script tự xoá thư mục tạm tương ứng — hữu ích để dọn rác khi 1 tính năng bị tắt.

### Ví dụ 3: kết hợp `depend-cascade` + `depend-onchange`

```xml
<param name="root_access" type="switch" title="Quyền Root" />

<param name="deep_clean" type="switch" title="Dọn dẹp sâu"
       depend-on="root_access" depend-value="1" />

<param name="deep_clean_confirm" type="input" title="Xác nhận dọn dẹp sâu"
       depend-on="deep_clean" depend-value="1"
       depend-onchange="log -t krscript &quot;deep_clean_confirm: $PARAM_VISIBLE&quot;" />
```
→ Nếu `root_access` tắt → `deep_clean` tự ẩn (điều kiện `depend-value` không khớp) → nhờ `depend-cascade`, `deep_clean_confirm` **cũng ẩn theo `deep_clean`** dù `deep_clean` từng có giá trị `1` trước đó → `depend-onchange` của `deep_clean_confirm` được gọi đúng 1 lần với `PARAM_VISIBLE=0`.

> ⚠️ **Lưu ý:** callback chạy bằng quyền root qua `ScriptEnvironmen.executeResultRoot`, hiện **chưa được truyền** `PAGE_CONFIG_DIR` / `PAGE_WORK_DIR` (biến môi trường chỉ có khi chạy trong ngữ cảnh của 1 trang cấu hình cụ thể). Nếu cần các biến này trong script `depend-onchange`, cần bổ sung thêm việc truyền `NodeInfoBase` vào `ActionParamsLayoutRender`.
>
> Callback **không** gọi khi dialog vừa mở (chỉ gọi khi có thay đổi thật so với lần đánh giá trước).

---

## Bảng tóm tắt nhanh

| Thuộc tính | Áp dụng cho | Giá trị | Mặc định |
|---|---|---|---|
| `depend-on` / `depend` | tên param cha | text, `\|` cách nhau | — |
| `depend-value` | điều kiện khớp | text, `,` OR trong 1 vị trí, `\|` cách vị trí | — |
| `depend-mode` | show/hide khi khớp | `show`/`hide`, có thể riêng từng cha | `show` |
| `depend-logic` | cách kết hợp nhiều cha | `and`, `priority`/`or`, `priority-rtl`/`or-rtl`, `xor`, `nand` | `and` |
| `depend-default` | kết quả khi không match | `show`/`hide` | `show` |
| `depend-initial` / `depend-initial-state` | trạng thái lúc mở dialog | `auto`/`show`/`hide` | `auto` |
| `depend-negate` | đảo kết quả cuối | `true`/`1`/`negate` | `false` |
| `depend-threshold` | % điều kiện cần thỏa (chỉ `and`) | 0–100 | -1 (=100%) |
| `depend-include-hidden` | có bỏ qua required khi ẩn | `true`/`1` | `false` |
| `depend-cascade` | cha ẩn thì con ẩn theo | `true`/`false` | `true` |
| `depend-onchange` / `depend-on-change` / `depend-callback` | script chạy khi đổi trạng thái | text (shell script), có `$PARAM_NAME`/`$PARAM_VISIBLE` | — |