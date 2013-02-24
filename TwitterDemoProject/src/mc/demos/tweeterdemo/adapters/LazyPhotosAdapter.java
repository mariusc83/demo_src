package mc.demos.tweeterdemo.adapters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import mc.demos.tweeterdemo.utils.Utils;

import com.example.android.bitmapfun.util.DiskLruCache;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * LazyPhotosAdapter class
 * 
 * @author Marius Constantin
 * 
 */
public class LazyPhotosAdapter<D> extends BaseAdapter {
	protected LruCache<String, Bitmap> lruCache;
	protected DiskLruCache diskLruCacher;
	private Boolean waitForCacher = true;
	private Object diskLruCacherLocker = new Object();
	protected ArrayList<D> data;
	protected WeakReference<Fragment> fragmentReference;
	protected final String DISK_CACHE_DIR;
	protected final int EXTERNAL_DISK_CACHE_SIZE;
	private File photosCacheDir;

	/**
	 * 
	 * @param f
	 * @param diskCacheDir
	 * @param externalDiskCacheSize
	 */
	public LazyPhotosAdapter(Fragment f, String diskCacheDir,
			int externalDiskCacheSize) {
		fragmentReference = new WeakReference<Fragment>(f);
		// initialize the lruCache
		ActivityManager am = (ActivityManager) f.getActivity()
				.getSystemService(Context.ACTIVITY_SERVICE);
		lruCache = new PhotosLruCache(1024 * 1024 * am.getMemoryClass() / 8);
		DISK_CACHE_DIR = diskCacheDir;
		EXTERNAL_DISK_CACHE_SIZE = 1024 * 1024 * 10;
		photosCacheDir = getDiskLruCacheDirectory(DISK_CACHE_DIR);
		if (photosCacheDir != null) {
			// start the AsyncTask to initialize the DiskLruCache
			new InitDiskLruCache(EXTERNAL_DISK_CACHE_SIZE)
					.execute(photosCacheDir);
		}
	}

	// actions
	/**
	 * initialize and return the photosCacheDir
	 * 
	 * @param folderName
	 * @return File
	 */
	private File getDiskLruCacheDirectory(String folderName) {
		if (fragmentReference.get() != null) {
			// get the current cache path
			// if the external storage is available for writing use the external
			// storage
			// if not we will use the internal storage
			String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState()) ? fragmentReference.get()
					.getActivity().getExternalCacheDir().getPath()
					: fragmentReference.get().getActivity().getCacheDir()
							.getPath();
			return new File(cachePath + File.separator + folderName);

		} else
			return null;

	}

	public boolean cancelBitmapWorkerTask(ImageView imgView, String path) {
		BitmapWorkerTask task = getBitmapWorkerTask(imgView);
		if (task != null)
			if (task.bitmapPath().equals(path)) {
				return false;
			} else {
				task.cancel(true);
				return true;
			}
		else
			return true;
	}

	/**
	 * Cache the current bitmap internally and on the disk cache folder
	 * 
	 * @param b
	 * @param key
	 */
	public void cacheBitmap(Bitmap b, String key) {

		if (lruCache != null && lruCache.get(key) == null) {
			lruCache.put(key, b);
		}

		synchronized (diskLruCacherLocker) {
			OutputStream out = null;
			try {
				DiskLruCache.Snapshot snapshot = diskLruCacher.get(key);
				if (snapshot == null) {
					final DiskLruCache.Editor editor = diskLruCacher.edit(key);
					if (editor != null) {
						out = editor.newOutputStream(0);
						b.compress(CompressFormat.PNG, 70, out);
						editor.commit();
						out.close();
					}
				} else {
					snapshot.getInputStream(0).close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {
				}
			}
		}

	}

	/**
	 * clear adapter data
	 */
	public void clear() {
		if (data != null)
			data.clear();
	}

	public void clearLruAndCloseDiskCache() {
		clearLruCache();
		new DiskCacheActionsTask(this)
				.execute(DiskCacheActionsTask.CLOSE_DISK_CACHE);
	}

	/**
	 * clear the stored data into the LruCache storage memory
	 */
	public void clearLruCache() {
		if (lruCache != null)
			lruCache.evictAll();
	}

	public void clearDiskCache() {
		if (diskLruCacher != null)
			try {
				diskLruCacher.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Closes the disk cache associated with this ImageCache object. Note that
	 * this includes disk access so this should not be executed on the main/UI
	 * thread.
	 */
	public void closeDiskCache() {
		synchronized (diskLruCacherLocker) {
			if (diskLruCacher != null) {
				try {
					if (!diskLruCacher.isClosed()) {
						diskLruCacher.close();
						diskLruCacher = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// actions
	public void clearData() {
		if (data != null)
			data.clear();
	}

	// setters
	public void setData(ArrayList<D> _data) {
		if (data == null)
			data = new ArrayList<D>();

		data.clear();
		data.addAll(_data);
		notifyDataSetChanged();
	}

	// getters
	public LruCache<String, Bitmap> getLruCache() {
		return lruCache;
	}

	@SuppressWarnings("unchecked")
	public BitmapWorkerTask getBitmapWorkerTask(ImageView imgView) {
		final Drawable d = imgView.getDrawable();

		if (d instanceof LazyPhotosAdapter.AsyncDrawable) {
			return ((AsyncDrawable) d).bitmapWorkerTask();
		} else
			return null;
	}

	public Bitmap decodeBitmapFromPath(int maxWidth, int maxHeight,
			String path, String bitmapKey) {
		// first we check the image into the diskCache
		Bitmap b = getBitmapFromDiskCache(bitmapKey);

		if (b == null) {
			InputStream is = null;
			// we download the image from server
			try {

				URL fileUrl = new URL(path);
				HttpURLConnection connection = (HttpURLConnection) fileUrl
						.openConnection();
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(15000);
				connection.setRequestMethod("GET");
				connection.setDoInput(true);

				connection.connect();
				if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
					is = connection.getInputStream();
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;

					BitmapFactory.decodeStream(is, null, options);
					Utils.getInstance().calculateInSampleSize(options,
							maxWidth, maxHeight);
					options.inJustDecodeBounds = false;
					is.close();
					connection.disconnect();
					connection = (HttpURLConnection) fileUrl.openConnection();
					connection.setConnectTimeout(10000);
					connection.setReadTimeout(15000);
					connection.setRequestMethod("GET");
					connection.setDoInput(true);
					connection.connect();
					if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
						is = connection.getInputStream();
						b = BitmapFactory.decodeStream(is, null, options);
					}

				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				try {
					if (is != null)
						is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (b != null) {
			cacheBitmap(b, bitmapKey);

		}

		return b;
	}

	/**
	 * 
	 * @param key
	 * @return Bitmap - image from external storage
	 */
	public Bitmap getBitmapFromDiskCache(String key) {

		synchronized (diskLruCacherLocker) {
			while (waitForCacher) {
				try {
					diskLruCacherLocker.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (diskLruCacher != null) {
				InputStream inputStream = null;
				try {
					final DiskLruCache.Snapshot snapshot = diskLruCacher
							.get(key);
					if (snapshot != null) {
						inputStream = snapshot.getInputStream(0);
						if (inputStream != null) {
							final Bitmap bitmap = BitmapFactory
									.decodeStream(inputStream);
							return bitmap;
						}
					}
				} catch (final IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (inputStream != null) {
							inputStream.close();
						}
					} catch (IOException e) {
					}
				}
			}

		}

		return null;
	}

	@Override
	public int getCount() {
		return data != null ? data.size() : 0;
	}

	@Override
	public Object getItem(int arg0) {
		if (data != null)
			return data.get(arg0);
		else
			return null;
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	// inner classes

	/**
	 * PhotosLruCache class
	 * 
	 * @author Marius Constantin
	 * 
	 */
	protected class PhotosLruCache extends LruCache<String, Bitmap> {
		public PhotosLruCache(int maxSize) {
			super(maxSize);
		}

		@Override
		protected int sizeOf(String key, Bitmap value) {
			// TODO Auto-generated method stub
			return super.sizeOf(key, value);
		}

	}

	protected class InitDiskLruCache extends AsyncTask<File, Void, Void> {
		private final int maxCacheSize;

		public InitDiskLruCache(int maxSize) {
			maxCacheSize = maxSize;
		}

		@Override
		protected Void doInBackground(File... params) {
			/*
			 * put a lock on the diskLruCacherLocker in order to force other
			 * threads to wait for this one to finish*
			 */

			synchronized (diskLruCacherLocker) {
				File cacheDir = params[0];
				try {
					diskLruCacher = DiskLruCache.open(cacheDir, 1, 1,
							maxCacheSize);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					// put the wait locker to false
					waitForCacher = false;
					// notify any waiting threads
					diskLruCacherLocker.notifyAll();
				}
			}
			return null;
		}
	}

	/**
	 * BitmapWorkerTask - used to load images from server side or from diskCache
	 * in a Non-UI thread
	 * 
	 * @author Marius
	 * 
	 */
	public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
		private final int imageMaxWidth;
		private final int imageMaxHeight;
		private final String bitmapKey;
		private WeakReference<ImageView> imageReference;
		private WeakReference<Fragment> fragmentReference;
		private volatile String imagePath;

		public BitmapWorkerTask(int maxWidth, int maxHeight, Fragment f,
				ImageView imgView, String _bitmapKey) {
			imageMaxWidth = maxWidth;
			imageMaxHeight = maxHeight;
			bitmapKey = _bitmapKey;
			imageReference = new WeakReference<ImageView>(imgView);
			fragmentReference = new WeakReference<Fragment>(f);
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			imagePath = params[0];
			if (imagePath == null) {
				cancel(true);
				return null;
			}

			if (imageReference == null || imageReference.get() == null) {
				cancel(true);
				return null;

			}
			return decodeBitmapFromPath(imageMaxWidth, imageMaxHeight,
					imagePath, bitmapKey);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (fragmentReference != null && fragmentReference.get() != null) {
				if (imageReference != null) {
					ImageView img = imageReference.get();
					if (img != null) {
						BitmapWorkerTask task = getBitmapWorkerTask(img);
						if (task.equals(this) && result != null)
							img.setImageBitmap(result);
					}
				}
			}
		}

		// getters
		public String bitmapPath() {
			return imagePath;
		}
	}

	private static class DiskCacheActionsTask extends
			AsyncTask<Integer, Void, Void> {
		public static final int CLOSE_DISK_CACHE = 0;
		@SuppressWarnings("rawtypes")
		private final WeakReference<LazyPhotosAdapter> adapterReference;

		@SuppressWarnings("rawtypes")
		public DiskCacheActionsTask(LazyPhotosAdapter adapter) {
			adapterReference = new WeakReference<LazyPhotosAdapter>(adapter);
		}

		@Override
		protected Void doInBackground(Integer... params) {
			int message = params[0];
			switch (message) {
			case CLOSE_DISK_CACHE:
				if (adapterReference != null && adapterReference.get() != null)
					adapterReference.get().closeDiskCache();
				break;
			}

			return null;
		}
	}

	public final class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> taskReference;

		public AsyncDrawable(Resources res, Bitmap defaultBitmap,
				BitmapWorkerTask task) {
			super(res, defaultBitmap);
			taskReference = new WeakReference<LazyPhotosAdapter<D>.BitmapWorkerTask>(
					task);
		}

		public BitmapWorkerTask bitmapWorkerTask() {
			return taskReference.get();
		}
	}

}
