package si.screenme.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.nio.ByteBuffer;

public class OverlayService extends Service {
    static final String CHANNEL="screenme_capture";
    WindowManager wm; ImageView bubble; TextView trash; WindowManager.LayoutParams bp,tp;
    MediaProjection projection; ImageReader reader; VirtualDisplay display; Handler handler=new Handler(Looper.getMainLooper());
    DisplayMetrics metrics; volatile boolean pendingCapture; boolean pendingEdit;
    boolean dragging; float downX,downY; int originX,originY;

    @Override public void onCreate(){super.onCreate(); channel(); startForeground(1,notification()); wm=(WindowManager)getSystemService(WINDOW_SERVICE); addBubble();}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i!=null && "stop".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
        if(i!=null && projection==null){int code=i.getIntExtra("resultCode",0); Intent data=i.getParcelableExtra("data"); if(data!=null){projection=((MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(code,data);initCapture();}}
        return START_NOT_STICKY;
    }
    void addBubble(){
        bubble=new ImageView(this); bubble.setImageResource(android.R.drawable.ic_menu_camera); bubble.setPadding(dp(14),dp(14),dp(14),dp(14)); bubble.setBackgroundResource(si.screenme.app.R.drawable.bubble);
        bp=params(dp(62),dp(62));bp.gravity=Gravity.TOP|Gravity.START;bp.x=20;bp.y=180;wm.addView(bubble,bp);
        trash=new TextView(this);trash.setText("×");trash.setTextColor(Color.WHITE);trash.setTextSize(42);trash.setGravity(Gravity.CENTER);trash.setBackgroundResource(R.drawable.danger_circle);
        tp=params(dp(76),dp(76));tp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;tp.y=dp(26);trash.setVisibility(View.GONE);wm.addView(trash,tp);
        GestureDetector gd=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            public boolean onDown(android.view.MotionEvent e){downX=e.getRawX();downY=e.getRawY();originX=bp.x;originY=bp.y;return true;}
            public void onLongPress(android.view.MotionEvent e){dragging=true;trash.setVisibility(View.VISIBLE);wm.updateViewLayout(trash,tp);}
            public boolean onSingleTapConfirmed(android.view.MotionEvent e){capture(false);return true;}
            public boolean onDoubleTap(android.view.MotionEvent e){capture(true);return true;}
        });
        bubble.setOnTouchListener((v,e)->{
            gd.onTouchEvent(e);
            if(dragging && e.getAction()==MotionEvent.ACTION_MOVE){bp.x=originX+(int)(e.getRawX()-downX);bp.y=originY+(int)(e.getRawY()-downY);wm.updateViewLayout(bubble,bp);return true;}
            if(dragging && (e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)){
                dragging=false;trash.setVisibility(View.GONE);wm.updateViewLayout(trash,tp);
                Point size=new Point();wm.getDefaultDisplay().getSize(size);if(e.getRawY()>size.y-dp(135)&&Math.abs(e.getRawX()-size.x/2)<dp(100)){stopSelf();}return true;
            }return true;
        });
    }
    void capture(boolean edit){
        if(projection==null){Toast.makeText(this,"Ponovno zaženi ScreenMe in dovoli zajem zaslona.",Toast.LENGTH_LONG).show();return;}
        bubble.setVisibility(View.INVISIBLE);pendingEdit=edit;handler.postDelayed(()->pendingCapture=true,500);
    }
    void initCapture(){metrics=new DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(metrics);reader=ImageReader.newInstance(metrics.widthPixels,metrics.heightPixels,PixelFormat.RGBA_8888,2);reader.setOnImageAvailableListener(r->{Image image=null;boolean captured=false;try{image=r.acquireLatestImage();if(image==null||!pendingCapture)return;pendingCapture=false;captured=true;Image.Plane p=image.getPlanes()[0];ByteBuffer buf=p.getBuffer();int stride=p.getPixelStride(),row=p.getRowStride(),pad=row-stride*metrics.widthPixels;Bitmap wide=Bitmap.createBitmap(metrics.widthPixels+pad/stride,metrics.heightPixels,Bitmap.Config.ARGB_8888);wide.copyPixelsFromBuffer(buf);Bitmap shot=Bitmap.createBitmap(wide,0,0,metrics.widthPixels,metrics.heightPixels);wide.recycle();File dir=Storage.newRecord(this);Storage.bitmap(shot,new File(dir,"screenshot.png"));Intent next=new Intent(this,pendingEdit?EditorActivity.class:NoteActivity.class).putExtra("dir",dir.getAbsolutePath()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(next);}finally{if(image!=null)image.close();if(captured)handler.postDelayed(()->bubble.setVisibility(View.VISIBLE),400);}},handler);projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){stopSelf();}},handler);display=projection.createVirtualDisplay("ScreenMe",metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);}
    void cleanupCapture(){if(display!=null){display.release();display=null;}if(reader!=null){reader.close();reader=null;}}
    WindowManager.LayoutParams params(int w,int h){return new WindowManager.LayoutParams(w,h,Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);}
    void channel(){if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"ScreenMe zajem",NotificationManager.IMPORTANCE_LOW));}
    Notification notification(){Intent stop=new Intent(this,OverlayService.class).setAction("stop");PendingIntent p=PendingIntent.getService(this,3,stop,PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,CHANNEL).setContentTitle("ScreenMe deluje").setContentText("Dotakni se plavajočega gumba za posnetek").setSmallIcon(android.R.drawable.ic_menu_camera).addAction(new Notification.Action.Builder(null,"Ustavi",p).build()).build();}
    @Override public void onDestroy(){cleanupCapture();if(projection!=null)projection.stop();if(bubble!=null)wm.removeView(bubble);if(trash!=null)wm.removeView(trash);super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
    int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
