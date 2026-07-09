# Kakathic

MPAT="${0%/*}"
path_modun="/data/adb/modules/Tool-Tree"
path_modun2="/data/adb/modules_update/Tool-Tree"

if [ "$1" == "code_option" ];then
echo '<option type="default" id="vip123" reload="true">@string/remove_text Module</option>'
elif [ "$1" == "code_shell" ];then
echo 'if [ "$menu_id" == "vip123" ]; then
[ -d '$path_modun' ] && touch '$path_modun'/remove
[ -d '$path_modun2' ] && rm -fr '$path_modun2'
echo "The module will be removed after the device restarts."
fi'
fi
