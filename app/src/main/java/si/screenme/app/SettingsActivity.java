package si.screenme.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends androidx.activity.ComponentActivity {
    static final int TREE = 77;
    EditText update;
    Spinner size, color, singleAction, doubleAction, longAction;
    TextView syncStatus, usageStatus, updateStatus, notificationStatus;
    Switch turbo, autoProject;
    boolean fromOverlay;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        fromOverlay = getIntent().getBooleanExtra("fromOverlay", false);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { closeSettings(); }
        });
        Ui.bars(this);
        build();
    }

    void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 28));
        scroll.addView(root);

        TextView nav = Ui.nav(this, "Nastavitve");
        nav.setOnClickListener(v -> closeSettings());
        root.addView(nav);

        root.addView(Ui.label(this, "PLAVAJOČI GUMB"));
        LinearLayout overlay = Ui.card(this);
        overlay.addView(Ui.text(this, "Velikost gumba", 15, Ui.INK));
        size = new Spinner(this);
        String[] sizes = {"Majhen", "Srednji", "Velik"};
        size.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sizes));
        int sp = getSharedPreferences("screenme", 0).getInt("bubbleSize", 62);
        size.setSelection(sp <= 54 ? 0 : sp >= 70 ? 2 : 1);
        overlay.addView(size);
        overlay.addView(Ui.text(this, "Barva gumba", 15, Ui.INK));
        color = new Spinner(this);
        color.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Vijolična", "Turkizna", "Koralna"}));
        color.setSelection(getSharedPreferences("screenme", 0).getInt("bubbleColor", 0));
        overlay.addView(color);
        root.addView(overlay);
        Ui.margin(overlay, 0, 8, 0, 18);

        root.addView(Ui.label(this, "DEJANJA GUMBA"));
        LinearLayout gestures = Ui.card(this);
        singleAction = actionSpinner(gestures, "Enojni dotik", OverlayActionPrefs.KEY_SINGLE,
                OverlayActionPrefs.NOTE);
        doubleAction = actionSpinner(gestures, "Dvojni dotik", OverlayActionPrefs.KEY_DOUBLE,
                OverlayActionPrefs.EDIT);
        longAction = actionSpinner(gestures, "Dolgi dotik", OverlayActionPrefs.KEY_LONG,
                OverlayActionPrefs.HOME);
        gestures.addView(Ui.text(this,
                "Pritisk in poteg vedno premakne gumb. Spust na spodnji X ustavi zajem.",
                13, Ui.MUTED));
        root.addView(gestures);
        Ui.margin(gestures, 0, 8, 0, 18);

        root.addView(Ui.label(this, "SAMODEJNI PROJEKT"));
        LinearLayout detection = Ui.card(this);
        autoProject = new Switch(this);
        autoProject.setText(R.string.auto_project_toggle);
        autoProject.setTextSize(17);
        autoProject.setTextColor(Ui.INK);
        autoProject.setChecked(getSharedPreferences("screenme", 0)
                .getBoolean("autoProject", false));
        detection.addView(autoProject, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
        detection.addView(Ui.text(this,
                "Ob posnetku ScreenMe sam izbere ali ustvari projekt z imenom odprte aplikacije.",
                13, Ui.MUTED));
        usageStatus = Ui.text(this, "", 13, Ui.MUTED);
        detection.addView(usageStatus);
        Ui.margin(usageStatus, 0, 12, 0, 0);
        detection.addView(Ui.text(this,
                "Preverjeno na Galaxy S25 Ultra z ročno nameščenim APK-jem:\n1. Spodaj odpri prepoznavo, izberi ScreenMe in tapni zatemnjeno stikalo, da se pokaže sistemsko opozorilo.\n2. Ne zapri Nastavitev. Vrni se v Aplikacije → ScreenMe in izberi Dovoli omejene nastavitve. Nato se vrni na dostop do uporabe in vklopi ScreenMe.\n\nČe možnosti ni, v Varnost in zasebnost → Samodejno blokiranje začasno izklopi Največje omejitve in ponovi korake.",
                13, Ui.AMBER));
        TextView usage = Ui.button(this, "1 · SPROŽI SISTEMSKO OPOZORILO", false);
        usage.setOnClickListener(v -> {
            save(false);
            startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        });
        detection.addView(usage);
        Ui.margin(usage, 0, 12, 0, 0);
        TextView restricted = Ui.button(this, "2 · ODPRI PODATKE SCREENME", false);
        restricted.setOnClickListener(v -> {
            save(false);
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        });
        detection.addView(restricted);
        Ui.margin(restricted, 0, 12, 0, 0);
        TextView security = Ui.button(this, "VARNOST IN SAMODEJNO BLOKIRANJE", false);
        security.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)));
        detection.addView(security);
        Ui.margin(security, 0, 12, 0, 0);
        autoProject.setOnCheckedChangeListener((button, on) -> updateUsageStatus());
        updateUsageStatus();
        root.addView(detection);
        Ui.margin(detection, 0, 8, 0, 18);

        root.addView(Ui.label(this, "SINHRONIZACIJA IN TURBO"));
        LinearLayout sync = Ui.card(this);
        turbo = new Switch(this);
        turbo.setText(R.string.turbo_mode);
        turbo.setTextSize(17);
        turbo.setTextColor(Ui.INK);
        turbo.setChecked(getSharedPreferences("screenme", 0).getBoolean("turbo", false));
        sync.addView(turbo, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
        sync.addView(Ui.text(this,
                "Novi zapisi postanejo skupna delovna vrsta za Codex. Zahteva oblačno mapo.",
                13, Ui.MUTED));
        syncStatus = Ui.text(this, syncText(), 14, Ui.MUTED);
        sync.addView(syncStatus);
        Ui.margin(syncStatus, 0, 14, 0, 0);
        TextView choose = Ui.button(this, "IZBERI OBLAČNO MAPO", false);
        choose.setOnClickListener(v -> chooseTree());
        sync.addView(choose);
        Ui.margin(choose, 0, 14, 0, 0);
        TextView clear = Ui.text(this, "Odstrani povezavo z mapo", 13, Ui.RED);
        clear.setPadding(0, Ui.dp(this, 15), 0, 0);
        clear.setOnClickListener(v -> {
            getSharedPreferences("screenme", 0).edit().remove("syncTree").apply();
            syncStatus.setText(syncText());
        });
        sync.addView(clear);
        turbo.setOnCheckedChangeListener((v, on) -> {
            getSharedPreferences("screenme", 0).edit().putBoolean("turbo", on).apply();
            syncStatus.setText(syncText());
            if (on && getSharedPreferences("screenme", 0).getString("syncTree", "").isEmpty()) {
                Ui.toast(this, "Za Turbo izberi mapo ScreenMe Turbo na Google Drive.");
            }
        });
        root.addView(sync);
        Ui.margin(sync, 0, 8, 0, 18);

        root.addView(Ui.label(this, "POSODOBITVE"));
        LinearLayout updates = Ui.card(this);
        updates.addView(Ui.text(this, "Vir različic", 15, Ui.INK));
        update = new EditText(this);
        update.setSingleLine();
        update.setText(getSharedPreferences("screenme", 0)
                .getString("updateUrl", UpdateReceiver.DEFAULT_URL));
        update.setTextSize(13);
        updates.addView(update);
        updateStatus = Ui.text(this, UpdateChecker.lastStatus(this), 13, Ui.MUTED);
        updates.addView(updateStatus);
        Ui.margin(updateStatus, 0, 12, 0, 0);
        TextView check = Ui.button(this, "PREVERI ZDAJ", false);
        check.setOnClickListener(v -> {
            save(false);
            check.setEnabled(false);
            check.setText(R.string.update_checking);
            updateStatus.setText(R.string.update_connecting);
            UpdateChecker.check(this, false, (info, error) -> runOnUiThread(() -> {
                check.setEnabled(true);
                check.setText(R.string.update_check_now);
                updateStatus.setText(UpdateChecker.lastStatus(this));
                if (error != null) {
                    Ui.toast(this, "Preverjanje ni uspelo");
                } else if (info != null
                        && info.versionCode > UpdateChecker.installedVersionCode(this)) {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("ScreenMe " + info.versionName)
                            .setMessage("Nova različica je pripravljena za prenos.")
                            .setPositiveButton("Prenesi",
                                    (dialog, which) -> UpdateChecker.openDownload(this, info.apkUrl))
                            .setNegativeButton("Pozneje", null)
                            .show();
                } else {
                    Ui.toast(this, "ScreenMe je posodobljen");
                }
            }));
        });
        updates.addView(check);
        Ui.margin(check, 0, 10, 0, 0);
        notificationStatus = Ui.text(this, notificationText(), 13,
                UpdateChecker.canNotify(this) ? Ui.GREEN : Ui.AMBER);
        updates.addView(notificationStatus);
        Ui.margin(notificationStatus, 0, 14, 0, 0);
        if (!UpdateChecker.canNotify(this)) {
            TextView allowNotifications = Ui.button(this, "DOVOLI OBVESTILA", false);
            allowNotifications.setOnClickListener(v -> requestNotificationAccess());
            updates.addView(allowNotifications);
            Ui.margin(allowNotifications, 0, 10, 0, 0);
        }
        updates.addView(Ui.text(this,
                "ScreenMe preveri takoj ob odprtju in nato približno vsake 3 ure, tudi ko ni odprt.",
                12, Ui.MUTED));
        root.addView(updates);
        Ui.margin(updates, 0, 8, 0, 18);

        root.addView(Ui.label(this, "APLIKACIJA"));
        LinearLayout app = Ui.card(this);
        app.addView(Ui.text(this, versionText(), 15, Ui.INK));
        app.addView(Ui.text(this,
                "Zapisi: " + RecordItem.list(getExternalFilesDir(null), null).size()
                        + "  ·  Android " + Build.VERSION.RELEASE,
                13, Ui.MUTED));
        TextView intro = Ui.text(this, "Ponovno prikaži uvod", 14, Ui.PURPLE);
        intro.setPadding(0, Ui.dp(this, 16), 0, 0);
        intro.setOnClickListener(v -> {
            getSharedPreferences("screenme", 0).edit().putBoolean("onboarded", false).apply();
            startActivity(new Intent(this, OnboardingActivity.class));
        });
        app.addView(intro);
        root.addView(app);
        Ui.margin(app, 0, 8, 0, 18);

        TextView save = Ui.button(this, "SHRANI NASTAVITVE", true);
        save.setOnClickListener(v -> {
            save(true);
            closeSettings();
        });
        root.addView(save);
        setContentView(scroll);
    }

    String versionText() {
        try {
            android.content.pm.PackageInfo i = getPackageManager().getPackageInfo(getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28 ? i.getLongVersionCode() : i.versionCode;
            return "ScreenMe " + i.versionName + "  ·  build " + code;
        } catch (Exception e) {
            return "ScreenMe";
        }
    }

    String notificationText() {
        return UpdateChecker.canNotify(this)
                ? "✓ Opozorila o novih različicah so dovoljena."
                : "⚠ Opozorila so izklopljena; preverjanje deluje, obvestilo pa se ne more prikazati.";
    }

    void requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
                && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
                && !getSharedPreferences("screenme", 0)
                .getBoolean("notificationPrompted", false)) {
            getSharedPreferences("screenme", 0).edit()
                    .putBoolean("notificationPrompted", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8);
            return;
        }
        Intent settings = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(settings);
    }

    Spinner actionSpinner(LinearLayout parent, String title, String key, String defaultValue) {
        parent.addView(Ui.text(this, title, 15, Ui.INK));
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, OverlayActionPrefs.LABELS));
        spinner.setSelection(OverlayActionPrefs.selection(this, key, defaultValue));
        parent.addView(spinner);
        Ui.margin(spinner, 0, 0, 0, 12);
        return spinner;
    }

    String syncText() {
        boolean folder = !getSharedPreferences("screenme", 0).getString("syncTree", "").isEmpty();
        boolean on = getSharedPreferences("screenme", 0).getBoolean("turbo", false);
        if (!folder) {
            return on ? "⚠  Turbo čaka na mapo ScreenMe Turbo."
                    : "Oblačna mapa še ni izbrana. Uporabi Google Drive, Dropbox ali drugo mapo ponudnika dokumentov.";
        }
        return on ? "⚡  Turbo je povezan. Novi zapisi gredo v skupno delovno vrsto."
                : "✓  Sinhronizacija je nastavljena. Novi zapisi se samodejno kopirajo v izbrano mapo.";
    }

    void updateUsageStatus() {
        if (usageStatus == null || autoProject == null) return;
        if (!autoProject.isChecked()) {
            usageStatus.setText(R.string.auto_project_manual);
        } else if (ForegroundAppDetector.hasAccess(this)) {
            usageStatus.setText(R.string.auto_project_ready);
            usageStatus.setTextColor(Ui.GREEN);
        } else {
            usageStatus.setText(R.string.auto_project_permission_needed);
            usageStatus.setTextColor(Ui.AMBER);
        }
    }

    void chooseTree() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, TREE);
    }

    @Override protected void onActivityResult(int r, int result, Intent data) {
        super.onActivityResult(r, result, data);
        if (r == TREE && result == RESULT_OK && data != null && data.getData() != null) {
            Uri u = data.getData();
            getContentResolver().takePersistableUriPermission(u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getSharedPreferences("screenme", 0).edit().putString("syncTree", u.toString()).apply();
            syncStatus.setText(syncText());
            Ui.toast(this, "Sinhronizacijska mapa je nastavljena");
        }
    }

    void save(boolean toast) {
        int[] sizes = {50, 62, 74};
        OverlayActionPrefs.save(this,
                singleAction == null ? 0 : singleAction.getSelectedItemPosition(),
                doubleAction == null ? 1 : doubleAction.getSelectedItemPosition(),
                longAction == null ? 2 : longAction.getSelectedItemPosition());
        getSharedPreferences("screenme", 0).edit()
                .putInt("bubbleSize", sizes[size.getSelectedItemPosition()])
                .putInt("bubbleColor", color.getSelectedItemPosition())
                .putBoolean("turbo", turbo != null && turbo.isChecked())
                .putBoolean("autoProject", autoProject != null && autoProject.isChecked())
                .putString("updateUrl", update.getText().toString().trim())
                .apply();
        if (toast) Ui.toast(this, "Nastavitve so shranjene");
    }

    void closeSettings() {
        if (fromOverlay) moveTaskToBack(true);
        finish();
    }

    @Override protected void onResume() {
        super.onResume();
        if (fromOverlay) OverlayService.setUiHidden(true);
        updateUsageStatus();
        if (notificationStatus != null) {
            notificationStatus.setText(notificationText());
            notificationStatus.setTextColor(UpdateChecker.canNotify(this) ? Ui.GREEN : Ui.AMBER);
        }
        if (updateStatus != null) updateStatus.setText(UpdateChecker.lastStatus(this));
    }

    @Override protected void onPause() {
        if (fromOverlay) OverlayService.setUiHidden(false);
        if (update != null) save(false);
        super.onPause();
    }
}
