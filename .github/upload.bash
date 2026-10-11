# Kakathic

mkdir -p Up Add
curl -sSLf "https://github.com/Kakathic/Tool-Tree/releases/download/V1/UZE.zip" -o "UZE.zip"
curl -sSLf "https://github.com/Kakathic/Tool-Tree/releases/download/V1/addon.log" -o "addon.log"

unzip -o UZE.zip -d Up
getp(){ grep -m1 "$1=" | cut -d= -f2 | sed 's|"||g'; }
list_file="$(curl -sS -u ":$UP_TOKEN" https://pixeldrain.com/api/user/files)"

# Xử lý file add-on
if echo "$list_file" | grep -q "Addon_"; then
  list_addonl="$(echo "$list_file" | jq --arg kw "Addon_" -r '.files[] | select(.name | startswith($kw)) | .id // empty')"
  for vc in $list_addonl; do
    file_name="$(echo "$list_file" | jq --arg kw "$vc" -r '.files[] | select(.id | startswith($kw)) | .name // empty')"
    file_size="$(echo "$list_file" | jq --arg kw "$vc" -r '.files[] | select(.id | startswith($kw)) | .size // empty')"
    if [[ "$file_size" -lt 10485760 ]]; then
      curl -sSLf -u ":$UP_TOKEN" -o "$file_name" "https://pixeldrain.com/api/file/$vc"
      eval "$(7z x "$file_name" Add-on.bash -so 2>/dev/null)"
      download='id='$id'\nauthor="'$author'"\nname="'$name'"\ndescription="'$description'"\nversion="'$version'"\nversionCode='$versionCode'\nroot='$root'\nshum256="'$shum256'"\nurl=https://github.com/Kakathic/Tool-Tree/releases/download/V1.0.2/${id}_$versionCode.7z'
      if [ -d Up/$id ]; then
        if [[ "$author" == "$(cat Up/$id/download.bash | getp author)" ]] && [[ "$shum256" == "$(cat Up/$id/download.bash | getp shum256)" ]]; then
        mv "$file_name" "Add/${id}_$versionCode.7z"
        echo -e "$download" > Up/$id/download.bash
        echo "$(date +'%Y-%m-%d_%H:%M:%S_%z') $id: Update detected: approved" | tee -a addon.log
        else
        echo "$(date +'%Y-%m-%d_%H:%M:%S_%z') $id: Mismatch in author name or shum: not approved." | tee -a addon.log
        fi
      else
        mv "$file_name" "Add/${id}_$versionCode.7z"
        mkdir -p Up/$id
        echo -e "$download" > Up/$id/download.bash
        echo "$(date +'%Y-%m-%d_%H:%M:%S_%z') $id: First upload: approved" | tee -a addon.log
      fi
    else
      echo "$(date +'%Y-%m-%d_%H:%M:%S_%z') $id: Ignored because the file was larger than 10MB" | tee -a addon.log
    fi
    # Xoá add-on
    curl -sS -X DELETE -u ":$UP_TOKEN" "https://pixeldrain.com/api/file/$vc"
    file_size= file_name=
  done
fi

# unset
unset vc list_addonl

# Xử lý xoá add-on
if echo "$list_file" | grep -q "Delete_"; then
  list_addonl="$(echo "$list_file" | jq --arg kw "Delete_" -r '.files[] | select(.name | startswith($kw)) | .id // empty')"
  for vc in $list_addonl; do
    file_name="$(echo "$list_file" | jq --arg kw "$vc" -r '.files[] | select(.id | startswith($kw)) | .name // empty')"
    curl -sSLf -u ":$UP_TOKEN" -o "$file_name" "https://pixeldrain.com/api/file/$vc"
    shum256ss="$(cat $file_name | getp shum256)"
    idss="$(cat $file_name | getp id)"
    name_add_onl="$(curl -sSGL "https://api.github.com/repos/Kakathic/Tool-Tree/releases/tags/V1.0.2" | jq -r --arg name "$idss" '.assets[]? | select(.name | startswith($name)) | .name // empty')"
    curl -sSLf "https://github.com/Kakathic/Tool-Tree/releases/download/V1.0.2/$name_add_onl" -o "del_$name_add_onl"
    eval "$(7z x "del_$name_add_onl" Add-on.bash -so 2>/dev/null)"
    if [[ "$shum256" == "$shum256ss" ]]; then
      echo "$(date +'%Y-%m-%d_%H:%M:%S_%z') Add-on has been removed: $idss" | tee -a addon.log
      rm -fr "Up/$idss"
      gh release delete-asset V1.0.2 "$name_add_onl"
      else
      echo "$(date +'%Y-%m-%d_%H:%M:%S_%z') Mismatch shum: not approved" | tee -a addon.log
    fi
    # Xoá add-on
    curl -sS -X DELETE -u ":$UP_TOKEN" "https://pixeldrain.com/api/file/$vc"
    file_name= name_add_onl= idss= shum256ss=
  done
fi

(
cd Up
zip -r UZE.zip *
)
