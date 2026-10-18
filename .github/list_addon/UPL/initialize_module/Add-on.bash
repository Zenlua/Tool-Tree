# Add-on

id=initialize_module
name="Initialize module"
author=Kakathic
description="Create a Module and apply the imported files without having to flash through magisk, ksu, etc"
version=1.0
versionCode=100
root=true
process=true
transai=true

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Khởi tạo mô-đun"
    description="Tạo một Mô-đun và áp dụng các tệp tin đã nhập mà không cần phải flash thông qua magisk, ksu, v.v"
    ;;
  hu*)
    description="Hozz létre egy modult, és alkalmazd az importált fájlokat anélkül, hogy magiskon, ksu-n keresztül kellene villognodnod..."
    ;;
  es*)
    description="Crea un módulo y aplica los archivos importados sin tener que flashear por magisk, ksu..."
    ;;
  *)
    ;;
esac
