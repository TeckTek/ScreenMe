package si.screenme.app;

import android.content.Context;
import android.content.SharedPreferences;

final class OverlayActionPrefs {
    static final String KEY_SINGLE = "overlayActionSingle";
    static final String KEY_DOUBLE = "overlayActionDouble";
    static final String KEY_LONG = "overlayActionLong";

    static final String NOTE = "note";
    static final String EDIT = "edit";
    static final String HOME = "home";
    static final String SETTINGS = "settings";
    static final String NONE = "none";

    static final String[] VALUES = {NOTE, EDIT, HOME, SETTINGS, NONE};
    static final String[] LABELS = {
            "Posnetek in hitra opomba",
            "Posnetek in urejevalnik",
            "Odpri ScreenMe / ustavi zajem",
            "Odpri nastavitve",
            "Brez dejanja"
    };

    private OverlayActionPrefs() {}

    static String get(Context context, String key, String defaultValue) {
        return prefs(context).getString(key, defaultValue);
    }

    static int selection(Context context, String key, String defaultValue) {
        String selected = get(context, key, defaultValue);
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].equals(selected)) return i;
        }
        return selectionFor(defaultValue);
    }

    static String valueAt(int position, String defaultValue) {
        return position >= 0 && position < VALUES.length ? VALUES[position] : defaultValue;
    }

    static void save(Context context, int single, int doubleTap, int longPress) {
        prefs(context).edit()
                .putString(KEY_SINGLE, valueAt(single, NOTE))
                .putString(KEY_DOUBLE, valueAt(doubleTap, EDIT))
                .putString(KEY_LONG, valueAt(longPress, HOME))
                .apply();
    }

    private static int selectionFor(String value) {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].equals(value)) return i;
        }
        return 0;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("screenme", 0);
    }
}
