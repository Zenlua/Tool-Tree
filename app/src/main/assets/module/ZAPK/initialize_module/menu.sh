# Kakathic

MPAT="${0%/*}"
if [ "$1" == "code_option" ];then
echo '<option type="default" id="vip123" reload="true" >@string/remove_text</option>'
elif [ "$1" == "code_shell" ];then
echo 'if [ "$menu_id" == "vip123" ]; then
touch /data/adb/modules/Tool-Tree/remove
echo "The module will be removed after the device restarts."
fi'
fi
