package com.example.administrator.koyoh_client;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Administrator on 2017/05/29.
 */

public class MyToast {
    public static Toast makeText(Context context, CharSequence text, int duration, float size ) {
        Toast result = new Toast( context );
        LayoutInflater inflate = (LayoutInflater)
                context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View v = inflate.inflate( R.layout.toast, null );
        TextView tv = (TextView) v.findViewById( R.id.message );
        tv.setText( text );
        tv.setTextSize( size );
        result.setView( v );
        result.setDuration( duration );
        return result;
    }
}
