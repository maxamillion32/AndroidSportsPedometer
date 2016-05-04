package com.example.androidsportspedometer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by 半米阳光 on 2016/4/13.
 */
public class MyDatabaseHelper extends SQLiteOpenHelper {
    public static final String CREATE_USER_INFORMATION = "create table UserInformation(" +
            "id integer primary key autoincrement," +
            "account text," +
            "password text)";

    public static final String CREATE_USER_SETTING_DATA = "create table UserSettingData(" +
            "account text primary key," +
            "step_length real," +
            "body_weight real," +
            "motion integer," +//使用int型来代替布尔型
            "sensitivity real)";

    public static final String CREATE_USER_MOTION_DATA ="create table UserMotionData(" +
            "account text," +
            "steps integer," +
            "distance real," +
            "time real," +
            "kcal real," +
            "date text," +
            "primary key(account,date))";

    public static final String CREATE_USER_LOCATION_DATA = "create table UserLocationData(" +
            "account text," +
            "latitude real," +
            "longitude real," +
            "date text)";


    private Context mContext;//用来获取context

    public MyDatabaseHelper(Context context,String name,SQLiteDatabase.CursorFactory factory,int version){
        super(context,name,factory,version);
        mContext = context;
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(CREATE_USER_INFORMATION);
        db.execSQL(CREATE_USER_SETTING_DATA);
        db.execSQL(CREATE_USER_MOTION_DATA);
        db.execSQL(CREATE_USER_LOCATION_DATA);
    }

    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){

    }

}
