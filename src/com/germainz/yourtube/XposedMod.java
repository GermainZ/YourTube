package com.germainz.yourtube;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class XposedMod implements IXposedHookLoadPackage {

    private static final String PREF_DEFAULT_PANE = "pref_default_pane";
    private static final String PREF_PLAYLIST = "pref_playlist";
    private static final String PREF_SUBSCRIPTION = "pref_subscription";
    private static final String PREF_OVERRIDE_DEVICE_SUPPORT = "pref_override_device_support";
    private static final String PREF_MAXIMUM_STREAM_QUALITY = "pref_maximum_stream_quality";
    private static final String DEFAULT_PANE = "FEsubscriptions";
    private static final String DEFAULT_STREAM_QUALITY = "-2";
    private static final String PANE_PLAYLIST = "0";
    private static final String PANE_SUBSCRIPTION = "1";
    private static boolean sNewVideo = true;
    private static ArrayList<Integer> sStreamQualities;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.youtube"))
            return;

        final XSharedPreferences prefs = new XSharedPreferences("com.germainz.yourtube");

        findAndHookMethod("com.google.android.apps.youtube.app.WatchWhileActivity", lpparam.classLoader, "O",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        String paneString = prefs.getString(PREF_DEFAULT_PANE, DEFAULT_PANE);
                        if (paneString.equals(PANE_PLAYLIST))
                            paneString = "VL" + prefs.getString(PREF_PLAYLIST, "");
                        else if (paneString.equals(PANE_SUBSCRIPTION))
                            paneString = prefs.getString(PREF_SUBSCRIPTION, "");

                        Class navigationClass = findClass("a", lpparam.classLoader);
                        Class innertubeClass = findClass("emw", lpparam.classLoader);
                        Object paneFromString = callStaticMethod(innertubeClass, "a", paneString);
                        return callStaticMethod(navigationClass, "a", paneFromString, false);
                    }
                }
        );

        XC_MethodHook deviceSupportHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean(PREF_OVERRIDE_DEVICE_SUPPORT, false))
                    param.setResult(true);
            }
        };

        findAndHookMethod("bts", lpparam.classLoader, "A", deviceSupportHook);
        findAndHookMethod("bts", lpparam.classLoader, "B", deviceSupportHook);
        findAndHookMethod("bts", lpparam.classLoader, "y", deviceSupportHook);
        findAndHookMethod("bts", lpparam.classLoader, "z", deviceSupportHook);

        findAndHookMethod("com.google.android.apps.youtube.app.fragments.PlayerFragment", lpparam.classLoader, "a",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        sNewVideo = true;
                    }
                });

        findAndHookMethod("cls", lpparam.classLoader, "handleFormatStreamChangeEvent", "fdy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object[] info = (Object[]) getObjectField(param.args[0], "d");
                sStreamQualities = new ArrayList<Integer>();
                for (Object streamQuality : info) {
                    sStreamQualities.add(getIntField(streamQuality, "a"));
                }
            }
        });

        findAndHookMethod("cmc", lpparam.classLoader, "a", String[].class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (sNewVideo) {
                    sNewVideo = false;
                    int maximumStreamQuality = Integer.parseInt(prefs.getString(PREF_MAXIMUM_STREAM_QUALITY,
                            DEFAULT_STREAM_QUALITY));
                    int quality = -2;
                    for (int streamQuality : sStreamQualities) {
                        if (streamQuality <= maximumStreamQuality)
                            quality = streamQuality;
                    }
                    if (quality == -2)
                        quality = sStreamQualities.get(sStreamQualities.size() - 1);
                    else
                        quality = sStreamQualities.indexOf(quality);
                    param.args[1] = quality;
                    callMethod(getObjectField(param.thisObject, "H"), "a", quality);
                }
            }
        });
    }
}
