# Add-on
id=view_source
name="View source code"
author=Kakathic
description="Decompile apk using tools to view java source code"
version=1.0
versionCode=100
root=false
transai=true

case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Xem mã nguồn"
    description="Dịch ngược apk bằng tool để xem mã nguồn java"
    ;;
  *)
    ;;
esac
