#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home() {
echo '
[[group]]
  title = "'$google_text'"

  [[group.action]]
  title = "'$home_text_1'"
  desc = "'$home_text_2'"
  script = """
    slog overlay_folder "$overlay_folder"
    '$MPAT'/overlay.bash
  """

    [[group.action.params]]
    name = "overlay_folder"
    desc = "'$home_text_7'"
    type = "folder"
    value-sh = "glog overlay_folder"
    required = true
    editable = true

  [[group.action]]
  title = "'$home_text_3'"
  desc = "'$home_text_4'"
  script = """
    slog extract_folder_lang "$extract_folder_lang"
    slog extract_folder_lang_text "$extract_folder_lang_text"
    '$MPAT'/extract.bash
  """

    [[group.action.params]]
    name = "extract_folder_lang"
    desc = "'$home_text_8'"
    type = "folder"
    value-sh = "glog extract_folder_lang"
    required = true
    editable = true

    [[group.action.params]]
    name = "extract_folder_lang_text"
    label = "'$home_text_5'"
    desc = "'$home_text_6'"
    placeholder = "values-vi,values-zh-rCN"
    type = "text"
    value-sh = "glog extract_folder_lang_text"
  '
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Ngôn ngữ & Google dịch
source trans_add "$MPAT"

# index add-on
"$@"
