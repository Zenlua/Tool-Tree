#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

infor() {
  xml_print '<?xml version="1.0" encoding="UTF-8" ?>
<group>

  <group title="'$project_text': '$PTSH'">
    <action title="'$desc_patch_prop'" warn="'$desc_patch_prop_long'" reload="true">
      <param name="fix_pris" label="'$select_text'" desc="'$string_text_2': vendor, system, system_ext, product" value-sh="'$pathsh' gprop_pris" options-sh="echo -e '"'4|none\n2|log\n3|disable\n1|enforce'"'" required="true"/>
      <set>'$pathsh' sprop_pris</set>
    </action>
  </group>

  <group>
    <action title="'$quick_custom_text'" warn="'$warn_delete_gms'" summary="Xiaomi" auto-off="true" reload="true">
      <param name="device_features" label="'$label_device_features'" desc="'$string_text_2': product" type="bool" value-sh="'$pathsh' get_patch_1"/>
      <param name="delete_gms" label="'$label_delete_gms'" desc="'$string_text_2': product" type="bool" value-sh="'$pathsh' get_patch_2"/>
      <set>'$pathsh' custom_patch</set>
    </action>
  </group>

  <group>
    <action title="'$label_rw_rom'" warn="'$warn_delete_gms'" summary="Xiaomi" reload="true">
      <param name="diss_ovelsy" label="'$diss_ovelsy_text'" desc="'$string_text_2': vendor, vendor_boot" value-sh="'$pathsh' get_rw_rom_1" type="bool"/>
      <param name="move_pangu" label="'$move_pangu_text'" desc="'$string_text_2': product, system" value-sh="'$pathsh' get_rw_rom_2" type="bool"/>
      <param name="move_miext" label="'$move_miext_text'" desc="'$string_text_2': system, product, system_ext, mi_ext" value-sh="'$pathsh' get_rw_rom_3" type="bool"/>
      <set>'$pathsh' rw_rom_ext</set>
    </action>
  </group>

  <group>
    <action title="'$cover_app_text_1'" warn="'$cover_app_text_2'">
      <param name="cover_data_app" type="text" option-sh="'$pathsh' search_apk | sort" required="true" multiple="true"/>
      <set>
        '$pathsh' cover_app "$cover_data_app"
        checktime
      </set>
    </action>
  </group>

  <group>
    <action title="'$title_delete'">
      <param name="del_app_patch" type="text" value-sh="glog del_app_patch '"'BaidulME MIGalleryLockscreen MIService MIUIEmail MIUIVirtualSim MIUIXiaoAiSpeechEngine OS2VipAccount SmartHome XMRemoteController iFlytekIME CarWith MITSMClient MIS MINextpay VoiceAssistAndroidT VoiceTrigger UPTsmService Music MIUIgreenguard MIUIQuickSearchBox MIUIBrowser MiGameCenterSDKService YouTube YTMusic'"' " required="true" desc="'$text_del_file'" placeholder="VoiceAssist Sogou"/>
      <set>
        slog del_app_patch "$del_app_patch"
        '$pathsh' del_app "$del_app_patch"
      </set>
    </action>
  </group>

</group>'
}

home() {
  [ -z "$google_text" ] && google_text="$version_text: $(gprop version $MPAT/addon.prop)"

  xml_print '<?xml version="1.0" encoding="UTF-8" ?>
<group>

  <group title="'$google_text'">
    <page title="'$title_quick'" config-sh="'$MPAT'/index.bash infor">
      <summary>'"$project_text: $PTSH"'</summary>
    </page>
  </group>

  <group title="'$reminder_notes'">
    <action title="'$title_framework_patch'" summary="Android 12+">
      <param name="FILE" option-sh="'$pathsh' search framework.jar services.jar miui-services.jar" multiple="true" value-sh="glog toolbox_patch_os" required="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" />
      <param name="fix_apksign" label="'$label_fix_apksign'" desc="'$required_files_text': framework.jar, services.jar, §(Xiaomi: miui-services.jar)" type="bool" />
      <param name="tool_box" label="'$label_fix_toolbox'" desc="'$required_files_text': framework.jar, services.jar" type="bool" />
      <param name="fix_enforce" label="'$label_fix_enforce'" desc="'$required_files_text': miui-services.jar" type="bool" />
      <set>
        slog toolbox_patch_os "$FILE"
        '$pathsh' toolbox "$FILE"
        checktime
      </set>
    </action>

    <action title="'$title_cn_global'" summary="Xiaomi, Android 12+">
      <param name="FILE" option-sh="'$pathsh' search miui-framework.jar miui-services.jar PowerKeeper.apk MiuiSystemUI.apk Settings.apk" value-sh="glog fix_noti_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" required="true"/>
      <param name="fix_noti" label="'$label_fix_noti'" desc="'$required_files_text': miui-framework.jar, miui-services.jar, PowerKeeper.apk, MiuiSystemUI.apk" type="bool" />
      <param name="settings_infor" label="'$global_mod_text_1'" desc="'$required_files_text': Settings.apk" type="bool" />
      <param name="settings_show" label="'$global_mod_text_2'" desc="'$required_files_text': Settings.apk" type="bool" />
      <param name="settings_icons" label="'$global_mod_text_3'" desc="'$required_files_text': Settings.apk" type="bool" />
      <param name="dark_show" label="'$global_mod_text_4'" desc="'$required_files_text': miui-services.jar" type="bool" />
      <param name="font_fix" label="'$global_mod_text_5'" desc="'$required_files_text': miui-framework.jar" type="bool" />
      <set>
        slog fix_noti_patch_os "$FILE"
        '$pathsh' fixnoti "$FILE"
        checktime
      </set>
    </action>

    <action title="'$title_ime'" summary="Xiaomi">
      <param name="FILE" option-sh="'$pathsh' search miui-framework.jar miui-services.jar *FrequentPhrase.apk MiuiSystemUI.apk Settings.apk" value-sh="glog fix_key_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK§Note: MiuiSystemUI.apk (global)" required="true"/>
      <param name="ime_app" placeholder="com.google.android.inputmethod.latin" desc="'$desc_ime_app'" type="text" value-sh="glog ime_app" required="true"/>
      <param name="ime_color" placeholder="#f0f3f8" desc="'$desc_color_light'" type="text" value-sh="glog ime_color" required="true"/>
      <param name="ime_color_dark" placeholder="#1e1f21" desc="'$desc_color_dark'" type="text" value-sh="glog ime_color_dark" required="true"/>
      <param name="ime_dimen" desc="'$desc_dimen'" type="text" value-sh="glog ime_dimen" />
      <set>
        slog ime_app "$ime_app"
        slog ime_color "$ime_color"
        slog ime_color_dark "$ime_color_dark"
        slog ime_dimen "$ime_dimen"
        slog fix_key_patch_os "$FILE"
        '$pathsh' fixkey "$FILE"
        checktime
      </set>
    </action>

    <action title="'$title_many_patch'" summary="Xiaomi, Android 12+">
      <param name="FILE" option-sh="'$pathsh' search services.jar miui-services.jar PowerKeeper.apk miui-framework.jar" value-sh="glog fix_manyo_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" required="true"/>
      <param name="fix_screen" label="'$label_fix_screen'" desc="'$required_files_text': miui-services.jar, services.jar" type="bool" />
      <param name="fix_fps" label="'$label_fix_fps'" desc="'$required_files_text': PowerKeeper.apk" type="bool" />
      <param name="fix_reset_theme" label="'$label_fix_reset_theme'" desc="'$required_files_text': miui-framework.jar" type="bool" />
      <param name="fix_show_error" label="'$label_fix_show_error'" desc="'$required_files_text': services.jar" type="bool" />
      <param name="fix_fpscam" label="'$label_fix_fps_cam'" desc="'$required_files_text': miui-services.jar" type="bool" />
      <set>
        slog fix_manyo_patch_os "$FILE"
        '$pathsh' fixmultiple "$FILE"
        checktime
      </set>
    </action>

    <action title="'$title_app_patch'" summary="Xiaomi">
      <param name="FILE" option-sh="'$pathsh' search *PersonalAssistant*.apk MIUIWeather.apk Joyose.apk Provision.apk MIUIGallery.apk *SecurityCenter.apk *ThemeManager.apk" value-sh="glog fix_manyo_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" required="true"/>
      <param name="fix_themes" label="'$label_fix_themes'" desc="'$required_files_text': ThemeManager.apk" type="bool" />
      <param name="fix_appvault" label="'$label_fix_appvault'" desc="'$required_files_text': PersonalAssistant.apk" type="bool" />
      <param name="fix_thoit" label="'$label_fix_thoit'" desc="'$required_files_text': MIUIWeather.apk" type="bool" />
      <param name="fix_joyose" label="'$label_fix_joyose'" desc="'$required_files_text': Joyose.apk" type="bool" />
      <param name="fix_mapcn" label="'$label_fix_mapcn'" desc="'$required_files_text': MIUIGallery.apk" type="bool" />
      <param name="fix_gmscn" label="'$label_fix_gmscn'" desc="'$required_files_text': Provision.apk" type="bool" />
      <param name="fix_off_10s" label="'$label_fix_off_10s'" desc="'$required_files_text': SecurityCenter.apk" type="bool" />
      <set>
        slog fix_manyo_patch_os "$FILE"
        '$pathsh' fixapps "$FILE"
        checktime
      </set>
    </action>
  </group>

  <group>
    <action title="'"$action_title"'" summary="'"$action_desc"'" warn="'"$action_warn"'" interruptible="false">
      <lock>
        [ "$ROT" == 0 ] && echo "'$root_warrn'" || echo 0
      </lock>
      <param name="kill_apk_list" title="'"$param1_title"'" option-sh="'$pathsh' list_apk_file" value-sh="glog kill_apk_list" required="true" label="'"$param1_label"'" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" />
      <param name="open_app" label="'$open_app_text_2'" value-sh="glog open_app_bool" type="switch" />
      <param name="dem_giay" value-sh="glog dem_giay 60" label="'"$param2_label"'" type="seekbar" min="5" max="300" desc="'"$param2_desc"'" />
      <param name="kill_customize" label="'"$param3_label"'" value-sh="glog kill_customize" type="text" placeholder="com.android.systemui" desc="'"$param3_desc"'" />
      <set>
        slog dem_giay $dem_giay
        slog kill_customize "$kill_customize"
        slog open_app_bool "$open_app"
        slog kill_apk_list "$kill_apk_list"
        '$MPAT'/index.bash test_app "$kill_apk_list"
      </set>
    </action>
  </group>

  <group>
    <action title="'$add_another_app_text'" warn="'$add_another_app_text_2', '$add_another_app_text_3'" summary="Xiaomi">
      <param name="add_app" label="InstallerX Revived" desc="'$string_text_2': (CN: product), (global: system)" type="bool" />
      <param name="add_app_2" label="Safetycore" desc="'$string_text_2': product" type="bool" />
      <set>
        '$pathsh' online_app
        checktime
      </set>
    </action>
  </group>

  <group>
    <action title="'$title_boot_patch'" warn="'$title_boot_patch2', '$title_boot_patch3_desc'">
      <param name="FOLDER" option-sh="findfile 0 $SDH/$PTSH | grep boot" title="'$decrypted_partition_text'" desc="'$string_text_2': vendor_boot, boot" label="'$select_text'" required="true"/>
      <param name="fix_fake_lock" label="'$title_boot_patch3'" desc="" type="bool" />
      <param name="fix_diselinux" label="'$title_boot_patch4'" options-sh="echo -e '"'0|'$default_text'\n1|enforcing\n2|permissive\n3|disabled'"'" />
      <set>
        '$pathsh' patch_boot "$FOLDER"
      </set>
    </action>
  </group>

</group>'
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
pathsh="$MPAT/patch-rom"

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/default.prop" | sed "/google_text=/d")"
[ -f "$MPAT/language.bash" ] && source "$MPAT/language.bash"

# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ]; then
  trans_add "$MPAT"
  [ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

# Điền dữ liệu mặc định
if [ -z "$(glog ime_color_dark)" ]; then
  slog ime_dimen '<dimen name="input_method_seek_bar_margin">6.5999756dp</dimen>
<dimen name="input_bottom_height">45.599976dp</dimen>
<dimen name="input_bottom_button_height">28.5dp</dimen>
<dimen name="input_bottom_button_margin_top">2.5dp</dimen>'
  slog ime_app com.google.android.inputmethod.latin
  slog ime_color '#f0f3f8'
  slog ime_color_dark '#1e1f21'
fi

# Index
if [ "$(type -t "$1")" = "function" ]; then
  "$@"
else
  echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
  cat "$ETC/error.xml"
  echo '</group>'
fi