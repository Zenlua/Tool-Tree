#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home(){ xml_print '
<group title="'$trans_text'"><action warn="'$oat_text_1'§§'$oat_text_2' '$PTSH'">
<title>'$oat_text_3'</title>
<desc>'$oat_text_4'</desc>
<param name="framework_switch" value-sh="glog framework_switch 1" label="'$oat_text_5'" type="switch" />
<param name="services_switch" value-sh="glog services_switch 1" label="'$oat_text_6'" type="switch" />
<param name="features_oat" value-sh="glog features_oat default" label="'$oat_text_7'" placeholder="default" desc=" " type="text"/>
<param name="apps_apk_oat" label="'$oat_text_8'" value-sh="glog apps_apk_oat none" options-sh="'$MPAT'/bin/listapk" type="text"/>
<param name="secontex" desc="'$oat_text_9'" value-sh="glog secontex" placeholder="PCL[]" type="text"/>
<set>
slog features_oat "$features_oat"
slog apps_apk_oat "$apps_apk_oat"
slog secontex "$secontex"
slog services_switch "$services_switch"
slog framework_switch "$framework_switch"
'$MPAT'/bin/dex2oat
</set>
</action></group>

<group><action>
<title>Sign boot</title>
<desc>Sign AVB 1.0 boot, vendor_boot</desc>
<param name="NAME" label="'$name_text'" value-sh="glog name_boot_key boot" type="text" placeholder="boot"/>
<param name="SIGN" value-sh="glog sign_boot_key testkey" label="'$sign_text'" options-sh="cd $ETC/key; ls *.pem | sed '"'s|.x509.pem||'"' "/>
<param name="FILE" desc="'$input_text' .img, '$folder_text' '$PTSD'" options-sh="cd $PTSD; ls *.img | grep boot" label="'$select_text'" required="true"/>
<set>
slog name_boot_key "$NAME"
slog sign_boot_key "$SIGN"
mkdir -p $PTSD/out
cp -rf "$PTSD/$FILE" "$PTSD/out/$FILE"
magiskboot sign "$PTSD/out/$FILE" "/$NAME" "$ETC/key/$SIGN.x509.pem" "$ETC/key/$SIGN.pk8" &>/dev/null
magiskboot verify "$PTSD/out/$FILE" "$ETC/key/$SIGN.x509.pem" 2>&1 || abort "failed to sign"
echo
echo "'$save_text': $PTSD/out/$FILE"
</set>
</action></group>

<group><action>
<title>Protoc</title>
<desc>'$protoc_text'</desc>
<param name="LIST" label="'$select_text'" options-sh="echo -e '"'Xml\nJson'"' "/>
<param name="FILE" desc="'$input_text' .pb .json .xml, '$folder_text' '$PTSD'" multiple="true" options-sh="cd $PTSD; ls *.pb *.json *.xml" required="true"/>
<set>
for vvc in $FILE; do
if [ $(file $PTSD/$vvc | grep -cm1 "data") == 1 ];then
    if [ "$LIST" == "Xml" ];then
    protoc_pb.py --xml -d "$PTSD/$vvc" > "$PTSD/${vvc%.*}.xml"
    echo "'$save_text': $PTSD/${vvc%.*}.xml"
    else
    protoc_pb.py -d "$PTSD/$vvc" > "$PTSD/${vvc%.*}.json"
    echo "'$save_text': $PTSD/${vvc%.*}.json"
    fi
elif [ $(file $PTSD/$vvc | grep -cm1 "text") == 1 ];then
    if [ "${vvc##*.}" == "xml" ];then
    protoc_pb.py --xml -e "$PTSD/$vvc" -o "$PTSD/${vvc%.*}_new.pb"
    else
    protoc_pb.py -e "$PTSD/$vvc" -o "$PTSD/${vvc%.*}_new.pb"
    fi
    [ -f "$PTSD/${vvc%.*}_new.pb" ] && echo "'$save_text': $PTSD/${vvc%.*}_new.pb"
else
echo "'$error_text' $vvc" >&2
fi
done
</set>
</action></group>

<group><action>
<title>Mi Thermal</title>
<desc>'$mi_thermal_text'</desc>
<param name="FILE" desc="'$input_text' .conf, .txt, '$folder_text' '$PTSD'" multiple="true" options-sh="cd $PTSD; ls *.conf *.txt" required="true"/>
<set>
    for vvc in $FILE; do
    if [ $(file $PTSD/$vvc | grep -cm1 "data") == 1 ];then
    thermal-crypt.py -i "$PTSD/$vvc" -o "$PTSD/${vvc%.*}.txt"
    elif [ $(file $PTSD/$vvc | grep -cm1 "text") == 1 ];then
    thermal-crypt.py -e -i "$PTSD/$vvc" -o "$PTSD/${vvc%.*}_new.conf"
    else
    echo "'$error_text' $vvc" >&2
    fi
    done
</set>
</action></group>'
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Ngôn ngữ mặc định
eval "$(sed '1,/root=/d' $MPAT/addon.prop)"

# Dịch tự động
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
sum_md5="$(sha256sum -b $MPAT/addon.prop)"
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
    home)
        "$1"
        ;;
    *)
        cat "$ETC/error.xml"
        ;;
esac
echo '</group>'
