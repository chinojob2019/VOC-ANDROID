package pe.com.sedapal.ofivirtual.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import pe.com.sedapal.ofivirtual.model.AgencyModel;
import pe.com.sedapal.ofivirtual.model.SubsidiaryModel;
import pe.com.sedapal.ofivirtual.navigation.Navigator;
import pe.com.sedapal.ofivirtual.presenter.sedapal.ListAgencyPresenter;
import pe.com.sedapal.ofivirtual.presenter.sedapal.ListSubsidiaryPresenter;
import pe.com.sedapal.ofivirtual.presenter.view.ListAgencyView;
import pe.com.sedapal.ofivirtual.presenter.view.ListSubsidiaryView;

public class PaymentPlaceFragment extends BaseFragmentPlaces implements
        HuaweiMap.OnMarkerClickListener,
        ListAgencyView,
        ListSubsidiaryView {

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

    private List<SubsidiaryModel> listPlaceModel = new ArrayList<>();
    private List<Marker> gaoAgencyMarkers;

    public static PaymentPlaceFragment newInstance() {
        Bundle args = new Bundle();
        PaymentPlaceFragment fragment = new PaymentPlaceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PaymentPlaceFragment.this.getComponent(CommercialOfficeComponent.class).inject(PaymentPlaceFragment.this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_example_payment_place, container, false);
        ButterKnife.bind(PaymentPlaceFragment.this, view);
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
            startLocationUpdates();
        } else {
            showDialogErrorConexionGps();
        }
    }

    public void setUpView() {
        this.opListAgencyPresenter.setView(PaymentPlaceFragment.this);
        this.opListSubsidiaryPresenter.setView(PaymentPlaceFragment.this);
    }

    @Override
    public void customOnMapReady(HuaweiMap poGoogleMap) {
        Log.i(TAG, "customOnMapReady");
        goHuaweiMap = poGoogleMap;
        goHuaweiMap.setOnMarkerClickListener(PaymentPlaceFragment.this);
        if (!checkPermissionLocation()) {
            return;
        }
        goHuaweiMap.setMyLocationEnabled(true);
        goHuaweiMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (goCurrentLatLng != null) {
            goHuaweiMap.setBuildingsEnabled(true);
            this.gaoAgencyMarkers = new ArrayList<>();
            showPoints(listPlaceModel);
            CameraUpdate loCameraUpdate = CameraUpdateFactory.newLatLngZoom(this.goCurrentLatLng, goZoom);
            goHuaweiMap.animateCamera(loCameraUpdate);

        }
    }

    private void initGetPointToCurrent() {
        LogUtil.i(TAG, "initGetPointToCurrent - init");
        opListSubsidiaryPresenter.initialize(1);
    }

    @Override
    public void showSucessListSubsidiary(List<SubsidiaryModel> poPlaceModel) {
        this.gaoAgencyMarkers = new ArrayList<>();
        this.listPlaceModel = poPlaceModel;
        if (isHuaweiServicesAvailable() && (isEnabledGPS())) {
            startLocationUpdates();
        }
    }

    public void showPoints(List<SubsidiaryModel> laoUbigeoModel) {
        try {
            BitmapDescriptor loBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_sedapal);
            List<Marker> laoMarker = gaoAgencyMarkers;

            for (SubsidiaryModel loUbigeoModel : laoUbigeoModel) {
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
    public boolean onMarkerClick(Marker marker) {
        if (!validLastClick()) {
            return false;
        }
        SubsidiaryModel loUbigeoModel = (SubsidiaryModel) marker.getTag();
        navigator.navigateToFindYourLocalDetail((Activity) getContext(), loUbigeoModel);
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

