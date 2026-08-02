#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

infor() {
echo '
[[group]]
  title = "'$project_text': '$PTSH'"

  [[group.action]]
  title = "'$desc_patch_prop'"
  warn = """'"$desc_patch_prop_long"'"""
  reload = true
  script = "'$pathsh' sprop_pris"

    [[group.action.params]]
    name = "fix_pris"
    label = "'$select_text'"
    desc = "'$string_text_2': vendor, system, system_ext, product"
    value-sh = "'$pathsh' gprop_pris"
    options-sh = "echo -e \"4|none\n2|log\n3|disable\n1|enforce\""
    required = true

[[group]]
  [[group.action]]
  title = "'$quick_custom_text'"
  summary = "Xiaomi"
  warn = "'$warn_delete_gms'"
  reload = true
  script = "'$pathsh' custom_patch"

    [[group.action.params]]
    name = "device_features"
    label = "'$label_device_features'"
    desc = "'$string_text_2': product"
    type = "bool"
    value-sh = "'$pathsh' get_patch_1"
    readonly = "'$pathsh' get_patch_1"

    [[group.action.params]]
    name = "delete_gms"
    label = "'$label_delete_gms'"
    desc = "'$string_text_2': product"
    type = "bool"
    value-sh = "'$pathsh' get_patch_2"
    readonly = "'$pathsh' get_patch_2"
    
    [[group.action.params]]
    name = "xeu_toolbox"
    label = "'$label_xeu_toolbox'"
    desc = "'$string_text_2': system_ext"
    type = "bool"
    value-sh = "'$pathsh' check_xeu"
    readonly = "'$pathsh' check_xeu"

[[group]]
  [[group.action]]
  title = "'$label_rw_rom'"
  summary = "Xiaomi"
  warn = "'$warn_delete_gms'"
  reload = true
  auto-off = true
  script = "'$pathsh' rw_rom_ext"

    [[group.action.params]]
    name = "diss_ovelsy"
    label = "'$diss_ovelsy_text'"
    desc = "'$string_text_2': vendor"
    type = "bool"
    value-sh = "'$pathsh' get_rw_rom_1"
    readonly = "'$pathsh' get_rw_rom_1"
    
    [[group.action.params]]
    name = "diss_ovelsy_boot"
    label = "'$diss_ovelsy_text'"
    desc = "'$string_text_2': vendor_boot"
    type = "bool"
    value-sh = "'$pathsh' get_rw_rom_2"
    readonly = "'$pathsh' get_rw_rom_2"

    [[group.action.params]]
    name = "move_pangu"
    label = "'$move_pangu_text'"
    desc = "'$string_text_2': product, system"
    type = "bool"
    value-sh = "'$pathsh' get_rw_rom_3"
    readonly = "'$pathsh' get_rw_rom_3"

    [[group.action.params]]
    name = "move_miext"
    label = "'$move_miext_text'"
    desc = "'$string_text_2': mi_ext, system, product, system_ext"
    type = "bool"
    value-sh = "'$pathsh' get_rw_rom_4"
    readonly = "'$pathsh' get_rw_rom_4"

[[group]]
  [[group.action]]
  title = "'$cover_app_text_1'"
  warn = "'$cover_app_text_2'"
  script = """
    '$pathsh' cover_app "$cover_data_app"
    checktime
  """

    [[group.action.params]]
    name = "cover_data_app"
    type = "text"
    options-sh = "'$pathsh' search_apk | sort"
    required = true
    multiple = true

[[group]]
  [[group.action]]
  title = "'$title_delete'"
  script = """
    slog del_app_patch "$del_app_patch"
    '$pathsh' del_app "$del_app_patch"
  """

    [[group.action.params]]
    name = "del_app_patch"
    type = "text"
    desc = "'$text_del_file'"
    placeholder = "VoiceAssist Sogou"
    value-sh = "glog del_app_patch \"BaidulME MIGalleryLockscreen MIService MIUIEmail MIUIVirtualSim MIUIXiaoAiSpeechEngine OS2VipAccount SmartHome XMRemoteController iFlytekIME CarWith MITSMClient MIS MINextpay VoiceAssistAndroidT VoiceTrigger UPTsmService Music MIUIgreenguard MIUIQuickSearchBox MIUIBrowser MiGameCenterSDKService YouTube YTMusic\""
    required = true
  '
}

home() {
  [ -z "$google_text" ] && google_text="$version_text: $(gprop version $MPAT/addon.prop)"

  # Điền dữ liệu mặc định
  (
  if [ -z "$(glog ime_color_dark)" ]; then
    slog ime_dimen '<dimen name="input_method_seek_bar_margin">6.5999756dp</dimen>
  <dimen name="input_bottom_height">45.599976dp</dimen>
  <dimen name="input_bottom_button_height">28.5dp</dimen>
  <dimen name="input_bottom_button_margin_top">2.5dp</dimen>'
    slog ime_app com.google.android.inputmethod.latin
    slog ime_color '#f0f3f8'
    slog ime_color_dark '#1e1f21'
  fi
  ) &
  
  echo '
[[group]]
  title = "'$google_text'"

  [[group.page]]
  title = "'$title_quick'"
  summary = "'$project_text': '$PTSH'"
  config-sh = "'$MPAT'/index.bash infor"

[[group]]
  title = "'$reminder_notes'"

  [[group.action]]
  title = "'$title_framework_patch'"
  warn = "'$warning_notes_text'"
  summary = "Android 12+"
  script = """
    slog toolbox_patch_os "$FILE"
    '$pathsh' toolbox "$FILE"
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    title = "'$list_file_text'"
    desc = "'$string_text_1': Project/'$PTSH', '$sdcard_text'"
    options-sh = "'$pathsh' search framework.jar services.jar miui-services.jar"
    value-sh = "glog toolbox_patch_os"
    required = true
    multiple = true

    [[group.action.params]]
    name = "fix_apksign"
    label = "'$label_fix_apksign'"
    desc = "'$required_files_text': framework.jar, services.jar, miui-services.jar"
    type = "bool"

    [[group.action.params]]
    name = "tool_box"
    label = "'$label_fix_toolbox'"
    desc = "'$required_files_text': framework.jar, services.jar"
    type = "bool"

    [[group.action.params]]
    name = "fix_enforce"
    label = "'$label_fix_enforce'"
    desc = "'$required_files_text': miui-services.jar"
    type = "bool"

  [[group.action]]
  title = "'$title_cn_global'"
  warn = "'$warning_notes_text'"
  summary = "Xiaomi, Android 12+"
  script = """
    slog fix_noti_patch_os "$FILE"
    '$pathsh' fixnoti "$FILE"
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    title = "'$list_file_text'"
    desc = "'$string_text_1': Project/'$PTSH', '$sdcard_text'"
    options-sh = "'$pathsh' search miui-framework.jar miui-services.jar PowerKeeper.apk MiuiSystemUI.apk Settings.apk"
    value-sh = "glog fix_noti_patch_os"
    required = true
    multiple = true

    [[group.action.params]]
    name = "fix_noti"
    label = "'$label_fix_noti'"
    desc = "'$required_files_text': miui-framework.jar, miui-services.jar, PowerKeeper.apk, MiuiSystemUI.apk"
    type = "bool"

    [[group.action.params]]
    name = "settings_infor"
    label = "'$global_mod_text_1'"
    desc = "'$required_files_text': Settings.apk"
    type = "bool"

    [[group.action.params]]
    name = "settings_show"
    label = "'$global_mod_text_2'"
    desc = "'$required_files_text': Settings.apk"
    type = "bool"

    [[group.action.params]]
    name = "settings_icons"
    label = "'$global_mod_text_3'"
    desc = "'$required_files_text': Settings.apk"
    type = "bool"

    [[group.action.params]]
    name = "sceen_lock"
    label = "'$global_mod_text_6'"
    desc = "'$required_files_text': MiuiSystemUI.apk"
    type = "bool"

    [[group.action.params]]
    name = "dark_show"
    label = "'$global_mod_text_4'"
    desc = "'$required_files_text': miui-services.jar"
    type = "bool"
    
    [[group.action.params]]
    name = "open_app"
    label = "'$open_app_text'"
    desc = "'$required_files_text': miui-services.jar"
    type = "bool"
    
    [[group.action.params]]
    name = "font_fix"
    label = "'$global_mod_text_5'"
    desc = "'$required_files_text': miui-framework.jar"
    type = "bool"

  [[group.action]]
  title = "'$title_ime'"
  warn = "Note: MiuiSystemUI.apk (global),\n'$warning_notes_text'"
  summary = "Xiaomi"
  script = """
    slog ime_app "$ime_app"
    slog ime_color "$ime_color"
    slog ime_color_dark "$ime_color_dark"
    slog ime_dimen "$ime_dimen"
    slog fix_key_patch_os "$FILE"
    '$pathsh' fixkey "$FILE"
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    title = "'$list_file_text'"
    desc = "'$string_text_1': Project/'$PTSH', '$sdcard_text'"
    options-sh = "'$pathsh' search miui-framework.jar miui-services.jar *FrequentPhrase.apk MiuiSystemUI.apk Settings.apk"
    value-sh = "glog fix_key_patch_os"
    required = true
    multiple = true

    [[group.action.params]]
    name = "ime_app"
    desc = "'$desc_ime_app'"
    placeholder = "com.google.android.inputmethod.latin"
    type = "text"
    value-sh = "glog ime_app"
    required = true

    [[group.action.params]]
    name = "app_ime"
    label = "'$install_text' LatinImeGoogle"
    desc = "'$string_text_2': product"
    type = "switch"

    [[group.action.params]]
    name = "ime_color"
    desc = "'$desc_color_light'"
    placeholder = "#f0f3f8"
    type = "text"
    value-sh = "glog ime_color"
    required = true

    [[group.action.params]]
    name = "ime_color_dark"
    desc = "'$desc_color_dark'"
    placeholder = "#1e1f21"
    type = "text"
    value-sh = "glog ime_color_dark"
    required = true

    [[group.action.params]]
    name = "ime_dimen"
    desc = "'$desc_dimen'"
    type = "text"
    value-sh = "glog ime_dimen"

  [[group.action]]
  title = "'$title_many_patch'"
  warn = "'$warning_notes_text'"
  summary = "Xiaomi, Android 12+"
  script = """
    slog fix_manyo_patch_os "$FILE"
    '$pathsh' fixmultiple "$FILE"
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    title = "'$list_file_text'"
    desc = "'$string_text_1': Project/'$PTSH', '$sdcard_text'"
    options-sh = "'$pathsh' search services.jar miui-services.jar PowerKeeper.apk miui-framework.jar ExternalStorageProvider.apk"
    value-sh = "glog fix_manyo_patch_os"
    required = true
    multiple = true

    [[group.action.params]]
    name = "fix_screen"
    label = "'$label_fix_screen'"
    desc = "'$required_files_text': miui-services.jar, services.jar"
    type = "bool"

    [[group.action.params]]
    name = "fix_fps"
    label = "'$label_fix_fps'"
    desc = "'$required_files_text': PowerKeeper.apk"
    type = "bool"

    [[group.action.params]]
    name = "fix_reset_theme"
    label = "'$label_fix_reset_theme'"
    desc = "'$required_files_text': miui-framework.jar"
    type = "bool"

    [[group.action.params]]
    name = "fix_show_error"
    label = "'$label_fix_show_error'"
    desc = "'$required_files_text': services.jar"
    type = "bool"

    [[group.action.params]]
    name = "fix_fpscam"
    label = "'$label_fix_fps_cam'"
    desc = "'$required_files_text': miui-services.jar"
    type = "bool"

    [[group.action.params]]
    name = "fix_window"
    label = "'$label_fix_window'"
    desc = "'$required_files_text': miui-services.jar, miui-framework.jar"
    type = "bool"

    [[group.action.params]]
    name = "app_setup"
    label = "'$install_text' SetupWizard"
    desc = "'$required_files_text': miui-services.jar"
    type = "bool"
    
    [[group.action.params]]
    name = "fix_data"
    label = "'$label_fix_data'"
    desc = "'$required_files_text': ExternalStorageProvider.apk"
    type = "bool"
    
    
  [[group.action]]
  title = "'$title_app_patch'"
  warn = "'$warning_notes_text'"
  summary = "Xiaomi"
  script = """
    slog fix_manyo_patch_os "$FILE"
    '$pathsh' fixapps "$FILE"
    checktime
  """

    [[group.action.params]]
    name = "FILE"
    title = "'$list_file_text'"
    desc = "'$string_text_1': Project/'$PTSH', '$sdcard_text'"
    options-sh = "'$pathsh' search *PersonalAssistant*.apk MIUIWeather.apk Joyose.apk Provision.apk MIUIGallery.apk *SecurityCenter.apk *ThemeManager.apk"
    value-sh = "glog fix_manyo_patch_os"
    required = true
    multiple = true

    [[group.action.params]]
    name = "fix_themes"
    label = "'$label_fix_themes'"
    desc = "'$required_files_text': ThemeManager.apk"
    type = "bool"

    [[group.action.params]]
    name = "fix_appvault"
    label = "'$label_fix_appvault'"
    desc = "'$required_files_text': PersonalAssistant.apk"
    type = "bool"

    [[group.action.params]]
    name = "fix_thoit"
    label = "'$label_fix_thoit'"
    desc = "'$required_files_text': MIUIWeather.apk"
    type = "bool"

    [[group.action.params]]
    name = "fix_joyose"
    label = "'$label_fix_joyose'"
    desc = "'$required_files_text': Joyose.apk"
    type = "bool"

    [[group.action.params]]
    name = "fix_mapcn"
    label = "'$label_fix_mapcn'"
    desc = "'$required_files_text': MIUIGallery.apk"
    type = "bool"

    [[group.action.params]]
    name = "fix_gmscn"
    label = "'$label_fix_gmscn'"
    desc = "'$required_files_text': Provision.apk"
    type = "bool"

    [[group.action.params]]
    name = "fix_off_10s"
    label = "'$label_fix_off_10s'"
    desc = "'$required_files_text': SecurityCenter.apk"
    type = "bool"

[[group]]
  [[group.action]]
  title = "'"$action_title"'"
  summary = "'"$action_desc"'"
  warn = "'"$action_warn"'"
  lock = "[ \"$ROT\" == 0 ] && echo \"'$root_warrn'\" || echo 0"
  interruptible = false
  script = """
    slog dem_giay $dem_giay
    slog kill_customize "$kill_customize"
    slog open_app_bool "$open_app"
    slog kill_apk_list "$kill_apk_list"
    '$MPAT'/index.bash test_app "$kill_apk_list"
  """

    [[group.action.params]]
    name = "kill_apk_list"
    title = "'"$param1_title"'"
    label = "'"$param1_label"'"
    desc = "'$string_text_1': Project/'$PTSH', '$sdcard_text'"
    options-sh = "'$pathsh' list_apk_file"
    value-sh = "glog kill_apk_list"
    required = true

    [[group.action.params]]
    name = "open_app"
    label = "'$open_app_text_2'"
    type = "switch"
    value-sh = "glog open_app_bool"

    [[group.action.params]]
    name = "dem_giay"
    label = "'"$param2_label"'"
    desc = "'"$param2_desc"'"
    type = "seekbar"
    min = 5
    max = 300
    value-sh = "glog dem_giay 60"

    [[group.action.params]]
    name = "kill_customize"
    label = "'"$param3_label"'"
    desc = "'"$param3_desc"'"
    placeholder = "com.android.systemui"
    type = "text"
    value-sh = "glog kill_customize"

[[group]]
  [[group.action]]
  title = "'$add_another_app_text'"
  summary = "Xiaomi"
  warn = "'$add_another_app_text_2', '$add_another_app_text_3'"
  script = """
    '$pathsh' online_app
    echo
    checktime
  """

    [[group.action.params]]
    name = "add_app"
    label = "InstallerX Revived"
    desc = "'$string_text_2': (CN: product), (global: system)"
    type = "bool"

    [[group.action.params]]
    name = "add_app_2"
    label = "Safetycore"
    desc = "'$string_text_2': product"
    type = "bool"

    [[group.action.params]]
    name = "app_playstore"
    label = "Play Store"
    desc = "'$string_text_2': product"
    type = "bool"

    [[group.action.params]]
    name = "app_restore"
    label = "Google Restore"
    desc = "'$string_text_2': product"
    type = "bool"

    [[group.action.params]]
    name = "app_velvet"
    label = "Google, Gemini"
    desc = "'$string_text_2': product"
    type = "bool"

    [[group.action.params]]
    name = "app_auto"
    label = "AndroidAuto"
    desc = "'$string_text_2': product"
    type = "bool"
    
    [[group.action.params]]
    name = "app_tts"
    label = "Google TTS"
    desc = "'$string_text_2': product"
    type = "bool"
    
    [[group.action.params]]
    name = "app_carrier"
    label = "Carrier Servicesi"
    desc = "'$string_text_2': product"
    type = "bool"
    
    [[group.action.params]]
    name = "app_gps"
    label = "LocationHistory"
    desc = "'$string_text_2': product"
    type = "bool"
    
    [[group.action.params]]
    name = "app_monet"
    label = "ThemePicker"
    desc = "'$string_text_2': system_ext"
    type = "bool"
    
    
[[group]]
  [[group.action]]
  title = "'$title_boot_patch'"
  warn = "'$title_boot_patch2', '$title_boot_patch3_desc'"
  script = "'$pathsh' patch_boot \"$FOLDER\""

    [[group.action.params]]
    name = "FOLDER"
    title = "'$decrypted_partition_text'"
    label = "'$select_text'"
    desc = "'$string_text_2': vendor_boot, boot"
    options-sh = "findfile 0 $SDH/$PTSH | grep boot"
    required = true

    [[group.action.params]]
    name = "fix_fake_lock"
    label = "'$title_boot_patch3'"
    desc = ""
    type = "bool"

    [[group.action.params]]
    name = "fix_diselinux"
    label = "'$title_boot_patch4'"
    options-sh = "echo -e \"0|'$default_text'\n1|enforcing\n2|permissive\n3|disabled\""
'
}

test_app() {
  echo "$test_app_text_1 $1"
  echo
  infor_pack="$(apkeditor info -t json -version-code -package -i "$1")"
  package_apk="$(echo "$infor_pack" | jq -r '.[0].package')"
  echo "$test_app_text_2 $package_apk"
  echo
  urlapk="$(pm path $package_apk | cut -d: -f2)"

  if [ "$(echo "$1" | grep -cm1 "$SDC")" == 1 ]; then
    [ -d $TMP/app ] && rm -fr $TMP/app
    mkdir -p $TMP/app
    cp -rf "$1" "$TMP/app/${urlapk##*/}"
    tep_apk="$TMP/app/${urlapk##*/}"
  else
    tep_apk="$1"
  fi

  infor_ver1="$(apkeditor info -t json -version-code -i "$urlapk" | jq '.[0].VersionCode')"
  infor_ver2="$(echo "$infor_pack" | jq '.[0].VersionCode')"

  if [ "$infor_ver1" != "$infor_ver2" ]; then
    echo "$infor_text_ver $infor_ver1 ≠ $infor_ver2"
    exit 1
  fi

  if [ "$(echo "$urlapk" | grep -cm1 "/data/")" == 1 ]; then
    primmsg='u:object_r:apk_data_file:s0'
    path_apk="$urlapk"
    goc_apk="$tep_apk"
  else
    primmsg='u:object_r:system_file:s0'
    path_apk="${urlapk%/*}"
    goc_apk="${tep_apk%/*}"
  fi

  su -mm -c umount -l "$path_apk" 2>/dev/null
  chmod -R 644 "$tep_apk"
  chcon -R $primmsg "$goc_apk" >/dev/null 2>&1
  su -mm -c mount --bind "$goc_apk" "$path_apk"
  pkill -f $package_apk >/dev/null 2>&1
  [ "$kill_customize" ] && pkill -f $kill_customize >/dev/null 2>&1

  if [ "$open_app" == 1 ]; then
    class_app="$(pm resolve-activity --components $package_apk)"
    [ "$class_app" == "No activity found" ] || am start -n $class_app &>/dev/null
  fi

  while [ $dem_giay -gt 0 ]; do
    echo "$test_app_text_3 ${dem_giay}s"
    sleep 1
    ((dem_giay--))
  done

  echo
  echo "$test_app_text_4"
  su -mm -c umount -l "$path_apk" 2>/dev/null
  killall $package_apk >/dev/null 2>&1
  [ "$kill_customize" ] && killall $kill_customize >/dev/null 2>&1
}

# check update add-on
update_addon() {
  if checkonline; then
    echo "$check_update_text_1"
    echo
    check_sum_onl="$(xem https://api.github.com/repos/Zenlua/Tool-Tree/releases/tags/V1 | jq -r '.assets[] | select(.name == "patch_rom.add") | .digest' | cut -d: -f2)"
    if [[ "$check_sum_onl" != "$(glog check_sum_addon_patch_rom)" ]]; then
      installadd "$(gprop url $MPAT/download.prop)" "${MPAT%/*}" 2>&1 || { echo "$check_update_text_2" >&2; exit 1; }
      echo
      [ -f $MPAT/changelog.txt ] && cat $MPAT/changelog.txt
    else
      echo "$check_update_text_3"
      echo
      [ -f $MPAT/changelog.txt ] && cat $MPAT/changelog.txt
    fi
  else
    echo "$network_text" >&2
  fi
}

# Thư mục hiện tại
MPAT="${0%/*}"
sdcard_text="${PTAD/$SDCARD_PATH/\/sdcard}"
pathsh="$MPAT/patch-rom"
source trans_add "$MPAT"

# Index
"$@"
