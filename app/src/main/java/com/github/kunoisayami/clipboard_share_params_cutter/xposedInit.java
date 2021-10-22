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

    private static final ArrayList<String> mp_qq_share_arguments = new ArrayList<>(
            Arrays.asList("sig",
                    "articl_id",
                    "time"));

    public static ClipData cutArticle(String originUrl, ArrayList<String> args, String url_base) {
        ArrayList<String> arguments = new ArrayList<>();
        for (String arg : originUrl.split("\\?")[1].split("&")) {
            String[] group = arg.split("=");
            if (!args.contains(group[0])) {
                continue;
            }
            arguments.add(arg);
        }
        String final_args = String.join("&", arguments);
        return ClipData.newPlainText("",
                new StringBuilder(url_base).append(final_args));
    }

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
                                param.args[0] = cutArticle(clipStr, wechat_share_arguments, "https://mp.weixin.qq.com/s?");
                            } else if (clipStr.startsWith("https://twitter.com")) {
                                XposedBridge.log("Catch share link!");
                                ClipData tmpData = ClipData.newPlainText("", clipStr.split("\\?")[0]);
                                param.args[0] = tmpData;
                            } else if (clipStr.startsWith("https://post.mp.qq.com") && clipStr.contains("?")) {
                                XposedBridge.log("Catch QQ article long share link!");
                                param.args[0] = cutArticle(clipStr, mp_qq_share_arguments, clipStr.split("\\?")[0] + "?");
                            }
                        } else if (clipStr.startsWith("{") && clipStr.endsWith("}") &&
                                clipStr.contains("com.tencent.structmsg")) {
                            String extract_url = clipStr.split("jumpUrl\":\"")[1];
                            extract_url = extract_url
                                    .split(extract_url.contains("?") ? "\\?" : "\"")[0]
                                    .replace("\\", "")
                                    .replace(".tv/", ".wtf/");
                            XposedBridge.log("Extract tcb link: " + extract_url);
                            param.args[0] = ClipData.newPlainText("", extract_url);
                        }
                    }
                });
    }

}
