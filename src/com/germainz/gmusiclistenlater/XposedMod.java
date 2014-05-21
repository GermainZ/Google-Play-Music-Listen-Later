package com.germainz.gmusiclistenlater;

import android.os.Bundle;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class XposedMod implements IXposedHookLoadPackage {

    private static final XSharedPreferences PREFS = new XSharedPreferences("com.germainz.gmusiclistenlater");
    private static final String PREF_DEFAULT_PANE= "pref_default_pane";
    private static final String PREF_DEFAULT_LIBRARY_TAB= "pref_library_default_tab";
    private static final String DEFAULT_PANE= "MY_LIBRARY";
    private static final String DEFAULT_LIBRARY_TAB = "ARTISTS";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.music"))
            return;

        findAndHookMethod("com.google.android.music.ui.HomeActivity", lpparam.classLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        PREFS.reload();
                        Class<?> screenClass = findClass("com.google.android.music.ui.HomeActivity.Screen", lpparam.classLoader);
                        Object screen = getStaticObjectField(screenClass,
                                PREFS.getString(PREF_DEFAULT_PANE, DEFAULT_PANE));
                        setObjectField(param.thisObject, "mTargetScreen", screen);
                    }
                }
        );

        findAndHookConstructor("com.google.android.music.ui.MyLibraryFragment", lpparam.classLoader,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        PREFS.reload();
                        setObjectField(param.thisObject, "mDefaultTab",
                                PREFS.getString(PREF_DEFAULT_LIBRARY_TAB, DEFAULT_LIBRARY_TAB));
                    }
                }
        );


    }
}
