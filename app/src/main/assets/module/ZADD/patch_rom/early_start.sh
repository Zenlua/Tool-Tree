# Kakathic
MPAT="${0%/*}"

# dọn dẹp
[ -d "$MPAT/apk" ] && rm -fr "$MPAT/apk"

# extract
if [ -f $MPAT/mod.7z ];then
echo "7z mod extract..."
7z x -t7z -y $MPAT/mod.7z -o$MPAT
rm -f $MPAT/mod.7z
fi
