#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic
path_modun="/data/adb/modules/Tool-Tree"
path_modun2="/data/adb/modules_update/Tool-Tree"

home(){
# Đoạn code chính
xml_print '<group title="'$google_text'">
<action warn="'$lang_action_warn'" >
<title>'$lang_title'</title>
<summary>'$lang_summary_path''$path_modun'</summary>
<param name="uri_file_modun" value-sh="glog uri_file_modun" type="file" required="required" />
<param name="uri_adb_moduls" desc="'$lang_desc_adb'" placeholder="/system_ext/priv-app/Settings/Settings.apk" value-sh="glog uri_adb_moduls" type="text" />
<param name="prop_modunls" desc="'$lang_desc_prop'" placeholder="ro.control_privapp_permissions=log" value-sh="cat '$path_modun'/system.prop 2>/dev/null" type="text" />
<set>
slog uri_adb_moduls "$uri_adb_moduls"
slog uri_file_modun "$uri_file_modun"
[ -d '$path_modun2' ] && rm -fr '$path_modun2'
[ -f '$path_modun'/remove ] && rm -fr '$path_modun'/remove
mkdir -p '$path_modun' '$path_modun2'
echo "$prop_modunls" > '$path_modun2'/system.prop
if [ "$uri_adb_moduls" ]; then
mkdir -p "'$path_modun2'${uri_adb_moduls%/*}"
cp -rf "$uri_file_modun" "'$path_modun2'$uri_adb_moduls"
echo "'$lang_save_at''$path_modun2'$uri_adb_moduls"
echo
else
echo "'$lang_searching'"
echo
link_find_file="$(find -L /system -name "${uri_file_modun##*/}" -print -quit)"
    if [ "$link_find_file" ]; then
    mkdir -p "'$path_modun2'${link_find_file%/*}"
    cp -rf "$uri_file_modun" "'$path_modun2'$link_find_file"
    echo "'$lang_save_at''$path_modun2'$link_find_file"
    echo
    else
    echo "'$lang_not_found'\${uri_file_modun##*/}'$lang_input_notice'"
    fi
fi
echo "id=Tool-Tree
name=Tool-Tree Module
version=1.0
versionCode=100
author=Kakathic
description=Modified system files" | tee '$path_modun'/module.prop
chmod 644 '$path_modun'/module.prop '$path_modun2'/system.prop
touch '$path_modun'/update
set_permis -R -o 0:0 -c u:object_r:system_file:s0 '$path_modun2'/system
</set>
</action>
</group>'
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
[ -f "$MPAT/language.sh" ] && source "$MPAT/language.sh"

# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
trans_add "$MPAT"
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

# index
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
# index
if [ "$(type -t "$1")" = "function" ];then
"$@"
else
cat "$ETC/error.xml"
fi
echo '</group>'
