package mc.demos.tweeterdemo;

public final class TwitterDemoConstants {
	public static boolean SUPPORTS_FROYO = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
	public static boolean SUPPORTS_GINGERBREAD = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
	public static boolean SUPPORTS_HONEYCOMB = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	public static boolean SUPPORTS_ICE_CREAM_SANDWICH = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	public static boolean SUPPORTS_JELLYBEAN = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
	public static final String PHOTOS_CACHE_DIR = "tweeter_photos";
	public static final String CONSUMER_KEY = "W16i3oHkLVwOybczhDg99A";
	public static final String CONSUMER_SECRET = "4mvwDaJLIC5aiHag8ywr7GgTAru2NRN7dxRw4qhHBU";
	public static final String PREF_KEY_TOKEN = "oauth_token_secret";
	public static final String PREF_KEY_SECRET = "oauth_token";
	public static final String CALLBACKURL = "oauth://t4jsample";
	public static final String IEXTRA_AUTH_URL = "auth_url";
	public static final String IEXTRA_OAUTH_VERIFIER = "oauth_verifier";
	public static final String IEXTRA_OAUTH_TOKEN = "oauth_token";

	public enum Pages {
		TWEETER_POSTS;
	}
}
