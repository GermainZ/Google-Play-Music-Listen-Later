package com.germainz.gmusiclistenlater;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class XposedMod implements IXposedHookLoadPackage {

    private static XSharedPreferences PREFS;
    private static final String PREF_DEFAULT_PANE = "pref_default_pane";
    private static final String PREF_DEFAULT_PLAYLIST = "pref_default_playlist";
    private static final String DEFAULT_PANE = "MY_LIBRARY";

    private boolean mFirstLaunch;
    private String mDefaultPlaylist;

    private static final Map<String, ScreenFragment> FRAGMENTS;

    private static final class ScreenFragment {
        String prefDefaultTab;
        String defaultTab;

        public ScreenFragment(String prefDefaultTab, String defaultTab) {
            this.prefDefaultTab = prefDefaultTab;
            this.defaultTab = defaultTab;
        }
    }

    static {
        FRAGMENTS = new HashMap<String, ScreenFragment>();
        FRAGMENTS.put("com.google.android.music.ui.MyLibraryFragment",
                new ScreenFragment("pref_library_default_tab", "artists"));
        FRAGMENTS.put("com.google.android.music.ui.InstantMixesFragment",
                new ScreenFragment("pref_instant_mixes_default_tab", "instant_mixes"));
        FRAGMENTS.put("com.google.android.music.ui.RadioFragment",
                new ScreenFragment("pref_radio_default_tab", "stations"));
        FRAGMENTS.put("com.google.android.music.ui.ExploreFragment",
                new ScreenFragment("pref_explore_default_tab", "top_charts"));
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.music"))
            return;

        PREFS = new XSharedPreferences("com.germainz.gmusiclistenlater");

        findAndHookMethod("com.google.android.music.ui.HomeActivity", lpparam.classLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        PREFS.reload();
                        mFirstLaunch = true;
                        mDefaultPlaylist = PREFS.getString(PREF_DEFAULT_PLAYLIST, "");
                        Class<?> screenClass = findClass("com.google.android.music.ui.HomeActivity.Screen", lpparam.classLoader);
                        Object screen = getStaticObjectField(screenClass,
                                PREFS.getString(PREF_DEFAULT_PANE, DEFAULT_PANE));
                        setObjectField(param.thisObject, "mTargetScreen", screen);
                    }
                }
        );

        findAndHookMethod("com.google.android.music.ui.PlaylistClustersFragment", lpparam.classLoader,
                "populatePlaylistDocument", "com.google.android.music.ui.cardlib.model.Document", Cursor.class, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!mFirstLaunch)
                            return;
                        Object document = param.args[0];
                        if (getObjectField(document, "mTitle").equals(mDefaultPlaylist)) {
                            mFirstLaunch = false;
                            callStaticMethod(findClass("com.google.android.music.ui.cardlib.model.DocumentClickHandler", lpparam.classLoader), "onDocumentClick", param.args[2], document);
                        }
                    }
                }
        );

        for (String className : FRAGMENTS.keySet()) {
            final ScreenFragment screenFragment = FRAGMENTS.get(className);
            findAndHookConstructor(className, lpparam.classLoader,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            PREFS.reload();
                            setObjectField(param.thisObject, "mDefaultTab",
                                    PREFS.getString(screenFragment.prefDefaultTab, screenFragment.defaultTab));
                        }
                    }
            );
        }

    }
}
