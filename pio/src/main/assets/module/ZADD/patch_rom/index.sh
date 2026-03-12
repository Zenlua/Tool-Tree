#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

update(){
if checkonline; then
cd "$MPAT"
for kvv in addon.prop Patch-Xiaomi.bash language.sh index.sh menu.sh mod.7z early_start.sh changelog.txt; do
downloadb "https://raw.githubusercontent.com/Zenlua/Tool-Tree/refs/heads/main/pio/src/main/assets/module/ZADD/patch_rom/$kvv" $kvv
done
    if [ -f $MPAT/mod.7z ];then
    echo "7z mod extract..."
    7z x -t7z -y $MPAT/mod.7z -o$MPAT
    rm -f $MPAT/mod.7z
    fi
[ -f update ] && rm -f update
[ -f changelog.txt ] && cat changelog.txt
fi
}

home(){

# lấy phiên bản
if [ ! -f "$MPAT/mod/version" ];then
    if checkonline; then
    linkurrl="$(xem https://api.github.com/repos/Wuang26/Kaorios-Toolbox/releases/latest 2>/dev/null)"
    echo "$(echo "$linkurrl" | jq -r '.tag_name')" > $MPAT/mod/version
    fi
fi

(
# check update add-on
if [ ! -f $MPAT/update ];then
number_ver="$(xem https://raw.githubusercontent.com/Zenlua/Tool-Tree/refs/heads/main/pio/src/main/assets/module/ZADD/patch_rom/addon.prop 2>/dev/null | grep -m1 "versionCode=" | cut -d= -f2)"
number_ver2="$(gprop versionCode "$MPAT/addon.prop")"
    if [[ ${number_ver:-0} -gt $number_ver2 ]];then
    echo 1 >$MPAT/update
    id_random="$RANDOM"
    notiservice --am --id $id_random --title "Patch ROM Xiaomi" --message "$addon_noti"
    sleep 10
    notiservice --am --id $id_random -d true
    fi
fi
) &

# điền dữ liệu mặc định
if [ -z "$(glog list_oat_tex)" ];then
glog ime_dimen '<dimen name="input_method_seek_bar_margin">6.5999756dp</dimen>
<dimen name="input_bottom_height">45.599976dp</dimen>
<dimen name="input_bottom_button_height">28.5dp</dimen>
<dimen name="input_bottom_button_margin_top">2.5dp</dimen>' >/dev/null
glog ime_app com.google.android.inputmethod.latin >/dev/null
glog ime_color '#f0f3f8' >/dev/null
glog ime_color_dark '#1e1f21' >/dev/null
glog list_oat_tex "/system_ext/priv-app/Settings/Settings.apk
/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk
/product/app/MIUIFrequentPhrase/MIUIFrequentPhrase.apk
/system/app/PowerKeeper/PowerKeeper.apk" >/dev/null
fi

# tải xuống nền mới nhất lần đầu
(
if [ ! -f "$MPAT/mod/classes.dex" ];then
if checkonline; then
[ "$linkurrl" ] || linkurrl="$(xem https://api.github.com/repos/Wuang26/Kaorios-Toolbox/releases/latest 2>/dev/null)"
downloadb "$(echo "$linkurrl" | jq -r '.assets[].browser_download_url' | grep 'classes.*\.dex')" "$MPAT/mod/classes.dex"
downloadb "$(echo "$linkurrl" | jq -r '.assets[].browser_download_url' | grep 'KaoriosToolbox.*\.apk')" "$MPAT/mod/KaoriosToolbox.apk"
downloadb "$(echo "$linkurrl" | jq -r '.assets[].browser_download_url' | grep 'com.kousei.kaorios.xml')" "$MPAT/mod/com.kousei.kaorios.xml"
fi
fi
) &>/dev/null &

echo '<?xml version="1.0" encoding="UTF-8" ?><group>

<group>
<action auto-off="true" reload="true" visible="cat '$MPAT'/update 2>/dev/null" warn="'$update_text'">
<title>'$update_text1'</title>
<set>'$MPAT'/index.sh update</set>
</action>
</group>

<group title="'$google_text'">
<action shell="hidden" reload="true">
<title>'$TITLE_CHANGE_PROJECT'</title>
<summary>'$SUMMARY_CURRENT': '${patch_rom_path##*/}'</summary>
<param name="NAME" label="'$LABEL_SELECT'" option-sh="findfile for $SDH" value-sh="glog patch_rom_path"/>
<set>slog patch_rom_path "$NAME"</set>
</action>
<text desc="'$NOTE_PATCH'" />
</group>

<group>
<action title="'$TITLE_FAST_PATCH'" auto-off="true">
<param name="patch_prop" options-sh="echo -e '"'log\ndisable'"'" title="'$TITLE_PROP'" desc="'$DESC_PROP'" label="'$LABEL_SELECT'" value-sh="'$patch_mi' patch_prop check"/>
<param name="device_features" label="'$LABEL_DISABLE_OTA'" value-sh="'$patch_mi' device_features check" type="switch" />
<param name="crypto_prop" label="'$LABEL_CRYPTO'" value-sh="'$patch_mi' crypto_prop check" type="switch" />
<param name="rw_rom" label="'$LABEL_RW_ROM'" value-sh="'$patch_mi' check_prop rw_rom" type="switch" />
<param name="home_poco" label="'$LABEL_HOME_POCO'" desc="'$DESC_HOME_POCO'" value-sh="'$patch_mi' home_poco check" type="switch" />
<set>
[ "$('$patch_mi' patch_prop check)" == "$patch_prop" ] || '$patch_mi' patch_prop
[ "$device_features" == 0 ] || '$patch_mi' device_features
[ "$crypto_prop" == 0 ] || '$patch_mi' crypto_prop
[ "$rw_rom" == 0 ] || '$patch_mi' rw_rom
[ "$home_poco" == 0 ] || '$patch_mi' home_poco
</set>
</action>
</group>

<group>
<action title="'$TITLE_REMOVE_APP'" >
<param name="del_app" type="text" value-sh="glog del_app_patch" placeholder="VoiceAssist Sogou"/>
<set>
slog del_app_patch "$del_app"
'$patch_mi' del_app
</set>
</action>
</group>

<group>
<action shell="hidden" title="'$TITLE_KEYBOARD'">
<param name="ime_app" placeholder="com.google.android.inputmethod.latin" desc="'$DESC_IME_APP'" type="text" value-sh="glog ime_app" />
<param name="ime_color" placeholder="#f0f3f8" desc="'$DESC_COLOR_LIGHT'" type="text" value-sh="glog ime_color" />
<param name="ime_color_dark" placeholder="#1e1f21" desc="'$DESC_COLOR_DARK'" type="text" value-sh="glog ime_color_dark" />
<param name="ime_dimen" desc="'$DESC_DIMEN'" type="text" value-sh="glog ime_dimen" />
<set>
slog ime_app "$ime_app"
slog ime_color "$ime_color"
slog ime_color_dark "$ime_color_dark"
slog ime_dimen "$ime_dimen"
</set>
</action>

<action title="'$TITLE_SYSTEM_PATCH'" >
<param name="fix_noti" label="'$LABEL_FIX_NOTI'" type="switch" value-sh="'$patch_mi' check_prop fix_noti" />
<param name="fix_fps" label="'$LABEL_FIX_FPS'" type="switch" value-sh="'$patch_mi' check_prop fix_fps" />
<param name="fix_window" label="'$LABEL_FIX_WINDOW'" type="switch" value-sh="'$patch_mi' check_prop fix_window" />
<param name="fix_reset_theme" label="'$LABEL_FIX_THEME'" type="switch" value-sh="'$patch_mi' check_prop fix_reset_theme" />
<param name="fix_global" label="'$LABEL_FIX_GLOBAL'" type="switch" value-sh="'$patch_mi' check_prop fix_global" />
<param name="fix_show_error" label="'$LABEL_FIX_ERROR'" type="switch" value-sh="'$patch_mi' check_prop fix_show_error" />
<param name="fix_ime" label="'$LABEL_FIX_IME'" type="switch" value-sh="'$patch_mi' check_prop fix_ime" />
<param name="fix_fwko" label="'$LABEL_FIX_FWKO' '$(cat $MPAT/mod/version 2>/dev/null)'" type="switch" value-sh="'$patch_mi' check_prop fix_fwko" />
<param name="fix_screen" label="'$LABEL_FIX_SCREEN'" type="switch" value-sh="'$patch_mi' check_prop fix_screen" />
<param name="fix_apksign" label="'$LABEL_FIX_APKSIGN'" type="switch" value-sh="'$patch_mi' check_prop fix_apksign" />
<param name="fix_appvault" label="'$LABEL_FIX_APPVAULT'" type="switch" value-sh="'$patch_mi' check_prop fix_appvault" />
<param name="fix_themes" label="'$LABEL_FIX_THEMES'" type="switch" value-sh="'$patch_mi' check_prop fix_themes" />
<param name="fix_thoit" label="'$LABEL_FIX_WEATHER'" type="switch" value-sh="'$patch_mi' check_prop fix_thoit" />
<param name="fix_joyose" label="'$LABEL_FIX_JOYOSE'" type="switch" value-sh="'$patch_mi' check_prop fix_joyose" />
<param name="fix_mapcn" label="'$LABEL_FIX_MAP'" type="switch" value-sh="'$patch_mi' check_prop fix_mapcn" />
<set>

[ "$fix_appvault" == 1 ] && '$patch_mi' PersonalAssistant
[ "$('$patch_mi' check_nums fix_appvault)" == 1 ] && '$patch_mi' set_prop fix_appvault

[ "$fix_themes" == 1 ] && '$patch_mi' ThemeManager
[ "$('$patch_mi' check_nums fix_themes)" == 1 ] && '$patch_mi' set_prop fix_themes

[ "$fix_thoit" == 1 ] && '$patch_mi' Weather
[ "$('$patch_mi' check_nums fix_thoit)" == 1 ] && '$patch_mi' set_prop fix_thoit

[ "$fix_joyose" == 1 ] && '$patch_mi' Joyose
[ "$('$patch_mi' check_nums fix_joyose)" == 1 ] && '$patch_mi' set_prop fix_joyose

[ "$fix_mapcn" == 1 ] && '$patch_mi' MIUIGallery
[ "$('$patch_mi' check_nums fix_mapcn)" == 1 ] && '$patch_mi' set_prop fix_mapcn

[ "$fix_fwko$fix_apksign" == 00 ] || '$patch_mi' framework
[ "$fix_show_error$fix_screen$fix_apksign" == 000 ] || '$patch_mi' services
[ "$fix_reset_theme$fix_global$fix_ime" == 000 ] || '$patch_mi' miui-framework
[ "$fix_ime$fix_global" == 00 ] || '$patch_mi' Settings
[ "$fix_ime" == 1 ] && '$patch_mi' FrequentPhrase
[ "$fix_noti$fix_window$fix_global$fix_ime$fix_apksign" == 00000 ] || '$patch_mi' miui-services
[ "$fix_noti$fix_ime" == 00 ] || '$patch_mi' MiuiSystemUI
[ "$fix_noti$fix_fps" == 00 ] || '$patch_mi' PowerKeeper

[ "$('$patch_mi' check_nums fix_noti)" == 3 ] && '$patch_mi' set_prop fix_noti
[ "$('$patch_mi' check_nums fix_fps)" == 1 ] && '$patch_mi' set_prop fix_fps
[ "$('$patch_mi' check_nums fix_reset_theme)" == 1 ] && '$patch_mi' set_prop fix_reset_theme
[ "$('$patch_mi' check_nums fix_global)" == 3 ] && '$patch_mi' set_prop fix_global
[ "$('$patch_mi' check_nums fix_window)" == 1 ] && '$patch_mi' set_prop fix_window
[ "$('$patch_mi' check_nums fix_show_error)" == 1 ] && '$patch_mi' set_prop fix_show_error
[ "$('$patch_mi' check_nums fix_ime)" == 5 ] && '$patch_mi' set_prop fix_ime
[ "$('$patch_mi' check_nums fix_fwko)" == 1 ] && '$patch_mi' set_prop fix_fwko
[ "$('$patch_mi' check_nums fix_screen)" == 2 ] && '$patch_mi' set_prop fix_screen
[ "$('$patch_mi' check_nums fix_apksign)" == 3 ] && '$patch_mi' set_prop fix_apksign

rm -fr "'$MPAT'/apk" 2>/dev/null
</set>
</action>
</group>

<group>
<action title="'$TITLE_DEX2OAT'" >
<param name="oat_fw_at" label="'$LABEL_OAT_FW'" type="switch" value="1"/>
<param name="list_oat_tex" options-sh="PTSH='${patch_rom_path##*/}' $AON/add_features/bin/listapk" multiple="multiple" value-sh="glog list_oat_tex"/>
<param name="mi_secontex" desc="'$DESC_SECONTEXT'" value-sh="glog mi_secontex" placeholder="PCL[]" type="text"/>
<set>
slog list_oat_tex "$list_oat_tex"
slog mi_secontex "$mi_secontex"
'$patch_mi' tao_oat
</set>
</action>
</group>

</group>' | sed -e 's|\&|\&amp;|g' -e 's|§|\&#xA;|g'
}

# Thư mục hiện tại
MPAT="${0%/*}"
patch_rom_path="$(glog patch_rom_path "$SDH/$PTSH")"
patch_mi="$MPAT/Patch-Xiaomi.bash"

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
[ -f "$MPAT/language.sh" ] && source "$MPAT/language.sh"

# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
trans_add "$MPAT"
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

# index
case "$1" in
    home|update)
        "$1"
        ;;
    *)
        cat "$ETC/error.xml"
        ;;
esac
