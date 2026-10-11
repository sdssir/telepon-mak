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
import android.provider.MediaStore;
import android.content.ContentUris;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  final int navy=Color.rgb(22,42,68), bg=Color.rgb(245,247,250), green=Color.rgb(16,125,68);
  LinearLayout root, body;
  ProgressBar batteryBar;
  TextView batteryLabel;
  final BroadcastReceiver batteryReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){refreshBattery(intent);}};
  boolean batteryRegistered=false;
  ArrayList<Person> people=new ArrayList<>();
  ArrayList<Uri> galleryImages=new ArrayList<>();
  int galleryPosition=0;
  int currentScreen=0;
  final int REQUEST_PHOTOS=29;
  static class Person {String name,number,photo; Person(String n,String p,String ph){name=n;number=p;photo=ph;}}
  int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
  TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(null,1);return t;}
  GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE},7);showContacts();}
  @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
    super.onRequestPermissionsResult(r,p,g);
    if(r==REQUEST_PHOTOS)showGallery();else showContacts();
  }
  void refreshBattery(Intent intent){
    if(intent==null || batteryBar==null || batteryLabel==null)return;
    int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
    int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
    if(level<0 || scale<=0)return;
    int percent=Math.max(0,Math.min(100,Math.round(level*100f/scale)));
    int color=percent<=15?Color.rgb(230,55,57):Color.rgb(46,165,245);
    batteryBar.setProgress(percent);
    batteryBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    batteryLabel.setText(percent+"%");
  }
  @Override protected void onStart(){super.onStart();if(!batteryRegistered){registerReceiver(batteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));batteryRegistered=true;}}
  @Override protected void onStop(){if(batteryRegistered){unregisterReceiver(batteryReceiver);batteryRegistered=false;}super.onStop();}
  void frame(String title){
    root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg);
    root.setPadding(dp(8),dp(5),dp(8),dp(6));setContentView(root);
    LinearLayout batteryHeader=new LinearLayout(this);batteryHeader.setGravity(Gravity.CENTER_VERTICAL);
    batteryHeader.setOrientation(0);batteryHeader.setPadding(dp(14),0,dp(14),0);
    batteryHeader.setBackground(shape(Color.rgb(16,30,48),16));
    LinearLayout.LayoutParams headerLp=new LinearLayout.LayoutParams(-1,dp(48));
    headerLp.setMargins(dp(2),0,dp(2),dp(7));root.addView(batteryHeader,headerLp);
    TextView batteryIcon=text("▰",19,Color.WHITE,true);batteryHeader.addView(batteryIcon,new LinearLayout.LayoutParams(dp(34),-1));
    batteryBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    batteryBar.setMax(100);batteryBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(61,72,87)));
    LinearLayout.LayoutParams barLp=new LinearLayout.LayoutParams(0,dp(14),1);barLp.setMargins(dp(5),0,dp(10),0);batteryHeader.addView(batteryBar,barLp);
    batteryLabel=text("--%",19,Color.WHITE,true);batteryHeader.addView(batteryLabel,new LinearLayout.LayoutParams(dp(65),-1));
    refreshBattery(registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
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
    currentScreen=0;load();frame("");
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
    addGalleryShortcut(grid,w);
    if(people.isEmpty()){TextView empty=text("Tiada contact. Benarkan akses Contacts dalam Settings.",21,navy,true);body.addView(empty);}
  }
  void addGalleryShortcut(android.widget.GridLayout grid,int w){
    android.widget.FrameLayout tile=new android.widget.FrameLayout(this);
    tile.setBackground(shape(Color.rgb(23,103,212),16));tile.setClipToOutline(true);
    android.widget.GridLayout.LayoutParams lp=new android.widget.GridLayout.LayoutParams();
    lp.width=w;lp.height=dp(174);lp.setMargins(dp(2),dp(3),dp(2),dp(3));grid.addView(tile,lp);
    TextView label=text("▧\nGAMBAR",24,Color.WHITE,true);
    tile.addView(label,new android.widget.FrameLayout.LayoutParams(-1,-1));
    tile.setOnClickListener(v->showGallery());
    label.setOnClickListener(v->showGallery());
  }
  boolean hasGalleryPermission(){
    String permission=Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_IMAGES:Manifest.permission.READ_EXTERNAL_STORAGE;
    return Build.VERSION.SDK_INT<23||checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED||(Build.VERSION.SDK_INT>=34&&checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")==PackageManager.PERMISSION_GRANTED);
  }
  void loadGallery(){
    galleryImages.clear();
    Cursor cursor=null;
    try{
      Uri collection=MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
      cursor=getContentResolver().query(collection,new String[]{MediaStore.Images.Media._ID},null,null,MediaStore.Images.Media.DATE_ADDED+" DESC");
      if(cursor!=null)while(cursor.moveToNext()){
        galleryImages.add(ContentUris.withAppendedId(collection,cursor.getLong(0)));
      }
    }catch(Exception ignored){}finally{if(cursor!=null)cursor.close();}
  }
  void galleryBackButton(LinearLayout parent,String label,Runnable action){
    TextView back=text("←  "+label,22,navy,true);
    back.setBackground(shape(Color.rgb(228,238,250),16));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(65));lp.setMargins(dp(3),dp(3),dp(3),dp(9));
    parent.addView(back,lp);back.setOnClickListener(v->action.run());
  }
  void showGallery(){
    currentScreen=2;
    frame("");
    if(!hasGalleryPermission()){
      TextView permission=text("BENARKAN AKSES GAMBAR",24,navy,true);
      body.addView(permission,new LinearLayout.LayoutParams(-1,dp(100)));
      galleryBackButton(body,"KEMBALI KE CONTACT",()->showContacts());
      String perm=Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_IMAGES:Manifest.permission.READ_EXTERNAL_STORAGE;
      if(Build.VERSION.SDK_INT>=34)requestPermissions(new String[]{perm,"android.permission.READ_MEDIA_VISUAL_USER_SELECTED"},REQUEST_PHOTOS);
      else requestPermissions(new String[]{perm},REQUEST_PHOTOS);
      return;
    }
    galleryBackButton(body,"KEMBALI KE CONTACT",()->showContacts());
    TextView loading=text("MEMUATKAN GAMBAR...",21,navy,true);
    body.addView(loading,new LinearLayout.LayoutParams(-1,dp(70)));
    new Thread(()->{loadGallery();runOnUiThread(()->{if(currentScreen==2)renderGallery(loading);});}).start();
  }
  void renderGallery(TextView loading){
    body.removeView(loading);
    ScrollView scroll=new ScrollView(this);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(3);scroll.addView(grid);
    int w=(getResources().getDisplayMetrics().widthPixels-dp(34))/3;
    for(int i=0;i<galleryImages.size();i++){
      final int position=i;
      ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      image.setBackground(shape(Color.rgb(220,233,247),12));image.setClipToOutline(true);
      android.widget.GridLayout.LayoutParams lp=new android.widget.GridLayout.LayoutParams();
      lp.width=w;lp.height=w;lp.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(image,lp);
      // Downsample thumbnails instead of decoding every full-resolution image.
      final Uri thumbUri=galleryImages.get(i);
      new Thread(()->{try{android.graphics.Bitmap bitmap=getContentResolver().loadThumbnail(thumbUri,new android.util.Size(dp(200),dp(200)),null);runOnUiThread(()->{if(currentScreen==2)image.setImageBitmap(bitmap);});}catch(Exception ignored){}}).start();
      image.setOnClickListener(v->showFullPhoto(position));
    }
    if(galleryImages.isEmpty())body.addView(text("Tiada gambar ditemui",23,navy,true));
  }
  void showFullPhoto(int position){
    if(position<0||position>=galleryImages.size()){showGallery();return;}
    currentScreen=3;galleryPosition=position;frame("");
    galleryBackButton(body,"KEMBALI KE GALERI",()->showGallery());
    ImageView full=new ImageView(this);full.setScaleType(ImageView.ScaleType.FIT_CENTER);
    full.setBackgroundColor(Color.BLACK);
    body.addView(full,new LinearLayout.LayoutParams(-1,0,1));
    try{full.setImageURI(galleryImages.get(position));}catch(Exception ignored){}
    final float[] touchX={0};
    full.setOnTouchListener((v,event)->{
      if(event.getAction()==MotionEvent.ACTION_DOWN){touchX[0]=event.getX();return true;}
      if(event.getAction()==MotionEvent.ACTION_UP){
        float dx=event.getX()-touchX[0];
        if(Math.abs(dx)>dp(65)){
          int next=galleryPosition+(dx<0?1:-1);
          if(next>=0&&next<galleryImages.size())showFullPhoto(next);
        }
        return true;
      }
      return true;
    });
    TextView count=text((position+1)+" / "+galleryImages.size(),18,navy,true);
    body.addView(count,new LinearLayout.LayoutParams(-1,dp(36)));
  }
  void showPerson(Person p){
    frame("");
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
    LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setGravity(Gravity.CENTER);
    content.setPadding(dp(8),dp(10),dp(8),dp(12));scroll.addView(content);
    int photoSize=Math.min(dp(290),getResources().getDisplayMetrics().widthPixels-dp(52));
    android.widget.FrameLayout avatar=new android.widget.FrameLayout(this);
    avatar.setBackground(shape(Color.rgb(220,233,247),24));avatar.setClipToOutline(true);
    content.addView(avatar,new LinearLayout.LayoutParams(photoSize,photoSize));
    boolean loaded=false;
    if(p.photo!=null){
      try{
        ImageView photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setImageURI(Uri.parse(p.photo));
        if(photo.getDrawable()!=null){avatar.addView(photo,new android.widget.FrameLayout.LayoutParams(-1,-1));loaded=true;}
      }catch(Exception ignored){}
    }
    if(!loaded){
      TextView initial=text(p.name.substring(0,1).toUpperCase(Locale.getDefault()),108,navy,true);
      avatar.addView(initial,new android.widget.FrameLayout.LayoutParams(-1,-1));
    }
    TextView name=text(p.name,32,navy,true);name.setPadding(0,dp(18),0,dp(10));content.addView(name);
    TextView number=text(p.number,23,navy,false);content.addView(number);
    TextView call=text("☎  PANGGIL",30,Color.WHITE,true);call.setBackground(shape(green,18));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(94));cp.setMargins(dp(8),dp(26),dp(8),dp(10));content.addView(call,cp);
    call.setOnClickListener(v->call(p.number));
    TextView back=text("←  KEMBALI",23,navy,true);back.setBackground(shape(Color.rgb(228,238,250),18));content.addView(back,new LinearLayout.LayoutParams(-1,dp(68)));
    back.setOnClickListener(v->showContacts());
  }
  void call(String number){
    if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.CALL_PHONE},8);return;
    }
    Uri tel=Uri.parse("tel:"+Uri.encode(number));
    try{
      // Request speakerphone on call start. The stock dialer/carrier may ignore this hint.
      android.telecom.TelecomManager telecom=(android.telecom.TelecomManager)getSystemService(Context.TELECOM_SERVICE);
      if(telecom!=null){
        Bundle extras=new Bundle();
        extras.putBoolean(android.telecom.TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE,true);
        telecom.placeCall(tel,extras);
        return;
      }
    }catch(Exception ignored){}
    try{startActivity(new Intent(Intent.ACTION_CALL,tel));}
    catch(Exception e){startActivity(new Intent(Intent.ACTION_DIAL,tel));}
  }
  void showApps(){frame("Aplikasi Lain");ScrollView scroll=new ScrollView(this);body.addView(scroll);LinearLayout list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);Intent query=new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LAUNCHER);PackageManager pm=getPackageManager();List<ResolveInfo> apps=pm.queryIntentActivities(query,0);Collections.sort(apps,(a,b)->a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));for(ResolveInfo a:apps){if(a.activityInfo.packageName.equals(getPackageName()))continue;TextView t=text(a.loadLabel(pm).toString(),23,navy,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(20),0,dp(8),0);t.setBackground(shape(Color.WHITE,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(72));lp.setMargins(0,dp(3),0,dp(3));list.addView(t,lp);t.setOnClickListener(v->{try{Intent launch=new Intent(Intent.ACTION_MAIN);launch.addCategory(Intent.CATEGORY_LAUNCHER);launch.setClassName(a.activityInfo.packageName,a.activityInfo.name);launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(launch);}catch(Exception ignored){}});}}
  @Override public void onBackPressed(){if(currentScreen==3)showGallery();else showContacts();}
}
