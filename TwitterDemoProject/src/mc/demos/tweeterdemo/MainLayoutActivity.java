package mc.demos.tweeterdemo;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import mc.demos.tweeterdemo.fragments.TweeterPostsFragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

public class MainLayoutActivity extends SherlockFragmentActivity {

	private Boolean isInForegrgound = true;
	private static Twitter tweeterApiController;
	private static RequestToken requestToken;
	private static SharedPreferences mSharedPreferences;
	private Boolean twitterWasInitialized = false;

	// life cycle methods
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_Sherlock);
		if (TwitterDemoConstants.SUPPORTS_HONEYCOMB) {
			StrictMode.setThreadPolicy(ThreadPolicy.LAX);
		}
		setContentView(R.layout.activity_main_layout);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		// initialize tweeterApiController

		/**
		 * Handle OAuth Callback
		 */
		Uri uri = getIntent().getData();
		if (uri != null) {
			final String uriString = uri.toString();
			if (uriString.contains(TwitterDemoConstants.CALLBACKURL)) {
				String verifier = uri
						.getQueryParameter(TwitterDemoConstants.IEXTRA_OAUTH_VERIFIER);
				try {

					AccessToken accessToken = tweeterApiController
							.getOAuthAccessToken(requestToken, verifier);
					Editor e = mSharedPreferences.edit();
					e.putString(TwitterDemoConstants.PREF_KEY_TOKEN,
							accessToken.getToken());
					e.putString(TwitterDemoConstants.PREF_KEY_SECRET,
							accessToken.getTokenSecret());
					e.commit();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG)
							.show();
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		isInForegrgound = true;
		if (isConnected()) {
			if (!!twitterWasInitialized) {
				String oauthAccessToken = mSharedPreferences.getString(
						TwitterDemoConstants.PREF_KEY_TOKEN, "");
				String oAuthAccessTokenSecret = mSharedPreferences.getString(
						TwitterDemoConstants.PREF_KEY_SECRET, "");

				ConfigurationBuilder confbuilder = new ConfigurationBuilder();
				Configuration conf = confbuilder
						.setOAuthConsumerKey(TwitterDemoConstants.CONSUMER_KEY)
						.setOAuthConsumerSecret(
								TwitterDemoConstants.CONSUMER_SECRET)
						.setOAuthAccessToken(oauthAccessToken)
						.setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
						.build();
				tweeterApiController = new TwitterFactory(conf).getInstance();

				if (getSupportFragmentManager().findFragmentById(
						R.id.fragmentContainer) == null) {
					FragmentTransaction ft = getSupportFragmentManager()
							.beginTransaction();
					ft.add(R.id.fragmentContainer, new TweeterPostsFragment());
					ft.commit();
				}
				twitterWasInitialized = true;
			}
		} else
			askOAuth();
	}

	@Override
	protected void onPause() {
		super.onPause();
		isInForegrgound = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		tweeterApiController = null;
	}

	// actions
	/**
	 * check if the account is authorized
	 * 
	 * @return
	 */
	private boolean isConnected() {

		return mSharedPreferences.getString(
				TwitterDemoConstants.PREF_KEY_TOKEN, null) != null;
	}

	private void askOAuth() {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder
				.setOAuthConsumerKey(TwitterDemoConstants.CONSUMER_KEY);
		configurationBuilder
				.setOAuthConsumerSecret(TwitterDemoConstants.CONSUMER_SECRET);
		Configuration configuration = configurationBuilder.build();
		tweeterApiController = new TwitterFactory(configuration).getInstance();

		try {
			requestToken = tweeterApiController
					.getOAuthRequestToken(TwitterDemoConstants.CALLBACKURL);
			Toast.makeText(this, "Please authorize this app!",
					Toast.LENGTH_LONG).show();
			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse(requestToken.getAuthenticationURL())));
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	// getters
	public Boolean getIsInForegrgound() {
		return isInForegrgound;
	}

	public Twitter getTweeterApiController() {
		return tweeterApiController;
	}
}
