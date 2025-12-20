#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

MPAT="${0%/*}"

# Ngôn ngữ mặc định
source $MPAT/addon.prop

# Dịch tự động
if [ "$(glog auto_trans_text)" == 1 ];then
rom_sum_md5_small="$(sha256sum -b $MPAT/addon.prop)"
if [[ "$rom_sum_md5_small" != "$(glog rom_sum_md5_small)" ]] || [[ ! -f $MPAT/auto.bash ]] || [[ "$(grep -cm1 '=\"\"' $MPAT/auto.bash)" == 1 ]];then
[ -f $MPAT/auto.bash ] && rm -fr $MPAT/auto.bash
for vc in $(grep "_text_.*.=" $MPAT/addon.prop | cut -d= -f1); do
echo "${vc}=\"$(echo "${!vc}" | trans $LANGUAGE-$COUNTRY | awk '{ $0 = toupper(substr($0,1,1)) substr($0,2); print }')\" #${!vc}" >>$MPAT/auto.bash &
done
    wait
    if [[ "$(grep -cm1 '=\"\"' $MPAT/auto.bash)" == 1 ]];then
    sed -i '/=\"\"/d' $MPAT/auto.bash
    else
    chmod 755 $MPAT/auto.bash
    slog rom_sum_md5_small "$rom_sum_md5_small"
    fi
fi
[[ -f $MPAT/auto.bash ]] && source $MPAT/auto.bash
fi

# index
echo '<?xml version="1.0" encoding="UTF-8" ?>
<page>

<group><action>
<title>Sign boot</title>
<desc>Sign AVB 1.0 boot, vendor_boot</desc>
<param name="NAME" label="'$sign_text_1'" value-sh="glog name_boot_key boot" type="text" placeholder="boot"/>
<param name="SIGN" value-sh="glog sign_boot_key testkey" label="'$sign_text_2'" options-sh="cd $ETC/key; ls *.pem | sed '"'s|.x509.pem||'"' "/>
<param name="FILE" desc="'$input_text_1' boot.img, '$folder_text_1' '$PTSD'" options-sh="cd $PTSD; ls *.img | grep boot" label="'$select_text_1'" required="true"/>
<set>
slog name_boot_key "$NAME"
slog sign_boot_key "$SIGN"
mkdir -p $PTSD/out
cp -rf "$PTSD/$FILE" "$PTSD/out/$FILE"
magiskboot sign "$PTSD/out/$FILE" "/$NAME" "$ETC/key/$SIGN.x509.pem" "$ETC/key/$SIGN.pk8" &>/dev/null
magiskboot verify "$PTSD/out/$FILE" "$ETC/key/$SIGN.x509.pem" 2>&1 || abort "failed to sign"
echo
echo "'$save_text_1': $PTSD/out/$FILE"
</set>
</action></group>

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
