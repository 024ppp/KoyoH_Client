package com.example.administrator.koyoh_client;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import static com.example.administrator.koyoh_client.R.id.parent;
import static java.lang.Integer.parseInt;

public class Setting extends Activity implements View.OnClickListener {
    EditText txtIP, txtPort;
    Button btn;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        txtIP = (EditText) findViewById(R.id.txtIP);
        txtPort = (EditText) findViewById(R.id.txtPort);
        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(this);

        //接続先を取得
        prefs = getSharedPreferences("ConnectionData", Context.MODE_PRIVATE);
        String ip = prefs.getString("ip", "");
        int myPort = prefs.getInt("myPort", 0);

        txtIP.setText(ip);
        txtPort.setText(Integer.toString(myPort));
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    //クリック処理の実装
    public void onClick(View v) {

        // 返すデータ(Intent&Bundle)の作成
        Intent data = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("key.StringData", "DATA");
        //bundle.putInt("key.intData", 123456789);
        data.putExtras(bundle);

        //Setting
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ip", txtIP.getText().toString());
        editor.putInt("myPort", parseInt(txtPort.getText().toString()));
        editor.apply();

        // setResult() で bundle を載せた
        // 送るIntent dataをセットする

        // 第一引数は…Activity.RESULT_OK,
        // Activity.RESULT_CANCELED など
        setResult(RESULT_OK, data);

        // finish() で終わらせて
        // Intent data を送る
        finish();
    }

}
