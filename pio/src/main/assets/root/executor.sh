#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

export EXECUTOR_PATH="$({EXECUTOR_PATH})"
export START_DIR="$({START_DIR})"
export TEMP="$({TEMP_DIR})"
export ANDROID_UID="$({ANDROID_UID})"
export API="$({ANDROID_SDK})"
export SDCARD_PATH="$({SDCARD_PATH})"
export PACKAGE_NAME="$({PACKAGE_NAME})"
export PACKAGE_VERSION_NAME="$({PACKAGE_VERSION_NAME})"
export PACKAGE_VERSION_CODE="$({PACKAGE_VERSION_CODE})"
export APP_USER_ID="$({APP_USER_ID})"
export PATH_APK="$({PATH_APK})"
export DARK_MODE="$({DARK_MODE})"
export KERNEL_VERSION="$({KERNEL_VERSION})"
export TOTAL_MEMORY="$({TOTAL_MEMORY})"
export CPU_ABI="$({CPU_ABI})"
export LANGUAGE="$({LANGUAGE})"
export COUNTRY="$({COUNTRY})"
export TIMEZONE="$({TIMEZONE})"
export ANDROID_DEVICE="$({ANDROID_DEVICE})"
export ANDROID_BRAND="$({ANDROID_BRAND})"
export ANDROID_MANUFACTURER="$({ANDROID_MANUFACTURER})"
export ANDROID_FINGERPRINT="$({ANDROID_FINGERPRINT})"
export ANDROID_RELEASE="$({ANDROID_RELEASE})"
export ANDROID_MODEL="$({ANDROID_MODEL})"
export ANDROID_ID="$({ANDROID_ID})"
export ROOT=$({ROOT_PERMISSION})
export START_TIME="$(date +%s)"
export HOME="$({TOOLKIT})"
export TERMUX="$HOME/termux"
export ETC="$HOME/etc"
export BIN="$HOME/bin"
export LOG="$HOME/log"
export SDH="$HOME/TREE"
export APK="$HOME/TOOL"
export AON="$HOME/ZADD"
export AOK="$HOME/ZAPK"
export TMPDIR="$HOME/tmp"
export LIB="$HOME/lib"
export TMP="$TMPDIR"
export SDC="$SDCARD_PATH/TREE"
export JAVA_HOME="$TERMUX"
export PYTHONHOME="$TERMUX"
export PIP_ROOT_USER_ACTION=ignore
# export LD_LIBRARY_PATH="$LIB"

if [ ! -f $LOG/PATH ]; then
export PATH="$BIN:$TERMUX/bin:$TERMUX/py:$PATH"
else
source $LOG/PATH
fi

export PTSD="$(glog PTSD $SDC/ROM 2>/dev/null)"; # $PTSD
export PTSH="$(glog PTSH ROM 2>/dev/null)"; # $SDH/$PTSH
export PTAD="$(glog PTAD $SDC/APK 2>/dev/null)"; # $PTAD
export PTAH="$(glog PTAH APK 2>/dev/null)"; # $APK/$PTAH

export WEBS="User-Agent: Mozilla/5.0 (Linux; Android $ANDROID_RELEASE; $ANDROID_MANUFACTURER $ANDROID_MODEL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

if [ "$ROOT" == 'true' ];then
export ROT=1
else
export ROT=0
export xu=xu
fi

if [ "$CPU_ABI" != 'arm64-v8a' ];then
text_error="Only arm64-v8a devices supported"
showtoast --am "$text_error"
echo "$text_error" >&2
sleep 10
exit 1
else
export ARCH=arm64
fi

if [ -f "$1" ]; then
chmod 755 "$1" 2>/dev/null
export shell_progres="$2";
cd "$HOME";
source "$1";
rm -f "$1";
else
    if [ "$1" ]; then
    echo "Error file" >&2
    sleep 5
    fi
fi
