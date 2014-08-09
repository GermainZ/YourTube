package com.germainz.yourtube;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

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

        findAndHookMethod("com.google.android.apps.youtube.app.fragments.navigation.d", lpparam.classLoader,
                "a", "com.google.a.a.a.a.rl", boolean.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        String paneString = prefs.getString(PREF_DEFAULT_PANE, DEFAULT_PANE);
                        if (paneString.equals(PANE_PLAYLIST))
                            paneString = "VL" + prefs.getString(PREF_PLAYLIST, "");
                        else if (paneString.equals(PANE_SUBSCRIPTION))
                            paneString = prefs.getString(PREF_SUBSCRIPTION, "");

                        boolean flag = (Boolean) param.args[1];
                        int byte0;
                        if (flag)
                            byte0 = 2;
                        else
                            byte0 = 0;

                        Class paneDescriptorClass = findClass("com.google.android.apps.youtube.app.fragments.navigation.PaneDescriptor", lpparam.classLoader);
                        Class browseFragmentClass = findClass("com.google.android.apps.youtube.app.fragments.BrowseFragment", lpparam.classLoader);
                        Class dClass = findClass("com.google.android.apps.youtube.app.fragments.navigation.d", lpparam.classLoader);
                        Object object = callStaticMethod(dClass, "a", byte0);
                        Object paneDescriptor = newInstance(paneDescriptorClass, browseFragmentClass, object);
                        Class aClass1 = findClass("com.google.a.a.a.a.rl", lpparam.classLoader);
                        Object aClass1Instance = newInstance(aClass1);
                        Class aClass2 = findClass("com.google.a.a.a.a.bl", lpparam.classLoader);
                        setObjectField(aClass1Instance, "d", newInstance(aClass2));
                        Object c = getObjectField(aClass1Instance, "d");
                        setObjectField(c, "b", paneString);
                        callMethod(paneDescriptor, "setNavigationEndpoint", aClass1Instance);

                        return paneDescriptor;
                    }
                }
        );

/*
        findAndHookMethod("com.google.android.apps.youtube.app.fragments.BrowseFragment", lpparam.classLoader,
                "a", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        XposedBridge.log("Pane: " + getObjectField(param.thisObject, "h"));
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
