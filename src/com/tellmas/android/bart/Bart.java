package com.tellmas.android.bart;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

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
public class Bart extends Activity {

    /*
     * number of 'station' nodes in 'stations.xml'
     */
    private final static int NUM_STATIONS = 44;
    /*
     * TAG for Android.util.Log
     */
    private final static String LOG_ID = "BART";

    private Location userLocation;
    private Station closestStation;

    LocationManager locationManager;

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
    /*
     * XML 'station' node name in 'stations.xml'
     */
    private static final String XML_TAG_NAME_STATION_ROOT = "station";


    /**
     *
     * @param savedInstanceState Bundle for onCreate
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.stationInfo = new ArrayList<Station>(NUM_STATIONS);

        this.getStations();
        
        setContentView(R.layout.main);

        // --- Refresh Location Button setup ---
        Button refreshLocationButton = (Button)findViewById(R.id.refresh_location);
        refreshLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView stationName = (TextView)findViewById(R.id.station_name);
                stationName.setText(getString(R.string.locating));
                getLocation();
            }
        });

        this.getLocation();
    }


    /**
     * Uses the system location services to determine the user's location on the planet
     */
    private void getLocation() {

        this.locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        // Determine which Location Sources are enabled
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        String preferedProvider = null;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (SecurityException se) {}
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException se) {}
        // Determine which Location Source to use
        if (networkEnabled) {
            preferedProvider = LocationManager.NETWORK_PROVIDER;
        } else if (gpsEnabled) {
            preferedProvider = LocationManager.GPS_PROVIDER;
        }

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                userLocation = location;
                locationManager.removeUpdates(this);
                displayStation();
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {
            }
            public void onProviderDisabled(String provider) {
                locationManager.removeUpdates(this);
            }
        };

        // if there's a location provider enabled...
        if (preferedProvider != null) {
            // ...register the listener with the Location Manager to receive location updates.
            locationManager.requestLocationUpdates(preferedProvider, 0, 0, locationListener);
        // ...else...
        } else {
            // ...notify the user.
            this.displayLocationError();
        }
    }

    
    /**
     * Displays in the Station name field the error message corresponding to no system location provider being enabled 
     */
    private void displayLocationError() {
        TextView errorView = (TextView)findViewById(R.id.station_name);
        errorView.setText(getString(R.string.error_location_access));

        Button refreshLocationButton = (Button)findViewById(R.id.refresh_location);
        refreshLocationButton.setVisibility(View.VISIBLE);
    }


    /**
     * Displays the name of the closest station to the set user Location
     */
    private void displayStation() {
        Station closestStation = null;
        String name = "";
        if (this.userLocation != null) {
            Log.i(LOG_ID, this.userLocation.getProvider());
            closestStation = this.getClosestStation(this.userLocation);
            Log.i(LOG_ID, closestStation.getName());
            name = closestStation.getName();
        }

        TextView stationName = (TextView)findViewById(R.id.station_name);
        stationName.setText(name);

        Button refreshLocationButton = (Button)findViewById(R.id.refresh_location);
        refreshLocationButton.setVisibility(View.VISIBLE);

        Button mapItButton = (Button)findViewById(R.id.map_it);
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
        Log.i(LOG_ID, "arraylist size: " + arrayLength);
        for (int i=0; i < arrayLength; i++) {
            Station st = (Station)this.stationInfo.get(i);
            //Log.i(LOG_ID, (this.stationInfo.get(i)).getLatitude() + "");
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
            Log.i(LOG_ID, "closest station: " + closestStation.getName());
            this.closestStation = closestStation; 
        } else {
            Log.i(LOG_ID, "closest station: NULL");
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
                        //Log.i(LOG_ID, "new tag: " + atWhichTag);
                        // if we're at the start of a <station>...
                        if (atWhichTag.equals(XML_TAG_NAME_STATION_ROOT)) {
                            station = new Station();
                            this.stationInfo.add(station);
                            //Log.i(LOG_ID, "new Station()");
                        }
                        // else if we're NOT at the start of a xml node we recognize...
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

                } catch(NumberFormatException nfe) {
                    // invalid xml value
                    String msg = nfe.getMessage();
                    msg.toString();
                } catch(Exception e) {
                    String msg = e.getMessage();
                    msg.toString();
                    Log.e(LOG_ID, msg, e);
                } finally {
                    // go to next element
                    eventType = xpp.next();
                }

            } // end while
        } catch(Exception e) {
            // reading xml failed
            String msg = e.getMessage();
            msg.toString();
        }

    }


    /**
     * Gets the last known location from the system location service
     *
     * @return the last known Location retruned from the system location service
     */
    private Location getLastKnownLocation() {
/*
        TextView stationName;

        stationName = (TextView)findViewById(R.id.station_name);
        stationName.setText("test");
*/
        Location location = null;
        //Log.d(LOG_ID, "in getLocation()");

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        // get the last known location
        try {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        // if location access is not turned on...
        } catch(NullPointerException npe) {
            //stationName.setText(stringErrorAccessLocation);
            Log.e(LOG_ID, npe.toString(), npe);
        } catch(Exception e) {
            Log.e(LOG_ID, e.toString(), e);
        }

        if (location == null) {
            Log.d(LOG_ID, "no Last Known Location");
        }

        return location;
    }


    /**
     *
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
                Log.e(LOG_ID, npe.toString(), npe);
            } catch (ActivityNotFoundException anfe) {
                Log.e(LOG_ID, anfe.toString(), anfe);
            }

        }

    }

}
