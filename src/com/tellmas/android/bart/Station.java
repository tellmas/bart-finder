package com.tellmas.android.bart;

import java.lang.Integer;
import java.lang.NumberFormatException;


public class Station {

	private String name;
	private String abbreviation;
	private String address;
	private String city;
	private String county;
	private String state;
	private String zipcode;
	private double latitude;
	private double longitude;

	public static final int MAX_ZIPCODE = 99950;
	public static final double MIN_LONGITUDE = -180;
	public static final double MAX_LONGITUDE = 180;
	public static final double MIN_LATITUDE = 0;
	public static final double MAX_LATITUDE = 90;


	public Station(String name, String abbreviation,
	               String address, String city, String county, String state, String zipcode,
	               double latitude, double longitude) {
		this.setName(name);
		this.setAbbreviation(abbreviation);
		this.setAddress(address);
		this.setCity(city);
		this.setCounty(county);
		this.setState(state);
		this.setZipcode(zipcode);
		this.setLatitude(latitude);
		this.setLongitude(longitude);
	}

	public Station(String name, double latitude, double longitude) {
		this(name, null, null, null, null, null, null, latitude, longitude);
	}

	public Station(String name) {
		this(name, null, null, null, null, null, null, 0, 0);
	}

	public Station() {
		this(null, null, null, null, null, null, null, 0, 0);
	}


	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getAbbreviation() {
		return this.abbreviation;
	}
	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public double getLatitude() {
		return this.latitude;
	}
	public void setLatitude(double latitude) throws NumberFormatException {
		if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
			throw new NumberFormatException();
		}
		this.latitude = latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}
	public void setLongitude(double longitude) throws NumberFormatException {
		if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
			throw new NumberFormatException();
		}
		this.longitude = longitude;
	}

	public String getAddress() {
		return this.address;
	}
	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return this.city;
	}
	public void setCity(String city) {
		this.city = city;
	}

	public String getCounty() {
		return this.county;
	}
	public void setCounty(String county) {
		this.county = county;
	}

	public String getState() {
		return this.state;
	}
	public void setState(String state) {
		this.state = state;
	}

	public String getZipcode() {
		return this.zipcode;
	}
	public void setZipcode(String zipcode) throws NumberFormatException {
		
		if (zipcode == null || zipcode.equals("")) {
			this.zipcode = null;
			return;
		}
		
		int zipNum = Integer.parseInt(zipcode);
		if (zipNum < 1 || zipNum > MAX_ZIPCODE || zipcode.length() != 5) {
			throw new NumberFormatException();
		}
		this.zipcode = zipcode;
	}

}
