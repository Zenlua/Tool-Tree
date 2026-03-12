# Kakathic
source language
MPAT="${0%/*}"
if [ "$1" == "code_option" ];then
echo '<option type="default" id="vip1" auto-off="true" reload="true" interruptible="false">'$update_text' Kaorios Toolbox</option>'
elif [ "$1" == "code_shell" ];then
echo '
if [ "$menu_id" == "vip1" ];then
    if checkonline; then
    linkurrl="$(xem https://api.github.com/repos/Wuang26/Kaorios-Toolbox/releases/latest 2>/dev/null)"
    echo "$(echo "$linkurrl" | jq -r ".tag_name")" > '$MPAT'/mod/version
    downloadb "$(echo "$linkurrl" | jq -r ".assets[].browser_download_url" | grep "KaoriosToolbox.*\.apk")" "'$MPAT'/mod/KaoriosToolbox.apk" 2>&1
    downloadb "$(echo "$linkurrl" | jq -r ".assets[].browser_download_url" | grep "com.kousei.kaorios.xml")" "'$MPAT'/mod/com.kousei.kaorios.xml" 2>&1
    downloadb "$(echo "$linkurrl" | jq -r ".assets[].browser_download_url" | grep "classes.*\.dex")" "'$MPAT'/mod/classes.dex" 2>&1
    fi
fi'
fi
