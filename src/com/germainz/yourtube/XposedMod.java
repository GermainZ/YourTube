package com.germainz.yourtube;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class XposedMod implements IXposedHookLoadPackage {

    private static final String PREF_DEFAULT_PANE = "pref_default_pane";
    private static final String PREF_PLAYLIST = "pref_playlist";
    private static final String PREF_SUBSCRIPTION = "pref_subscription";
    private static final String DEFAULT_PANE = "FEsubscriptions";
    private static final String PANE_PLAYLIST = "0";
    private static final String PANE_SUBSCRIPTION = "1";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.youtube"))
            return;

        final XSharedPreferences prefs = new XSharedPreferences("com.germainz.yourtube");

        findAndHookMethod("com.google.android.apps.youtube.app.WatchWhileActivity", lpparam.classLoader, "M",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        String paneString = prefs.getString(PREF_DEFAULT_PANE, DEFAULT_PANE);
                        if (paneString.equals(PANE_PLAYLIST))
                            paneString = "VL" + prefs.getString(PREF_PLAYLIST, "");
                        else if (paneString.equals(PANE_SUBSCRIPTION))
                            paneString = prefs.getString(PREF_SUBSCRIPTION, "");

                        Class navigationClass = findClass("a", lpparam.classLoader);
                        Class innertubeClass = findClass("dhh", lpparam.classLoader);
                        Object paneFromString = callStaticMethod(innertubeClass, "a", paneString);
                        return callStaticMethod(navigationClass, "a", paneFromString, false);
                    }
                }
        );

/*
        findAndHookMethod("anw", lpparam.classLoader,
                "a", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        XposedBridge.log("Pane: " + getObjectField(param.thisObject, "ae"));
        // What to watch: FEwhat_to_watch
        // Subscriptions: FEsubscriptions
        // Watch Later: VLWL
        // Playlist: VL + <playlist_id>
        // get playlist id from: https://www.youtube.com/playlist?list=playlist_id
        // Liked videos: same as above
        // subscriptions: <subscription_id>
        // get subscriptions id from: https://www.youtube.com/channel/subscription_id
        // Browse channels: FEguide_builder
                    }
                }
        );
*/

    }
}
