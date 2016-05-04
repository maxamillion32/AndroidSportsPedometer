package com.example.androidsportspedometer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * Created by 半米阳光 on 2016/4/29.
 */
public class StepDetectorService extends Service {

    private StepDetector stepDetector;
    private SensorManager sensorManager;
    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private String account;
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    String date=sdf.format(new java.util.Date());

    private float stepLength = 0.71f;
    private float bodyWeight = 65;
    private Boolean motion = false;

    private StepDectorBinder mBinder = new StepDectorBinder();

    private LocalBroadcastManager localBroadcastManager;

    public IBinder onBind(Intent intent){
        return mBinder;
    }
    //必须保证该服务在锁屏时可工作
    public void onCreate(){
        super.onCreate();
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        Intent intent = new Intent(this,MainActivity.class);//主活动单任务模式
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle("乐活计步器运行中")
                .setContentText("点击查看乐活运动数据")
                .setContentIntent(pi)
                .setSmallIcon(R.mipmap.pedometer)
                .setWhen(System.currentTimeMillis())
                .setTicker("乐活通知")
               /* .setDefaults(Notification.DEFAULT_ALL)//此设置可以直接将振动、LED灯、铃声设为默认*/
                .build();

        startForeground(1, notification);
        Log.d("CPU", "主线程号为：" + Thread.currentThread().toString());

    }
    public int onStartCommand(Intent intent,int flags,int startId){
        return super.onStartCommand(intent,flags,startId);
    }

    public void onDestroy(){
        super.onDestroy();
        if(sensorManager != null)
            sensorManager.unregisterListener(stepDetector);
    }

    class StepDectorBinder extends Binder{
        //开始计步
        public void startDectection(){//计步线程,计步代码还是运行在主线程中
            sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            stepDetector = new StepDetector();
            sensorManager.registerListener(stepDetector, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d("CPU",Thread.currentThread().toString());
        }

        public void setUserInformation(String accountData,float stepLengthData,float bodyWeightData,Boolean motionData){
            account = accountData;
            stepLength = stepLengthData;
            bodyWeight = bodyWeightData;
            motion = motionData;
        }

        public void setSensitivity(float sensitivity){
            stepDetector.setSensitivity(sensitivity);
        }

    }


    class StepDetector implements SensorEventListener {//使用内部类的方式来实现获取TextView控件
        private final static String TAG = "StepDetector";
        private float mLimit = 40;//50 - 灵敏度的默认�?
        private float mLastValues[] = new float[3 * 2];
        private float mScale[] = new float[2];
        private float mYOffset;
        private long start = 0;//记录每一步开始的时间
        private long end = 0;//记录每一步结束的时间

        private float mLastDirections[] = new float[3 * 2];
        private float mLastExtremes[][] = {new float[3 * 2], new float[3 * 2]};
        private float mLastDiff[] = new float[3 * 2];
        private int mLastMatch = -1;

        private int current_step = 0;//记录当前运动的步�?
        private float time = 0;
        private float motionDistance = 0;
        private float motionKcal = 0;

        DecimalFormat df = new DecimalFormat("########.000");//使其格式化，输出三位小数
        private float distanceTemp = 0;
        private float timeTemp;
        private float kcalTemp;


        public StepDetector() {
            int h = 480; // TODO: remove this constant
            mYOffset = h * 0.5f;
            mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
            mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
            start = System.currentTimeMillis();//创建监听器的初始时间


            //读取数据库中的当天数据
            dbHelper = new MyDatabaseHelper(StepDetectorService.this,"User.db",null,1);
            db = dbHelper.getWritableDatabase();

            Cursor cursor = db.rawQuery("select * from UserMotionData", null);
            while (cursor.moveToNext()) {
                String accountContent = cursor.getString(cursor.getColumnIndex("account"));
                String dateContent = cursor.getString(cursor.getColumnIndex("date"));

                if (account.equals(accountContent) && date.equals(dateContent)) {
                    current_step = cursor.getInt(cursor.getColumnIndex("steps"));
                    motionDistance = cursor.getFloat(cursor.getColumnIndex("distance"));
                    time = cursor.getFloat(cursor.getColumnIndex("time"));
                    motionKcal = cursor.getFloat(cursor.getColumnIndex("kcal"));

                    Intent intent=new Intent();
                    intent.setAction("com.example.androidsportspedometer.StepDetectorService");

                    //设置数据库内容的显示,通过广播发送给活动
                    intent.putExtra("steps",current_step);

                    distanceTemp = motionDistance;//必须使用distanceTemp来辅助显示小数点位数
                    distanceTemp = Float.parseFloat(df.format(distanceTemp));
                    intent.putExtra("distance",distanceTemp);

                    timeTemp = time;
                    timeTemp = Float.parseFloat(df.format(timeTemp));
                    intent.putExtra("time",timeTemp);

                    kcalTemp = motionKcal;
                    kcalTemp = Float.parseFloat(df.format(kcalTemp));
                    intent.putExtra("kcal", kcalTemp);

                    localBroadcastManager.sendBroadcast(intent);
                    break;
                }
            }
            cursor.close();

        }

        public void setSensitivity(float sensitivity) {
            mLimit = 50 - sensitivity; //0-50
        }


        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
                synchronized (this) {
                if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
                } else {
                    int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
                    if (j == 1) {
                        float vSum = 0;
                        for (int i = 0; i < 3; i++) {
                            final float v = mYOffset + event.values[i] * mScale[j];
                            vSum += v;
                        }
                        int k = 0;
                        float v = vSum / 3;

                        float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                        if (direction == -mLastDirections[k]) {
                            // Direction changed
                            int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                            mLastExtremes[extType][k] = mLastValues[k];
                            float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                            if (diff > mLimit) {

                                boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
                                boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
                                boolean isNotContra = (mLastMatch != 1 - extType);

                                if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                                    end = System.currentTimeMillis();
                                    if (end - start > 500) {//此时判断用户走了�?�?
                                        Log.i(TAG, "step");
                                        mLastMatch = extType;

                                        Intent intent = new Intent();
                                        intent.setAction("com.example.androidsportspedometer.StepDetectorService");
                                        current_step++;
                                        intent.putExtra("steps", current_step);

                                        motionDistance = motionDistance + stepLength / 1000;//修改计算距离的算�?,使之在修改步长之后能正确运行
                                        distanceTemp = motionDistance;//必须使用distanceTemp来辅助显示小数点位数
                                        distanceTemp = Float.parseFloat(df.format(distanceTemp));
                                        intent.putExtra("distance",distanceTemp);

                                        time = time + (float) (end - start) / 3600000;
                                        timeTemp = time;
                                        timeTemp = Float.parseFloat(df.format(timeTemp));
                                        intent.putExtra("time", timeTemp);

                                        if (motion) {
                                            motionKcal = motionKcal + bodyWeight * (stepLength / 1000) * 1.036f;
                                            kcalTemp = motionKcal;
                                            kcalTemp = Float.parseFloat(df.format(kcalTemp));
                                            intent.putExtra("kcal",kcalTemp);
                                        } else {
                                            motionKcal = motionKcal + bodyWeight * (stepLength / 1000) * 0.487f;
                                            kcalTemp = motionKcal;
                                            kcalTemp = Float.parseFloat(df.format(kcalTemp));
                                            intent.putExtra("kcal", kcalTemp);
                                        }
                                        localBroadcastManager.sendBroadcast(intent);
                                        Log.d("CPU","still count!" + Thread.currentThread().toString());

                                        //将数据库中的运动数据进行更新
                                        ContentValues values = new ContentValues();
                                        values.put("steps", current_step);
                                        values.put("distance", motionDistance);
                                        values.put("time", time);
                                        values.put("kcal", motionKcal);
                                        db.update("UserMotionData", values, "account = ? AND date = ?", new String[]{account, date});
                                        Log.d("Main", "更新数据成功�?");
                                        start = end;
                                    }

                                } else {
                                    mLastMatch = -1;
                                }
                            }
                            mLastDiff[k] = diff;
                        }
                        mLastDirections[k] = direction;
                        mLastValues[k] = v;
                    }
                }

            }

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

    }
}
