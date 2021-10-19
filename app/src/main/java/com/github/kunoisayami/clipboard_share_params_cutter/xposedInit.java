package com.github.kunoisayami.clipboard_share_params_cutter;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class xposedInit implements IXposedHookLoadPackage {
    private static final ArrayList<String> wechat_share_arguments = new ArrayList<>(
            Arrays.asList("__biz",
                    "mid",
                    "idx",
                    "sn",
                    "chksm"));

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
                            if (clipStr.startsWith("https://mp.weixin.qq.com/s?")) {
                                XposedBridge.log("Catch wechat article long share link!");
                                //HashMap<String, String> arguments = new HashMap<>();
                                ArrayList<String> arguments = new ArrayList<>();
                                for (String arg : clipStr.split("\\?")[1].split("&")) {
                                    String[] group = arg.split("=");
                                    if (!wechat_share_arguments.contains(group[0])) {
                                        continue;
                                    }
                                    arguments.add(arg);
                                }
                                String final_args = String.join("&", arguments);
                                ClipData tmpData = ClipData.newPlainText("",
                                        new StringBuilder("https://mp.weixin.qq.com/s?").append(final_args));
                                param.args[0] = tmpData;
                            } else if (clipStr.startsWith("https://twitter.com")) {
                                XposedBridge.log("Catch share link!");
                                ClipData tmpData = ClipData.newPlainText("", clipStr.split("\\?")[0]);
                                param.args[0] = tmpData;
                            }
                        } else if (clipStr.startsWith("{") && clipStr.endsWith("}") &&
                                clipStr.contains("com.tencent.structmsg") &&
                                clipStr.contains("b23.tv")) {
                            String extract_url = clipStr.split("jumpUrl\":\"")[1].split("\\?")[0]
                                    .replace("\\", "")
                                    .replace(".tv/", ".wtf/");
                            XposedBridge.log("Extract bilibili video link: " + extract_url);
                            param.args[0] = ClipData.newPlainText("", extract_url);
                        }
                    }
                });
    }

}
