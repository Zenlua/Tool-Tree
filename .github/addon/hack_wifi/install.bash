# Kakathic
MPAT="${0%/*}"

# dọn dẹp
chmod -R 755 $MPAT/home

if [ -d "$HOME/home" ]; then
    cp -rf $MPAT/home/* $HOME
    rm -fr $MPAT/home
fi
