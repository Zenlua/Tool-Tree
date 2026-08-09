#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

urlpng() {
  if [ "$(glog Ticon)" != 1 ]; then
  if [ -f "$ETC/icon/$1_$DARK_MODE.png" ]; then
  echo "$ETC/icon/$1_$DARK_MODE.png"
  else
  echo "$ETC/icon/$1.png"
  fi
  fi
}

urladd() {
  if [ "$(glog Ticon)" != 1 ]; then
  if [ -f "$dirvad/$1_$DARK_MODE.png" ]; then
  echo "$dirvad/$1_$DARK_MODE.png"
  elif [ -f "$dirvad/$1.png" ]; then
  echo "$dirvad/$1.png"
  else
  echo "$ETC/icon/icon.png"
  fi
  fi
}

show_sett() {
  echo '
  [[group.action]]
  icon = "'`urlpng folder_rom`'"
  shell = "hidden"
  reload = "true"
  title = "'$input_folder_text'"
  summary = "'$path_text': '${PTSD/$SDCARD_PATH/\/sdcard}'"
  script = """
    if [ ! -d "$Folder" ] || [ ! -d "$SDH/${Folder##*/}" ]; then
    slog PTSD "$Folder"
    slog PTSH "${Folder##*/}"
    mkdir -p "$SDH/${Folder##*/}" "$Folder/out"
    elif [ -d "$SDH/$Name" ]; then
    slog PTSH "$Name"
    slog PTSD "$SDC/$Name"
    fi
  """

  [[group.action.params]]
  name = "Name"
  desc = "'$config_text_1'"
  label = "'$setting_text_3'"
  options-sh = "findfile for $SDH"
  value-sh = "glog PTSH"

  [[group.action.params]]
  name = "Folder"
  desc = "'$config_text_2'"
  value-sh = "glog PTSD"
  type = "folder"
  editable = "true"
  required = "true"
'
}

show_apkset() {
  echo '
  [[group.action]]
  icon = "'`urlpng folder_apk`'"
  shell = "hidden"
  reload = "true"
  title = "'$input_folder_text'"
  summary = "'$path_text': '${PTAD/$SDCARD_PATH/\/sdcard}'"
  script = """
    if [ ! -d "$Folder" ] || [ ! -d "$APK/${Folder##*/}" ]; then
    slog PTAD "$Folder"
    slog PTAH "${Folder##*/}"
    mkdir -p "$APK/${Folder##*/}" "$Folder/out"
    elif [ -d "$APK/$Name" ]; then
    slog PTAH "$Name"
    slog PTAD "$SDC/$Name"
    fi
  """

  [[group.action.params]]
  name = "Name"
  desc = "'$config_text_1'"
  label = "'$setting_text_3'"
  options-sh = "findfile for $APK"
  value-sh = "glog PTAH"

  [[group.action.params]]
  name = "Folder"
  desc = "'$config_text_2'"
  value-sh = "glog PTAD"
  type = "folder"
  editable = "true"
  required = "true"
  '
}

shell_bash() {
  echo '[[group]]
  [[group.editor]]
  title = "'$home_text_5'"
  desc = "'$more_text_9'"
  file = "home/usr/run_'$1'.bash"
  need-input = "true"
  placeholder = "#!/data/data/com.tool.tree/files/home/bin/bash"
  icon = "'`urlpng shell`'" '
}

(
  # Tạo thư mục
  [ -d $PTAD/out ] && mkdir -p $PTAD/out &>/dev/null
  [ -d $PTSD/out ] && mkdir -p $PTSD/out &>/dev/null
  # Dịch ngôn ngữ
  [[ "$1" == "Home" || "$1" == "More" ]] && auto_trans &>/dev/null
) &

# Ngôn ngữ
source language 2>/dev/null
if [[ "$1" == "Home" || "$1" == "More" ]]; then
    # Thông tin
    text_id_1="$(glog show_infor_text_1 "ROOT: \$ROOT  |  Android: \$ANDROID_RELEASE  -  SDK: \$API  |  CPU: \$CPU_ABI")"
    text_id_2="$(glog show_infor_text_2 "\$trademark_text: \$ANDROID_BRAND  |  \$device_text: \$ANDROID_DEVICE  |  \$version_text: \$PACKAGE_VERSION_NAME")"
    [ -z "$text_id_2" ] || text_id_3="\n\n"
    Vip_text_infor="$(eval echo "\"${text_id_1}${text_id_3}${text_id_2}\"")"
fi

# Văn bản
Home() {
  if [ -n "$Vip_text_infor" ]; then
  echo -e '[[group]]
  [[group.text]]
  summary = """'"$Vip_text_infor"'""" '
  fi

  if [ -f "$AON/patch_rom/addon.prop" ]; then
  vdbfbfsn='
  [[group.page.options]]
  key = "v4"
  type = "checkbox"
  title = "Patch ROM"
  box = "glog hide_show_patch_rom"
  silent = true
  reload = true'
  fi

  echo '
  [[group]]
  [[group.page]]
  title = "'$setting_text'"
  desc = "'$home_text_1'"
  icon = "'`urlpng settings`'"
  config-sh = "'$ETC'/tool-tree.bash Info"
  handler = """
    if [ "$menu_id" == "v1" ]; then
    echo "am:[start -a android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -d package:com.tool.tree]"
    elif [ "$menu_id" == "v2" ]; then
    echo "am:[start -a android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -d package:com.tool.tree]"
    elif [ "$menu_id" == "v3" ]; then
    echo "am:[start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.tool.tree]"
    fi
  """

    [[group.page.options]]
    key = "v1"
    type = "default"
    title = "'$permis_text_1'"
    silent = true

    [[group.page.options]]
    key = "v2"
    type = "default"
    title = "'$permis_text_4'"
    silent = true

    [[group.page.options]]
    key = "v3"
    type = "default"
    title = "'$setting_text_5'"
    silent = true

  [[group]]
  [[group.page]]
  title = "'$utilities_text'"
  desc = "'$home_text_2'"
  icon = "'`urlpng utilities`'"
  config-sh = "'$ETC'/tool-tree.bash Utilities"
  handler = """
    if [ "$menu_id" == "v1" ]; then
    [ "$(glog hide_show)" == 1 ] && slog hide_show 0 || slog hide_show 1
    elif [ "$menu_id" == "v4" ]; then
    [ "$(glog hide_show_patch_rom)" == 1 ] && slog hide_show_patch_rom 0 || slog hide_show_patch_rom 1
    elif [ "$menu_id" == "v2" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/external_files${PTSD#$SDCARD_PATH}]"
    elif [ "$menu_id" == "v3" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/root$SDH/$PTSH]"
    fi
  """
    [[group.page.options]]
    key = "v1"
    type = "checkbox"
    title = "'$input_folder_text'"
    box = "glog hide_show"
    silent = true
    reload = true
    '"$vdbfbfsn"'

    [[group.page.options]]
    key = "v2"
    type = "default"
    title = "'$open_activity_text' ROM"
    silent = true

    [[group.page.options]]
    key = "v3"
    type = "default"
    title = "'$open_activity_text' (data-root)"
    silent = true

    [[group.page.options]]
    type = "default"
    title = "'$setting_text' - '$setting_text_3'"
    config-sh = "'$ETC'/tool-tree.bash Project"

  [[group]]
  [[group.page]]
  title = "'$tools_text'"
  desc = "'$home_text_3'"
  icon = "'`urlpng tools`'"
  config-sh = "'$ETC'/tool-tree.bash Root"

  [[group]]
  [[group.page]]
  title = "'$addon_text'"
  desc = "'$home_text_4'"
  icon = "'`urlpng addon`'"
  config-sh = "PATHADD=\"$AON\" '$ETC'/tool-tree.bash Addon"
  handler = """
    case "$menu_id" in
    hide) slog settadd 1 ;;
    xoa) slog settadd 2 ;;
    home) slog settadd 0 ;;
    file) installadd "$file" "$AON"; slog settadd 0 ;;
    esac
  """

    [[group.page.options]]
    type = "refresh"
    title = "'$refresh_text'"

    [[group.page.options]]
    key = "hide"
    type = "default"
    title = "'$hide_add_text'"
    silent = true
    reload = true

    [[group.page.options]]
    type = "default"
    title = "'$download_text'"
    link = "https://zenlua.github.io/Tool-Tree/website/Addon.html"
    silent = true

    [[group.page.options]]
    key = "xoa"
    type = "default"
    title = "'$deleted_text'"
    silent = true
    reload = true

    [[group.page.options]]
    key = "file"
    type = "file"
    title = "'$input_add_text'"
    suffix = "add,zip,7z"
    style = "fab"
    reload = true

    [[group.page.options]]
    key = "home"
    type = "default"
    title = "'$home_text'"
    silent = true
    reload = true
  '

  [ "$(glog shellc)" == 1 ] && shell_bash shellc
}

More() {
  if [ -n "$Vip_text_infor" ]; then
  echo -e '[[group]]
  [[group.text]]
  summary = """'"$Vip_text_infor"'""" '
  fi

  echo '
  [[group]]
  [[group.page]]
  title = "'$setting_text'"
  desc = "'$home_text_1'"
  icon = "'`urlpng settings`'"
  config-sh = "'$ETC'/tool-tree.bash Info"
  handler = """
    if [ "$menu_id" == "v1" ]; then
    echo "am:[start -a android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -d package:com.tool.tree]"
    elif [ "$menu_id" == "v2" ]; then
    echo "am:[start -a android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -d package:com.tool.tree]"
    elif [ "$menu_id" == "v3" ]; then
    echo "am:[start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.tool.tree]"
    fi
  """

    [[group.page.options]]
    key = "v1"
    type = "default"
    title = "'$permis_text_1'"
    silent = true

    [[group.page.options]]
    key = "v2"
    type = "default"
    title = "'$permis_text_4'"
    silent = true

    [[group.page.options]]
    key = "v3"
    type = "default"
    title = "'$setting_text_5'"
    silent = true

  [[group]]
  [[group.page]]
  title = "'$utilities_text'"
  desc = "'$more_text_6'"
  icon = "'`urlpng apk_utility`'"
  config-sh = "'$ETC'/tool-tree.bash Utiliapk"
  lock = "[ -f $LOG/javaww ] && echo \"'$boot_text_1'\" || echo 0"
  handler = """
    if [ "$menu_id" == "v1" ]; then
    [ "$(glog hide_show2)" == 1 ] && slog hide_show2 0 || slog hide_show2 1
    elif [ "$menu_id" == "b2" ]; then
    echo "Looking for system apk..."
    for mm in $(pm list package -s | cut -f2 -d:); do
    cggdccg="$(pm path $mm | cut -f2 -d:)"
    [ -f "$cggdccg" ] && apktool if "$cggdccg" 2>/dev/null | sed "/127.apk/d"
    done
    rm -fr $HOME/.local/share/apktool/framework/1.apk
    echo
    checktime
    elif [ "$menu_id" == "b3" ]; then
    echo "'$more_text_4' $file"
    echo
    unzip -oq $ETC/apktool.jar prebuilt/linux/aapt2 -d $TMP
    cp -rf "$file" $TMP/apktool.jar || killtree "File copy error"
    cd $TMP
    zip -qr apktool.jar prebuilt/*
    mv apktool.jar $ETC/apktool.jar
    rm -fr prebuilt
    elif [ "$menu_id" == "b4" ]; then
    echo "'$more_text_4' $file"
    echo
    apktool if "$file"
    elif [ "$menu_id" == "v2" ]; then
    echo "'$more_text_4' $file"
    echo
    [ "$(unzip -ql "$file" | grep -cm1 ".x509.pem")" == 1 ] || killtree "'$more_text_5' .x509.pem"
    [ "$(unzip -ql "$file" | grep -cm1 ".pk8")" == 1 ] || killtree "'$more_text_5' .pk8"
    unzip -oj "$file" *.x509.pem *.pk8 -d "$ETC/key"
    elif [ "$menu_id" == "v3" ]; then
    echo "'$more_text_4' $file"
    echo
    cp -rf "$file" $ETC/apkeditor.jar || killtree "File copy error"
    elif [ "$menu_id" == "v4" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/external_files${PTAD#$SDCARD_PATH}]"
    elif [ "$menu_id" == "v5" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/root$APK/$PTAH]"
    fi
  """

    [[group.page.options]]
    key = "v1"
    type = "checkbox"
    title = "'$input_folder_text'"
    box = "glog hide_show2"
    silent = true
    reload = true

    [[group.page.options]]
    key = "b2"
    type = "default"
    title = "'$framework_auto_text'"

    [[group.page.options]]
    key = "v2"
    type = "file"
    title = "'$more_text_3'"
    suffix = "zip"
    auto-off = true

    [[group.page.options]]
    key = "v3"
    type = "file"
    title = "'$more_text_10' apkeditor.jar"
    suffix = "jar"
    reload = true
    auto-off = true

    [[group.page.options]]
    key = "b3"
    type = "file"
    title = "'$more_text_10' apktool.jar"
    suffix = "jar"
    reload = true
    auto-off = true

    [[group.page.options]]
    key = "b4"
    type = "file"
    title = "'$more_text_10' framework"
    suffix = "apk"

    [[group.page.options]]
    key = "v4"
    type = "default"
    title = "'$open_activity_text' APK"
    silent = true

    [[group.page.options]]
    key = "v5"
    type = "default"
    title = "'$open_activity_text' (data-root)"
    silent = true

  [[group]]
  [[group.page]]
  title = "'$tools_text'"
  desc = "'$more_text_7'"
  icon = "'`urlpng tool_apk`'"
  config-sh = "'$ETC'/tool-tree.bash Troot"

  [[group]]
  [[group.page]]
  title = "'$addon_text'"
  desc = "'$more_text_8'"
  icon = "'`urlpng apk_addon`'"
  config-sh = "PATHADD=\"$AOK\" '$ETC'/tool-tree.bash Addon"
  handler = """
    case "$menu_id" in
    hide) slog settadd2 1 ;;
    xoa) slog settadd2 2 ;;
    home) slog settadd2 0 ;;
    file) installadd "$file" "$AOK"; slog settadd2 0 ;;
    esac
  """

    [[group.page.options]]
    type = "refresh"
    title = "'$refresh_text'"

    [[group.page.options]]
    key = "hide"
    type = "default"
    title = "'$hide_add_text'"
    silent = true
    reload = true

    [[group.page.options]]
    type = "default"
    title = "'$download_text'"
    link = "https://zenlua.github.io/Tool-Tree/website/Apkon.html"
    silent = true

    [[group.page.options]]
    key = "xoa"
    type = "default"
    title = "'$deleted_text'"
    silent = true
    reload = true

    [[group.page.options]]
    key = "file"
    type = "file"
    title = "'$input_add_text'"
    suffix = "add,zip,7z"
    style = "fab"
    reload = true

    [[group.page.options]]
    key = "home"
    type = "default"
    title = "'$home_text'"
    silent = true
    reload = true
  '

  [ "$(glog shellc)" == 1 ] && shell_bash shells
}

Info() {
  echo '
  [[group]]
  [[group.page]]
  title = "'$setting_text_1'"
  desc = "'$setting_text_2'"
  icon = "'`urlpng info`'"
  config-sh = "'$ETC'/tool-tree.bash Update"
  handler = """
    if [ "$menu_id" == "share" ]; then
      echo "am:[start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT https://zenlua.github.io/Tool-Tree]"
    elif [ "$menu_id" == "data" ]; then
      slog boot_ver_code 1
    elif [ "$menu_id" == "beta" ]; then
      echo "'$update_text_3'"
      echo
      if [ -f "$TMP/Tool-Tree.apk" ]; then
        openfile "$TMP/Tool-Tree.apk"
        exit
        else
        taive "https://github.com/Zenlua/Tool-Tree/releases/download/beta/Tool-Tree-beta.apk" "$TMP/Tool-Tree.apk" 2>&1
        openfile "$TMP/Tool-Tree.apk"
        echo
        echo "'$save_text' $TMP/Tool-Tree.apk"
      fi
    fi
  """

    [[group.page.options]]
    title = "'$google_translate_text'"
    box = "glog gg_trans_ver"
    reload = true
    silent = true
    type = "checkbox"
    script = """
      if [ "$(glog gg_trans_ver)" == 1 ]; then
      slog gg_trans_ver 0
      else
      slog gg_trans_ver 1
      fi
    """
    
    [[group.page.options]]
    key = "share"
    type = "default"
    title = "'$share_text'"
    silent = true
    
    [[group.page.options]]
    key = "beta"
    type = "default"
    title = "'$download_text' beta"
    
    [[group.page.options]]
    key = "data"
    auto-kill = true
    silent = true
    type = "default"
    title = "'$reset_data_text'"
    
    [[group.page.options]]
    type = "refresh"
    style = "fab"
    icon = "'$ETC'/icon/Loading.png"

  [[group]]
  [[group.page]]
  title = "'$setting_text_3'"
  desc = "'$setting_text_4'"
  icon = "'`urlpng project`'"
  config-sh = "'$ETC'/tool-tree.bash Project"

    [[group.page.options]]
    type = "refresh"
    title = "'$refresh_text'"
    
  [[group]]
  [[group.page]]
  title = "'$setting_text_7'"
  desc = "'$setting_text_8'"
  icon = "'`urlpng feature`'"
  config-sh = "'$ETC'/tool-tree.bash Feature"

    [[group.page.options]]
    link = "https://aistudio.google.com/api-keys"
    title = "'$generate_text' Gemini API"
    silent = true

  [[group]]
  [[group.picker]]
  title = "'$permis_text_2'"
  desc = "'$permis_text_5'"
  icon = "'`urlpng language`'"
  option-sh = """
  echo -e "|'$default_text'\nauto|'$google_translate_text'\nai|Gemini\nen|English\nvi|Việt nam\nru|Русский\nzh|简体中文\nhu|Hungarian\nid|Indonesia\nes|Spanish"
  """
  get = "glog language_kkts"
  set = """
    if [ "$state" == "auto" ]; then
      slog language_kkts "$state"
      auto_trans
      echo "exit:[restart]"
      exit
    elif [ "$state" == "ai" ]; then
      if transai -c; then
        slog language_kkts "$state"
        auto_trans
        echo "exit:[restart]"
        exit
      fi
    else
      slog language_kkts "$state"
      [ -f $ETC/lang/auto.sh ] && rm -fr $ETC/lang/auto.sh
      slog language "$state"
      echo "exit:[restart]"
      exit
    fi
  """

  [[group]]
  [[group.editor]]
  id = "shella"
  title = "'$home_text_5'"
  desc = "'$home_text_6'"
  icon = "'`urlpng shella`'"
  file = "home/usr/run_shella.bash"
  need-input = true
  placeholder = "#!/data/data/com.tool.tree/files/home/bin/bash"
  '
}

Update() {
  if [ ! -f $TEMP/update ]; then
  check_update &>/dev/null
  fi
  if [ -f $TEMP/update ]; then
  url_dowload="$(cat $TEMP/update 2>/dev/null)"
  show_update=1
  fi
  if [ "$(glog gg_trans_ver 1)" == 1 ] && [ -f $TEMP/version_trans.txt ]; then
  link_vers="version_trans.txt"
  else
  link_vers="version.txt"
  fi
  echo '
  [[group]]
  [[group.page]]
  title = "'$author_text'"
  icon = "'`urlpng like`'"
  html = "https://zenlua.github.io/Tool-Tree/website/Information.html"

  [[group]]
  [[group.page]]
  title = "'$update_text_5'"
  icon = "'`urlpng website`'"
  link = "https://zenlua.github.io/Tool-Tree"

  [[group]]
  [[group.action]]
  title = "'$update_text'"
  desc = "'$sizes_text': '$(cat $TEMP/size 2>/dev/null)'"
  icon = "'`urlpng update`'"
  warn = "'$use_network_text'"
  support = "echo '$show_update'"
  script = """
    echo "'$update_text_2'"
    if [[ -f "$TMP/Tool-Tree.apk" ]] && [[ "$(checksum "$TMP/Tool-Tree.apk")" == "$(cat $TEMP/sum 2>/dev/null)" ]]; then
    openfile "$TMP/Tool-Tree.apk"
    exit
    fi
    echo
    if [[ "'$show_update'" == 1 ]]; then
    taive "'$url_dowload'" "$TMP/Tool-Tree.apk" 2>&1 || killtree "\n'$load_text_2'"
    if [ -f "$TMP/Tool-Tree.apk" ]; then
    openfile "$TMP/Tool-Tree.apk"
    fi
    echo
    echo "'$save_text' $TMP/Tool-Tree.apk"
    else
    echo "'$update_text_4'"
    fi
  """

  [[group.text]]
  desc-sh = "cat $TEMP/'$link_vers' 2>/dev/null"
  [[group.text.rows]]
  photo = "'$ETC'/icon/tool-tree.jpg"
  '

}

Project() {
  mkdir -p $PTSD/out &>/dev/null &
  mkdir -p $PTAD/out &>/dev/null &

  # Thêm group
  echo "[[group]]"
  show_sett
  show_apkset

  echo '
  [[group]]
  [[group.action]]
  title = "'$project_text_3'"
  icon = "'`urlpng cleanup`'"
  warn = "'$project_text_4'"
  auto-off = true
  script = """
    for vl in $dels; do
    echo "Deleting the folder: $vl"
    rm -fr "$vl"
    done
  """

    [[group.action.params]]
    name = "dels"
    label = "'$option_text'"
    options-sh = "findfile folders $SDH/$PTSH; findfile folders $APK/$PTAH"
    multiple = true

  [[group]]
  [[group.action]]
  title = "'$customize_tools_text'"
  icon = "'`urlpng list_tool`'"
  shell = "hidden"
  reload = true
  script = """
    slog un_tool_ext4 "$un_tool_ext4"
    slog un_tool_erofs "$un_tool_erofs"
    slog un_tool_f2fs "$un_tool_f2fs"
    slog re_tool_ext4 "$re_tool_ext4"
    slog re_tool_erofs "$re_tool_erofs"
    slog re_tool_f2fs "$re_tool_f2fs"
  """

    [[group.action.params]]
    name = "un_tool_ext4"
    title = "'$tool_unpack_text' ext4"
    label = "'$option_text'"
    value-sh = "glog un_tool_ext4 0"
    options-sh = "echo -e \"0|imgextractor\n1|imgkit_scuti\""

    [[group.action.params]]
    name = "un_tool_erofs"
    title = "'$tool_unpack_text' erofs"
    label = "'$option_text'"
    value-sh = "glog un_tool_erofs 2"
    options-sh = "echo -e \"0|extract.erofs\n1|imgkit_scuti\n2|extract.erofs (old)\""

    [[group.action.params]]
    name = "un_tool_f2fs"
    title = "'$tool_unpack_text' f2fs"
    label = "'$option_text'"
    value-sh = "glog un_tool_f2fs 0"
    options-sh = "echo -e \"0|extract.f2fs\n1|imgkit_scuti\""

    [[group.action.params]]
    name = "re_tool_ext4"
    title = "'$tool_repack_text' ext4"
    label = "'$option_text'"
    value-sh = "glog re_tool_ext4 1"
    options-sh = "echo -e \"0|make_ext4fs\n1|mke2fs+e2fsdroid\n2|imgkit_scuti\""

    [[group.action.params]]
    name = "re_tool_erofs"
    title = "'$tool_repack_text' erofs"
    label = "'$option_text'"
    value-sh = "glog re_tool_erofs 2"
    options-sh = "echo -e \"0|mkfs.erofs\n1|imgkit_scuti\n2|mkfs.erofs (old)\""

    [[group.action.params]]
    name = "re_tool_f2fs"
    title = "'$tool_repack_text' f2fs"
    label = "'$option_text'"
    value-sh = "glog re_tool_f2fs 0"
    options-sh = "echo -e \"0|sload_f2fs\""
  '

  for vvsskk in $SDH/$PTSH/*; do
  [[ -d "$vvsskk" ]] && namept="${vvsskk##*/}" || continue
  [[ "$namept" == *config* || "$namept" == *raw* ]] && continue
  sload+='if [[ -z "$name_'$namept'" || "$name_'$namept'" == 0 ]]; then
  rm -rf $SDH/$PTSH/config/'$namept'_size.txt
  else
  echo "$name_'$namept'" > $SDH/$PTSH/config/'$namept'_size.txt
  fi
  '
  vbload+='
  [[group.action.params]]
  name = "name_'$namept'"
  type = "number"
  placeholder = "0"
  label = "'$namept'"
  value-sh = "cat $SDH/$PTSH/config/'$namept'_size.txt 2>/dev/null"
  '
  done
  
  if [ -n "$(ls -1d "$SDH/$PTSH"/* 2>/dev/null | grep -vE '/(raw|config)')" ]; then
  echo '[[group]]
  [[group.action]]
  title = "'$custom_size'"
  warn = "'$custom_size_desc'"
  icon = "'`urlpng size_icon`'"
  shell = "hidden"
  reload = true
  script = """
  '"$sload"'
  """
  '"$vbload"'
  '
  fi
  
}

Feature() {
  echo '
  [[group]]
  [[group.switch]]
  title = "'$project_text_5'"
  icon = "'`urlpng set_home`'"
  shell = "hidden"
  get = "glog Tset"
  set = "slog Tset $state"

  [[group.switch]]
  title = "'$project_text_7'"
  icon = "'`urlpng icon_off`'"
  shell = "hidden"
  reload = true
  get = "glog Ticon"
  set = "slog Ticon $state"

  [[group.switch]]
  title = "'$project_text_6'"
  icon = "'`urlpng shell_off`'"
  shell = "hidden"
  get = "glog shellc"
  set = "slog shellc $state"

  [[group]]
  [[group.action]]
  title = "'$project_text_10'"
  icon = "'`urlpng java`'"
  warn = "'$project_text_9'"
  shell = "hidden"
  script = "slog ramoccupied \"$ramoccupied\""

    [[group.action.params]]
    name = "ramoccupied"
    label = "'$option_text'"
    value-sh = "glog ramoccupied 4096"
    options-sh = "echo -e \"512\n1024\n2048\n3072\n4096\n5120\n6144\n7168\n8192\""

  [[group]]
  [[group.action]]
  title = "'$infor_text'"
  icon = "'`urlpng icon_info`'"
  shell = "hidden"
  script = """
    slog show_infor_text_1 "$show_infor_text_1"
    slog show_infor_text_2 "$show_infor_text_2"
  """

    [[group.action.params]]
    name = "show_infor_text_1"
    title = "Text 1"
    type = "text"
    value-sh = "glog show_infor_text_1"

    [[group.action.params]]
    name = "show_infor_text_2"
    title = "Text 2"
    type = "text"
    value-sh = "glog show_infor_text_2"

  [[group]]
  [[group.action]]
  title = "'$project_text_12'"
  icon = "'`urlpng cpu`'"
  warn = "'$project_text_13'"
  shell = "hidden"
  support = "command -v taskset &>/dev/null && echo 1"
  script = "slog use_cpu \"$use_cpus\""

    [[group.action.params]]
    name = "use_cpus"
    label = "'$option_text'"
    value-sh = "glog use_cpu"
    options-sh = "seq 1 $(nproc --all)"

  [[group]]
  [[group.action]]
  title = "'$project_text_14'"
  icon = "'`urlpng background`'"
  warn = "'$project_text_15'"
  shell = "hidden"
  auto-restart = true
  script = """
    slog dissblur "$dissblur"
    slog uri_change_background "$uri_change_background"
    [ -f "$uri_change_background" ] && cp -f "$uri_change_background" "$ETC/wallpaper.jpg"
    [ -z "$uri_change_background" ] && rm -f "$ETC/wallpaper.jpg"
    set_permis "$ETC/wallpaper.jpg" &>/dev/null
  """

    [[group.action.params]]
    name = "dissblur"
    label = "'$dissblur_text'"
    type = "bool"
    value-sh = "glog dissblur"

    [[group.action.params]]
    name = "uri_change_background"
    type = "file"
    suffix = "jpg"
    editable = true
    value-sh = "glog uri_change_background"
  
  [[group]]
  [[group.action]]
  title = "'$api_key_text'"
  warn = "'$note_genmini_text'"
  icon = "'`urlpng apikey`'"
  reload = true
  shell = "hidden"
  script = """
    [ -z "$models_genmini" ] && slog -d models_genmini || slog models_genmini "$models_genmini"
    [ -z "$api_genmini" ] || slog api_genmini "$(tokenenc "$api_genmini")"
  """
    [[group.action.params]]
    name = "api_genmini"
    title = "Gemini API"
    placeholder = "*******************"
    type = "text"
    desc-sh = "transai -c 2>&1"
    
    [[group.action.params]]
    name = "models_genmini"
    placeholder = "gemini-3.5-flash-lite"
    title = "Models Gemini"
    type = "text"
    value-sh = "glog models_genmini \"gemini-3.5-flash-lite\""
  '
}

Root() {
  echo '
  [[group]]
  [[group.action]]
  title = "'$mount_text_1'"
  summary = "'$show_root_text'"
  icon = "'`urlpng mount`'"
  lock = "[ \"$ROT\" == 0 ] && echo \"'$root_warning_text'\" || echo 0"
  interruptible = false
  script = """
    for kkh in $IMG_NAME; do
    if [ "$(ls $SDH/raw/${kkh%.*} 2>/dev/null)" ]; then
        su -mm -c umount -l $SDH/raw/${kkh%.*}
    fi
    mkdir -p $SDH/raw/${kkh%.*}
    if [ "$(checktype $PTSD/$kkh)" == "erofs" ]; then
    su -mm -c mount -r -t erofs $PTSD/$kkh $SDH/raw/${kkh%.*}
    else
    su -mm -c mount -w $PTSD/$kkh $SDH/raw/${kkh%.*}
    fi
    done
    echo "'$save_text' $SDH/raw"
  """

    [[group.action.params]]
    name = "IMG_NAME"
    desc = "'$mount_text_2'"
    options-sh = "findfile 3 $PTSD | grep -E \"(f2fs)|(ext)|(erofs)\""
    required = true
    multiple = true

  [[group.action]]
  title = "'$umount_text_1'"
  summary = "'$show_root_text'"
  icon = "'`urlpng umount`'"
  lock = "[ \"$ROT\" == 0 ] && echo \"'$root_warning_text'\" || echo 0"
  interruptible = false
  script = """
    for kkh in $IMG_NAME; do
    su -mm -c umount -l $SDH/raw/$kkh
    rm -fr $SDH/raw/$kkh
    if [ "$(checktype $PTSD/${kkh}.img)" == "ext" ]; then
    [ -f $PTSD/${kkh}.img ] && e2fsck -yf $PTSD/${kkh}.img
    elif [ "$(checktype $PTSD/${kkh}.img)" == "f2fs" ]; then
    [ -f $PTSD/${kkh}.img ] && fsck.f2fs -yf $PTSD/${kkh}.img
    fi
    done
    echo "'$umount_text_2'"
  """

    [[group.action.params]]
    name = "IMG_NAME"
    desc = "'$umount_text_3'"
    options-sh = "findfile 4 $SDH/raw"
    required = true
    multiple = true

  [[group]]
  [[group.action]]
  title = "'$backup_text_1'"
  summary = "'$show_root_text'"
  icon = "'`urlpng backup`'"
  lock = "[ \"$ROT\" == 0 ] && echo \"'$root_warning_text'\" || echo 0"
  interruptible = false
  script = """
    Extract=$PTSD/backup
    [[ ! -d "$Extract" ]] && mkdir -p "$Extract"
    for i in $IMG; do
    e=${i##*/}
    File="$Extract/${e}.img"
    if [[ ! -L $i ]]; then
    echo "'$backup_text_3' $e" >&2
    else
    echo "'$backup_text_4' $e"
    echo
    dd if="$i" of="$File" 2>&1
    echo
    fi
    done
    echo "'$save_text' $Extract"
  """

    [[group.action.params]]
    name = "IMG"
    label = "IMAGE"
    desc = "'$backup_text_2 $PTSD'"
    options-sh = "search_image"
    required = true
    multiple = true

  [[group.action]]
  title = "'$flash_text_1'"
  summary = "'$show_root_text'"
  icon = "'`urlpng flash`'"
  lock = "[ \"$ROT\" == 0 ] && echo \"'$root_warning_text'\" || echo 0"
  script = """
    e=${IMG##*/}
    if [ "$e" = "vendor" ] || [ "$e" = "system" ] || [ "$e" = "super" ]; then
    killtree "($e) '$flash_text_8'"
    fi
    echo "'$more_text_4' $Brush_in"
    echo
    if [ "$(checktype "$Brush_in")" == "space" ]; then
    simg2img "$Brush_in"
    fi
    if [[ -f "$Brush_in" ]]; then
    echo "Flash (${Brush_in##*/}) ➠ ($e)"
    echo
    dd if="$Brush_in" of="$IMG" 2>&1
    if [[ $CQ1 = 1 ]]; then
    echo
    echo "'$flash_text_9'..."
    for i in $(seq 4 -1 1); do
    echo $i
    sleep 1
    done
    reboot recovery
    fi
    if [[ $CQ = 1 ]]; then
    echo
    echo "'$flash_text_10'..."
    for i in $(seq 4 -1 1); do
    echo $i
    sleep 1
    done
    reboot
    fi
    else
    killtree "! ($Brush_in) '$flash_text_11' ($e)"
    fi
    echo
    echo "'$flash_text_12'"
  """

    [[group.action.params]]
    name = "CQ"
    label = "'$flash_text_2'"
    type = "checkbox"
    depend-on = "CQ1"
    depend-value = "1"
    depend-mode = "hide"
    depend-cascade = false
    depend-readonly = true

    [[group.action.params]]
    name = "CQ1"
    label = "'$flash_text_3'"
    type = "checkbox"
    depend-on = "CQ"
    depend-value = "1"
    depend-mode = "hide"
    depend-cascade = false
    depend-readonly = true

    [[group.action.params]]
    name = "IMG"
    title = "'$flash_text_4'"
    desc = "'$flash_text_5'"
    label = "IMAGE"
    options-sh = "search_image"
    required = true

    [[group.action.params]]
    name = "Brush_in"
    title = "'$flash_text_6'"
    desc = "'$flash_text_7'"
    type = "file"
    suffix = "img"
    editable = true
    required = true
  '
}

Troot() {
  echo '
  [[group]]
  [[group.action]]
  title = "'$dexopt_app_text'"
  summary = "'$show_root_text'"
  icon = "'`urlpng dexopt_app`'"
  lock = "[ \"$ROT\" == 0 ] && echo \"'$root_warning_text'\" || echo 0"
  script = """
    if [ "$bools" == 1 ]; then
    pm compile -v -a -m $name_dex_list
    echo
    checktime
    else
    for vv in $apps; do
    pm compile -v -f -m $name_dex_list $vv
    echo
    done
    checktime
    fi
  """

    [[group.action.params]]
    name = "name_dex_list"
    label = "'$option_text'"
    value = "speed-profile"
    options-sh = "echo -e \"everything\nspeed\nspeed-profile\nverify\""

    [[group.action.params]]
    name = "bools"
    label = "'$dexopt_app_text_2'"
    desc = "'$dexopt_app_text_3'"
    type = "checkbox"

    [[group.action.params]]
    name = "apps"
    desc = "'$dexopt_app_text_1'"
    type = "app"
    options-sh = "pm list package -3 | cut -f2 -d:"
    multiple = true

  [[group]]
  [[group.action]]
  title = "'$backups_text_2'"
  icon = "'`urlpng backup_apk`'"
  warn = "'$backups_text_1'"
  interruptible = false
  script = """
    for v in $Sapp; do
    patk="$(pm path $v | cut -f2 -d:)"
    patk22="$(pm path "$v" | cut -f2 -d: | head -n1)"
    pathvv="${patk22%/*}"
    hcdf="$(echo "$patk" | grep -c ".apk"$)"
    paptn="$(echo "$patk" | grep "base.apk"$)"
    if [[ -n "$paptn" ]]; then
    infor="$(apkeditor info -i "$paptn")"
    nameapk="$(echo "$infor" | grep -m1 "AppName" | cut -d\" -f2)"
    else
    nameapk="${pathvv##*/}"
    fi
    if [ "$hcdf" -ge 2 ]; then
    zip -j -r "$PTAD/${nameapk}.apks" $patk
    echo "'$save_text' $PTAD/${nameapk}.apks"
    else
    cp -rf "$patk" "$PTAD/${nameapk}.apk"
    echo "'$save_text' $PTAD/${nameapk}.apk"
    fi
    done
  """

    [[group.action.params]]
    name = "Sapp"
    label = "'$backups_text_3'"
    type = "app"
    multiple = true
  '
}

Generate() {
    # Thêm ẩn
    if [ "$(glog hide_show_generate)" == 1 ]; then
    echo "[[group]]"
    show_sett
    fi

  echo '
  [[group]]
  [[group.action]]
  title = "'$generate_text' Payload"
  icon = "'`urlpng build_payload`'"
  script = """
    slog sign_payload "$sign_payload"
    slog payload_switch "$payload_switch"
    slog payload_super_size "$payload_super_size"
    slog payload_super_group "$payload_super_group"
    payload_repack -m "$IMAGES" -i "$PTSD" -s "$sign_payload" -w "$payload_switch" -e "$payload_super_size" -g "$payload_super_group"
    echo
    checktime
  """

    [[group.action.params]]
    name = "payload_switch"
    label = "'$payload_text_3'"
    type = "switch"
    value-sh = "glog payload_switch"

    [[group.action.params]]
    name = "payload_super_size"
    label = "'$sizes_text'"
    desc = "'$default_text': 11GB, '$payload_text_4'"
    type = "number"
    value-sh = "glog payload_super_size 11"
    required = true
    depend-on = "payload_switch"
    depend-value = "0"
    depend-mode = "hide"
    depend-readonly = true

    [[group.action.params]]
    name = "payload_super_group"
    label = "'$super_text_5'"
    desc = "'$super_text_6', '$payload_text_4'"
    value-sh = "glog payload_super_group qti_dynamic_partitions"
    required = true
    depend-on = "payload_switch"
    depend-value = "0"
    depend-mode = "hide"
    depend-readonly = true

    [[group.action.params]]
    name = "sign_payload"
    label = "'$sign_text'"
    value-sh = "glog sign_payload testkey"
    options-sh = "findfile file $ETC/key/2048 .pem | sed \"s|.pem||\""

    [[group.action.params]]
    name = "IMAGES"
    desc = "'$payload_text_2'"
    options-sh = "findfile 11 $PTSD"
    required = true
    multiple = true

  [[group]]
  [[group.action]]
  title = "'$generate_text' Amlogic"
  icon = "'`urlpng build_amlogic`'"
  script = """
    slog amlogic_boolbox "$amlogic_boolbox"
    slog amlogic_ver "$amlogic_ver"
    echo "'$apkb_text_1' $PTSD/$FOLDER"
    echo
    if [ "$(checktype "$PTSD/$FOLDER/super.img")" == "super" ]; then
    echo "'$unpack_text_0' super(raw) ➠ super(sparse)..."
    img2simg "$PTSD/$FOLDER/super.img"
    echo
    fi
    if [ -n "$(ls -1d $PTSD/$FOLDER/*.img 2>/dev/null)" ]; then
    for vv in $PTSD/$FOLDER/*.img; do
    echo "mv: ${vv##*/} ➠ $(echo "${vv##*/}" | sed "s|\\.img$|.PARTITION|")"
    mv "$vv" "${vv%.*}.PARTITION"
    done
    echo
    fi
    ampack pack --verify --out-ver $amlogic_ver --out-align $amlogic_align "$PTSD/$FOLDER" "$PTSD/out/$FOLDER.img" | tee $TMP/amlogic_pack.log
    [ "$?" == 1 ] && bug_rom=1 || bug_rom=0
    for vv in $(ls -1d $PTSD/$FOLDER/*.PARTITION 2>/dev/null); do
    mv "$vv" "${vv%.*}.img"
    done
    [ "$bug_rom" == 1 ] && killtree "Log check error: $TMP/amlogic_pack.log"
    [ "$amlogic_boolbox" == 1 ] && rm -fr "$PTSD/$FOLDER"
    echo "'$unpack_text_2' $TMP/amlogic_pack.log"
    echo
    echo "'$save_text' $PTSD/out/$FOLDER.img"
    echo
    checktime
  """

    [[group.action.params]]
    name = "amlogic_boolbox"
    label = "'$deleted_project_text'"
    type = "checkbox"
    value-sh = "glog amlogic_boolbox"

    [[group.action.params]]
    name = "amlogic_ver"
    label = "'$version_text'"
    value-sh = "glog amlogic_ver v2"
    options-sh = "echo -e \"v2\nv1\""

    [[group.action.params]]
    name = "amlogic_align"
    label = "'$alignment_text'"
    value-sh = "glog amlogic_align 8"
    options-sh = "echo -e \"4|4\n8|8 (Android 11+)\""

    [[group.action.params]]
    name = "FOLDER"
    desc = "'$builds_text_1'"
    options-sh = "findfile file $PTSD platform.conf | sed \"s|/platform.conf||\""
    required = true
    multiple = true
  '
}

Utilities() {
    [ -d $PTSD/out ] && mkdir -p $PTSD/out &>/dev/null &
    time_riviu="$(date -d "@`glog build_times 1230768000`")"

    if [ "$(glog hide_show 1)" == 1 ]; then
        echo "[[group]]"
        show_sett
    else
        desc_rom="$path_text: $(glog PTSD | sed "s|$SDCARD_PATH|\/sdcard|")"
        desc_rom1="$projects_text: $PTSH"
    fi

  echo '
  [[group]]
  [[group.action]]
  title = "'$decompile_text'"
  desc = "'$desc_rom'"
  icon = "'`urlpng decom`'"
  script = """
    slog vavbbgdf "$vavb"
    slog xoa_oat_boot "$xoa_oat_boot"
    slog dkjdj "$nounpak"
    slog pcvbmeta "$pcvbmeta"
    slog dkhdh "$cboxk"
    slog text_oat_boot "$text_oat_boot"
    for vkl in $IMAGES; do
    if [ -f "$PTSD/${vkl#*=}" ]; then
    unpack_img -i "$PTSD/${vkl#*=}" -p "${vkl%%=*}" -o "$SDH/$PTSH" -n $nounpak -d $cboxk -r $xoa_oat_boot -a $vavb -m $pcvbmeta
    else
    unpack_img -i "$PTSD/$vkl" -o "$SDH/$PTSH" -n $nounpak -d $cboxk -r $xoa_oat_boot -a $vavb -m $pcvbmeta
    fi
    done
    checktime
  """

    [[group.action.params]]
    name = "cboxk"
    label = "'$deleted_file_text'"
    type = "checkbox"
    value-sh = "glog dkhdh"

    [[group.action.params]]
    name = "nounpak"
    label = "'$decode_text_1'"
    type = "switch"
    value-sh = "glog dkjdj"

    [[group.action.params]]
    name = "xoa_oat_boot"
    label = "'$xoaoat_text_1'"
    type = "switch"
    value-sh = "glog xoa_oat_boot"
    depend-on = "nounpak"
    depend-value = "1"
    depend-mode = "hide"
    depend-readonly = true

    [[group.action.params]]
    name = "text_oat_boot"
    type = "text"
    value-sh = "glog text_oat_boot \"oat,vdex,odex,prof,bprof,fsv_meta\""
    depend-on = "xoa_oat_boot"
    depend-value = "1"
    depend-mode = "show"
    depend-default = "hide"

    [[group.action.params]]
    name = "vavb"
    label = "'$builds_text_8'"
    type = "switch"
    depend-on = "nounpak"
    depend-value = "1"
    depend-mode = "hide"
    depend-readonly = true

    [[group.action.params]]
    name = "pcvbmeta"
    label = "'$patch_text' vbmeta"
    value-sh = "glog pcvbmeta 0"
    options-sh = "echo -e \"0|'$default_text'\n1|'$disable_text' dm-verity\n2|'$disable_text' Verification\n3|'$disable_text' dm-verity + Verification\""
    depend-on = "nounpak"
    depend-value = "1"
    depend-mode = "hide"
    depend-readonly = true
    
    [[group.action.params]]
    name = "IMAGES"
    desc = "'$input_file_text': br, dat, img, zst, zstd, bin, zip"
    options-sh = "findfile 2 $PTSD"
    required = true
    multiple = true

  [[group.action]]
  title = "'$build_text'"
  desc = "'$desc_rom1'"
  icon = "'`urlpng build`'"
  script = """
    slog dang_nen "$dang_nen"
    slog format_imgs "$format_imgs"
    slog boolboxdjh "$boolbox"
    slog dinh_dang "$dinh_dang"
    slog build_size "$build_size"
    slog offfscontex "$offfscontex"
    slog muc_nen "$muc_nen"
    slog nen_br "$nen_br"
    slog build_times "$build_times"
    for vkl in $IMAGES; do
        repack_img -i "$SDH/$PTSH/$vkl" -o "$PTSD/out" -n "$dang_nen" -l "$muc_nen" -k "$dinh_dang" -s "$build_size" -d "$boolbox" -c "$format_imgs" -p "$offfscontex"
    done
    echo "'$save_text' $PTSD/out"
    echo
    checktime
  """

    [[group.action.params]]
    name = "boolbox"
    label = "'$deleted_project_text'"
    type = "checkbox"
    value-sh = "glog boolboxdjh"

    [[group.action.params]]
    name = "IMAGES"
    title = "'$list_partition_text'"
    desc = "'$builds_text_1'"
    options-sh = "findfile 0 $SDH/$PTSH"
    required = true
    multiple = true

    [[group.action.params]]
    name = "dinh_dang"
    label = "'$build_text'"
    desc = "'$builds_text_2'"
    value-sh = "glog dinh_dang 0"
    options-sh = "echo -e \"0|'$default_text'\n1|RO (EROFS)\n2|RW (EXT4)\n3|RO (F2FS)\n4|RW (F2FS)\""
    depend-on = "IMAGES"
    depend-value = "(erofs),(ext),(f2fs)"
    depend-mode = "show"
    depend-default = "hide"

    [[group.action.params]]
    name = "dang_nen"
    label = "'$option_text'"
    desc = "'$builds_text_3'"
    value-sh = "glog dang_nen lz4hc"
    options-sh = "echo -e \"lz4hc\nlz4\nlzma\ndeflate\nzstd\""
    depend-on = "dinh_dang|dinh_dang|IMAGES"
    depend-value = "EROFS|EXT4,F2FS|(erofs)"
    depend-mode = "show|hide|show"
    depend-logic = "priority"
    depend-default = "hide"

    [[group.action.params]]
    name = "muc_nen"
    label = "'$builds_text_4'"
    desc = "'$builds_text_6': lz4: 0, lz4hc: 0-12, deflate,lzma: 0-9, zstd: 0-22"
    type = "seekbar"
    min = 0
    max = 22
    value-sh = "glog muc_nen 8"
    depend-on = "dang_nen"
    depend-value = "lz4"
    depend-mode = "hide"
    depend-logic = "priority"

    [[group.action.params]]
    name = "format_imgs"
    label = "'$convert_text'"
    desc = "'$convert_img_text'"
    value-sh = "glog format_imgs raw"
    depend-on = "IMAGES"
    depend-value = "(erofs),(ext),(f2fs)"
    depend-mode = "show"
    depend-readonly = true
    options-sh = """
    echo -e "raw|File.img (raw)\nsparse|File.img (sparse)\nzstd|File.img.zstd\nzst|File.img.zst\ndat|File.new.dat\nbr|File.new.dat.br"
    """

    [[group.action.params]]
    name = "nen_br"
    label = "'$builds_text_4'"
    desc = "'$convert_text_2'"
    type = "seekbar"
    min = 0
    max = 22
    value-sh = "glog nen_br 4"
    required = true
    depend-on = "format_imgs"
    depend-value = "raw,sparse,File.new.dat"
    depend-mode = "hide"

    [[group.action.params]]
    name = "build_times"
    label = "'$time_text'"
    desc = "'$build_time_text_1': '$time_riviu'"
    type = "number"
    value-sh = "glog build_times"
    required = true
    depend-on = "IMAGES"
    depend-value = "(erofs),(ext),(f2fs)"
    depend-mode = "show"
    depend-readonly = true

    [[group.action.params]]
    name = "offfscontex"
    label = "'$patch_text_fscontex'"
    desc = "'$patch_text_fsdesc'"
    type = "switch"
    value-sh = "glog offfscontex 1"
    depend-on = "IMAGES"
    depend-value = "(erofs),(ext),(f2fs)"
    depend-mode = "show"
    depend-readonly = true

    [[group.action.params]]
    name = "build_size"
    label = "'$sizes_text'"
    desc = "'$builds_text_7'"
    type = "number"
    value-sh = "glog build_size 0"
    required = true
    depend-on = "dinh_dang|dinh_dang|IMAGES"
    depend-value = "EROFS|EXT4,F2FS|(ext),(f2fs)"
    depend-mode = "hide|show|show"
    depend-logic = "priority"
    depend-default = "hide"

  [[group]]
  [[group.action]]
  title = "'$convert_text_1'"
  icon = "'`urlpng convert_file`'"
  script = """
    slog format_img "$format_img"
    slog nen_br "$nen_br"
    slog cboxksbhd "$cboxk"
    for vinput in $IMAGES; do
    cover_img -i "$PTSD/$vinput" -o "$PTSD/out" -c $format_img -l $nen_br -d $cboxk
    done
    echo "'$save_text' $PTSD/out"
    echo
    checktime
  """

    [[group.action.params]]
    name = "cboxk"
    label = "'$deleted_file_text'"
    type = "checkbox"
    value-sh = "glog cboxksbhd"

    [[group.action.params]]
    name = "format_img"
    label = "'$option_text'"
    value-sh = "glog format_img raw"
    required = true
    options-sh = """
    echo -e "raw|File.img (raw)\nsparse|File.img (sparse)\ndat|File.new.dat\nbr|File.new.dat.br\nzstd|File.img.zstd\nzst|File.img.zst\nlzma|File.img.lzma\nlz4|File.img.lz4\nxz|File.img.xz\ngz|File.img.gz"
    """

    [[group.action.params]]
    name = "nen_br"
    label = "'$builds_text_4'"
    desc = "'$convert_text_2'"
    type = "seekbar"
    min = 0
    max = 22
    value-sh = "glog nen_br 4"
    required = true
    depend-on = "format_img"
    depend-value = "raw,sparse,File.new.dat"
    depend-mode = "hide"
    depend-readonly = true

    [[group.action.params]]
    name = "IMAGES"
    desc = "'$input_file_text': br, dat, zstd, img"
    options-sh = "findfile 1 $PTSD"
    required = true
    multiple = true

  [[group]]
  [[group.action]]
  title = "'$generate_text' Super"
  icon = "'`urlpng build_super`'"
  script = """
    slog typeheh "$type"
    slog fromdjfh "$from"
    slog super_sizedj "$super_size"
    slog super_group "$super_group"
    repack_super -m "$IMAGES" -g "$super_group" -s "$super_size" -f "$from" -t "$type" -i "$PTSD"
    echo
    checktime
  """

    [[group.action.params]]
    name = "type"
    label = "'$super_text_2'"
    value-sh = "glog typeheh VAB"
    options-sh = "echo -e \"A|a_only\nAB|ab\nVAB|virtual_ab\""

    [[group.action.params]]
    name = "from"
    label = "'$super_text_3'"
    value-sh = "glog fromdjfh raw"
    options-sh = "echo -e \"raw|raw\nsparse|sparse\""

    [[group.action.params]]
    name = "super_size"
    label = "'$sizes_text'"
    desc = "'$default_text': 8.5GB"
    type = "number"
    value-sh = "glog super_sizedj 8.5"
    required = true

    [[group.action.params]]
    name = "super_group"
    label = "'$super_text_5'"
    desc = "'$super_text_6'"
    value-sh = "glog super_group qti_dynamic_partitions"
    required = true

    [[group.action.params]]
    name = "IMAGES"
    desc = "'$super_text_7'"
    options-sh = "findfile 3 $PTSD"
    required = true
    multiple = true
    
  [[group.action]]
  title = "'$super_split_text_1'"
  icon = "'`urlpng super_split`'"
  script = """
    slog cboxkshg "$cboxk"
    slog slipdhhe "$slipdhhe"
    slog khoi_dau_dem "$khoi_dau_dem"
    echo "'$super_split_text_4' ${IMAGES}..."
    echo
    if [ $(checktype "$PTSD/$IMAGES") == "sparse" ]; then
    simg2img "$PTSD/$IMAGES"
    fi
    size_super_mb=$(($(stat -c %s "$PTSD/$IMAGES") / 1048576))
    chunk_size_mb=$((size_super_mb / slipdhhe))
    echo "${size_super_mb}M ÷ $slipdhhe = ${chunk_size_mb}M"
    echo
    chunk_split -s .cache.%02d -B 4K -C "$chunk_size_mb"M "$PTSD/$IMAGES"
    [ "$khoi_dau_dem" == 1 ] && sonum=1 || sonum=0
    cd "$PTSD"
    for vcd in ${IMAGES}.cache.*; do
    if [ -f "$vcd" ]; then
    echo "$vcd ➠ ${IMAGES}.$sonum"
    mv "$vcd" "out/${IMAGES}.$sonum"
    sonum=$((sonum + 1))
    fi
    done
    echo
    [ "$cboxk" == 0 ] || rm -fr "$PTSD/$IMAGES"
    echo "'$save_text' $PTSD/out"
    echo
    checktime
  """

    [[group.action.params]]
    name = "cboxk"
    label = "'$deleted_file_text'"
    type = "checkbox"
    value-sh = "glog cboxkshg"

    [[group.action.params]]
    name = "slipdhhe"
    label = "'$number_text'"
    title = "'$split_number_desc'"
    type = "number"
    min = 2
    max = 50
    value-sh = "glog slipdhhe 9"
    required = true

    [[group.action.params]]
    name = "khoi_dau_dem"
    label = "'$split_number_label'"
    type = "switch"
    value-sh = "glog khoi_dau_dem 0"

    [[group.action.params]]
    name = "IMAGES"
    label = "'$option_text'"
    title = "'$super_split_text_3'"
    options-sh = "findfile 7 $PTSD"
    required = true
  
  [[group.action]]
  title = "'$super_merge_text_1'"
  icon = "'`urlpng super_merge`'"
  script = """
    slog silence $silence
    echo "'$super_merge_text_2'..."
    simg2img $MERGE "$PTSD/super.img" || killtree "Error" "$PTSD/super.img"
    [ "$silence" == 0 ] || rm -fr $MERGE
    echo
    echo "'$save_text' $PTSD/super.img"
    echo
    checktime
  """

    [[group.action.params]]
    name = "silence"
    label = "'$deleted_file_text'"
    type = "checkbox"
    value-sh = "glog silence 1"

    [[group.action.params]]
    name = "MERGE"
    desc = "'$super_merge_text_3'"
    options-sh = "findfile 5 $PTSD | sort -n -t . -k 3"
    required = true
    multiple = true
  
  [[group]]
  [[group.page]]
  title = "'$synthetic_text'"
  icon = "'`urlpng generate`'"
  config-sh = "'$ETC'/tool-tree.bash Generate"
  handler = """
    if [ "$menu_id" == "v1" ]; then
    [ "$(glog hide_show_generate)" == 1 ] && slog hide_show_generate 0 || slog hide_show_generate 1
    fi
  """
    [[group.page.options]]
    key = "v1"
    type = "checkbox"
    title = "'$input_folder_text'"
    box = "glog hide_show_generate"
    silent = true
    reload = true
  '

  if [ "$(glog hide_show_patch_rom 1)" == 1 ] && [ -f "$AON/patch_rom/addon.prop" ]; then
  dirvad="$AON/patch_rom"
  echo '
  [[group]]
  [[group.page]]
  title = "Patch ROM"
  icon = "'`urladd icon`'"
  config-sh = "'$dirvad'/index.bash home"
  handler = """
    if [ "$menu_id" == "123" ]; then
        '$dirvad'/index.bash update_addon
    fi
  """

    [[group.page.options]]
    type = "refresh"
    title = "'$refresh_text'"

    [[group.page.options]]
    key = "123"
    type = "default"
    title = "'$update_text' add-on"
    auto-finish = true
  '
    fi
}

Apex() {
    if [ "$(glog hide_show_apex)" == 1 ]; then
    echo "[[group]]"
    show_apkset
    else
    desc_apkd="$path_text: $(glog PTAD | sed "s|$SDCARD_PATH|\/sdcard|")"
    desc_apkd1="$projects_text: $PTAH"
    fi

  echo '
  [[group]]
  [[group.action]]
  title = "'$decompile_text'"
  desc = "'$desc_apkd'"
  icon = "'`urlpng decom`'"
  script = """
    IFS=$'"'\n'"'
    for vv in $FILE; do
        apexeditor d -i "$PTAD/$vv" -o "$APK/$PTAH"
        echo
    done
    echo "'$save_text' $APK/$PTAH"
    echo
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    desc = "'$apex_text_2'"
    options-sh = "findfile 12 $PTAD"
    required = true
    multiple = true

  [[group.action]]
  title = "'$build_text'"
  desc = "'$desc_apkd1'"
  icon = "'`urlpng build`'"
  script = """
    slog gobo_apex "$gobo_apex"
    slog nen_apex "$nen_apex"
    slog payload_type "$payload_type"
    slog signs_apex "$SIGNS"
    IFS=$'"'\n'"'
    for vv in $FILE; do
        apexeditor b -d "$gobo_apex" -k "$SIGNS" -c "$nen_apex" -i "$APK/$PTAH/$vv" -o "$PTAD/out"
        echo
    done
    checktime
  """

    [[group.action.params]]
    name = "gobo_apex"
    label = "'$deleted_project_text'"
    type = "checkbox"
    value-sh = "glog gobo_apex"

    [[group.action.params]]
    title = "'$apex_text_1'"
    name = "nen_apex"
    label = "'$option_text'"
    value-sh = "glog nen_apex auto"
    options-sh = "echo -e \"auto|'$default_text'\n0|'$off_text'\n1|'$on_text'\""

    [[group.action.params]]
    name = "SIGNS"
    label = "'$sign_text'"
    value-sh = "glog signs_apex testkey"
    options-sh = "findfile file $ETC/key/4096 .pem | sed \"s|.pem||\""

    [[group.action.params]]
    name = "FILE"
    desc = "'$builds_text_1'"
    options-sh = "findfile forapex $APK/$PTAH"
    required = true
    multiple = true
  '
}

Utiliapk() {
    [ -d $PTAD/out ] && mkdir -p $PTAD/out &>/dev/null &

    if [ "$(glog hide_show2 1)" == 1 ]; then
    echo "[[group]]"
    show_apkset
    else
    desc_apks="$path_text: $(glog PTAD | sed "s|$SDCARD_PATH|\/sdcard|")"
    desc_apks1="$projects_text: $PTAH"
    fi

  echo '
  [[group]]
  [[group.action]]
  title = "'$decompile_text'"
  desc = "'$desc_apks'"
  icon = "'`urlpng decom`'"
  warn = "'$decom_apk_text_15'"
  script = """
    slog dexlib "$dexlib"
    slog tooldecom "$tooldecom"
    slog xoa_debug_info "$xoa_debug_info"
    slog type_apk "$type_apk"
    slog dexlibk "$dexlibk"
    slog mutiresk "$mutiresk"
    slog redivdd "$redivdd"
    IFS=$'"'\n'"'
    for vapk in $FILE; do
    if [ "$tooldecom" == "apkeditor" ]; then
    apkeditor_d -i "$PTAD/$vapk" -t "$type_apk" -b "$xoa_debug_info" -d "$dexlib" -o "$APK/$PTAH" -s "$redivdd"
    else
    apktool_d -i "$PTAD/$vapk" -r "$mutiresk" -b "$xoa_debug_info" -d "$dexlibk" -o "$APK/$PTAH" -s "$redivdd"
    fi
    echo
    done
    echo "'$save_text' $APK/$PTAH"
    echo
    checktime
  """

    [[group.action.params]]
    name = "tooldecom"
    title = "'$customize_tools_text'"
    label = "'$tools_text'"
    value-sh = "glog tooldecom apkeditor"
    options-sh = "echo -e \"apkeditor|Apkeditor\napktool|Apktool\""

    [[group.action.params]]
    name = "mutiresk"
    title = "'$decom_apk_text_11'"
    label = "'$option_text'"
    value-sh = "glog mutiresk 1"
    options-sh = "echo -e \"0|'$decom_apk_text_3'\n1|'$default_text'\n2|'$decom_apk_text_5'\""
    depend-on = "tooldecom"
    depend-value = "apkeditor"
    depend-mode = "hide"

    [[group.action.params]]
    name = "type_apk"
    title = "'$decom_apk_text_11'"
    label = "'$option_text'"
    value-sh = "glog type_apk xml"
    depend-on = "tooldecom"
    depend-value = "apktool"
    depend-mode = "hide"
    options-sh = "echo -e \"raw|'$decom_apk_text_3'\nxml|'$default_text'\nreso|'$decom_apk_text_10'\""

    [[group.action.params]]
    name = "dexlibk"
    title = "'$decom_apk_text_12'"
    label = "'$option_text'"
    value-sh = "glog dexlibk 2"
    options-sh = "echo -e \"0|'$decom_apk_text_3'\n1|'$default_text'\n2|Baksmali 3.0.9\""
    depend-on = "tooldecom"
    depend-value = "apkeditor"
    depend-mode = "hide"

    [[group.action.params]]
    name = "dexlib"
    title = "'$decom_apk_text_12'"
    label = "'$option_text'"
    value-sh = "glog dexlib smali"
    options-sh = "echo -e \"nodex|'$decom_apk_text_3'\ninternal|'$default_text'\nsmali|Baksmali 3.0.9\""
    depend-on = "tooldecom"
    depend-value = "apktool"
    depend-mode = "hide"

    [[group.action.params]]
    name = "xoa_debug_info"
    label = "'$decom_apk_text_7'"
    type = "switch"
    value-sh = "glog xoa_debug_info 1"
    depend-on = "dexlib|dexlibk"
    depend-value = "nodex|0"
    depend-mode = "hide|hide"
    depend-cascade = false
    depend-readonly = true
    depend-logic = "priority"

    [[group.action.params]]
    name = "redivdd"
    label = "'$decom_apk_text_14'"
    type = "switch"
    value-sh = "glog redivdd 0"
    depend-on = "dexlib|dexlibk"
    depend-value = "smali|2"
    depend-mode = "show|show"
    depend-default = "hide"
    depend-cascade = false
    depend-readonly = true

    [[group.action.params]]
    name = "dex_methods"
    label = "'$number_text'"
    type = "number"
    min = 40000
    max = 65535
    value-sh = "glog dex_methods 64000"
    depend-on = "redivdd"
    depend-value = "1"
    depend-mode = "show"
    depend-default = "hide"

    [[group.action.params]]
    name = "FILE"
    title = "'$decom_apk_text_9'"
    desc = "'$input_file_text': apk, apks, apkm, xapk, jar, zip"
    options-sh = "findfile 9 $PTAD"
    required = true
    multiple = true

  [[group.action]]
  title = "'$build_text'"
  desc = "'$desc_apks1'"
  icon = "'`urlpng build`'"
  warn = "'$build_apk_text_2'"
  script = """
    slog sign "$sign"
    slog comlib "$comlib"
    slog sstring "$sstring"
    slog xoatm "$xoatm"
    slog copysign "$copysign"
    IFS=$'"'\n'"'
    for vbapk in $FOLDER; do
      if [ -f "$APK/$PTAH/$vbapk/archive-info.json" ]; then
        apkeditor_b -i "$APK/$PTAH/$vbapk" -o "$PTAD/out" -s "$sign" -n "$sstring" -d "$xoatm" -x "$comlib"
      else
        apktool_b -i "$APK/$PTAH/$vbapk" -o "$PTAD/out" -c "$copysign" -s "$sign" -n "$sstring" -d "$xoatm" -x "$comlib"
      fi
      echo
    done
    echo "'$save_text' $PTAD/out"
    echo
    checktime
  """

    [[group.action.params]]
    name = "xoatm"
    label = "'$deleted_project_text'"
    type = "bool"
    value-sh = "glog xoatm 0"

    [[group.action.params]]
    name = "sign"
    label = "'$sign_text'"
    value-sh = "glog sign default"
    options-sh = "findfile file $ETC/key .pk8 | sed \"s|.pk8||\""

    [[group.action.params]]
    name = "sstring"
    label = "'$build_apk_text_1'"
    type = "switch"
    value-sh = "glog sstring 1"

    [[group.action.params]]
    name = "copysign"
    label = "'$decom_apk_text_13'"
    type = "switch"
    value-sh = "glog copysign"
    depend-on = "FOLDER"
    depend-value = "(apktool)"
    depend-mode = "show"
    depend-default = "hide"

    [[group.action.params]]
    name = "comlib"
    label = "'$addlang_text_2'"
    desc = "'$addlang_text_3'"
    value-sh = "glog comlib manifest"
    options-sh = "echo -e \"manifest|'$default_text'\ntrue|'$on_text'\nfalse|'$off_text'\""

    [[group.action.params]]
    name = "FOLDER"
    desc = "'$builds_text_1'"
    options-sh = "findfile forapk $APK/$PTAH"
    required = true
    multiple = true

  [[group]]
  [[group.page]]
  title = "'$apex_text'"
  icon = "'`urlpng apex`'"
  config-sh = "'$ETC'/tool-tree.bash Apex"
  handler = """
    if [ "$menu_id" == "v1" ]; then
        [ "$(glog hide_show_apex)" == 1 ] && slog hide_show_apex 0 || slog hide_show_apex 1
    fi
  """

    [[group.page.options]]
    key = "v1"
    type = "checkbox"
    title = "'$folder_text' APK"
    box = "glog hide_show_apex"
    silent = true
    reload = true

  [[group]]
  [[group.action]]
  title = "'$distur_apk_text_2'"
  icon = "'`urlpng apk_distur`'"
  warn = "'$distur_apk_text_1'"
  script = """
    IFS=$'"'\n'"'
    for v in $FILE; do
        echo "'$more_text_4' $FILE"
        echo
        apkeditor p -skip-manifest -f -i "$PTAD/$v" -o "$PTAD/out/$v" 2>&1 | sed -u -e "1,/__/d"
        echo
    done
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    options-sh = "findfile 10 $PTAD"
    required = true
    multiple = true

  [[group.action]]
  title = "'$apk_restore_text_2'"
  icon = "'`urlpng apk_restore`'"
  warn = "'$apk_restore_text_1'"
  script = """
    IFS=$'"'\n'"'
    for v in $FILE; do
        echo "'$more_text_4' $FILE"
        echo
        apkeditor x -fix-types -f -i "$PTAD/$v" -o "$PTAD/out/$v" 2>&1 | sed -u -e "1,/__/d"
        echo
    done
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    options-sh = "findfile 10 $PTAD"
    required = true
    multiple = true

  [[group]]
  [[group.action]]
  title = "'$apk_mager_text_2'"
  icon = "'`urlpng merge_apk`'"
  warn = "'$apk_mager_text_1'"
  script = """
    IFS=$'"'\n'"'
    for v in $FILE; do
        echo "'$more_text_4' $FILE"
        echo
        apkeditor m -f -i "$PTAD/$v" -o "$PTAD/out/$v" 2>&1 | sed -u -e "1,/__/d"
        echo
    done
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    options-sh = "findfile 9 $PTAD | grep -E \"(apks)|(apkm)|(xapk)\""
    required = true
    multiple = true

  [[group]]
  [[group.action]]
  title = "'$restore_apk_text_3'"
  icon = "'`urlpng restore_sign`'"
  script = """
    slog apk_restore_sign "$FILE"
    slog apk_restore_sign2 "$FILE2"
    echo "'$more_text_4' $FILE"
    echo
    apkeditor d -f -t sig -i "$PTAD/$FILE" -sig "$TMP/signatures_dir" 2>&1
    echo
    apkeditor b -f -t sig -i "$PTAD/$FILE2" -sig "$TMP/signatures_dir" -o "$PTAD/out/$FILE2" 2>&1
    rm -fr "$TMP/signatures_dir"
  """

    [[group.action.params]]
    name = "FILE"
    title = "'$restore_apk_text_1'"
    value-sh = "glog apk_restore_sign"
    options-sh = "findfile 10 $PTAD"
    required = true

    [[group.action.params]]
    name = "FILE2"
    title = "'$restore_apk_text_2'"
    value-sh = "glog apk_restore_sign2"
    options-sh = "findfile 10 $PTAD"
    required = true
  '
}

Addon() {

Download() {
  if [ "$url_vb" ]; then
  echo '[[group]]
  [[group.action]]
  '$farooot'
  warn = "'$use_network_text'"
  icon = "'$icon_vb'"
  reload = true
  title = "'$name_vb'"
  desc = "'$desc_vb'"
  script = """
    echo "'$update_text_3'"
    echo
    installadd '$url_vb' "'${dirvad%/*}'"
  """'
  fi
}

Features() {
  [ "$1" == "status" ] && atextx="$addon_text_10" || atextx="$addon_text_2"
  echo '[[group]]
  [[group.switch]]
  warn = "'$atextx'"
  title = "'$name_vb'"
  desc = "'$desc_vb'"
  icon = "'$icon_vb'"
  shell = "hidden"
  get = "cat '$dirvad'/'$1'"
  set = "echo \"$state\" > '$dirvad'/'$1'" '
}

Homeadd() {

  # Load index
  if [ -f "$dirvad/index.bash" ]; then
    pagesh='config-sh = "'$dirvad'/index.bash home"'
  elif [ -f "$dirvad/index.toml" ]; then
    pagesh='config = "'$dirvad'/index.toml"'
  else
    pagesh='config = "'$ETC'/error.toml"'
  fi

  if [ -f "$dirvad/before-load.bash" ]; then
    beforesh='before-load = "'$dirvad'/before-load.bash"'
  fi

  # Load menu
  if [ -f "$dirvad/menu.bash" ]; then
    code_option="$($dirvad/menu.bash 2>/dev/null)"
  elif [ -f "$dirvad/menu.toml" ]; then
    code_option="$(cat $dirvad/menu.toml 2>/dev/null)"
  fi

  # Load trang
  if [ "$name_vb" ]; then

    # Xác nhận có google dịch
    google_trankk='
    [[group.page.options]]
    title = "'$google_translate_text'"
    box = "glog auto_trans_text_'$idadd'"
    reload = true
    silent = true
    type = "checkbox"
    script = """
      if [ "$(glog auto_trans_text_'$idadd')" == 1 ]; then
        slog auto_trans_text_'$idadd' 0
      else
        if [ "$(glog transai_text_'$idadd')" == 1 ]; then
        rm -fr '$dirvad'/auto.sh
        slog transai_text_'$idadd' 0
        fi
        slog auto_trans_text_'$idadd' 1
      fi
    """
    
    [[group.page.options]]
    title = "Gemini"
    box = "glog transai_text_'$idadd'"
    reload = true
    silent = true
    type = "checkbox"
    script = """
      if [ "$(glog transai_text_'$idadd')" == 1 ]; then
        slog transai_text_'$idadd' 0
      else
        if [ "$(glog auto_trans_text_'$idadd')" == 1 ]; then
        rm -fr '$dirvad'/auto.sh
        slog auto_trans_text_'$idadd' 0
        fi
        transai -c && slog transai_text_'$idadd' 1 || showbanner -t "Gemini" -m "$(transai -c 2>&1)" -y error
      fi
    """
    '

  echo '
  [[group]]
  [[group.page]]
  '$farooot'
  '$shortcut'
  '$pagesh'
  '$summss'
  '$beforesh'
  title = "'$name_vb'"
  desc = "'$desc_vb'"
  icon = "'$icon_vb'"

  '"$google_trankk"'
  [[group.page.options]]
  title = "'$pin_text_add'"
  auto-finish = true
  silent = true
  script = """
    if [ -f "'$dirvad'/pin" ]; then
    rm -f "'$dirvad'/pin"
    else
    echo > "'$dirvad'/pin"
    fi
  """

  '"$code_option"'
  '
  fi
}

Vips() {
  # Xoá giá trị cũ
  code_option=''; farooot=''; index_adds=''; atextx='';
  google_trankk=''; shortcut=''; beforesh='';
  
  # Chọn bên
  if [ "$PATHADD" == "$AON" ]; then
    index_adds="$(glog settadd)"
  else
    index_adds="$(glog settadd2)"
  fi

  # getprop
  gprop() {
    cat "$vadd" 2>/dev/null | awk -F= -v k="$1" '$1==k{print $2; exit}'
  }

  # Phát hiện root
  if [ "$(gprop root)" == "true" ]; then
    farooot='lock = "[ $ROT == 0 ] && echo \"'$root_warning_text'\" || echo 0"'
  fi

  # phát hiện tính năng
  summss="$(gprop summary)"
  [ "$summss" ] && summss='summary = "'$summss'" '
  [ "$(gprop shortcut)" == "true" ] && shortcut='key = "'$idadd'" '

  # Desc ngôn ngữ
  description_text="$(gprop 'description_'$LANGUAGE'_'$COUNTRY'')"
  if [ "$description_text" ]; then
    description_text=" | $description_text"
  else
    description_text="$(gprop 'description_'$LANGUAGE'')"
    if [ "$description_text" ]; then
      description_text=" | $description_text"
    else
      description_text="$(gprop description)"
      [ "$description_text" ] && description_text=" | $description_text"
    fi
  fi

  # tên và desc
  name_vb="$(gprop name)"
  desc_vb="$(gprop version) $(gprop author)$description_text"
  url_vb="$(gprop url)"
  icon_vb="$(urladd icon)"

  # Load trang tính năng
  if [ "$(cat $dirvad/delete 2>/dev/null)" == 1 ]; then
    [ -f "$dirvad/uninstall.bash" ] && $dirvad/uninstall.bash
    find "$dirvad" -maxdepth 1 ! -path "$dirvad" ! -name 'download.prop' ! -name 'pin' ! -name 'status' -exec rm -rf {} +
  elif [ "$index_adds" == 1 ]; then
    Features status
  elif [ "$index_adds" == 2 ]; then
    [ -f $dirvad/nodelete ] || Features delete
  else
    if [ "$(cat $dirvad/status 2>/dev/null)" != 1 ]; then
      if [[ -f "$dirvad/index.sh" || -f "$dirvad/index.bash" || -f "$dirvad/index.toml" ]]; then
        Homeadd
      elif [ -f "$dirvad/download.prop" ]; then
        Download
      fi
    fi
  fi
}

  # Load trang add-on có pin trước
  for vadd in $PATHADD/*/addon.prop; do
    [ -f "$vadd" ] || continue
    dirvad="${vadd%/*}"
    idadd="${dirvad##*/}"
    pin_text_add="$unpin_text"
    [ -f "$dirvad/pin" ] || continue
    if [[ -f "$dirvad/index.bash" || -f "$dirvad/index.toml" ]]; then
      Vips
    fi
  done

  # Load trang không có pin
  for vadd in $PATHADD/*/addon.prop; do
    [ -f "$vadd" ] || continue
    dirvad="${vadd%/*}"
    idadd="${dirvad##*/}"
    pin_text_add="$pin_text"
    [ -f "$dirvad/pin" ] && continue
    if [[ -f "$dirvad/index.bash" || -f "$dirvad/index.toml" ]]; then
      Vips
    fi
  done

  # Load trang tải xuống ở dưới cùng
  for vadd in $PATHADD/*/download.prop; do
    [ -f "$vadd" ] || continue
    dirvad="${vadd%/*}"
    idadd="${dirvad##*/}"
    pin_text_add="$pin_text"
    if [[ -f "$dirvad/pin" || -f "$dirvad/index.bash" || -f "$dirvad/index.toml" ]]; then
      continue
    fi
    Vips
  done

}

# Điều hướng chính
"$@"