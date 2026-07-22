
if [ "$LANGUAGE-$COUNTRY" == "vi-VN" ]; then
fs_text_1="Chạy fstrim để dọn dẹp các khối bộ nhớ"
fs_text_2="Dọn dẹp khối"
fs_text_3="Yêu cầu quyền Root"
fs_text_4="Dọn dẹp các khối"

ufs_text_1="Tình trạng tuổi thọ (EOL):"
ufs_text_2="Không xác định"
ufs_text_3="Mức độ hao mòn:"
ufs_text_4="Đã vượt quá tuổi thọ"
ufs_text_5="Cảnh báo"
ufs_text_6="Bình thường"

check_ufs_text="Kiểm tra UFS"
check_ufs_text_1="Đang đo tốc độ đọc..."
check_ufs_text_2="Đang đo tốc độ ghi..."
elif [ "$LANGUAGE-$COUNTRY" == "hu-HU" ]; then
fs_text_1="Futtassa az fstrim parancsot a memóriablokkok tisztításához"
fs_text_2="Blokk törlése"
fs_text_3="Root-hozzáférést igényel"
fs_text_4="Távolítsd el a blokkokat"

ufs_text_1="Várható élettartam szerinti státusz (EOL):"
ufs_text_2="Meghatározatlan"
ufs_text_3="Elhasználódás mértéke:"
ufs_text_4="Az élettartam lejárt"
ufs_text_5="Figyelmeztetés"
ufs_text_6="Normál"

check_ufs_text="UFS-ellenőrzés"
check_ufs_text_1="Olvasási sebesség mérése..."
check_ufs_text_2="Rögzítési sebesség mérése..."
fi
