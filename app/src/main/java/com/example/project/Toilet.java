package com.example.project;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public class Toilet extends AppCompatActivity implements OnMapReadyCallback,Overlay.OnClickListener {
    private static final String TAG = "Toilet";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    StringBuffer buffer = new StringBuffer();
    int count = 0;
    String[] str_toilet_name = new String[35000];
    String[] str_toilet_addr = new String[35000];
    String[] str_openTime = new String[35000];
    String[] str_toilet_latitude = new String[35000];
    String[] str_toilet_longitude = new String[35000];
    Marker[] markers = new Marker[35000];


    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    private long backKeyPressedTime = 0;

    private Toast toast;
    private Button camera_btn;
    ProgressDialog progressDialog;

    com.naver.maps.map.overlay.InfoWindow InfoWindow;
    double lat, lon;  // 위도 경도

    int SearchMarkerIndex = 0; //마커클릭시 비교 숫자

    int int_nav_map_index; // 길찾기 선택 시 네이버맵 or 카카오맵 선택을 넘겨주기 위한 인덱스
    int int_markNumber = 0;
    int int_kind;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toilet_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        //Activity를 full screen으로 만들기 (status bar 숨기기)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        // 지도 객체 생성
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.toilet_map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.toilet_map, mapFragment).commit();
        }

        // getMapAsync를 호출하여 비동기로 onMapReady 콜백 메서드 호출
        // onMapReady에서 NaverMap 객체를 받음
        mapFragment.getMapAsync(this);

        // 위치를 반환하는 구현체인 FusedLocationSource 생성
        mLocationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);


        camera_btn = findViewById(R.id.toilet_camera);
        camera_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(com.example.project.Toilet.this, camera.class);
                startActivity(intent);
            }
        });


        MyToiletAsyncTask asyncTask = new MyToiletAsyncTask();
        asyncTask.execute();// 파싱 Task 실행

        Spinner spinner2 = findViewById(R.id.kind); //무료,유료 구분하여 선택가능하도록 설정
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    int_kind = 0;
                } else if (position == 1) {
                    int_kind = 1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button button = findViewById(R.id.check);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateMarkers(int_kind);
            }
        });



    }

    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d(TAG, "onMapReady");
        // 지도상에 마커 표시
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);
        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setCompassEnabled(false); // 기본값 : true
        uiSettings.setScaleBarEnabled(false); // 기본값 : true
        uiSettings.setZoomControlEnabled(false); // 기본값 : true
        uiSettings.setLocationButtonEnabled(false); // 기본값 : false
        uiSettings.setLogoGravity(Gravity.RIGHT | Gravity.BOTTOM);

        uiSettings.setScrollGesturesEnabled(true); // 기본값 : true  여러 제스쳐가 있는데 기본값은 모두 true로 되어있음.


        CompassView compassView = findViewById(R.id.toilet_compass);
        compassView.setMap(mNaverMap);
        ScaleBarView scaleBarView = findViewById(R.id.toilet_scalebar);
        scaleBarView.setMap(mNaverMap);
        ZoomControlView zoomControlView = findViewById(R.id.toilet_zoom);
        zoomControlView.setMap(mNaverMap);
        LocationButtonView locationButtonView = findViewById(R.id.toilet_location);
        locationButtonView.setMap(mNaverMap);

        LatLng initialPosition = new LatLng(37.506855, 127.066242);

        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(initialPosition);
        naverMap.moveCamera(cameraUpdate);


        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);

        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        InfoWindow = new InfoWindow(); //마커 클릭시 보여지는 기본정보창
        InfoWindow.setAdapter(new InfoWindow.DefaultViewAdapter(this) {
            @NonNull
            protected View getContentView(@NonNull InfoWindow infoWindow) {
                Marker marker = infoWindow.getMarker();
                View view = View.inflate(com.example.project.Toilet.this, R.layout.item1, null);
                TextView title = (TextView) view.findViewById(R.id.prkplceNm);
                TextView money = (TextView) view.findViewById(R.id.operday);
                title.setText("화장실이름: " + str_toilet_name[SearchMarkerIndex]);
                money.setText("운영시간 :" + str_openTime[SearchMarkerIndex]);
                return view;
            }
        });


        Button b4 = (Button) findViewById(R.id.toilet_search_btn); //지역 검색 기능
        final EditText et3 = (EditText) findViewById(R.id.toilet_address_input);

        final Geocoder geocoder = new Geocoder(this); //
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 주소입력후 지도버튼 클릭시 해당 위도경도값의 지도화면으로 이동
                List<Address> list = null;

                String str = et3.getText().toString();
                try {
                    list = geocoder.getFromLocationName
                            (str, // 지역 이름
                                    10); // 읽을 개수
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
                }
                if (list != null) {
                    if (list.size() != 0) {
                        // 해당되는 주소로 인텐트 날리기
                        Address addr = list.get(0);
                        double lat = addr.getLatitude();
                        double lon = addr.getLongitude();
                        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(
                                        new LatLng(lat, lon), 15)
                                .animate(CameraAnimation.Fly, 3000);
                        naverMap.moveCamera(cameraUpdate);


                    }
                }


            }
        });
    }


    private void getToiletXmlData(int q){
        String queryUrl = "http://api.data.go.kr/openapi/tn_pubr_public_toilet_api?serviceKey=2oGAFASBFjG7%2Bea%2FJZLVo1vqgs8P%2FTw7alO5%2Bj3H4oBiIQaP%2FZxrcZEdlVTm3zKkHxg%2FhLsGYfnVBjAs6sLESA%3D%3D&pageNo=0&numOfRows=35000&type=xml";

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
                        tag = xpp.getName();

                        if (tag.equals("item")) ;
                        else if (tag.equals("toiletNm")) { //화장실 이름
                            xpp.next();
                            str_toilet_name[count] = xpp.getText();

                        }
                        else if (tag.equals("rdnmadr")) { //화징실 도로명 주소
                            xpp.next();
                            str_toilet_addr[count] = xpp.getText();

                        }
                        else if (tag.equals("openTime")) { //개방시간
                            xpp.next();
                            str_openTime[count] = xpp.getText();
                        }
                        else if (tag.equals("latitude")) { //위도
                            xpp.next();
                            str_toilet_latitude[count] = xpp.getText();

                        }
                        else if (tag.equals("longitude")) { //경도
                            xpp.next();
                            str_toilet_longitude[count] = xpp.getText();
                            count++;
                        }


                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag = xpp.getName(); //테그 이름 얻어오기
                        if (tag.equals("item"))
                            buffer.append("\n");// 첫번째 검색결과종료 줄바꿈
                        break;
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        buffer.append("파싱 끝\n");
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
            for (int k = 0; k < count; k++) {
                if ((str_toilet_latitude[k] != null) && (str_toilet_longitude[k] != null)) {
                    if ((Double.parseDouble(str_toilet_latitude[k]) == lat) && (Double.parseDouble(str_toilet_longitude[k]) == lon)) {
                        SearchMarkerIndex = k;
                        continue;
                    }
                }
            }
            if (marker.getInfoWindow() != null) { //마커 클릭시 다이어로그를 활용하여 상세정보 표시
                InfoWindow.close();
                Toast.makeText(this.getApplicationContext(), "정보창을 닫습니다.", Toast.LENGTH_LONG).show();
            } else {
                InfoWindow.open(marker);
                AlertDialog.Builder dlg = new AlertDialog.Builder(com.example.project.Toilet.this);
                dlg.setTitle("상세정보"); //제목
                dlg.setMessage("주소 : " + str_toilet_addr[SearchMarkerIndex] + "\n운영시간 : " + str_openTime[SearchMarkerIndex] + "\n이름 : " + str_toilet_name[SearchMarkerIndex] + "대");


                dlg.setPositiveButton("길찾기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int a) { //길찾기 버튼 클릭시 주소값을 intent하여 다른 내비게이션 어플과 연동

                        AlertDialog.Builder dlg = new AlertDialog.Builder(com.example.project.Toilet.this);
                        dlg.setTitle("길찾기");
                        final String[] versionArray = new String[]{"카카오맵", "네이버 지도"};
                        dlg.setSingleChoiceItems(versionArray, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int a) {
                                int_nav_map_index = a;
                            }
                        });
                        dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int a) {
                                //토스트 메시지
                                Intent intent;
                                if (int_nav_map_index == 0) {
                                    try {
                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("kakaomap://route?ep=" + Double.parseDouble(str_toilet_latitude[SearchMarkerIndex]) + "," + Double.parseDouble(str_toilet_longitude[SearchMarkerIndex]) + "&by=CAR"));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details?id=net.daum.android.map&hl=ko"));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }

                                } else if (int_nav_map_index == 1) {
                                    try {
                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("nmap://navigation?dlat=" + lat + "&dlng=" + lon + "&dname=목적지&appname=com.example.maptest"));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details?id=com.skt.tmap.ku&hl=ko"));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                }
                                Toast.makeText(com.example.project.Toilet.this, "확인을 눌르셨습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        dlg.show();
                    }
                });
                AlertDialog alertDialog = dlg.create();
                alertDialog.getWindow().setGravity(Gravity.BOTTOM);
                alertDialog.show();

            }

            return true;
        }
        return false;

    }


    public void onBackPressed() { //종료방법 설정
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

    public class MyToiletAsyncTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... strings) {
            getToiletXmlData(0);// 파싱 실행
            return true;
        }

        @Override
        protected void onPreExecute() {// progressDialog창 구현(파싱값 저장전까지 실행)
            progressDialog = ProgressDialog.show(Toilet.this, "잠시만 기다려주세요", "진행중입니다.", true);
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean s) {// 초기 무료 마커 설정
            super.onPostExecute(s);
            for (int i = 0; i < count; i++) {
                if (str_toilet_name[i] == null || str_toilet_longitude[i] == null || str_toilet_latitude[i] == null || str_toilet_addr[i] == null) {
                    continue;
                }
                markers[int_markNumber] = new Marker();
                markers[int_markNumber].setPosition(new LatLng(Double.parseDouble(str_toilet_latitude[i]), Double.parseDouble(str_toilet_longitude[i])));
                markers[int_markNumber].setHideCollidedMarkers(true);
                markers[int_markNumber].setWidth(50);
                markers[int_markNumber].setHeight(50);
                markers[int_markNumber].setIcon(OverlayImage.fromResource(R.drawable.ic_parking));
                markers[int_markNumber].setMap(mNaverMap);
                markers[int_markNumber].setOnClickListener(com.example.project.Toilet.this);
                int_markNumber++;
            }
            progressDialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled(Boolean s) {
            super.onCancelled(s);
        }
    }

    public void UpdateMarkers(int s) {// 무료,유료 마커 설정
        for (int i = 0; i < int_markNumber; i++) {
            markers[i].setMap(null);
        }// 마커 초기화
        int_markNumber = 0;
        for (int i = 0; i < count; i++) {
            if (str_toilet_name[i] == null || str_toilet_longitude[i] == null || str_toilet_latitude[i] == null || str_toilet_addr[i] == null) {
                continue;
            }
            markers[int_markNumber] = new Marker();
            markers[int_markNumber].setPosition(new LatLng(Double.parseDouble(str_toilet_latitude[i]), Double.parseDouble(str_toilet_longitude[i])));
            markers[int_markNumber].setHideCollidedMarkers(true);
            markers[int_markNumber].setWidth(50);
            markers[int_markNumber].setHeight(50);
            markers[int_markNumber].setIcon(OverlayImage.fromResource(R.drawable.ic_parking));
            markers[int_markNumber].setMap(mNaverMap);
            markers[int_markNumber].setOnClickListener(com.example.project.Toilet.this);
            int_markNumber++;
        }
    }


}