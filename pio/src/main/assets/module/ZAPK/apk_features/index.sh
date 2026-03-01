#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home(){
[ "$ROT" == 0 ] && text_root="ROOT" || text_rr="$fs_text_1"
xml_print '<group title="'$google_text'">
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
