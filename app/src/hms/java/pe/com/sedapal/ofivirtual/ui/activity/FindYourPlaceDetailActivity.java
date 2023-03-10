package pe.com.sedapal.ofivirtual.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.model.BitmapDescriptor;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.CameraPosition;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.MarkerOptions;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pe.com.sedapal.ofivirtual.R;
import pe.com.sedapal.ofivirtual.model.SubsidiaryModel;

public class FindYourPlaceDetailActivity extends BaseActivity implements
        OnMapReadyCallback {

    private static final String BUNDLE_UBIGEO_MODEL = "UBIGEO";

    public static Intent getCallingIntent(Context poContext, SubsidiaryModel poUbigeoModel) {
        Bundle loBundle = new Bundle();
        loBundle.putSerializable(BUNDLE_UBIGEO_MODEL, poUbigeoModel);
        Intent loIntent = new Intent(poContext, FindYourPlaceDetailActivity.class);
        loIntent.putExtras(loBundle);
        return loIntent;
    }

    @BindView(R.id.toolbar_back)
    Toolbar toolbar_back;
    @BindView(R.id.toolbar_title)
    TextView toolbar_title;

    @BindView(R.id.txtNomRecaudador)
    TextView txtNomRecaudador;
    @BindView(R.id.txtDirRecaudador)
    TextView txtDirRecaudador;
    @BindView(R.id.txtDistritoRecaudador)
    TextView txtDistritoRecaudador;

    private SubsidiaryModel goUbigeoModel;
    private HuaweiMap goGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_your_place_detail);
        ButterKnife.bind(this);
        setUpView();
    }

    @OnClick(R.id.btnComoLlegar)
    public void onClick(View poView) {
        if (poView.getId() == R.id.btnComoLlegar) {
            final String loUri = "geo:0,0?q=" +
                    Uri.encode(String.format("%s,%s", goUbigeoModel.getLatitud(), goUbigeoModel.getLongitud()), "UTF-8");
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(loUri));
            startActivity(intent);
        }
    }

    private void setUpView() {
        Bundle loBundle = getIntent().getExtras();
        goUbigeoModel = (SubsidiaryModel) loBundle.getSerializable(BUNDLE_UBIGEO_MODEL);

        configToolbar();

        SupportMapFragment goMapFragment = SupportMapFragment.newInstance();
        goMapFragment.getMapAsync(this);
        addFragment(R.id.frlContainer, goMapFragment);

        setLabels();
    }

    private void configToolbar() {
        setSupportActionBar(toolbar_back);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar_back.setNavigationIcon(R.drawable.ic_back_toolbar);
        toolbar_title.setText(goUbigeoModel.getTipRecaudador());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(HuaweiMap poGoogleMap) {
        this.goGoogleMap = poGoogleMap;
        this.goGoogleMap.getUiSettings().setScrollGesturesEnabled(false);
        this.goGoogleMap.setMyLocationEnabled(true);
        this.goGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.goGoogleMap.getUiSettings().setAllGesturesEnabled(false);
        this.goGoogleMap.setOnMarkerClickListener(marker -> true);
        setPlaceLocation();
    }

    private void setLabels() {
        txtDirRecaudador.setText(goUbigeoModel.getDirRecaudador());
        txtDistritoRecaudador.setText(goUbigeoModel.getDistrito());
        txtNomRecaudador.setText(goUbigeoModel.getNomRecaudador());
    }

    private void setPlaceLocation() {
        if (this.goGoogleMap == null && this.goUbigeoModel == null) {
            return;
        }
        BitmapDescriptor loBitmapDescriptor;
        loBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_sedapal);
        LatLng loLatLng = goUbigeoModel.getLatLng();
        if (loLatLng != null) {
            goGoogleMap.addMarker(new MarkerOptions().position(loLatLng).icon(loBitmapDescriptor));
        }
        CameraPosition cameraPosition = new CameraPosition.Builder().target(
                new LatLng(
                        goUbigeoModel.getLatitud(),
                        goUbigeoModel.getLongitud())).zoom(16).build();
        goGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}
