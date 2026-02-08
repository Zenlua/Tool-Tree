#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

#current directory
MPAT="${0%/*}"
if [ -n "$(glog url_text_payload)" ];then
checkdjhrh=1
    if [ "$(glog url_text_payload | checksum)" != "$(cat $MPAT/pmd)" ];then
    payload_dumper --out $TMP --list "$(glog url_text_payload)" | sed "1,/-----/d" | awk '{print $1"|"$1" ("$2$3")"}' > $MPAT/list
    glog url_text_payload | checksum > $MPAT/pmd
    fi
else
checkdjhrh=0
fi

# Dịch tự động
source $MPAT/addon.prop
sum_md5_small="$(checksum $MPAT/addon.prop)"
if [[ "$sum_md5_small" != "$(cat $MPAT/md5)" ]] || [[ ! -f $MPAT/auto.sh ]] || [[ "$(grep -cm1 '=\"\"' $MPAT/auto.sh)" == 1 ]];then
[ -f $MPAT/auto.sh ] && rm -fr $MPAT/auto.sh
for vc in $(grep "_text_.*.=" $MPAT/addon.prop | cut -d= -f1); do
echo "${vc}=\"$(echo "${!vc}" | trans $LANGUAGE-$COUNTRY | awk '{ $0 = toupper(substr($0,1,1)) substr($0,2); print }')\" #${!vc}" >>$MPAT/auto.sh &
done
    wait
    if [[ "$(grep -cm1 '=\"\"' $MPAT/auto.sh)" == 1 ]];then
    sed -i '/=\"\"/d' $MPAT/auto.sh
    else
    chmod 755 $MPAT/auto.sh
    echo -n "$sum_md5_small" > $MPAT/md5
    fi
fi
[[ -f $MPAT/auto.sh ]] && source $MPAT/auto.sh

# index
echo '<?xml version="1.0" encoding="UTF-8" ?>
<page>

<group>
<action shell="hidden" reload="true">
<title>'$payload_text_1'</title>
<summary>'$payload_text_2' '"$(glog url_text_payload)"'</summary>
<param name="url_text_payload" value-sh="glog url_text_payload" type="text" required="required" />
<set>slog url_text_payload "$url_text_payload"</set>
</action>
</group>

<group>
<action reload="true" visible="echo '$checkdjhrh'">
<title>'$payload_text_3'</title>
<summary>'$payload_text_4' '$PTSD'</summary>
<param name="partition" desc="'$payload_text_5'" multiple="multiple" separator="," options-sh="cat '$MPAT'/list"/>
<set>
echo "Downloading..." | trans -b $LANGUAGE-$COUNTRY
echo
payload_dumper --out "$PTSD" -i "$partition" "$(glog url_text_payload)" | trans -b $LANGUAGE-$COUNTRY
</set>
</action>
</group>

</page>' | sed -z -e 's|\&|\&amp;|g' -e 's|§|\&#xA;|g'
