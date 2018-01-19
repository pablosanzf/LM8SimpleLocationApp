package es.deusto.lm8_simplelocationapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import es.deusto.ivazquez.android.net.SimpleHttpClient;

import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DebugUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;


/**
 * A sample app for exploring location through Google Play Services
 *Las interfaces dos son para la comunicación con google y otro es con el location listener
 * @author Inaki Vazquez
 * @version 1.0
 */
public class MainActivity extends Activity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		LocationListener {

	// For accessing Google Play Services
	private GoogleApiClient mGoogleApiClient;

	private TextView txtLocation;
	private ProgressBar barConnectivity;

	// For storing the last up-to-date smartphone location
	private Location location = null;

	// Change this phone number to avoid collisions with the other students
	private String mPhoneNumber = "34678000991";

	// Internal code of the operation for identifying the callback
	private static final int REQUEST_PERMISSION_LOCATION_UPDATES = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtLocation = (TextView) findViewById(R.id.txtLocation);
		barConnectivity = (ProgressBar) findViewById(R.id.barConnection);
		barConnectivity.setMax(100);
		barConnectivity.setVisibility(View.GONE);

	}

	@Override
	protected void onStart() {
		super.onStart();
		// Start the connection to Google Play Services (a callback will be received with the status of the connection)
		connectToGooglePlayServices();
		if (connectToGooglePlayServices() == true) {
			Log.i("Location client", "el conectar va bien");
		} else {
			Log.i("Location client", "el conectar va mal");
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Start the disconnection process from Google Play Services (a callback will be received with the status of the process)
		disconnectFromGooglePlayServices();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_update_location) {
			// Download the most up-to-date location registered in the Web
			refreshLocation();
		} else if (item.getItemId() == R.id.action_send_location) {
			// Upload the most recent location detected by the smartphone to the Web
			sendLocation();
		} else if (item.getItemId() == R.id.action_settings) {
		}
		return true;
	}


	/**
	 * Starts the process of downloading the last known location from the Web
	 */
	private void refreshLocation() {
		// First check if there is connectivity
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected()) {
			// OK -> Access the Internet
			txtLocation.setVisibility(View.GONE);
			barConnectivity.setProgress(30);
			barConnectivity.setVisibility(View.VISIBLE);

			new RefreshLocation().execute(getString(R.string.location_service_uri) + "get_last.php?device_id=" + mPhoneNumber);
		} else {
			// No -> Display error message
			Toast.makeText(this, R.string.msg_error_no_connection, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Starts the process of uploading the location of the smartphone to the Web
	 */
	private void sendLocation() {
		// Check if there is connectivity
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected() && mGoogleApiClient.isConnected()) {
			// OK -> Access the Internet
			new SendLocation().execute();
		} else {
			// No -> Display error message
			Toast.makeText(this, R.string.msg_error_no_connection, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Starts the connection process to Google Play Services
	 * @return
	 */
	private boolean connectToGooglePlayServices() {
		// Check that Google Play Services is available
		if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.SUCCESS) {
			return false;
		}

		// Just log Google Play Services version code to check errors.
		// If the version installed in the smartphone is lower than the version linked in the app (build.gradle) our app will not work
		Log.i("Location client", "Play services version code: " + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE);

		// Create the Google API Client
		mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();

		// And connect!
		mGoogleApiClient.connect();
		return true;
	}

	/**
	 * Starts the disconnction process from Google Play Services
	 */
	private void disconnectFromGooglePlayServices() {
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
			Log.i("Location client", "Disconnected");
		}
	}

	/**
	 * Callback when the connection to Google Play Services fails
	 * @param arg0 additional info on the status
	 */
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.i("Location client", "Connection failed");
		Toast.makeText(this, R.string.msg_error_no_connection, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Callback when the connection to Google Play Services is successful
	 * @param arg0 additional data about the connection may be provided here
	 */
	@Override
	public void onConnected(Bundle arg0) {
		Log.i("Location client", "Connected");
		// Request location updates
		checkLocationPermission();
	}

	/**
	 * Callback when the connection to Google Play Services is suspended
	 * @param i a constant indicating the cause
	 */
	@Override
	public void onConnectionSuspended(int i) {
		Log.i("Location client", "Connection suspended");
	}

	/**
	 * Method that checks if the app has the ACCESS_FINE_LOCATION permission and triggers the runtime request to the user if required
	 * or starts receiving location updates if the permission has been granted
	 * By using the ContextCompat version this works on API level >= 23 and < 23 devices (dangerous or non dangerous permissions)
	 */
	public void checkLocationPermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			//mis cambios descomentar toda
			/*
			Log.i("Location client", "TOY ENEL IFFFFF");
			// If there is a known location

			location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			Log.i("Location client", "TOY ENEL IFFFFF" + location);

			// Update if changed
			LocationRequest mLocationRequest = LocationRequest.create()
					.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
					.setInterval(5000);
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);*/
		} else {
			Log.i("Location client", "TOY ENEL ELSEEE");
			ActivityCompat.requestPermissions(this,
					new String[]{
							Manifest.permission.ACCESS_FINE_LOCATION
					}, REQUEST_PERMISSION_LOCATION_UPDATES);
		}
	}

	/**
	 * Callback from the request permission in runtime (API level >=23)
	 * If the permission is granted, location updates are requested
	 *
	 * @param requestCode operation code
	 * @param permissions permissions
	 * @param grantResults results of the requests
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_PERMISSION_LOCATION_UPDATES:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// Permission Granted
					LocationRequest mLocationRequest = LocationRequest.create()
							.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
							.setInterval(5000);
					LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
				}
				break;
		}

	}

	/**
	 * Callback originated from requesting location updates, when the location changes
	 * @param location the new location of the smartphone
	 */
	@Override
	public void onLocationChanged(Location location) {
		// Location has changed, do any action if required
		this.location = location;
		Log.i("Location client", "New location: 00000000000000000000000000000000000000 " + location.toString());
	}

	/**
	 * Convenience class to access the Internet and update UI elements
	 */
	private class RefreshLocation extends AsyncTask<String, Integer, String>{

		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			SimpleHttpClient shc = new SimpleHttpClient(url);
			publishProgress(40);
			String result = shc.doGet();
			if(result != null){
				Log.i("Location data",result);
				double[] coords = getLatLonFromJson(result);
				publishProgress(100);
				return coords[0] + "," + coords[1];
			}else
				return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			barConnectivity.setProgress(values[0].intValue());
		}

		@Override
		protected void onPostExecute(String result) {
			barConnectivity.setVisibility(View.GONE);
			txtLocation.setVisibility(View.VISIBLE);
			if(result != null){
				txtLocation.setText(result);
			}else{
				Toast.makeText(MainActivity.this, R.string.msg_error_server, Toast.LENGTH_SHORT).show();
			}
		}

		private double[] getLatLonFromJson(String data){
			double[] coords = {0.0,0.0};
			try {
				JSONObject json = new JSONObject(data);
				coords[0] = json.getDouble("latitude");
				coords[1] = json.getDouble("longitude");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return coords;
		}

	}

	/**
	 * Convenience class to access the Internet and update UI elements
	 */
	private class SendLocation extends AsyncTask<Void, Void, Boolean>{

		@Override
		protected Boolean doInBackground(Void... params) {
			if (location == null) {
				Log.i("LocationClient URL", "URL NULLLLL");
				return false;
			}else{
			// Formatting the timestamp
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			String timestamp =  sdf.format(new Date());

			String locationEndpoint = getString(R.string.location_service_uri) + "add_position.php?device_id=" + mPhoneNumber;

			String url = locationEndpoint
					+ "&latitude=" + location.getLatitude()
					+ "&longitude=" + location.getLongitude()
					+ "&timestamp_utc=" + timestamp;
			Log.i("LocationClient URL", url);

			SimpleHttpClient shc = new SimpleHttpClient(url);
			shc.doGet();
			return true;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(!result)
				Toast.makeText(getApplicationContext(), R.string.msg_error_server, Toast.LENGTH_SHORT).show();
		}


	}


}