package si.screenme.app;
import android.app.*;import android.content.*;import android.os.*;
public class EditorTestActivity extends Activity{@Override public void onCreate(Bundle b){super.onCreate(b);startActivity(new Intent(this,EditorActivity.class).putExtra("dir",getIntent().getStringExtra("dir")));finish();}}
