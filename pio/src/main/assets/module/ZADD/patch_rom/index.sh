#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

# làm cho mọi thứ ra ngoài to ra không phải ở 1 group 
home(){
echo '<?xml version="1.0" encoding="UTF-8" ?><group>

<group title="'$google_text'">
<action shell="hidden" reload="true">
<title>Thay đổi dự án</title>
<summary>Hiện tại: '${patch_rom_path##*/}'</summary>
<param name="NAME" label="Lựa chọn" option-sh="findfile for $SDH" value-sh="glog patch_rom_path"/>
<set>slog patch_rom_path "$NAME"</set>
</action>
<text desc="Lưu ý: Sau khi thực hiện các tính năng được bật sẽ được áp dụng vào dự án và không thể tắt tính năng đó nữa, giải mã lại toàn bộ phân vùng nếu muốn thay đổi các tính năng khác" />
</group>

<group>
<action title="Vá nhanh cơ bản" auto-off="true">
<param name="patch_prop" options-sh="echo -e '"'log\ndisable'"'" title="Thay đổi ro.control_privapp_permissions" desc="enforce: nếu thiếu quyền có thể bị bootloop, log: thông báo lỗi vào logcat và không cấp quyền, disable: tự động cấp quyền còn thiếu cho app" label="Lựa chọn" value-sh="'$patch_mi' patch_prop check"/>
<param name="device_features" label="Tắt update ota" value-sh="'$patch_mi' device_features check" type="switch" />
<param name="crypto_prop" label="Thêm ro.crypto.state=encrypted" value-sh="'$patch_mi' crypto_prop check" type="switch" />
<param name="rw_rom" label="Vá RW rom erofs" value-sh="'$patch_mi' check_prop rw_rom" type="switch" />
<set>
[ "$('$patch_mi' patch_prop check)" == "$patch_prop" ] || '$patch_mi' patch_prop
[ "$device_features" == 0 ] || '$patch_mi' device_features
[ "$crypto_prop" == 0 ] || '$patch_mi' crypto_prop
[ "$rw_rom" == 0 ] || '$patch_mi' rw_rom
</set>
</action>
</group>

<group>
<action title="Xoá app rác" >
<param name="del_app" type="text" value-sh="glog del_app_patch" placeholder="VoiceAssist Sogou"/>
<set>
slog del_app_patch "$del_app"
'$patch_mi' del_app
</set>
</action>
</group>

<group>
<action shell="hidden" title="Tùy chỉnh bàn phím">
<param name="ime_app" placeholder="com.google.android.inputmethod.latin" desc="Ứng dụng bàn phím" type="text" value-sh="glog ime_app" />
<param name="ime_color" placeholder="#f0f3f8" desc="Mã màu nền sáng" type="text" value-sh="glog ime_color" />
<param name="ime_color_dark" placeholder="#1e1f21" desc="Mã màu nền tối" type="text" value-sh="glog ime_color_dark" />
<param name="ime_dimen" desc="Điều chỉnh dimen chiều rộng" type="text" value-sh="glog ime_dimen" />
<set>
slog ime_app "$ime_app"
slog ime_color "$ime_color"
slog ime_color_dark "$ime_color_dark"
slog ime_dimen "$ime_dimen"
</set>
</action>

<action title="Bản vá hệ thống" >
<param name="fix_noti" label="Sửa lỗi thông báo chậm CN" type="switch" value-sh="'$patch_mi' check_prop fix_noti" />
<param name="fix_fps" label="Mở khóa fps Max" type="switch" value-sh="'$patch_mi' check_prop fix_fps" />
<param name="fix_window" label="Tối đa 6 cửa sổ nhỏ" type="switch" value-sh="'$patch_mi' check_prop fix_window" />
<param name="fix_reset_theme" label="Sửa lỗi reset theme" type="switch" value-sh="'$patch_mi' check_prop fix_reset_theme" />
<param name="fix_global" label="Tính năng global rom CN" type="switch" value-sh="'$patch_mi' check_prop fix_global" />
<param name="fix_show_error" label="Xoá hộp thoại lỗi vân tay khi khởi động" type="switch" value-sh="'$patch_mi' check_prop fix_show_error" />
<param name="fix_ime" label="Bàn phím nâng cao" type="switch" value-sh="'$patch_mi' check_prop fix_ime" />
<param name="fix_fwko" label="Thêm Kaorios Toolbox '$(cat $MPAT/mod/version 2>/dev/null)'" type="switch" value-sh="'$patch_mi' check_prop fix_fwko" />
<param name="fix_screen" label="Mở khóa giới hạn chụp ảnh màn hình" type="switch" value-sh="'$patch_mi' check_prop fix_screen" />
<param name="fix_apksign" label="Bỏ qua xác minh chữ ký" type="switch" value-sh="'$patch_mi' check_prop fix_apksign" />
<param name="fix_appvault" label="Bẻ khóa giao dịch Appvault" type="switch" value-sh="'$patch_mi' check_prop fix_appvault" />
<param name="fix_themes" label="Bẻ khóa giao dịch Theme" type="switch" value-sh="'$patch_mi' check_prop fix_themes" />
<param name="fix_thoit" label="Hiện aqi ở thời tiết CN" type="switch" value-sh="'$patch_mi' check_prop fix_thoit" />
<param name="fix_joyose" label="Mod ứng dụng Joyose" type="switch" value-sh="'$patch_mi' check_prop fix_joyose" />
<param name="fix_mapcn" label="Xoá map china ở thư viện CN" type="switch" value-sh="'$patch_mi' check_prop fix_mapcn" />
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
[ "$('$patch_mi' check_nums fix_screen)" == 1 ] && '$patch_mi' set_prop fix_screen
[ "$('$patch_mi' check_nums fix_apksign)" == 3 ] && '$patch_mi' set_prop fix_apksign

rm -fr "'$MPAT'/apk" 2>/dev/null
</set>
</action>
</group>

<group>
<action title="Dex2oat toàn bộ" >
<param name="oat_fw_at" label="Tạo oat framework, service" type="switch" value="1"/>
<param name="list_oat_tex" options-sh="PTSH=${patch_rom_path##*/} $AON/add_features/bin/listapk | sed '"/none/d"' " multiple="multiple" value-sh="glog list_oat_tex"/>
<set>
slog list_oat_tex "$list_oat_tex"
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

if [ -z "$(glog list_oat_tex)" ];then
glog ime_dimen '<dimen name="input_method_seek_bar_margin">6.5999756dp</dimen>
<dimen name="input_bottom_height">45.599976dp</dimen>
<dimen name="input_bottom_button_height">28.5dp</dimen>
<dimen name="input_bottom_button_margin_top">2.5dp</dimen>' >/dev/null
glog ime_app com.google.android.inputmethod.latin >/dev/null
glog ime_color '\#f0f3f8' >/dev/null
glog ime_color_dark '\#1e1f21' >/dev/null
glog list_oat_tex "/system_ext/priv-app/Settings/Settings.apk
/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk
/product/app/MIUIFrequentPhrase/MIUIFrequentPhrase.apk
/system/app/PowerKeeper/PowerKeeper.apk" >/dev/null
fi

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
# Google dịch
if [ "$(glog "auto_trans_text_${1##*/}")" == 1 ];then
trans_add "$MPAT"
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

if [ -f "$MPAT/mod/version" ];then
if checkonline; then
linkurrl="$(xem https://api.github.com/repos/Wuang26/Kaorios-Toolbox/releases/latest 2>/dev/null)"
echo "$(echo "$linkurrl" | jq -r '.tag_name')" > $MPAT/mod/version
fi
fi

(
if [ ! -f "$MPAT/mod/classes.dex" ];then
if checkonline; then
[ "$linkurrl" ] || linkurrl="$(xem https://api.github.com/repos/Wuang26/Kaorios-Toolbox/releases/latest 2>/dev/null)"
downloadb "$(echo "$linkurrl" | jq -r '.assets[].browser_download_url' | grep 'KaoriosToolbox.*\.apk')" "$MPAT/mod/KaoriosToolbox.apk"
downloadb "$(echo "$linkurrl" | jq -r '.assets[].browser_download_url' | grep 'com.kousei.kaorios.xml')" "$MPAT/mod/com.kousei.kaorios.xml"
downloadb "$(echo "$linkurrl" | jq -r '.assets[].browser_download_url' | grep 'classes.*\.dex')" "$MPAT/mod/classes.dex"
fi
fi
) &>/dev/null &

# index
case "$1" in
    home)
        "$1"
        ;;
    *)
        cat "$ETC/error.xml"
        ;;
esac
