package si.screenme.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NoteActivity extends androidx.activity.ComponentActivity {
    static final int SPEECH = 88;
    File dir;
    boolean edited;
    EditText title, note;
    Spinner severity;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmDiscard(); }
        });
        Ui.bars(this);
        dir = new File(getIntent().getStringExtra("dir"));
        edited = getIntent().getBooleanExtra("edited", false);
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        OverlayService.setUiHidden(true);
    }

    @Override protected void onPause() {
        OverlayService.setUiHidden(false);
        super.onPause();
    }

    void build() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 12));
        scroll.addView(root);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(Ui.nav(this, "Nova opomba"));

        LinearLayout preview = Ui.card(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        File pic = new File(dir, edited ? "annotated.png" : "screenshot.png");
        image.setImageBitmap(BitmapFactory.decodeFile(pic.getAbsolutePath()));
        preview.addView(image, new LinearLayout.LayoutParams(-1, Ui.dp(this, 240)));
        root.addView(preview);

        LinearLayout form = Ui.card(this);
        form.addView(Ui.label(this, "NASLOV NAPAKE"));
        title = new EditText(this);
        title.setHint("Kratek in jasen naslov");
        title.setSingleLine();
        title.setTextSize(17);
        form.addView(title, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));

        form.addView(Ui.label(this, "RESNOST"));
        severity = new Spinner(this);
        severity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Nizka", "Običajna", "Visoka", "Kritična"}));
        severity.setSelection(1);
        form.addView(severity, new LinearLayout.LayoutParams(-1, Ui.dp(this, 50)));

        form.addView(Ui.label(this, "OPIS IN KORAKI"));
        TextView voice = Ui.button(this, "NAREKUJ", false);
        voice.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic, 0, 0, 0);
        voice.setCompoundDrawablePadding(Ui.dp(this, 9));
        voice.setOnClickListener(v -> dictate());
        form.addView(voice, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));
        note = new EditText(this);
        note.setHint("Kaj se je zgodilo? Kaj si pričakoval? Kako napako ponovimo?");
        note.setGravity(Gravity.TOP);
        note.setMinLines(5);
        note.setTextSize(15);
        form.addView(note);
        root.addView(form);
        Ui.margin(form, 0, 14, 0, 0);

        LinearLayout footer = Ui.row(this);
        footer.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 16));
        footer.setBackgroundColor(Ui.CARD);
        footer.setElevation(Ui.dp(this, 8));
        LinearLayout row = Ui.row(this);
        TextView cancel = Ui.button(this, "ZAVRZI", false);
        cancel.setTextColor(Ui.RED);
        cancel.setOnClickListener(v -> confirmDiscard());
        boolean sends = !getSharedPreferences("screenme", 0).getString("syncTree", "").isEmpty();
        TextView save = Ui.button(this, sends ? "SHRANI IN POŠLJI" : "SHRANI ZAPIS", true);
        save.setOnClickListener(v -> save());
        row.addView(cancel, Ui.weight(0, Ui.dp(this, 56), 1));
        row.addView(save, Ui.weight(0, Ui.dp(this, 56), 2));
        Ui.margin(save, 10, 0, 0, 0);
        footer.addView(row, new LinearLayout.LayoutParams(-1, -2));
        page.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        int footerLeft = Ui.dp(this, 20), footerTop = Ui.dp(this, 12);
        int footerRight = Ui.dp(this, 20), footerBottom = Ui.dp(this, 16);
        ViewCompat.setOnApplyWindowInsetsListener(page, (view, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            footer.setPadding(footerLeft, footerTop, footerRight, footerBottom + ime.bottom);
            return insets;
        });
        setContentView(page);
        title.requestFocus();
    }

    void dictate() {
        Intent speech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sl-SI")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Opiši napako")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        if (Build.VERSION.SDK_INT >= 33) {
            speech.putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                    RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY);
        }
        try {
            startActivityForResult(speech, SPEECH);
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "Na telefonu ni nameščenega prepoznavalnika govora.");
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != SPEECH || result != Activity.RESULT_OK || data == null) return;
        ArrayList<String> values = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (values == null || values.isEmpty()) return;
        String spoken = values.get(0).trim();
        if (spoken.isEmpty()) return;
        String old = note.getText().toString().trim();
        note.setText(old.isEmpty() ? spoken : old + "\n" + spoken);
        note.requestFocus();
        note.setSelection(note.length());
    }

    void save() {
        String t = title.getText().toString().trim();
        String n = note.getText().toString().trim();
        String sev = severity.getSelectedItem().toString();
        if (t.isEmpty()) {
            title.setError("Dodaj kratek naslov");
            title.requestFocus();
            return;
        }
        if (n.isEmpty()) {
            note.setError("Dodaj opis napake");
            note.requestFocus();
            return;
        }
        String project = ProjectStore.current(this);
        String md = "# " + t + "\n\n**Projekt:** " + project + "  \n**Resnost:** " + sev
                + "\n\n## Opis\n\n" + n + "\n";
        Storage.text(new File(dir, "note.md"), md);
        Storage.text(new File(dir, "metadata.json"), Storage.json(project, t, sev, n, edited));
        Storage.sync(this, dir);
        boolean turbo = getSharedPreferences("screenme", 0).getBoolean("turbo", false);
        boolean folder = !getSharedPreferences("screenme", 0).getString("syncTree", "").isEmpty();
        Ui.toast(this, turbo ? (folder ? "Zapis je poslan v Turbo vrsto"
                : "Zapis je lokalen · Turbo čaka na mapo") : "Zapis je shranjen");
        returnToSource();
    }

    void returnToSource() {
        moveTaskToBack(true);
        finish();
    }

    void confirmDiscard() {
        new AlertDialog.Builder(this)
                .setTitle("Zavrnem posnetek?")
                .setMessage("Ta posnetek še ni shranjen.")
                .setPositiveButton("Zavrzi", (d, w) -> { EditorActivity.delete(dir); returnToSource(); })
                .setNegativeButton("Nadaljuj urejanje", null)
                .show();
    }
}
