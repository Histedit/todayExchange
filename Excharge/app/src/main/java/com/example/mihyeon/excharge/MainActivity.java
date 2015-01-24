package com.example.mihyeon.excharge;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity implements OnEditorActionListener {


    Handler handler = new Handler();
    TextView ex_rate;  //환율 비율
    EditText edit_from;
    TextView edit_to;
    StringBuilder strBuilder = new StringBuilder();
    String tag_value;

    SQLiteDatabase db;
    DBManager helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ex_rate= (TextView)findViewById(R.id.excharge_rate);

        edit_to = (TextView)findViewById(R.id.edit_to);
        edit_from = (EditText)findViewById(R.id.edit_from);
        edit_from.setOnEditorActionListener(this);


        //db생성
        helper = new DBManager(MainActivity.this, "Excharge.db",null,1); //현재화면의 context, 파일명, 커서 팩토리, 버전 번호

        insert("대한민국","KRW");
        /*insert("미국","USD");
        insert("일본","JPY");
        insert("중국","CNY");
        insert("호주","AUD");
        insert("캐나다","CAD");
        insert("뉴질랜드","NZD");*/

        select();

    }

    public void insert(String full_name, String short_name){
        db = helper.getWritableDatabase();  //db 객체를 얻어온다. 쓰기가능
        Cursor c = db.query("country",null,null,null,null,null,null);
        Boolean db_check = false;
        String db_value;

        while(c.moveToNext()){
            db_value = c.getString(c.getColumnIndex("full_name"));
            if(db_value.equals(full_name)){
               db_check = true;
            }
        }

        if(!true){
            ContentValues values = new ContentValues();
            values.put("full_name", full_name);
            values.put("short_name", short_name);
            db.insert("country", null, values);
        }
    }

    public void delete(){

    }

    public void select(){
        db = helper.getReadableDatabase(); //db객체를 얻어온다. 읽기전용
        Cursor c = db.query("country",null,null,null,null,null,null);
        /*
        위의 결과는 select * from counrty가 된다. Cursor는 DB결과를 저장한다.
        query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy)
         */

        while(c.moveToNext()){
            String full_name = c.getString(c.getColumnIndex("full_name"));
            String short_name = c.getString(c.getColumnIndex("short_name"));

            Log.i("db","full_name"+full_name+", short_name"+short_name);
        }

    }


    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if(actionId == EditorInfo.IME_ACTION_DONE){   //edittext에서 숫자 입력후 완료 눌렀을때 해야할 일들.
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edit_from.getWindowToken(), 0);   // 완료클릭시 edittext관련 키보드 제거
            edit_from.clearFocus(); //edittext 포커스 제거

            new Thread(){  // 환율정보 보여주기
                public void run(){
                    connection();
                }
            }.start();

        }
        return false;
    }


    private void connection(){

        try{
            URL url = new URL("http://www.webservicex.net/CurrencyConvertor.asmx/ConversionRate?FromCurrency=USD&ToCurrency=KRW");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            if(conn!=null) {
                conn.setConnectTimeout(10000);  //서버 접속시 연결시간
                conn.setRequestMethod("GET");  // 서버 요청방식
                int resCode = conn.getResponseCode();
                if(resCode == HttpURLConnection.HTTP_OK) {  //서버연결 성공시(200)
                    InputStream is = conn.getInputStream();
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    XmlPullParser parser = factory.newPullParser();
                    //parser.setInput(new StringReader(content.replace("&","&amp;")));
                    parser.setInput(is, "UTF-8");


                    int eventType = parser.getEventType();
                    String tag = "";  // XML파일 내 double 태그값을 받기위함.

                    while(eventType != XmlPullParser.END_DOCUMENT) {
                        String tagName = parser.getName();
                        switch(eventType){
                            case XmlPullParser.START_TAG:  //2
                                tag = parser.getName();
                                Log.i("(Start_Tag)names are ",tag);  //double
                                break;
                            case XmlPullParser.START_DOCUMENT:  //0
                                Log.i("(Start_Document)names are ",tag);
                                break;
                            case XmlPullParser.END_DOCUMENT:  //1
                                Log.i("(End_Document)names  ", tag);
                                break;
                            case XmlPullParser.END_TAG:   //3
                                tag = "nothing";
                                break;
                            case XmlPullParser.TEXT: //4
                                if(tag.equals("double")){
                                    tag_value = parser.getText();
                                }
                                break;
                        }

                        eventType = parser.next();
                    }
                }
            }

            //화면처리 담당 handler
            handler.post(new Runnable() {
                public void run() {
                    // 기준 x 비율 보여주기
                    ex_rate.setText("기준 x "+tag_value.toString());
                    ex_rate.setVisibility(View.VISIBLE);

                    // 입력값 x 비율 = 환율 가격 보여주기
                    Double tmp_value1 = Double.parseDouble("" + edit_from.getText());
                    Double tmp_value2 = Double.parseDouble(tag_value);
                    Double tmp_result = tmp_value1 * tmp_value2;
                    String result = String.format("%.2f", tmp_result);
                    edit_to.setText(result);

                }
            });
        }catch(Exception ex){
            Log.e("접속오류", ex.toString());
        }

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
