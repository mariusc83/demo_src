package mc.demos.tweeterdemo.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * TweeterPostModel class - used to hold and deliver TweeterPost information
 * 
 * @author Marius Constantin
 * 
 */
public class TweeterPostModel implements Parcelable {
	private String avatarURL = "";
	private String header = "";
	private String shortDescription = "";

	public TweeterPostModel() {
	}

	public TweeterPostModel(Parcel in) {
		avatarURL = in.readString();
		header = in.readString();
		shortDescription = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(avatarURL);
		dest.writeString(header);
		dest.writeString(shortDescription);
	}

	// setters
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	// getters
	public String getAvatarURL() {
		return avatarURL;
	}

	public String getHeader() {
		return header;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	// CREATOR
	public static final Parcelable.Creator<TweeterPostModel> CREATOR = new Parcelable.Creator<TweeterPostModel>() {
		@Override
		public TweeterPostModel createFromParcel(Parcel source) {
			return new TweeterPostModel(source);
		}

		@Override
		public TweeterPostModel[] newArray(int size) {
			return new TweeterPostModel[size];
		}
	};
}
