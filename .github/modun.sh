# Kakathic
cd app/src/main/assets/module

for vmk in $(find etc/*.jar .local/share/apktool/framework/*.apk -type f); do
mkdir -p "${vmk%.*}®${vmk##*.}_7zv2"
7z x "$vmk" -o"${vmk%.*}®${vmk##*.}_7zv2" -y >/dev/null
rm -fr "$vmk"
done

for vnk in $(find etc/apkeditor®jar_7zv2/frameworks/android/*apk etc/apktool®jar_7zv2/prebuilt/*.jar -type f); do
mkdir -p "${vnk%.*}®${vnk##*.}_7zv1"
7z x "$vnk" -o"${vnk%.*}®${vnk##*.}_7zv1" -y >/dev/null
rm -fr "$vnk"
done

mkdir -p lib log root tmp TREE/ROM TOOL/APK usr

7z a -t7z -snh -y ../module.7z
rm -fr ../module
mv ../module.7z ../module
ls -lh ../module
