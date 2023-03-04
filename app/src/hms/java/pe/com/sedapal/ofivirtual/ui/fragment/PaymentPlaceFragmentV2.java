package pe.com.sedapal.ofivirtual.ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.huawei.hms.maps.model.BitmapDescriptor;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pe.com.sedapal.ofivirtual.R;
import pe.com.sedapal.ofivirtual.data.util.LogUtil;
import pe.com.sedapal.ofivirtual.di.component.CommercialOfficeComponent;
import pe.com.sedapal.ofivirtual.model.AgencyModel;
import pe.com.sedapal.ofivirtual.model.SubsidiaryModel;
import pe.com.sedapal.ofivirtual.navigation.Navigator;
import pe.com.sedapal.ofivirtual.presenter.sedapal.ListAgencyPresenter;
import pe.com.sedapal.ofivirtual.presenter.sedapal.ListSubsidiaryPresenter;
import pe.com.sedapal.ofivirtual.presenter.view.ListAgencyView;
import pe.com.sedapal.ofivirtual.presenter.view.ListSubsidiaryView;

public class PaymentPlaceFragmentV2 extends BaseFragment implements
        ListAgencyView,
        ListSubsidiaryView,
        OnMapReadyCallback,
        HuaweiMap.OnMarkerClickListener{

    public static HuaweiApiClient goGoogleApiClient;

    private static final String TAG = PaymentPlaceFragmentV2.class.getSimpleName();
    private static final String LOCATION_KEY = "LOCATION";
    private static final int REQUEST_DISABLED = 10001;
    private static final int REQUEST_LOCATION = 10004;

    @BindView(R.id.fabMyLocation)
    FloatingActionButton fabMyLocation;

    @BindView(R.id.rlNoLocationEnabled)
    RelativeLayout rlNoLocationEnabled;

    @BindView(R.id.rlContentPendingInvoice)
    RelativeLayout rlContentPendingInvoice;

    @BindView(R.id.pbProgressBarLoadData)
    ProgressBar pbProgressBarLoadData;

    @BindView(R.id.btnReintentarGps)
    Button btnReintentarGps;

    @Inject
    ListAgencyPresenter opListAgencyPresenter;

    @Inject
    ListSubsidiaryPresenter opListSubsidiaryPresenter;

    @Inject
    public Navigator navigator;

    private HuaweiMap goGoogleMap;

    private LatLng goCurrentLatLng;
    private List<Marker> gaoAgencyMarkers;
    private boolean gbInService = false;
    private boolean gbInWait = false;
    private boolean gbIsEnableAgent = true;
    private Handler goHandler;
    private LocationCallback mLocationCallback;

    final Runnable goRunnable = new Runnable() {
        public void run() {
            LogUtil.i("onCameraMove", "init gorunnable: " + goCurrentLatLng.toString());
            initGetPointToCurrent();
        }
    };

    public static PaymentPlaceFragmentV2 newInstance() {
        Bundle args = new Bundle();
        PaymentPlaceFragmentV2 fragment = new PaymentPlaceFragmentV2();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getComponent(CommercialOfficeComponent.class).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_places, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUpView();
        if (isHuaweiServicesAvailable() && (isEnabledGPS())) setUpMaps();
        updateValuesFromBundle(savedInstanceState);
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

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            rlNoLocationEnabled.setVisibility(View.VISIBLE);
            rlContentPendingInvoice.setVisibility(View.GONE);
            return false;
        }

        hideInfoDisableGps();
        return true;
    }

    public void hideInfoDisableGps(){
        rlNoLocationEnabled.setVisibility(View.GONE);
        rlContentPendingInvoice.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_DISABLED:
                break;

            case REQUEST_LOCATION:
                reintentarGps();
                break;
        }
    }

    @OnClick(R.id.btnReintentarGps)
    public void reintentarGps(){
        if (isHuaweiServicesAvailable() && (isEnabledGPS())) {
            hideInfoDisableGps();
            setUpMaps();
        }else {
            showDialogWarningOption(getResources().getString(R.string.lbl_habilitar_gps),
                    getResources().getString(R.string.lbl_descripcion_gps),
                    getResources().getString(R.string.lbl_aceptar),
                    () -> startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION));
        }
    }

    public void setUpView() {
        this.opListAgencyPresenter.setView(this);
        this.opListSubsidiaryPresenter.setView(this);
    }

    private void setUpMaps() {
        SupportMapFragment goMapFragment = SupportMapFragment.newInstance();
        goMapFragment.getMapAsync(this);
        final FragmentTransaction fragmentTransaction = Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.frlContainer, goMapFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onMapReady(HuaweiMap poGoogleMap) {

        this.goGoogleMap = poGoogleMap;
        this.goGoogleMap.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.goGoogleMap.setMyLocationEnabled(true);
        this.goGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        //this.initGetPointToCurrent();
        this.goGoogleMap.setOnCameraIdleListener(() -> {
            LogUtil.i(TAG, "setOnCameraIdleListener - STOP");
            if (goCurrentLatLng != null) {
                if (goHandler != null) {
                    goHandler.removeCallbacks(goRunnable);
                }
                gbInWait = true;
                if (!gbInService) {
                    doGetPoints();
                }
            } else {
                gbInWait = false;
            }
        });
    }

    private void doGetPoints() {
        LogUtil.i(TAG, "doGetPoints -  gbInWait: " + gbInWait);
        if (gbInWait) {
            gbInWait = false;
            goCurrentLatLng = goGoogleMap.getCameraPosition().target;
            goHandler = new Handler();
            goHandler.postDelayed(goRunnable, 100);

            initGetPointToCurrent();
            LogUtil.i(TAG, "doGetPoints - currentlatlg: " + goCurrentLatLng.toString());
        }
    }

    private void initGetPointToCurrent() {
        LogUtil.i(TAG, "initGetPointToCurrent - init");
        if (this.goGoogleMap == null || goCurrentLatLng == null) {
            return;
        }
        gbInService = true;
        LogUtil.i(TAG, "initGetPointToCurrent - initialize");
        opListSubsidiaryPresenter.initialize(1);
    }

    private void startLocationUpdates() {
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
    public void showSucessListSubsidiary(List<SubsidiaryModel> poPlaceModel) {
        this.gbInService = false;
        this.gaoAgencyMarkers = new ArrayList<>();
        this.goGoogleMap.clear();
        if (poPlaceModel != null) {
            if (gbIsEnableAgent) {
                showPoints(poPlaceModel);
            }
        }
    }

    public void showPoints(List<SubsidiaryModel> laoUbigeoModel) {
        BitmapDescriptor loBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_sedapal);
        List<Marker> laoMarker = gaoAgencyMarkers;

        for (SubsidiaryModel loUbigeoModel : laoUbigeoModel) {
            LatLng loLatLng = loUbigeoModel.getLatLng();
            if (loLatLng != null) {
                Marker loMarker = goGoogleMap.addMarker(new MarkerOptions()
                        .position(loLatLng)
                        .icon(loBitmapDescriptor));
                loMarker.setTag(loUbigeoModel);
                laoMarker.add(loMarker);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker poMarker) {
        SubsidiaryModel loUbigeoModel = (SubsidiaryModel) poMarker.getTag();
        navigator.navigateToFindYourLocalDetail(getContext(),loUbigeoModel);
        return true;
    }

    @OnClick({R.id.fabMyLocation})
    public void onViewClicked(View view) {
        Fragment loFragment = Objects.requireNonNull(getActivity()).getSupportFragmentManager().findFragmentById(R.id.frlContainer);
        if (view.getId() == R.id.fabMyLocation) {
            if (loFragment instanceof SupportMapFragment) {
                if (isHuaweiServicesAvailable() && (isEnabledGPS())) {

                    startLocationUpdates();
                } else {
                    showDialogWarningOption(getResources().getString(R.string.lbl_habilitar_gps),
                            getResources().getString(R.string.lbl_descripcion_gps),
                            getResources().getString(R.string.lbl_aceptar),
                            () -> startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION));
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, goCurrentLatLng);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
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
        CameraUpdate loCameraUpdate = CameraUpdateFactory.newLatLngZoom(this.goCurrentLatLng, 12);
        goGoogleMap.animateCamera(loCameraUpdate);
        initGetPointToCurrent();
    }

    @Override
    public void showRetry() {
    }

    @Override
    public void hideRetry() {
    }

    @Override
    public void showError(String psMessage) {
        this.gbInService = false;
    }

    public void onBackPressed() {

    }

    @Override
    public Context context() {
        return getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.opListAgencyPresenter.resume();
        this.opListSubsidiaryPresenter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.opListAgencyPresenter.pause();
        this.opListSubsidiaryPresenter.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.opListAgencyPresenter.destroy();
        this.opListSubsidiaryPresenter.destroy();

        goGoogleApiClient = null;
    }

    /**
     * Webservices
     *
     * @param poListAgencyModel
     */

    @Override
    public void showSucessListAgency(List<AgencyModel> poListAgencyModel) {
    }

    @Override
    public void showLoadingPersonalized() {
    }

    @Override
    public void hideLoadingPersonalized() {
    }

    @Override
    public void showLoadingPersonalizedSubsidiary() {
        pbProgressBarLoadData.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingPersonalizedSubsidiary() {
        pbProgressBarLoadData.setVisibility(View.GONE);
    }
}
