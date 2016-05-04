package com.example.androidsportspedometer;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by 半米阳光 on 2016/4/12.
 */
public class Setting extends AppCompatActivity implements View.OnClickListener{
    private Button confirm;
    private EditText stepLength;
    private EditText bodyWeight;
    private CheckBox walk;
    private CheckBox run;
    private SeekBar sensitivity;
    private TextView sensitivityShow;

    private boolean motion = false;//默认是步行
    private float sensitivityValue = 10;//默认为10
    private float StepLength = 0.71f;
    private float BodyWeight = 65;
    private String account;

    private MyDatabaseHelper dbHelper;


    protected void onCreate(Bundle SavedInstanceStates){
        super.onCreate(SavedInstanceStates);
        /*supportRequestWindowFeature(Window.FEATURE_NO_TITLE);*/
        setContentView(R.layout.setting);
        confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(this);
        stepLength =(EditText)findViewById(R.id.step_length);
        bodyWeight = (EditText)findViewById(R.id.body_weight);
        walk = (CheckBox)findViewById(R.id.walk);
        run = (CheckBox)findViewById(R.id.run);
        walk.setOnClickListener(this);
        run.setOnClickListener(this);
        sensitivityShow = (TextView)findViewById(R.id.sensitivity_show);
        sensitivity = (SeekBar)findViewById(R.id.sensitivity);
        sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {//由于seekbar需要重写的函数较多，因此该写法较好
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivityShow.setText("" + progress);
                sensitivityValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //获取设置的初始值
        Intent intent = getIntent();
        StepLength = intent.getFloatExtra("step_length",0.71f);
        BodyWeight = intent.getFloatExtra("body_weight", 65);
        motion = intent.getBooleanExtra("motion", false);
        sensitivityValue = intent.getFloatExtra("sensitivity", 10);
        account = intent.getStringExtra("account");

        stepLength.setText("" + StepLength);
        bodyWeight.setText("" + BodyWeight);
        if(motion){
            run.setChecked(true);
        }
        else{
            walk.setChecked(true);
        }
        sensitivity.setProgress((int) sensitivityValue);
        sensitivityShow.setText("" + (int)sensitivityValue);

    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.confirm:
                Intent intent = new Intent();
                if(stepLength.getText().toString().equals("")){//若用户不输入任何数值，则设置为默认0.71
                    intent.putExtra("step_length",0.71f);
                }
                else{
                    intent.putExtra("step_length",Float.parseFloat(stepLength.getText().toString()));
                }
                if(bodyWeight.getText().toString().equals("")){//若用户不输入任何数值，则设置为默认65
                    intent.putExtra("body_weight",65f);
                }
                else{
                    intent.putExtra("body_weight",Float.parseFloat(bodyWeight.getText().toString()));
                }
                intent.putExtra("motion",motion);
                intent.putExtra("sensitivity",sensitivityValue);

                dbHelper = new MyDatabaseHelper(this,"User.db",null,1);
                SQLiteDatabase db = dbHelper.getWritableDatabase();//此方法可以在数据库满时报异常
                ContentValues values = new ContentValues();
                values.put("step_length",Float.parseFloat(stepLength.getText().toString()));
                values.put("body_weight",Float.parseFloat(bodyWeight.getText().toString()));
                if(motion){
                    values.put("motion",1);
                }
                else{
                    values.put("motion",0);
                }
                values.put("sensitivity",sensitivityValue);
                db.update("UserSettingData",values,"account = ?",new String[]{account});//每次点击确定都需要更新数据库
                setResult(RESULT_OK, intent);
                finish();
                break;
            case R.id.walk://不使用oncheckedchangd,避免影响
                if(walk.isChecked()){
                    run.setChecked(false);
                    motion = false;
                }
                else{
                    walk.setChecked(true);
                }
                break;
            case R.id.run:
                if(run.isChecked()){
                    walk.setChecked(false);
                    motion = true;
                }
                else{
                    run.setChecked(true);
                }
                break;
            default:
        }
    }






}
