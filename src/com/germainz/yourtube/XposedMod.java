package com.germainz.yourtube;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
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
    private static XC_MethodHook.Unhook sPaneHook;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.youtube"))
            return;

        final XSharedPreferences prefs = new XSharedPreferences("com.germainz.yourtube");

        // Default pane.
        // =============

        sPaneHook = findAndHookMethod(findClass("fia", lpparam.classLoader), "a", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0].equals("FEwhat_to_watch")) {
                    // change pane only on initial start up
                    sPaneHook.unhook();
                }

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

                param.args[0] = paneString;
            }
        });

        // Override compatibility checks.
        // ==============================

        XC_MethodHook deviceSupportHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean(PREF_OVERRIDE_DEVICE_SUPPORT, false))
                    param.setResult(true);
            }
        };

        findAndHookMethod("cmp", lpparam.classLoader, "A", deviceSupportHook);
        findAndHookMethod("cmp", lpparam.classLoader, "B", deviceSupportHook);
        findAndHookMethod("cmp", lpparam.classLoader, "C", deviceSupportHook);
        findAndHookMethod("cmp", lpparam.classLoader, "z", deviceSupportHook);

        // Default resolution.
        // ===================

        // We don't want to override the resolution when it's manually changed by the user, so we need to know
        // if the video was just opened (in which case the next time the resolution is set would be automatic) or not.
        findAndHookMethod("com.google.android.apps.youtube.app.fragments.PlayerFragment", lpparam.classLoader, "a",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        sNewVideo = true;
                    }
                });

        // We also want to get a list of the available qualities for this video, because the one that is passed
        // below is localized, so not comparable easily.
        findAndHookMethod("dbv", lpparam.classLoader, "handleFormatStreamChangeEvent", "gdq", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object[] info = (Object[]) getObjectField(param.args[0], "d");
                sStreamQualities = new ArrayList<Integer>();
                for (Object streamQuality : info) {
                    sStreamQualities.add(getIntField(streamQuality, "a"));
                }
            }
        });

        // Override the default quality.
        findAndHookMethod("dcd", lpparam.classLoader, "a", String[].class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (sNewVideo) {
                    sNewVideo = false;
                    /* Stream qualities:
                       -2 is used for "Auto", which resolves by default to the maximum available resolution.
                       Other qualities have their respective values (e.g. 720, 1080, etc). */
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
                    /* This method only controls the list shown to the user and the current selection.
                       It's called by handleFormatStreamChangeEvent, which in turn seems to be called by native code
                       *after* the quality has been changed.
                       This means that changing will only affect what's shown to the user as the selected quality, but
                       not the *actual* quality. */
                    param.args[1] = quality;
                    // This method is the one called when the user presses the button, and actually causes
                    // the quality to change.
                    callMethod(getObjectField(param.thisObject, "D"), "a", quality);
                }
            }
        });
    }
}
