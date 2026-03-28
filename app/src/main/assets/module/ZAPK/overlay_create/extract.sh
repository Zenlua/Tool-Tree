# kakathic
MPAT="${0%/*}"
if [ -f $MPAT/auto.prop ] && [ "$(glog auto_trans_text_${MPAT##*/})" == 1 ];then
source $MPAT/auto.prop
else
eval "$(grep "=\"" $MPAT/addon.prop)"
fi

[ -d "$extract_folder_lang" ] || killtree "$overlay_text_1"
[ "$extract_folder_lang_text" ] && text_filters="$(echo "$extract_folder_lang_text" | tr ',' ' ')"

echo "$overlay_text_4"
echo

for vv in "$extract_folder_lang"/*.apk; do
if [ -f "$vv" ];then
demso=$((demso + 1))
if [ -f "$extract_folder_lang/1out/${vv##*/}" ];then
echo "$demso: ${vv##*/}: $overlay_text_6"
continue
fi
if [ ! -d "${vv%.*}" ];then
mkdir -p "$extract_folder_lang/1out"
apktool d -f -s "$vv" -o "$TMP/${vv##*/}" &>"$extract_folder_lang/1out/1build.log" || killtree "$overlay_text_10 $extract_folder_lang/1out/1build.log"
    if [ -z "$text_filters" ];then
    text_filter="$(cd "$TMP/${vv##*/}"/res; find values-* -type f \( -name "plurals.xml" -o -name "arrays.xml" -o -name "strings.xml" \))"
    else
    text_filter="$(cd "$TMP/${vv##*/}"/res; find $text_filters -type f \( -name "plurals.xml" -o -name "arrays.xml" -o -name "strings.xml" \))"
    fi
    for vc in $text_filter; do
    mkdir -p "${vv%.*}/res/${vc%/*}"
    cp -rf "$TMP/${vv##*/}/res/$vc" "${vv%.*}/res/${vc%/*}"
    done
# lọc package
calssname="$(grep "package=" "$TMP/${vv##*/}/AndroidManifest.xml" | tr ' ' '\n' | grep -m1 "package=" | cut -d'"' -f2)"
rm -fr "$TMP/${vv##*/}"
file_manifest='<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="z.'$calssname'">
    <overlay
        android:targetPackage="'$calssname'"
        android:targetName="z.'${vv##*/}'"
        android:isStatic="true"
        android:priority="1000"/>
    <uses-sdk
        android:minSdkVersion="24"
        android:targetSdkVersion="'$API'" />
    <application
        android:label="z.'${vv##*/}'"
        android:hasCode="false"
        android:extractNativeLibs="false"/>
</manifest>'
file_apktool="version: 3.0.0
apkFileName: ${vv##*/}
usesFramework:
  ids:
  - 1
sdkInfo:
  minSdkVersion: 24
  targetSdkVersion: $API
versionInfo:
  versionCode: $API
  versionName: $ANDROID_RELEASE
resourcesInfo:
  packageId: 127
doNotCompress:
- arsc"
[ -f "${vv%.*}/apktool.yml" ] || echo "$file_apktool" > "${vv%.*}/apktool.yml"
[ -f "${vv%.*}/AndroidManifest.xml" ] || echo "$file_manifest" > "${vv%.*}/AndroidManifest.xml"
mkdir -p "${vv%.*}/res/values"
[ -f "${vv%.*}/res/values/public.xml" ] || echo '<?xml version="1.0" encoding="utf-8"?>
<resources>
  <public type="array" name="test" id="0x7f010000" />
  <public type="plurals" name="test2" id="0x7f020000" />
  <public type="string" name="test3" id="0x7f030000" />
  <public type="bool" name="test4" id="0x7f040000" />
  <public type="integer" name="test5" id="0x7f050000" />
  <public type="dimen" name="test6" id="0x7f060000" />
</resources>' > "${vv%.*}/res/values/public.xml"
fi
echo "$demso: ${vv##*/}: $overlay_text_5"
for vc in $(find ${vv%.*}/res -type f | sed "/\/values\//d"); do
if [ "$(echo "$vc" | grep -c -e "values\-" -e "\.xml")" -ge 1 ];then
filter_xml.py "$vc" &>"$extract_folder_lang/1out/1filterxml.log"
update_id.py --use-file "${vv%.*}/res/values/public.xml" "$vc" &>>"$extract_folder_lang/1out/1filterxml.log"
fi
done
sed -i "/name=\"test/d" "${vv%.*}/res/values/public.xml"
find "${vv%.*}" -type f -name "*.bak" -delete >/dev/null
apktool b -f -o "$TMP/${vv##*/}" "${vv%.*}" &>"$extract_folder_lang/1out/1build.log" || killtree "\n$overlay_text_8 $extract_folder_lang/1out/1build.log" "$TMP/${vv##*/}"
sign -i "$TMP/${vv##*/}" -o "$extract_folder_lang/1out/${vv##*/}"
rm -fr "$TMP/${vv##*/}" "${vv%.*}"
fi
done
echo
echo "$overlay_text_9 $extract_folder_lang/1out"
echo
checktime
