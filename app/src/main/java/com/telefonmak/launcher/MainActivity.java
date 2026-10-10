package com.telefonmak.launcher;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  final int navy=Color.rgb(22,42,68), bg=Color.rgb(245,247,250), green=Color.rgb(16,125,68);
  LinearLayout root, body;
  ArrayList<Person> people=new ArrayList<>();
  static class Person {String name,number,photo; Person(String n,String p,String ph){name=n;number=p;photo=ph;}}
  int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
  TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(null,1);return t;}
  GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE},7);showContacts();}
  @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);showContacts();}
  void frame(String title){
    root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg);
    root.setPadding(dp(8),dp(5),dp(8),dp(6));setContentView(root);
    if(title!=null && !title.isEmpty()){TextView h=text(title,26,navy,true);h.setGravity(Gravity.CENTER_VERTICAL);root.addView(h,new LinearLayout.LayoutParams(-1,dp(52)));}
    body=new LinearLayout(this);body.setOrientation(1);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout dock=new LinearLayout(this);dock.setOrientation(0);root.addView(dock,new LinearLayout.LayoutParams(-1,dp(90)));
    dockButton(dock,"☎\nCONTACTS",green,()->showContacts());
    dockButton(dock,"▦\nAPPS",Color.rgb(18,105,231),()->showApps());
  }
  void dockButton(LinearLayout row,String label,int color,Runnable run){
    TextView b=text(label,20,Color.WHITE,true);b.setBackground(shape(color,19));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(b,lp);b.setOnClickListener(v->run.run());
  }
  void load(){people.clear();if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return;Cursor c=null;try{c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.PHOTO_URI},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE ASC");HashSet<String> seen=new HashSet<>();if(c!=null)while(c.moveToNext()){String n=c.getString(0),num=c.getString(1),ph=c.getString(2);if(n==null||num==null)continue;String key=n+"|"+num;if(seen.add(key))people.add(new Person(n,num,ph));}}catch(Exception ignored){}finally{if(c!=null)c.close();}}
  void showContacts(){
    load();frame("");
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body.addView(scroll);
    android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(3);scroll.addView(grid);
    int w=(getResources().getDisplayMetrics().widthPixels-dp(34))/3;
    for(Person p:people){
      android.widget.FrameLayout tile=new android.widget.FrameLayout(this);
      tile.setBackground(shape(Color.rgb(220,233,247),16));tile.setClipToOutline(true);
      android.widget.GridLayout.LayoutParams lp=new android.widget.GridLayout.LayoutParams();
      lp.width=w;lp.height=dp(174);lp.setMargins(dp(2),dp(3),dp(2),dp(3));grid.addView(tile,lp);
      ImageView photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
      boolean loaded=false;
      try{if(p.photo!=null){photo.setImageURI(Uri.parse(p.photo));loaded=photo.getDrawable()!=null;}}catch(Exception ignored){}
      if(loaded){tile.addView(photo,new android.widget.FrameLayout.LayoutParams(-1,-1));}
      else{TextView initial=text(p.name.substring(0,1).toUpperCase(Locale.getDefault()),63,navy,true);initial.setBackground(shape(Color.rgb(220,233,247),16));tile.addView(initial,new android.widget.FrameLayout.LayoutParams(-1,-1));}
      View shade=new View(this);
      GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.TRANSPARENT,0x33000000,0xDD000000});
      shade.setBackground(gradient);android.widget.FrameLayout.LayoutParams shadeLp=new android.widget.FrameLayout.LayoutParams(-1,dp(90),Gravity.BOTTOM);tile.addView(shade,shadeLp);
      TextView name=text(p.name,16,Color.WHITE,true);name.setMaxLines(3);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(dp(3),0,dp(3),dp(8));
      android.widget.FrameLayout.LayoutParams nameLp=new android.widget.FrameLayout.LayoutParams(-1,dp(70),Gravity.BOTTOM);tile.addView(name,nameLp);
      final long[] last={0};tile.setOnClickListener(v->{long now=SystemClock.elapsedRealtime();if(now-last[0]<550){last[0]=0;showPerson(p);}else last[0]=now;});
    }
    if(people.isEmpty()){TextView empty=text("Tiada contact. Benarkan akses Contacts dalam Settings.",21,navy,true);body.addView(empty);}
  }
  void showPerson(Person p){frame("Maklumat Contact");LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setGravity(Gravity.CENTER);body.addView(content,new LinearLayout.LayoutParams(-1,-1));TextView initial=text(p.name.substring(0,1).toUpperCase(Locale.getDefault()),90,navy,true);initial.setBackground(shape(Color.rgb(220,233,247),20));content.addView(initial,new LinearLayout.LayoutParams(dp(170),dp(170)));TextView name=text(p.name,32,navy,true);name.setPadding(0,dp(18),0,dp(12));content.addView(name);TextView number=text(p.number,23,navy,false);content.addView(number);TextView call=text("☎  CALL",30,Color.WHITE,true);call.setBackground(shape(green,18));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(94));cp.setMargins(dp(12),dp(28),dp(12),dp(10));content.addView(call,cp);call.setOnClickListener(v->call(p.number));TextView back=text("KEMBALI",23,navy,true);content.addView(back,new LinearLayout.LayoutParams(-1,dp(68)));back.setOnClickListener(v->showContacts());}
  void call(String number){if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CALL_PHONE},8);return;}try{startActivity(new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+Uri.encode(number))));}catch(Exception e){startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(number))));}}
  void showApps(){frame("Aplikasi Lain");ScrollView scroll=new ScrollView(this);body.addView(scroll);LinearLayout list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);Intent query=new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LAUNCHER);PackageManager pm=getPackageManager();List<ResolveInfo> apps=pm.queryIntentActivities(query,0);Collections.sort(apps,(a,b)->a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));for(ResolveInfo a:apps){if(a.activityInfo.packageName.equals(getPackageName()))continue;TextView t=text(a.loadLabel(pm).toString(),23,navy,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(20),0,dp(8),0);t.setBackground(shape(Color.WHITE,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(72));lp.setMargins(0,dp(3),0,dp(3));list.addView(t,lp);t.setOnClickListener(v->{try{Intent launch=new Intent(Intent.ACTION_MAIN);launch.addCategory(Intent.CATEGORY_LAUNCHER);launch.setClassName(a.activityInfo.packageName,a.activityInfo.name);launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(launch);}catch(Exception ignored){}});}}
  @Override public void onBackPressed(){showContacts();}
}
