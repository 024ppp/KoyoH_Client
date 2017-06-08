package com.example.administrator.koyoh_client;

/**
 * Created by Administrator on 2017/05/12.
 */

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class ClientThread implements Runnable {
    private Socket s = null;
    private Handler handler;
    public Handler revHandler;
    BufferedReader br = null;
    OutputStream os = null;
    String ip;
    int myPort;

    //コンストラクタ
    public ClientThread(Handler handler, String ip, int myPort) {
        this.handler = handler;
        this.ip = ip;
        this.myPort = myPort;
    }

    public void run() {
        try {
            s = new Socket(ip, myPort);
            br = new BufferedReader(new InputStreamReader(
                    s.getInputStream()));
            os = s.getOutputStream();

            OutputStream out = s.getOutputStream();
            BufferedWriter bw = new BufferedWriter( new OutputStreamWriter(out, "UTF-8")  );
            //接続成功時
            bw.write(ProcessCommand.SAG.getString() + "\n");    //作業者検索コマンドを投げる
            //データを確定させて通信処理を起こさせる
            bw.flush();

            // スレッド起動
            new Thread() {
                @Override
                public void run() {
                    String content = null;
                    // Socketのinputストリーム読み取り
                    try {
                        while ((content = br.readLine()) != null) {
                            // Mainスレッドに通知
                            Message msg = new Message();
                            msg.what = 0x123;
                            msg.obj = content;
                            handler.sendMessage(msg);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            // Lopper初期化
            Looper.prepare();
            revHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // UIスレッドメッセージ取得
                    if (msg.what == 0x345) {
                        // サーバにチャット内容送信
                        try {
                            os.write((msg.obj.toString() + "\r\n")
                                    .getBytes("utf-8"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            // Looper起動
            Looper.loop();
        } catch (SocketTimeoutException e1) {
            System.out.println("TIME OUT！！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}