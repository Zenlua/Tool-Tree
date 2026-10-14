# Kakathic

chmod -R 755 $1/home

if [ -d "$1/home" ]; then
    cp -rf $1/home/* $1
    rm -fr $1/home
fi
