package si.screenme.app;

import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.ColorDrawable;import android.os.*;import android.view.*;import android.widget.*;import java.io.*;import java.util.*;

public class EditorActivity extends Activity {
    DrawView draw; File dir;
    @Override public void onCreate(Bundle b){super.onCreate(b);dir=new File(getIntent().getStringExtra("dir"));Bitmap src=BitmapFactory.decodeFile(new File(dir,"screenshot.png").getAbsolutePath());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.DKGRAY);
        LinearLayout bar=new LinearLayout(this);String[] names={"✎ Svinčnik","○ Elipsa","Barva","↶","↷"};for(String n:names){Button x=new Button(this);x.setText(n);bar.addView(x,new LinearLayout.LayoutParams(0,-2,1));if(n.startsWith("✎"))x.setOnClickListener(v->draw.tool=0);else if(n.startsWith("○"))x.setOnClickListener(v->draw.tool=1);else if(n.equals("Barva"))x.setOnClickListener(v->colors());else if(n.equals("↶"))x.setOnClickListener(v->draw.undo());else x.setOnClickListener(v->draw.redo());}root.addView(bar);
        draw=new DrawView(this,src);root.addView(draw,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(this);Button cancel=new Button(this);cancel.setText("PREKLIČI");cancel.setOnClickListener(v->{delete(dir);finish();});Button save=new Button(this);save.setText("SHRANI IN DODAJ OPOMBO");save.setOnClickListener(v->{Storage.bitmap(draw.flatten(),new File(dir,"annotated.png"));startActivity(new Intent(this,NoteActivity.class).putExtra("dir",dir.getAbsolutePath()).putExtra("edited",true));finish();});actions.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));actions.addView(save,new LinearLayout.LayoutParams(0,-2,2));root.addView(actions);setContentView(root);
    }
    void colors(){final int[] cs={Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.BLACK,Color.WHITE};LinearLayout l=new LinearLayout(this);for(int c:cs){Button b=new Button(this);b.setBackgroundColor(c);b.setOnClickListener(v->{draw.color=((ColorDrawable)v.getBackground()).getColor();((Dialog)v.getTag()).dismiss();});l.addView(b,new LinearLayout.LayoutParams(0,80,1));}Dialog d=new AlertDialog.Builder(this).setTitle("Izberi barvo").setView(l).create();for(int i=0;i<l.getChildCount();i++)l.getChildAt(i).setTag(d);d.show();}
    static void delete(File f){File[] a=f.listFiles();if(a!=null)for(File x:a)x.delete();f.delete();}
    @Override public void onBackPressed(){new AlertDialog.Builder(this).setMessage("Zavrnem ta posnetek?").setPositiveButton("Zavrzi",(d,w)->{delete(dir);finish();}).setNegativeButton("Nazaj",null).show();}

    static class DrawView extends View{
        Bitmap base,layer;Canvas canvas;Paint paint=new Paint(3);Path path;float sx,sy,cx,cy;int tool=0,color=Color.RED;ArrayList<Bitmap> undo=new ArrayList<>(),redo=new ArrayList<>();
        DrawView(Context c,Bitmap b){super(c);base=b;layer=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);canvas=new Canvas(layer);paint.setStrokeWidth(Math.max(6,b.getWidth()/180f));paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);setBackgroundColor(Color.BLACK);}
        protected void onDraw(Canvas c){super.onDraw(c);float s=Math.min(getWidth()/(float)base.getWidth(),getHeight()/(float)base.getHeight());float ox=(getWidth()-base.getWidth()*s)/2,oy=(getHeight()-base.getHeight()*s)/2;c.save();c.translate(ox,oy);c.scale(s,s);c.drawBitmap(base,0,0,null);c.drawBitmap(layer,0,0,null);paint.setColor(color);if(tool==1&&sx!=cx)c.drawOval(Math.min(sx,cx),Math.min(sy,cy),Math.max(sx,cx),Math.max(sy,cy),paint);c.restore();}
        public boolean onTouchEvent(android.view.MotionEvent e){float scale=Math.min(getWidth()/(float)base.getWidth(),getHeight()/(float)base.getHeight());float ox=(getWidth()-base.getWidth()*scale)/2,oy=(getHeight()-base.getHeight()*scale)/2;float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;if(e.getAction()==0){saveUndo();sx=cx=x;sy=cy=y;path=new Path();path.moveTo(x,y);return true;}if(e.getAction()==2){cx=x;cy=y;if(tool==0){path.lineTo(x,y);paint.setColor(color);canvas.drawPath(path,paint);path=new Path();path.moveTo(x,y);}invalidate();return true;}if(e.getAction()==1){cx=x;cy=y;paint.setColor(color);if(tool==1)canvas.drawOval(Math.min(sx,cx),Math.min(sy,cy),Math.max(sx,cx),Math.max(sy,cy),paint);invalidate();return true;}return true;}
        void saveUndo(){undo.add(layer.copy(Bitmap.Config.ARGB_8888,true));if(undo.size()>20)undo.remove(0);redo.clear();}
        void undo(){if(undo.isEmpty())return;redo.add(layer.copy(Bitmap.Config.ARGB_8888,true));layer=undo.remove(undo.size()-1);canvas=new Canvas(layer);invalidate();}
        void redo(){if(redo.isEmpty())return;undo.add(layer.copy(Bitmap.Config.ARGB_8888,true));layer=redo.remove(redo.size()-1);canvas=new Canvas(layer);invalidate();}
        Bitmap flatten(){Bitmap out=base.copy(Bitmap.Config.ARGB_8888,true);new Canvas(out).drawBitmap(layer,0,0,null);return out;}
    }
}
