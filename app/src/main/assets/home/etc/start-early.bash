#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

unset shell_progres
# Dọn dẹp tmp
find "$TMPDIR" -maxdepth 1 ! -path "$TMPDIR" ! -name '*.log' -exec rm -rf {} + &
rm -fr $TEMP/documents $TEMP/WebView &

{
if checkonline; then
    # Tải về nhật ký
    echo -e "Download the log and the latest version..."
    timeout 20 taive -s 'https://raw.githubusercontent.com/Zenlua/Tool-Tree/refs/heads/main/Version.md' $TEMP/Version.md

    if [[ -f $TEMP/Version.md ]] && [[ "$(glog sum_ver_boot)" != "$(checksum $TEMP/Version.md)" ]]; then
        sed -e 's|\*\*||g' -e 's|+|•|g' $TEMP/Version.md | awk 'BEGIN{RS="Version:"} NR>=2 && NR<=7 {printf "Version:%s", $0}' > $TEMP/version.txt
        cat $TEMP/version.txt | trans -b "$LANGUAGE-$COUNTRY" > $TEMP/version_trans.txt
        slog sum_ver_boot "$(checksum $TEMP/Version.md)"
    fi
fi
} &

check_update boot &

{
# Nếu được cấp quyền rish
rish -c "
    [ -e /data/local/TOOL ] || ln -sf '$APK' /data/local/TOOL
    [ -e /data/local/TREE ] || ln -sf '$SDH' /data/local/TREE
    dumpsys deviceidle whitelist +$PACKAGE_NAME &>/dev/null
    am set-inactive --user 0 $PACKAGE_NAME false &>/dev/null
    am set-standby-bucket $PACKAGE_NAME active &>/dev/null
    am set-bg-restriction-level --user 0 $PACKAGE_NAME unrestricted
    am unfreeze --sticky $PACKAGE_NAME &>/dev/null
    cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME RUN_ANY_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME WAKE_LOCK allow
    cmd appops set $PACKAGE_NAME 10022 allow
    cmd appops set $PACKAGE_NAME GET_USAGE_STATS allow
    [ "$API" -ge 30 ] && cmd appops set $PACKAGE_NAME QUERY_ALL_PACKAGES allow
    cmd appops set $PACKAGE_NAME 10017 allow
    search_image &>/dev/null
"
} &

{
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
    am set-bg-restriction-level --user 0 $PACKAGE_NAME unrestricted
    am unfreeze --sticky $PACKAGE_NAME &>/dev/null
    cmd appops set $PACKAGE_NAME RUN_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME RUN_ANY_IN_BACKGROUND allow
    cmd appops set $PACKAGE_NAME WAKE_LOCK allow

    # Cấp quyền ở MIUI, HyperOS
    cmd appops set $PACKAGE_NAME 10022 allow
    cmd appops set $PACKAGE_NAME GET_USAGE_STATS allow
    [ "$API" -ge 30 ] && cmd appops set $PACKAGE_NAME QUERY_ALL_PACKAGES allow

    # Phím tắt màn hình chính
    cmd appops set $PACKAGE_NAME 10017 allow

    # Loaded sẵn danh sách img
    search_image &>/dev/null
fi
} &

{
# Dọn bộ đếm
rm -fr $AON/*/check $AOK/*/check
# Khởi động các file shell ở add-on
set_permis $AON/*/* $AOK/*/* &>/dev/null
for vadd in $AON/* $AOK/*; do
    if [ -f "$vadd/early_start.bash" ]; then
        echo "Run shell: $vadd/early_start.bash"
        $vadd/early_start.bash &
    elif [ -f "$vadd/early_start.sh" ]; then
        echo "Run shell: $vadd/early_start.sh"
        $vadd/early_start.sh &
    fi
done
} &
