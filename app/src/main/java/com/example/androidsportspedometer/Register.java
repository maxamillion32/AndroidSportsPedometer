package com.example.androidsportspedometer;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by 半米阳光 on 2016/4/13.
 */
public class Register extends AppCompatActivity implements View.OnClickListener{

    private EditText account;
    private EditText password;
    private EditText passwordConfirm;
    private Button confirm;
    private MyDatabaseHelper dbHelper;

    protected void onCreate(Bundle SavedInstanceStates){
        super.onCreate(SavedInstanceStates);
        /*supportRequestWindowFeature(Window.FEATURE_NO_TITLE);*/
        setContentView(R.layout.register);
        account = (EditText)findViewById(R.id.account);
        password = (EditText)findViewById(R.id.password);
        passwordConfirm = (EditText)findViewById(R.id.password_confirm);
        confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(this);
    }

    public void onClick(View v){
        boolean flag = true;
        switch (v.getId()){
            case R.id.confirm:
                if(account.getText().toString().length() < 5 || account.getText().toString().length() > 16){
                    Toast.makeText(this,"Your account is invalid!\nPlease input again!",Toast.LENGTH_SHORT).show();
                    account.setText("");
                    password.setText("");
                    passwordConfirm.setText("");
                }
                else if(password.getText().toString().length() < 5 || password.getText().toString().length() > 16){
                    Toast.makeText(this,"Your password is invalid!\nPlease input again!",Toast.LENGTH_SHORT).show();
                    password.setText("");
                    passwordConfirm.setText("");
                }
                else if(!password.getText().toString().equals(passwordConfirm.getText().toString())){
                    Toast.makeText(this,",The passwords you entered must be the same!",Toast.LENGTH_SHORT).show();
                    password.setText("");
                    passwordConfirm.setText("");
                }
                else{ //将用户输入的正确账号和密码存储进数据库中

                    dbHelper = new MyDatabaseHelper(this,"User.db",null,1);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();//此方法可以在数据库满时报异常

                    //实现注册时的查重功能
                    Cursor cursor = db.rawQuery("select * from UserInformation", null);
                    while(cursor.moveToNext()){
                        String accountContent = cursor.getString(cursor.getColumnIndex("account"));
                        if(account.getText().toString().equals(accountContent)){
                            flag = false;
                            break;
                        }
                    }
                    cursor.close();

                    if(flag){
                        ContentValues values = new ContentValues();
                        values.put("account",account.getText().toString());
                        values.put("password", password.getText().toString());
                        db.insert("UserInformation", null, values);//此时无查重功能
                        Toast.makeText(this,"Register succeeded!",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this,Login.class);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(this,"The account is registered!\nPlease try another account!",Toast.LENGTH_SHORT).show();
                        account.setText("");
                        password.setText("");
                        passwordConfirm.setText("");
                    }

                }
                break;
            default:
        }


    }
}
