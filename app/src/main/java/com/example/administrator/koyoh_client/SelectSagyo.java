package com.example.administrator.koyoh_client;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import android.content.Intent;

public class SelectSagyo extends Activity implements View.OnClickListener {
    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_sagyo);

        Intent intent = getIntent();
        String names = intent.getStringExtra("name");
        String[] members = names.split(",");

        //String[] members = { names, "相田みつを", "石原さとみ", "宇野けんたろう", "江川卓",
        //        "オリバー・カーン", "河北麻友子", "木村拓哉", "蔵座敷", "県民共済", "コイキング" };

        lv = (ListView) findViewById(R.id.listView1);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1, members);

        lv.setAdapter(adapter);

        //リスト項目がクリックされた時の処理
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                String item = (String) listView.getItemAtPosition(position);

                // 返すデータ(Intent&Bundle)の作成
                Intent data = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("key.StringData", item);
                //bundle.putInt("key.intData", 123456789);
                data.putExtras(bundle);

                // setResult() で bundle を載せた
                // 送るIntent dataをセットする

                // 第一引数は…Activity.RESULT_OK,
                // Activity.RESULT_CANCELED など
                setResult(RESULT_OK, data);

                // finish() で終わらせて
                // Intent data を送る
                finish();
            }
        });

        lv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //リスト項目が選択された時の処理
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                String item = (String) listView.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(), item + " selected",
                        Toast.LENGTH_LONG).show();
            }
            //リスト項目がなにも選択されていない時の処理
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(getApplicationContext(), "no item selected",
                        Toast.LENGTH_LONG).show();
            }
        });

        //リスト項目が長押しされた時の処理
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                String item = (String) listView.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(), item + " long clicked",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        });
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

    }

}
