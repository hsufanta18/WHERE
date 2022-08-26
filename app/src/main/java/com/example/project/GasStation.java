package com.example.project;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.Tm128;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.CompassView;
import com.naver.maps.map.widget.LocationButtonView;
import com.naver.maps.map.widget.ScaleBarView;
import com.naver.maps.map.widget.ZoomControlView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class GasStation extends AppCompatActivity implements OnMapReadyCallback, Overlay.OnClickListener, NaverMap.OnLocationChangeListener {
    private static final String TAG = "GasStation";
    GpsTracker gpsTracker;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    LinearLayout linearLayout;
    Marker[] markers = new Marker[150];
    NowGas[] nowGas = new NowGas[150];
    Button[] buttons = new Button[150];
    int nowcount = 0;
    String[] Os = new String[150];
    String[] Price = new String[150];
    String[] Distance = new String[150];
    String[] x = new String[150];
    String[] y = new String[150];
    String[] Trademark = new String[150];
    StringBuffer buffer = new StringBuffer();
    double latitude, longitude, lat, lon; //현재 위도 경도
    int number; // 길찾기 dialog 숫자
    private FusedLocationSource mLocationSource;
    NaverMap mNaverMap;
    int CC; // 마커 클릭 시 비교 숫자
    InfoWindow InfoWindow;

    LinearLayout.LayoutParams linearLayoutParams;
    LinearLayout.LayoutParams linearLayoutParams1;
    LinearLayout.LayoutParams linearLayoutParams2;
    LinearLayout.LayoutParams linearLayoutParams3;
    LinearLayout linearLayouts [] = new LinearLayout[150];// 추천뷰
    LinearLayout linearLayouts1 [] =new LinearLayout[150];// 추천뷰
    LinearLayout linearLayouts2 [] =new LinearLayout[150];// 추천뷰
    LinearLayout linearLayouts3 [] =new LinearLayout[150];// 추천뷰

    TextView info [] = new TextView[150];// 주유소 상호명
    TextView info1 [] = new TextView[150];// 추천뷰 가격
    TextView info2 [] = new TextView[150];// 추천뷰 km
    TextView info3 [] = new TextView[150];// 추천뷰 상표

    int sort = 1; // 가격순,거리순 초기값
    String prodcd = "B027"; // 기름 종류 초기값
    int km = 1000; // 반경 초기값

    Boolean isInitialUpdate = true;

    ImageView imageView[]= new ImageView[150]; //추천뷰의 마커 그림

    private long backKeyPressedTime = 0; //뒤로가기 초기값
    private Toast toast;// 뒤로가기 토스트 메시지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gas_main);
        // 지도 객체 생성
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT); }

        
        CreateObjectArray();


        //버튼을 누르면 주유소 상호정보가 뜸. ex) 빨간건 sk에너지 파란건 현대오일뱅크
        //AlertDialog에 여러 정보를 세팅할려면 먼저 AlerDialog.Builder객체를 생성.
        //아래에선 setView함수를 통해 inflater를 통해 반환받은 View객체를 화면에 띄우도록 설정   (setTitle, setMessage등등이 있음)
        //설정이 끝난 후 builder.create 여기선 dlg변수로 선언했으니 dlg.create를 통해 AlertDialog 객체를 생성하고 show함수를 통해 다이얼로그 창을 화면에 보여줌.
        //https://lktprogrammer.tistory.com/155
        Button button =(Button)findViewById(R.id.infomation);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(GasStation.this);
                LayoutInflater gascolor = LayoutInflater.from(GasStation.this);  // LayoutInflater는 xml에 정의된 Resource를 View객체로 반환해주는 역할
                final View view = gascolor.inflate(R.layout.gascolor,null);  // inflate함수를 통해 새로운 뷰를 생성 .  // https://yejinson97gaegul.tistory.com/entry/LayoutInflater%EB%9E%80
                dlg.setView(view);
                AlertDialog alertDialog = dlg.create();
                alertDialog.getWindow().setGravity(Gravity.BOTTOM);
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(255,62,79,92)));
                alertDialog.show();
            }
        });




        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        // getMapAsync를 호출하여 비동기로 onMapReady 콜백 메서드 호출
        // onMapReady에서 NaverMap 객체를 받음

        mapFragment.getMapAsync(this);
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);
        gpsTracker = new GpsTracker(GasStation.this);
        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();
        // 어플 시작 위도경도 값 저장
        Button now = findViewById(R.id.now);
        now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateNow(sort,prodcd,km);// 초기화 함수(가격 거리순, 기름종류, 반경 거리를 입력받아 초기화 함수 호출)
                Toast.makeText(GasStation.this, "추천뷰가 최신화 되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });// 마커 추천뷰 초기화 버튼


        linearLayout = findViewById(R.id.lin);
        linearLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        linearLayoutParams1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        linearLayoutParams2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        linearLayoutParams3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        updateNow(1,"B027",1000);

        Spinner spinner = findViewById(R.id.추천);//반경설정(spinner를 이용하여 1km,3km,5km 설정)
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0){
                    km = 1000;
                } else if(position == 1){
                    km = 3000;
                } else if(position == 2){
                    km = 5000;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Spinner spinner2 = findViewById(R.id.spinner2); // 기름종류설정
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0){
                    prodcd = "B027";
                } else if(position == 1){
                    prodcd = "D047";
                } else if(position == 2){
                    prodcd = "B034";
                } else if(position == 3){
                    prodcd = "C004";
                } else if(position == 4){
                    prodcd = "K015";
                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Spinner spinner3 = findViewById(R.id.spinner3); // 가격순, 거리순 설정
        spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0){
                    sort = 1;
                }
                else if(position == 1){
                    sort = 2;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d(TAG, "onMapReady");

        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);
        // UI 컨트롤 재배치
        mNaverMap.addOnLocationChangeListener(this);
        // UI 컨트롤 재배치
        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setCompassEnabled(false); // 기본값 : true
        uiSettings.setScaleBarEnabled(false); // 기본값 : true
        uiSettings.setZoomControlEnabled(false); // 기본값 : true
        uiSettings.setLocationButtonEnabled(false); // 기본값 : false
        uiSettings.setLogoGravity(Gravity.RIGHT | Gravity.BOTTOM);

        CompassView compassView = findViewById(R.id.compass);
        compassView.setMap(mNaverMap);
        ScaleBarView scaleBarView = findViewById(R.id.scalebar);
        scaleBarView.setMap(mNaverMap);
        ZoomControlView zoomControlView = findViewById(R.id.zoom);
        zoomControlView.setMap(mNaverMap);
        LocationButtonView locationButtonView = findViewById(R.id.location);
        locationButtonView.setMap(mNaverMap);

        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        InfoWindow = new InfoWindow();
        InfoWindow.setAdapter(new InfoWindow.DefaultViewAdapter(this) {
            @NonNull
            @Override
            protected View getContentView(@NonNull InfoWindow infoWindow) { // 마커 클릭시 정보 확인
                Marker marker = infoWindow.getMarker();
                View view = View.inflate(GasStation.this, R.layout.item, null);
                TextView title = (TextView) view.findViewById(R.id.title);
                TextView money = (TextView) view.findViewById(R.id.money);
                title.setText("주유소이름: " + nowGas[CC].getOS());
                money.setText("가격 :" + nowGas[CC].getPrice() + "원");
                return view;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // request code와 권한획득 여부 확인
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }

    @Override
    public boolean onClick(@NonNull Overlay overlay) {
        if (overlay instanceof Marker) {
            LatLng aa = ((Marker) overlay).getPosition();
            lat = aa.latitude;
            lon = aa.longitude;
            Marker marker = (Marker) overlay;
            for (int k = 0; k < nowcount; k++) {

                if ((nowGas[k].getX() == lat) && (nowGas[k].getY() == lon)) {
                    CC = k;
                }
            }
            if (marker.getInfoWindow() != null) {
                InfoWindow.close();
                Toast.makeText(this.getApplicationContext(), "정보창을 닫습니다.", Toast.LENGTH_LONG).show();
            } else {
                InfoWindow.open(marker);
                Toast.makeText(this.getApplicationContext(), "선택한 주유소입니다..", Toast.LENGTH_LONG).show();
            }

            return true;
        }
        return false;
    }


    public void nowGetXmlData(double lat, double lon, int sort, String prodcd, int km) { // 파싱 함수

        String queryUrl = "http://www.opinet.co.kr/api/aroundAll.do?code=F960210322&x=" + lat + "&y=" + lon + "&radius="+km+"&sort="+sort+"&prodcd="+prodcd+"&out=xml"; // 파싱 URL
        try {
            URL url = new URL(queryUrl);//문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is = url.openStream(); //url위치로 입력스트림 연결

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();//xml파싱을 위한
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new InputStreamReader(is, "UTF-8")); //inputstream 으로부터 xml 입력받기

            String tag;

            xpp.next();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("파싱 시작...\n\n");
                        break;

                    case XmlPullParser.START_TAG:
                        tag = xpp.getName();//테그 이름 얻어오기

                        if (tag.equals("OIL")) ;// 첫번째 검색결과
                        else if (tag.equals("POLL_DIV_CO")) {
                            xpp.next();
                            Trademark[nowcount] = xpp.getText();
                        }
                        else if (tag.equals("OS_NM")) {
                            xpp.next();
                            Os[nowcount] = xpp.getText();
                        } else if (tag.equals("PRICE")) {
                            xpp.next();
                            Price[nowcount] = xpp.getText();
                        } else if (tag.equals("DISTANCE")) {
                            xpp.next();
                            Distance[nowcount] = xpp.getText();
                        } else if (tag.equals("GIS_X_COOR")) {
                            xpp.next();
                            x[nowcount] = xpp.getText();
                        } else if (tag.equals("GIS_Y_COOR")) {
                            xpp.next();
                            y[nowcount] = xpp.getText();
                            nowcount++;
                        }


                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xpp.getName(); //테그 이름 얻어오기

                        if (tag.equals("OIL")) buffer.append("\n");// 첫번째 검색결과종료..줄바꿈
                        break;
                }

                eventType = xpp.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        buffer.append("파싱 끝\n");


    }//getXmlData method....


    public void updatemarkers(int i) { // 마커 찍는 함수
        markers[i] = new Marker();
        markers[i].setPosition(new LatLng(nowGas[i].getX(), nowGas[i].getY()));
        markers[i].setMap(mNaverMap);
        markers[i].setWidth(170);
        markers[i].setHeight(170);
        if (Trademark[i].equals("SKE") ){ //  상표명에 따라 마커 이미지 설정
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.skfinal2_foreground));
        }else if(Trademark[i].equals("GSC")) {
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.gsfinal2_foreground));
        }else if(Trademark[i].equals("HDO")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.hunfinal2_foreground));
        }else if(Trademark[i].equals("SOL")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.soilfinal2_foreground));
        }else if(Trademark[i].equals("RTO")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.salefinal2_foreground));
        }else if(Trademark[i].equals("RTX")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.roadfinal2_foreground));
        }else if(Trademark[i].equals("NHO")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.nongfinal2_foreground));
        }else if(Trademark[i].equals("ETC")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.selffinal2_foreground));
        }else if(Trademark[i].equals("E1G")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.e1final4_foreground));
        }else if(Trademark[i].equals("SKG")){
            markers[i].setIcon(OverlayImage.fromResource(R.mipmap.skgfinal2_foreground));
        }
        markers[i].setOnClickListener(this);
    }

    public void updateNow(int sort, String prodcd, int km){ // 추천뷰, 마커 초기화 함수
        if(isInitialUpdate == false) { //초기 업데이트가 아닐 시 마커 초기화
            for (int i = 0; i < nowcount; i++) { // 마커 초기화
                markers[i].setMap(null);
            }
        }
        else{ //초기 업데이트시
            isInitialUpdate = false;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                nowcount = 0;
                LatLng latLng = new LatLng(latitude,longitude);
                Tm128 tm128 = Tm128.valueOf(latLng);
                nowGetXmlData(tm128.x,tm128.y,sort,prodcd,km);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (nowcount != 0) {
                            linearLayout.removeAllViews();// 추천뷰 초기화
                            if (sort ==1) {
                                for (int i = 0; i < nowcount; i++) {
                                    for (int j = 0; j < nowcount; j++) {
                                        if (Float.parseFloat(Distance[i]) < Float.parseFloat(Distance[j]) && Integer.parseInt(Price[i]) == Integer.parseInt(Price[j])) {
                                            String temp = Distance[i]; Distance[i] = Distance[j]; Distance[j] = temp;
                                            temp = Os[i]; Os[i] = Os[j]; Os[j] = temp;
                                            temp = Price[i]; Price[i] = Price[j]; Price[j] = temp;
                                            temp = x[i]; x[i] = x[j]; x[j] = temp;
                                            temp = y[i]; y[i] = y[j]; y[j] = temp;
                                            temp = Trademark[i]; Trademark[i] = Trademark[j]; Trademark[j] = temp;
                                        }
                                    }
                                }
                            }
                            for (int i = 0; i < nowcount; i++) {
                                int ci = i;
                                nowGas[i] = new NowGas();
                                Tm128 tm128 = new Tm128(Double.parseDouble(x[i]), Double.parseDouble(y[i]));
                                LatLng latLng = tm128.toLatLng();
                                nowGas[i].setX(latLng.latitude);
                                nowGas[i].setY(latLng.longitude);
                                nowGas[i].setOS(Os[i]);
                                nowGas[i].setPrice(Integer.parseInt(Price[i]));
                                nowGas[i].setDistance(Float.parseFloat(Distance[i]));
                                updatemarkers(i);
                                linearLayouts[i] = new LinearLayout(GasStation.this);
                                linearLayouts[i].setOrientation(linearLayouts[i].HORIZONTAL);
                                linearLayouts1[i] = new LinearLayout(GasStation.this);
                                linearLayouts1[i].setOrientation(linearLayouts[i].HORIZONTAL);
                                linearLayoutParams3.width = 650;  linearLayoutParams3.height = 180;
                                linearLayouts1[i].setLayoutParams(linearLayoutParams3);
                                linearLayouts2[i] = new LinearLayout(GasStation.this);
                                linearLayouts2[i].setOrientation(linearLayouts[i].VERTICAL);
                                linearLayouts3[i] = new LinearLayout(GasStation.this);
                                linearLayouts3[i].setOrientation(linearLayouts[i].VERTICAL);
                                info3[i] = new TextView(GasStation.this);
                                if (Trademark[i].equals("SKE") ){
                                    info3[i].setText("SK에너지");
                                }else if(Trademark[i].equals("GSC")) {
                                    info3[i].setText("GS칼텍스");
                                }else if(Trademark[i].equals("HDO")){
                                    info3[i].setText("현대오일");
                                }else if(Trademark[i].equals("SOL")){
                                    info3[i].setText("S_OIL");
                                }else if(Trademark[i].equals("RTO")){
                                    info3[i].setText("자영알뜰");
                                }else if(Trademark[i].equals("RTX")){
                                    info3[i].setText("고속도로알뜰");
                                }else if(Trademark[i].equals("NHO")){
                                    info3[i].setText("NH알뜰");
                                }else if(Trademark[i].equals("ETC")){
                                    info3[i].setText("자가상표");
                                }else if(Trademark[i].equals("E1G")){
                                    info3[i].setText("E1");
                                }else if(Trademark[i].equals("SKG")){
                                    info3[i].setText("SK가스");
                                }
                                info3[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP,14.0f);
                                info3[i].setLayoutParams(linearLayoutParams);
                                info3[i].setTextColor(Color.WHITE);
                                info3[i].setBackground(ContextCompat.getDrawable(GasStation.this, R.drawable.shape));
                                linearLayouts3[i].addView(info3[i]);
                                info[i] = new TextView(GasStation.this);
                                info[i].setText(nowGas[i].getOS());
                                info[i].setTextColor(Color.BLACK);
                                info[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP,15.0f);
                                info[i].setLayoutParams(linearLayoutParams);
                                buttons[i] = new Button(GasStation.this);
                                linearLayoutParams2.width = 120; linearLayoutParams2.height = 100;
                                linearLayoutParams2.topMargin = 10;
                                buttons[i].setText("길찾기");
                                buttons[i].setTextSize(8);
                                buttons[i].setBackgroundColor(Color.BLACK);
                                buttons[i].setTextColor(Color.WHITE);
                                buttons[i].setLayoutParams(linearLayoutParams2);
                                buttons[i].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        lat = nowGas[ci].getX();
                                        lon = nowGas[ci].getY();
                                        AlertDialog.Builder dlg = new AlertDialog.Builder(GasStation.this);
                                        dlg.setTitle("길찾기"); //제목
                                        final String[] versionArray = new String[]{"카카오맵", "네이버 지도"};

                                        dlg.setSingleChoiceItems(versionArray, 0, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int a) {
                                                number = a;
                                            }
                                        });

                                        dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int a) {
                                                //토스트 메시지
                                                Intent intent;
                                                if (number == 0) {
                                                    try {
                                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("kakaomap://route?sp=" + latitude + "," + longitude + "&ep=" + lat + "," + lon + "&by=CAR"));
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(intent);
                                                    } catch (Exception e) {
                                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details?id=net.daum.android.map&hl=ko"));
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(intent);
                                                    }
                                                }

                                                else if (number == 1) {
                                                    try {
                                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("nmap://navigation?dlat=" + lat + "&dlng=" + lon + "&dname=목적지&appname=com.example.MapProject"));
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(intent);
                                                    } catch (Exception e) {
                                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details?id=com.skt.tmap.ku&hl=ko"));
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(intent);
                                                    }
                                                }
                                                Toast.makeText(GasStation.this, "확인을 눌르셨습니다.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        dlg.show();
                                    }
                                });
                                linearLayouts3[i].addView(buttons[i]);
                                linearLayouts1[i].addView(linearLayouts3[i]);
                                linearLayouts1[i].addView(info[i]);
                                linearLayoutParams1.width = 220; linearLayoutParams1.height = 150;
                                imageView[i] = new ImageView(GasStation.this);
                                if (Trademark[i].equals("SKE") ){
                                    imageView[i].setImageResource(R.mipmap.sk);
                                }else if(Trademark[i].equals("GSC")) {
                                    imageView[i].setImageResource(R.mipmap.gs);
                                }else if(Trademark[i].equals("HDO")){
                                    imageView[i].setImageResource(R.mipmap.hun);
                                }else if(Trademark[i].equals("SOL")){
                                    imageView[i].setImageResource(R.mipmap.soil);
                                }else if(Trademark[i].equals("RTO")){
                                    imageView[i].setImageResource(R.mipmap.sale);
                                }else if(Trademark[i].equals("RTX")){
                                    imageView[i].setImageResource(R.mipmap.road);
                                }else if(Trademark[i].equals("NHO")){
                                    imageView[i].setImageResource(R.mipmap.nong);
                                }else if(Trademark[i].equals("ETC")){
                                    imageView[i].setImageResource(R.mipmap.self);
                                }else if(Trademark[i].equals("E1G")){
                                    imageView[i].setImageResource(R.mipmap.e1);
                                }else if(Trademark[i].equals("SKG")){
                                    imageView[i].setImageResource(R.mipmap.skg);
                                }
                                imageView[i].setLayoutParams(linearLayoutParams1);
                                info1[i] = new TextView(GasStation.this);
                                info1[i].setText(nowGas[i].price+"원");
                                info1[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP,19.0f);
                                info1[i].setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                                info1[i].setTextColor(Color.BLACK);
                                info1[i].setLayoutParams(linearLayoutParams);
                                linearLayouts2[i].addView(info1[i]);
                                info2[i] = new TextView(GasStation.this);
                                float nNumber = nowGas[i].distance/1000;
                                String strNumber = String.format("%.2f", nNumber);
                                info2[i].setText(strNumber+"km");
                                info2[i].setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                                info2[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP,19.0f);
                                info2[i].setTextColor(Color.BLACK);
                                info2[i].setLayoutParams(linearLayoutParams);
                                linearLayouts2[i].addView(info2[i]);
                                linearLayouts[i].setBackground(ContextCompat.getDrawable(GasStation.this, R.drawable.main));
                                linearLayouts[i].addView(imageView[i]);
                                linearLayouts[i].addView(linearLayouts1[i]);
                                linearLayouts[i].addView(linearLayouts2[i]);
                                linearLayouts[i].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(nowGas[ci].getX(), nowGas[ci].getY())).animate(CameraAnimation.Easing);
                                        mNaverMap.moveCamera(cameraUpdate);
                                        for (int i = 0; i < nowcount; i++) {
                                            Tm128 tm128 = new Tm128(Double.parseDouble(x[i]), Double.parseDouble(y[i]));
                                            LatLng latLng = tm128.toLatLng();
                                            if (nowGas[ci].getX() == latLng.latitude && nowGas[ci].getY() == latLng.longitude) {
                                                CC = i;
                                                lat = markers[i].getPosition().latitude;
                                                lon = markers[i].getPosition().longitude;
                                                if (markers[i].getInfoWindow() == null) {
                                                    InfoWindow.open(markers[CC]);
                                                }
                                            }
                                        }
                                    }
                                });
                                linearLayout.addView(linearLayouts[i]);
                            }
                        }
                        else{
                            linearLayout.removeAllViews();
                            TextView asd = new TextView(GasStation.this);
                            asd.setText("주변에 주유소가 없습니다.");
                            asd.setLayoutParams(linearLayoutParams);
                            linearLayout.addView(asd);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onLocationChange(@NonNull Location location) { // 현재위치 변경 시, 자동 호출 함수
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    public void onBackPressed() { // 뒤로가기 함수
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(this, "뒤로 가기 버튼을 한 번 더 누르시면 종료됩니다.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finish();
            toast.cancel();
            toast = Toast.makeText(this, "이용해 주셔서 감사합니다.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void CreateObjectArray(){
        for(int i = 0 ; i < 150; i++){ //배열 값 저장
            info[i] = new TextView(GasStation.this);
            info1[i] = new TextView(GasStation.this);
            info2[i] = new TextView(GasStation.this);
            info3[i] = new TextView(GasStation.this);
            linearLayouts [i] = new LinearLayout(GasStation.this);
            linearLayouts1 [i] = new LinearLayout(GasStation.this);
            linearLayouts2 [i] = new LinearLayout(GasStation.this);
            linearLayouts3 [i] = new LinearLayout(GasStation.this);
            Os[i] = new String();
            Price[i] = new String();
            Distance[i] = new String();
            x[i] = new String();
            y[i] = new String();
            Trademark[i] = new String();
            markers[i] = new Marker();
            nowGas[i] = new NowGas();
            buttons[i]  = new Button(GasStation.this);
            imageView[i]= new ImageView(GasStation.this);
        }
    }

}


