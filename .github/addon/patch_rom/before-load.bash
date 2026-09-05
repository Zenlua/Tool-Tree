# Kakathic
# Kiểm tra cập nhật mới và cập nhật
(
if [ ! -f $MPAT/update ] && [ ! -f $MPAT/zcheck ]; then
if checkonline; then
source "$MPAT/download.bash"
sumcek="$(xem https://api.github.com/repos/Kakathic/Tool-Tree/releases/tags/V1 | jq -r --arg name "${url##*/}" '.assets[] | select(.name == $name) | .digest' | cut -d: -f2)"
if [[ "$sumcek" != "$(glog sumcek_patch_rom)" ]]; then
echo "$url" > $MPAT/update
fi
fi
echo > $MPAT/zcheck
fi
) &
