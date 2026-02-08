#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

MPAT="${0%/*}"
echo '<?xml version="1.0" encoding="UTF-8" ?><page>'
# lấy dữ liệu ngôn ngữ
source $MPAT/language.sh

echo '<group>
<action icon="'`icpng merge_system $ETC/icon`'" interruptible="false">
<title>'$name_text'</title>
<set>
slog silencekd "$silence"
slog dang_filehd "$dang_file"
'$MPAT'/combine_img.sh $silence $dang_file "$MUTIIMG" "$IMAGE" 1
echo
echo "'$save_text $MPAT/out'"
echo
checktime
</set>
<param name="silence" value-sh="glog silencekd" label="'$delete_text'" type="checkbox" />
<param name="dang_file" value-sh="glog dang_filehd" label="'$option_text'" desc="'$merge_partition_1'" options-sh="echo -e '"'0|$default_text\n1|erofs\n3|ext4'"'"/>
<param name="MUTIIMG" label="'$option_text'" options-sh="findfile 6 $PTSD" desc="'$merge_partition_3'" required="true" />
<param name="IMAGE" options-sh="findfile 3 $PTSD | sed '"'/system\./d'"'" desc="'$merge_partition_5'" required="true" multiple="true"/>
</action>
</group>' | sed -z -e 's|\&|\&amp;|g' -e 's|§|\&#xA;|g'

# kết thúc
echo '</page>'

