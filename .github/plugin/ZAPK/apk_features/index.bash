#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home() {
[ "$ROT" == 0 ] && text_root="ROOT" || text_rr="$fs_text_1"
echo '
[[group]]
title = "'$google_text'"

  [[group.action]]
  title = "'$check_ufs_text'"
  summary = "'$text_root'"
  lock = """
  [ "$ROT" == 0 ] && echo "'$fs_text_3'" || echo 0
  """
  script = """
  export MPAT='$MPAT'
  '$MPAT'/scrip/ufs.bash
  """

[[group]]

  [[group.action]]
  title = "'$fs_text_2'"
  desc = "'$text_rr'"
  summary = "'$text_root'"
  lock = """
  [ "$ROT" == 0 ] && echo "'$fs_text_3'" || echo 0
  """
  script = """
  echo "'$fs_text_4'"
  echo
  fstrim -v /vendor
  fstrim -v /system
  fstrim -v /system_ext
  fstrim -v /product
  fstrim -v /cache
  fstrim -v /data
  echo
  echo "'$fs_text_4' auto"
  echo
  sm fstrim
  echo
  checktime
  """

[[group]]

  [[group.page]]
  html = "https://zenlua.github.io/Tool-Tree/website/web/terminal.html"
  title = "Web Terminal"

[[group]]

  [[group.page]]
  html = "https://zenlua.github.io/Tool-Tree/website/web/manager.html"
  title = "Web Manager"
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
