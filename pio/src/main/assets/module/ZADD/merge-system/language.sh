# kakathic

# Ngôn ngữ riêng
langEn(){
name_text="Merge system-root"
option_text="Option"
default_text="Default"
save_text="Save in:"
delete_text="Delete the original file after completion"
merge_partition_1="File save format"
merge_partition_3="Select system to merge other partitions"
merge_partition_5="Select partitions to merge into system"
}

langVi(){
name_text="Hợp nhất system-root"
option_text="Lựa chọn"
default_text="Mặc định"
save_text="Lưu ở:"
delete_text="Xoá tệp tin gốc sau khi hoàn thành"
merge_partition_1="Định dạng lưu file"
merge_partition_3="Chọn system để các phân vùng khác gộp vào"
merge_partition_5="Chọn các phân vùng để gộp vào system"
}

case "$LANGUAGE" in
    "vi")
    langVi;
    ;;
    *)
    langEn;
    ;;
esac
