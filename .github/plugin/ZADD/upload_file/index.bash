#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home() {
echo '[[group]]
title = "'$google_text'"

  [[group.action]]
  title = "Upload Gofile"
  script = """
  set -o pipefail
  if [ -f "$FILE" ]; then
      urls="$(xem https://api.gofile.io/servers | jq -r .data.servers[0].name)"
      [ -z "$urls" ] && urls="upload.gofile.io" || urls="$urls.gofile.io"
      echo "'$gofile_text_1' $urls"
      echo
      curl -L -H "$WEBS" -F "file=@$FILE" "https://$urls/contents/uploadfile" 2>&1 | jq || killtree "'$gofile_text_2'"
      echo
      echo "'$gofile_text_3' $(jq -r .data.downloadPage "$TMP/Upload.log")"
      echo
      checktime
  else
      echo "'$gofile_text_5'"
  fi
  """

    [[group.action.params]]
    name = "FILE"
    type = "file"
    required = "true"
    desc = "https://gofile.io"

  [[group.action]]
  title = "Upload Pixeldrain"
  script = """
  set -o pipefail
  [ "$TEXT" ] || echo "'$gofile_text_6'"
  slog tocken_key_upload_free "$TEXT"
  if [ -f "$FILE" ]; then
      echo "'$gofile_text_1' pixeldrain.com"
      echo
      curl -T "$FILE" -u ":$TEXT" https://pixeldrain.com/api/file 2>&1 | jq -r .id | awk '"'{print \"https://pixeldrain.com/u/\"\$1}'"' || killtree "'$gofile_text_2'"
      echo
      echo "'$gofile_text_3' $(cat "$TMP/Upload.log")"
      echo
      checktime
  else
      echo "'$gofile_text_5'"
  fi
  """

    [[group.action.params]]
    name = "FILE"
    type = "file"
    required = "true"
    desc = "https://pixeldrain.com"

    [[group.action.params]]
    name = "TEXT"
    label = "Token"
    desc = "Token: xxx-xxx-xxx-xxx-xxx"
    value-sh = "glog tocken_key_upload_free"
    type = "text"
'
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
[ -f "$MPAT/language.bash" ] && source "$MPAT/language.bash"

# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ]; then
    trans_add "$MPAT"
    [ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

# index
"$@"
