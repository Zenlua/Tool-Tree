#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home(){
[ "$ROT" == 0 ] && text_root="ROOT" || text_rr="$fs_text_1"
xml_print '<group>
<action title="'$check_ufs_text'" summary="'$text_root'">
<lock>
[ "$ROT" == 0 ] && echo "'$fs_text_3'" || echo 0
</lock>
<set>
export MPAT='$MPAT'
'$MPAT'/scrip/ufs.sh
</set>
</action>
</group>

<group>
<action title="'$fs_text_2'" desc="'$text_rr'" summary="'$text_root'">
<lock>
[ "$ROT" == 0 ] && echo "'$fs_text_3'" || echo 0
</lock>
<set>
    echo "'$fs_text_4' /data"
    fstrim /data;
    echo "'$fs_text_4' /cache"
    fstrim /cache
    echo "'$fs_text_4' auto"
    echo
    sm fstrim
    echo
    checktime
</set>
</action>
</group>

<group>
<page html="https://zenlua.github.io/Tool-Tree/add-on/web/terminal.html" title="Web Terminal" />
</group>

<group>
<page html="https://zenlua.github.io/Tool-Tree/add-on/web/manager.html" title="Web Manager" />
</group>'
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Ngôn ngữ mặc định
eval "$(sed '1,/root=/d' $MPAT/addon.prop)"

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
