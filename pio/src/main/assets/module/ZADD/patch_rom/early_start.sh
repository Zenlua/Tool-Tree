# Kakathic
MPAT="${0%/*}"

# extract
if [ -f $MPAT/mod.7z ];then
echo "7z mod extract..."
7z x -t7z -y $MPAT/mod.7z -o$MPAT
rm -f $MPAT/mod.7z
fi

# check update add-on
if [ ! -f $MPAT/update ];then
echo "Check for the latest patch ROM version..."
number_ver="$(xem https://raw.githubusercontent.com/Zenlua/Tool-Tree/refs/heads/main/pio/src/main/assets/module/ZADD/patch_rom/addon.prop 2>/dev/null | grep -m1 "versionCode=" | cut -d= -f2)"
number_ver2="$(gprop versionCode "$MPAT/addon.prop")"
    if [[ ${number_ver:-0} -gt $number_ver2 ]];then
    echo 1 >$MPAT/update
    fi
fi
