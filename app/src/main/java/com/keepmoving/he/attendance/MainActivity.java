package com.keepmoving.he.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {

    private Button button = null;
    private Button buttonList = null;
    private EditText et1 = null;
    private EditText et2 = null;

    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";

    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private List<String> msgList=new ArrayList<String>();
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private ReadThread mreadThread = null;
    private BluetoothServerSocket mserverSocket = null;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ServerThread startServerThread = null;
    private ClientThread clientConnectThread = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init(){
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

                    JSONObject jsonObject=new JSONObject();
                    try {
                        jsonObject.put("phone_ID", deviceId);
                        jsonObject.put("link_flag", String.valueOf(ipAddress));
                        jsonObject.put("name", et1.getText().toString());
                        jsonObject.put("number", et2.getText().toString());
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                    sendMessageHandle(jsonObject);
                    //editMsgView.setText("");
                    //editMsgView.clearFocus();
                    //close InputMethodManager
                    //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    //imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);

                    //发送广播
                    //sendBroadcas(values);

                    //Toast.makeText(MainActivity.this,"签到成功！",Toast.LENGTH_SHORT).show();
                    //display("签到成功！");
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

    @Override
    protected void onStart() {
        super.onStart();
        BluetoothMsg.serviceOrCilent=BluetoothMsg.ServerOrCilent.SERVICE;
        /*
        if(BluetoothMsg.isOpen) {
            //Toast.makeText(MainActivity.this, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
            //display("连接已经打开，可以通信。");
            System.out.println("连接已经打开，可以通信。");
            return;
        }
        */
        openBluetooth();

    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothMsg.serviceOrCilent=BluetoothMsg.ServerOrCilent.CILENT;
        /*
        if(BluetoothMsg.isOpen) {
            //Toast.makeText(MainActivity.this, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
            //display("连接已经打开，可以通信。");
            System.out.println("连接已经打开，可以通信。");
            return;
        }
        */
        openBluetooth();

    }

    /**
     * 打开蓝牙服务
     */
    private void openBluetooth(){

        if(BluetoothMsg.serviceOrCilent==BluetoothMsg.ServerOrCilent.CILENT) {
            String address = BluetoothMsg.BlueToothAddress;
            if(!address.equals("null")) {
                device = mBluetoothAdapter.getRemoteDevice(address);
                clientConnectThread = new ClientThread();
                clientConnectThread.start();
                BluetoothMsg.isOpen = true;
                System.out.println("客户端开启");
            } else {
                //Toast.makeText(MainActivity.this, "地址为空!", Toast.LENGTH_SHORT).show();
                //display("地址为空！");
                System.out.println("地址为空！");
            }
        }
        else if(BluetoothMsg.serviceOrCilent==BluetoothMsg.ServerOrCilent.SERVICE) {
            startServerThread = new ServerThread();
            startServerThread.start();
            BluetoothMsg.isOpen = true;
            System.out.println("服务端开启！");
        }
    }

    private Handler LinkDetectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
            if(msg.what==1) {
                msgList.add((String)msg.obj);
                System.out.println(msg.obj);
            } else {
                msgList.add((String)msg.obj);
            }
            //mAdapter.notifyDataSetChanged();
            //mListView.setSelection(msgList.size() - 1);
        }
    };
    //开启客户端
    private class ClientThread extends Thread {
        @Override
        public void run() {
            try {
                //创建一个Socket连接：只需要服务器在注册时的UUID号
                // socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                //连接
                Message msg2 = new Message();
                msg2.obj = "请稍候，正在连接服务器:"+BluetoothMsg.BlueToothAddress;
                msg2.what = 0;
                LinkDetectedHandler.sendMessage(msg2);

                socket.connect();

                Message msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
                //启动接受数据
                mreadThread = new ReadThread();
                mreadThread.start();
            }
            catch (IOException e) {
                Log.e("connect", "", e);
                Message msg = new Message();
                msg.obj = "连接服务端异常！断开连接重新试一试。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
            }
        }
    };

    //开启服务器
    private class ServerThread extends Thread {
        @Override
        public void run() {

            try {
                    /* 创建一个蓝牙服务器
                     * 参数分别：服务器名称、UUID   */
                mserverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                Log.d("server", "wait cilent connect...");

                Message msg = new Message();
                msg.obj = "请稍候，正在等待客户端的连接...";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);

                    /* 接受客户端的连接请求 */
                socket = mserverSocket.accept();
                Log.d("server", "accept success !");

                Message msg2 = new Message();
                String info = "客户端已经连接上！可以发送信息。";
                msg2.obj = info;
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg2);
                //启动接受数据
                mreadThread = new ReadThread();
                mreadThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    //发送数据
    private void sendMessageHandle(JSONObject jsonObject)
    {
        if (socket == null)
        {
            //Toast.makeText(MainActivity.this, "没有连接", Toast.LENGTH_SHORT).show();
            display("签到失败，没有连接！");
            return;
        }
        try {
            OutputStream os = socket.getOutputStream();
            os.write(jsonObject.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //msgList.add(msg);
        //mAdapter.notifyDataSetChanged();
        //mListView.setSelection(msgList.size() - 1);
    }

    //读取数据
    private class ReadThread extends Thread {
        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;

            try {
                mmInStream = socket.getInputStream();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (true) {
                try {
                    // Read from the InputStream
                    if( (bytes = mmInStream.read(buffer)) > 0 ) {
                        byte[] buf_data = new byte[bytes];
                        for(int i=0; i<bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Message msg = new Message();
                        msg.obj = s;
                        msg.what = 1;
                        LinkDetectedHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    try {
                        mmInStream.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }



    /**
     * 发送广播
     * @param values
     */
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
}