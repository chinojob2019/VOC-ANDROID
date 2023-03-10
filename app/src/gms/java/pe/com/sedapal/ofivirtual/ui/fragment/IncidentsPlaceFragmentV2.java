package pe.com.sedapal.ofivirtual.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pe.com.sedapal.ofivirtual.R;
import pe.com.sedapal.ofivirtual.data.util.LogUtil;
import pe.com.sedapal.ofivirtual.di.component.CommercialOfficeComponent;
import pe.com.sedapal.ofivirtual.model.IncidentsMunicipalitiesModel;
import pe.com.sedapal.ofivirtual.model.MunicipalitiesModel;
import pe.com.sedapal.ofivirtual.navigation.Navigator;
import pe.com.sedapal.ofivirtual.presenter.sedapal.MunicipalitiesPresenter;
import pe.com.sedapal.ofivirtual.presenter.view.IncidentsMunicipalitiesView;
import pe.com.sedapal.ofivirtual.presenter.view.MunicipalitiesView;
import pe.com.sedapal.ofivirtual.ui.adapter.CustomInfoWindowMapAdapter;

public class IncidentsPlaceFragmentV2 extends BaseFragment implements
        MunicipalitiesView,
        IncidentsMunicipalitiesView,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener {

    public static GoogleApiClient goGoogleApiClient;

    private static final String TAG = IncidentsPlaceFragmentV2.class.getSimpleName();
    private static final String LOCATION_KEY = "LOCATION";
    private static final long UPDATE_INTERVAL = 20 * 1000;  /* 10 secs */
    private static final long FASTEST_INTERVAL = 2 * 1000; /* 2 sec */
    private static final int REQUEST_DISABLED = 10001;
    private static final int REQUEST_LOCATION = 10004;

    @BindView(R.id.fabMyLocation)
    FloatingActionButton fabMyLocation;

    //@BindView(R.id.fabRefresh)
    //FloatingActionButton fabRefresh;

    @BindView(R.id.rlNoLocationEnabled)
    RelativeLayout rlNoLocationEnabled;

    @BindView(R.id.rlContentPendingInvoice)
    RelativeLayout rlContentPendingInvoice;

    @BindView(R.id.pbProgressBarLoadData)
    ProgressBar pbProgressBarLoadData;

    @BindView(R.id.btnReintentarGps)
    Button btnReintentarGps;

    @Inject
    MunicipalitiesPresenter opMunicipalitiesPresenter;

    //@Inject
    //IncidentsMunicipalitiesPresenter opIncidentsMunicipalitiesPresenter;

    @Inject
    public Navigator navigator;

    private GoogleMap goGoogleMap;
    private LatLng goCurrentLatLng;
    private List<Marker> gaoIncidentsMarkers;
    private boolean gbInService = false;
    private boolean gbInWait = false;
    private boolean gbIsEnableAgent = true;
    private Handler goHandler;

    final Runnable goRunnable = new Runnable() {
        public void run() {
            LogUtil.i("onCameraMove", "init gorunnable: " + goCurrentLatLng.toString());
            initGetPointToCurrent();
        }
    };

    public static IncidentsPlaceFragmentV2 newInstance() {
        Bundle args = new Bundle();
        IncidentsPlaceFragmentV2 fragment = new IncidentsPlaceFragmentV2();
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
        View view = inflater.inflate(R.layout.fragment_incidents_places, container, false);
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
        if (isGooglePlayServicesAvailable() && (isEnabledGPS())) setUpMaps();
        updateValuesFromBundle(savedInstanceState);
    }

    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability loGoogleApiAvailability = GoogleApiAvailability.getInstance();
        int liStatus = loGoogleApiAvailability.isGooglePlayServicesAvailable(context());
        if (liStatus == ConnectionResult.SUCCESS) {
            return true;
        } else {
            loGoogleApiAvailability.getErrorDialog(getActivity(), liStatus, REQUEST_DISABLED, dialog -> {
                //finish();
            }).show();
            return false;
        }
    }

    private boolean isEnabledGPS() {
        final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Call your Alert message
            //showDialogErrorOption(getResources().getString(R.string.lbl_habilitar_gps),
            //        getResources().getString(R.string.lbl_descripcion_gps),
            //        getResources().getString(R.string.lbl_aceptar),
            //        new CallbackDialogOption() {
            //            @Override
            //            public void onClickBtnOption() {
            //                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
            //            }
            //        });

            rlNoLocationEnabled.setVisibility(View.VISIBLE);
            rlContentPendingInvoice.setVisibility(View.GONE);

            return false;
        }

        hideInfoDisableGps();

        return true;
    }

    public void hideInfoDisableGps() {
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
                //if (isGooglePlayServicesAvailable() && (isEnabledGPS())) {
                //    setUpMaps();
                //    hideInfoDisableGps();
                //}
                reintentarGps();
                break;
        }
    }

    @OnClick(R.id.btnReintentarGps)
    public void reintentarGps() {
        setUpMaps();
        if (isGooglePlayServicesAvailable() && (isEnabledGPS())) {

            hideInfoDisableGps();
        } else {
            showDialogWarningOption(getResources().getString(R.string.lbl_habilitar_gps),
                    getResources().getString(R.string.lbl_descripcion_gps),
                    getResources().getString(R.string.lbl_aceptar),
                    new CallbackDialogOption() {
                        @Override
                        public void onClickBtnOption() {
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
                        }
                    });
        }
    }

    public void setUpView() {
        //this.opIncidentsMunicipalitiesPresenter.setView(this);
        this.opMunicipalitiesPresenter.setView(this);
    }


    private void setUpMaps() {
        SupportMapFragment goMapFragment = SupportMapFragment.newInstance();
        goMapFragment.getMapAsync(this);
        final FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.frlContainer, goMapFragment);
        fragmentTransaction.commit();
        //addFragment(R.id.frlContainer, goMapFragment);

        if (goGoogleApiClient == null) {
            goGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .build();
        }
    }

    @Override
    public void onMapReady(GoogleMap poGoogleMap) {

        this.goGoogleMap = poGoogleMap;
        this.goGoogleMap.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission((Activity) getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((Activity) getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                //if (goHandler != null) {
                //    goHandler.removeCallbacks(goRunnable);
                //}
                //gbInWait = true;
                //if (!gbInService) {
                //    doGetPoints();
                //}
            }
        });
    }

    public boolean isFirstLoad = true;

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

    //@OnClick(R.id.fabRefresh)
    public void initGetPointToCurrent() {
        LogUtil.i(TAG, "initGetPointToCurrent - init");
        if (this.goGoogleMap == null || goCurrentLatLng == null) {
            return;
        }
        gbInService = true;
        LogUtil.i(TAG, "initGetPointToCurrent - initialize");

        /**
         * Obteniendo el listado de distritos
         */
        opMunicipalitiesPresenter.initialize();
    }

    protected void startLocationUpdates() {
        LogUtil.i(TAG, "startLocationUpdates - INIT");
        LocationServices.FusedLocationApi.removeLocationUpdates(goGoogleApiClient, this);
        LocationRequest goLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission((Activity) getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((Activity) getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                goGoogleApiClient, goLocationRequest, this);
        LogUtil.i(TAG, "startLocationUpdates - END");
    }

    public void showPoints(List<IncidentsMunicipalitiesModel> laoUbigeoModel) {
        BitmapDescriptor loBitmapDescriptor = null;
        List<Marker> laoMarker = null;
        loBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_incident);
        laoMarker = gaoIncidentsMarkers;

        for (IncidentsMunicipalitiesModel loUbigeoModel : laoUbigeoModel) {
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

    public void showOnlyPoint(IncidentsMunicipalitiesModel laoIncidentsMunicipalitiesModel) {
        BitmapDescriptor loBitmapDescriptor = null;
        List<Marker> laoMarker = null;
        loBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_incident);
        laoMarker = gaoIncidentsMarkers;

        LatLng loLatLng = laoIncidentsMunicipalitiesModel.getLatLng();
        if (loLatLng != null) {
            Marker loMarker = goGoogleMap.addMarker(new MarkerOptions()
                    .position(loLatLng)
                    .icon(loBitmapDescriptor));
            loMarker.setTag(laoIncidentsMunicipalitiesModel);
            laoMarker.add(loMarker);
        }
    }

    @Override
    public boolean onMarkerClick(Marker poMarker) {
        IncidentsMunicipalitiesModel loUbigeoModel = (IncidentsMunicipalitiesModel) poMarker.getTag();
        goGoogleMap.setInfoWindowAdapter(new CustomInfoWindowMapAdapter(LayoutInflater.from(getActivity())));
        goGoogleMap.setOnInfoWindowClickListener(null);
        poMarker.showInfoWindow();
        CameraUpdate loCameraUpdate = CameraUpdateFactory.newLatLngZoom(loUbigeoModel.getLatLng(), 12);
        goGoogleMap.animateCamera(loCameraUpdate);

        return true;
    }

    public void removeMarkers(List<Marker> paoMarkers) {
        for (Marker loMarker : paoMarkers) {
            loMarker.remove();
        }
    }

    @OnClick({R.id.fabMyLocation})
    public void onViewClicked(View view) {
        int liHeigth = 0;
        Fragment loFragment = getChildFragmentManager().findFragmentById(R.id.frlContainer);
        switch (view.getId()) {
            case R.id.fabMyLocation:
                if (loFragment instanceof SupportMapFragment) {
                    //startLocationUpdates();
                    if (isGooglePlayServicesAvailable() && (isEnabledGPS())) {

                        startLocationUpdates();
                    } else {
                        showDialogWarningOption(getResources().getString(R.string.lbl_habilitar_gps),
                                getResources().getString(R.string.lbl_descripcion_gps),
                                getResources().getString(R.string.lbl_aceptar),
                                new CallbackDialogOption() {
                                    @Override
                                    public void onClickBtnOption() {
                                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
                                    }
                                });
                    }
                }
                break;
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (goCurrentLatLng == null) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        LogUtil.i(TAG, "onConnectionSuspended - INIT");
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            LogUtil.i(TAG, "onConnectionSuspended - CAUSE_SERVICE_DISCONNECTED");
        } else if (i == CAUSE_NETWORK_LOST) {
            LogUtil.i(TAG, "onConnectionSuspended - CAUSE_NETWORK_LOST");
        } else {
            LogUtil.i(TAG, "onConnectionSuspended - ELSE");
        }
    }

    @Override
    public void onLocationChanged(Location poLocation) {
        LogUtil.i(TAG, "onLocationChanged - INIT");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                goGoogleApiClient, this);
        onLocationChangedCamera(new LatLng(poLocation.getLatitude(), poLocation.getLongitude()));
    }

    private void onLocationChangedCamera(LatLng poLatLng) {
        this.goCurrentLatLng = poLatLng;
        CameraUpdate loCameraUpdate = CameraUpdateFactory.newLatLngZoom(this.goCurrentLatLng, 12);
        goGoogleMap.animateCamera(loCameraUpdate);
        //initGetPointToCurrent();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        LogUtil.i(TAG, "onConnectionFailed - INIT");
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
        //DialogView.create(context()).showBasicDialog("", "");
        showDialogError(psMessage);
    }


    /**
     * Webservices
     */

    //Lista con los elementos que contiene las coordenadas de las incidencias
    //List<IncidentsMunicipalitiesModel> poListIncidentsMunicipalitiesModel;
    @Override
    public void showSucessListMunicipalities(List<MunicipalitiesModel> poListMunicipalities) {
        /**
         * Una vez obtenida la lista de elementos se enviara a ser mostrado
         */
        this.gbInService = false;
        this.gaoIncidentsMarkers = new ArrayList<>();
        this.goGoogleMap.clear();

        if (poListMunicipalities != null) {
            if (!poListMunicipalities.isEmpty()) {
                //List<IncidentsMunicipalitiesModel> tempListIncidents= new ArrayList<>();
                for (MunicipalitiesModel poIncidet : poListMunicipalities) {
                    if(poIncidet.isMapa()) {
                        showPoints(poIncidet.getIncidencias());
                    }
                }
            }

        }
    }

    @Override
    public void showSucessListIncidentsMunicipalities
            (List<IncidentsMunicipalitiesModel> poIncidentsMunicipalitiesModel) {
        //if (poIncidentsMunicipalitiesModel != null)
        //    if (!poIncidentsMunicipalitiesModel.isEmpty()) {
        //        //List<IncidentsMunicipalitiesModel> tempListIncidents= new ArrayList<>();
        //        for (IncidentsMunicipalitiesModel poIncidet : poIncidentsMunicipalitiesModel) {
        //            if (poIncidet.getLatitud() != 0 && poIncidet.getLongitud() != 0) {
        //                //tempListIncidents.add(poIncidet);
        //                showOnlyPoint(poIncidet);
        //            }
        //        }
//
        //        //poListIncidentsMunicipalitiesModel.addAll(tempListIncidents);
        //    }

    }

    @Override
    public void showLoadingPersonalized() {
        pbProgressBarLoadData.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingPersonalized() {
        pbProgressBarLoadData.setVisibility(View.GONE);
    }


    public void onBackPressed() {

    }

    @Override
    public Context context() {
        return getActivity();
    }

    @Override
    public void onStart() {
        if (goGoogleApiClient != null) {
            goGoogleApiClient.connect();
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        if (goGoogleApiClient != null) {
            if (goGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(
                        goGoogleApiClient, this);
                goGoogleApiClient.disconnect();
            }
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.opMunicipalitiesPresenter.resume();
        //this.opIncidentsMunicipalitiesPresenter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.opMunicipalitiesPresenter.pause();
        //this.opIncidentsMunicipalitiesPresenter.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.opMunicipalitiesPresenter.destroy();
        //this.opIncidentsMunicipalitiesPresenter.destroy();
        goGoogleApiClient = null;
    }
}
