#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

MPAT="${0%/*}"

# Dịch tự động
source $MPAT/addon.prop
sum_md5_small="$(sha256sum -b $MPAT/addon.prop)"
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

<group><action>
<title>Protoc</title>
<desc>'$protoc_text_1'</desc>
<param name="LIST" label="'$select_text_1'" options-sh="echo -e '"'Xml\nJson'"' "/>
<param name="FILE" desc="'$input_text_1' .pb .json .xml, '$folder_text_1' '$PTSD'" multiple="true" options-sh="cd $PTSD; ls *.pb *.json *.xml" required="true"/>
<set>
for vvc in $FILE; do
if [ $(file $PTSD/$vvc | grep -cm1 "data") == 1 ];then
    if [ "$LIST" == "Xml" ];then
    protoc_pb.py --xml -d "$PTSD/$vvc" > "$PTSD/${vvc%.*}.xml"
    echo "'$save_text_1': $PTSD/${vvc%.*}.xml"
    else
    protoc_pb.py -d "$PTSD/$vvc" > "$PTSD/${vvc%.*}.json"
    echo "'$save_text_1': $PTSD/${vvc%.*}.json"
    fi
elif [ $(file $PTSD/$vvc | grep -cm1 "text") == 1 ];then
    if [ "${vvc##*.}" == "xml" ];then
    protoc_pb.py --xml -e "$PTSD/$vvc" -o "$PTSD/${vvc%.*}_new.pb"
    else
    protoc_pb.py -e "$PTSD/$vvc" -o "$PTSD/${vvc%.*}_new.pb"
    fi
    [ -f "$PTSD/${vvc%.*}_new.pb" ] && echo "'$save_text_1': $PTSD/${vvc%.*}_new.pb"
else
echo "'$error_text_1' $vvc" >&2
fi
done
</set>
</action></group>

<group><action>
<title>Mi Thermal</title>
<desc>'$mi_thermal_text_1'</desc>
<param name="FILE" desc="'$input_text_1' .conf, .txt, '$folder_text_1' '$PTSD'" multiple="true" options-sh="cd $PTSD; ls *.conf *.txt" required="true"/>
<set>
    for vvc in $FILE; do
    if [ $(file $PTSD/$vvc | grep -cm1 "data") == 1 ];then
    thermal-crypt.py -i "$PTSD/$vvc" -o "$PTSD/${vvc%.*}.txt"
    elif [ $(file $PTSD/$vvc | grep -cm1 "text") == 1 ];then
    thermal-crypt.py -e -i "$PTSD/$vvc" -o "$PTSD/${vvc%.*}_new.conf"
    else
    echo "'$error_text_1' $vvc" >&2
    fi
    done
</set>
</action></group>

</page>' | sed -z -e 's|\&|\&amp;|g' -e 's|§|\&#xA;|g'
