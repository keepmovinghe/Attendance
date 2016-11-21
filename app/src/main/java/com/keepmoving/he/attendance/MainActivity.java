package com.keepmoving.he.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Button button = null;
    private Button buttonList = null;
    private EditText et1 = null;
    private EditText et2 = null;
    //Toast数据
    private void display(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    //做文本输入对话框
    private void dialog(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(msg);
        builder.show();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //做spinner的效果
        //做按钮跳转
        button = (Button)findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v) {
                // TODO Auto-generated method stub
                et1 = (EditText)findViewById(R.id.editText);
                et2 = (EditText)findViewById(R.id.editText1);
                if((et1.getText().toString()).equals("请输入姓名")||((et1.getText().toString()).equals(""))) {
                    Toast.makeText(MainActivity.this, "请输入姓名！", Toast.LENGTH_SHORT).show();
                    et1.setFocusable(true);
                    et1.setFocusableInTouchMode(true);
                    et1.requestFocus();
                    et1.findFocus();
                    //dialog("请输入姓名！");
                }
                else if((et2.getText().toString()).equals("请输入学号")||((et2.getText().toString()).equals(""))) {
                    Toast.makeText(MainActivity.this, "请输入学号", Toast.LENGTH_SHORT).show();
                    et2.setFocusable(true);
                    et2.setFocusableInTouchMode(true);
                    et2.requestFocus();
                    et2.findFocus();
                    //dialog("请输入学号！");
                }
                else {
                    // 设备ID
                    TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                    String deviceId = tm.getDeviceId();
                    // wifi ip
                    WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    int ipAddress = wifiInfo.getIpAddress();
                    // db实例
                    DatabaseHelper dbh = new DatabaseHelper(MainActivity.this,"SamG_Checkin");
                    SQLiteDatabase sdRead = dbh.getReadableDatabase();
                    // 查询是否已签到
                    Cursor cursor=sdRead.query("CheckinTable", new String[]{"name","number"}, "number=?", new String[]{et2.getText().toString()}, null, null, null);
                    if(cursor.getCount()>0){
                        // 该设备已签到
                        //dialog("您已签到！");
                        Toast.makeText(MainActivity.this,"您已签到！",Toast.LENGTH_SHORT).show();
                        cursor.close();
                        sdRead.close();
                        return;
                    }
                    // 查询是否替签
                    cursor=sdRead.query("CheckinTable", new String[]{"name","number"}, "phone_ID=?", new String[]{deviceId}, null, null, null);
                    if(cursor.getCount()>0){
                        // 该设备已签到
                        //dialog("该设备已签到！");
                        Toast.makeText(MainActivity.this,"该设备已签到！",Toast.LENGTH_SHORT).show();
                        cursor.close();
                        sdRead.close();
                        return;
                    }

                    // 保存数据
                    //SQLiteDatabase sd = dbh.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("phone_ID",deviceId);
                    values.put("link_flag",String.valueOf(ipAddress));
                    values.put("name", et1.getText().toString());
                    values.put("number", et2.getText().toString());
                    //sd.insert("CheckinTable", null, values);
                    //sd.close();

                    //发送广播
                    sendBroadcas(values);

                    Toast.makeText(MainActivity.this,"签到成功！",Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonList = (Button) findViewById(R.id.button);
        buttonList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, ListActivity.class);
                    startActivity(intent);

            }
        });
    }

    public void sendBroadcas(ContentValues values){
        Intent intent = new Intent("com.keepmoving.he.attendance");
        //intent.putExtra("values", values);
        intent.putExtra("phone_ID",String.valueOf(values.get("phone_ID")));
        intent.putExtra("link_flag",String.valueOf(values.get("link_flag")));
        intent.putExtra("name", String.valueOf(values.get("name")));
        intent.putExtra("number", String.valueOf(values.get("number")));
        //sendBroadcast(intent);
        //发送广播
        System.out.println(values.get("number"));
        sendBroadcast(intent);
    }

    @Override
    //做菜单栏
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("退出");
        menu.add("关于我们");
        return true;
    }
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // TODO Auto-generated method stub
        if(item.getTitle().equals("退出"))
            finish();
        else if(item.getTitle().equals("关于我们"));
        return super.onMenuItemSelected(featureId, item);

    }
}