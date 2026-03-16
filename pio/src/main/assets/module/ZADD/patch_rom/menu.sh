# Kakathic
source language
MPAT="${0%/*}"
if [ "$1" == "code_option" ];then
echo '<option type="default" id="vip1" auto-off="true" reload="true" interruptible="false">'$update_text' Kaorios Toolbox</option>
option type="default" id="vip2" auto-off="true" reload="true" interruptible="false">'$update_text' Add-on</option>'
elif [ "$1" == "code_shell" ];then
echo '
if [ "$menu_id" == "vip1" ];then
'$MPAT'/index.sh toolsbox
elif [ "$menu_id" == "vip2" ];then
'$MPAT'/index.sh update
fi
'
fi
