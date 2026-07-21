#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic
path_modun="/data/adb/modules/Tool-Tree"
path_modun2="/data/adb/modules_update/Tool-Tree"

home() {
    # Đoạn code chính
    echo '<group title="'$google_text'">
    <action warn="'$lang_action_warn'">
        <title>'$lang_title'</title>
        <summary>'$lang_summary_path''$path_modun'</summary>
        <param name="uri_file_modun" value-sh="glog uri_file_modun" options-sh="findfile files $PTAD" required="required" label="@string/options_text" multiple="true"/>
        <param name="uri_adb_moduls" desc="'$lang_desc_adb'" placeholder="/system_ext/priv-app/Settings/Settings.apk" value-sh="glog uri_adb_moduls" type="text" />
        <param name="prop_modunls" desc="'$lang_desc_prop'" placeholder="ro.control_privapp_permissions=log" value-sh="cat '$path_modun'/system.prop 2>/dev/null" type="text" />
        <set>
            slog uri_adb_moduls "$uri_adb_moduls"
            slog uri_file_modun "$uri_file_modun"
            [ -f '$path_modun'/remove ] && rm -fr '$path_modun'/remove
            mkdir -p '$path_modun' '$path_modun2'
            echo "$prop_modunls" > '$path_modun2'/system.prop
            for vcc in $uri_file_modun; do
                if [ "$uri_adb_moduls" ]; then
                    mkdir -p "'$path_modun2'${uri_adb_moduls%/*}"
                    cp -rf "$PTAD/$vcc" "'$path_modun2'$uri_adb_moduls"
                    echo "'$lang_save_at''$path_modun2'$uri_adb_moduls"
                    echo
                else
                    echo "'$lang_searching'"
                    echo
                    link_find_file="$(find -L /system -name "$vcc" -type f -print -quit)"
                    if [ "$link_find_file" ]; then
                        mkdir -p "'$path_modun2'${link_find_file%/*}"
                        cp -rf "$PTAD/$vcc" "'$path_modun2'$link_find_file"
                        echo "'$lang_save_at''$path_modun2'$link_find_file"
                        echo
                    else
                        echo "'$lang_not_found'\$vcc'$lang_input_notice'"
                    fi
                fi
            done
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
</group>

<group>
    <action>
        <title>'$lang_del_tile'</title>
        <desc>'$lang_del_desc'</desc>
        <param name="del_file_modun" desc="'$lang_del_desc2' '$path_modun2'" value-sh="glog del_file_modun" label="@string/options_text" options-sh="[ -d '$path_modun2' ] && find '$path_modun2'/system -type f -printf '"'%p|%f\n'"'" required="required" multiple="true"/>
        <set>
            for vcx in $del_file_modun; do
                echo "Delete file: $vcx"
                [ -f "$vcx" ] && rm -fr "$vcx"
            done
        </set>
    </action>
</group>

<text desc="'$list_modul'" summary-sh="find '$path_modun' -type f" />'
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
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'

if [ "$(type -t "$1")" = "function" ]; then
    "$@"
else
    cat "$ETC/error.xml"
fi

echo '</group>'