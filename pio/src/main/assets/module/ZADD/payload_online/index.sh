#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

home(){
# index
echo '
<group>
<action shell="hidden" reload="true">
<title>'$payload_text_1'</title>
<summary>'$payload_text_2' '"http://...$(glog url_text_payload | tail -c 25)"'</summary>
<param name="url_text_payload" placeholder="https://web.com/rom-payload-ota.zip" value-sh="glog url_text_payload" type="text" required="required" />
<set>slog url_text_payload "$url_text_payload"</set>
</action>
</group>

<group>
<action visible="echo '$checkdjhrh'" reload="true">
<title>'$payload_text_3'</title>
<summary>'$payload_text_4' '$PTSD'</summary>
<param name="partition" desc="'$payload_text_5'" multiple="multiple" options-sh="cat '$MPAT'/list_payload" required="required"/>
<set>
echo "Downloading..." | trans -b $LANGUAGE-$COUNTRY
echo
for vv in $partition; do
'$MPAT'/payload.sh $vv
done
echo
echo "'$payload_text_4' $PTSD"
echo
checktime
</set>
</action>
</group>' | sed -z -e 's|\&|\&amp;|g' -e 's|§|\&#xA;|g'
}

# Thư mục hiện tại
MPAT="${0%/*}"
if [ -n "$(glog url_text_payload)" ];then
checkdjhrh=1
    if [ "$(glog url_text_payload | checksum)" != "$(glog url_text_payload_md5)" ];then
    listpayload "$(glog url_text_payload)" | awk '{print $1"|"$1" "$2}' > $MPAT/list_payload
    slog url_text_payload_md5 "$(glog url_text_payload | checksum)"
    fi
else
checkdjhrh=0
fi

# Ngôn ngữ mặc định
eval "$(sed '1,/root=/d' $MPAT/addon.prop)"

# Dịch tự động
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
sum_md5="$(sha256sum $MPAT/addon.prop | awk '{print $1}')"
if [[ "$sum_md5" != "$(glog "sum_md5_${MPAT##*/}")" ]] || [[ ! -f $MPAT/auto.sh ]] || [[ "$(grep -cm1 '=\"\"' $MPAT/auto.sh)" == 1 ]];then
[ -f $MPAT/auto.sh ] && rm -fr $MPAT/auto.sh
for vc in $(grep -e '="' $MPAT/addon.prop | cut -d= -f1); do
echo -e "${vc}=\"$(echo "${!vc}" | trans $LANGUAGE-$COUNTRY | awk '{ $0 = toupper(substr($0,1,1)) substr($0,2); print }')\" #${!vc}" >>$MPAT/auto.sh &
done
    wait
    if [[ "$(grep -cm1 '=\"\"' $MPAT/auto.sh)" == 1 ]];then
    sed -i '/=\"\"/d' $MPAT/auto.sh
    else
    slog "sum_md5_${MPAT##*/}" "$sum_md5"
    fi
fi
[[ -f $MPAT/auto.sh ]] && source $MPAT/auto.sh
trans_text="$google_text"
fi

# index
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
case "$1" in
    home)
        "$1"
        ;;
    *)
        cat "$ETC/error.xml"
        ;;
esac
echo '</group>'
