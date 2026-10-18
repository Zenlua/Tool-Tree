# Add-on
id=upload_file
name="Upload file online"
author=Kakathic
description="Upload the file to the web gofile, or pixeldrain if you have an API key"
version=1.0
versionCode=100
root=false
transai=true

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    description="Tải tập tin lên web gofile, hoặc pixeldrain nếu có api key"
    ;;
  hu*)
    description="Töltsd fel a fájlt a webes gofile-re, vagy pixeldrainre, ha van API kulcsod"
    ;;
  es*)
    description="Sube el archivo al gofile web o pixeldrain si tienes una clave API"
    ;;
  *)
    ;;
esac
