#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home(){
xml_print '<group title="'$google_text'">

<action title="'$home_text_1'" desc="'$home_text_2'">
<param desc="'$home_text_7'" name="overlay_folder" type="folder" value-sh="glog overlay_folder" required="true" editable="true"/>
<set>
slog overlay_folder "$overlay_folder"
'$MPAT'/overlay.sh
</set>
</action>

<action title="'$home_text_3'" desc="'$home_text_4'">
<param desc="'$home_text_8'" name="extract_folder_lang" type="folder" value-sh="glog extract_folder_lang" required="true" editable="true"/>
<param name="extract_folder_lang_text" label="'$home_text_5'" desc="'$home_text_6'" placeholder="values-vi,values-zh-rCN" type="text" value-sh="glog extract_folder_lang_text"/>
<set>
slog extract_folder_lang "$extract_folder_lang"
slog extract_folder_lang_text "$extract_folder_lang_text"
'$MPAT'/extract.sh
</set>
</action>

</group>'
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Google dịch
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
[ "$(glog "auto_trans_text_${1##*/}")" == 1 ] && trans_add "$MPAT"
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"

# index
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
case "$1" in
    home|clean)
        "$1"
        ;;
    *)
        cat "$ETC/error.xml"
        ;;
esac
echo '</group>'
