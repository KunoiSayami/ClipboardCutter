package com.github.kunoisayami.clipboard_share_params_cutter;

import android.content.ClipData;
import android.content.ClipboardManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class xposedInit implements IXposedHookLoadPackage{
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod(ClipboardManager.class, "setPrimaryClip",
                ClipData.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        ClipData clipData = (ClipData) param.args[0];
                        String clipStr = clipData.getItemAt(0).getText().toString();
                        if (clipStr.contains("?") && clipStr.startsWith("https://")) {
                            XposedBridge.log("Catch share link!");
                            ClipData tmpData = ClipData.newPlainText("", clipStr.split("\\?")[0]);
                            param.args[0] = tmpData;
                        }
                    }
                });
    }

}
