package com.example.androidsportspedometer;

;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Created by 半米阳光 on 2016/4/15.
 */
public class MotionCurve extends AppCompatActivity implements View.OnClickListener {

    private static final int YEAR = 1;
    private static final int MONTH = 2;
    private static final int DAY = 3;

    private static final int STEP = 1;
    private static final int DISTANCE = 2;
    private static final int TIME = 3;
    private static final int KCAL = 4;

    private CheckBox year;
    private CheckBox month;
    private CheckBox day;
    private CheckBox step;
    private CheckBox distance;
    private CheckBox time;
    private CheckBox kcal;


    private int cycle;//统计周期
    private int parameter;//统计参数

    private LinearLayout layout;
    private String[] titles = new String[]{"motion-curve"};
    private int[] colors = new int[]{Color.BLUE};
    private PointStyle[] styles = new PointStyle[]{PointStyle.CIRCLE};

    private String account;

    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private List<MotionData> motionDatas = new ArrayList<>();

    private int arrayCount = 0;

    private double [] xV;
    private double [] yV;//最多serieslength个数据
    private String [] zV;

    protected void onCreate(Bundle SavedInstanceStates) {
        super.onCreate(SavedInstanceStates);
        setContentView(R.layout.motion_curve);

        year = (CheckBox) findViewById(R.id.year);
        month = (CheckBox) findViewById(R.id.month);
        day = (CheckBox) findViewById(R.id.day);
        step = (CheckBox) findViewById(R.id.step);
        distance = (CheckBox) findViewById(R.id.distance);
        time = (CheckBox) findViewById(R.id.time);
        kcal = (CheckBox) findViewById(R.id.kcal);
        year.setOnClickListener(this);
        month.setOnClickListener(this);
        day.setOnClickListener(this);
        step.setOnClickListener(this);
        distance.setOnClickListener(this);
        time.setOnClickListener(this);
        kcal.setOnClickListener(this);

        Intent intent = getIntent();
        account = intent.getStringExtra("account");

        dbHelper = new MyDatabaseHelper(MotionCurve.this,"User.db",null,1);
        db = dbHelper.getWritableDatabase();



        Cursor cursor = db.rawQuery("select * from UserMotionData where account = ?",new String[]{account});
        while (cursor.moveToNext()){
            MotionData motionData = new MotionData();//添加对象的时候，必须新建
            motionData.steps = cursor.getInt(cursor.getColumnIndex("steps"));
            motionData.distance = cursor.getFloat(cursor.getColumnIndex("distance"));
            motionData.time = cursor.getFloat(cursor.getColumnIndex("time"));
            motionData.kcal = cursor.getFloat(cursor.getColumnIndex("kcal"));
            motionData.date = cursor.getString(cursor.getColumnIndex("date"));
            motionDatas.add(motionData);//将所有数据添加进motionDatas中
            Log.d("Motion",motionData.steps + "|" + motionData.date);
        }

        cursor.close();

        layout = (LinearLayout)findViewById(R.id.line_chart);//成功实现在xml中添加linechart


    }


    protected void paintLineChart(String[] titles,List<MotionData> motionDatas,int[] colors,
                                  PointStyle[] styles, String title, String xTitle,
                                  String yTitle, double xMin, double xMax,
                                  double yMin, double yMax, int axesColor, int labelsColor,int YLableNum){


        XYMultipleSeriesDataset dataset = buildDataset(titles, motionDatas);
        XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles, true);
        setChartSettings(renderer, title, xTitle, yTitle, xMin, xMax, yMin, yMax, axesColor, labelsColor, YLableNum);


        View chart = ChartFactory.getLineChartView(this, dataset, renderer);

        layout.removeAllViews();

        layout.addView(chart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));


    }



    protected XYMultipleSeriesDataset buildDataset(String[] titles, List<MotionData> motionDatas)
    {

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        int length = titles.length;    //有几条线

        for (int i = 0; i < length; i++)

        {

            XYSeries series = new XYSeries(titles[i]);    //根据每条线的名称创建

            //线里面的点坐标由date时间来确定
            int seriesLength = motionDatas.size();    //有几个点
            Log.d("Motion",seriesLength + "");
            String yearTemp = "";
            String monthTemp = "";
            String dayTemp = "";
            int yTemp = 0;
            int xStart = 6;

            arrayCount = 0;


            xV = new double[seriesLength];//直接清空原数组
            yV = new double[seriesLength];
            zV = new String[seriesLength];

            for(int k = seriesLength - 1;k >= 0;k--){
                if(cycle == YEAR && parameter == STEP){
                    if(yearTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = (double)motionDatas.get(k).steps / 100;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp)){
                            yV[yTemp] = yV[yTemp] + (double)motionDatas.get(k).steps / 100;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,3)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = (double)motionDatas.get(k).steps / 100;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }

                }
                else if(cycle == YEAR && parameter == DISTANCE){
                    if(yearTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).distance;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).distance;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,3)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).distance;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }

                }
                else if(cycle == YEAR && parameter == TIME){
                    if(yearTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).time;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).time;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,3)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).time;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == YEAR && parameter == KCAL){
                    if(yearTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).kcal;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).kcal;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,3)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).kcal;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,4);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == MONTH && parameter == STEP){
                    if(monthTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = (double)motionDatas.get(k).steps / 100;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp)){
                            yV[yTemp] = yV[yTemp] + (double)motionDatas.get(k).steps / 100;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = (double)motionDatas.get(k).steps / 100;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == MONTH && parameter == DISTANCE){
                    if(monthTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).distance;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).distance;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).distance;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == MONTH && parameter == TIME){
                    if(monthTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).time;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).time;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).time;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == MONTH && parameter == KCAL){
                    if(monthTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).kcal;
                        zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).kcal;
                        }
                        else {
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).kcal;
                            zV[arrayCount] = motionDatas.get(k).date.substring(0,7);
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == DAY && parameter == STEP){
                    if(dayTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        dayTemp = motionDatas.get(k).date.substring(8,10);
                        Log.d("Motion",yearTemp + monthTemp + dayTemp);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = (double)motionDatas.get(k).steps / 100;
                        zV[arrayCount] = motionDatas.get(k).date;
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp) && motionDatas.get(k).date.substring(8,10).equals(dayTemp)){
                            yV[yTemp] = yV[yTemp] + (double)motionDatas.get(k).steps / 100;
                            Log.d("Motion",k + "|" + motionDatas.get(k).date);

                        }
                        else {
                            //未考虑闰年情况以及大小月，统一30天
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 * 30 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7))) * 30 - (Integer.parseInt(dayTemp) - Integer.parseInt(motionDatas.get(k).date.substring(8,10)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = (double)motionDatas.get(k).steps / 100;
                            zV[arrayCount] = motionDatas.get(k).date;
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            dayTemp = motionDatas.get(k).date.substring(8,10);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                    Log.d("Motion",xV[arrayCount - 1] + "|" + yV[arrayCount - 1] + "|" + zV[arrayCount - 1]);
                }
                else if(cycle == DAY && parameter == DISTANCE){
                    if(dayTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        dayTemp = motionDatas.get(k).date.substring(8,10);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).distance;
                        zV[arrayCount] = motionDatas.get(k).date;
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp) && motionDatas.get(k).date.substring(8,10).equals(dayTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).distance;
                        }
                        else {
                            //未考虑闰年情况以及大小月，统一30天
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 * 30 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7))) * 30 - (Integer.parseInt(dayTemp) - Integer.parseInt(motionDatas.get(k).date.substring(8,10)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).distance;
                            zV[arrayCount] = motionDatas.get(k).date;
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            dayTemp = motionDatas.get(k).date.substring(8,10);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == DAY && parameter == TIME){
                    if(dayTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        dayTemp = motionDatas.get(k).date.substring(8,10);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).time;
                        zV[arrayCount] = motionDatas.get(k).date;
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp) && motionDatas.get(k).date.substring(8,10).equals(dayTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).time;
                        }
                        else {
                            //未考虑闰年情况以及大小月，统一30天
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 * 30 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7))) * 30 - (Integer.parseInt(dayTemp) - Integer.parseInt(motionDatas.get(k).date.substring(8,10)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).time;
                            zV[arrayCount] = motionDatas.get(k).date;
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            dayTemp = motionDatas.get(k).date.substring(8,10);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }
                else if(cycle == DAY && parameter == KCAL){
                    if(dayTemp.equals("")){
                        yearTemp = motionDatas.get(k).date.substring(0,4);
                        monthTemp = motionDatas.get(k).date.substring(5,7);
                        dayTemp = motionDatas.get(k).date.substring(8,10);
                        xV[arrayCount] = xStart;
                        yV[arrayCount] = motionDatas.get(k).kcal;
                        zV[arrayCount] = motionDatas.get(k).date;
                        yTemp = arrayCount;
                        arrayCount++;
                    }
                    else {
                        if(motionDatas.get(k).date.substring(0,4).equals(yearTemp) && motionDatas.get(k).date.substring(5,7).equals(monthTemp) && motionDatas.get(k).date.substring(8,10).equals(dayTemp)){
                            yV[yTemp] = yV[yTemp] + motionDatas.get(k).kcal;
                        }
                        else {
                            //未考虑闰年情况以及大小月，统一30天
                            xV[arrayCount] = xStart - (Integer.parseInt(yearTemp) - Integer.parseInt(motionDatas.get(k).date.substring(0,4))) * 12 * 30 - (Integer.parseInt(monthTemp) - Integer.parseInt(motionDatas.get(k).date.substring(5,7))) * 30 - (Integer.parseInt(dayTemp) - Integer.parseInt(motionDatas.get(k).date.substring(8,10)));
                            xStart = (int)xV[arrayCount];
                            yV[arrayCount] = motionDatas.get(k).kcal;
                            zV[arrayCount] = motionDatas.get(k).date;
                            yearTemp = motionDatas.get(k).date.substring(0,4);
                            monthTemp = motionDatas.get(k).date.substring(5,7);
                            dayTemp = motionDatas.get(k).date.substring(8,10);
                            yTemp = arrayCount;
                            arrayCount++;
                        }
                    }
                }

            }

            DecimalFormat df = new DecimalFormat(".000");
            double yVTemp;
            for (int k = arrayCount - 1; k >= 0; k--)    //数组中的数据点个数,注意添加的数据x坐标一定是增长的，否则无法绘制图像
            {
                yVTemp = yV[k];
                yVTemp =Double.parseDouble(df.format(yVTemp));
                series.add(xV[k], yVTemp);
            }

            dataset.addSeries(series);

        }

        return dataset;

    }

    protected XYMultipleSeriesRenderer buildRenderer(int[] colors, PointStyle[] styles, boolean fill)

    {

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        int length = colors.length;

        for (int i = 0; i < length; i++)

        {

            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(colors[i]);
            r.setPointStyle(styles[i]);

            r.setFillPoints(fill);

            renderer.addSeriesRenderer(r);

        }

        return renderer;

    }

    protected void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle, String yTitle, double xMin,
                                    double xMax, double yMin, double yMax, int axesColor, int labelsColor,int YLableNum) {

        renderer.setChartTitle(title);

        renderer.setXTitle(xTitle);

        renderer.setYTitle(yTitle);

        renderer.setXAxisMin(xMin);

        renderer.setXAxisMax(xMax);

        renderer.setYAxisMin(yMin);

        renderer.setYAxisMax(yMax);

        renderer.setAxesColor(axesColor);//设置轴颜色

        renderer.setLabelsColor(labelsColor);//设置XY轴标识颜色,不影响刻度

        renderer.setXLabelsColor(Color.BLACK);//设置X刻度的颜色
        renderer.setYLabelsColor(0, Color.BLACK);//设置Y刻度的颜色

        renderer.setYLabelsAlign(Paint.Align.RIGHT);//设置Y轴和刻度值之间的位置关系
        renderer.setYLabelsPadding(5);//设置Y轴和刻度值之间的填充间隔

        renderer.setLegendTextSize(20);//设置图例的文字大小
        renderer.setLabelsTextSize(15);//设置坐标刻度值文字大小，不影响轴标识

        renderer.setXLabels(0);//屏蔽x轴的刻度值


        for(int k = 0;k < arrayCount;k++)
        {
            renderer.addXTextLabel(xV[k],zV[k]);
        }

        renderer.setYLabels(YLableNum);
        renderer.setDisplayChartValues(true);//显示折线点的Y轴值

        renderer.setShowGrid(true);
        renderer.setGridColor(Color.DKGRAY);
        renderer.setZoomEnabled(false, false);//设置无法缩放
        renderer.setPanEnabled(true, true);//设置x轴可滑动，y轴可滑动


        renderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));//不能直接使用Color.transparent,其实原文件将其定义为0，只是表示没有效果，并不是真正的透明



    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.year:
                if (year.isChecked()) {
                    cycle = YEAR;
                    month.setChecked(false);
                    day.setChecked(false);
                } else {
                    year.setChecked(true);
                }
                //判断参数部分是否已经选定好
                if(step.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动步数-曲线","年","步数（百步）",0,6,0,4800,Color.BLACK,Color.BLUE,21);
                }
                else if(distance.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动距离-曲线","年","距离（km）",0,6,0,1200,Color.BLACK,Color.BLUE,11);
                }
                else if(time.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动时间-曲线","年","时间（h）",0,6,0,2400,Color.BLACK,Color.BLUE,21);
                }
                else if(kcal.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动消耗卡路里-曲线","年","卡路里（kcal）",0,6,0,2400,Color.BLACK,Color.BLUE,21);
                }

                break;
            case R.id.month:
                if (month.isChecked()) {
                    cycle = MONTH;
                    year.setChecked(false);
                    day.setChecked(false);
                } else {
                    month.setChecked(true);
                }
                //判断参数部分是否已经选定好
                if(step.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动步数-曲线","月","步数（百步）",0,6,0,400,Color.BLACK,Color.BLUE,21);//需要扩大
                }
                else if(distance.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动距离-曲线","月","距离（km）",0,6,0,100,Color.BLACK,Color.BLUE,11);
                }
                else if(time.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动时间-曲线","月","时间（h）",0,6,0,200,Color.BLACK,Color.BLUE,21);
                }
                else if(kcal.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动消耗卡路里-曲线","月","卡路里（kcal）",0,6,0,200,Color.BLACK,Color.BLUE,21);
                }
                break;
            case R.id.day:
                if (day.isChecked()) {
                    cycle = DAY;
                    month.setChecked(false);
                    year.setChecked(false);
                } else {
                    day.setChecked(true);
                }
                //判断参数部分是否已经选定好
                if(step.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动步数-曲线","日","步数（百步）",0,6,0,20,Color.BLACK,Color.BLUE,21);
                }
                else if(distance.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动距离-曲线","日","距离（km）",0,6,0,5,Color.BLACK,Color.BLUE,11);
                }
                else if(time.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动时间-曲线","日","时间（h）",0,6,0,10,Color.BLACK,Color.BLUE,21);
                }
                else if(kcal.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动消耗卡路里-曲线","日","卡路里（kcal）",0,6,0,10,Color.BLACK,Color.BLUE,21);
                }
                break;
            case R.id.step:
                if (step.isChecked()) {
                    parameter = STEP;
                    distance.setChecked(false);
                    time.setChecked(false);
                    kcal.setChecked(false);
                } else {
                    step.setChecked(true);
                }
                //判断周期部分是否已经选定好
                if(year.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动步数-曲线","年","步数（百步）",0,6,0,4800,Color.BLACK,Color.BLUE,21);
                }
                else if(month.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动步数-曲线","月","步数（百步）",0,6,0,400,Color.BLACK,Color.BLUE,21);
                }
                else if(day.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动步数-曲线","日","步数（百步）",0,6,0,20,Color.BLACK,Color.BLUE,21);
                }

                break;
            case R.id.distance:
                if (distance.isChecked()) {
                    parameter = DISTANCE;
                    step.setChecked(false);
                    time.setChecked(false);
                    kcal.setChecked(false);
                } else {
                    distance.setChecked(true);
                }
                //判断周期部分是否已经选定好
                if(year.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动距离-曲线","年","距离（km）",0,6,0,1200,Color.BLACK,Color.BLUE,11);
                }
                else if(month.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动距离-曲线","月","距离（km）",0,6,0,100,Color.BLACK,Color.BLUE,11);
                }
                else if(day.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动距离-曲线","日","距离（km）",0,6,0,5,Color.BLACK,Color.BLUE,11);
                }
                break;
            case R.id.time:
                if (time.isChecked()) {
                    parameter = TIME;
                    distance.setChecked(false);
                    step.setChecked(false);
                    kcal.setChecked(false);
                } else {
                    time.setChecked(true);
                }
                //判断周期部分是否已经选定好
                if(year.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动时间-曲线","年","时间（h）",0,6,0,2400,Color.BLACK,Color.BLUE,21);
                }
                else if(month.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动时间-曲线","月","时间（h）",0,6,0,200,Color.BLACK,Color.BLUE,21);
                }
                else if(day.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动时间-曲线","日","时间（h）",0,6,0,10,Color.BLACK,Color.BLUE,21);
                }
                break;
            case R.id.kcal:
                if (kcal.isChecked()) {
                    parameter = KCAL;
                    distance.setChecked(false);
                    time.setChecked(false);
                    step.setChecked(false);
                } else {
                    kcal.setChecked(true);
                }
                //判断周期部分是否已经选定好
                if(year.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"年-运动消耗卡路里-曲线","年","卡路里（kcal）",0,6,0,2400,Color.BLACK,Color.BLUE,21);
                }
                else if(month.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"月-运动消耗卡路里-曲线","月","卡路里（kcal）",0,6,0,200,Color.BLACK,Color.BLUE,21);
                }
                else if(day.isChecked()){
                    paintLineChart(titles,motionDatas,colors,styles,"日-运动消耗卡路里-曲线","日","卡路里（kcal）",0,6,0,10,Color.BLACK,Color.BLUE,21);
                }
                break;
            default:
        }
    }

    class MotionData{//使用公有变量，避免复杂操作
        public int steps;
        public float distance;
        public float time;
        public float kcal;
        public String date;
    }
}





