package mc.demos.twitterdemo.actionproviders;

import mc.demos.tweeterdemo.R;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionProvider;

/**
 * SearchActionProvider class
 * 
 * @author Marius Constantin
 * 
 */
public class SearchActionProvider extends ActionProvider {

	private SearchActionWidgetListener callback;
	private EditText searchField;
	/** Context for accessing resources. */
	private final Context mContext;

	public SearchActionProvider(Context c) {
		super(c);
		mContext = c;
	}

	// life cycle methods
	@Override
	public View onCreateActionView() {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View v = inflater.inflate(R.layout.search_widget_layout, null);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		v.setLayoutParams(lp);
		searchField = (EditText) v.findViewById(android.R.id.text1);
		initListeners();
		return v;
	}

	// initializers
	private void initListeners() {
		searchField
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {

					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH) {
							if (callback != null)
								callback.onSearch(searchField.getText()
										.toString());
							return true;
						}

						return false;
					}
				});

		searchField.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			public void afterTextChanged(Editable s) {
				if (callback != null)
					callback.onSearch(s.toString());

			}
		});
	}

	// actions
	public void refresh() {
		searchField.setText("");
	}

	// setters
	public void setCallback(SearchActionWidgetListener callback) {
		this.callback = callback;
	}

	// interfaces
	public interface SearchActionWidgetListener {
		public void onSearch(String toSearchFor);
	}
}
