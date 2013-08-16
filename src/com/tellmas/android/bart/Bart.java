package com.tellmas.android.bart;

import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.content.Context;

import android.widget.TextView;
import android.widget.Toast;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import android.util.Log;
import java.lang.NumberFormatException;
import java.lang.reflect.Method;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;



public class Bart extends Activity {

	private final static int NUM_STATIONS = 44;
	private final static String LOG_ID = "BART";
	
	
	private String stringErrorAccessLocation;
	private Location location;

	LocationManager locationManager;
	
	private ArrayList<Station> stationInfo;

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
	private static final String[] xmlTagNames = {
	}:
	private static final Map<String,String> xmlTagNames = new HashMap<String,String>() {
		"stations", "station", "name", "abbr", "gtfs_latitude", "gtfs_longitude",
		"address", "city", "county", "state", "zipcode"
	};
*/
	private static final String XML_TAG_NAME_ROOT = "stations";
	private static final String XML_TAG_NAME_STATION_ROOT = "station";
	private static final String XML_TAG_NAME_STATION_NAME = "name";
	private static final String XML_TAG_NAME_STATION_ABBR = "abbr";
	private static final String XML_TAG_NAME_STATION_LAT = "gtfs_latitude";
	private static final String XML_TAG_NAME_STATION_LONG = "gtfs_longitude";
	private static final String XML_TAG_NAME_STATION_STREET = "address";
	private static final String XML_TAG_NAME_STATION_CITY = "city";
	private static final String XML_TAG_NAME_STATION_COUNTY = "county";
	private static final String XML_TAG_NAME_STATION_STATE = "state";
	private static final String XML_TAG_NAME_STATION_ZIP = "zipcode";


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		this.stringErrorAccessLocation = getString(R.string.error_location_access);
		this.stationInfo = new ArrayList<Station>(NUM_STATIONS);

        setContentView(R.layout.main);

		//this.getClosestStation();

		//new StationInfoTask().execute();

        this.getStations();
        (Toast.makeText(Bart.this, Integer.valueOf(this.stationInfo.size()).toString(), Toast.LENGTH_LONG)).show();
        Location location = this.getLastKnownLocation();
        if (location == null) {

        	locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

    		// Define a listener that responds to location updates
    		LocationListener locationListener = new LocationListener() {
    			public void onLocationChanged(Location location) {
    				locationManager.removeUpdates(this);
    				displayStation(location);
    			}
    			public void onStatusChanged(String provider, int status, Bundle extras) {}
    			public void onProviderEnabled(String provider) {
    			}
    			public void onProviderDisabled(String provider) {
    				locationManager.removeUpdates(this);
    			}
    		};

    		// Register the listener with the Location Manager to receive location updates
    		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
        	this.displayStation(location);
        }
        
    }
    
    private void displayStation(Location location) {
        Station closestStation = null;
        String name = "blah";
        if (location != null) {
        	Log.i(LOG_ID, location.getProvider());
        	closestStation = this.getClosestStation(location);
        	Log.i(LOG_ID, closestStation.getName());
        	name = closestStation.getName();
        }

        TextView stationName = (TextView)findViewById(R.id.station_name);
        stationName.setText(name);    	
    }

    
    private Station getClosestStation(Location location) {
    	Station closestStation = (Station)this.stationInfo.get(0);
    	float shortestDistance = 1000000;
Log.i(LOG_ID, "arraylist size: " + this.stationInfo.size());
    	for (int i=0; i < this.stationInfo.size(); i++) {
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
    	} else {
    		Log.i(LOG_ID, "closest station: NULL");
    	}
    	return closestStation;
    }
    
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

/*						
Method meth = null;
Log.i(LOG_ID, "at the text of a node: " + xpp.getText());
						try {
Log.i(LOG_ID, "tag name: " + xmlTags.get(atWhichTag));
							String methodName = xmlTags.get(atWhichTag);
							Class<? extends Station> c = station.getClass();
Log.i(LOG_ID, "class: " + c.getName());
							Class[] params = new Class[1];
							params[0] = Station.class;
							Method method = c.getMethod(methodName, params);
							//Method method = station.getClass().getMethod(xmlTags.get(atWhichTag));
meth = method;
Log.i(LOG_ID, method.getName());
							method.invoke(station, xpp.getText());
							Log.i(LOG_ID, "called modifiying method: " + method.getName());
						} catch (NoSuchMethodException nsme) {
							Log.e(LOG_ID, nsme.toString());
						} catch (NullPointerException npe) {
							// 'atWhichTag' was null, so just skip this node.
Log.i(LOG_ID, "NullPointerException when invoking method: " + meth.getName());
						}
*/
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
    
    

	private Location getLastKnownLocation() {
/*
		TextView stationName;

		stationName = (TextView)findViewById(R.id.station_name);
		stationName.setText("test");
*/
		Location location = null;
Log.d(LOG_ID, "in getLocation()");

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


	private class StationInfoTask extends AsyncTask<Void, Station, Void> {

/*
		Map<String,boolean> atWhichTag = new (HashMap<String,boolean>() {
			{
				put("name", false);
				put("abbr", false);
				put("gtfs_latitude", false);
				put("gtfs_longitude", false);
				put("address", false);
				put("city", false);
				put("county", false);
				put("state", false);
				put("zipcode", false);
			}
		};
*/

		@Override
		protected Void doInBackground(Void... unused) {
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
								Method method = station.getClass().getMethod(xmlTags.get(atWhichTag));
								method.invoke(xpp.getText());
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

publishProgress(new Station());
			return null;
		}

		@Override
		protected void onProgressUpdate(Station... station) {
Station st = new Station("blah");
//this.stationInfo.add(st);
stationInfo.add(st);
Toast.makeText(Bart.this, "Added station", Toast.LENGTH_SHORT).show();
			//this.stationInfo.add(station);
		}

		@Override
		protected void onPostExecute(Void unused) {
			Toast.makeText(Bart.this, "Done!", Toast.LENGTH_SHORT).show();
		}

	}
}
