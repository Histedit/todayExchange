package com.example.mihyeon.excharge;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/**
 * Created by Mihyeon on 2015-01-22.
 */
public class DBManager extends SQLiteOpenHelper {

    public DBManager(Context context, String name, CursorFactory factory, int version){
        super(context, name, factory, version);
    }
    @Override
    public void onCreate(SQLiteDatabase db){  //DB가 생성될 때 호출되는 메소드
        String sql = "CREATE TABLE country"+"(full_name text not null,"+"short_name text not null)";
        db.execSQL(sql);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){  //DB를 갈아엎고 새로 만들 필요가 있을 때 호출되는 메소드

    }

}
