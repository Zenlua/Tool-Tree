source language 2>/dev/null
echo '
<option type="default" id="v1" auto-off="true" reload="true" interruptible="false" >'$google_translate_text'</option>
<handler>
if [ "$menu_id" == "v1" ];then
[ "$(glog auto_trans_text)" == 1 ] && slog auto_trans_text 0 || slog auto_trans_text 1
fi
</handler>
'
