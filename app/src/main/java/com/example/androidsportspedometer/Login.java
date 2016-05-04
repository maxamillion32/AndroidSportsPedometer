package com.example.androidsportspedometer;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.OnekeyShareTheme;


/**
 * Created by 半米阳光 on 2016/4/11.
 */
public class Login extends AppCompatActivity implements View.OnClickListener{
    private Button login;
    private Button register;
    private EditText account;
    private EditText password;
    private MyDatabaseHelper dbHelper;


    protected void onCreate(Bundle SavedInstanceStates){
        super.onCreate(SavedInstanceStates);
        /*supportRequestWindowFeature(Window.FEATURE_NO_TITLE);*/
        setContentView(R.layout.login);
        login = (Button)findViewById(R.id.login);
        login.setOnClickListener(this);
        register = (Button)findViewById(R.id.register);
        register.setOnClickListener(this);



        Log.d("Test", Environment.getExternalStorageDirectory() + "");

    }

    public void onClick(View v){
        boolean flag = true;

        switch (v.getId()){
            case R.id.login:
                account = (EditText)findViewById(R.id.account);
                password = (EditText)findViewById(R.id.password);

                dbHelper = new MyDatabaseHelper(this,"User.db",null,1);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                Cursor cursor = db.rawQuery("select * from UserInformation",null);
                while(cursor.moveToNext()){
                    String accountContent = cursor.getString(cursor.getColumnIndex("account"));
                    String passwordContent = cursor.getString(cursor.getColumnIndex("password"));

                    if(account.getText().toString().equals(accountContent) && password.getText().toString().equals(passwordContent)){
                        Intent intent = new Intent(this,MainActivity.class);
                        intent.putExtra("account",accountContent);
                        startActivity(intent);
                        flag = false;
                        break;//此时应该跳出，无需再搜索了
                    }
                }
                cursor.close();

                if(flag){//可以用来屏蔽login成功的情况
                    Toast.makeText(this,"Sorry,account or password is wrong!\nPlease try again!",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.register:
                Intent intent = new Intent(this,Register.class);
                startActivity(intent);
                break;

            default:
        }
    }
}
