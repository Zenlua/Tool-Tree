#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic
set -o pipefail

fixapps(){
for vv in $@; do
[ ! -e "$vv" ] && { about "Item not found: $vv"; continue; }
tmpl="${vv##*/}"; oi="$MPAT/apk/${tmpl%.*}"
apkeditor_d -i "$vv" -o "${oi%/*}" -t raw
echo
# patch method
if [ "${vv##*/}" == "MIUIGallery.apk" ];then
    if [ "$fix_mapcn" == 1 ];then
    patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"
    Thayvc 0 'method .*. checkMapAvailable()Z' $oi/smali/classes*/com/miui/gallery/map/utils/MapInitializerImpl.smali
    Thaythe 'Lcom/miui/gallery/util/BuildUtil;->isGlobal()Z' 'Lcom/xBuild;->isOne()Z' $oi/smali/classes*/com/miui/gallery/ui/featured/type/ItemTypeSortManager.smali
    fi
elif [ "${vv##*/}" == "Joyose.apk" ];then
    if [ "$fix_joyose" == 1 ];then
    patgpu="$(Timkiem GPUTUNER_SWITCH $oi/smali/classes)"
    sed -i "`grep -nA2 GPUTUNER_SWITCH $patgpu | grep -m1 getString | cut -d- -f1`i\ const/4 v0, 0x1 \n return v0" $patgpu || about "Error: GPUTUNER_SWITCH"
    sed -i "`grep -nA2 SUPPORT_UGD $patgpu | grep -m1 getString | cut -d- -f1`i\ const/4 v0, 0x1 \n return v0" $patgpu || about "Error: SUPPORT_UGD"
    Thayvc -v 'method public run()V' $(Timkiem "job exist, sync local" $oi/smali)
    fi
elif [ "${vv##*/}" == "MIUIWeather.apk" ];then
    if [ "$fix_thoit" == 1 ];then
    patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"
    Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' "$oi/smali/classes*/com/miui/weather2/view/onOnePage/DailyForecastTable.smali
    $oi/smali/classes*/com/miui/weather2/structures/AQIData.smali
    $oi/smali/classes*/com/miui/weather2/view/onOnePage/RealTimeDetailCard.smali
    $oi/smali/classes*/com/miui/weather2/view/WeatherScrollView.smali
    $(Timkiem 'https://weatherapi.market.xiaomi.com/' $oi/smali)"
    fi
elif [[ "${vv##*/}" == *ThemeManager* ]];then
    if [ "$fix_themes" == 1 ];then
    IFS=$'\n'
    Tmtong="$(Timkiem '$onlineThemeDetail' $oi/smali)"
    if [ "$Tmtong" ]; then
    for vqq1 in $(grep 'Lcom/android/thememanager/detail/theme/model/OnlineResourceDetail;->bought:Z' $Tmtong); do
    luv="$(echo "$vqq1" | awk '{print $2}')"
    Thaythe "$vqq1" "$vqq1 \n const/4 $luv 0x1" $Tmtong
    done
    fi
    Tmtong2="$(Timkiem 'OnlineResourceDetailPresenter_code_changed' $oi/smali)"
    if [ "$Tmtong2" ]; then
    for vv2 in $(grep 'Lcom/android/thememanager/detail/theme/model/OnlineResourceDetail;->bought:Z' $Tmtong2); do
    luv2="$(echo "$vv2" | awk '{print $2}')"
    Thaythe "$vv2" "$vv2 \n const/4 $luv2 0x1" $Tmtong2
    done
    fi
    Tmtong3="$(Timkiem 'miui.systemui.plugin.theme_adapter_os_version' $oi/smali)"
    if [ "$Tmtong3" ]; then
    for vv3 in $(grep 'Lcom/android/thememanager/activity/ai/.*.:Lcom/android/thememanager/activity/ai/.*.;' $Tmtong3); do
    luv3="$(echo "$vv3" | awk '{print $2}')"
    Thaythe "$vv3" "const/4 $luv3 0x1 \n return ${luv3/,/} \n $vv3" $Tmtong3
    done
    fi
    pathanhk="$(Timkiem 'Apply Failed for rightPath is null or rightPath file not exist' $oi/smali)"
    if [ "$pathanhk" ]; then
    for vv4 in $(grep -B 2 'const-string .*., "Apply Failed for rightPath is null or rightPath file not exist"' $pathanhk | grep ':' | awk '{print $1}'); do
    sed -i "/$vv4/d" $pathanhk || about "Error: pathanhk"
    done
    fi
    Thayvc 0 '.method .*. isVideoAd()Z' $oi/smali/classes*/com/android/thememanager/basemodule/ad/model/AdInfo.smali
    Thayvc 0 '.method .*. isAdValid(Lcom/android/thememanager/basemodule/ad/model/AdInfo;)Z' $oi/smali/classes*/com/android/thememanager/basemodule/ad/model/AdInfoResponse.smali
    Thayvc 0 '.method .*. isAuthorizedResource()Z' $oi/smali/classes*/com/android/thememanager/basemodule/resource/model/Resource.smali
    Thayvc 0 '.method .*. themeManagerSupportPaidWidget(Landroid/content/Context;)Z' $oi/smali/classes*/com/miui/maml/widget/edit/MamlutilKt.smali
    Thaythe '>DRM_ERROR_UNKNOWN' '>DRM_SUCCESS' "$(Timkiem DRM_ERROR_UNKNOWN $oi/smali)"
    fi
elif [[ "${vv##*/}" == *PersonalAssistant* ]];then
    if [ "$fix_appvault" == 1 ];then
    Thayvc 0 '.method .*. themeManagerSupportPaidWidget(Landroid' $oi/smali/classes*/com/miui/maml/widget/edit/MamlutilKt.smali
    Thayvc 0 '.method .*. isPay()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponse.smali
    Thayvc 1 '.method .*. isBought()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponse.smali
    Thayvc 0 '.method .*. isPay()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponseWrapper.smali
    Thayvc 1 '.method .*. isBought()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponseWrapper.smali
    Thayvc 1 '.method .*. isCanDownload(Lcom' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/utils/PickerDetailDownloadManager\$Companion.smali
    Thayvc 1 '.method .*. isCanAutoDownloadMaMl()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/utils/PickerDetailUtil.smali
    Thayvc 0 '.method .*. isTargetPositionMamlPayAndDownloading(I)Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/PickerDetailViewModel.smali
    Thaythe 'MM:dd' 'dd:MM' "$(Timkiem '"MM:dd"' $oi/smali)"
    fi
fi
# End patch smali
echo
apkeditor_b -i "$oi" -o "${vv%/*}" -d 1 -x false
# di chuyển vào product app
if [ "$(echo "$vv" | grep -cm1 "/data-app/")" == 1 ];then
pproduct="$(ls -1d $SDH/$PTSH/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
[ -z "$pproduct" ] && pproduct="$(ls -1d $SDH/$PTSH/*/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
mv "${vv%/*}" "$pproduct/app"
fi
echo
done
}

fixmultiple(){
for vv in $@; do
[ ! -e "$vv" ] && { about "Item not found: $vv"; continue; }
tmpl="${vv##*/}"; oi="$MPAT/apk/${tmpl%.*}"
apkeditor_d -i "$vv" -o "${oi%/*}" -t raw
echo
# patch method
if [ "${vv##*/}" == "miui-services.jar" ];then
    [ "$fix_screen" == 1 ] && Thayvc 0 '.method .*. notAllowCaptureDisplay(Lcom' $oi/smali/classes*/com/android/server/wm/WindowManagerServiceImpl.smali
    [ "$fix_window" == 1 ] && Thayvc 6 '.method .*. getMaxMiuiFreeFormStackCount(Ljava' $oi/smali/classes*/com/android/server/wm/MiuiFreeFormStackDisplayStrategy.smali
elif [ "${vv##*/}" == "miui-framework.jar" ];then
    [ "$fix_reset_theme" == 1 ] && Thayvc -v '.method .*. validateTheme(Landroid' $oi/smali/classes/miui/drm/ThemeReceiver.smali
elif [ "${vv##*/}" == "services.jar" ];then
    if [ "$fix_screen" == 1 ]; then
    Thayvc 0 '.method isSecureLocked()Z' $oi/smali/classes*/com/android/server/wm/WindowState.smali
    Thayvc 1 '.method .*. isScreenCaptureAllowed(I)Z' $oi/smali/classes*/com/android/server/devicepolicy/DevicePolicyCacheImpl.smali
    Thayvc 1 '.method .*. getScreenCaptureDisabled(Landroid/content/ComponentName;IZ)Z' $oi/smali/classes*/com/android/server/devicepolicy/DevicePolicyManagerService.smali
    Thayvc -v .'method .*. setScreenCaptureDisabled(Landroid/content/ComponentName;Ljava/lang/String;ZZ)V' $oi/smali/classes*/com/android/server/devicepolicy/DevicePolicyManagerService.smali
    fi
    [ "$fix_show_error" == 1 ] && Thayvc -v '.method .*. showSystemReadyErrorDialogsIfNeeded()V' $oi/smali/classes*/com/android/server/wm/ActivityTaskManagerService\$LocalService.smali
elif [ "${vv##*/}" == "PowerKeeper.apk" ];then
    if [ "$fix_fps" == 1 ];then
    Thayvc 0 '.method .*. getDisplayCtrlCode()I' $oi/smali/classes*/com/miui/powerkeeper/feedbackcontrol/ThermalManager.smali
    Thayvc -v '.method .*. displayControl(I)V' $oi/smali/classes*/com/miui/powerkeeper/feedbackcontrol/ThermalManager.smali
    Thayvc -v '.method .*. setScreenEffect(II)V' $oi/smali/classes*/com/miui/powerkeeper/statemachine/DisplayFrameSetting.smali
    Thayvc -v '.method .*. setScreenEffectInternal(IILjava/lang/String;)V' $oi/smali/classes*/com/miui/powerkeeper/statemachine/DisplayFrameSetting.smali
    Thayvc -v '.method .*. setScreenEffect(Ljava/lang/String;II)V' $oi/smali/classes*/com/miui/powerkeeper/statemachine/DisplayFrameSetting.smali
    fi
fi
# End patch smali
echo
apkeditor_b -i "$oi" -o "${vv%/*}" -d 1
echo
done
}

fixkey(){
for vv in $@; do
[ ! -e "$vv" ] && { about "Item not found: $vv"; continue; }
tmpl="${vv##*/}"; oi="$MPAT/apk/${tmpl%.*}"
[[ "${vv##*/}" == *FrequentPhrase* ]] && tres=reso || tres=raw
apkeditor_d -i "$vv" -o "${oi%/*}" -t $tres
echo
# Patch smali
psystem="$(ls -1d $SDH/$PTSH/*/system/build.prop 2>/dev/null | grep -m1 'system' | sed 's|\/build.prop||')"
pproduct="$(ls -1d $SDH/$PTSH/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
[ -z "$pproduct" ] && pproduct="$(ls -1d $SDH/$PTSH/*/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
# patch method
if [ "${vv##*/}" == "miui-services.jar" ];then
    Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali" | grep -i "Input")"
elif [[ "${vv##*/}" == *FrequentPhrase* ]]; then
    Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali")"
    Thaythe "</resources>" '<color name="input_bottom_background_color">'"$(glog ime_color_dark)"'</color>
    </resources>' "$oi/resources/package_1/res/values-night/colors.xml"
    Thaythe "</resources>" '<color name="input_bottom_background_color">'"$(glog ime_color)"'</color>
    </resources>' "$oi/resources/package_1/res/values/colors.xml"
    Thaythe "</resources>" ''"$(glog ime_dimen)"'
    </resources>' "$oi/resources/package_1/res/values/dimens.xml"
elif [ "${vv##*/}" == "MiuiSystemUI.apk" ];then
    if [ "$(gprop ro.miui.support_miui_ime_bottom "$psystem/build.prop")" != 1 ];then
    patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"
    Thaythe 'Lmiuix/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isFalse:Z' $oi/smali/classes*/com/android/systemui/navigationbar/NavigationBar.smali
    fi
elif [ "${vv##*/}" == "miui-framework.jar" ];then
    Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali")"
    if [ "$(gprop ro.miui.support_miui_ime_bottom "$psystem/build.prop")" != 1 ];then
    sprop "ro.miui.support_miui_ime_bottom" 1 "$psystem/build.prop"
    cp -rf "$MPAT/mod/GestureLineOverlay.apk" "$pproduct/overlay"
    fi
elif [ "${vv##*/}" == "Settings.apk" ];then
    Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali" | sed '/MecBoardInputController/d')"
    Thayvc 1 '.method .*. isMiuiImeBottomSupport()Z' $oi/smali/classes*/com/android/settings/inputmethod
fi
# End patch smali
echo
apkeditor_b -i "$oi" -o "${vv%/*}" -d 1
echo
done
}

fixnoti(){
for vv in $@; do
[ ! -e "$vv" ] && { about "Item not found: $vv"; continue; }
tmpl="${vv##*/}"; oi="$MPAT/apk/${tmpl%.*}"
[ "${vv##*/}" == "Settings.apk" ] && tres=reso || tres=raw
apkeditor_d -i "$vv" -o "${oi%/*}" -t $tres
echo
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"
# patch method
if [ "${vv##*/}" == "miui-services.jar" ];then
    [ "$fix_noti" == 1 ] && Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' "$oi/smali/classes*/com/android/server/notification/NotificationManagerServiceImpl.smali
    $oi/smali/classes*/com/android/server/am/ProcessSceneCleaner.smali
    $oi/smali/classes*/com/android/server/am/ProcessManagerService.smali
    $oi/smali/classes*/com/android/server/am/BroadcastQueueModernStubImpl.smali"
    if [ "$fix_global" == 1 ];then
    Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' "$oi/smali/classes/com/android/server/ForceDarkAppListManager.smali"
    fi
elif [ "${vv##*/}" == "PowerKeeper.apk" ];then
    [ "$fix_noti" == 1 ] && Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' "$oi/smali/classes*/com/miui/powerkeeper/statemachine/PhoneSleepModeController\$SleepHandler.smali
    $oi/smali/classes*/com/miui/powerkeeper/statemachine/PhoneSleepModeController.smali
    $oi/smali/classes*/com/miui/powerkeeper/statemachine/PadSleepModeController\$SleepHandler.smali
    $oi/smali/classes*/com/miui/powerkeeper/statemachine/PadSleepModeController.smali
    $oi/smali/classes*/com/miui/powerkeeper/millet/MilletConfig.smali
    $oi/smali/classes*/com/miui/powerkeeper/statemachine/SleepModeControllerNew.smali
    $oi/smali/classes*/com/miui/powerkeeper/statemachine/SleepModeControllerNew\$SleepHandler.smali
    $oi/smali/classes*/com/miui/powerkeeper/customerpower/CustomerPowerCheck.smali
    "
elif [ "${vv##*/}" == "miui-framework.jar" ];then
    [ "$fix_noti" == 1 ] && Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' "$oi/smali/classes*/com/android/internal/app/ChooserActivityStubImpl.smali
    $oi/smali/classes*/android/app/AppOpsManagerInjector.smali"
    if [ "$fix_global" == 1 ];then
    cp -rf "$MPAT/mod/eu" "$oi/smali/classes"
    Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' "$oi/smali/classes*/miui/util/font/MultiLangHelper.smali
    $oi/smali/classes*/com/android/internal/app/ChooserActivityStubImpl.smali
    $oi/smali/classes*/miui/util/font/SymlinkUtils.smali
    $oi/smali/classes*/android/app/AppOpsManagerInjector.smali"
    fi
elif [ "${vv##*/}" == "MiuiSystemUI.apk" ];then
    if [ "$fix_noti" == 1 ];then
    Thaythe 'Lcom/miui/utils/configs/MiuiConfigs;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' \
    "$oi/smali/classes*/com/android/systemui/statusbar/notification/utils/NotificationUtil.smali
    $oi/smali/classes*/com/miui/systemui/notification/MiuiBaseNotifUtil.smali
    $oi/smali/classes*/com/miui/systemui/notification/NotificationSettingsManager.smali
    $oi/smali/classes*/com/android/systemui/statusbar/notification/interruption/MiuiNotificationInterruptStateProviderImpl.smali"
    Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lcom/xBuild;->isTrue:Z' "$oi/smali/classes*/com/miui/keyguard/biometrics/fod/MiuiGxzwQuickOpenUtil.smali
    $oi/smali/classes*/com/android/systemui/assist/PhoneStateMonitor.smali"
    fi
elif [ "${vv##*/}" == "Settings.apk" ];then
    if [ "$fix_global" == 1 ];then
        Thaycn '.method public static isNeedRemoveKidSpaceInternal(Landroid/content/Context;)Z' 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' \
        'Lcom/xBuild;->isTrue:Z' $oi/smali/classes*/com/android/settings/utils/SettingsFeatures.smali
        Thaythe 'sget-boolean v0, Lmiui/os/Build;->IS_GLOBAL_BUILD:Z' 'sget-boolean v0, Lcom/xBuild;->isTrue:Z' $oi/smali/classes*/com/android/settings/MiuiSettings.smali
        Thayvc 1 '.method .*. isLocationNeeded(Landroid/content/Context;)Z' $oi/smali/classes*/com/android/settings/utils/SettingsFeatures.smali
        Thayvc 1 '.method .*. isPrivacyNeeded(Landroid/content/Context;)Z' $oi/smali/classes*/com/android/settings/utils/SettingsFeatures.smali
        # Thêm gr telegram
        Thayvc 1 '.method .*. isVisible(Landroid/content/Context;)Z' $oi/smali/classes*/com/android/settings/device/MiuiGuaranteeCard.smali
        Thayme '.method public onClick(Landroid/view/View;)V
        .registers 5
        invoke-virtual {p0}, Lcom/android/settings/device/MiuiGuaranteeCard;->getContext()Landroid/content/Context;
        move-result-object v0
        if-eqz v0, :cond_0
        :try_start_0
        new-instance v1, Landroid/content/Intent;
        const-string v2, "android.intent.action.VIEW"
        invoke-direct {v1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
        const-string v2, "tg://resolve?domain=tooltree"
        invoke-static {v2}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
        move-result-object v2
        invoke-virtual {v1, v2}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;
        const/high16 v2, 0x10000000
        invoke-virtual {v1, v2}, Landroid/content/Intent;->setFlags(I)Landroid/content/Intent;
        invoke-virtual {v0, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
        goto :goto_0
        :try_end_0
        .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
        :catch_0
        move-exception v1
        invoke-virtual {v1}, Ljava/lang/Exception;->printStackTrace()V
        :cond_0
        :goto_0
        return-void
        .end method' $oi/smali/classes*/com/android/settings/device/MiuiGuaranteeCard.smali
        Thayme '.method private initView()V
        .registers 4
        iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
        invoke-static {v0}, Lcom/android/settings/device/MiuiGuaranteeCard;->isVisible(Landroid/content/Context;)Z
        move-result v0
        if-nez v0, :cond_e
        const/16 v0, 0x8
        invoke-virtual {p0, v0}, Landroid/widget/FrameLayout;->setVisibility(I)V
        return-void
        :cond_e
        iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
        const-string/jumbo v1, "micare_expiry_time"
        invoke-static {v0, v1}, Lcom/android/settings/device/MiCareUtils;->getMiCareInfoWithPrefKey(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
        move-result-object v0
        iput-object v0, p0, Lcom/android/settings/device/MiuiGuaranteeCard;->mMiCareExpiryTime:Ljava/lang/String;
        iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
        const-string/jumbo v1, "micare_link"
        invoke-static {v0, v1}, Lcom/android/settings/device/MiCareUtils;->getMiCareInfoWithPrefKey(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
        move-result-object v0
        iput-object v0, p0, Lcom/android/settings/device/MiuiGuaranteeCard;->mMiCareLink:Ljava/lang/String;
        iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
        invoke-static {v0}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;
        move-result-object v0
        sget v1, Lcom/android/settings/R$layout;->my_device_info_item:I
        const/4 v2, 0x1
        invoke-virtual {v0, v1, p0, v2}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
        invoke-virtual {p0, p0}, Landroid/widget/FrameLayout;->setOnClickListener(Landroid/view/View$OnClickListener;)V
        sget v0, Lcom/android/settings/R$id;->title:I
        invoke-virtual {p0, v0}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;
        move-result-object v0
        check-cast v0, Landroid/widget/TextView;
        new-instance v1, Ljava/lang/StringBuilder;
        invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
        const-string/jumbo v2, "Kakathic"
        invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
        invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
        move-result-object v1
        invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
        sget v0, Lcom/android/settings/R$id;->summary:I
        invoke-virtual {p0, v0}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;
        move-result-object v0
        check-cast v0, Landroid/widget/TextView;
        new-instance v1, Ljava/lang/StringBuilder;
        invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
        const-string/jumbo v2, "@tooltree"
        invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
        invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
        move-result-object p0
        invoke-virtual {v0, p0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
        return-void
        .end method' $oi/smali/classes*/com/android/settings/device/MiuiGuaranteeCard.smali
    
        # thông tin
        if [ "$(grep -cm1 device_description_cpu "$oi/resources/package_1/res/values/strings.xml")" != 1 ];then
            smurl1="$(find $oi/smali/classes*/com/android/settings/device/DeviceParamsInitHelper.smali -type f)"
            if [ -f "$smurl1" ];then
                IFS=$'\n'
                for vnl in $(grep -B 2 -n "langType" "$smurl1" | tac | grep 'move-result-object'); do
                sed -i "$(echo "$vnl" | cut -d- -f1)a\ const-string $(echo "$vnl" | awk '{print $3}'), \"enUS\"" "$smurl1"
                done
            fi
            Timueelnd="$(find $oi/smali/classes*/com/android/settings/device/DeviceBasicInfoPresenter.smali -type f)"
            if [ -f "$Timueelnd" ];then
                thdbdbi1="$(grep -A 2 'Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Lorg/json/JSONObject;)Ljava/lang/String;' "$Timueelnd" | grep move-result-object | awk '{print $2}')"
                thdbdbi2="$(grep 'Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Lorg/json/JSONObject;)Ljava/lang/String;' "$Timueelnd" | awk '{print $2}' | sed "s|{|{$thdbdbi1, |")"
                if [ "$thdbdbi1" ];then
                Thaythe "$(grep 'Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Lorg/json/JSONObject;)Ljava/lang/String;' "$Timueelnd")" \
                "iget-object $thdbdbi1, p0, Lcom/android/settings/device/DeviceBasicInfoPresenter;->mContext:Landroid/content/Context;
                invoke-static $thdbdbi2 Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Landroid/content/Context;Lorg/json/JSONObject;)Ljava/lang/String;" "$Timueelnd"
                fi
                idkfjf1="$(grep -nA 20 'sget .*., Lcom/android/settings/R$string;->camera_front:I' "$Timueelnd" | grep 'const-string .*., ""')"
                [ "$idkfjf1" ] && sed -i "$(echo "$idkfjf1" | cut -d- -f1)c\ const-string $(echo "$idkfjf1" | awk '{print $3}') \" \"" "$Timueelnd"
            fi
    
            Thayme '.method public static getItemTitle(Landroid/content/Context;Lorg/json/JSONObject;)Ljava/lang/String;
            .registers 4
            invoke-static {p1}, Lcom/android/settings/device/ParseMiShopDataUtils;->getItemIndex(Lorg/json/JSONObject;)I
            move-result v0
            const/4 v1, 0x0
            if-eq v0, v1, :cond_17
            const/4 v1, 0x1
            if-eq v0, v1, :cond_22
            const/4 v1, 0x3
            if-eq v0, v1, :cond_2d
            const/4 v1, 0x4
            if-eq v0, v1, :cond_38
            const-string v1, "Title"
            invoke-static {p1, v1}, Lcom/android/settings/device/JSONUtils;->getString(Lorg/json/JSONObject;Ljava/lang/String;)Ljava/lang/String;
            move-result-object v0
            return-object v0
            :cond_17
            invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
            move-result-object v1
            sget p0, Lcom/android/settings/R$string;->device_description_cpu:I
            invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
            move-result-object v0
            return-object v0
            :cond_22
            invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
            move-result-object v1
            sget p0, Lcom/android/settings/R$string;->device_description_battery:I
            invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
            move-result-object v0
            return-object v0
            :cond_2d
            invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
            move-result-object v1
            sget p0, Lcom/android/settings/R$string;->device_description_screen:I
            invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
            move-result-object v0
            return-object v0
            :cond_38
            invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
            move-result-object v1
            sget p0, Lcom/android/settings/R$string;->device_description_resolution:I
            invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
            move-result-object v0
            return-object v0
            .end method' $oi/smali/classes*/com/android/settings/device/ParseMiShopDataUtils.smali
            Thaythe '</resources>' '    <string name="device_description_cpu" public="false">CPU</string>
            <string name="device_description_resolution" public="false">Resolution</string>
            <string name="device_description_screen" public="false">Display</string>
            </resources>' "$oi/resources/package_1/res/values/strings.xml"
        fi
    fi
fi
# End patch smali
echo
apkeditor_b -i "$oi" -o "${vv%/*}" -d 1
echo
done
}

toolbox(){
for vv in $@; do
[ ! -e "$vv" ] && { about "Item not found: $vv"; continue; }
tmpl="${vv##*/}"; oi="$MPAT/apk/${tmpl%.*}"
apkeditor_d -i "$vv" -o "${oi%/*}" -t raw
echo
if [ "$fix_apksign" == 1 ];then
    if [ "${vv##*/}" == "framework.jar" ];then
        patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"
        for kssc in ApkSignatureSchemeV2Verifier.smali ApkSignatureSchemeV3Verifier.smali ApkSignatureSchemeV4Verifier.smali ApkSigningBlockUtils.smali; do
            Thayivo 1 'invoke-static.*Ljava/security/MessageDigest;->isEqual([B[B)Z' $oi/smali/classes*/android/util/apk/$kssc
            Thayivo 1 'invoke-virtual.*Ljava/security/Signature;->verify([B)Z' $oi/smali/classes*/android/util/apk/$kssc
        done
        Thayvc 1 '.method .*. getMinimumSignatureSchemeVersionForTargetSdk(I)I' $oi/smali/classes*/android/util/apk/ApkSignatureVerifier.smali
        Thayvc 1 '.method .*. verifyMessageDigest([B[B)Z' $oi/smali/classes*/android/util/jar/StrictJarVerifier.smali
        Thayvc 0 '.method .*. containsAllocatedTable()Z' $oi/smali/classes*/android/content/res/AssetManager.smali
        Thayvc 1 '.method .*. checkCapability(Landroid' $oi/smali/classes*/android/content/pm
        Thayvc 1 '.method .*. signaturesMatchExactly(Landroid' $oi/smali/classes*/android/content/pm
        Thayvc 1 '.method .*. hasCommonAncestor(Landroid' $oi/smali/classes*/android/content/pm
        Thayvc 1 '.method .*. checkCapability(Ljava' $oi/smali/classes*/android/content/pm
        Thayvc 1 '.method .*. checkCapabilityRecover(Landroid' $oi/smali/classes*/android/content/pm
        Thayvc 1 '.method .*. hasAncestorOrSelf(Landroid' $oi/smali/classes*/android/content/pm
        if [ "$(grep -A 6 "StrictJarFile;->findEntry" $oi/smali/classes*/android/util/jar/StrictJarFile.smali 2>/dev/null | grep -cm1 "const/4 .*. 0x1")" != 1 ]; then
           sed -i -E '/StrictJarFile;->findEntry/,/move-result-object ([vp][0-9]+)/s/(move-result-object ([vp][0-9]+))/\1\nconst\/4 \2, 0x1/' $oi/smali/classes*/android/util/jar/StrictJarFile.smali || about "Error: StrictJarFile;->findEntry"
        fi
        if [ "$(grep -B 4 "iput-boolean .*RollbackProtectionsEnforced:Z" $oi/smali/classes*/android/util/jar/StrictJarVerifier.smali 2>/dev/null | grep -cm1 "const/4 .*. 0x0")" != 1 ]; then
            sed -i -E 's/(iput-boolean ([vp][0-9]+), ([vp][0-9]+), .*RollbackProtectionsEnforced:Z)/const\/4 \2, 0x0\n    \1/' $oi/smali/classes*/android/util/jar/StrictJarVerifier.smali || about "Error: signatureSchemeRollbackProtectionsEnforced"
        fi
    elif [ "${vv##*/}" == "core-oj.jar" ];then
        Thayvc 1 '.method .*. isEqual([B[B)Z' $oi/smali/classes*/java/security/MessageDigest.smali
        Thayvc 1 '.method .*. verify([B)Z' $oi/smali/classes*/java/security/Signature.smali
        Thayvc 1 '.method .*. verify([BII)Z' $oi/smali/classes*/java/security/Signature.smali
        Thayvc 1 '.method .*. verifyManifestHash(Ljava' $oi/smali
    elif [ "${vv##*/}" == "services.jar" ];then
        Thayme '.method static constructor <clinit>()V
            .locals 1
            const/4 v0, 0x1
            sput-boolean v0, Lcom/android/server/pm/ReconcilePackageUtils;->ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS:Z
            return-void
        .end method' $oi/smali/classes*/com/android/server/pm/ReconcilePackageUtils.smali
        file_smali_hhttss="$(Timkiem ".method .*. preparePackageLI(Lcom" $oi/smali/classes*/com/android/server/pm)"
        [ -f "$file_smali_hhttss" ] && awk -v t=$(grep -c '"<nothing>"' "$file_smali_hhttss") '/"<nothing>"/{c++;f=(c==t)} f&&/if-eqz v[0-9]+/{match($0,/v[0-9]+/);r=substr($0,RSTART,RLENGTH);if(last!~"const/4 "r", 0x1")print "    const/4 " r ", 0x1"} {print; if($0!~/^[[:space:]]*$/)last=$0} /isLeavingSharedUser\(\)Z/{f=0}' "$file_smali_hhttss" > "${file_smali_hhttss}_s" && mv "${file_smali_hhttss}_s" "$file_smali_hhttss"
        Thayvc 1 '.method .*. doesSignatureMatchForPermissions(Ljava' $oi/smali/classes*/com/android/server/pm
        Thayvc -v '.method .*. checkDowngrade(Lcom/android/server/pm/parsing/pkg/AndroidPackage' $oi/smali/classes*/com/android/server/pm
        Thayvc 0 '.method .*. matchSignaturesRecover(Ljava' $oi/smali/classes*/com/android/server/pm
        Thayvc 0 '.method .*. matchSignatureInSystem(Lcom' $oi/smali/classes*/com/android/server/pm
        Thayvc 0 '.method .*. matchSignaturesCompat(Ljava' $oi/smali/classes*/com/android/server/pm
        Thayvc 0 '.method .*. verifySignatures(Lcom' $oi/smali/classes*/com/android/server/pm
        Thayvc 0 '.method .*. shouldCheckUpgradeKeySetLocked(Lcom' $oi/smali/classes*/com/android/server/pm
        Thayvc 1 '.method .*. checkUpgradeKeySetLocked(Lcom' $oi/smali/classes*/com/android/server/pm
        Thayvc 0 '.method .*. isVerificationEnabled(Landroid' $oi/smali/classes*/com/android/server/pm
        Thayvc 1 '.method .*. checkAppSignature([Landroid' $oi/smali/classes*/com/miui/server
        Thayvc -v '.method .*. checkDowngrade(Landroid/content/pm/PackageParser$Package' $oi/smali/classes*/com/miui/server
        Thayvc -v '.method .*. checkSystemSelfProtection(Z)V' $oi/smali/classes*/com/miui/server
        Thayvc 1 '.method .*. checkSysAppCrack()Z' $oi/smali/classes*/com/miui/server
        Thayvc 1 '.method .*. isDowngradePermitted(IZ)Z' $oi/smali/classes*/com/android/server/pm
        Thayvc 0 '.method .*. isApkVerityEnabled()Z' $oi/smali/classes*/com/android/server/pm
    elif [ "${vv##*/}" == "miui-services.jar" ];then
        Thayvc -v '.method .*. verifyIsolationViolation(Lcom' $oi/smali/classes*/com/android/server/pm
        Thayvc -v '.method .*. canBeUpdate(Ljava' $oi/smali/classes*/com/android/server/pm
        Thayvc 1 '.method .*. checkAppSignature([Landroid' $oi/smali/classes*/com/miui/server
        Thayvc -v '.method .*. checkSystemSelfProtection(Z)V' $oi/smali/classes*/com/miui/server
        Thayvc 1 '.method .*. checkSysAppCrack()Z' $oi/smali/classes*/com/miui/server
    fi
fi
if [ "$fix_toolbox" == 1 ];then
    if [ "${vv##*/}" == "framework.jar" ];then
    # kiểm tra mạng và tải xuống
        if checkonline; then
            echo "Check version KaoriosToolbox..."
            echo
            linkurrl="$(xem https://api.github.com/repos/Wuang26/Kaorios-Toolbox/releases/tags/V1.0.9 2>/dev/null)"
            pbver="$(echo "$linkurrl" | jq -r ".tag_name")"
            [ -z "$pbver" ] && killtree "Version not found error !"
            if [ ! -f "$MPAT/mod/version" ] || [ "$pbver" != "$(cat "$MPAT/mod/version" 2>/dev/null)" ];then
            echo "Updating: $pbver"
            echo "$pbver" > "$MPAT/mod/version"
            downloadb "$(echo "$linkurrl" | jq -r ".assets[].browser_download_url" | grep "KaoriosToolbox.*\.apk")" "$MPAT/mod/KaoriosToolbox.apk" &>/dev/null
            downloadb "$(echo "$linkurrl" | jq -r ".assets[].browser_download_url" | grep "com.kousei.kaorios.xml")" "$MPAT/mod/com.kousei.kaorios.xml" &>/dev/null
            downloadb "$(echo "$linkurrl" | jq -r ".assets[].browser_download_url" | grep "classes.*\.dex")" "$MPAT/mod/classes.dex" &>/dev/null
            else
            echo "Latest version: $pbver"
            fi
        else
            [ -f $MPAT/mod/version ] || killtree "$network_text"
        fi
        echo
        # Patch smali
        if ! ls "$oi"/smali/classes*/com/android/internal/util/kaorios &>/dev/null; then
            kkklast=$(ls -1d "$oi"/smali/classes* 2>/dev/null | sort | tail -n1)
            cp -rf "$MPAT/mod/classes.dex" "$oi/dex/classes$(( ${kkklast##*classes} + 1 )).dex"
            sed -i '/"resources\.arsc"/i\    "classes'$(( ${kkklast##*classes} + 1 ))'.dex",' $oi/uncompressed-files.json
            path_smali_4="$(find $oi/smali/classes*/android/security/KeyStore2.smali -type f)"
            [ -f "$path_smali_4" ] && sed -i '/\.method public .*getKeyEntry/,/\.end method/ s/check-cast \([vp][0-9][0-9]*\), Landroid\/system\/keystore2\/KeyEntryResponse;/check-cast \1, Landroid\/system\/keystore2\/KeyEntryResponse;\
            invoke-static {\1}, Lcom\/android\/internal\/util\/kaorios\/KaoriKeyboxHooks;->KaoriGetKeyEntry(Landroid\/system\/keystore2\/KeyEntryResponse;)Landroid\/system\/keystore2\/KeyEntryResponse;\
            move-result-object \1/' "$path_smali_4" || about "Error method public .*getKeyEntry"
            path_smali_3="$(find $oi/smali/classes*/android/security/keystore2/AndroidKeyStoreSpi.smali -type f)"
            if [ -f "$path_smali_3" ];then
                sed -i '/\.method public .*engineGetCertificateChain/,/\.end method/{
                /\.locals/a\ invoke-static {}, Lcom/android/internal/util/kaorios/KaoriPropsUtils;->KaoriGetCertificateChain()V
                }' "$path_smali_3" || about "Error method public .*engineGetCertificateChain"
                so_dong_1="$(nl -ba "$path_smali_3" | sed -n '/\.method public .*engineGetCertificateChain/,/\.end method/p' | grep -A2 'aput-object' | grep -B2 'return-object' | awk '/return-object/{print $1;exit}')"
                sed -i "${so_dong_1}s/return-object \([vp][0-9]\+\)/invoke-static {\1}, Lcom\/android\/internal\/util\/kaorios\/KaoriKeyboxHooks;->KaoriGetCertificateChain([Ljava\/security\/cert\/Certificate;)[Ljava\/security\/cert\/Certificate;\nmove-result-object \1\nreturn-object \1/" "$path_smali_3" || about "Error method public .*engineGetCertificateChain 2"
            fi
            path_smali_1="$(find $oi/smali/classes*/android/app/ApplicationPackageManager.smali -type f)"
            if [ -f "$path_smali_1" ];then
                sed -i '/\.method public .*hasSystemFeature(Ljava\/lang\/String;)Z/,/\.end method/{
                s/\(move-result \([vp][0-9]\+\)\)/&\
                :try_start\
                invoke-static {\2, p1}, Lcom\/android\/internal\/util\/kaorios\/KaoriPropsUtils;->KaoriFeatureBlock(ZLjava\/lang\/String;)Z\
                move-result \2\
                :try_end\
                .catchall {:try_start .. :try_end} :after_appinj\
                :after_appinj/
                }' "$path_smali_1" || about "Error method public .*hasSystemFeature"
                sed -i '/\.method public .*hasSystemFeature(Ljava\/lang\/String;I)Z/,/\.end method/{
                /\.locals [0-9]\+/a\
                invoke-static {}, Landroid/app/ActivityThread;->currentPackageName()Ljava/lang/String;\
                move-result-object v0\
                :try_start_kaori\
                iget-object v1, p0, Landroid/app/ApplicationPackageManager;->mContext:Landroid/app/ContextImpl;\
                invoke-static {v1, p1, v0}, Lcom/android/internal/util/kaorios/KaoriFeatureOverrides;->getOverride(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Boolean;\
                move-result-object v0\
                :try_end_kaori\
                .catchall {:try_start_kaori .. :try_end_kaori} :catch_kaori\
                if-eqz v0, :cond_kaori\
                invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z\
                move-result v0\
                return v0\
                :catch_kaori\
                :cond_kaori
                }' "$path_smali_1" || about "Error method public .*hasSystemFeature 2"
            fi
            path_smali_2="$(find $oi/smali/classes*/android/app/Application.smali -type f)"
            [ -f "$path_smali_2" ] && sed -i '/iput-object .* Landroid\/app\/Application;->mLoadedApk:Landroid\/app\/LoadedApk;/a\
            :try_start\
            invoke-static {p0}, Lcom/android/internal/util/kaorios/KaoriPropsUtils;->KaoriProps(Landroid/content/Context;)V\
            :try_end\
            .catchall {:try_start .. :try_end} :after_appinj\
            :after_appinj' "$path_smali_2" || about "Error method iput-object mLoadedApk"
            # xử lý còn lại
            mkdir -p "$psystem/priv-app/KaoriosToolbox/lib/arm64"
            cp -rf "$MPAT/mod/KaoriosToolbox.apk" "$psystem/priv-app/KaoriosToolbox"
            unzip -qoj "$MPAT/mod/KaoriosToolbox.apk" lib/arm64-v8a/* -d "$psystem/priv-app/KaoriosToolbox/lib/arm64"
            cp -rf "$MPAT/mod/com.kousei.kaorios.xml" "$psystem/etc/permissions"
            sprop persist.sys.kaorios kousei "$psystem/build.prop"
            echo "Auto added to the project: persist.sys.kaorios=kousei, com.kousei.kaorios.xml, KaoriosToolbox.apk"
        fi
    fi
fi
# End patch smali
echo
apkeditor_b -i "$oi" -o "${vv%/*}" -d 1
echo
done
}

custom_patch(){
# system
psystem="$(ls -1d $SDH/$PTSH/*/system/build.prop 2>/dev/null | grep -m1 'system' | sed 's|\/build.prop||')"
# vendor
pvendor="$(ls -1d $SDH/$PTSH/*endo*/build.prop 2>/dev/null | grep -m1 'vendor' | sed 's|\/build.prop||')"
# system_ext
psystem_ext="$(ls -1d $SDH/$PTSH/*ystem_ex*/etc/build.prop 2>/dev/null | sed 's|\/etc/build.prop||' | grep -m1 'system_ext')"
[ -z "$psystem_ext" ] && psystem_ext="$(ls -1d $SDH/$PTSH/*/*ystem_ex*/etc/build.prop 2>/dev/null | grep -m1 'system_ext' | sed 's|\/etc/build.prop||')"
# product
pproduct="$(ls -1d $SDH/$PTSH/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
[ -z "$pproduct" ] && pproduct="$(ls -1d $SDH/$PTSH/*/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
####
pvendor_boot="$(ls -1d $SDH/$PTSH/*/vendor_boot.img 2>/dev/null | grep -m1 'vendor_boot' | sed 's|\/vendor_boot.img||')"
pmi_ext="$(ls -1d $SDH/$PTSH/*i_ex*/etc/build.prop 2>/dev/null | grep -m1 'mi_ext' | sed 's|\/etc/build.prop||')"

if [ "$rw_rom" == 1 ];then
    [ -d "$psystem" ] || about "Partition not found system"
    [ -d "$psystem_ext" ] || about "Partition not found system_ext"
    [ -d "$pproduct" ] || about "Partition not found product"
    [ -d "$pvendor" ] || killtree "Partition not found vendor"
    [ -d "$pvendor_boot" ] || echo "Warning: Partition not found vendor_boot" >&2
    [ -d "$pmi_ext" ] || echo "Warning: Partition not found mi_ext" >&2
    for vv in $pvendor/etc/fstab.* $pvendor_boot/ramdisk/first_stage_ramdisk/fstab.*; do
    if [ -f "$vv" ];then
    [ "$(grep '/system .*.discard' "$vv" | grep -cm1 ext4)" == 1 ] || echo "Warning: No partition containing ext4 was found; it needs to be added manually: $vv" >&2
    sed -i '/camera\|audio\|sensor/!s/^\(overlay.*\)/#\1/' "$vv"
    fi
    done
    # di chuyển pangu và dọn dẹp
    if [ -d $pproduct/pangu/system ];then
    echo "Move pangu..."
    cp -rf $pproduct/pangu/system/* $psystem 2>/dev/null
    rm -fr $pproduct/pangu/system
    fi
    # copy mi_ext
    if [ -d "$pmi_ext" ];then
    echo "Move mi_ext..."
    cp -rf $pmi_ext/product/* $pproduct 2>/dev/null
    cp -rf $pmi_ext/system_ext/* $psystem_ext 2>/dev/null
    cp -rf $pmi_ext/system/* $psystem 2>/dev/null
    rm -fr $pmi_ext/product/* $pmi_ext/system_ext/* $pmi_ext/system/*
    fi
fi

if [ "$device_features" == 1 ];then
[ -d "$pproduct" ] || about "Partition not found product"
for vv in $pproduct/etc/device_features/*.xml; do
[ -f "$vv" ] && sed -i "s|support_ota_validate\">true|support_ota_validate\">false|" "$vv"
done
fi

if [ "$home_poco" == 1 ];then
[ -d "$psystem" ] || about "Partition not found system"
[ -d "$psystem_ext" ] || about "Partition not found system_ext"
file_ext_ss="$psystem_ext/etc/init/init.miui.ext.rc"
[ -f "$file_ext_ss" ] && sed -i "s|com.mi.android.globallauncher|com.miui.home|g" "$file_ext_ss"
sprop "ro.miui.product.home" "com.miui.home" "$psystem/build.prop"
fi

if [ "$delete_gms" == 1 ];then
[ -d "$pproduct" ] || about "Partition not found product"
[ -f "$pproduct/etc/permissions/cn.google.services.xml" ] && rm -f $pproduct/etc/permissions/cn.google.services.xml
fi

if [ "$patch_prop" != "none" ];then
[ -d "$psystem" ] || about "Partition not found system"
[ -d "$psystem_ext" ] || about "Partition not found system_ext"
[ -d "$pproduct" ] || about "Partition not found product"
[ -d "$pvendor" ] || about "Partition not found vendor"
for vv in $pvendor/build.prop $psystem_ext/etc/build.prop $pproduct/etc/build.prop; do
[ -f "$vv" ] && sed -i "/ro.control_privapp_permissions/d" "$vv"
done
sprop ro.control_privapp_permissions $patch_prop "$psystem/build.prop"
fi
}

del_app(){
for vcdel in $@; do
[ -n "$vcdel" ] && find $SDH/$PTSH -name "$vcdel" -print -exec rm -rf {} +
done
}

cover_app(){
pproduct="$(ls -1d $SDH/$PTSH/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
[ -z "$pproduct" ] && pproduct="$(ls -1d $SDH/$PTSH/*/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
for vcapp in $@; do
tmpl="${vcapp##*/}"; oi="$TMP/apk/${tmpl%.*}"
if zipalign -c -v 4 "$vcapp" | grep -q 'lib/.*.(OK)'; then
mv "${vcapp%/*}" "$pproduct/app"
else
if unzip -ql "$vcapp" | grep -q 'lib/'; then
apktool_d -i "$vcapp" -o "${oi%/*}" -d 0 -r 0
echo
apktool_b -i "$oi" -o "${vcapp%/*}" -d 1 -x false
echo
fi
mv "${vcapp%/*}" "$pproduct/app"
fi
echo "Save at: $pproduct/app/${tmpl%.*}/$tmpl"
echo
done
}

search(){
for vcs in $@; do
seprojects="$(find $SDH/$PTSH -type d \( -name "app" -o -name "priv-app" -o -name "framework" -o -name "data-app" -o -name "overlay" -o -name "apex" \) -exec find {} -type f -name "$vcs" -print -quit \; 2>/dev/null)"
[ -f "$seprojects" ] && echo "$seprojects|${seprojects##*/}" || echo "ERROR_$RANDOM|File not found: $vcs"
done
}

search_apk(){
pproduct="$(ls -1d $SDH/$PTSH/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
[ -z "$pproduct" ] && pproduct="$(ls -1d $SDH/$PTSH/*/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
if [ -d "$pproduct" ]; then
projectpro="$(find $pproduct/data-app -type f -name "*.apk" 2>/dev/null)"
for vcxz in $projectpro; do
[ -f "$vcxz" ] && echo "$vcxz|${vcxz##*/}"
done
fi
}

Timkiem(){ grep -rl --include="*.*" "$1" $2 2>/dev/null; }
about(){ echo -e "$1" >&2; }

patch_smali(){
if [ ! -f "$1" ];then
mkdir -p "${1%/*}"
echo '.class public Lcom/xBuild;
.super Ljava/lang/Object;

# static fields
.field public static final isTrue:Z = true
.field public static final isFalse:Z

# invoke-static {}, Lcom/xBuild;->isOne()Z
# move-result v0
.method public static isOne()Z
    .registers 1
    const/4 v0, 0x1
    return v0
.end method

# invoke-static {}, Lcom/xBuild;->isZero()Z
# move-result v0
.method public static isZero()Z
    .registers 1
    const/4 v0, 0x0
    return v0
.end method' > "$1";
fi
}

Thaycn(){
urlsmali="$(find $4 -type f -print -quit 2>/dev/null)"
vcv="$(echo "$1" | head -n1 | sed 's|/|\\/|g')"
[ -f "$urlsmali" ] && sed -i "/$vcv/,/\.end method/ s|sget-boolean \(.*\), $2|sget-boolean \1, $3|" "$urlsmali" && echo "Patch: ${urlsmali##*/}" || about "Error: $urlsmali"
}

Thayme(){
for vcc2 in $2; do
if [ -f "$vcc2" ];then
vcv="$(echo "$1" | head -n1 | sed -e 's|/|\\/|g' -e 's|\[|\\[|g')"
sed -i "/$vcv/,/.end method/d" "$vcc2" && echo "$1" >> "$vcc2" && echo "Patch: ${vcc2##*/}" || about "Error: $vcv - $vcc2"
fi
done
}

Themme(){
urlsmali="$(find $2 -type f -print -quit 2>/dev/null)"
[ -f "$urlsmali" ] && echo "$1" >> "$urlsmali" && echo "Patch: ${urlsmali##*/}" || about "Error: $urlsmali"
}

Thaythe(){
for vcc1 in $3; do
if [ -f "$vcc1" ]; then
toybox sed -i "s|$1|$2|g" "$vcc1" && echo "Patch: ${vcc1##*/}" || about "Error: $vcc1"
fi
done
}

Thayvc(){
for vcx in $(Timkiem "${2//\[/\\[}" $3); do
if [ -f "$vcx" ];then
    if [ "$1" == "-v" ];then
    Thayme "$(grep -m1 "${2//\[/\\[}" "$vcx")
        .locals 1
        return-void
    .end method" "$vcx"
    else
    [ $1 -ge 8 ] && ui=16 || ui=4
    Thayme "$(grep -m1 "${2//\[/\\[}" "$vcx")
        .locals 1
        const/$ui v0, 0x$1
        return v0
    .end method" "$vcx"
    fi
fi
done
}

Thayivo(){
urlsmali="$(find $3 -type f -print -quit 2>/dev/null)"
if [ "$1" == 1 ];then
textvbs='invoke-static {}, Lcom/xBuild;->isOne()Z'
else
textvbs='invoke-static {}, Lcom/xBuild;->isZero()Z'
fi
[ -f "$urlsmali" ] && sed -i "s|${2//\[/\\[}|$textvbs|" "$urlsmali" && echo "Patch: ${urlsmali##*/}" || about "Error: $urlsmali"
}

# hiện tại
MPAT="${0%/*}"

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop")"
[ -f "$MPAT/language.sh" ] && source "$MPAT/language.sh"

# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

# index
if [ "$(type -t "$1")" = "function" ];then
"$@"
else
killtree "Error value !"
fi
