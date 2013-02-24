package mc.demos.tweeterdemo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.net.ConnectivityManagerCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Utils {
	public enum ConnectionType {
		WIFI, MOBILE, NONE;

		@Override
		public String toString() {
			return super.toString();
		}
	}

	// debug tag name
	private static final String TAG_NAME = "Utils";

	private static Utils instance;

	public Utils() {

	}

	// getters
	public static Utils getInstance() {
		if (instance == null)
			instance = new Utils();
		return instance;
	}

	public InputStream getHttpInputStream(String url) throws IOException {
		InputStream toReturnSteam = null;
		try {
			URL urlObj = new URL(url);
			URLConnection connection = urlObj.openConnection();

			// checking to see if the current connection is a HttpURLConnection
			if (!(connection instanceof HttpURLConnection)) {
				throw (new IOException(
						"the required connection is not a HttpUrlConnection"));
			} else {
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				httpConnection.setConnectTimeout(20000);
				httpConnection.setAllowUserInteraction(false);
				httpConnection.setRequestMethod("GET");
				httpConnection.setUseCaches(true);
				httpConnection.setInstanceFollowRedirects(true);
				httpConnection.connect();
				if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					toReturnSteam = httpConnection.getInputStream();
				}
			}
		} catch (IOException e) {
			throw (e);
		}

		return toReturnSteam;
	}

	public OutputStream getHttpOutputStream(String url) throws IOException {
		OutputStream toReturnSteam = null;
		try {
			URL urlObj = new URL(url);
			URLConnection connection = urlObj.openConnection();

			// checking to see if the current connection is a HttpURLConnection
			if (!(connection instanceof HttpURLConnection)) {
				throw (new IOException(
						"the required connection is not a HttpUrlConnection"));
			} else {
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				httpConnection.setConnectTimeout(20000);
				httpConnection.setAllowUserInteraction(false);
				httpConnection.setRequestMethod("PUT");
				httpConnection.setInstanceFollowRedirects(true);
				httpConnection.connect();
				if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					toReturnSteam = httpConnection.getOutputStream();
				}
			}
		} catch (IOException e) {
			throw (e);
		}

		return toReturnSteam;
	}

	/** returns the current internet connection status (true/false) */
	public boolean getInternetConnection(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		// checking to see if the netInfo is null (airplane mode) or netInfo is
		// connected
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		} else
			return false;
	}

	/**
	 * return the available connection type on the current device
	 * 
	 * @param c
	 *            (current activity Context)
	 * @return connectionType (the current connection type : wifi, mobile or
	 *         none in case of no internet connection)
	 */
	public ConnectionType getInternetConnectionType(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (ConnectivityManagerCompat.isActiveNetworkMetered(cm))
			return ConnectionType.MOBILE;
		else
			return ConnectionType.WIFI;
	}

	/** returns JSON format from the specified URL */
	public JSONObject getJSONFromUrl(String serverUrl, String operation) {
		JSONObject result = null;
		StringBuilder builder = new StringBuilder();
		// initialize HTTPConnectionParams
		HttpParams connectionParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(connectionParams, 20000);
		// initialize the API auth credentials
		HttpClient client = new DefaultHttpClient(connectionParams);
		// ((DefaultHttpClient)client).getCredentialsProvider().setCredentials(new
		// AuthScope(JSnoopApplication.HOST, 80), new
		// UsernamePasswordCredentials(JSnoopApplication.API_USERNAME,JSnoopApplication.API_PASSWORD));

		try {
			HttpGet request = new HttpGet(serverUrl + operation);
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}

				result = new JSONObject(builder.toString());
			}
		} catch (IOException e) {
			Log.e(TAG_NAME, "IOException on getJSONFromUrl:" + operation);
		} catch (JSONException je) {
			Log.e(TAG_NAME, "JSONException on getJSONFromUrl:" + operation);
		} finally {
			client.getConnectionManager().shutdown();
		}
		return result;
	}

	// tools
	public Bitmap resizeBitmap(Bitmap b, float newWidth, float newHeight) {
		float scaleRatio = Math.min(newWidth / b.getWidth(),
				newHeight / b.getHeight());
		Matrix bMatrix = new Matrix();
		bMatrix.postScale(scaleRatio, scaleRatio);
		Bitmap newB = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(),
				bMatrix, true);
		return newB;
	}

	public int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		return inSampleSize;
	}

	public void setFontForAllTextViewsInHierarchy(ViewGroup aViewGroup,
			Typeface aFont) {
		for (int i = 0; i < aViewGroup.getChildCount(); i++) {
			View _v = aViewGroup.getChildAt(i);
			if (_v instanceof TextView) {
				((TextView) _v).setTypeface(aFont);
			} else if (_v instanceof ViewGroup) {
				setFontForAllTextViewsInHierarchy((ViewGroup) _v, aFont);
			}
		}
	}

	/**
	 * checks intent availability according with the provided action
	 * 
	 * @param context
	 * @param action
	 * @return
	 */
	public boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

}
