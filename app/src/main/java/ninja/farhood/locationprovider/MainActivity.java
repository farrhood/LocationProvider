package ninja.farhood.locationprovider;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 777;
    TextView tvCapturedGeolocation;
    private GoogleApiClient mGoogleApiClient;
    ImageView ivRadar;
    private double longitude;
    private double latitude;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivRadar = (ImageView) findViewById(R.id.ivRadar);
        tvCapturedGeolocation = (TextView) findViewById(R.id.tvCapturedGeolocation);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
    }

    public void btnCaptureLocationOnClick(View view) {
        if (checkPermission()) {

        } else {
            requestPermission();
        }

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(tvCapturedGeolocation, "Permission Granted, Now you can access location data.", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(tvCapturedGeolocation, "Permission Denied, You cannot access location data.", Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "GPS permission needed in order to capture your location", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION);
        }
    }

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            String msg = "Location captured successfully: longitude:%slatitude:%s";
            msg = String.format(msg, location.getLongitude(), location.getLatitude());

            longitude = location.getLongitude();
            latitude = location.getLatitude();
            mapFragment.getMapAsync(MainActivity.this);

            try {
                msg += "\nStreet: " + getAddressFromLocation(location.getLatitude(), location.getLongitude());
            } catch (IOException e) {
                e.printStackTrace();
            }
            tvCapturedGeolocation.setText(msg);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this, "Provider Enabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, "Provider Disabled", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {

        CameraUpdate center =

                CameraUpdateFactory.newLatLng(new LatLng(longitude, latitude));

        CameraUpdate zoom = CameraUpdateFactory.zoomTo(10);

        googleMap.moveCamera(center);
        googleMap.animateCamera(zoom);

        getNearByRestaurant(longitude, latitude, googleMap);

    }

    private void getNearByRestaurant(double lat, double longu, GoogleMap googleMap) {
        googleMap.clear();
        String url = getUrl(lat, longu);
        Object[] DataTransfer = new Object[2];
        DataTransfer[0] = googleMap;
        DataTransfer[1] = url;
        NearByPlacesProvider nearByPlacesProvider = new NearByPlacesProvider();
        nearByPlacesProvider.execute(DataTransfer);
        Toast.makeText(this, "Nearby Restaurants", Toast.LENGTH_LONG).show();
    }

    private String getUrl(double latitude, double longitude) {
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + 15000);
        googlePlacesUrl.append("&type=restaurant");
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + "AIzaSyB58-446-3ZOWDv70wlHghQuuoudOqJvo8");
        Log.d("URL", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }

    private String getAddressFromLocation(double latitude, double longitude) throws IOException {
        Geocoder geocoder = new Geocoder(this);
        List<Address> matches = geocoder.getFromLocation(latitude, longitude, 1);
        if (matches.size() < 1) return "";
        return (matches.isEmpty() ? null : matches.get(0)).getAddressLine(0).toString();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to find nearby restaurants, check your internet connection", Toast.LENGTH_LONG).show();
    }
}
