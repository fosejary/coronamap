package com.zeroninelab.coronamap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private AdView mAdView;
    private GoogleApiClient mGoogleApiClient = null;
    private Marker currentMarker = null;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 1000;
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500;

    private AppCompatActivity mActivity;
    boolean askPermissionOnceAgain = false;
    boolean mRequestingLocationUpdates = false;
    Location mCurrentLocation;
    boolean mMoveMapByUser = true;
    boolean mMoveMapByAPI = true;
    LatLng currentPosition;

    LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mActivity = this;


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        MobileAds.initialize(this, "ca-app-pub-7583212045442485~1722705008");

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            if (!mRequestingLocationUpdates) startLocationUpdates();
        }

        if (askPermissionOnceAgain) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionOnceAgain = false;
                checkPermissions();
            }
        }
    }

    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        }else {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }


            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            mRequestingLocationUpdates = true;

            mMap.setMyLocationEnabled(true);

        }

    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
    }

    public String getCurrentAddress(LatLng latlng) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }


        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }

    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap;

        List<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(245, 8, 2));
        colors.add(Color.rgb(247, 126, 6));
        colors.add(Color.rgb(248, 178, 5));
        colors.add(Color.rgb(74, 172, 38));
        colors.add(Color.rgb(25, 100, 69));

        colors.add(Color.rgb(34, 90, 165));
        colors.add(Color.rgb(48, 66, 150));
        colors.add(Color.rgb(61, 14, 123));
        colors.add(Color.rgb(145, 1, 124));
        colors.add(Color.rgb(153, 2, 52));

        colors.add(Color.rgb(206, 6, 31));
        colors.add(Color.rgb(0, 160, 140));
        colors.add(Color.rgb(0, 5, 130));
        colors.add(Color.rgb(110, 0, 90));
        colors.add(Color.rgb(0, 20, 30));

        String[] counts = {
                "1", "2", "3"
                //"4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"
        };
        String[] peopleInfos = {
                "중국 (여,35)", "한국 (남,55)", "한국 (남,54)"
                //"한국 (남,55)", "한국 (남,33)", "한국 (남,55)", "한국 (남,28)", "한국 (여,62)", "한국 (여,28)", "한국 (여,54)", "한국 (남,25)", "한국 (남,48)", "한국 (남,28)", "중국 (여,40)", "한국 (남,43)"
        };
        String[] contactCounts = {
                "45명", "75명", "98명"
                //"172명", "33명", "25명", "21명", "72명", "2명", "2명", "확인", "138명", "확인", "확인", "확인"
        };
        String[] nums = {
                "1", "1", "1"
                //"1", "1", "2", "1", "1", "2", "3", "3", "2", "1", "3", "1"
        };
        String[] reasons = {
                "-", "-", "-"
                //, "-", "-", "3번째 확진자 접촉", "-", "-", "5번째 확진자 접촉", "6번째 확진자 가족", "6번째 확진자 가족", "일본 확진자와 접촉", "-", "12번째 확진자 가족", "-"
        };
        String[] locks = {
                "인천의료원", "국립중앙의료원", "명지병원"
                //, "분당서울대병원", "서울의료원", "서울대병원", "서울의료원", "원광대병원", "서울의료원", "서울대병원", "서울대병원", "분당서울대병원", "국립중앙의료원", "분당서울대병원", "국군수도병원"
        };

        ArrayList<ArrayList<LatLng>> arrayLists = new ArrayList<>();

        LatLng INC_AIRPORT = new LatLng(37.4601908, 126.438507);

        //1
        ArrayList<LatLng> latLngs = new ArrayList<>();
        latLngs.add(INC_AIRPORT);
        latLngs.add(new LatLng(37.4785259, 126.6663152));
        arrayLists.add(latLngs);

        //2
        latLngs = new ArrayList<>();
        latLngs.add(new LatLng(37.5599086, 126.7941577));
        latLngs.add(new LatLng(37.5672412, 127.0034702));
        arrayLists.add(latLngs);

        //3
        latLngs = new ArrayList<>();
        latLngs.add(INC_AIRPORT);
        latLngs.add(new LatLng(37.524355, 127.0257595));
        latLngs.add(new LatLng(37.527737, 127.0302893));
        latLngs.add(new LatLng(37.5277357, 127.0149684));
        latLngs.add(new LatLng(37.5029926, 127.0469452));
        latLngs.add(new LatLng(37.5231801, 127.0108106));
        latLngs.add(new LatLng(37.4986582, 127.0473274));
        latLngs.add(new LatLng(37.4954707, 127.0424065));
        latLngs.add(new LatLng(37.6740054, 126.7747448));
        latLngs.add(new LatLng(37.6778604, 126.8099867));
        latLngs.add(new LatLng(37.6778604, 126.8099867));
        latLngs.add(new LatLng(37.681997, 126.7678513));
        latLngs.add(new LatLng(37.642134, 126.829043));
        arrayLists.add(latLngs);
//
//        //4
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //5
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //6
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //7
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //8
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //9
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //10
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //11
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //12
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //13
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //14
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);
//
//        //15
//        latLngs = new ArrayList<>();
//        latLngs.add(new LatLng());
//        latLngs.add(new LatLng());
//        arrayLists.add(latLngs);


        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < arrayLists.get(i).size(); j++) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.title(counts[i] + "번째 확진자");
                markerOptions.position(arrayLists.get(i).get(j));
                markerOptions.snippet(peopleInfos[i]);
                mMap.addMarker(markerOptions);
            }
        }


        for (int i = 0; i < arrayLists.size(); i++) {
            PolylineOptions polylineOptions = new PolylineOptions().width(6);
            for (int j = 0; j < arrayLists.get(i).size(); j++) {
                LatLng point = arrayLists.get(i).get(j);
                polylineOptions.color(colors.get(i)).geodesic(true);
                polylineOptions.add(point);
            }
            mMap.addPolyline(polylineOptions);
        }


//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(SEOUL);
//        markerOptions.title("서울");
//        markerOptions.snippet("한국의 수도");
//        mMap.addMarker(markerOptions);
//
        mMap.moveCamera(CameraUpdateFactory.newLatLng(INC_AIRPORT));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
    }

    @Override
    protected void onStart() {

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected() == false){

            mGoogleApiClient.connect();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {

        if (mRequestingLocationUpdates) {

            stopLocationUpdates();
        }

        if ( mGoogleApiClient.isConnected()) {

            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mRequestingLocationUpdates == false) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

                if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {

                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                } else {

                    startLocationUpdates();
                    mMap.setMyLocationEnabled(true);
                }

            } else {

                startLocationUpdates();
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (cause == CAUSE_NETWORK_LOST) {

        } else if (cause == CAUSE_SERVICE_DISCONNECTED) {

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        setDefaultLocation();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentPosition
                = new LatLng( location.getLatitude(), location.getLongitude());


        String markerTitle = getCurrentAddress(currentPosition);
        String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                + " 경도:" + String.valueOf(location.getLongitude());

        //현재 위치에 마커 생성하고 이동
        setCurrentLocation(location, markerTitle, markerSnippet);

        mCurrentLocation = location;
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {

        mMoveMapByUser = false;


        if (currentMarker != null) currentMarker.remove();


        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);


        currentMarker = mMap.addMarker(markerOptions);


        if (mMoveMapByAPI) {

            // CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 15);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
            mMap.moveCamera(cameraUpdate);
        }
    }

    public void setDefaultLocation() {

        mMoveMapByUser = false;


        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요";


        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mMap.moveCamera(cameraUpdate);

    }

    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        boolean fineLocationRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager
                .PERMISSION_DENIED && fineLocationRationale)
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

        else if (hasFineLocationPermission
                == PackageManager.PERMISSION_DENIED && !fineLocationRationale) {
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");
        } else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {

            if (mGoogleApiClient.isConnected() == false) {

                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (permsRequestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {

            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (permissionAccepted) {


                if (mGoogleApiClient.isConnected() == false) {
                    mGoogleApiClient.connect();
                }


            } else {
                checkPermissions();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                askPermissionOnceAgain = true;

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {


                        if (mGoogleApiClient.isConnected() == false) {

                            mGoogleApiClient.connect();
                        }
                        return;
                    }
                }

                break;
        }
    }
}