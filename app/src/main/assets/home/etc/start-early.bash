#!/data/data/com.tool.tree/files/home/bin/bash
source language 2>/dev/null
unset shell_progres

# Dọn dẹp tmp
[ -d "$TMPDIR" ] && rm -fr $TMPDIR/* &
rm -fr $HOME/cache/* $START_DIR/cache/* $TEMP/documents $TEMP/WebView &

(
if checkonline; then
    # Tải về nhật ký
    echo -e "Download the log and the latest version..."
    timeout 20 taive 'https://raw.githubusercontent.com/Zenlua/Tool-Tree/refs/heads/main/Version.md' $TEMP/Version.md
    if [ -f $TEMP/Version.md ]; then
    sed -e 's|\*\*||g' -e 's|+|•|g' $TEMP/Version.md | awk 'BEGIN{RS="Version:"} NR>=2 && NR<=6 {printf "Version:%s", $0}' | trans -b "$LANGUAGE-$COUNTRY" > $TEMP/version.txt
    fi

    # Thông báo cập nhật
    if [ "$(unzip -qp "$PATH_APK" assets/beta 2>/dev/null)" == 1 ]; then
        websums="$(xem https://api.github.com/repos/Zenlua/Tool-Tree/releases/tags/beta)"
        tagname="${PACKAGE_VERSION_NAME//./}"
    else
        websums="$(xem https://api.github.com/repos/Zenlua/Tool-Tree/releases/latest)"
        tagname="$(echo "$websums" | jq -r .tag_name | sed -e 's|\.||g' -e 's|V||')"
    fi
    websum="$(echo "$websums" | jq -r .assets[0].digest | cut -d: -f2)"
    filesum="$(checksum "$PATH_APK")"
    echo "Tag: $tagname"
    echo "Sum online: $websum"
    echo "Sum apk: $filesum"
    if [[ ${PACKAGE_VERSION_NAME//./} -gt $tagname ]]; then
        [ -f $TEMP/update ] && rm -f $TEMP/update
    elif [[ ${PACKAGE_VERSION_NAME//./} == $tagname ]]; then
        if [[ "$websum" != "$filesum" ]] && [[ -n $websum ]]; then
            if [ ! -f $TEMP/update ] && [ -n "$tagname" ]; then
            echo "$tagname" > $TEMP/update
            fi
        else
            [ -f $TEMP/update ] && rm -f $TEMP/update
        fi
    else
        if [ ! -f $TEMP/update ] && [ -n "$tagname" ]; then
        echo "$tagname" > $TEMP/update
        fi
    fi
    [ -f $TEMP/update ] && showtoast "$update_text_6"
fi
) &

(
# Cấp quyền tự động nếu đã root
if [ "$ROT" == 1 ]; then
    chown -R 0:0 $HOME/.cache
    # Tạo link home
    [ -e /data/local/TOOL ] || ln -sf $APK /data/local/TOOL
    [ -e /data/local/TREE ] || ln -sf $SDH /data/local/TREE
    # Thêm không giới hạn tiết kiệm pin
    dumpsys deviceidle whitelist +$PACKAGE_NAME &>/dev/null
    am set-inactive --user 0 $PACKAGE_NAME false &>/dev/null
    am set-standby-bucket $PACKAGE_NAME active &>/dev/null
    am set-bg-restriction-level --user 0 $PACKAGE_NAME unrestricted &>/dev/null
    am unfreeze --sticky $PACKAGE_NAME &>/dev/null
    cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME RUN_ANY_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME WAKE_LOCK allow
    # Cấp quyền ở miui, hyper
    # Danh sách các ứng dụng đã cài đặt
    cmd appops set $PACKAGE_NAME 10022 allow
    cmd appops set $PACKAGE_NAME GET_USAGE_STATS allow
    [ "$API" -ge 30 ] && cmd appops set $PACKAGE_NAME QUERY_ALL_PACKAGES allow
    # Phím tắt màn hình chính
    cmd appops set $PACKAGE_NAME 10017 allow
    # Loaded sẵn danh sách img
    search_image &>/dev/null
fi
# Khởi động các file shell ở add-on
set_permis $AON/*/* $AOK/*/* &>/dev/null
for vadd in $AON/* $AOK/*; do
    if [ -f "$vadd/early_start.bash" ]; then
    (
    echo "Run shell: $vadd/early_start.bash"
    $vadd/early_start.bash
    ) &
    elif [ -f "$vadd/early_start.sh" ]; then
    (
    echo "Run shell: $vadd/early_start.sh"
    $vadd/early_start.sh
    ) &
    fi
done
) &
