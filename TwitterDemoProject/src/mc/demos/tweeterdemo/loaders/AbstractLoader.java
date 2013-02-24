package mc.demos.tweeterdemo.loaders;

import java.util.ArrayList;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * AbstractLoader class - can be extended in fragments in order to handle the
 * data loading processes
 * 
 * @author Marius Constantin
 * 
 * @param <D>
 */
public abstract class AbstractLoader<D> extends AsyncTaskLoader<ArrayList<D>> {
	protected ArrayList<D> data;

	public AbstractLoader(Context c) {
		super(c);
	}

	@Override
	public ArrayList<D> loadInBackground() {
		return null;
	}

	@Override
	public void deliverResult(ArrayList<D> _data) {

		// check to see if we are in the middle of reset event
		if (isReset()) {
			onReleaseResource(data);
		}

		// caching the data for future use
		this.data = _data;

		if (isStarted()) {
			super.deliverResult(data);
		}

	}

	@Override
	protected void onStartLoading() {
		// checking to see if we already have some data available
		if (data != null)
			deliverResult(data);
		else
			forceLoad();
		
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void onReset() {
		super.onReset();
		// ensure the loading is canceled
		onStopLoading();

		// release the current cached data
		if (isReset())
			onReleaseResource(data);
	}

	@Override
	public void onCanceled(ArrayList<D> data) {
		super.onCanceled(data);
		// release the cached data
		onReleaseResource(data);
	}

	public void onReleaseResource(ArrayList<D> data) {
		if (data != null) {
			data.clear();
			data = null;
		}
	}
}