package demo.app;

import android.content.*;
import android.media.*;
import android.net.*;
import android.support.v4.media.*;
import android.support.v4.media.session.*;
import java.io.*;

public final class MediaUtils {
	
	public static final IntentFilter NOISY_ACTION_FILTER = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

	public static final MediaMetadataCompat EMPTY_METADATA = new MediaMetadataCompat.Builder()
	.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "No Available Track")
	.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "No Available Track")
	.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "No Available Track")
	.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "No Available Track")
	.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "No Available Track")
	.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "No Available Track")
	.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, Uri.EMPTY.toString())
	.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, Uri.EMPTY.toString())
	.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, Uri.EMPTY.toString())
	.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "0")
	.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
	.build();
	
	public static final AudioAttributes DEFAULT_AUDIO_ATTRS = new AudioAttributes.Builder()
	.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
	.setUsage(AudioAttributes.USAGE_MEDIA)
	.build();

	private MediaUtils() { }
	
	public static boolean isPlaying(PlaybackStateCompat state) {
		return state.getState() == PlaybackStateCompat.STATE_PLAYING;
	}
	
	public static String mediaId(MediaMetadataCompat metadata) {
		return metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
	}
	
	public static String mediaArtist(MediaMetadataCompat metadata) {
		return metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
	}
	
	public static String mediaTitle(MediaMetadataCompat metadata) {
		return metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
	}
	
	public static long mediaDurationInSeconds(MediaMetadataCompat metadata) {
		return metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000;
	}
	
	public static Uri mediaArt(MediaMetadataCompat metadata) {
		return Uri.parse(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
	}

	public static Uri mediaUri(MediaMetadataCompat metadata) {
		return Uri.parse(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI));
	}

	public static boolean exists(ContentResolver resolver, Uri uri) {
		try {
			resolver.openFileDescriptor(uri, "r").close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
