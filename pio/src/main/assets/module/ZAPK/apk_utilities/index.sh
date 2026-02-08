#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# clean
clean(){
if [ -n "$(glog project_apk_clean)" ];then
show_clean=1
path_clean="$(glog project_apk_clean)"
fi
xml_print '
<group>
<action shell="hidden" reload="true">
<title>'$clean_text_1'</title>
<summary>'$clean_text_2' '"$path_clean"'</summary>
<param name="project" label="'$select_text_1'" options-sh="cd $APK/$PTAH; ls -1d */apktool.yml */archive-info.json | sed -e '"'s|/apktool.yml||'"' -e '"'s|/archive-info.json||'"' " value-sh="glog project_apk_clean" required="true" />
<set>slog project_apk_clean "$project"</set>
</action>
</group>

<group>
<action reload="true" visible="echo '$show_clean'">
<title>'$clean_text_3'</title>
<param name="LIST" desc="'$clean_text_4'" multiple="multiple" options-sh="cd $APK/$PTAH/'$path_clean'; ls -1d resources/package_1/res/values-*/strings.xml res/values-*/strings.xml | sed -e '"'s|resources/package_1/||'"' -e '"'s|res/||'"' -e '"'s|/strings.xml||'"' "/>
<set>
for vv in $LIST; do
echo "'$clean_text_5' $vv"
if [ -d "$APK/$PTAH/'$path_clean'/resources/package_1/res" ];then
rm -fr "$APK/$PTAH/'$path_clean'/resources/package_1/res/$vv"
elif [ -d "$APK/$PTAH/'$path_clean'/res" ];then
rm -fr "$APK/$PTAH/'$path_clean'/res/$vv"
fi
done
</set>
</action>
</group>'
}

# home
home(){
xml_print '<group title="'$trans_text'">
<page title="'$home_text_1'" config-sh="'$MPAT'/index.sh clean"/>
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
