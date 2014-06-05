package com.germainz.yourtube;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.newInstance;

public class XposedMod implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.youtube"))
            return;

        findAndHookMethod("com.google.android.apps.youtube.app.GuideActivity", lpparam.classLoader,
                "D", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        Class paneDescriptorClass = findClass("com.google.android.apps.youtube.app.fragments.navigation.PaneDescriptor", lpparam.classLoader);
                        Class mySubscriptionsFragment = findClass("com.google.android.apps.youtube.app.fragments.MySubscriptionsFragment", lpparam.classLoader);
                        Class dClass = findClass("com.google.android.apps.youtube.app.fragments.navigation.d", lpparam.classLoader);
                        Object bundle = callStaticMethod(dClass, "a", 3);
                        Object panedescriptor = newInstance(paneDescriptorClass, mySubscriptionsFragment, bundle);
//                        String s = "FEwhat_to_watch";
//                        Class d = findClass("com.google.android.apps.youtube.app.fragments.navigation.d", lpparam.classLoader);
//                        Object panedescriptor = callStaticMethod(d, "b", s, false);
//                        Class kz = findClass("com.google.a.a.a.a.kz", lpparam.classLoader);
//                        Object kz1 = newInstance(kz);
//                        Class am = findClass("com.google.a.a.a.a.am", lpparam.classLoader);
//                        setObjectField(kz1, "c", newInstance(am));
//                        Object c = getObjectField(kz1, "c");
//                        setObjectField(c, "b", s);
//                        callMethod(panedescriptor, "setNavigationEndpoint", kz1);
                        return panedescriptor;
                    }
                }
        );

//        findAndHookMethod("com.google.android.apps.youtube.app.fragments.BrowseFragment", lpparam.classLoader,
//                "a", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
//                        XposedBridge.log("Z: " + getObjectField(param.thisObject, "Z"));
                        // What to watch: FEwhat_to_watch
                        // Watch Later: VLWL
                        // Playlist: VL + <playlist_id>
                        // get playlist id from: https://www.youtube.com/playlist?list=playlist_id
                        // Liked videos: same as above
                        // subscriptions: <subscription_id>
                        // get subscriptions id from: https://www.youtube.com/channel/subscription_id
                        // Browse channels: FEguide_builder
//                    }
//                }
//        );

    }
}
