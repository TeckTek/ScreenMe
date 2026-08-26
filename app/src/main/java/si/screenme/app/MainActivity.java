package si.screenme.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int CAPTURE = 42;
    private EditText profile, updateUrl;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); buildUi(); UpdateScheduler.schedule(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24),dp(36),dp(24),dp(24));
        TextView title = text("ScreenMe", 32); title.setTextColor(Color.rgb(103,80,164)); root.addView(title);
        root.addView(text("Hitro zabeleži napako s posnetkom zaslona in opombo.", 16), lp(-1,-2,0,0,0,24));
        root.addView(text("Profil projekta", 14));
        profile = new EditText(this); profile.setHint("npr. Knjizni-Svet"); profile.setSingleLine();
        profile.setText(getPreferences(0).getString("profile", "")); root.addView(profile, lp(-1,-2,0,8,0,16));
        root.addView(text("Vir posodobitev (JSON URL, neobvezno)", 14));
        updateUrl=new EditText(this);updateUrl.setHint("https://…/screenme-update.json");updateUrl.setSingleLine();updateUrl.setText(getSharedPreferences("screenme",0).getString("updateUrl",UpdateReceiver.DEFAULT_URL));root.addView(updateUrl,lp(-1,-2,0,8,0,16));
        Button start = new Button(this); start.setText("ZAČNI ZAJEM"); start.setOnClickListener(v -> begin()); root.addView(start);
        Button records = new Button(this); records.setText("IZBERI MAPO ZA SINHRONIZACIJO"); records.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i, 77);
        }); root.addView(records);
        TextView info = text("En klik: posnetek + opomba\nDvojni klik: posnetek + risanje + opomba\nDolg pritisk: premikanje gumba", 14);
        root.addView(info, lp(-1,-2,0,24,0,0)); setContentView(root);
    }

    private void begin() {
        String p = profile.getText().toString().trim();
        if (p.isEmpty()) { profile.setError("Vnesi profil"); return; }
        getPreferences(0).edit().putString("profile", p).apply();
        getSharedPreferences("screenme",0).edit().putString("profile", p).apply();
        getSharedPreferences("screenme",0).edit().putString("updateUrl",updateUrl.getText().toString().trim()).apply();
        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));
            Toast.makeText(this,"Dovoli prikaz čez druge aplikacije, nato znova pritisni Začni zajem.",Toast.LENGTH_LONG).show(); return;
        }
        MediaProjectionManager m = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(m.createScreenCaptureIntent(), CAPTURE);
    }

    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(req==77 && result==RESULT_OK && data!=null && data.getData()!=null){Uri u=data.getData();getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);getSharedPreferences("screenme",0).edit().putString("syncTree",u.toString()).apply();Toast.makeText(this,"Sinhronizacijska mapa je nastavljena.",Toast.LENGTH_LONG).show();return;}
        if(req==CAPTURE && result==RESULT_OK && data!=null) {
            Intent s=new Intent(this,OverlayService.class).putExtra("resultCode",result).putExtra("data",data);
            startForegroundService(s); Toast.makeText(this,"ScreenMe je pripravljen.",Toast.LENGTH_SHORT).show(); moveTaskToBack(true);
        }
    }
    TextView text(String s,int size){ TextView v=new TextView(this);v.setText(s);v.setTextSize(size);return v; }
    LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}
