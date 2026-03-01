#!/data/data/com.tool.tree/files/home/bin/bash
# kakathic

# home
home(){ xml_print '<group title="'$google_text'">
<action>
<title>Upload Gofile</title>
<param name="FILE" type="file" required="true"/>
<set>
set -o pipefail
if [ -f "$FILE" ];then
urls="$(xem https://api.gofile.io/servers | jq -r .data.servers[0].name)"
[ -z "$urls" ] && urls="upload-ap-sgp.gofile.io" || urls="$urls.gofile.io"
echo "'$gofile_text_1' $urls"
echo
curl -L -H "$WEBS" -F "file=@$FILE" "https://$urls/contents/uploadfile" | jq | tee "$TMP/Upload.log" || killtree "'$gofile_text_2'"
echo
echo "'$gofile_text_3' $(jq -r .data.downloadPage "$TMP/Upload.log")"
echo
echo "'$gofile_text_4' $TMP/Upload.log"
echo
checktime
else
echo "'$gofile_text_5'"
fi
</set>
</action>

<action>
<title>Upload Pixeldrain</title>
<param name="FILE" type="file" required="true"/>
<param name="TEXT" label="Token" desc="Token: xxx-xxx-xxx-xxx-xxx" value-sh="glog tocken_key_upload_free" type="text"/>
<set>
set -o pipefail
[ "$TEXT" ] || echo "'$gofile_text_6'"
slog tocken_key_upload_free "$TEXT"
if [ -f "$FILE" ];then
echo "'$gofile_text_1' pixeldrain.com"
echo
curl -T "$FILE" -u ":$TEXT" https://pixeldrain.com/api/file/ | jq -r .id | awk '"'{print \"https://pixeldrain.com/u/\"\$1}'"' | tee "$TMP/Upload.log" || killtree "'$gofile_text_2'"
echo
echo "'$gofile_text_3' $(cat "$TMP/Upload.log")"
echo
echo "'$gofile_text_4' $TMP/Upload.log"
echo
checktime
else
echo "'$gofile_text_5'"
fi
</set>
</action>
</group>'
}

# Thư mục hiện tại
MPAT="${0%/*}"

# Google dịch
eval "$(grep '="' "$MPAT/addon.prop" | sed "/google_text=/d")"
[ "$(glog "auto_trans_text_${1##*/}")" == 1 ] && trans_add "$MPAT"
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"

# index
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
case "$1" in
    home)
        "$1"
        ;;
    *)
        cat "$ETC/error.xml"
        ;;
esac
echo '</group>'
