# Kakathic

MPAT="${0%/*}"
if [ "$1" == "code_option" ];then
echo '<option type="refresh">@string/refresh_text</option>
<option type="default" auto-finish="true" id="123">@string/update_text add-on</option>'
elif [ "$1" == "code_shell" ];then
echo '[ "$menu_id" == "123" ] && '$MPAT'/index.bash update_addon'
fi
