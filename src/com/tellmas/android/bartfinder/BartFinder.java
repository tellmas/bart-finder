package com.tellmas.android.bartfinder;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import android.util.Log;
import java.lang.NumberFormatException;

import org.xmlpull.v1.XmlPullParser;



/**
 * Main Activity
 *
 */
public class BartFinder extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    /**
     * number of 'station' nodes in 'stations.xml'
     */
    private final static int NUM_STATIONS = 45;
    /**
     * TAG for Android.util.Log
     */
    private final static String LOG_ID = "BARTFINDER";

    private Location userLocation;
    private Station closestStation;

    LocationManager locationManager;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private boolean areUpdatesRequested;

    private ArrayList<Station> stationInfo;

    /**
     * XML nodes in 'stations.xml' and corresponding setter methods
     */
    private static final Map<String,String> xmlTags;
    static {
        Map<String,String> initMap = new HashMap<String,String>();
        initMap.put("stations", "");
        initMap.put("station", "");
        initMap.put("name", "setName");
        initMap.put("abbr", "setAbbreviation");
        initMap.put("gtfs_latitude", "setLatitude");
        initMap.put("gtfs_longitude", "setLongitude");
        initMap.put("address", "setAddress");
        initMap.put("city", "setCity");
        initMap.put("county", "setCounty");
        initMap.put("state", "setState");
        initMap.put("zipcode", "setZipcode");
        xmlTags = Collections.unmodifiableMap(initMap);
    };
    /**
     * XML 'station' node name in 'stations.xml'
     */
    private static final String XML_TAG_NAME_STATION_ROOT = "station";


    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int LOCATION_UPDATE_INTERVAL = 5000;
    private final static float LOCATION_UPDATE_DISPLACEMENT = 10.0f;


    /**
     *
     * @param savedInstanceState Bundle for onCreate
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_ID, "onCreate()");
        super.onCreate(savedInstanceState);

        this.stationInfo = new ArrayList<Station>(NUM_STATIONS);
        this.getStations();

        setContentView(R.layout.main);

        if (this.isPlayServicesConnected()) {
            Log.d(LOG_ID, "Attempting to instantiate a new LocationClient.");

            this.googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

            this.locationRequest = LocationRequest.create();
            this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            this.locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
            this.locationRequest.setSmallestDisplacement(LOCATION_UPDATE_DISPLACEMENT);
            this.areUpdatesRequested = true;

            this.locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        }
    }


    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onStart()
     */
    @Override
    protected void onStart() {
        Log.v(LOG_ID, "onStart()");
        super.onStart();

        this.googleApiClient.connect();
    }


    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        Log.v(LOG_ID, "onResume()");
        super.onResume();

        if (!(this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
              this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            this.displayLocationError();
        }
    }


    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause() {
        Log.v(LOG_ID, "onPause()");

        super.onPause();
    }


    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onStop()
     */
    @Override
    protected void onStop() {
        Log.v(LOG_ID, "onStop()");

        this.googleApiClient.disconnect();

        super.onStop();
    }


    /*
     * (non-Javadoc)
     * @see com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener#onConnectionFailed(com.google.android.gms.common.ConnectionResult)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.v(LOG_ID, "onConnectionFailed()");

        Log.w(LOG_ID, "Attempt to connect to Google Play Services failed.");
        if (result.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                result.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            // Thrown if Google Play services canceled the original PendingIntent
            } catch (IntentSender.SendIntentException sie) {
                Log.e(LOG_ID, "", sie);
            }
        } else {
            // If no resolution is available, display a dialog to the user with the error.
            this.showErrorDialog(result.getErrorCode(), "Unable to connect to Google Play Services");
        }
    }


    /*
     * (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnected(android.os.Bundle)
     */
    @Override
    public void onConnected(Bundle connection) {
        Log.v(LOG_ID, "onConnected()");

        Log.d(LOG_ID, "onConnected(): areUpdatesRequested: " + this.areUpdatesRequested);

        if (this.areUpdatesRequested) {
            LocationServices.FusedLocationApi.requestLocationUpdates(this.googleApiClient, this.locationRequest, this);
            this.userLocation = LocationServices.FusedLocationApi.getLastLocation(this.googleApiClient);
            try {
                Log.d(LOG_ID, this.userLocation.toString());
                this.displayStation();
            } catch (NullPointerException npe) {
                Log.d(LOG_ID, "user location object is null");
            }
        }
    }


    /*
     * (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnectionSuspended(int)
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(LOG_ID, "onConnectionSuspended()");
        Log.d(LOG_ID, "connection to Google Api Client suspended");
    }


    /*
     * (non-Javadoc)
     * @see com.google.android.gms.location.LocationListener#onLocationChanged(android.location.Location)
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.v(LOG_ID, "onLocationChanged()");

        // if passed same location as already had...
        if (this.userLocation.describeContents() == location.describeContents()) {
            // ...ignore it.
            Log.d(LOG_ID, "onLocationChanged(): same location received");
        // ...else passed a different location...
        } else {
            // ...use it.
            this.userLocation = location;
            this.displayStation();
        }
    }


    /**
     * Checks to see if Play Services is available and connected.
     * @return a boolean indicating if Play Services is connected or not
     */
    private boolean isPlayServicesConnected() {

        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode == ConnectionResult.SUCCESS) {
            Log.d(LOG_ID, "Google Play services is avaialble.");
            return true;
        } else {
            Log.w(LOG_ID, "Google Play services is NOT avaialble.");
            this.showErrorDialog(resultCode, "Google Play Services");
            return false;
        }
    }


    /*
     *
     */
    private void showErrorDialog(int code, String reason) {
        // Get the error dialog from Google Play services
        final Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                code,
                this,
                CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment = ErrorDialogFragment.newInstance(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(getFragmentManager(), reason);
        }
    }


    /**
     * Displays in the Station name field the error message corresponding to no system location provider being enabled
     */
    private void displayLocationError() {
        TextView errorView = (TextView)findViewById(R.id.station_name);
        errorView.setText(getString(R.string.error_location_access));
    }


    /**
     * Displays the name of the closest station to the set user Location
     */
    private void displayStation() {
        Station closestStation = null;
        String name = "";
        if (this.userLocation != null) {
            closestStation = this.getClosestStation(this.userLocation);
            name = closestStation.getName();
        }

        TextView stationName = (TextView)this.findViewById(R.id.station_name);
        stationName.setText(name);

        Button mapItButton = (Button)this.findViewById(R.id.map_it);
        mapItButton.setOnClickListener(new MapItOnClickListener(this.userLocation, this.closestStation));
        mapItButton.setVisibility(View.VISIBLE);
    }


    /**
     * Determines the closest Station to the user's Location
     *
     * @param location user's Location
     * @return the Station which was the closest to the passed Location
     */
    private Station getClosestStation(Location location) {
        Station closestStation = (Station)this.stationInfo.get(0);
        float shortestDistance = Float.MAX_VALUE;
        int arrayLength = this.stationInfo.size();
        for (int i=0; i < arrayLength; i++) {
            Station st = (Station)this.stationInfo.get(i);
            Location stationLocation = new Location(location);
            stationLocation.setLatitude(st.getLatitude());
            stationLocation.setLongitude(st.getLongitude());

            float distance = stationLocation.distanceTo(location);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestStation = st;
            }
        }

        if (closestStation != null) {
            this.closestStation = closestStation;
        }
        return closestStation;
    }

    /**
     * Uses the XML data in 'stations.xml' to populate the 'stationInfo' List
     */
    private void getStations() {

        String atWhichTag = "";
        try {
            XmlPullParser xpp = getResources().getXml(R.xml.stations);
            int eventType = xpp.getEventType();
            Station station = null;
            while(eventType != XmlPullParser.END_DOCUMENT) {
                try {
                    // if we're at the start of a node...
                    if (eventType == XmlPullParser.START_TAG) {
                        atWhichTag = xpp.getName();
                        // if we're at the start of a <station>...
                        if (atWhichTag.equals(XML_TAG_NAME_STATION_ROOT)) {
                            station = new Station();
                            this.stationInfo.add(station);
                        }
                        // else if we're NOT at the start of an xml node we recognize...
                        else if (! xmlTags.containsKey(atWhichTag)) {
                            //atWhichTag.put(XML_TAG_NAME_STATION_NAME, true);
                            //atWhichTag = XML_TAG_NAME_STATION_NAME;
                            // ...skip it.
                            atWhichTag = "";
                        }
                    // ...else we're at the value of the node.
                    } else if (eventType == XmlPullParser.TEXT) {
                        try {
                            if (atWhichTag.equals("name")) {
                                station.setName(xpp.getText());
                            } else if (atWhichTag.equals("gtfs_latitude")) {
                                double latitude = Double.valueOf(xpp.getText()).doubleValue();
                                station.setLatitude(latitude);
                            } else if (atWhichTag.equals("gtfs_longitude")) {
                                double longitude = Double.valueOf(xpp.getText()).doubleValue();
                                station.setLongitude(longitude);
                            } else if (atWhichTag.equals("address")) {
                                station.setAddress(xpp.getText());
                            } else if (atWhichTag.equals("city")) {
                                station.setCity(xpp.getText());
                            } else if (atWhichTag.equals("county")) {
                                station.setCounty(xpp.getText());
                            } else if (atWhichTag.equals("state")) {
                                station.setState(xpp.getText());
                            } else if (atWhichTag.equals("zipcode")) {
                                station.setZipcode(xpp.getText());
                            }
                        } catch (NullPointerException npe) {
                            // 'atWhichTag' was null, so just skip this node.
                        }

                    } else if (eventType == XmlPullParser.END_TAG) {

                        // if we've reached the end of the <station>...
                        if (atWhichTag.equals(XML_TAG_NAME_STATION_ROOT)) {
                            //publishProgress(station);
                        }
                        atWhichTag = "";
                    }
                    // ...else at START_DOCUMENT, in which case we don't do anything on it.

                // invalid xml value
                } catch(NumberFormatException nfe) {
                    if (Debug.LOG) {
                        String msg = nfe.getMessage();
                        msg.toString();
                        Log.e(LOG_ID, msg, nfe);
                    }
                } catch(Exception e) {
                    if (Debug.LOG) {
                        String msg = e.getMessage();
                        msg.toString();
                        Log.e(LOG_ID, msg, e);
                    }
                } finally {
                    // go to next element
                    eventType = xpp.next();
                }

            } // end while

        // reading xml failed
        } catch(Exception e) {
            if (Debug.LOG) {
                String msg = e.getMessage();
                msg.toString();
                Log.e(LOG_ID, msg, e);
            }
        }
    }


    /**
     * Intent launcher for Google Maps for the "Map it" button
     */
    public class MapItOnClickListener implements OnClickListener {

        private Location location;
        private Station station;


        public MapItOnClickListener(Location location, Station station) {
            this.location = location;
            this.station = station;
        }


        /* (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v) {
            String uri = "";
            Intent intent = null;
            uri = "https://maps.google.com/maps?" +
                "saddr="  + this.location.getLatitude() + "," + this.location.getLongitude() +
                "&daddr=" + this.station.getLatitude()  + "," + this.station.getLongitude() + "(" + station.getName() + " BART)";
            try {
                intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            } catch (NullPointerException npe) {
                if (Debug.LOG) {
                    Log.e(LOG_ID, npe.toString(), npe);
                }
            } catch (ActivityNotFoundException anfe) {
                if (Debug.LOG) {
                    Log.e(LOG_ID, anfe.toString(), anfe);
                }
            }
        }
    }


    /*
     * (non-Javadoc)
     * Handle results returned to the FragmentActivity by Google Play services.
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {

            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            // If the result code is Activity.RESULT_OK, try to connect again
                switch (resultCode) {
                    case Activity.RESULT_OK:
                    // TODO Try the request again
                    break;
                }
        }
    }


    private class Debug {
        public static final boolean LOG = true;
    }

}