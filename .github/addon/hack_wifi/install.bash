# Kakathic
MPAT="${0%/*}"

# dọn dẹp
chmod 755 $MPAT/home
[ -d "$HOME/libnl" ] || cp -rf $MPAT/home/* $HOME
