package si.screenme.app;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class ImageImporter {
    private ImageImporter() {}

    static File importOne(Context context, Uri source, String project) throws Exception {
        String displayName = displayName(context, source);
        File record = Storage.newRecord(context, project);
        File image = new File(record, "screenshot.png");
        try (InputStream input = context.getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(image)) {
            if (input == null) throw new IOException("Slike ni mogoče odpreti");
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) > 0) output.write(buffer, 0, count);
        } catch (Exception error) {
            ProjectStore.deleteTree(record);
            throw error;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image.getAbsolutePath(), bounds);
        if (bounds.outWidth < 1 || bounds.outHeight < 1) {
            ProjectStore.deleteTree(record);
            throw new IOException("Izbrana datoteka ni veljavna slika");
        }
        String title = title(displayName);
        String note = "Uvožena slika: " + displayName;
        Storage.text(new File(record, "note.md"), "# " + title + "\n\n**Projekt:** "
                + project + "  \n**Resnost:** Običajna\n\n## Opis\n\n" + note + "\n");
        Storage.text(new File(record, "metadata.json"),
                Storage.json(project, title, "Običajna", note, false));
        Storage.sync(context, record);
        return record;
    }

    private static String displayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                if (value != null && !value.trim().isEmpty()) return value.trim();
            }
        } catch (Exception ignored) {}
        String fallback = uri.getLastPathSegment();
        return fallback == null || fallback.trim().isEmpty() ? "slika" : fallback;
    }

    private static String title(String name) {
        String value = name.replaceFirst("\\.[^.]+$", "").replaceAll("[_-]+", " ").trim();
        if (value.isEmpty()) value = "Uvožena slika";
        if (value.length() > 72) value = value.substring(0, 71).trim() + "…";
        return value;
    }
}
