#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

urlpng() {
    if [ "$(glog Ticon)" != 1 ]; then
        [ -f "$ETC/icon/$1.png" ] && echo "$ETC/icon/$1.png" || echo "$ETC/icon/$1_$DARK_MODE.png"
    fi
}

urladd() {
    if [ "$(glog Ticon)" != 1 ]; then
        if [ -f "$dirvad/$1.png" ]; then
            echo "$dirvad/$1.png"
        elif [ -f "$dirvad/$1_$DARK_MODE.png" ]; then
            echo "$dirvad/$1_$DARK_MODE.png"
        else
            echo "$ETC/icon/icon.png"
        fi
    fi
}

show_sett() {
    echo '
<action icon="'`urlpng folder_rom`'" shell="hidden" reload="true">
<title>'$input_folder_text'</title>
<summary>'$path_text': '${PTSD/$SDCARD_PATH/\/sdcard}'</summary>
<param name="Name" desc="'$config_text_1'" label="'$setting_text_3'" option-sh="findfile for $SDH" value-sh="glog PTSH"/>
<param name="Folder" desc="'$config_text_2'" value-sh="glog PTSD" type="folder" editable="true" required="true"/>
<set>
if [ ! -d "$Folder" ] || [ ! -d "$SDH/${Folder##*/}" ]; then
    slog PTSD "$Folder"
    slog PTSH "${Folder##*/}"
    mkdir -p "$SDH/${Folder##*/}" "$Folder/out"
elif [ -d "$SDH/$Name" ]; then
    slog PTSH "$Name"
    slog PTSD "$SDC/$Name"
fi
</set>
</action>'
}

show_apkset() {
    echo '
<action icon="'`urlpng folder_apk`'" shell="hidden" reload="true">
<title>'$input_folder_text'</title>
<summary>'$path_text': '${PTAD/$SDCARD_PATH/\/sdcard}'</summary>
<param name="Name" desc="'$config_text_1'" label="'$setting_text_3'" option-sh="findfile for $APK" value-sh="glog PTAH"/>
<param name="Folder" desc="'$config_text_2'" value-sh="glog PTAD" type="folder" editable="true" required="true"/>
<set>
if [ ! -d "$Folder" ] || [ ! -d "$APK/${Folder##*/}" ]; then
    slog PTAD "$Folder"
    slog PTAH "${Folder##*/}"
    mkdir -p "$APK/${Folder##*/}" "$Folder/out"
elif [ -d "$APK/$Name" ]; then
    slog PTAH "$Name"
    slog PTAD "$SDC/$Name"
fi
</set>
</action>'
}

shell_bash() {
    echo '<group><editor title="'$home_text_5'" desc="'$more_text_9'" file="home/usr/run_'$1'.bash" placeholder="#!/data/data/com.tool.tree/files/home/bin/bash" icon="'`urlpng shell`'"/></group>'
}


# Tạo ngôn ngữ tự động
if [ "$(glog language_kkts)" == 'auto' ]; then
    [ -f $ETC/lang/$LANGUAGE.bash ] && texgg="$LANGUAGE.bash" || texgg=vi.bash
    sum_md5_kk="$(checksum $ETC/lang/$texgg)"
    if [[ "$sum_md5_kk" != "$(glog sum_md5_kk)" ]] || [[ "$(grep -cm1 '=""' $ETC/lang/auto.sh)" == 1 ]] || [[ ! -f $ETC/lang/auto.sh ]]; then
        source $ETC/lang/$texgg
        [ -f $ETC/lang/auto.sh ] && rm -fr $ETC/lang/auto.sh
        for vc in $(grep "=" $ETC/lang/$texgg | cut -d= -f1); do
            (
                xfhtfvgf="$(echo "${!vc}" | trans $LANGUAGE-$COUNTRY)"
                echo "${vc}=\"${xfhtfvgf^}\" # ${!vc}" >>$ETC/lang/auto.sh
                xfhtfvgf=""
            ) &
        done
        wait
        error_txxt_bug="$(echo "Translation error detected:" | trans -b $LANGUAGE-$COUNTRY)"
        if [ "$(grep -cm1 '=""' $ETC/lang/auto.sh)" == 1 ]; then
            showtoast "$error_txxt_bug $(grep -c '=""' $ETC/lang/auto.sh)"
            sed -i '/=""/d' $ETC/lang/auto.sh
        fi
        slog sum_md5_kk "$sum_md5_kk"
    fi
    error_txxt_bug=''
    texgg=''
    sum_md5_kk=''
fi

# Tạo thư mục
(
    [ -d $PTAD/out ] && mkdir -p $PTAD/out &>/dev/null
    [ -d $PTSD/out ] && mkdir -p $PTSD/out &>/dev/null
) &

# Ngôn ngữ
source language 2>/dev/null

# Thông tin
text_id_1="\"$(glog show_infor_text_1 'ROOT: $ROOT  |  Android: $ANDROID_RELEASE  -  SDK: $API  |  CPU: $CPU_ABI')\""
text_id_2="\"$(glog show_infor_text_2 '$trademark_text: $ANDROID_BRAND  |  $device_text: $ANDROID_DEVICE  |  $version_text: $PACKAGE_VERSION_NAME')\""
[ "$text_id_2" == '""' ] || text_id_3="§§"
Vip_text_infor="$(eval echo "${text_id_1}${text_id_3}${text_id_2}")"

# Văn bản
Home() {
    [ -z "$Vip_text_infor" ] || echo '<group><text summary="'"$Vip_text_infor"'"/></group>'
    [ -f "$AON/patch_rom/addon.prop" ] && vdbfbfsn='<option type="checkbox" id="v4" box="glog hide_show_patch_rom" silent="true" reload="true">Patch ROM</option>'

    echo '<group>
<page icon="'`urlpng settings`'" config-sh="$ETC/tool-tree.bash Info">
<title>'$setting_text'</title>
<desc>'$home_text_1'</desc>
<option type="default" id="v1" silent="true">'$permis_text_1'</option>
<option type="default" id="v2" silent="true">'$permis_text_4'</option>
<option type="default" id="v3" silent="true">'$setting_text_5'</option>
<handler>
if [ "$menu_id" == "v1" ]; then
    echo "am:[start -a android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -d package:com.tool.tree]"
elif [ "$menu_id" == "v2" ]; then
    echo "am:[start -a android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -d package:com.tool.tree]"
elif [ "$menu_id" == "v3" ]; then
    echo "am:[start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.tool.tree]"
fi
</handler>
</page>
</group>

<group>
<page icon="'`urlpng utilities`'" config-sh="$ETC/tool-tree.bash Utilities">
<title>'$utilities_text'</title>
<desc>'$home_text_2'</desc>
<option type="default" config-sh="$ETC/tool-tree.bash Project">'$setting_text' - '$setting_text_3'</option>
<option type="checkbox" id="v1" box="glog hide_show" silent="true" reload="true" >'$input_folder_text'</option>
'$vdbfbfsn'
<option type="default" id="v2" silent="true">'$open_activity_text' ROM</option>
<option type="default" id="v3" silent="true">'$open_activity_text' (data-root)</option>
<handler>
if [ "$menu_id" == "v1" ]; then
    [ "$(glog hide_show)" == 1 ] && slog hide_show 0 || slog hide_show 1
elif [ "$menu_id" == "v4" ]; then
    [ "$(glog hide_show_patch_rom)" == 1 ] && slog hide_show_patch_rom 0 || slog hide_show_patch_rom 1
elif [ "$menu_id" == "v2" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/external_files${PTSD#$SDCARD_PATH}]"
elif [ "$menu_id" == "v3" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/root$SDH/$PTSH]"
fi
</handler>
</page>
</group>

<group>
<page icon="'`urlpng tools`'" config-sh="$ETC/tool-tree.bash Root">
<title>'$tools_text'</title>
<desc>'$home_text_3'</desc>
</page>
</group>

<group>
<page icon="'`urlpng addon`'" config-sh="PATHADD='$AON' $ETC/tool-tree.bash Addon">
<title>'$addon_text'</title>
<desc>'$home_text_4'</desc>
<option type="refresh">'$refresh_text'</option>
<option type="default" id="hide" silent="true" reload="true">'$hide_add_text'</option>
<option type="default" link="https://zenlua.github.io/Tool-Tree/website/Addon.html" silent="true">'$download_text'</option>
<option type="default" id="xoa" silent="true" reload="true">'$deleted_text'</option>
<option id="file" suffix="add,zip,7z" type="file" style="fab" reload="true">'$input_add_text'</option>
<option type="default" id="home" silent="true" reload="true">'$home_text'</option>
<handler>
case "$menu_id" in
    hide) slog settadd 1 ;;
    xoa) slog settadd 2 ;;
    home) slog settadd 0 ;;
    file) installadd "$file" "$AON"; slog settadd 0 ;;
esac
</handler>
</page>
</group>'

    [ "$(glog shellc)" == 1 ] && shell_bash shellc
}

More() {
    [ -z "$Vip_text_infor" ] || echo '<group><text summary="'"$Vip_text_infor"'"/></group>'
    echo '<group>
<group>
<page icon="'`urlpng settings`'" config-sh="$ETC/tool-tree.bash Info">
<title>'$setting_text'</title>
<desc>'$home_text_1'</desc>
<option type="default" id="v1" silent="true">'$permis_text_1'</option>
<option type="default" id="v2" silent="true">'$permis_text_4'</option>
<option type="default" id="v3" silent="true">'$setting_text_5'</option>
<handler>
if [ "$menu_id" == "v1" ]; then
    echo "am:[start -a android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -d package:com.tool.tree]"
elif [ "$menu_id" == "v2" ]; then
    echo "am:[start -a android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -d package:com.tool.tree]"
elif [ "$menu_id" == "v3" ]; then
    echo "am:[start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.tool.tree]"
fi
</handler>
</page>
</group>

<group>
<page icon="'`urlpng apk_utility`'" config-sh="$ETC/tool-tree.bash Utiliapk">
<title>'$utilities_text'</title>
<desc>'$more_text_6'</desc>
<lock>
[ -f $LOG/javaww ] && echo "'$boot_text_1'" || echo 0
</lock>
<option type="checkbox" id="v1" box="glog hide_show2" silent="true" reload="true">'$input_folder_text'</option>
<option type="default" id="b2" >'$framework_auto_text'</option>
<option type="file" id="v2" suffix="zip" auto-off="true">'$more_text_3'</option>
<option type="file" id="v3" suffix="jar" reload="true" auto-off="true">'$more_text_10' apkeditor.jar</option>
<option type="file" id="b3" suffix="jar" reload="true" auto-off="true">'$more_text_10' apktool.jar</option>
<option type="file" id="b4" suffix="apk">'$more_text_10' framework</option>
<option type="default" id="v4" silent="true">'$open_activity_text' APK</option>
<option type="default" id="v5" silent="true">'$open_activity_text' (data-root)</option>
<handler>
if [ "$menu_id" == "v1" ]; then
    [ "$(glog hide_show2)" == 1 ] && slog hide_show2 0 || slog hide_show2 1
elif [ "$menu_id" == "b2" ]; then
    echo "Looking for system apk..."
    for mm in $(pm list package -s | cut -f2 -d:); do
        cggdccg="$(pm path $mm | cut -f2 -d:)"
        [ -f "$cggdccg" ] && apktool if "$cggdccg" 2>/dev/null | sed "/127.apk/d"
    done
    rm -fr $HOME/.local/share/apktool/framework/1.apk
    echo
    checktime
elif [ "$menu_id" == "b3" ]; then
    echo "'$more_text_4' $file"
    echo
    unzip -oq $ETC/apktool.jar prebuilt/linux/aapt2 -d $TMP
    cp -rf "$file" $TMP/apktool.jar || killtree "File copy error"
    cd $TMP
    zip -qr apktool.jar prebuilt/*
    mv apktool.jar $ETC/apktool.jar
    rm -fr prebuilt
elif [ "$menu_id" == "b4" ]; then
    echo "'$more_text_4' $file"
    echo
    apktool if "$file"
elif [ "$menu_id" == "v2" ]; then
    echo "'$more_text_4' $file"
    echo
    [ "$(unzip -ql "$file" | grep -cm1 ".x509.pem")" == 1 ] || killtree "'$more_text_5' .x509.pem"
    [ "$(unzip -ql "$file" | grep -cm1 ".pk8")" == 1 ] || killtree "'$more_text_5' .pk8"
    unzip -oj "$file" *.x509.pem *.pk8 -d "$ETC/key"
elif [ "$menu_id" == "v3" ]; then
    echo "'$more_text_4' $file"
    echo
    cp -rf "$file" $ETC/apkeditor.jar || killtree "File copy error"
elif [ "$menu_id" == "v4" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/external_files${PTAD#$SDCARD_PATH}]"
elif [ "$menu_id" == "v5" ]; then
    echo "am:[start -a android.intent.action.SEND -t */* -d content://'$PACKAGE_NAME'.provider/root$APK/$PTAH]"
fi
</handler>
</page>
</group>

<group>
<page icon="'`urlpng tool_apk`'" config-sh="$ETC/tool-tree.bash Troot">
<title>'$tools_text'</title>
<desc>'$more_text_7'</desc>
</page>
</group>

<group>
<page icon="'`urlpng apk_addon`'" config-sh="PATHADD='$AOK' $ETC/tool-tree.bash Addon">
<title>'$addon_text'</title>
<desc>'$more_text_8'</desc>
<option type="refresh">'$refresh_text'</option>
<option type="default" id="hide" silent="true" reload="true">'$hide_add_text'</option>
<option type="default" link="https://zenlua.github.io/Tool-Tree/website/Apkon.html" silent="true">'$download_text'</option>
<option type="default" id="xoa" silent="true" reload="true">'$deleted_text'</option>
<option id="file" suffix="add,zip,7z" type="file" style="fab" reload="true">'$input_add_text'</option>
<option type="default" id="home" silent="true" reload="true">'$home_text'</option>
<handler>
case "$menu_id" in
    hide) slog settadd2 1 ;;
    xoa) slog settadd2 2 ;;
    home) slog settadd2 0 ;;
    file) installadd "$file" "$AOK"; slog settadd2 0 ;;
esac
</handler>
</page>
</group>'

    [ "$(glog shellc)" == 1 ] && shell_bash shells
}

Info() {
    echo '<group>
<page icon="'`urlpng info`'" config-sh="$ETC/tool-tree.bash Update" >
<title>'$setting_text_1'</title>
<desc>'$setting_text_2'</desc>
<option type="default" id="share" silent="true">'$share_text'</option>
<option type="refresh" style="fab" icon="'$ETC'/icon/Loading.png" />
<handler>
progress 0.02 10 &
if [ "$menu_id" == "share" ]; then
    echo "am:[start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT https://zenlua.github.io/Tool-Tree]"
fi
</handler>
</page>
</group>

<group>
<page config-sh="$ETC/tool-tree.bash Project" icon="'`urlpng project`'" >
<title>'$setting_text_3'</title>
<desc>'$setting_text_4'</desc>
</page>
</group>

<group>
<page config-sh="$ETC/tool-tree.bash Feature" icon="'`urlpng feature`'" >
<title>'$setting_text_7'</title>
<desc>'$setting_text_8'</desc>
</page>
</group>

<group>
<picker icon="'`urlpng language`'" warning="'$permis_text_3'" auto-kill="true" auto-off="true" option-sh="echo -e '"'|$default_text\nauto|$google_translate_text\nen-US|English\nvi-VN|Việt nam\nru-RU|Русский\nzh-CN|简体中文\nhu-HU|Hungarian\nid-ID|Indonesia'"' ">
<title>'$permis_text_2'</title>
<desc>'$permis_text_5'</desc>
<get>glog language_kkts</get>
<set>
slog language_kkts "$state"
if [ "$state" == "auto" ]; then
    sum_md5_kk="$(checksum $ETC/lang/vi.bash)"
    source $ETC/lang/vi.bash
    [ -f $ETC/lang/auto.sh ] && rm -fr $ETC/lang/auto.sh
    for vc in $(grep "=" $ETC/lang/vi.bash | cut -d= -f1); do
        (
            xfhtfvgf="$(echo "${!vc}" | trans $LANGUAGE-$COUNTRY)"
            echo "${vc}=\"${xfhtfvgf^}\" # ${!vc}" | tee -a $ETC/lang/auto.sh
            xfhtfvgf=""
        ) &
    done
    wait
    if [ "$(grep -q "=\"\"" $ETC/lang/auto.sh)" ]; then
        error_txxt_bug="$(echo "Translation error detected:" | trans -b $LANGUAGE-$COUNTRY)"
        killtree "\n$error_txxt_bug $(grep -c "=\"\"" $ETC/lang/auto.sh)" >&2
        slog language ""
    else
        slog sum_md5_kk "$sum_md5_kk"
    fi
else
    [ -f $ETC/lang/auto.sh ] && rm -fr $ETC/lang/auto.sh
    slog language "$state"
fi
</set>
</picker>
</group>

<group>
<editor title="'$home_text_5'" desc="'$home_text_6'" file="home/usr/run_shella.bash" placeholder="#!/data/data/com.tool.tree/files/home/bin/bash" icon="'`urlpng shella`'"/>
</group>'
}

Update() {
    # Thông báo cập nhật
    link_url="https://api.github.com/repos/Zenlua/Tool-Tree/releases"
    if checkonline; then
        if [ -f $TEMP/update ]; then
            show_update=1
            text_desc_size="$sizes_text: $(cat $TEMP/update | coverbyte)"
        else
            if [ "$(unzip -qp "$PATH_APK" assets/beta 2>/dev/null)" == 1 ]; then
                websums="$(xem $link_url/tags/beta)"
                tagname="${PACKAGE_VERSION_NAME//./}"
            else
                websums="$(xem $link_url/latest)"
                tagname="$(echo "$websums" | jq -r .tag_name | sed -e 's|\.||g' -e 's|V||')"
            fi
            websum="$(echo "$websums" | jq -r .assets[0].digest | cut -d: -f2)"
            filesum="$(checksum "$PATH_APK")"
            websize="$(echo "$websums" | jq -r '.assets[0].size')"
            text_desc_size="$sizes_text: $(coverbyte $websize 2>/dev/null)"
            if [[ ${PACKAGE_VERSION_NAME//./} == $tagname ]]; then
                if [[ "$websum" != "$filesum" ]] && [[ -n $websum ]]; then
                    if [ -n "$tagname" ]; then
                        show_update=1
                        echo "$websize" >$TEMP/update
                    fi
                fi
            else
                if [ -n "$tagname" ]; then
                    show_update=1
                    echo "$websize" >$TEMP/update
                fi
            fi
        fi
    fi

    echo '<group>
<page icon="'`urlpng like`'" html="https://zenlua.github.io/Tool-Tree/website/Information.html" >
<title>'$author_text'</title>
</page>
</group>

<group>
<page icon="'`urlpng website`'" link="https://zenlua.github.io/Tool-Tree" >
<title>'$update_text_5'</title>
</page>
</group>

<group>
<action icon="'`urlpng update`'" warning="'$use_network_text'" visible="echo '$show_update'">
<title>'$update_text'</title>
<desc>'$text_desc_size'</desc>
<set>
echo "'$update_text_2'"
echo
if [ "$(unzip -qp "$PATH_APK" assets/beta 2>/dev/null)" == 1 ]; then
    data_json="$(xem '$link_url'/tags/beta)"
else
    data_json="$(xem '$link_url'/latest)"
fi
websum="$(echo "$data_json" | jq -r .assets[0].digest | cut -d: -f2)"
name_apk="$(echo "$data_json" | jq -r .name)"
if [[ -f "$TMP/Tool-Tree.apk" ]] && [[ "$websum" == "$(checksum "$TMP/Tool-Tree.apk")" ]]; then
    openfile "$TMP/Tool-Tree.apk"
    exit
fi
if [[ "$websum" != "$(checksum "$PATH_APK")" ]]; then
    echo "'$update_text_3'"
    echo
    taive "$(echo "$data_json" | jq -r ".assets[0].browser_download_url")" "$TMP/Tool-Tree.apk" 2>&1
    if [[ "$websum" == "$(checksum "$TMP/Tool-Tree.apk")" ]]; then
        cp -rf "$TMP/Tool-Tree.apk" "$SDCARD_PATH/Download/${name_apk}.apk"
        echo
        echo "'$save_text' $SDCARD_PATH/Download/${name_apk}.apk"
        openfile "$TMP/Tool-Tree.apk"
    fi
else
    echo "'$update_text_4'"
fi
</set>
</action>
<text>
<desc>'"$(cat $TEMP/version.txt 2>/dev/null)"'</desc>
<slices>
<slice photo="'$ETC'/icon/tool-tree.jpg" />
</slices>
</text>
</group>'
}

Project() {
    mkdir -p $PTSD/out &>/dev/null &
    mkdir -p $PTAD/out &>/dev/null &

    # Thêm group
    echo "<group>"
    show_sett
    show_apkset
    echo "</group>"

    echo '<group>
<action icon="'`urlpng cleanup`'" warning="'$project_text_4'" auto-off="true" >
<title>'$project_text_3'</title>
<param name="dels" label="'$option_text'" option-sh="findfile folders $SDH/$PTSH; findfile folders $APK/$PTAH" multiple="multiple"/>
<set>
for vl in $dels; do
    echo "Deleting the folder: $vl"
    rm -fr "$vl"
done
</set>
</action>
</group>

<group>
<action icon="'`urlpng list_tool`'" shell="hidden" reload="true">
<title>'$customize_tools_text'</title>
<param name="un_tool_ext4" value-sh="glog un_tool_ext4 0" label="'$option_text'" title="'$tool_unpack_text' ext4" options-sh="echo -e '"'0|imgextractor\n1|imgkit_scuti'"' "/>
<param name="un_tool_erofs" value-sh="glog un_tool_erofs 0" label="'$option_text'" title="'$tool_unpack_text' erofs" options-sh="echo -e '"'0|extract.erofs\n1|imgkit_scuti\n2|extract.erofs (old)'"' "/>
<param name="un_tool_f2fs" value-sh="glog un_tool_f2fs 0" label="'$option_text'" title="'$tool_unpack_text' f2fs" options-sh="echo -e '"'0|extract.f2fs\n1|imgkit_scuti'"' "/>
<param name="re_tool_ext4" value-sh="glog re_tool_ext4 0" label="'$option_text'" title="'$tool_repack_text' ext4" options-sh="echo -e '"'0|make_ext4fs\n1|mke2fs+e2fsdroid\n2|imgkit_scuti'"' "/>
<param name="re_tool_erofs" value-sh="glog re_tool_erofs 0" label="'$option_text'" title="'$tool_repack_text' erofs" options-sh="echo -e '"'0|mkfs.erofs\n1|imgkit_scuti\n2|mkfs.erofs (old)'"' "/>
<param name="re_tool_f2fs" value-sh="glog re_tool_f2fs 0" label="'$option_text'" title="'$tool_repack_text' f2fs" options-sh="echo -e '"'0|sload_f2fs'"' "/>
<set>
slog un_tool_ext4 "$un_tool_ext4"
slog un_tool_erofs "$un_tool_erofs"
slog un_tool_f2fs "$un_tool_f2fs"
slog re_tool_ext4 "$re_tool_ext4"
slog re_tool_erofs "$re_tool_erofs"
slog re_tool_f2fs "$re_tool_f2fs"
</set>
</action>
</group>'
}

Feature() {
    echo '<group>
<switch icon="'`urlpng set_home`'" shell="hidden">
<title>'$project_text_5'</title>
<get>glog Tset</get>
<set>slog Tset $state</set>
</switch>

<switch icon="'`urlpng icon_off`'" shell="hidden" reload="true">
<title>'$project_text_7'</title>
<get>glog Ticon</get>
<set>slog Ticon $state</set>
</switch>

<switch icon="'`urlpng shell_off`'" shell="hidden">
<title>'$project_text_6'</title>
<get>glog shellc</get>
<set>slog shellc $state</set>
</switch>

<switch icon="'`urlpng log_ngang`'" shell="hidden" >
<title>'$scroll_ngang_text'</title>
<get>glog scroll_ngang</get>
<set>slog scroll_ngang $state</set>
</switch>
</group>

<group>
<action icon="'`urlpng java`'" warning="'$project_text_9'" shell="hidden" >
<title>'$project_text_10'</title>
<param name="ramoccupied" label="'$option_text'" option-sh="echo -e '"'512\n1024\n2048\n3072\n4096\n5120\n6144\n7168\n8192'"' " value-sh="glog ramoccupied 4096" />
<set>
slog ramoccupied "$ramoccupied"
</set>
</action>
</group>

<group>
<action icon="'`urlpng icon_info`'" title="'$infor_text'" shell="hidden">
<param name="show_infor_text_1" title="Text 1" value-sh="glog show_infor_text_1" type="text" />
<param name="show_infor_text_2" title="Text 2" value-sh="glog show_infor_text_2" type="text" />
<set>
slog show_infor_text_1 "$show_infor_text_1"
slog show_infor_text_2 "$show_infor_text_2"
</set>
</action>
</group>

<group>
<action icon="'`urlpng cpu`'" warning="'$project_text_13'" shell="hidden" visible="command -v taskset &>/dev/null && echo 1">
<title>'$project_text_12'</title>
<param name="use_cpus" label="'$option_text'" option-sh="seq 1 $(nproc --all)" value-sh="glog use_cpu" />
<set>slog use_cpu "$use_cpus"</set>
</action>
</group>

<group>
<action icon="'`urlpng background`'" warning="'$project_text_15'" shell="hidden" auto-restart="true">
<title>'$project_text_14'</title>
<param name="dissblur" label="'$dissblur_text'" value-sh="glog dissblur" type="bool" />
<param name="uri_change_background" type="file" suffix="jpg" value-sh="glog uri_change_background" editable="true" />
<set>
slog dissblur "$dissblur"
slog uri_change_background "$uri_change_background"
[ -f "$uri_change_background" ] && cp -f "$uri_change_background" "$ETC/wallpaper.jpg"
[ -z "$uri_change_background" ] && rm -f "$ETC/wallpaper.jpg"
set_permis "$ETC/wallpaper.jpg" &>/dev/null
</set>
</action>
</group>'
}

Root() {
    echo '<group>
<action icon="'`urlpng mount`'" interruptible="false">
<title>'$mount_text_1'</title>
<summary>'$show_root_text'</summary>
<lock>[ "$ROT" == 0 ] && echo "'$root_warning_text'" || echo 0</lock>
<set>
for kkh in $IMG_NAME; do
    if [ "$(ls $SDH/raw/${kkh%.*} 2>/dev/null)" ]; then
        su -mm -c umount -l $SDH/raw/${kkh%.*}
    fi
    mkdir -p $SDH/raw/${kkh%.*}
    su -mm -c mount -w $PTSD/$kkh $SDH/raw/${kkh%.*}
done
echo "'$save_text' $SDH/raw"
</set>
<param name="IMG_NAME" options-sh="findfile 3 $PTSD | grep -E '"'\(f2fs\)|\(ext\)|\(erofs\)'"' " desc="'$mount_text_2'" required="true" multiple="true"/>
</action>

<action icon="'`urlpng umount`'" interruptible="false">
<title>'$umount_text_1'</title>
<summary>'$show_root_text'</summary>
<lock>
[ "$ROT" == 0 ] && echo "'$root_warning_text'" || echo 0
</lock>
<set>
for kkh in $IMG_NAME; do
    su -mm -c umount -l $SDH/raw/$kkh
    rm -fr $SDH/raw/$kkh
    if [ "$(checktype $PTSD/${kkh}.img)" == "ext" ]; then
        [ -f $PTSD/${kkh}.img ] && e2fsck -yf $PTSD/${kkh}.img &>/dev/null
    elif [ "$(checktype $PTSD/${kkh}.img)" == "f2fs" ]; then
        [ -f $PTSD/${kkh}.img ] && fsck.f2fs -yf $PTSD/${kkh}.img &>/dev/null
    fi
done
echo "'$umount_text_2'"
</set>
<param name="IMG_NAME" options-sh="findfile 4 $SDH/raw" desc="'$umount_text_3'" required="true" multiple="true"/>
</action>
</group>

<group>
<action icon="'`urlpng backup`'" interruptible="false">
<title>'$backup_text_1'</title>
<summary>'$show_root_text'</summary>
<lock>
[ "$ROT" == 0 ] && echo "'$root_warning_text'" || echo 0
</lock>
<param name="IMG" multiple="true" options-sh="search_image" label="IMAGE" desc="'$backup_text_2 $PTSD'" required="true"/>
<set>
Extract=$PTSD/backup
[[ ! -d "$Extract" ]] && mkdir -p "$Extract"
for i in $IMG; do
    e=${i##*/}
    File="$Extract/${e}.img"
    if [[ ! -L $i ]]; then
        echo "'$backup_text_3' $e" >&2
    else
        echo "'$backup_text_4' $e"
        echo
        dd if="$i" of="$File" 2>&1
        echo
    fi
done
echo "'$save_text' $Extract"
</set>
</action>

<action icon="'`urlpng flash`'">
<title>'$flash_text_1'</title>
<summary>'$show_root_text'</summary>
<lock>
[ "$ROT" == 0 ] && echo "'$root_warning_text'" || echo 0
</lock>
<param name="CQ" label="'$flash_text_2'" type="checkbox" depend-on="CQ1" depend-value="1" depend-mode="hide" depend-cascade="false" depend-readonly="true"/>
<param name="CQ1" label="'$flash_text_3'" type="checkbox" depend-on="CQ" depend-value="1" depend-mode="hide" depend-cascade="false" depend-readonly="true"/>
<param name="IMG" label="IMAGE" title="'$flash_text_4'" desc="'$flash_text_5'" options-sh="search_image" required="true"/>
<param name="Brush_in" type="file" suffix="img" editable="true" required="true" title="'$flash_text_6'" desc="'$flash_text_7'" required="true"/>
<set>
e=${IMG##*/}
if [ "$e" = "vendor" ] || [ "$e" = "system" ] || [ "$e" = "super" ]; then
    killtree "($e) '$flash_text_8'"
fi
echo "'$more_text_4' $Brush_in"
echo
if [ "$(checktype "$Brush_in")" == "space" ]; then
    simg2img "$Brush_in"
fi
if [[ -f "$Brush_in" ]]; then
    echo "Flash (${Brush_in##*/}) ➠ ($e)"
    echo
    dd if="$Brush_in" of="$IMG" 2>&1
    if [[ $CQ1 = 1 ]]; then
        echo
        echo "'$flash_text_9'..."
        for i in $(seq 4 -1 1); do
            echo $i
            sleep 1
        done
        reboot recovery
    fi
    if [[ $CQ = 1 ]]; then
        echo
        echo "'$flash_text_10'..."
        for i in $(seq 4 -1 1); do
            echo $i
            sleep 1
        done
        reboot
    fi
else
    killtree "! ($Brush_in) '$flash_text_11' ($e)"
fi
echo
echo "'$flash_text_12'"
</set>
</action>
</group>'
}

Troot() {
    echo '<group>
<action icon="'`urlpng dexopt_app`'">
<lock>
[ "$ROT" == 0 ] && echo "'$root_warning_text'" || echo 0
</lock>
<title>'$dexopt_app_text'</title>
<summary>'$show_root_text'</summary>
<param name="name_dex_list" label="'$option_text'" options-sh="echo -e '"'everything\nspeed\nspeed-profile\nverify'"'" value="speed-profile"/>
<param name="bools" label="'$dexopt_app_text_2'" desc="'$dexopt_app_text_3'" type="checkbox" />
<param name="apps" type="app" multiple="multiple" desc="'$dexopt_app_text_1'" options-sh="pm list package -3 | cut -f2 -d:" />
<set>
if [ "$bools" == 1 ]; then
    pm compile -v -a -m $name_dex_list
    echo
    checktime
else
    for vv in $apps; do
        pm compile -v -f -m $name_dex_list $vv
        echo
    done
    checktime
fi
</set>
</action>
</group>

<group>
<action icon="'`urlpng backup_apk`'" interruptible="false" warning="'$backups_text_1'">
<title>'$backups_text_2'</title>
<param label="'$backups_text_3'" name="Sapp" type="app" multiple="multiple"/>
<set>
for v in $Sapp; do
    patk="$(pm path $v | cut -f2 -d:)"
    patk22="$(pm path "$v" | cut -f2 -d: | head -n1)"
    pathvv="${patk22%/*}"
    hcdf="$(echo "$patk" | grep -c "\.apk"$)"
    paptn="$(echo "$patk" | grep "base\.apk"$)"
    if [[ -n "$paptn" ]]; then
        infor="$(apkeditor info -i "$paptn")"
        nameapk="$(echo "$infor" | grep -m1 "AppName" | cut -d\" -f2)"
    else
        nameapk="${pathvv##*/}"
    fi
    if [ "$hcdf" -ge 2 ]; then
        zip -j -r "$PTAD/${nameapk}.apks" $patk
        echo "'$save_text' $PTAD/${nameapk}.apks"
    else
        cp -rf "$patk" "$PTAD/${nameapk}.apk"
        echo "'$save_text' $PTAD/${nameapk}.apk"
    fi
done
</set>
</action>
</group>'
}

Generate() {
    # Thêm ẩn
    if [ "$(glog hide_show_generate)" == 1 ]; then
        echo "<group>"
        show_sett
        echo "</group>"
    fi

    echo '<group>
<action icon="'`urlpng build_super`'">
<title>'$generate_text' Super</title>
<param name="type" label="'$super_text_2'" value-sh="glog typeheh">
<option value="A">a_only</option>
<option value="AB">ab</option>
<option value="VAB">virtual_ab</option>
</param>
<param name="from" label="'$super_text_3'" value-sh="glog fromdjfh">
<option value="raw">raw</option>
<option value="sparse">sparse</option>
</param>
<param name="super_size" required="true" value-sh="glog super_sizedj 8.5" label="'$sizes_text'" desc="'$default_text': 8.5GB" type="number"/>
<param name="super_group" required="true" value-sh="glog super_group qti_dynamic_partitions" label="'$super_text_5'" desc="'$super_text_6'" />
<param name="IMAGES" desc="'$super_text_7'" options-sh="findfile 3 $PTSD" required="true" multiple="true"/>
<set>
slog typeheh "$type"
slog fromdjfh "$from"
slog super_sizedj "$super_size"
slog super_group "$super_group"
repack_super -m "$IMAGES" -g "$super_group" -s "$super_size" -f "$from" -t "$type" -i "$PTSD"
echo
checktime
</set>
</action></group>

<group>
<action icon="'`urlpng build_payload`'" >
<title>'$generate_text' Payload</title>
<param name="payload_switch" value-sh="glog payload_switch" label="'$payload_text_3'" type="switch" />
<param name="payload_super_size" required="true" value-sh="glog payload_super_size 11" label="'$sizes_text'" desc="'$default_text': 11GB, '$payload_text_4'" type="number" depend-on="payload_switch" depend-value="0" depend-mode="hide" depend-readonly="true"/>
<param name="payload_super_group" required="true" value-sh="glog payload_super_group qti_dynamic_partitions" label="'$super_text_5'" desc="'$super_text_6', '$payload_text_4'" depend-on="payload_switch" depend-value="0" depend-mode="hide" depend-readonly="true"/>
<param name="sign_payload" label="'$sign_text'" options-sh="findfile file $ETC/key/2048 .pem | sed '"'s|.pem||'"'" value-sh="glog sign_payload testkey"/>
<param name="IMAGES" desc="'$payload_text_2'" options-sh="findfile 11 $PTSD" required="true" multiple="true"/>
<set>
slog sign_payload "$sign_payload"
slog payload_switch "$payload_switch"
slog payload_super_size "$payload_super_size"
slog payload_super_group "$payload_super_group"
payload_repack -m "$IMAGES" -i "$PTSD" -s "$sign_payload" -w "$payload_switch" -e "$payload_super_size" -g "$payload_super_group"
echo
checktime
</set>
</action></group>

<group>
<action icon="'`urlpng build_amlogic`'">
<title>'$generate_text' Amlogic</title>
<param name="amlogic_boolbox" value-sh="glog amlogic_boolbox" label="'$deleted_project_text'" type="checkbox" />
<param name="amlogic_ver" label="'$version_text'" value-sh="glog amlogic_ver v2">
<option value="v2">v2</option>
<option value="v1">v1</option>
</param>
<param name="amlogic_align" label="'$alignment_text'" value-sh="glog amlogic_align 8">
<option value="4">4</option>
<option value="8">8 (Android 11+)</option>
</param>
<param name="FOLDER" desc="'$builds_text_1'" options-sh="findfile file $PTSD platform.conf | sed '"'s|/platform\.conf||'"' " required="true" multiple="true"/>
<set>
slog amlogic_boolbox "$amlogic_boolbox"
slog amlogic_ver "$amlogic_ver"
echo "'$apkb_text_1' $PTSD/$FOLDER"
echo
if [ "$(checktype "$PTSD/$FOLDER/super.img")" == "super" ]; then
    echo "'$unpack_text_0' super(raw) ➠ super(sparse)..."
    img2simg "$PTSD/$FOLDER/super.img"
    echo
fi
if [ -n "$(ls -1d $PTSD/$FOLDER/*.img 2>/dev/null)" ]; then
    for vv in $PTSD/$FOLDER/*.img; do
        echo "mv: ${vv##*/} ➠ $(echo "${vv##*/}" | sed "s|\.img$|\.PARTITION|")"
        mv "$vv" "${vv%.*}.PARTITION"
    done
    echo
fi
ampack pack --verify --out-ver $amlogic_ver --out-align $amlogic_align "$PTSD/$FOLDER" "$PTSD/out/$FOLDER.img" | tee $TMP/amlogic_pack.log
[ "$?" == 1 ] && bug_rom=1 || bug_rom=0
for vv in $(ls -1d $PTSD/$FOLDER/*.PARTITION 2>/dev/null); do
    mv "$vv" "${vv%.*}.img"
done
[ "$bug_rom" == 1 ] && killtree "Log check error: $TMP/amlogic_pack.log"
[ "$amlogic_boolbox" == 1 ] && rm -fr "$PTSD/$FOLDER"
echo "'$unpack_text_2' $TMP/amlogic_pack.log"
echo
echo "'$save_text' $PTSD/out/$FOLDER.img"
echo
checktime
</set>
</action></group>'
}

Utilities() {
    [ -d $PTSD/out ] && mkdir -p $PTSD/out &>/dev/null &
    time_riviu="$(date -d "@`glog build_times 1230768000`")"

    if [ "$(glog hide_show 1)" == 1 ]; then
        echo "<group>"
        show_sett
        echo "</group>"
    else
        desc_rom="$path_text: $(glog PTSD | sed "s|$SDCARD_PATH|\/sdcard|")"
        desc_rom1="$projects_text: $PTSH"
    fi

    echo '<group>
<action icon="'`urlpng decom`'" desc="'$desc_rom'">
<title>'$decompile_text'</title>
<param name="cboxk" value-sh="glog dkhdh" label="'$deleted_file_text'" type="checkbox" />
<param name="nounpak" value-sh="glog dkjdj" label="'$decode_text_1'" type="switch" />
<param name="xoa_oat_boot" value-sh="glog xoa_oat_boot" label="'$xoaoat_text_1'" type="switch" depend-on="nounpak" depend-value="1" depend-mode="hide" depend-readonly="true"/>
<param name="vavb" label="'$builds_text_8'" type="switch" depend-on="nounpak" depend-value="1" depend-mode="hide" depend-readonly="true"/>
<param name="IMAGES" desc="'$decode_text_3'" multiple="true" options-sh="findfile 2 $PTSD" required="true"/>
<set>
slog vavbbgdf "$vavb"
slog xoa_oat_boot "$xoa_oat_boot"
slog dkjdj "$nounpak"
slog dkhdh "$cboxk"
for vkl in $IMAGES; do
    if [ -f "$PTSD/${vkl#*=}" ]; then
        unpack_img -i "$PTSD/${vkl#*=}" -p "${vkl%%=*}" -o "$SDH/$PTSH" -n $nounpak -d $cboxk -r $xoa_oat_boot -a $vavb
    else
        unpack_img -i "$PTSD/$vkl" -o "$SDH/$PTSH" -n $nounpak -d $cboxk -r $xoa_oat_boot -a $vavb
    fi
done
checktime
</set>
</action>

<action icon="'`urlpng build`'" desc="'$desc_rom1'">
<title>'$build_text'</title>
<param name="boolbox" value-sh="glog boolboxdjh" label="'$deleted_project_text'" type="checkbox" />
<param name="IMAGES" title="'$list_partition_text'" desc="'$builds_text_1'" multiple="true" options-sh="findfile 0 $SDH/$PTSH" required="true"/>
<param name="dinh_dang" value-sh="glog dinh_dang 0" label="'$build_text'" desc="'$builds_text_2'" options-sh="echo -e '"'0|$default_text\n1|RO (EROFS)\n2|RW (EXT4)\n3|RO (F2FS)\n4|RW (F2FS)'"'" depend-on="IMAGES" depend-value="(erofs),(ext),(f2fs)" depend-mode="show" depend-default="hide"/>
<param name="dang_nen" value-sh="glog dang_nen lz4hc" label="'$option_text'" desc="'$builds_text_3'" options-sh="echo -e '"'lz4hc\nlz4\nlzma\ndeflate\nzstd'"'" depend-on="dinh_dang|dinh_dang|IMAGES" depend-value="EROFS|EXT4,F2FS|(erofs)" depend-mode="show|hide|show" depend-logic="priority" depend-default="hide"/>
<param name="muc_nen" value-sh="glog muc_nen 8" label="'$builds_text_4'" desc="'$builds_text_6': lz4: 0, lz4hc: 0-12, deflate,lzma: 0-9, zstd: 0-22" min="0" max="22" type="seekbar" depend-on="dang_nen" depend-value="lz4" depend-mode="hide" depend-logic="priority"/>
<param name="format_img" value-sh="glog format_imgs raw" label="'$convert_text'" desc="'$convert_img_text'" depend-on="IMAGES" depend-value="(erofs),(ext),(f2fs)" depend-mode="show">
<option value="raw">File.img (raw)</option>
<option value="sparse">File.img (sparse)</option>
<option value="zstd">File.img.zstd</option>
<option value="zst">File.img.zst</option>
<option value="dat">File.new.dat</option>
<option value="br">File.new.dat.br</option>
</param>
<param name="nen_br" required="true" value-sh="glog nen_br 4" label="'$builds_text_4'" type="seekbar" min="0" max="22" desc="'$convert_text_2'" depend-on="format_img" depend-value="raw,sparse,File.new.dat" depend-mode="hide"/>
<param name="build_times" label="'$time_text'" value-sh="glog build_times" type="number" desc="'$build_time_text_1': '$time_riviu'" required="required" depend-on="IMAGES" depend-value="(erofs),(ext),(f2fs)" depend-mode="show" />
<param name="offfscontex" value-sh="glog offfscontex 1" label="'$patch_text_fscontex'" type="switch" depend-on="IMAGES" depend-value="(erofs),(ext),(f2fs)" depend-mode="show"/>
<param name="build_size" label="'$sizes_text'" value-sh="glog build_size 0" type="number" desc="'$builds_text_7'" required="required" depend-on="dinh_dang|dinh_dang|IMAGES" depend-value="EROFS|EXT4,F2FS|(ext),(f2fs)" depend-mode="hide|show|show" depend-logic="priority" depend-default="hide"/>
<set>
slog dang_nen "$dang_nen"
slog format_imgs "$format_img"
slog boolboxdjh "$boolbox"
slog dinh_dang "$dinh_dang"
slog build_size "$build_size"
slog offfscontex "$offfscontex"
slog muc_nen "$muc_nen"
slog nen_br "$nen_br"
slog build_times "$build_times"
for vkl in $IMAGES; do
    repack_img -i "$SDH/$PTSH/$vkl" -o "$PTSD" -n "$dang_nen" -l $muc_nen -k $dinh_dang -s $build_size -d $boolbox -c $format_img -p "$offfscontex"
done
echo "'$save_text' $PTSD/out"
echo
checktime
</set>
</action>
</group>

<group>
<page icon="'`urlpng generate`'" config-sh="$ETC/tool-tree.bash Generate">
<title>'$synthetic_text'</title>
<option type="checkbox" box="glog hide_show_generate" silent="true" id="v1" auto-off="true" reload="true">'$folder_text' ROM</option>
<handler>
if [ "$menu_id" == "v1" ]; then
    [ "$(glog hide_show_generate)" == 1 ] && slog hide_show_generate 0 || slog hide_show_generate 1
fi
</handler>
</page></group>

<group>
<action icon="'`urlpng super_split`'">
<title>'$super_split_text_1'</title>
<param name="cboxk" value-sh="glog cboxkshg" label="'$deleted_file_text'" type="checkbox" />
<param name="size" label="'$sizes_text'" required="true" value-sh="glog sizedhhe 1024" desc="'$super_split_text_2'" type="number"/>
<param name="IMAGES" desc="'$super_split_text_3'" options-sh="findfile 7 $PTSD" label="'$option_text'" required="true"/>
<set>
slog cboxkshg "$cboxk"
slog sizedhhe "$size"
echo "'$super_split_text_4' ${IMAGES}..."
mkdir -p "$PTSD/out"
cd "$PTSD/out"
[ $(checktype "$PTSD/$IMAGES") == "sparse" ] && simg2img "$PTSD/$IMAGES"
chunk_split -s .%d -B 4K -C "$size"M "$PTSD/$IMAGES"
[ "$cboxk" == 0 ] || rm -fr "$PTSD/$IMAGES"
echo
echo "'$save_text' $PTSD"
echo
checktime
</set>
</action>

<action icon="'`urlpng super_merge`'">
<title>'$super_merge_text_1'</title>
<param name="silence" value-sh="glog silence 1" label="'$deleted_file_text'" type="checkbox" />
<param name="MERGE" desc="'$super_merge_text_3'" options-sh="findfile 5 $PTSD | sort -n -t . -k 3" required="true" multiple="true"/>
<set>
slog silence $silence
echo "'$super_merge_text_2'..."
simg2img $MERGE "$PTSD/super.img" || killtree "Error" "$PTSD/super.img"
[ "$silence" == 0 ] || rm -fr $MERGE
echo
echo "'$save_text' $PTSD/super.img"
echo
checktime
</set>
</action>
</group>

<group>
<action icon="'`urlpng convert_file`'" >
<title>'$convert_text_1'</title>
<param name="cboxk" value-sh="glog cboxksbhd" label="'$deleted_file_text'" type="checkbox" />
<param name="format_img" value-sh="glog format_img raw" required="true" label="'$option_text'" >
<option value="raw">File.img (raw)</option>
<option value="sparse">File.img (sparse)</option>
<option value="zstd">File.img.zstd</option>
<option value="zst">File.img.zst</option>
<option value="lzma">File.img.lzma</option>
<option value="lz4">File.img.lz4</option>
<option value="xz">File.img.xz</option>
<option value="gz">File.img.gz</option>
<option value="dat">File.new.dat</option>
<option value="br">File.new.dat.br</option>
</param>
<param name="nen_br" required="true" value-sh="glog nen_br 4" label="'$builds_text_4'" type="seekbar" min="0" max="22" desc="'$convert_text_2'" depend-on="format_img" depend-value="raw,sparse,File.new.dat" depend-mode="hide" depend-readonly="true"/>
<param name="IMAGES" desc="'$convert_text_3'" options-sh="findfile 1 $PTSD" multiple="true" required="true"/>
<set>
slog format_img "$format_img"
slog nen_br "$nen_br"
slog cboxksbhd "$cboxk"
for vinput in $IMAGES; do
    cover_img -i "$PTSD/$vinput" -o "$PTSD/out" -c $format_img -l $nen_br -d $cboxk
done
echo "'$save_text' $PTSD/out"
echo
checktime
</set>
</action>
</group>'

    if [ "$(glog hide_show_patch_rom 1)" == 1 ] && [ -f "$AON/patch_rom/addon.prop" ]; then
        dirvad="$AON/patch_rom"
        echo '<group>
<page icon="'`urladd icon`'" config-sh="'$dirvad'/index.bash home">
<title>Patch ROM</title>
<option type="checkbox" box="glog auto_trans_text_patch_rom" id="v1" auto-off="true" reload="true" interruptible="false">'$google_translate_text'</option>
<option type="refresh">'$refresh_text'</option>
<option type="default" auto-finish="true" id="123">'$update_text' add-on</option>
<handler>
if [ "$menu_id" == "v1" ]; then
    [ "$(glog auto_trans_text_patch_rom)" == 1 ] && slog auto_trans_text_patch_rom 0 || slog auto_trans_text_patch_rom 1
elif [ "$menu_id" == "123" ]; then
    '$dirvad'/index.bash update_addon
fi
</handler>
</page>
</group>'
    fi
}

Apex() {
    if [ "$(glog hide_show_apex)" == 1 ]; then
        echo "<group>"
        show_apkset
        echo "</group>"
    else
        desc_apkd="$path_text: $(glog PTAD | sed "s|$SDCARD_PATH|\/sdcard|")"
        desc_apkd1="$projects_text: $PTAH"
    fi

    echo '
<group>
<action icon="'`urlpng decom`'">
<title>'$decompile_text'</title>
<desc>'$desc_apkd'</desc>
<param name="FILE" desc="'$apex_text_2'" multiple="true" options-sh="findfile 12 $PTAD" required="true"/>
<set>
IFS=$'"'\n'"'
for vv in $FILE; do
    apexeditor -s decom -i "$PTAD/$vv" -o "$APK/$PTAH"
    echo
done
echo "'$save_text' $APK/$PTAH"
echo
checktime
</set>
</action>

<action icon="'`urlpng build`'" desc="'$desc_apkd1'">
<title>'$build_text'</title>
<param name="gobo_apex" value-sh="glog gobo_apex" label="'$deleted_project_text'" type="checkbox" />
<param name="SIGNS" value-sh="glog signs_apex com.android.example.apex" options-sh="findfile file $ETC/key/4096 .pem | sed '"'s|.pem||'"' " label="'$sign_text'" />
<param name="APIs" label="API" value-sh="glog apis_apex auto" options-sh="echo -e '"'auto|$default_text\n30\n31\n32\n33\n34\n35\n36'"' " />
<param name="nen_apex" value-sh="glog nen_apex" label="'$apex_text_1'" type="switch" />
<param name="payload_type" label="'$super_text_2'" value-sh="glog payload_type auto" options-sh="echo -e '"'auto|$default_text\next4\nerofs\nf2fs'"' " />
<param name="FILE" multiple="true" options-sh="findfile forapex $APK/$PTAH" required="true" desc="'$builds_text_1'" />
<set>
slog gobo_apex "$gobo_apex"
slog nen_apex "$nen_apex"
slog apis_apex "$APIs"
slog payload_type "$payload_type"
slog signs_apex "$SIGNS"
IFS=$'"'\n'"'
for vv in $FILE; do
    apexeditor -s build -f "$payload_type" -k "$SIGNS" -a "$APIs" -c "$nen_apex" -d "$gobo_apex" -i "$APK/$PTAH/$vv" -o "$PTAD/out"
    echo
done
echo "'$save_text' $PTAD/out"
echo
checktime
</set>
</action></group>'
}

Utiliapk() {
    [ -d $PTAD/out ] && mkdir -p $PTAD/out &>/dev/null &

    if [ "$(glog hide_show2 1)" == 1 ]; then
        echo "<group>"
        show_apkset
        echo "</group>"
    else
        desc_apks="$path_text: $(glog PTAD | sed "s|$SDCARD_PATH|\/sdcard|")"
        desc_apks1="$projects_text: $PTAH"
    fi

    echo '<group>
<action icon="'`urlpng decom`'" warn="'$decom_apk_text_15'">
<title>'$decompile_text'</title>
<desc>'$desc_apks'</desc>
<param name="tooldecom" label="'$tools_text'" title="'$customize_tools_text'" value-sh="glog tooldecom apkeditor" option-sh="echo -e '"'apkeditor|Apkeditor\napktool|Apktool'"'"/>
<param name="mutiresk" title="'$decom_apk_text_11'" label="'$option_text'" value-sh="glog mutiresk 1" option-sh="echo -e '"'0|$decom_apk_text_3\n1|$default_text\n2|$decom_apk_text_5'"'" depend-on="tooldecom" depend-value="apkeditor" depend-mode="hide"/>
<param name="type_apk" title="'$decom_apk_text_11'" value-sh="glog type_apk xml" label="'$option_text'" depend-on="tooldecom" depend-value="apktool" depend-mode="hide">
<option value="raw">'$decom_apk_text_3'</option>
<option value="xml">'$default_text'</option>
<option value="reso">'$decom_apk_text_10'</option>
</param>
<param name="dexlibk" title="'$decom_apk_text_12'" label="'$option_text'" value-sh="glog dexlibk 2" option-sh="echo -e '"'0|$decom_apk_text_3\n1|$default_text\n2|Baksmali 3.0.9'"'" depend-on="tooldecom" depend-value="apkeditor" depend-mode="hide"/>
<param name="dexlib" title="'$decom_apk_text_12'" label="'$option_text'" value-sh="glog dexlib smali" option-sh="echo -e '"'nodex|$decom_apk_text_3\ninternal|$default_text\nsmali|Baksmali 3.0.9'"'" depend-on="tooldecom" depend-value="apktool" depend-mode="hide"/>
<param name="xoa_debug_info" value-sh="glog xoa_debug_info 1" label="'$decom_apk_text_7'" type="switch" depend-on="dexlib|dexlibk" depend-value="nodex|0" depend-mode="hide|hide" depend-cascade="false" depend-readonly="true" depend-logic="priority"/>
<param name="FILE" multiple="multiple" option-sh="findfile 9 $PTAD" required="true" title="'$decom_apk_text_9'" desc="'$input_file_text': apk, apks, apkm, xapk, jar, zip"/>
<set>
slog dexlib "$dexlib"
slog tooldecom "$tooldecom"
slog xoa_debug_info "$xoa_debug_info"
slog type_apk "$type_apk"
slog dexlibk "$dexlibk"
slog mutiresk "$mutiresk"
IFS=$'"'\n'"'
for vapk in $FILE; do
    if [ "$tooldecom" == "apkeditor" ]; then
        apkeditor_d -i "$PTAD/$vapk" -t "$type_apk" -b "$xoa_debug_info" -d "$dexlib" -o "$APK/$PTAH"
    else
        apktool_d -i "$PTAD/$vapk" -r "$mutiresk" -b "$xoa_debug_info" -d "$dexlibk" -o "$APK/$PTAH"
    fi
    echo
done
echo "'$save_text' $APK/$PTAH"
echo
checktime
</set>
</action>

<action icon="'`urlpng build`'" warn="'$build_apk_text_2'">
<title>'$build_text'</title>
<desc>'$desc_apks1'</desc>
<param name="xoatm" label="'$deleted_project_text'" type="bool" value-sh="glog xoatm 0" />
<param name="sign" value-sh="glog sign default" option-sh="findfile file $ETC/key .pk8 | sed '"'s|.pk8||'"'" label="'$sign_text'"/>
<param name="sstring" label="'$build_apk_text_1'" type="switch" value-sh="glog sstring 1"/>
<param name="redivdd" label="'$decom_apk_text_14'" type="switch" value-sh="glog redivdd"/>
<param name="copysign" label="'$decom_apk_text_13'" type="switch" value-sh="glog copysign" depend-on="FOLDER" depend-value="(apktool)" depend-mode="show" depend-default="hide"/>
<param name="comlib" label="'$addlang_text_2'" desc="'$addlang_text_3'" value-sh="glog comlib" option-sh="echo -e '"'manifest|$default_text\ntrue|$on_text\nfalse|$off_text'"'"/>
<param name="FOLDER" required="true" option-sh="findfile forapk $APK/$PTAH" multiple="multiple" desc="'$builds_text_1'"/>
<set>
slog sign "$sign"
slog comlib "$comlib"
slog sstring "$sstring"
slog xoatm "$xoatm"
slog copysign "$copysign"
slog redivdd "$redivdd"
IFS=$'"'\n'"'
for vbapk in $FOLDER; do
    if [ -f "$APK/$PTAH/$vbapk/archive-info.json" ]; then
        apkeditor_b -i "$APK/$PTAH/$vbapk" -o "$PTAD/out" -s "$sign" -n "$sstring" -d "$xoatm" -x "$comlib" -r "$redivdd"
    else
        apktool_b -i "$APK/$PTAH/$vbapk" -o "$PTAD/out" -c "$copysign" -s "$sign" -n "$sstring" -d "$xoatm" -r "$redivdd" -x "$comlib"
    fi
    echo
done
echo "'$save_text' $PTAD/out"
echo
checktime
</set>
</action></group>

<group>
<page icon="'`urlpng apex`'" config-sh="$ETC/tool-tree.bash Apex" >
<title>'$apex_text'</title>
<option type="checkbox" box="glog hide_show_apex" id="v1" silent="true" reload="true">'$folder_text' APK</option>
<handler>
if [ "$menu_id" == "v1" ]; then
    [ "$(glog hide_show_apex)" == 1 ] && slog hide_show_apex 0 || slog hide_show_apex 1
fi
</handler>
</page></group>

<group>
<action icon="'`urlpng apk_distur`'" warning="'$distur_apk_text_1'">
<title>'$distur_apk_text_2'</title>
<param name="FILE" multiple="multiple" option-sh="findfile 10 $PTAD" required="true" />
<set>
IFS=$'"'\n'"'
for v in $FILE; do
    echo "'$more_text_4' $FILE"
    echo
    apkeditor p -skip-manifest -f -i "$PTAD/$v" -o "$PTAD/out/$v" 2>&1 | sed -u -e "1,/__/d"
    echo
done
checktime
</set>
</action>

<action icon="'`urlpng apk_restore`'" warning="'$apk_restore_text_1'">
<title>'$apk_restore_text_2'</title>
<param name="FILE" multiple="multiple" option-sh="findfile 10 $PTAD" required="true" />
<set>
IFS=$'"'\n'"'
for v in $FILE; do
    echo "'$more_text_4' $FILE"
    echo
    apkeditor x -fix-types -f -i "$PTAD/$v" -o "$PTAD/out/$v" 2>&1 | sed -u -e "1,/__/d"
    echo
done
checktime
</set>
</action></group>

<group>
<action icon="'`urlpng merge_apk`'" warning="'$apk_mager_text_1'">
<title>'$apk_mager_text_2'</title>
<param name="FILE" multiple="multiple" option-sh="findfile 9 $PTAD | grep -E '"'\(apks\)|\(apkm\)|\(xapk\)'"' " required="true" />
<set>
IFS=$'"'\n'"'
for v in $FILE; do
    echo "'$more_text_4' $FILE"
    echo
    apkeditor m -f -i "$PTAD/$v" -o "$PTAD/out/$v" 2>&1 | sed -u -e "1,/__/d"
    echo
done
checktime
</set>
</action></group>

<group>
<action icon="'`urlpng restore_sign`'">
<title>'$restore_apk_text_3'</title>
<param name="FILE" title="'$restore_apk_text_1'" value-sh="glog apk_restore_sign" option-sh="findfile 10 $PTAD" required="true"/>
<param name="FILE2" title="'$restore_apk_text_2'" value-sh="glog apk_restore_sign2" option-sh="findfile 10 $PTAD" required="true"/>
<set>
slog apk_restore_sign "$FILE"
slog apk_restore_sign2 "$FILE2"
echo "'$more_text_4' $FILE"
echo
apkeditor d -f -t sig -i "$PTAD/$FILE" -sig "$TMP/signatures_dir" 2>&1
echo
apkeditor b -f -t sig -i "$PTAD/$FILE2" -sig "$TMP/signatures_dir" -o "$PTAD/out/$FILE2" 2>&1
rm -fr "$TMP/signatures_dir"
</set>
</action></group>'
}

Addon() {
    Download() {
        if [ "$(gprop url)" ]; then
            echo '<group>
<action warn="'$use_network_text'" icon="'`urladd icon`'" reload="true">
<title>'$(gprop name)'</title>
<desc>'$(gprop version)' '$(gprop author)$description_text'</desc>
'"$farooot"'
<set>
echo "'$update_text_3'"
echo
taive "'$(gprop url)'" $TMP/addon.add 2>&1
echo
if [ -f $TMP/addon.add ]; then
    installadd $TMP/addon.add "'${dirvad%/*}'"
    rm -fr $TMP/addon.add
else
    echo "Add-on download failed !" >&2
fi
</set>
</action>
</group>'
        fi
    }

    Features() {
        [ "$1" == "status" ] && atextx="$addon_text_10" || atextx="$addon_text_2"
        echo '<group><switch icon="'`urladd icon`'" shell="hidden" warn="'$atextx'">
<title>'$(gprop name)'</title>
<desc>'$(gprop version)' '$(gprop author)$description_text'</desc>
<get>cat '$dirvad'/'$1'</get>
<set>echo "$state" > '$dirvad'/'$1'</set>
</switch>
</group>'
    }

    Homeadd() {
        # Load index
        if [ -f "$dirvad/index.bash" ]; then
            pagesh='config-sh="'$dirvad'/index.bash home"'
        elif [ -f "$dirvad/index.sh" ]; then
            pagesh='config-sh="'$dirvad'/index.sh home"'
        elif [ -f "$dirvad/index.xml" ]; then
            pagesh='config="'$dirvad'/index.xml"'
        else
            pagesh='config="'$ETC'/error.xml"'
        fi

        # Load menu
        if grep -q code_option "$dirvad/menu.bash" 2>/dev/null; then
            code_option="$($dirvad/menu.bash code_option 2>/dev/null)"
            code_shell="$($dirvad/menu.bash code_shell 2>/dev/null)"
        elif grep -q code_option "$dirvad/menu.sh" 2>/dev/null; then
            code_option="$($dirvad/menu.sh code_option 2>/dev/null)"
            code_shell="$($dirvad/menu.sh code_shell 2>/dev/null)"
        fi

        # Load trang
        if [ "$(gprop name)" ]; then
            # Xác nhận có google dịch
            if grep -q "trans_add" "$dirvad/index.sh" 2>/dev/null || grep -q "trans_add" "$dirvad/index.bash" 2>/dev/null; then
                google_trankk='<option type="checkbox" box="glog auto_trans_text_'${dirvad##*/}'" id="v1" auto-off="true" reload="true" silent="true">'$google_translate_text'</option>'
                google_tran_shellkk='elif [ "$menu_id" == "v1" ]; then
[ "$(glog auto_trans_text_'${dirvad##*/}')" == 1 ] && slog auto_trans_text_'${dirvad##*/}' 0 || slog auto_trans_text_'${dirvad##*/}' 1'
            fi

            # phát hiện tính năng
            [ "$(gprop summary)" ] && summss='<summary>'"$(gprop summary)"'</summary>'
            [ "$(gprop shortcut)" == "true" ] && shortcut='id="'${dirvad##*/}'"'

            echo '<group>
<page '$shortcut' icon="'`urladd icon`'" '$pagesh'>
<title>'$(gprop name)'</title>
<desc>'$(gprop version) $(gprop author)$description_text'</desc>
'"$summss"'
'"$farooot"'
'"$google_trankk"'
<option type="default" id="v2" auto-finish="true" silent="true">'$pin_text_add'</option>
'"$code_option"'
<handler>
if [ "$menu_id" == "v2" ]; then
    [ -f "'$dirvad'/pin" ] && rm -f "'$dirvad'/pin" || echo > "'$dirvad'/pin"
    '"$google_tran_shellkk"'
fi
'"$code_shell"'
</handler>
</page>
</group>'
        fi
    }

    Vips() {
        # Xoá giá trị cũ
        code_option=''
        code_shell=''
        description_text=''
        farooot=''
        summss=''
        google_trankk=''
        google_tran_shellkk=''
        shortcut=''
        atextx=''
        index_adds=''
        if [ "$PATHADD" == "$AON" ]; then
            index_adds="$(glog settadd)"
        else
            index_adds="$(glog settadd2)"
        fi

        # getprop
        gprop() {
            cat "$vadd" 2>/dev/null | awk -F= -v k="$1" '$1==k{print $2; exit}'
        }

        # Phát hiện root
        if [ "$(gprop root)" == "true" ]; then
            farooot='<lock>
[ "$ROT" == 0 ] && echo "'$root_warning_text'" || echo 0
</lock>'
        fi

        # Desc ngôn ngữ
        if [ "$(gprop 'description_'$LANGUAGE'_'$COUNTRY'')" ]; then
            [ "$(gprop 'description_'$LANGUAGE'_'$COUNTRY'')" ] && description_text=" | $(gprop 'description_'$LANGUAGE'_'$COUNTRY'')"
        elif [ "$(gprop 'description_'$LANGUAGE'')" ]; then
            [ "$(gprop description_$LANGUAGE)" ] && description_text=" | $(gprop description_$LANGUAGE)"
        else
            [ "$(gprop description)" ] && description_text=" | $(gprop description)"
        fi

        if [ "$(cat $dirvad/delete 2>/dev/null)" == 1 ]; then
            if [ -f "$dirvad/uninstall.sh" ]; then
                $dirvad/uninstall.sh
            elif [ -f "$dirvad/uninstall.bash" ]; then
                $dirvad/uninstall.bash
            fi
            find "$dirvad" -maxdepth 1 ! -path "$dirvad" ! -name 'download.prop' -exec rm -rf {} +
        elif [ "$index_adds" == 1 ]; then
            Features status
        elif [ "$index_adds" == 2 ]; then
            [ -f $dirvad/nodelete ] || Features delete
        else
            if [ "$(cat $dirvad/status 2>/dev/null)" != 1 ]; then
                if [[ -f "$dirvad/index.sh" || -f "$dirvad/index.bash" || -f "$dirvad/index.xml" ]]; then
                    Homeadd
                elif [ -f "$dirvad/download.prop" ]; then
                    Download
                fi
            fi
        fi
    }

    # Load trang add-on có pin trước
    for vadd in $PATHADD/*/addon.prop; do
        [ -f "$vadd" ] || continue
        dirvad="${vadd%/*}"
        pin_text_add="$unpin_text"
        [ -f "$dirvad/pin" ] || continue
        if [[ -f "$dirvad/index.sh" || -f "$dirvad/index.bash" || -f "$dirvad/index.xml" ]]; then
            Vips
        fi
    done

    # Load trang không có pin
    for vadd in $PATHADD/*/addon.prop; do
        [ -f "$vadd" ] || continue
        dirvad="${vadd%/*}"
        pin_text_add="$pin_text"
        [ -f "$dirvad/pin" ] && continue
        if [[ -f "$dirvad/index.sh" || -f "$dirvad/index.bash" || -f "$dirvad/index.xml" ]]; then
            Vips
        fi
    done

    # Load trang tải xuống ở dưới cùng
    for vadd in $PATHADD/*/download.prop; do
        [ -f "$vadd" ] || continue
        dirvad="${vadd%/*}"
        pin_text_add="$pin_text"
        if [[ -f "$dirvad/pin" || -f "$dirvad/index.sh" || -f "$dirvad/index.bash" || -f "$dirvad/index.xml" ]]; then
            continue
        fi
        Vips
    done
}

# index
echo '<?xml version="1.0" encoding="UTF-8" ?>
<group>'
"$@"
echo '</group>'