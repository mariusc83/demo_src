package mc.demos.tweeterdemo.fragments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.TwitterException;

import mc.demos.tweeterdemo.MainLayoutActivity;
import mc.demos.tweeterdemo.R;
import mc.demos.tweeterdemo.TwitterDemoConstants;
import mc.demos.tweeterdemo.adapters.LazyPhotosAdapter;
import mc.demos.tweeterdemo.loaders.AbstractLoader;
import mc.demos.tweeterdemo.models.TweeterPostModel;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * TweeterPostsFragment class
 * 
 * @author Marius Constantin
 * 
 */
public class TweeterPostsFragment extends SherlockListFragment implements
		LoaderCallbacks<ArrayList<TweeterPostModel>>, ComponentCallbacks {

	private MainLayoutActivity parentActivity;
	private PhotosListAdapter listAdapter;
	private EditText filterInput;
	// initial search value set to 'android'
	private String filterValue = "android";

	public TweeterPostsFragment() {
	}

	// life cycle
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		parentActivity = (MainLayoutActivity) getSherlockActivity();
		setHasOptionsMenu(true);
		setListShown(false);
		// set the max size of the disk cache folder to 10 MB
		listAdapter = new PhotosListAdapter(this,
				TwitterDemoConstants.PHOTOS_CACHE_DIR, 1024 * 1024 * 10);
		getListView().setAdapter(listAdapter);
		// initialize the loader
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<ArrayList<TweeterPostModel>> onCreateLoader(int arg0,
			Bundle arg1) {

		return new TweeterPostsLoader(parentActivity, this);
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<TweeterPostModel>> arg0) {
		if (listAdapter != null)
			listAdapter.clear();
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<TweeterPostModel>> arg0,
			ArrayList<TweeterPostModel> arg1) {
		if (arg1 != null && listAdapter != null)
			listAdapter.setData(arg1);

		if (isResumed())
			setListShownNoAnimation(true);
		else
			setListShown(true);
	}

	@Override
	public void onDestroyView() {
		if (listAdapter != null)
			listAdapter.clear();

		// clear LruCache and close Local Disk cache
		listAdapter.clearLruAndCloseDiskCache();
		getListView().setAdapter(null);
		super.onDestroyView();
		listAdapter = null;
		filterInput.setOnEditorActionListener(null);
		filterInput = null;

	}

	@Override
	public void onDetach() {
		super.onDetach();
		parentActivity = null;
	}

	// @Override
	// public void onTrimMemory(int level) {
	// if (level >= TRIM_MEMORY_MODERATE)
	// // if the level is greater than the middle level of the background
	// // memory we evict all the cached data
	// listAdapter.clearLruCache();
	// else if (level >= TRIM_MEMORY_BACKGROUND)
	// // the process has gone on the LRU list
	// listAdapter.getLruCache().trimToSize(
	// listAdapter.getLruCache().size() / 2);
	// }

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		// evict all the cached data
		listAdapter.clearLruCache();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		MenuItem item = menu.add(Menu.NONE, R.id.searchMenuItem, Menu.NONE,
				R.string.search_hint);
		item.setIcon(android.R.drawable.ic_menu_search)
				.setActionView(R.layout.search_widget_layout)
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_ALWAYS
								| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		// add the listeners on the actionView
		filterInput = (EditText) item.getActionView().findViewById(
				android.R.id.text1);
		filterInput
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {

					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH
								|| actionId == EditorInfo.IME_ACTION_DONE) {
							// refresh filter value
							filterValue = v.getText().toString();
							// hide the listView
							setListShown(false);
							// restart the loader
							getLoaderManager().restartLoader(0, null,
									TweeterPostsFragment.this);
							return true;
						}
						return false;
					}
				});
	}

	// loaders
	private static class TweeterPostsLoader extends
			AbstractLoader<TweeterPostModel> {

		// we will use a WeakReference here in order to avoid possible memory
		// leaks
		private WeakReference<TweeterPostsFragment> fragmentReference;

		public TweeterPostsLoader(Context c, TweeterPostsFragment f) {
			super(c);
			fragmentReference = new WeakReference<TweeterPostsFragment>(f);
		}

		@Override
		public ArrayList<TweeterPostModel> loadInBackground() {
			// interrogate TweeterApi according with the new search parameter
			ArrayList<TweeterPostModel> data = new ArrayList<TweeterPostModel>();
			if (fragmentReference != null && fragmentReference.get() != null) {
				final String filterValue = fragmentReference.get().filterValue;
				// initialize the Query
				final Query toSearchFor = new Query();
				toSearchFor.setQuery(filterValue);
				try {
					QueryResult result = fragmentReference.get().parentActivity
							.getTweeterApiController().search(toSearchFor);
					for (Tweet s : result.getTweets()) {
						TweeterPostModel m = new TweeterPostModel();
						m.setAvatarURL(s.getProfileImageUrl());
						m.setHeader(s.getFromUserName());
						m.setShortDescription(s.getText());
						data.add(m);
					}
				} catch (TwitterException e) {
					e.printStackTrace();
				}
			}

			return data;
		}
	}

	// adapter
	private static class PhotosListAdapter extends
			LazyPhotosAdapter<TweeterPostModel> {

		private final int maxPictureHeight;
		private final int maxPictureWidth;

		public PhotosListAdapter(Fragment f, String diskCacheDir,
				int externalDiskCacheSize) {
			super(f, diskCacheDir, externalDiskCacheSize);
			maxPictureHeight = f.getResources().getDimensionPixelSize(
					R.dimen.gallery_row_image_max_height);
			maxPictureWidth = f.getResources().getDimensionPixelSize(
					R.dimen.gallery_row_image_max_width);
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			View v = arg1;
			ViewHolder holder;
			Fragment f = fragmentReference.get();
			if (v == null && f != null) {
				v = fragmentReference.get().getLayoutInflater(null)
						.inflate(R.layout.thumbnail_row, arg2, false);
				holder = new ViewHolder();
				holder.photoHolder = (ImageView) v
						.findViewById(R.id.thumbnailContainer);
				holder.textHolder = (TextView) v
						.findViewById(android.R.id.text1);
				v.setTag(holder);

			}

			holder = (ViewHolder) v.getTag();
			final TweeterPostModel postModel = data.get(arg0);
			holder.textHolder.setText(postModel.getHeader());
			// first we search for the requested bitmap into the LruCache
			Bitmap b = lruCache.get(postModel.getAvatarURL());
			if (b != null) {
				holder.photoHolder.setImageBitmap(b);
			} else if (f != null) {
				Resources res = fragmentReference.get().getResources();
				// we assign an AsyncDrawable and start the BitmapWorkerTask
				// first we check if the current ImageView has already a working
				// task for the same image path
				if (cancelBitmapWorkerTask(holder.photoHolder,
						postModel.getAvatarURL())) {
					BitmapWorkerTask task = new BitmapWorkerTask(
							maxPictureWidth, maxPictureHeight, f,
							holder.photoHolder, postModel.getAvatarURL());
					AsyncDrawable drawable = new AsyncDrawable(res,
							BitmapFactory.decodeResource(res,
									R.drawable.ic_launcher), task);
					holder.photoHolder.setImageDrawable(drawable);
					task.execute(postModel.getAvatarURL());
				}
			}

			return v;
		}

		private static class ViewHolder {
			public ImageView photoHolder;
			public TextView textHolder;
		}

	}
}
