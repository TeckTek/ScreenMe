package si.screenme.app;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.text.*;import android.view.*;import android.widget.*;import java.util.*;

public class RecordsActivity extends Activity{
    LinearLayout list;EditText search;Spinner filter;ArrayList<RecordItem> all=new ArrayList<>();
    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.bars(this);build();}
    @Override protected void onResume(){super.onResume();load();}
    void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.BG);root.setPadding(Ui.dp(this,20),0,Ui.dp(this,20),Ui.dp(this,18));root.addView(Ui.nav(this,"Knjižnica zapisov"));
        LinearLayout controls=Ui.card(this);search=new EditText(this);search.setHint("Išči po opombi ali projektu …");search.setSingleLine();search.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search,0,0,0);search.setCompoundDrawablePadding(Ui.dp(this,10));controls.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));filter=new Spinner(this);ArrayList<String> ps=ProjectStore.all(this);ps.add(0,"Vsi projekti");filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ps));controls.addView(filter,new LinearLayout.LayoutParams(-1,Ui.dp(this,50)));root.addView(controls);
        ScrollView s=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(0,Ui.dp(this,14),0,Ui.dp(this,24));s.addView(list);root.addView(s,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence x,int a,int c,int d){}public void onTextChanged(CharSequence x,int a,int b,int c){render();}public void afterTextChanged(Editable e){}});filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?>p){}public void onItemSelected(android.widget.AdapterView<?>p,View v,int pos,long id){render();}});
    }
    void load(){all=RecordItem.list(getExternalFilesDir(null),null);render();}
    void render(){
        if(list==null)return;list.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);String p=filter.getSelectedItem()==null?"Vsi projekti":filter.getSelectedItem().toString();int shown=0;
        for(RecordItem r:all){if(!p.equals("Vsi projekti")&&!p.equals(r.project)&&!ProjectStore.safe(p).equals(r.project))continue;if(!q.isEmpty()&&!(r.note+" "+r.project).toLowerCase(Locale.ROOT).contains(q))continue;list.addView(card(r));shown++;}
        if(shown==0){LinearLayout empty=Ui.card(this);ImageView i=new ImageView(this);i.setImageResource(R.drawable.ic_library);i.setColorFilter(Ui.PURPLE);empty.addView(i,new LinearLayout.LayoutParams(-1,Ui.dp(this,70)));TextView t=Ui.title(this,"Ni zapisov");t.setGravity(Gravity.CENTER);empty.addView(t);TextView b=Ui.text(this,q.isEmpty()?"Ko shraniš prvi posnetek, se bo pojavil tukaj.":"Poskusi z drugim iskalnim izrazom.",14,Ui.MUTED);b.setGravity(Gravity.CENTER);empty.addView(b);list.addView(empty);Ui.margin(empty,0,40,0,0);}
    }
    View card(RecordItem r){
        LinearLayout c=Ui.card(this);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=4;image.setImageBitmap(BitmapFactory.decodeFile(r.image.getAbsolutePath(),o));c.addView(image,new LinearLayout.LayoutParams(Ui.dp(this,96),Ui.dp(this,96)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);TextView title=Ui.text(this,r.title,16,Ui.INK);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setMaxLines(2);info.addView(title);TextView meta=Ui.text(this,r.project+"  ·  "+r.severity,12,Ui.PURPLE);info.addView(meta);Ui.margin(meta,0,6,0,0);info.addView(Ui.text(this,r.date(),12,Ui.MUTED));c.addView(info,Ui.weight(0,-2,1));Ui.margin(info,14,0,6,0);c.addView(Ui.text(this,"›",32,Ui.PURPLE));c.setOnClickListener(v->startActivity(new Intent(this,RecordDetailActivity.class).putExtra("dir",r.dir.getAbsolutePath()).putExtra("project",r.project)));Ui.margin(c,0,0,0,10);return c;
    }
}
