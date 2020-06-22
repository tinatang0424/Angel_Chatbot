package com.addzero;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity implements RecognitionListener {

    boolean hasAllPermission;

    List<ScanResult> mWifiScanResultList;
    List<WifiConfiguration> mWifiConfigurationList;
    WifiInfo mWifiInfo;
    String IP; //Wi-Fi IP位置
    String serverIP;
    String SSID; //Wi-Fi名稱
    int LEVEL; //Wi-Fi訊號強弱
    int NETWORKID; //Wi-Fi連線ID
    int IPADRRESS; //Wi-Fi連線位置

    TextView tvResponse;

    SpeechRecognizer speech;
    Intent recognizerIntent;
    String finResult;
    TextToSpeech tts;

    Button btnSpeech;

    ImageView imageView;

    int[] picArr = new int[] {R.drawable.pic1,R.drawable.pic2,R.drawable.pic3,R.drawable.pic4,R.drawable.pic5,
            R.drawable.pic6,R.drawable.pic7,R.drawable.pic8,R.drawable.pic9,R.drawable.pic10,R.drawable.pic11,
            R.drawable.pic12,R.drawable.pic13,R.drawable.pic14,R.drawable.pic15};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();   //權限

        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //WifiDetect(mWifiManager);   //取得IP
        serverIP = "http://" + "192.168.1.124" + ":9999/";
        Toast.makeText(MainActivity.this, serverIP, Toast.LENGTH_SHORT).show();

        Init();   //初始
        Init_SpeechRecognizer();   //初始STT

        speechOnClick();

        randomPic();

        imageOnClick();

    }

    /*隨機圖片*/
    private void randomPic() {
        int i = (int)(Math.random()* 14);
        imageView.setImageResource(picArr[i]);
    }

    private void imageOnClick() {
        imageView.setOnClickListener(v -> {
            randomPic();
        });
    }

    /*初始化*/
    private void Init() {

        tvResponse = findViewById(R.id.textView);
        tvResponse.setSelected(true);   //啟動跑馬燈

        btnSpeech = findViewById(R.id.btn_speech);

        imageView = findViewById(R.id.imageView);

    }

    /*初始化STT*/
    private void Init_SpeechRecognizer() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-TW");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    /*點擊按鈕開始監聽*/
    private void speechOnClick() {
        btnSpeech.setOnClickListener(v -> {
            speech.startListening(recognizerIntent);   //呼叫STT，開始聆聽
        });
    }

    /*得到IP位址*/
    @SuppressLint("MissingPermission")
    private void WifiDetect(WifiManager mWifiManager) {

        // 先判斷是否有開啟Wi-Fi，有開啟則回傳true沒有則回傳false
        if (mWifiManager.isWifiEnabled()) {

            mWifiManager.startScan();   //重新掃描Wi-Fi資訊

            mWifiScanResultList = mWifiManager.getScanResults(); //偵測周圍的Wi-Fi環境(因為會有很多組Wi-Fi，所以型態為List)

            mWifiConfigurationList = mWifiManager.getConfiguredNetworks();
            //目前已連線的Wi-Fi資訊
            mWifiInfo = mWifiManager.getConnectionInfo();

            for(int i = 0 ; i < mWifiScanResultList.size() ; i++ )
            {
                //手機目前周圍的Wi-Fi環境
                SSID = mWifiScanResultList.get(i).SSID ; //SSID (Wi-Fi名稱)
                LEVEL = mWifiScanResultList.get(i).level ; //LEVEL (Wi-Fi訊號強弱)
            }

            for(int i = 0 ; i < mWifiConfigurationList.size() ; i++ )
            {
                //手機內已儲存(已連線過)的Wi-Fi資訊
                SSID = mWifiConfigurationList.get(i).SSID ;
                NETWORKID = mWifiConfigurationList.get(i).networkId ; //NETWORKID (Wi-Fi連線ID)
            }

            //目前手機已連線(現在連線)的Wi-Fi資訊
            SSID = mWifiInfo.getSSID() ;
            NETWORKID = mWifiInfo.getNetworkId() ;
            IPADRRESS = mWifiInfo.getIpAddress() ; //IPADRRESS (Wi-Fi連線位置)
            IP = String.format("%d.%d.%d.%d", (IPADRRESS & 0xff), (IPADRRESS >> 8 & 0xff), (IPADRRESS >> 16 & 0xff),( IPADRRESS >> 24 & 0xff)) ; //(Wi-Fi IP位置)
        }
        else
        {
            //把Wi-Fi開啟
            mWifiManager.setWifiEnabled(true);
        }

        serverIP = "http://" + IP + ":9999/";
    }



    /*權限*/
    @SuppressLint("CheckResult")
    private void requestPermissions() {
        final RxPermissions rxPermissions = new RxPermissions(this);

        rxPermissions
                .request(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.RECORD_AUDIO)
                .subscribe(granted -> {
                    if (granted) {
                        // All requested permissions are granted
                        hasAllPermission = true;
                    } else {
                        // At least one permission is denied
                        requestPermissions();
                    }
                });
    }

    /*STT*/
    @Override
    public void onReadyForSpeech(Bundle params) {
        runOnUiThread(() -> tvResponse.setText("請開始說話"));   //在TextView顯示回傳的文字

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        System.out.println(error+"錯誤");
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    @Override
    public void onResults(Bundle results) {
        if(results!=null){
            ArrayList matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            assert matches != null;
            finResult = matches.get(0).toString();
            //runOnUiThread(() -> tvResponse.setText(finResult));
            uploadTxt(finResult);

        }
    }

    /*上傳字串*/
    private void uploadTxt(String text) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(1000, TimeUnit.MILLISECONDS)
                .readTimeout(1000, TimeUnit.MILLISECONDS)
                .build();

        OkHttpUtils.initClient(client);

        OkHttpUtils.post()
                .addParams("UserSay", text)
                .url(serverIP)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.d("ttt","文字傳送錯誤");
                        Log.e("eee",e.toString());
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Log.d("rrr",response);
                        runOnUiThread(() -> tvResponse.setText(response));   //在TextView顯示回傳的文字
                        tts.speak(response, TextToSpeech.QUEUE_FLUSH, null);   //TTS
                    }
                });
    }


    @Override
    protected void onPause() {

        if (tts!=null) {
            tts.stop();
            tts.shutdown();
        }

        if (speech!=null) {
            speech.stopListening();
            speech.cancel();
        }

        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

        /*TTS*/
        tts = new TextToSpeech(this, status -> {
            if(status==TextToSpeech.SUCCESS) {
                int result=tts.setLanguage(Locale.TAIWAN);
                if (result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED)
                    Log.i("TextToSpeech", "Language Not Supported");

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.i("TextToSpeech", "On Start");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.i("TextToSpeech", "On Done");

                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.i("TextToSpeech", "On Error");
                    }
                });
            }
            else
                Log.i("TextToSpeech", "Initialization Failed");
        });
    }
}