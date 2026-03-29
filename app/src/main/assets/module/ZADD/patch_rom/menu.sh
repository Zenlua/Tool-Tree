# Kakathic
source language
MPAT="${0%/*}"
if [ "$1" == "code_option" ];then
echo '
<option type="default" id="vip2" auto-off="true" reload="true" interruptible="false">'$update_text'</option>
'
elif [ "$1" == "code_shell" ];then
echo '
[ "$menu_id" == "vip2" ] && '$MPAT'/index.sh update
'
fi
