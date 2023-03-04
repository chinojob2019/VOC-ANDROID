package pe.com.sedapal.ofivirtual.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
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
import pe.com.sedapal.ofivirtual.model.IncidentsMunicipalitiesModel;
import pe.com.sedapal.ofivirtual.model.MunicipalitiesModel;
import pe.com.sedapal.ofivirtual.navigation.Navigator;
import pe.com.sedapal.ofivirtual.presenter.sedapal.MunicipalitiesPresenter;
import pe.com.sedapal.ofivirtual.presenter.view.MunicipalitiesView;
import pe.com.sedapal.ofivirtual.ui.adapter.CustomInfoWindowMapAdapter;

public class IncidentPlaceFragment extends BaseFragmentPlaces implements
        MunicipalitiesView,
        HuaweiMap.OnMarkerClickListener {

    @BindView(R.id.fabMyLocation)
    FloatingActionButton fabMyLocation;

    @BindView(R.id.rlNoLocationEnabled)
    RelativeLayout rlNoLocationEnabled;

    @BindView(R.id.rlContentPendingInvoice)
    RelativeLayout rlContentPendingInvoice;

    @BindView(R.id.btnReintentarGps)
    Button btnReintentarGps;

    @BindView(R.id.crdInfoUpdateDataMap)
    CardView crdInfoUpdateDataMap;

    @Inject
    MunicipalitiesPresenter opMunicipalitiesPresenter;

    @Inject
    public Navigator navigator;

    private List<Marker> gaoIncidentsMarkers;
    private List<IncidentsMunicipalitiesModel> listPlaceModel = new ArrayList<>();

    public static IncidentPlaceFragment newInstance() {
        Bundle args = new Bundle();
        IncidentPlaceFragment fragment = new IncidentPlaceFragment();
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
        View view = inflater.inflate(R.layout.fragment_example_incident_place, container, false);
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
        updateValuesFromBundle(savedInstanceState);
        initGetPointToCurrent();
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
                reintentarConexionGps();
                break;
        }
    }

    @OnClick(R.id.btnReintentarGps)
    public void reintentarConexionGps() {
        if (isHuaweiServicesAvailable() && (isEnabledGPS())) {
            hideInfoDisableGps();
            setUpMaps();
        } else {
            showDialogErrorConexionGps();
        }
    }

    public void setUpView() {
        this.opMunicipalitiesPresenter.setView(this);
    }

    public void customOnMapReady(HuaweiMap poGoogleMap) {
        this.goHuaweiMap = poGoogleMap;
        this.goHuaweiMap.setOnMarkerClickListener(this);
        if (!checkPermissionLocation()) {
            return;
        }
        this.goHuaweiMap.setMyLocationEnabled(true);
        this.goHuaweiMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (goCurrentLatLng != null) {
            goHuaweiMap.setBuildingsEnabled(true);
            this.gaoIncidentsMarkers = new ArrayList<>();
            showPoints(listPlaceModel);
            CameraUpdate loCameraUpdate = CameraUpdateFactory.newLatLngZoom(this.goCurrentLatLng, goZoom);
            goHuaweiMap.animateCamera(loCameraUpdate);
        }
    }

    private void initGetPointToCurrent() {
        LogUtil.i(TAG, "initGetPointToCurrent - init");
        /*
          Obteniendo el listado de distritos
         */
        opMunicipalitiesPresenter.initialize();
    }

    public void showPoints(List<IncidentsMunicipalitiesModel> laoUbigeoModel) {
        try {
            BitmapDescriptor loBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_incident);
            List<Marker> laoMarker = gaoIncidentsMarkers;

            for (IncidentsMunicipalitiesModel loUbigeoModel : laoUbigeoModel) {
                LatLng loLatLng = loUbigeoModel.getLatLng();
                if (loLatLng != null) {
                    Marker loMarker = goHuaweiMap.addMarker(new MarkerOptions()
                            .position(loLatLng)
                            .icon(loBitmapDescriptor));
                    loMarker.setTag(loUbigeoModel);
                    laoMarker.add(loMarker);
                }
            }
        } catch (Exception e) {
            LogUtil.i(TAG, "Exception -  showPoints: " + e.getMessage());
        }
    }

    @Override
    public boolean onMarkerClick(Marker poMarker) {
        if (!validLastClick()) {
            return false;
        }
        IncidentsMunicipalitiesModel loUbigeoModel = (IncidentsMunicipalitiesModel) poMarker.getTag();
        goHuaweiMap.setInfoWindowAdapter(new CustomInfoWindowMapAdapter(LayoutInflater.from(getActivity())));
        goHuaweiMap.setOnInfoWindowClickListener(null);
        poMarker.showInfoWindow();
        goZoom = 12f;
        CameraUpdate loCameraUpdate = CameraUpdateFactory.newLatLngZoom(loUbigeoModel.getLatLng(), goZoom);
        goHuaweiMap.animateCamera(loCameraUpdate);
        return true;
    }

    @OnClick({R.id.fabMyLocation})
    public void onViewClicked(View view) {
        if (isHuaweiServicesAvailable() && (isEnabledGPS())) {
            goZoom = 12f;
            CameraUpdate loCameraUpdate = CameraUpdateFactory.newLatLngZoom(this.goCurrentLatLng, goZoom);
            goHuaweiMap.animateCamera(loCameraUpdate);
        } else {
            showDialogErrorConexionGps();
        }
    }

    @Override
    public void showRetry() {
    }

    @Override
    public void hideRetry() {
    }

    @Override
    public void showError(String psMessage) {
        showDialogError(psMessage);
    }

    /**
     * Webservices
     */

    @Override
    public void showSucessListMunicipalities(List<MunicipalitiesModel> poListMunicipalities) {
        /*
          Una vez obtenida la lista de elementos se enviara a ser mostrado
         */
        this.gaoIncidentsMarkers = new ArrayList<>();
        if (poListMunicipalities != null) {
            if (!poListMunicipalities.isEmpty()) {
                for (MunicipalitiesModel poIncidet : poListMunicipalities) {
                    if (poIncidet.isMapa()) {
                        listPlaceModel.addAll(poIncidet.getIncidencias());
                    }
                }
            }
        }
        if (isHuaweiServicesAvailable() && (isEnabledGPS())) {
            startLocationUpdates();
        }
    }

    @Override
    public void showLoadingPersonalized() {
        Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left);
        crdInfoUpdateDataMap.startAnimation(animation);
        crdInfoUpdateDataMap.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadingPersonalized() {
        Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right);
        crdInfoUpdateDataMap.startAnimation(animation);
        crdInfoUpdateDataMap.setVisibility(View.GONE);
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
        this.opMunicipalitiesPresenter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.opMunicipalitiesPresenter.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.opMunicipalitiesPresenter.destroy();
    }
}
