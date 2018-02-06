package com.example.administrator.koyoh_client;

import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.os.Handler;
import android.widget.EditText;
import android.os.Message;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Toolbar toolbar;
    Button btnClear, btnUpd;
    TextView show;
    EditText txtSagyo, txtVkon, txtAmime1;
    Handler handler;
    String ip;
    int myPort;
    // サーバと通信するスレッド
    ClientThread clientThread;
    NfcWriter nfcWriter = null;
    //インスタンス化無しで使える
    ProcessCommand pc;
    private static final int SETTING = 8888;
    //入力チェック用配列
    EditText arrEditText[];
    //バイブ
    //CAT40の場合は、バイブを鳴らすとエラーになるため注意！
    Vibrator vib;
    private long m_vibPattern_read[] = {0, 200};
    private long m_vibPattern_error[] = {0, 200, 200, 500};

    String mSagyoName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //バイブ
        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        //-- view取得 --
        //toolbar
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar.setTitle("Ｃ棟ふるい網目セット管理");

        setSupportActionBar(toolbar);
        //Button
        btnClear = (Button) findViewById(R.id.btnClear);
        btnUpd = (Button) findViewById(R.id.btnUpd);
        //TextView
        show = (TextView) findViewById(R.id.show);
        //EditText
        txtSagyo = (EditText) findViewById((R.id.txtSagyo));
        txtVkon = (EditText) findViewById(R.id.txtVkon);
        txtAmime1 = (EditText) findViewById(R.id.txtAmime1);

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                // サブスレッドからのメッセージ
                if (msg.what == 0x123) {
                    // 表示する
                    String sMsg = msg.obj.toString();
                    //show.append("\n PCから受信-" + sMsg);
                    selectMotionWhenReceiving(sMsg);
                }
            }
        };

        //接続先を取得
        SharedPreferences prefs = getSharedPreferences("ConnectionData", Context.MODE_PRIVATE);
        ip = prefs.getString("ip", "");
        myPort = prefs.getInt("myPort", 0);
        clientThread = new ClientThread(handler, ip, myPort, true);
        // サーバ接続スレッド開始
        new Thread(clientThread).start();

        this.nfcWriter = new NfcWriter(this);

        btnClear.setOnClickListener(this);
        btnUpd.setOnClickListener(this);

        txtSagyo.setClickable(true);
        txtSagyo.setOnClickListener(this);

        //入力チェック用配列にセット
        arrEditText = new EditText[]{txtSagyo, txtVkon, txtAmime1};

        //画面初期設定
        initPage();
    }

    //受信した文字列のコマンド値によって分岐（switch文ではenum使えず...）
    private void selectMotionWhenReceiving(String sMsg) {
        String cmd = pc.COMMAND_LENGTH.getCmdText(sMsg);
        String excmd  = pc.COMMAND_LENGTH.getExcludeCmdText(sMsg);

        if (cmd.equals(pc.SAG.getString())) {
            mSagyoName = excmd;
            showSelectSagyo();
        }
        else if (cmd.equals(pc.VKO.getString())) {
            //Vコン存在チェック後、検索結果が返ってくる
            txtVkon.setText(excmd);
            focusToNextControl();
        }
        else if (cmd.equals(pc.UPD.getString())) {
            MyToast.makeText(this, "登録完了しました。", Toast.LENGTH_SHORT, 32f).show();
            initPage();
        }
        else if (cmd.equals(pc.ERR.getString())) {
            //バイブ
            //vib.vibrate(m_vibPattern_error, -1);
            show.setText(excmd);
        }

    }

    //タグテキストのコマンド値によって分岐
    private void selectMotionTagText(String sMsg) {
        String cmd = pc.COMMAND_LENGTH.getCmdText(sMsg);
        String excmd  = pc.COMMAND_LENGTH.getExcludeCmdText(sMsg);

        if (cmd.equals(pc.VKO.getString())) {
            if (txtVkon.isFocused()) {
                //TAGテキストをそのまま送信
                sendMsgToServer(sMsg);
            }
        }
        else if (cmd.equals(pc.AMI.getString())) {
            if (txtAmime1.isFocused()) {
                txtAmime1.setText(excmd);
                focusToNextControl();
                show.setText("全てＯＫです。\n登録してください。");
                btnUpd.setEnabled(true);
            }
        }
        else {
            show.setText("タグテキストエラー！\n（" + sMsg + "）");
        }
    }

    //登録ボタン押下時にサーバに送る更新値の生成
    private String createUpdText() {
        String txt = "";

        txt += txtVkon.getText().toString();
        txt += ",";
        txt += txtAmime1.getText().toString();

        return txt;
    }

    private void initPage() {
        for (int i = 0; i < arrEditText.length; i++) {
            arrEditText[i].setText("");

            //!!! 注意 : (1),(2)の順番は変更しない。フォーカスが当たるようになってしまう。
            //(1)タップされてもキーボードを出さなくする
            arrEditText[i].setRawInputType(InputType.TYPE_CLASS_TEXT);
            arrEditText[i].setTextIsSelectable(true);
            //(2)フォーカスが当たらなくする
            arrEditText[i].setFocusableInTouchMode(false);
            arrEditText[i].setFocusable(false);
        }
        //登録ボタンを無効化
        btnUpd.setEnabled(false);

        //作業者選択画面呼び出し
        showSelectSagyo();

    }

    @Override
    //クリック処理の実装
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.btnUpd :
                    //Dialog(OK,Cancel Ver.)
                    new AlertDialog.Builder(this)
                            .setTitle("確認")
                            .setMessage("登録しますか？")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // OK button pressed
                                    //更新値の生成
                                    String updText = createUpdText();
                                    sendMsgToServer(pc.UPD.getString() + updText);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    break;

                case R.id.btnClear :
                    //Dialog(OK,Cancel Ver.)
                    new AlertDialog.Builder(this)
                            .setTitle("確認")
                            .setMessage("クリアしてよろしいですか？")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // OK button pressed
                                    initPage();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    break;

                case R.id.txtSagyo:
                    showSelectSagyo();
                    break;
            }
        }
    }

    @Override
    //タグを読み込んだ時に実行される
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String tagText = "";
        tagText = this.nfcWriter.getTagText(intent);
        if (!tagText.equals("")) {
            selectMotionTagText(tagText);
        }
        //バイブ
        //vib.vibrate(m_vibPattern_read, -1);
    }

    //サーバへメッセージを送信する
    private void sendMsgToServer(String sMsg) {
        try {
            // メッセージ送信
            Message msg = new Message();
            msg.what = 0x345;   //？
            msg.obj = sMsg;
            clientThread.revHandler.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setShowMessage(int order) {
        switch (order) {
            case 1:
                show.setText("Vコンを\nタッチしてください。");
                break;
            case 2:
                show.setText("網を\nタッチしてください。");
                break;
            case 3:
                show.setText("をタッチしてください。");
                break;
        }
    }

    //次のコントロールにフォーカスを当てる
    private void focusToNextControl(){
        for (int i = 0; i < arrEditText.length; i++) {
            if (TextUtils.isEmpty(arrEditText[i].getText().toString())) {
                arrEditText[i - 1].setFocusableInTouchMode(false);
                arrEditText[i - 1].setFocusable(false);

                arrEditText[i].setFocusableInTouchMode(true);
                arrEditText[i].setFocusable(true);
                arrEditText[i].requestFocus();
                setShowMessage(i);
                return;
            }
        }
        //最終まで値のセットが終わっている場合
        int max = arrEditText.length - 1;
        arrEditText[max].setFocusableInTouchMode(false);
        arrEditText[max].setFocusable(false);
    }

    // startActivityForResult で起動させたアクティビティが
    // finish() により破棄されたときにコールされる
    // requestCode : startActivityForResult の第二引数で指定した値が渡される
    // resultCode : 起動先のActivity.setResult の第一引数が渡される
    // Intent data : 起動先Activityから送られてくる Intent
    //今のところSelectSagyoからの戻り値の取得専用
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle = data.getExtras();

        switch (requestCode) {
            case 1001:
                if (resultCode == RESULT_OK) {
                    txtSagyo.setText(bundle.getString("key.StringData"));
                    focusToNextControl();

                } else if (resultCode == RESULT_CANCELED) {
                    show.setText(
                            "requestCode:" + requestCode
                                    + "\nresultCode:" + resultCode
                                    + "\ndata:" + bundle.getString("key.canceledData"));
                }
                break;
            case 8888:
                Toast.makeText(this, "Setting has been completed.", Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            //Dialog(OK,Cancel Ver.)
            new AlertDialog.Builder(this)
                    .setTitle("確認")
                    .setMessage("終了してもよろしいですか？")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // OK button pressed
                            finishAndRemoveTask();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingIntent pendingIntent = this.createPendingIntent();
        // Enable NFC adapter
        this.nfcWriter.enable(this, pendingIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disable NFC adapter
        this.nfcWriter.disable(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.nfcWriter = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //設定画面呼び出し
            Intent intent = new Intent(this, Setting.class);
            int requestCode = SETTING;
            startActivityForResult(intent, requestCode);
            return true;
        }
        else if (id == R.id.action_reconnection) {
            show.setText("再接続に失敗しました。\n無線LAN状況を確認してください。");
            //再接続を行う
            clientThread = new ClientThread(handler, ip, myPort, false);
            // サーバ接続スレッド開始
            new Thread(clientThread).start();
        }
        else if (id == R.id.action_finish) {
            //Dialog(OK,Cancel Ver.)
            new AlertDialog.Builder(this)
                    .setTitle("確認")
                    .setMessage("終了してもよろしいですか？")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // OK button pressed
                            finishAndRemoveTask();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    //作業者選択画面呼び出し
    private void showSelectSagyo() {
        if (mSagyoName.equals("")) {
            show.setText("作業者名取得エラー。");
            return;
        }
        Intent intent = new Intent(this, SelectSagyo.class);
        intent.putExtra("name", mSagyoName);
        int requestCode = pc.SAG.getInt();
        startActivityForResult(intent, requestCode);
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }
}
