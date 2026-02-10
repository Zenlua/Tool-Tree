#!/data/data/com.tool.tree/files/home/bin/bash
[[ -z "$1" ]] && exit 1
MPAT="${0%/*}"

# Run code
for v in $3 $4; do
hcdgv=$(($hcdgv + 2))

progress $hcdgv/12
unpack_img -i "$PTSD/$v" -o "$MPAT/tmp" -n 0 -d $1
tm="$(echo "$v" | cut -d "/" -f2 | cut -d "." -f1 \
| sed -e 's|.img||' -e 's|.new.img||')"
    if [ "$tm" != "system" ];then
    fc="$MPAT/tmp/config/${tm}_file_contexts"
    fcc="$MPAT/tmp/config/${tm}_fs_config"
    sfc="$MPAT/tmp/config/system_file_contexts"
    sfcc="$MPAT/tmp/config/system_fs_config"
    cat "$fc" | sed -e "/\/ u:/d" -e "/\/$tm(\/.*)? u:/d" \
    -e "s|/$tm/|/system/$tm/|" >> "$sfc"
    cat "$fcc" | sed -e "/\/ 0 0 0755/d" -e "/$tm\/ 0 0 0755/d" \
    -e "s|$tm/|system/$tm/|" >> "$sfcc"
    rm -fr $MPAT/tmp/system/$tm
    mv -f $MPAT/tmp/$tm $MPAT/tmp/system
    fi
done

progress "-1/0"
if [ "$5" == 1 ];then
repack_img -i "$MPAT/tmp/system" -o "$MPAT/out" -k $dang_file -d $1
rm -fr "$MPAT/tmp"
fi
