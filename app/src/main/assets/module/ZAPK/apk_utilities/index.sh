#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

search_pro(){
if [ -e "$APK/$PTAH" ];then
cd "$APK/$PTAH"
ls -1d */apktool.yml */archive-info.json 2>/dev/null | sed -e 's|/apktool.yml||' -e 's|/archive-info.json||'
fi
}

search_values(){
path_clean="$(glog project_apk_clean)"
if [ -e "$APK/$PTAH/$path_clean" ];then
cd "$APK/$PTAH/$path_clean"
ls -1d resources/package_1/res/values-*/strings.xml res/values-*/strings.xml 2>/dev/null | sed -e 's|resources/package_1/||' -e 's|res/||' -e 's|/strings.xml||'
fi
}

# clean
clean(){
if [ -n "$(glog project_apk_clean)" ];then
show_clean=1
path_clean="$(glog project_apk_clean)"
fi
xml_print '<?xml version="1.0" encoding="UTF-8" ?>
<group>
<group>
<action shell="hidden" reload="true">
<title>'$clean_text_1'</title>
<summary>'$clean_text_2' '"$path_clean"'</summary>
<param name="project" label="'$select_text_1'" options-sh="'$MPAT'/index.sh search_pro" value-sh="glog project_apk_clean" required="true" />
<set>slog project_apk_clean "$project"</set>
</action>
</group>

<group>
<action reload="true" visible="echo '$show_clean'">
<title>'$clean_text_3'</title>
<param name="LIST" desc="'$clean_text_4'" multiple="multiple" options-sh="'$MPAT'/index.sh search_values"/>
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
</group>
</group>'
}

# home
home(){
xml_print '<?xml version="1.0" encoding="UTF-8" ?>
<group>
<group title="'$google_text'">
<page title="'$home_text_1'" config-sh="'$MPAT'/index.sh clean"/>
</group>
</group>'
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
trans_add "$MPAT"
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

# index
if [ "$(type -t "$1")" = "function" ];then
"$@"
else
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
cat "$ETC/error.xml"
echo '</group>'
fi
