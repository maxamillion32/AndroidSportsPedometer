package com.example.androidsportspedometer;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;


import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.OnekeyShareTheme;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    private TextView steps;
    private Button setting;
    private Button queryHistory;
    private Button screenShotShare;
    private float stepLength = 0.71f;
    private float bodyWeight = 65;
    private boolean motion = false;
    private float sensitivity = 10;

    private TextView distance;
    private TextView motionTime;
    private TextView kcal;

    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private String account;
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    String date=sdf.format(new java.util.Date());

    PowerManager.WakeLock mWakeLock;

    //以下是运动轨迹模块的全局变量
    private MapView mapView;
    private BaiduMap baiduMap;
    private List<MarkerHelper> markerHelpers = new ArrayList<>();

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();

    //计步服务模块的全局变量
    private StepDetectorService.StepDectorBinder stepDectorBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            stepDectorBinder = (StepDetectorService.StepDectorBinder)service;
            stepDectorBinder.setUserInformation(account, stepLength, bodyWeight, motion);//成功绑定后调用
            stepDectorBinder.startDectection();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private LocalBroadcastManager localBroadcastManager;
    private MyReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());//必须在之前进行调�?
        setContentView(R.layout.activity_main);
        acquireWakeLock();//获取电源�?

        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());//必须保持一致

        steps = (TextView)findViewById(R.id.steps);
        setting = (Button)findViewById(R.id.setting);
        setting.setOnClickListener(this);
        queryHistory = (Button)findViewById(R.id.query_history);
        queryHistory.setOnClickListener(this);
        screenShotShare = (Button)findViewById(R.id.screen_shot_share);
        screenShotShare.setOnClickListener(this);
        distance = (TextView)findViewById(R.id.distance);
        motionTime = (TextView)findViewById(R.id.motion_time);
        kcal = (TextView)findViewById(R.id.kcal);

        Intent intent = getIntent();
        account = intent.getStringExtra("account");

        initSettingAndMotionData();

        Intent bindIntent = new Intent(this,StepDetectorService.class);
        bindService(bindIntent,connection,BIND_AUTO_CREATE);


        myReceiver =  new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.androidsportspedometer.StepDetectorService");
        localBroadcastManager.registerReceiver(myReceiver,intentFilter);

        initMotionTrack();


    }


    private void initMotionTrack(){
        //运动轨迹
        mapView = (MapView)findViewById(R.id.mapView);//该版本百度地图自带定位功�?

        baiduMap = mapView.getMap();

        getPreviousLocation();//显示数据库中以前的位置信息

        //使用百度地图定位更加精准
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(getApplicationContext());
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        option.setCoorType("bd09ll");// 设置定位结果类型
        option.setScanSpan(120000);// 设置发起定位请求的间隔时间,两分钟一次
        option.setIsNeedAddress(true);// 返回的定位结果包含地址信息
        option.setNeedDeviceDirect(true);// 设置返回结果包含手机的方向
        mLocClient.setLocOption(option);
        mLocClient.start();//开始定位
        Log.d("Test", "run to here!");



        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                for (MarkerHelper m : markerHelpers) {
                    if (m.getMarkers()[0] == marker || m.getMarkers()[1] == marker) {//点击红点或�?�当前位置，触发事件
                        //实例化一个地理编码查询对�?
                        GeoCoder geoCoder = GeoCoder.newInstance();
                        //设置反地理编码位置坐�?
                        ReverseGeoCodeOption op = new ReverseGeoCodeOption();
                        op.location(m.getLocation());
                        //发起反地理编码请�?(经纬�?->地址信息)
                        geoCoder.reverseGeoCode(op);
                        geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                            @Override
                            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult arg0) {
                                //显示点击的坐标地�?
                                Toast.makeText(MainActivity.this, arg0.getAddress(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onGetGeoCodeResult(GeoCodeResult arg0) {
                            }
                        });
                        break;
                    }
                }
                return false;
            }
        });

    }

    private void initSettingAndMotionData(){
        Boolean flag = true;


        //此时�?测数据库，若无account信息，则添加默认数据；否则，将设置调为数据库中的数据
        dbHelper = new MyDatabaseHelper(this,"User.db",null,1);
        db = dbHelper.getWritableDatabase();

        Cursor cursor = db.rawQuery("select * from UserSettingData",null);
        while(cursor.moveToNext()){
            String accountContent = cursor.getString(cursor.getColumnIndex("account"));
            if(accountContent.equals(account)){//如果数据库中有数�?
                stepLength = cursor.getFloat(cursor.getColumnIndex("step_length"));
                bodyWeight = cursor.getFloat(cursor.getColumnIndex("body_weight"));
                if(cursor.getInt(cursor.getColumnIndex("motion")) == 1)
                    motion = true;
                else
                    motion =false;
                sensitivity = cursor.getFloat(cursor.getColumnIndex("sensitivity"));
                flag = false;
                break;
            }
        }
        cursor.close();

        if(flag){//如果数据库中没有数据
            ContentValues values = new ContentValues();
            values.put("account",account);
            values.put("step_length",stepLength);
            values.put("body_weight", bodyWeight);
            if(motion){
                values.put("motion",1);
            }
            else {
                values.put("motion",0);
            }
            values.put("sensitivity", sensitivity);
            db.insert("UserSettingData", null, values);
        }


        flag = true;

        cursor = db.rawQuery("select * from UserMotionData", null);
        while (cursor.moveToNext()){
            String accountContent = cursor.getString(cursor.getColumnIndex("account"));
            String dateContent = cursor.getString(cursor.getColumnIndex("date"));

            if(account.equals(accountContent) && date.equals(dateContent)){
                flag = false;
                break;
            }
        }
        cursor.close();
        if(flag){
            ContentValues values = new ContentValues();
            values.put("account",account);
            values.put("steps",0);
            values.put("distance",0f);
            values.put("time",0f);
            values.put("kcal",0f);
            values.put("date",date);
            db.insert("UserMotionData", null, values);
            Log.d("Main","当天数据添加成功！全�?0");
        }
    }

    private void acquireWakeLock()
    {
        if (null == mWakeLock)
        {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |PowerManager.ON_AFTER_RELEASE, "okTag");
            if (null != mWakeLock)
            {
                mWakeLock.acquire();
            }
        }
    }

    private void getPreviousLocation(){
        Boolean flag = true;
        Cursor cursor = db.rawQuery("select * from UserLocationData where account = ? AND date = ?",new String[]{account,date});//直接使用sql语句实现搜索功能
        //此时数据库中应该有多组位置数�?
        while (cursor.moveToNext()){
            Double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
            Double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
            LatLng point = new LatLng(latitude,longitude);
            if(flag){//除了第一次加载地图信息外，否则缩放默认都是1公里，比较正常显示轨迹
                flag = false;
                baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(14).target(point).build()));
            }
            showLocation(point);
        }
        cursor.close();


    }

    private void updateSQLiteLocationData(LatLng point){
        ContentValues contentValues = new ContentValues();
        contentValues.put("account",account);
        contentValues.put("latitude",point.latitude);
        contentValues.put("longitude",point.longitude);
        contentValues.put("date",date);
        db.insert("UserLocationData", null, contentValues);
    }

    private void showLocation(LatLng point){

        BitmapDescriptor bitmap_1 = BitmapDescriptorFactory
                .fromResource(R.drawable.red_dot);//图片的名称必须小�?,这部分代码不能做为全�?变量.即bitmap对象不能重用

        BitmapDescriptor bitmap_2 = BitmapDescriptorFactory
                .fromResource(R.drawable.my_location);//图片的名称必须小�?,这部分代码不能做为全�?变量*/


        OverlayOptions option_1 = new MarkerOptions()
                .position(point)
                .icon(bitmap_1);

        OverlayOptions option_2 = new MarkerOptions()
                .position(point)
                .icon(bitmap_2);//第二个会将第�?个给覆盖�?

        Marker[] markers = new Marker[2];
        markers[0] = (Marker) baiduMap.addOverlay(option_1);
        markers[1] = (Marker) baiduMap.addOverlay(option_2);


        MarkerHelper markerHelper = new MarkerHelper(markers,point);

        if(markerHelpers.size() != 0){
            markerHelpers.get(markerHelpers.size() - 1).getMarkers()[1].remove();//去除当前位置标志
        }

        markerHelpers.add(markerHelper);//添加标记

        //将记录下来的当前位置添加进入数据库中,不放在此�?
    }

    private void releaseWakeLock()
    {
        if (null != mWakeLock)
        {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    protected void onResume(){
        super.onResume();
        mapView.onResume();

    }

    protected void onPause(){
        super.onPause();
        //百度地图暂停
        mapView.onPause();
    }

    protected void onDestroy(){
        super.onDestroy();
        /*if(sensorManager != null){
            sensorManager.unregisterListener(stepDetector);
        }*/
        releaseWakeLock();//活动被销毁时释放电源�?
        mapView.onDestroy();
        if(mLocClient != null){
            mLocClient.unRegisterLocationListener(myListener);
        }

        unbindService(connection);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.setting:
                Intent intent = new Intent(this,Setting.class);
                intent.putExtra("step_length",stepLength);
                intent.putExtra("body_weight",bodyWeight);
                intent.putExtra("motion",motion);
                intent.putExtra("sensitivity",sensitivity);
                intent.putExtra("account", account);
                startActivityForResult(intent, 1);//请求码为1
                break;
            case R.id.query_history:
                Intent queryIntent = new Intent(this,MotionCurve.class);
                queryIntent.putExtra("account",account);
                startActivity(queryIntent);
                Log.d("Main","点击查询");
                break;
            case R.id.screen_shot_share:
                //实现截屏，并且分享
                baiduMap.snapshot(new BaiduMap.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(Bitmap bitmap) {
                        String fname = combineBitmap(screenShot(),bitmap);
                        showShare(fname);
                    }
                });

                break;
            default:
        }
    }

    //截屏函数
    private Bitmap screenShot(){
        Log.d("Test", "screenshot!");
        View view = getWindow().getDecorView();

        view.setDrawingCacheEnabled(true);

        view.buildDrawingCache();

        Bitmap bitmap = view.getDrawingCache();

        return bitmap;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private String combineBitmap(Bitmap background, Bitmap foreground) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        File sdcardDir = Environment.getExternalStorageDirectory();
        String path = sdcardDir.getPath() + "/LOHAS/";

        File localImage = new File(path);
        if(!localImage.exists()){
            localImage.mkdir();//若不存在，则创建目录
        }
        String fname = path + sdf.format(new Date()) + ".png";//这样写具有通用性

        if (background == null) {
            return null;
        }

        Rect outRect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect);//状态栏
        int title_top = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();//标题栏

        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();

        Bitmap newmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(foreground, 0, dip2px(this,145) + title_top + outRect.top, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        if(newmap != null)
        {
            try{
                FileOutputStream out = new FileOutputStream(fname);

                newmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            }catch(Exception e) {
                e.printStackTrace();
            }
        }
        return fname;
    }

    //分享函数
    private void showShare(String fname) {
        ShareSDK.initSDK(this);
        OnekeyShare oks = new OnekeyShare();
        Log.d("Test","showShare!");

        //ShareSDK快捷分享提供两个界面第一个是九宫格 CLASSIC  第二个是SKYBLUE
        oks.setTheme(OnekeyShareTheme.CLASSIC);
        // 令编辑页面显示为Dialog模式
        oks.setDialogMode();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();

        oks.setTitle("乐活计步器1.0");//经过测试，只有邮件，才能显示标题

        // text是分享文本，所有平台都需要这个字段
        oks.setText("这是我今天的运动成果哦——来自有爱的乐活计步器~~");

        // imagePath是图片的本地路径：除Linked-In以外的平台都支持此参数
        oks.setImagePath(fname);//确保SDcard下面存在此张图片

        // 启动分享GUI
        oks.show(this);
    }

    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        switch (requestCode){
            case 1:
                if(resultCode == RESULT_OK){
                    stepLength = data.getFloatExtra("step_length",0.71f);//此处必须得到数据才行，不能为�?
                    bodyWeight = data.getFloatExtra("body_weight", 65);
                    motion = data.getBooleanExtra("motion", false);
                    sensitivity = data.getFloatExtra("sensitivity",10);
                    stepDectorBinder.setUserInformation(account,stepLength,bodyWeight,motion);
                    stepDectorBinder.setSensitivity(sensitivity);
                }
                break;
            default:
        }
    }

    class MyReceiver extends BroadcastReceiver{
        public void onReceive(Context context,Intent intent){
             steps.setText("" + intent.getIntExtra("steps",0));
             distance.setText("" + intent.getFloatExtra("distance", 0));
             motionTime.setText("" + intent.getFloatExtra("time", 0));
             kcal.setText("" + intent.getFloatExtra("kcal",0));
        }
    }

    class MarkerHelper{
        private Marker[] markers = new Marker[2];
        private LatLng location;
        public MarkerHelper(Marker markers[],LatLng location){
            this.markers[0] = markers[0];
            this.markers[1] = markers[1];
            this.location = location;
        }
        public void setMarkers(Marker markers[]){
            this.markers[0] = markers[0];
            this.markers[1] = markers[1];
        }
        public void setLocation(LatLng location){
            this.location = location;
        }
        public Marker[] getMarkers(){
            return markers;
        }
        public LatLng getLocation(){
            return location;
        }
    }

    class MyLocationListenner implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || baiduMap == null) {
                return;
            }
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            showLocation(point);
            updateSQLiteLocationData(point);
        }
    }



}
