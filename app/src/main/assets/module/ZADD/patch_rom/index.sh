#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

home(){
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>

<group title="'$google_text'">
<action title="'$title_quick'" auto-off="true">
<summary>'"$project_text: $PTSH"'</summary>
<param name="patch_prop" options-sh="echo -e '"'none\nenforce\nlog\ndisable'"'" title="'$desc_patch_prop'" desc="'$desc_patch_prop_long'" label="'$select_text'"/>
<param name="device_features" label="'$label_device_features'" type="bool" />
<param name="rw_rom" label="'$label_rw_rom'" desc="'$desc_rw_rom'" type="bool" />
<param name="delete_gms" label="'$label_delete_gms'" desc="'$desc_delete_gms'" type="bool" />
<param name="home_poco" label="'$label_home_poco'" desc="'$desc_home_poco'" type="bool" />
<set>'$pathsh' custom_patch</set>
<param name="rezetprop_patch" label="'$label_reset_prop'" desc="'$desc_reset_prop'" type="bool" />
<set>'$pathsh' rezetprop_patch</set>
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

<action title="'$title_delete'" >
<param name="del_app_patch" type="text" value-sh="glog del_app_patch" required="true" placeholder="VoiceAssist Sogou"/>
<set>
slog del_app_patch "$del_app_patch"
'$pathsh' del_app "$del_app_patch"
</set>
</action>
</group>

<group>
<action title="'$title_framework_patch'" summary="Android 12+">
<param name="FILE" option-sh="'$pathsh' search framework.jar services.jar miui-services.jar" multiple="true" value-sh="glog kaorios_toolbox_patch_os" required="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" />
<param name="fix_toolbox" label="'$label_fix_toolbox'" desc="'$required_files_text': framework.jar" type="bool" />
<param name="fix_apksign" label="'$label_fix_apksign'" desc="'$required_files_text': framework.jar, services.jar, (Xiaomi: miui-services.jar)" type="bool" />
<set>
slog kaorios_toolbox_patch_os "$FILE"
'$pathsh' toolbox "$FILE"
checktime
</set>
</action>

<action title="'$title_cn_global'" summary="Xiaomi, Android 12+">
<param name="FILE" option-sh="'$pathsh' search miui-framework.jar miui-services.jar PowerKeeper.apk MiuiSystemUI.apk Settings.apk" value-sh="glog fix_noti_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" required="true"/>
<param name="fix_noti" label="'$label_fix_noti'" desc="'$required_files_text': miui-framework.jar, miui-services.jar, PowerKeeper.apk, MiuiSystemUI.apk" type="bool" />
<param name="fix_global" label="'$label_fix_global'" desc="'$required_files_text': miui-framework.jar, miui-services.jar, Settings.apk" type="bool" />
<set>
slog fix_noti_patch_os "$FILE"
'$pathsh' fixnoti "$FILE"
checktime
</set>
</action>
</group>

<group>
<action title="'$title_ime'" summary="Xiaomi">
<param name="FILE" option-sh="'$pathsh' search miui-framework.jar miui-services.jar *FrequentPhrase.apk MiuiSystemUI.apk Settings.apk" value-sh="glog fix_key_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" required="true"/>
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
</group>

<group>
<action title="'$title_many_patch'" summary="Xiaomi, Android 12+">
<param name="FILE" option-sh="'$pathsh' search services.jar miui-services.jar PowerKeeper.apk miui-framework.jar" value-sh="glog fix_manyo_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" required="true"/>
<param name="fix_screen" label="'$label_fix_screen'" desc="'$required_files_text': miui-services.jar, services.jar" type="bool" />
<param name="fix_fps" label="'$label_fix_fps'" desc="'$required_files_text': PowerKeeper.apk" type="bool" />
<param name="fix_window" label="'$label_fix_window'" desc="'$required_files_text': miui-services.jar" type="bool" />
<param name="fix_reset_theme" label="'$label_fix_reset_theme'" desc="'$required_files_text': miui-framework.jar" type="bool" />
<param name="fix_show_error" label="'$label_fix_show_error'" desc="'$required_files_text': services.jar" type="bool" />
<set>
slog fix_manyo_patch_os "$FILE"
'$pathsh' fixmultiple "$FILE"
checktime
</set>
</action>

<action title="'$title_app_patch'" summary="Xiaomi">
<param name="FILE" option-sh="'$pathsh' search *ThemeManager.apk *PersonalAssistant*.apk MIUIWeather.apk Joyose.apk MIUIGallery.apk" value-sh="glog fix_manyo_patch_os" multiple="true" desc="'$string_text_1': '$PTSH'/***, /sdcard/TREE/APK" required="true"/>
<param name="fix_themes" label="'$label_fix_themes'" desc="'$required_files_text': ThemeManager.apk" type="bool" />
<param name="fix_appvault" label="'$label_fix_appvault'" desc="'$required_files_text': PersonalAssistant.apk" type="bool" />
<param name="fix_thoit" label="'$label_fix_thoit'" desc="'$required_files_text': MIUIWeather.apk" type="bool" />
<param name="fix_joyose" label="'$label_fix_joyose'" desc="'$required_files_text': Joyose.apk" type="bool" />
<param name="fix_mapcn" label="'$label_fix_mapcn'" desc="'$required_files_text': MIUIGallery.apk" type="bool" />
<set>
slog fix_manyo_patch_os "$FILE"
'$pathsh' fixapps "$FILE"
checktime
</set>
</action>
</group>

</group>' | sed -e 's|\&|\&amp;|g' -e 's|§|\&#xA;|g'
}

# điền dữ liệu mặc định
if [ -z "$(glog ime_color_dark)" ];then
slog ime_dimen '<dimen name="input_method_seek_bar_margin">6.5999756dp</dimen>
<dimen name="input_bottom_height">45.599976dp</dimen>
<dimen name="input_bottom_button_height">28.5dp</dimen>
<dimen name="input_bottom_button_margin_top">2.5dp</dimen>'
slog ime_app com.google.android.inputmethod.latin
slog ime_color '#f0f3f8'
slog ime_color_dark '#1e1f21'
fi

# Thư mục hiện tại
MPAT="${0%/*}"
pathsh="$MPAT/patch-rom.bash"

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
[ -f "$MPAT/language.sh" ] && source "$MPAT/language.sh"

# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
trans_add "$MPAT"
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

(
# check update add-on
if checkonline; then
    number_ver="$(xem https://raw.githubusercontent.com/Zenlua/Tool-Tree/refs/heads/main/app/src/main/assets/module/ZADD/patch_rom/addon.prop 2>/dev/null | grep -m1 "versionCode=" | cut -d= -f2)"
    number_ver2="$(gprop versionCode "$MPAT/addon.prop")"
    if [[ ${number_ver:-0} -gt $number_ver2 ]];then
        echo 1 >$MPAT/update
        showtoast "$addon_noti"
    else
    [ -f $MPAT/update ] && rm -f $MPAT/update
    fi
fi
) &

update(){
if checkonline; then
cd "$MPAT"
for kvv in addon.prop patch-rom.bash language.sh index.sh menu.sh mod.7z early_start.sh changelog.txt; do
downloadb "https://raw.githubusercontent.com/Zenlua/Tool-Tree/refs/heads/main/app/src/main/assets/module/ZADD/patch_rom/$kvv" $kvv 2>&1
done
    if [ -f $MPAT/mod.7z ];then
    echo "7z mod extract..."
    7z x -t7z -y $MPAT/mod.7z -o$MPAT/mod >/dev/null
    rm -f $MPAT/mod.7z
    chmod -R 755 $MPAT
    fi
[ -f update ] && rm -f update
if [ -f changelog.txt ]; then
cat changelog.txt
sleep 8
fi
else
    killtree "$network_text"
fi
}

# index
if [ "$(type -t "$1")" = "function" ];then
"$@"
else
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
cat "$ETC/error.xml"
echo '</group>'
fi
