# Kakathic

MPAT="${0%/*}"

if checkonline; then
echo "Start checking for updates: Patch ROM"
echo
check_sum_onl="$(xem https://api.github.com/repos/Zenlua/Tool-Tree/releases/tags/V1 | jq -r '.assets[] | select(.name == "patch_rom.add") | .digest' | cut -d: -f2)"
if [[ "$check_sum_onl" != "$(glog check_sum_addon_patch_rom)" ]]; then
installadd "$(gprop url $MPAT/download.prop)" "${MPAT%/*}" 2>&1
echo
[ -f $MPAT/changelog.txt ] && cat $MPAT/changelog.txt
else
echo "You are using the latest version."
fi
else
echo "Network error"
fi
