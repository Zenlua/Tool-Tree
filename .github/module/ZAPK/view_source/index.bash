#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home() {
echo '
[[group]]
  title = "'$google_text'"

  [[group.action]]
  title = "Jadx"
  desc = "'$view_source_text_11'"
  warn = "'$view_source_text_10'"
  script = """
  [ "$no_res" == 1 ] && no_res_v="-r"
  [ "$no_dex" == 1 ] && no_dex_v="-s"
  jadx $no_res_v $no_dex_v -j 4 -d "${FILE%.*}_jadx" "$FILE" || killtree "'$view_source_text_13'"
  echo
  echo "'$view_source_text_12': ${FILE%.*}_jadx"
  echo
  checktime
  """
    
    [[group.action.params]]
    name = "no_res"
    title = "'$view_source_text_9'"
    label = "'$view_source_text_8'"
    type = "switch"

    [[group.action.params]]
    name = "no_dex"
    label = "'$view_source_text_7'"
    type = "switch"
    
    [[group.action.params]]
    name = "FILE"
    title = "'$view_source_text_6'"
    desc = "'$view_source_text_5': apk, dex, jar, class, smali, zip, aar, arsc, aab, xapk, apkm"
    type = "file"
    required = true
    editable = true
  '
}

jadx_install(){
echo "$view_source_text_1"
echo
websums="$(xem https://api.github.com/repos/skylot/jadx/releases/latest)"
sumon="$(echo "$websums" | jq -r '.assets[0].digest // empty' | cut -d: -f2)"
url_dowload="$(echo "$websums" | jq -r ".assets[0].browser_download_url // empty")"
if [[ "$sumon" ]] && [[ "$sumon" != "$(glog sum_jadx_online)" ]]; then
taive -# "$url_dowload" "$TMP/Jadx.zip" 2>&1
  if [[ "$sumon" == "$(checksum "$TMP/Jadx.zip")" ]]; then
  unzip -oj "$TMP/Jadx.zip" bin/jadx -d "$BIN"
  unzip -oj "$TMP/Jadx.zip" lib/* -d "$LIB"
  sed -i "s|#!/usr/bin/env sh|#!/data/data/com.tool.tree/files/home/bin/bash|" "$BIN/jadx"
  chmod 755 "$BIN/jadx"
  slog sum_jadx_online "$sumon"
  rm -rf "$TMP/Jadx.zip"
  echo
  echo "$view_source_text_2"
  else
  echo "$view_source_text_3"
  fi
else
  echo "$view_source_text_4"
fi
}

if [ ! -f "$BIN/jadx" ]; then
showtoast "$view_source_text_1"
jadx_install &>/dev/null
fi

# Ngôn ngữ & Google dịch
source langadd "$MPAT"
"$@"
