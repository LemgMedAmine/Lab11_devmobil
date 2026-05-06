package com.example.googlemapsactivity;

/*
 * TP Google Maps Activity
 * Realise par : Lemghili Mohammed Amine
 *
 * Cette activite affiche une carte Google Maps, demande la permission de
 * localisation au runtime, puis centre la camera sur la position actuelle.
 */

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;
    private static final long LOCATION_MIN_TIME_MS = 1000;
    private static final float LOCATION_MIN_DISTANCE_M = 50f;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Marker currentMarker;
    private boolean locationUpdatesStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Lemghili Mohammed Amine : recuperation du fragment Google Maps depuis le layout.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Lemghili Mohammed Amine : marker initial pour verifier que la carte est bien chargee.
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10f));
        Toast.makeText(getApplicationContext(), "Map Ready", Toast.LENGTH_SHORT).show();

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        // Lemghili Mohammed Amine : eviter de lancer plusieurs ecoutes de localisation en meme temps.
        if (mMap == null || locationManager == null || locationUpdatesStarted) {
            return;
        }

        // Lemghili Mohammed Amine : Android 6+ impose la demande de permission pendant l'execution.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        // Lemghili Mohammed Amine : proposer les parametres GPS si aucun provider n'est actif.
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return;
        }

        try {
            mMap.setMyLocationEnabled(true);

            String provider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    ? LocationManager.NETWORK_PROVIDER
                    : LocationManager.GPS_PROVIDER;

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    // Lemghili Mohammed Amine : mise a jour du marker et centrage sur la position actuelle.
                    LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

                    Toast.makeText(getApplicationContext(),
                            location.getLatitude() + " " + location.getLongitude(),
                            Toast.LENGTH_SHORT).show();

                    if (currentMarker == null) {
                        currentMarker = mMap.addMarker(
                                new MarkerOptions().position(position).title("Position actuelle")
                        );
                    } else {
                        currentMarker.setPosition(position);
                    }

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // Optional on old Android versions.
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Toast.makeText(getApplicationContext(),
                            provider + " enabled",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProviderDisabled(String provider) {
                    buildAlertMessageNoGps();
                }
            };

            // Lemghili Mohammed Amine : NETWORK_PROVIDER est choisi en priorite pour mieux marcher en interieur.
            locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_MIN_TIME_MS,
                    LOCATION_MIN_DISTANCE_M,
                    locationListener
            );
            locationUpdatesStarted = true;
        } catch (SecurityException e) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accordee", Toast.LENGTH_SHORT).show();
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permission refusee", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (locationManager != null
                && locationListener != null
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
            locationUpdatesStarted = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    private void buildAlertMessageNoGps() {
        // Lemghili Mohammed Amine : afficher une alerte si la localisation est desactivee.
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
