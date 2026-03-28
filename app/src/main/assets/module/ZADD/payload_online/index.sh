#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

home(){
# index
echo '
<group title="'$google_text'">
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
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
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
