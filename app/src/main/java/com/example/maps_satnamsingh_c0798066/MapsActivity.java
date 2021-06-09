package com.example.maps_satnamsingh_c0798066;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng homeLocation;
    private Marker destMarker;
    private Polyline line;
    private int REQUEST_CODE = 20;
    // location with location manager and listener
    LocationManager locationManager;
    LocationListener locationListener;
    private boolean isFirstTime = true;
    private int markerCount = 0;
    Polygon shape;
    private boolean isRemovalLocation = false;
    private int removalPosition = -1;
    private ArrayList<Polyline> lines = new ArrayList<>();
    private PolygonMarker polygonMarkers[] = {new PolygonMarker("A"),
            new PolygonMarker("B"),
            new PolygonMarker("C"),
            new PolygonMarker("D")};

    private class PolygonMarker implements Comparable<PolygonMarker> {
        Marker marker;
        String name;

        PolygonMarker(String name) {
            this.name = name;
        }

        @Override
        public int compareTo(PolygonMarker o) {
            LatLng origin = new LatLng(0, 0);
            if (getDistance(origin, this.marker.getPosition()) < getDistance(origin, o.marker.getPosition())) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                setHomeMarker(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        }
        mMap = googleMap;
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                marker.setSnippet("Distance: " + getDistance(homeLocation, marker.getPosition()) + " Km");
                marker.setTag(getLocationInfo(marker.getPosition()));
            }
        });
        mMap.setOnMapLongClickListener(latLng -> {
            if (isRemovalLocation) {
                isRemovalLocation = false;
                polygonMarkers[removalPosition].marker = mMap.addMarker(new MarkerOptions().position(latLng).title(polygonMarkers[removalPosition].name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
                polygonMarkers[removalPosition].marker.setSnippet("Distance: " + getDistance(homeLocation, latLng) + " Km");
                polygonMarkers[removalPosition].marker.setTag(getLocationInfo(latLng));
                polygonMarkers[removalPosition].marker.setDraggable(true);
                if (polygonMarkers.length == markerCount) {
                    drawShape();
                }
                removalPosition = -1;
                return;
            }
            if (markerCount > 0) {
                for (int i = 0; i < markerCount; i++) {
                    Marker temp = polygonMarkers[i].marker;
                    if (getDistance(temp.getPosition(), latLng) < 0.05) {
                        temp.remove();
                        if (shape != null) {
                            shape.remove();
                            shape = null;
                        }
                        for (Polyline line : lines)
                            line.remove();
                        lines.clear();
                        isRemovalLocation = true;
                        removalPosition = i;
                        return;
                    }
                }
            }
            this.setPolygonMarker(latLng);
        });
        mMap.setOnPolylineClickListener(polyline -> {
            Toast.makeText(this, "Distance is :  " + polyline.getTag().toString() + " Km", Toast.LENGTH_SHORT).show();

        });
        mMap.setOnMarkerClickListener(marker -> {
            Toast.makeText(this, marker.getTag().toString(), Toast.LENGTH_LONG).show();
            return false;
        });
        mMap.setOnPolygonClickListener(polygon -> {
            float distance = 0;
            for (Polyline line : lines)
                distance += Float.parseFloat(line.getTag().toString());
            new AlertDialog.Builder(this)
                    .setTitle("Total Distance")
                    .setMessage("Distance from A-B-C-D is: " + distance + " Km")
                    .setPositiveButton("Ok", (dialog, which) -> {
                    })
                    .show();
        });
    }

    private void setPolygonMarker(LatLng latLng) {
        if (polygonMarkers.length == markerCount) {
            for (PolygonMarker marker : polygonMarkers)
                marker.marker.remove();
            shape.remove();
            shape = null;
            markerCount = 0;
            for (Polyline line : lines)
                line.remove();
            lines.clear();
        }
        polygonMarkers[markerCount].marker = mMap.addMarker(new MarkerOptions().position(latLng).title(polygonMarkers[markerCount].name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
        polygonMarkers[markerCount].marker.setSnippet("Distance: " + getDistance(homeLocation, latLng) + " Km");
        polygonMarkers[markerCount].marker.setTag(getLocationInfo(latLng));
        polygonMarkers[markerCount].marker.setDraggable(true);
        markerCount += 1;
        if (polygonMarkers.length == markerCount)
            drawShape();
    }

    private void drawShape() {
        PolygonOptions options = new PolygonOptions()
                .fillColor(0x5903fc73)
                .strokeColor(Color.RED)
                .strokeWidth(5).geodesic(true);
        for (int i = 0; i < markerCount; i++) {
            polygonMarkers[i].marker.setDraggable(false);
            options.add(polygonMarkers[i].marker.getPosition());
            if (i < markerCount - 1) {
                drawLine(polygonMarkers[i].marker, polygonMarkers[i + 1].marker);
            } else {
                drawLine(polygonMarkers[i].marker, polygonMarkers[0].marker);
            }
        }
        shape = mMap.addPolygon(options);
        shape.setClickable(true);
    }

    private String getLocationInfo(LatLng latLng) {
        Location location = new Location("");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);

        String outPutString = "";
        //following is to get location using geocoder
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addressList != null && addressList.size() > 0) {
                // following to add addresss
                outPutString += "Address below: \n";
                outPutString += addressList.get(0).getThoroughfare() != null ? "Street:- " + addressList.get(0).getThoroughfare() + "\n" : "?\n";
                outPutString += addressList.get(0).getLocality() != null ? "City:- " + addressList.get(0).getLocality() + "\n" : "?\n";
                outPutString += addressList.get(0).getPostalCode() != null ? "Postal Code:- " + addressList.get(0).getPostalCode() + "\n" : "?\n";
                outPutString += addressList.get(0).getAdminArea() != null ? "Province:- " + addressList.get(0).getAdminArea() + "\n" : "?\n";
            } else {
                outPutString += "\nAddress not found";
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return outPutString;
    }

    private void drawLine(Marker start, Marker end) {

        PolylineOptions options = new PolylineOptions()
                .color(Color.RED)
                .width(10)
                .add(start.getPosition(), end.getPosition());
        line = mMap.addPolyline(options);
        line.setTag(getDistance(start.getPosition(), end.getPosition()));
        line.setClickable(true);
        lines.add(line);
    }

    private float getDistance(LatLng start, LatLng end) {
        float[] distance = new float[1];
        Location.distanceBetween(start.latitude, start.longitude,
                end.latitude, end.longitude, distance);
        return distance[0] / 1000;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("Location:-", "inside onrequestpermission");
        if (requestCode == REQUEST_CODE) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Location is needed")
                        .setMessage("Are you want to close this application?")
                        .setPositiveButton("No",(dialog, which) -> {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                        })
                        .setNegativeButton("Yes",(dialog, which) -> {this.finish();})
                        .show();
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        }
    }

    private void setHomeMarker(Location location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        Marker temp = mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location")
                .icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker())));
        temp.setTag("Current Location");
        homeLocation = new LatLng(location.getLatitude(),location.getLongitude());
        if (isFirstTime){
            isFirstTime = !isFirstTime;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(homeLocation, 11));
        }
    }

    //this is to create custom marker
    public Bitmap createCustomMarker() {
        Context context = getApplicationContext();
        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker, null);
        CircleImageView markerImage = (CircleImageView) marker.findViewById(R.id.user_dp);
        Bitmap image = BitmapFactory.decodeResource(getResources(),R.drawable.user_image);
        markerImage.setImageBitmap(image);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap1 = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap1);
        marker.draw(canvas);
        return bitmap1;
    }
}