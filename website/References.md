# Tool-Tree Configuration XML Reference (KR Script Framework)

- [Tiếng Việt](https://zenlua.github.io/Tool-Tree/website/References_vi.html)

> This document is derived directly from the source code (`PageConfigReader.kt` and models in `com.omarea.krscript.model`), listing **all** XML tags and attributes that the application currently reads and interprets. Any attribute not included in the list below will be ignored by the parser (it will not cause errors, but will have no effect).

---

## 1. General Configuration File Structure

A configuration file consists of multiple **nodes** placed side-by-side or nested inside a `<group>`. There are 7 top-level node types:

| Tag | Meaning |
|---|---|
| `<page>` | Subpage (opens another list/screen) |
| `<action>` | An action that executes a script when clicked (can contain `<param>` to prompt for user input beforehand) |
| `<switch>` | An on/off toggle switch |
| `<picker>` | A value selection field (single or multiple) from a list, without entering a separate param screen like action |
| `<text>` | A pure text display block (can contain multiple `<slice>` tags with different formatting) |
| `<editor>` | An item that opens a text editor to view/edit a file |
| `<group>` | Groups the above nodes into a single block with a title |
| `<resource>` | Invisible, used solely to extract assets to the device storage |

**Note on the root tag:** If the file describes *an entire page* (multiple sub-items), the entire content must be wrapped in a outermost pair of `<page> ... </page>` tags. This outermost `<page>` pair only acts as a wrapper (it is not processed as an actual subpage). If the file defines **exactly one** single `<action>`/`<switch>`/`<picker>`, you can use that tag directly as the root without wrapping it in a `<page>`.

```xml
<page>
    <title>Example Page</title>

    <group>
        <title>Group 1</title>
        <action> ... </action>
        <switch> ... </switch>
    </group>

    <action> ... </action>
    <page> ... </page>   <!-- A nested subpage -->
</page>
```

---

## 2. Common Attributes

### 2.1. Basic Group (Applies to EVERY node: page/action/switch/picker/text/editor/group)

| Attribute | Notes |
|---|---|
| `key` / `index` / `id` | Unique identifier of the node (used when creating home screen shortcuts) |
| `title` | Display title |
| `desc` | Description (static text; use a nested `<desc>` child tag if long content is needed) |
| `desc-sh` | Description retrieved from a shell command output (runs once upon loading) |
| `summary` | Short summary |
| `summary-sh` | Summary retrieved from a shell command output |
| `support` / `visible` | Runs a shell command; if the returned output is not `"1"`, this node is **completely hidden** (not loaded) |

An alternative way to declare desc/summary is by using child tags:

```xml
<desc su="cat /proc/version">Static text if "su" is absent</desc>
<!-- "su", "sh", and "desc-sh" are equivalent -->
<summary sh="get_state.sh">Summary</summary>
```

### 2.2. "Clickable" Group (page / action / switch / picker / editor)

| Attribute | Notes |
|---|---|
| `lock` / `lock-state` / `locked` | Static value `"1"`/`"true"`/`"locked"` = always locked. Alternatively, use a `<lock>`/`<lock-state>` child tag containing a script to evaluate dynamic locking |
| `min-sdk` / `sdk-min` | Minimum required SDK |
| `max-sdk` / `sdk-max` | Maximum allowed SDK |
| `target-sdk` / `sdk-target` | Target SDK |
| `icon` / `icon-path` | Icon displayed in the list |
| `logo` / `logo-path` | Icon used when creating a home screen shortcut |
| `photo` / `photo-path` | Illustration image |
| `bg` / `bg-path` | Background image |
| `allow-shortcut` | Whether to allow shortcut creation (`allow`/`true`/`1`) — default: allowed if `key` starts with `@` |

**Path Resolution Rules** (for icon/logo/photo/bg, as well as `file` in `<editor>` and `config` in `<page>`):
- Starts with `/` → Absolute path on the device (read via root permissions if necessary).
- Starts with `file:///android_asset/...` → Resource packaged inside the app's assets.
- Other relative paths → Evaluated relative to the directory containing the current configuration file. If not found, the app attempts to locate it in its private directory.

### 2.3. "Runnable" Group (action / switch / picker — nodes capable of running scripts)

| Attribute | Notes |
|---|---|
| `confirm` | `"true"`/`"1"` → Prompts for confirmation before execution |
| `warn` / `warning` | Warning content displayed during confirmation |
| `auto-off` / `auto-close` | Automatically closes the log dialog after execution completes |
| `auto-finish` | Automatically closes the Activity after execution completes |
| `auto-kill` | Automatically kills the process after execution completes |
| `auto-restart` | Automatically restarts the (app) process after execution completes |
| `interruptible` / `interruptable` | Allows the user to abort midway (default: true) |
| `need-input` / `needs-input` / `require-input` | Whether the script uses the `read` command to accept keyboard input during runtime |
| `reload-page` | `"true"`/`"1"` → Reloads the entire page after execution completes |
| `reload` | `"reload"`/`"true"`/`"1"` → Same as `reload-page`; or a comma-separated string of `id`s → reloads only those specific blocks |
| `shell` | Execution mode: default shows a log dialog; `bg-task`/`async-task`/`background`/`background-task`/`true`/`1` → runs in the background without showing a dialog |
| `bg-task` / `background-task` / `async-task` | Alternative way to enable background execution (equivalent to the `shell` values above) |

---

## 3. `<action>` — Action with Optional Parameters

```xml
<action key="@my_action" title="Example" confirm="true" warning="This action cannot be undone!"
        reload="true">
    <title>Action Example</title>
    <desc>Short description displayed under the title</desc>

    <param name="mode" label="Mode" type="list" value="a"
           option-sh="echo -e 'a|A
b|B'"/>

    <!-- Script executed when clicked; can read the $name variables of each <param> -->
    <set>
        echo "mode=$mode"
    </set>
</action>
```

Child elements of `<action>`:
- `<title>`, `<desc>`, `<summary>` (as described in section 2.1)
- `<script>` / `<set>` / `<setstate>` — The script content that will run when the user clicks the action. It can reference the `$param_name` variable of each `<param>`.
- `<lock>` / `<lock-state>` — Script to evaluate dynamic locking status.
- `<param ...>` — See section 5 for details.

---

## 4. `<switch>` — On/Off Toggle Switch

```xml
<switch title="Battery Saver Mode" confirm="false">
    <get>cat /sys/power/state 2>/dev/null | grep -q on && echo 1 || echo 0</get>
    <set>echo "$1" > /sys/power/state</set>
</switch>
```

Child elements of `<switch>`: `<title>`, `<desc>`, `<summary>`, `<resource>`, `<lock>`/`<lock-state>`, and:

| Tag | Notes |
|---|---|
| `<get>` / `<getstate>` | Script returning `1`/`true` = turned on, otherwise = turned off |
| `<set>` / `<setstate>` | Script executed when the user toggles the switch |

---

## 5. `<picker>` — Quick Value Selector (Does not open a separate param screen)

```xml
<picker title="Screen Brightness" name="brightness" multiple="false" separator=",">
    <option val="low">Low</option>
    <option val="mid">Medium</option>
    <option val="high">High</option>

    <getstate>settings get system screen_brightness_mode</getstate>
    <setstate>settings put system screen_brightness_mode "$1"</setstate>
</picker>
```

Specific attributes for the `<picker>` tag:

| Attribute | Notes |
|---|---|
| `option-sh` / `options-sh` / `options-su` | Script returning a dynamic options list, formatted as `value|title` per line |
| `multiple` | Allows selecting multiple values |
| `separator` | Character used to join selected values when `multiple` is true (default: newline) |

Child elements of `<picker>`: `<title>`, `<desc>`, `<summary>`, `<option val="">Title</option>` (static declaration, used when `option-sh` is not applied), `<getstate>`/`<get>`, `<setstate>`/`<set>`, `<resource>`, `<lock>`/`<lock-state>`.

---

## 6. `<text>` — Text Block

```xml
<text>
    <title>Notes</title>
    <slice bold="true" color="#FF0000" break="true">Red, bold text line</slice>
    <slice link="https://example.com">Click here</slice>
    <slice sh="date">Dynamic content fetched from shell</slice>
    <slice run="reboot">Click to run script</slice>
</text>
```

Each `<slice>` (text segment) supports the following attributes:

| Attribute | Notes |
|---|---|
| `bold` / `b` | Bold |
| `italic` / `i` | Italic |
| `underline` / `u` | Underline |
| `foreground` / `color` | Text color (Hex code, e.g., `#FF0000`) |
| `bg` / `background` / `bgcolor` | Background color |
| `size` | Font size |
| `break` | Line break after this segment |
| `align` | `opposite` \| `center` \| `normal` |
| `link` / `href` | Click to open a URL |
| `activity` / `a` / `intent` | Click to launch an Activity |
| `photo` / `photo-path` | Illustration image for this segment |
| `script` / `run` | Click to execute a script |
| `sh` | Segment content dynamically fetched from shell command output |

---

## 7. `<editor>` — Text Editor Launcher

```xml
<editor title="Edit Config File" file="/sdcard/config.txt" wrap="true"/>
```

| Attribute | Notes |
|---|---|
| `file` / `path` | **Required.** The path of the file to open for editing. If the file does not exist, it will be created upon saving |
| `wrap` | `"0"`/`"false"`/`"off"`/`"no-wrap"` → disables default text wrapping; otherwise text wrapping is enabled by default |

---

## 8. `<group>` — Node Grouping

```xml
<group key="grp1" title="Settings Group">
    <action>...</action>
    <switch>...</switch>
    <page>...</page>
</group>
```

Attributes: `key`/`index`/`id`, `title`, `support`/`visible`. If `support`/`visible` evaluates to anything other than `"1"`, **all nodes inside the group will also be skipped**.

---

## 9. `<page>` — Subpage

```xml
<page title="Advanced Settings" config="advanced.xml">
    <option title="Refresh" type="refresh"/>
    <option title="Create New File" type="file" suffix="txt" config-sh="echo new.xml"/>
</page>
```

Specific attributes for `<page>`:

| Attribute | Notes |
|---|---|
| `config` | Path to another XML file containing the subpage content |
| `config-sh` | Script returning the configuration file path (dynamic) |
| `html` | The subpage is a web page (WebView) |
| `link` / `href` | Click to open a link instead of opening a page |
| `activity` / `a` / `intent` | Click to launch an Activity instead of opening a page |
| `before-load` / `before-read` | Script executed before reading the subpage configuration file |
| `after-load` / `after-read` | Script executed after reading completes |
| `load-ok` / `load-success` | Script executed when the subpage loads successfully |
| `load-fail` / `load-error` | Script executed when the subpage fails to load |
| `option-sh` / `option-su` / `options-sh` | Script dynamically generating menu items (replacing static `<option>` declarations) |
| `handler-sh` / `handler` / `set` / `getstate` / `script` | Handler script executed upon clicking a menu item or Floating Action Button (FAB) |

Child elements of `<page>`: `<title>`, `<desc>`, `<summary>`, `<resource>`, `<html>`, `<config>`, `<handler-sh>`/`<handler>`/`<set>`/`<getstate>`/`<script>`, `<lock>`/`<lock-state>`, and menu items declared via `<option>` / `<page-option>` / `<menu>` / `<menu-item>`:

```xml
<option title="Export File" type="file" suffix="zip" style="fab"
        box="echo 1" silent="false" link="https://..."/>
```

| Attribute of `<option>` (menu-item) | Notes |
|---|---|
| `type` | Special behavior type: `finish` (close page), `refresh` (reload page), `file` (pick a file before running)... |
| `style="fab"` | Displays as a Floating Action Button (FAB) |
| `suffix` / `mime` | Allowed file types for selection (when `type="file"`) |
| `box` / `visible` / `check` | Script determining the checked state (checkbox), re-evaluates every time the menu opens |
| `silent` / `hidden` | Runs in the background without showing a log dialog |
| `link` / `href`, `activity`/`a`/`intent`, `html`, `config`, `config-sh` | Allows the menu item to open as a page/link instead of running a script (priority order: link > activity > html/config-sh/config) |
| + all "runnable" attributes from section 2.3 | since a menu-item can also execute scripts just like an action |

---

## 10. `<param>` — Input Parameter for `<action>`

```xml
<param name="var_name" label="Display Label" title="Sub-title" desc="Description"
       type="text" value="default" required="true" readonly="false"
       placeholder="Hint" maxlength="20"/>
```

### 10.1. General Attributes for All `<param>` Nodes

| Attribute | Notes |
|---|---|
| `name` | **Required, must be unique.** The variable name `$name` utilized inside the action's script |
| `label` | Label displayed next to the input field |
| `title` | Sub-title displayed above |
| `desc` | Description |
| `value` | Default value (static) |
| `value-sh` / `value-su` | Default value dynamically fetched from shell |
| `type` | Input type — see table 10.2 |
| `placeholder` | Hint text displayed inside an empty field |
| `required` | `"true"`/`"1"`/`"required"` → Input mandatory, cannot be left blank |
| `readonly` | `"readonly"`/`"true"`/`"1"` → Read-only, modification disabled |
| `maxlength` | Maximum character length (used for text inputs) |
| `min` / `max` | Value boundaries (used for `type="int"`/`"number"`/`"seekbar"`) |
| `support` / `visible` | Visibility condition script — if not `"1"`, this param is **completely omitted** (no row generated) |
| `multiple` | `"multiple"`/`"true"`/`"1"` → Allows multiple value selection (used with `type="list"`/`"app"`/`"packages"`) |
| `separator` | Character used to join multiple selected values (default: `
`) |
| `editable` | Allows manual typing of values (used with `type="file"`/`"folder"`) |
| `suffix` | Allowed file extension for selection (used with `type="file"`), also infers `mime` if not explicitly declared |
| `mime` | Allowed MIME type for selection (used with `type="file"`) |
| `depend-on` / `depend` | Name(s) of the parent param(s) controlling the visibility of this row — see section 10.4 |
| `depend-value` | Value(s) required to match for visibility — see section 10.4 |
| `depend-mode` | `show` (default) or `hide` — see section 10.4 |

### 10.2. Supported `type` Values

| `type` | Display Widget | Notes |
|---|---|---|
| *(undeclared, default)* | Text input field | |
| `int`, `number` | Numeric input field | Validates against `min`/`max` |
| `color` | Hex color code input + color palette picker button | |
| `bool`, `checkbox` | Checkbox | `value`/`value-sh` = `"1"`/`"true"` → Checked by default |
| `switch` | Toggle switch | Same as above |
| `seekbar` | Seekbar slider + `+`/`−` adjustment buttons | Utilizes `min`/`max` |
| `file` | File picker | Utilizes `suffix`/`mime`, `editable` |
| `folder` | Folder picker | Utilizes `editable` |
| `app`, `packages` | Selection list of installed applications (single or multiple if `multiple="true"`) | Can restrict the allowed app list using `option-sh` (returning `packageName|Display Title`) |
| `list` *(requires `option-sh`/`options-sh` or nested `<option>` tags)* | Dropdown/Spinner (≤6 selections) or selection dialog (>6 selections); dialog supporting multi-select if `multiple="true"` | |

### 10.3. Declaring Options Lists (For `type="list"` or App Filters)

**Method 1 — Dynamic via shell** (each line formatted as `value|title`):
```xml
<param name="mode" type="list" option-sh="echo -e 'a|A
b|B (so)
c|C'"/>
```

**Method 2 — Static via child `<option>` tags:**
```xml
<param name="mode" type="list">
    <option val="a">A</option>
    <option val="b">B (so)</option>
    <option val="c">C</option>
</param>
```
(`val`/`value` represents the actual value; if omitted, the text content itself is treated as the value)

### 10.4. `depend-on` / `depend-value` / `depend-mode` — Conditional Visibility Based on Other Params

**Basic Syntax (1 parent param):**
```xml
<param name="cam" type="list" option-sh="..." depend-on="mode" depend-value="b" depend-mode="show"/>
```
→ The `cam` row only appears when `mode` is currently set to `b`. Setting `depend-mode="hide"` reverses this logic (hides when matched, shows when unmatched).

`depend-value` can list **multiple accepted values** (comma-separated, matching via OR logic):
```xml
depend-value="b,c"   <!-- Matches if value = b OR c -->
```

**Multiple Parent Params Simultaneously** (separated by `|`; each position maps to a parent param, all conditions must evaluate to true — AND logic):
```xml
<param name="xom" ... depend-on="mode|cam" depend-value="a|b" depend-mode="show|hide"/>
<!-- Visible when: mode = a  AND  cam is NOT b -->

<param name="zum" ... depend-on="mode|cam|xom" depend-value="a|b|b,c" depend-mode="show|hide|show"/>
<!-- Visible when: mode = a  AND  cam is NOT b  AND  xom is either b or c -->
```

**So khớp mở rộng theo option** — `depend-value` không chỉ so với `value` thực tế của lựa chọn đang chọn ở param cha, mà còn khớp được với **title** hiển thị của option đó, và với **phần văn bản nằm trong dấu ngoặc `()`** của title (cả có ngoặc lẫn không ngoặc):
```xml
<param name="mode" type="list" option-sh="echo -e 'a|A (so)
b|B
c|C'"/>
<param name="ex" ... depend-on="mode" depend-value="a,A,(so)"/>
<!-- Matches when the chosen option in "mode" has a value of "a" (value), OR "A" (title),
     OR "(so)"/"so" (the bracketed text inside the title) -->
```

Cơ chế này áp dụng cho **mọi type** có thể làm param cha: `list` (đơn/đa), `bool`/`checkbox`, `switch`, `seekbar`, `file`/`folder`, `app`/`packages`, `color`, và input số/văn bản mặc định — tất cả đều cập nhật ẩn/hiện ngay lập tức khi giá trị thay đổi (không cần đóng dialog/màn hình lại).

---

## 11. `@string/string_name` Syntax

Wherever static text is accepted (`title`, `desc`, `<slice>` body content, `<option>` body content, etc.), you can reference a string resource declared in `res/values*/strings.xml` instead of hardcoding text:

```xml
<title>@string/app_name</title>
```

If the corresponding resource cannot be located, the raw string `@string/...` will be retained as-is without crashing.

---

## 12. `<resource>` — Extract Pre-packaged Assets

```xml
<resource file="scripts/backup.sh"/>
<resource dir="scripts/"/>
```
Used to extract a single file or an entire directory from assets into the device storage prior to use (for instance, a shell script that needs to be executed). It can be positioned anywhere inside `page`/`action`/`switch`/`picker`/`text`.

---

## 13. Comprehensive Example

```xml
<page>
    <title>Demo Page</title>

    <group title="Information">
        <text>
            <title>Introduction</title>
            <slice bold="true" color="#2196F3">KR Script Configuration Demo App</slice>
            <slice sh="date" break="true">Current Time:</slice>
        </text>
    </group>

    <group title="Options">
        <switch title="Enable Dark Mode">
            <get>settings get system ui_night_mode</get>
            <set>settings put system ui_night_mode "$1"</set>
        </switch>

        <picker title="Priority" name="priority" multiple="false">
            <option val="low">Low</option>
            <option val="high">High</option>
            <getstate>echo low</getstate>
            <setstate>echo "Selected: $1"</setstate>
        </picker>
    </group>

    <group title="Actions">
        <action title="Run Task" confirm="true" warning="Are you sure?">
            <param name="mode" label="Select Mode" type="list" value="a"
                   option-sh="echo -e 'a|A
b|B'"/>

            <param name="cam" label="CAM" type="list"
                   option-sh="echo -e 'a|A
b|B'"
                   depend-on="mode" depend-value="b" depend-mode="show"/>

            <param name="level" label="Level" type="seekbar" value="5" min="0" max="10"/>

            <param name="target_app" label="Application" type="app"/>

            <param name="advanced_hint" label="Show only in Advanced" type="text"
                   depend-on="mode|level" depend-value="b|7,8,9,10" depend-mode="show|show"/>

            <set>
                echo "mode=$mode cam=$cam level=$level app=$target_app"
            </set>
        </action>

        <editor title="Edit Log File" file="/sdcard/log.txt"/>
    </group>
</page>
```
