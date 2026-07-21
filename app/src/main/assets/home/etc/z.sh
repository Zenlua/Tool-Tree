
for vv in $(grep '="' lang/vi.bash | cut -d= -f1); do
if ! grep -q $vv tool-tree.bash; then
    if ! grep -q $vv /data/user/0/com.termux/files/home/Tool-Tree/app/src/main/assets/home/bin/*; then
    echo "$vv"
    fi
fi
done
