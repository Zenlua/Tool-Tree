#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home(){
xml_print '<group title="'$trans_text'">

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

# Ngôn ngữ mặc định
eval "$(grep '="' $MPAT/addon.prop)"

# Tự động dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
sum_md5="$(sha256sum $MPAT/addon.prop | awk '{print $1}')"
if [[ "$sum_md5" != "$(glog "sum_md5_${MPAT##*/}")" ]] || [[ ! -f $MPAT/auto.prop ]] || [[ "$(grep -cm1 '=\"\"' $MPAT/auto.prop)" == 1 ]];then
[ -f $MPAT/auto.prop ] && rm -fr $MPAT/auto.prop
for vc in $(grep -e '="' $MPAT/addon.prop | cut -d= -f1); do
echo -e "${vc}=\"$(echo "${!vc}" | trans $LANGUAGE-$COUNTRY | awk '{ $0 = toupper(substr($0,1,1)) substr($0,2); print }')\" #${!vc}" >>$MPAT/auto.prop &
done
    wait
    if [[ "$(grep -cm1 '=\"\"' $MPAT/auto.prop)" == 1 ]];then
    sed -i '/=\"\"/d' $MPAT/auto.prop
    else
    slog "sum_md5_${MPAT##*/}" "$sum_md5"
    fi
fi
[[ -f $MPAT/auto.prop ]] && source $MPAT/auto.prop
trans_text="$google_text"
fi

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
