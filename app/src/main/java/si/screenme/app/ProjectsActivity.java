package si.screenme.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class ProjectsActivity extends androidx.activity.ComponentActivity {
    LinearLayout root;

    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Ui.bars(this);
        build();
    }

    void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 28));
        scroll.addView(root);

        root.addView(Ui.nav(this, "Projekti"));
        root.addView(Ui.text(this,
                "Izberi aktivni projekt, ga preimenuj ali varno odstrani.", 14, Ui.MUTED));

        TextView add = Ui.button(this, "+ NOV PROJEKT", true);
        add.setOnClickListener(v -> newProject());
        root.addView(add);
        Ui.margin(add, 0, 18, 0, 20);

        ArrayList<String> projects = ProjectStore.all(this);
        if (projects.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            TextView title = Ui.title(this, "Ni projektov");
            title.setTextSize(21);
            empty.addView(title);
            empty.addView(Ui.text(this,
                    "Ustvari prvi projekt, da bodo novi zapisi pravilno ločeni.",
                    14, Ui.MUTED));
            root.addView(empty);
        } else {
            String current = ProjectStore.current(this);
            for (String project : projects) addProjectCard(project, project.equals(current));
        }

        setContentView(scroll);
    }

    void addProjectCard(String project, boolean active) {
        LinearLayout card = Ui.card(this);
        LinearLayout heading = Ui.row(this);
        TextView name = Ui.text(this, project, 19, Ui.INK);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.addView(name, Ui.weight(0, -2, 1));
        if (active) {
            TextView badge = Ui.text(this, "AKTIVEN", 11, Ui.GREEN);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            heading.addView(badge);
        }
        card.addView(heading);

        int count = RecordItem.list(getExternalFilesDir(null), project).size();
        card.addView(Ui.text(this,
                count == 1 ? "1 shranjen zapis" : count + " shranjenih zapisov",
                13, Ui.MUTED));

        if (!active) {
            TextView select = Ui.button(this, "IZBERI KOT AKTIVNEGA", false);
            select.setOnClickListener(v -> {
                ProjectStore.select(this, project);
                build();
            });
            card.addView(select);
            Ui.margin(select, 0, 14, 0, 0);
        }

        LinearLayout actions = Ui.row(this);
        TextView rename = Ui.button(this, "PREIMENUJ", false);
        rename.setOnClickListener(v -> renameProject(project));
        actions.addView(rename, Ui.weight(0, Ui.dp(this, 50), 1));
        TextView delete = Ui.button(this, "IZBRIŠI", false);
        delete.setTextColor(Ui.RED);
        delete.setOnClickListener(v -> deleteProject(project, count));
        actions.addView(delete, Ui.weight(0, Ui.dp(this, 50), 1));
        Ui.margin(delete, 10, 0, 0, 0);
        card.addView(actions);
        Ui.margin(actions, 0, 10, 0, 0);

        root.addView(card);
        Ui.margin(card, 0, 0, 0, 12);
    }

    void newProject() {
        editNameDialog("Nov projekt", "Ustvari", "", name -> {
            if (ProjectStore.contains(this, name)) {
                Ui.toast(this, "Projekt s tem imenom že obstaja");
                return;
            }
            ProjectStore.select(this, name);
            build();
        });
    }

    void renameProject(String project) {
        editNameDialog("Preimenuj projekt", "Shrani", project, name -> {
            if (project.equals(name)) return;
            if (!ProjectStore.rename(this, project, name)) {
                Ui.toast(this, "Preimenovanje ni uspelo. Preveri ime ali obstoječi projekt.");
                return;
            }
            Ui.toast(this, "Projekt je preimenovan");
            build();
        });
    }

    void deleteProject(String project, int count) {
        if (count == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Izbrišem projekt?")
                    .setMessage("Projekt »" + project
                            + "« nima lokalnih zapisov in bo odstranjen s seznama.")
                    .setPositiveButton("Izbriši projekt", (dialog, which) -> {
                        if (!ProjectStore.deleteWithRecords(this, project)) {
                            Ui.toast(this, "Projekta ni bilo mogoče izbrisati");
                            return;
                        }
                        Ui.toast(this, "Projekt je izbrisan");
                        build();
                    })
                    .setNegativeButton("Prekliči", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Odstrani projekt")
                .setMessage("Projekt »" + project + "« vsebuje " + count
                        + " lokalnih zapisov. Kaj želiš narediti?")
                .setItems(new String[]{
                        "Odstrani samo s seznama",
                        "Izbriši projekt in vse lokalne zapise"
                }, (dialog, which) -> {
                    if (which == 0) {
                        ProjectStore.remove(this, project);
                        build();
                    } else {
                        confirmPermanentDelete(project, count);
                    }
                })
                .setNegativeButton("Prekliči", null)
                .show();
    }

    void confirmPermanentDelete(String project, int count) {
        new AlertDialog.Builder(this)
                .setTitle("Trajno izbrišem?")
                .setMessage("Izbrisan bo projekt »" + project + "« in " + count
                        + " lokalnih zapisov. Kopije v oblačni mapi ne bodo izbrisane.")
                .setPositiveButton("Trajno izbriši", (dialog, which) -> {
                    if (!ProjectStore.deleteWithRecords(this, project)) {
                        Ui.toast(this, "Vseh lokalnih datotek ni bilo mogoče izbrisati");
                        return;
                    }
                    Ui.toast(this, "Projekt je izbrisan");
                    build();
                })
                .setNegativeButton("Prekliči", null)
                .show();
    }

    void editNameDialog(String title, String positive, String initial, NameAction action) {
        EditText input = new EditText(this);
        input.setSingleLine();
        input.setHint("Ime projekta");
        input.setText(initial);
        input.setSelection(input.length());
        input.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(positive, null)
                .setNegativeButton("Prekliči", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        input.setError("Vpiši ime projekta");
                        return;
                    }
                    action.run(name);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    interface NameAction { void run(String name); }
}
