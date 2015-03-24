package com.germainz.yourtube;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedBridge.unhookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
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

    private static final String VAR_1 = "O";
    private static final String VAR_2 = "fur";
    private static final String VAR_3 = "a";
    private static final String VAR_4 = "cus";
    private static final String VAR_5 = "A";
    private static final String VAR_6 = "B";
    private static final String VAR_7 = "C";
    private static final String VAR_8 = "z";
    private static final String VAR_9 = "bis";
    private static final String VAR_10 = "B";
    private static final String VAR_11 = "czb";
    private static final String VAR_12 = "gyq";
    private static final String VAR_13 = "d";
    private static final String VAR_14 = "a";
    private static final String VAR_15 = "btg";
    private static final String VAR_16 = "a";
    private static final String VAR_17 = "G";
    private static final String VAR_18 = "a";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.youtube"))
            return;

        final XSharedPreferences prefs = new XSharedPreferences("com.germainz.yourtube");

        // Default pane.
        // =============

        findAndHookMethod("com.google.android.apps.youtube.app.WatchWhileActivity", lpparam.classLoader, VAR_1,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String paneString = prefs.getString(PREF_DEFAULT_PANE, DEFAULT_PANE);
                        /* Pane ID:
                           What to watch: FEwhat_to_watch
                           Subscriptions: FEsubscriptions
                           Watch Later: VLWL
                           Playlist: VL + <playlist_id>
                               Get playlist id from: https://www.youtube.com/playlist?list=playlist_id
                           Liked videos: same as above
                           Subscriptions: <subscription_id>
                               Get subscriptions id from: https://www.youtube.com/channel/subscription_id
                           Browse channels: FEguide_builder */
                        if (paneString.equals(PANE_PLAYLIST))
                            paneString = "VL" + prefs.getString(PREF_PLAYLIST, "");
                        else if (paneString.equals(PANE_SUBSCRIPTION))
                            paneString = prefs.getString(PREF_SUBSCRIPTION, "");
                        final String finalPaneString = paneString;
                        findAndHookMethod(VAR_2, lpparam.classLoader, VAR_3, String.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.args[0] = finalPaneString;
                                unhookMethod(param.method, this);
                            }
                        });
                    }
                }
        );

        // Override compatibility checks.
        // ==============================

        XC_MethodHook deviceSupportHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean(PREF_OVERRIDE_DEVICE_SUPPORT, false))
                    param.setResult(true);
            }
        };

        findAndHookMethod(VAR_4, lpparam.classLoader, VAR_5, deviceSupportHook);
        findAndHookMethod(VAR_4, lpparam.classLoader, VAR_6, deviceSupportHook);
        findAndHookMethod(VAR_4, lpparam.classLoader, VAR_7, deviceSupportHook);
        findAndHookMethod(VAR_4, lpparam.classLoader, VAR_8, deviceSupportHook);

        // Default resolution.
        // ===================

        // We don't want to override the resolution when it's manually changed by the user, so we need to know
        // if the video was just opened (in which case the next time the resolution is set would be automatic) or not.
        findAndHookMethod(VAR_9, lpparam.classLoader, VAR_10, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        sNewVideo = true;
                    }
                });

        // We also want to get a list of the available qualities for this video, because the one that is passed
        // below is localized, so not comparable easily.
        findAndHookMethod(VAR_11, lpparam.classLoader, "handleFormatStreamChangeEvent", VAR_12, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object[] info = (Object[]) getObjectField(param.args[0], VAR_13);
                sStreamQualities = new ArrayList<Integer>();
                for (Object streamQuality : info)
                    sStreamQualities.add(getIntField(streamQuality, VAR_14));
            }
        });

        // Override the default quality.
        findAndHookMethod(VAR_15, lpparam.classLoader, VAR_16, String[].class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (sNewVideo) {
                    sNewVideo = false;
                    /* Stream qualities:
                       -2 is used for "Auto".
                       Other qualities have their respective values (e.g. 720, 1080, etc). */
                    int maximumStreamQuality = Integer.parseInt(prefs.getString(PREF_MAXIMUM_STREAM_QUALITY,
                            DEFAULT_STREAM_QUALITY));
                    int quality = -2;
                    for (int streamQuality : sStreamQualities) {
                        if (streamQuality <= maximumStreamQuality)
                            quality = streamQuality;
                    }
                    if (quality == -2)
                        return;
                    else
                        quality = sStreamQualities.indexOf(quality) + 1;
                    /* This method only controls the list shown to the user and the current selection.
                       It's called by handleFormatStreamChangeEvent, which in turn seems to be called by native code
                       *after* the quality has been changed.
                       This means that changing will only affect what's shown to the user as the selected quality, but
                       not the *actual* quality. */
                    param.args[1] = quality;
                    // This method is the one called when the user presses the button, and actually causes
                    // the quality to change.
                    callMethod(getObjectField(param.thisObject, VAR_17), VAR_18, quality);
                }
            }
        });
    }
}
