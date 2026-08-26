package si.screenme.app;

import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;

final class Ui {
    static final int INK=Color.rgb(31,27,46), MUTED=Color.rgb(106,101,121), PURPLE=Color.rgb(98,70,199), PURPLE_2=Color.rgb(126,92,224), BG=Color.rgb(248,247,252), CARD=Color.WHITE, LINE=Color.rgb(231,227,240), GREEN=Color.rgb(26,145,99), RED=Color.rgb(204,58,75), AMBER=Color.rgb(224,153,37), DARK=Color.rgb(21,17,38);
    static int dp(Context c,int n){return(int)(n*c.getResources().getDisplayMetrics().density+.5f);}
    static TextView text(Context c,String s,float size,int color){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("kern");return v;}
    static TextView title(Context c,String s){TextView v=text(c,s,26,INK);v.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return v;}
    static TextView label(Context c,String s){TextView v=text(c,s,12,MUTED);v.setAllCaps(true);v.setLetterSpacing(.08f);v.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return v;}
    static GradientDrawable shape(int color,int radius,Context c){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(c,radius));return g;}
    static Drawable outlined(Context c,int color,int stroke){GradientDrawable g=shape(color,16,c);g.setStroke(dp(c,stroke),LINE);return g;}
    static Drawable ripple(Context c,int color,int radius){return new RippleDrawable(android.content.res.ColorStateList.valueOf(0x22FFFFFF),shape(color,radius,c),null);}
    static TextView button(Context c,String s,boolean primary){TextView v=text(c,s,14,primary?Color.WHITE:PURPLE);v.setGravity(Gravity.CENTER);v.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);v.setLetterSpacing(.03f);v.setMinHeight(dp(c,52));v.setBackground(primary?ripple(c,PURPLE,16):outlined(c,CARD,1));v.setClickable(true);v.setFocusable(true);v.setPadding(dp(c,16),0,dp(c,16),0);return v;}
    static LinearLayout card(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(c,18),dp(c,18),dp(c,18),dp(c,18));v.setBackground(outlined(c,CARD,1));v.setElevation(dp(c,1));return v;}
    static LinearLayout row(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
    static LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    static LinearLayout.LayoutParams weight(int w,int h,float x){return new LinearLayout.LayoutParams(w,h,x);}
    static void margin(View v,int l,int t,int r,int b){ViewGroup.LayoutParams raw=v.getLayoutParams();ViewGroup.MarginLayoutParams p=raw instanceof ViewGroup.MarginLayoutParams?(ViewGroup.MarginLayoutParams)raw:new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(v.getContext(),l),dp(v.getContext(),t),dp(v.getContext(),r),dp(v.getContext(),b));v.setLayoutParams(p);}
    static void bars(Activity a){androidx.core.view.WindowCompat.setDecorFitsSystemWindows(a.getWindow(),true);a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}
    static void barsDark(Activity a){androidx.core.view.WindowCompat.setDecorFitsSystemWindows(a.getWindow(),true);a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);a.getWindow().getDecorView().setPadding(0,dp(a,34),0,dp(a,18));}
    static void insets(View v){final int l=v.getPaddingLeft(),t=v.getPaddingTop(),r=v.getPaddingRight(),b=v.getPaddingBottom();v.setOnApplyWindowInsetsListener((x,i)->{x.setPadding(l+i.getSystemWindowInsetLeft(),t+i.getSystemWindowInsetTop(),r+i.getSystemWindowInsetRight(),b+i.getSystemWindowInsetBottom());return i;});v.requestApplyInsets();}
    static TextView nav(Activity a,String title){TextView v=text(a,"‹   "+title,20,INK);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(dp(a,4),dp(a,34),0,0);v.setMinHeight(dp(a,92));v.setOnClickListener(x->a.finish());return v;}
    static void toast(Context c,String s){Toast.makeText(c,s,Toast.LENGTH_SHORT).show();}
}
