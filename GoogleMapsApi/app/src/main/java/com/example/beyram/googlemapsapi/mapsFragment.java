package com.example.beyram.googlemapsapi;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by beyram.
 * Fragment google map.
 */
public class mapsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener {

    GoogleMap mMap;
    ProgressDialog progress;
    private GoogleApiClient mLocationClient;
    Marker currLocationMarker;
    Marker ParkingMarker;
    private RequestQueue requestQueue;
    ArrayList<Parking> list;
    Location currentLocation;
    View view;
    final String API_KEY = "#######" ;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle("Parking Near Me");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.map_layout, container, false);
        startMap();
        setHasOptionsMenu(true);
        return view;
    }


    /**
     * Loading Map
     */
    public void startMap(){
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Loading");
        progress.setMessage("Loading position...");
        progress.setCancelable(false);
        if (isGoogleServiceOk()) {
            if (initMap()) {
                statusCheck();
                mLocationClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(LocationServices.API)
                        .addApi(Places.GEO_DATA_API)
                        .addApi(Places.PLACE_DETECTION_API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                mLocationClient.connect();
            } else {
                Toast.makeText(getActivity(), "Map is Not Ready", Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * Initialize map
     */
    private boolean initMap() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
            mMap.setOnMarkerClickListener(this);
        }
        return (mMap != null);
    }

    /**
     * Verify google services
     */
    private boolean isGoogleServiceOk() {
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, getActivity(), 123456);
            dialog.show();
        } else {
            Toast.makeText(getActivity(), "Can't Connect to Google Services !", Toast.LENGTH_LONG).show();
        }
        return false;
    }


    /**
     * Success of the connection
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
        if (currentLocation == null || ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity() ,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 123);
                mLocationClient.reconnect();
                currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
            } else {
                mLocationClient.reconnect();
                currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
            }

        } else {
            showCurrentPlace(currentLocation.getLatitude(), currentLocation.getLongitude());
            getNearbyParking(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
    }

    /**
     * Loading Nearby Parking
     */
    private void getNearbyParking(double lat, double lng) {
        list = new ArrayList<Parking>();
        String GoogleLink = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + lat + "," + lng + "&radius=50000&type=parking&key="+API_KEY+"&language=fr";
        progress.setMessage("Loading Nearby Parkings...");
        requestQueue = Volley.newRequestQueue(getActivity());
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, GoogleLink, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Parking parking = null;
                        try {
                            JSONArray listMusem = response.getJSONArray("results");
                            for (int i = 0; i < listMusem.length(); i++) {
                                JSONObject row = listMusem.getJSONObject(i);
                                parking = new Parking();
                                parking.setLat(row.getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
                                parking.setLng(row.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
                                parking.setName(row.getString("name").isEmpty() ? " " : row.getString("name"));
                                list.add(parking);
                                Log.d("mus", parking.getName());
                            }
                            displayParking(list);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getActivity(), "error: " + "Cannot Find Museum", Toast.LENGTH_LONG).show();
                    }
                }
        );
        // add it to the RequestQueue
        requestQueue.add(getRequest);
    }

    /**
     * Display List Parking
     */
    private void displayParking(ArrayList<Parking> list) {
        progress.dismiss();
        Log.d("list", list.toString());
        for (int i = 0; i < list.size(); i++) {
            LatLng latLang = new LatLng(list.get(i).getLat(), list.get(i).getLng());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(list.get(i).getName());
            markerOptions.position(latLang);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            ParkingMarker = mMap.addMarker(markerOptions);

        }
        Toast.makeText(getActivity(), "Nearby Parkings:" + list.size(), Toast.LENGTH_LONG).show();

    }

    /**
     * Animation to the current position
     */
    private void showCurrentPlace(double lat, double lng) {
        LatLng latLang = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLang);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        if (currLocationMarker != null) {
            currLocationMarker.remove();
        }
        currLocationMarker = mMap.addMarker(markerOptions.title("Current position"));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLang, (float) 9.5);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * Show Information about Every Marker
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTitle().equals("Current position")) {
            Toast.makeText(getActivity(), "Current position", Toast.LENGTH_SHORT).show();
        } else {
            Parking parking = getParkingFromList(marker.getTitle());
            LatLng latmark = marker.getPosition();
            LatLng currentMark = currLocationMarker.getPosition();
            double distance = CalculationByDistance(latmark, currentMark);
            Toast.makeText(getActivity(), parking.getName() + "\n Distance: " + distance + "KM", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public Parking getParkingFromList(String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name)) {
                return list.get(i);
            }
        }
        return null;
    }

    /**
     * Calculate the distance from the current position
     */
    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);
        double disance = Radius * c;
        return Math.floor(disance * 100) / 100;
    }

    // Check GPS
    public void statusCheck() {
        final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Votre GPS est désactivé. Utiliser ma position?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        progress.show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        System.exit(0);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();

    }
}