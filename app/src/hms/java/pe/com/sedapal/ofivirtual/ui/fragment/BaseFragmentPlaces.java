package pe.com.sedapal.ofivirtual.ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.Marker;

import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.com.sedapal.ofivirtual.AndroidApplication;
import pe.com.sedapal.ofivirtual.R;
import pe.com.sedapal.ofivirtual.data.util.LogUtil;
import pe.com.sedapal.ofivirtual.di.component.CommercialOfficeComponent;
import pe.com.sedapal.ofivirtual.model.SubsidiaryModel;
import pe.com.sedapal.ofivirtual.presenter.view.BaseFragmentView;
import pe.com.sedapal.ofivirtual.util.CommonUtil;

public abstract class BaseFragmentPlaces extends BaseFragment implements
        BaseFragmentView,
        OnMapReadyCallback{

    public static final String TAG = BaseFragmentPlaces.class.getSimpleName();
    public static final String LOCATION_KEY = "LOCATION";
    public static final long UPDATE_INTERVAL = 20 * 1000;  /* 10 secs */
    public static final int REQUEST_DISABLED = 10001;
    public static final int REQUEST_LOCATION = 10004;
    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 34243;

    public HuaweiMap goHuaweiMap;
    public LatLng goCurrentLatLng;
    public Float goZoom = 12f;
    public LocationCallback mLocationCallback;
    private boolean isFirstLoad = true;
    private long mLastClickTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getComponent(CommercialOfficeComponent.class).inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (isHuaweiServicesAvailable() && (isEnabledGPS())) setUpMaps();
        updateValuesFromBundle(savedInstanceState);
        isFirstLoad = !CommonUtil.getFirstLoadMap(getContext());
    }

    public boolean isHuaweiServicesAvailable() {
        HuaweiApiAvailability loHuaweiApiAvailability = HuaweiApiAvailability.getInstance();
        int liStatus = loHuaweiApiAvailability.isHuaweiMobileServicesAvailable(context());
        if (liStatus == ConnectionResult.SUCCESS) {
            return true;
        } else {
            loHuaweiApiAvailability.getErrorDialog(getActivity(), liStatus, REQUEST_DISABLED, dialog -> {
                //finish();
            }).show();
            return false;
        }
    }

    private boolean isEnabledGPS() {
        final LocationManager manager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_DISABLED:
                break;

            case REQUEST_LOCATION:
                startLocationUpdates();
                break;
        }
    }

    public void setUpMaps() {
        SupportMapFragment goMapFragment = SupportMapFragment.newInstance();
        final FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.frlContainer, goMapFragment);
        fragmentTransaction.commit();

        goMapFragment.onAttach(getContext());
        goMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(HuaweiMap poGoogleMap) {
        customOnMapReady(poGoogleMap);
    }

    abstract void customOnMapReady(HuaweiMap poGoogleMap);

    void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onLocationChanged(locationResult.getLastLocation());
                finishLocationUpdates(); // after get the first location
            }
        };
        LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getContext()))
                .requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    private void finishLocationUpdates() {
        if (mLocationCallback != null) {
            LocationServices.getFusedLocationProviderClient(Objects.requireNonNull(getContext()))
                    .removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, goCurrentLatLng);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                goCurrentLatLng = savedInstanceState.getParcelable(LOCATION_KEY);
            }
        }
    }

    public void onLocationChanged(Location poLocation) {
        LogUtil.i(TAG, "onLocationChanged - INIT");
        onLocationChangedCamera(new LatLng(poLocation.getLatitude(), poLocation.getLongitude()));
    }

    private void onLocationChangedCamera(LatLng poLatLng) {
        this.goCurrentLatLng = poLatLng;
        goZoom = 12f;
        CameraUpdate goCameraUpdate = CameraUpdateFactory.newLatLngZoom(this.goCurrentLatLng, goZoom);
        goHuaweiMap.animateCamera(goCameraUpdate);
        setUpMaps();
    }

    public void onBackPressed() {
    }

    @Override
    public void showRetry() {
    }

    @Override
    public void hideRetry() {
    }

    @Override
    public void showError(String psMessage) {
    }

    @Override
    public Context context() {
        return getActivity();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMaps();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(goCurrentLatLng != null && goHuaweiMap != null){
            goCurrentLatLng = goHuaweiMap.getCameraPosition().target;
            goZoom = goHuaweiMap.getCameraPosition().zoom;
        }
    }

    public boolean checkPermissionLocation(){
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                return false;
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
                return false;
            }
        }else{
            if(isFirstLoad){
                //La primera vez que acepta los permisos necesita recargar el mapa
                isFirstLoad = false;
                startLocationUpdates();
                CommonUtil.saveFirstLoadMap(getContext());
            }
            return true;
        }
    }

    public void showDialogErrorConexionGps() {
        showDialogWarningOption(getResources().getString(R.string.lbl_habilitar_gps),
                getResources().getString(R.string.lbl_descripcion_gps),
                getResources().getString(R.string.lbl_aceptar),
                () -> startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION));
    }

    public boolean validLastClick(){
        // mis-clicking prevention, using threshold of 1000 ms
        if (SystemClock.elapsedRealtime() - mLastClickTime < 2000){
            return false;
        }else{
            mLastClickTime = SystemClock.elapsedRealtime();
            return true;
        }

    }
}
