package demo.app;

import android.*;
import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.net.*;
import android.provider.*;
import android.support.v4.media.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public final class TracksLoader {

	private static final Uri ARTWORK_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");

	private static final String[] projection = {
		MediaStore.Audio.Media._ID,
		MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.ARTIST,
		MediaStore.Audio.Media.DURATION,
		MediaStore.Audio.Media.ALBUM_ID,
		MediaStore.Audio.Media.ALBUM,
	};

	private static final String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1";

	private static final String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

	private final AppExecutors executors;
	private final Context context;

	public TracksLoader(AppExecutors executors, Context context) {
		this.executors = executors;
		this.context = context;
	}

	public Task loadAsync(final Continuation<List<MediaMetadataCompat>> callback) {

		final AtomicBoolean cancelled = new AtomicBoolean();

		final Runnable task = new Runnable() {
			@Override
			public void run() {
				final List<MediaMetadataCompat> tracks = load(cancelled);
				if (cancelled.get()) return;
				callback.resume(tracks);
			}
		};

		final ContentObserver observer = new ContentObserver(executors.main.handler) {
			@Override
			public void onChange(boolean selfChange) {
				executors.io.execute(task);
			}
		};

		context.getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer);

		executors.io.execute(task);

		return new Task() {
			@Override
			public void cancel() {
				context.getContentResolver().unregisterContentObserver(observer);
				cancelled.set(true);
			}
		};
	}

	private List<MediaMetadataCompat> load(AtomicBoolean cancelled) {

		if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			return Collections.emptyList();
		}

		try (final Cursor cursor = context.getContentResolver().query(
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
			projection, selection, null, sortOrder)
		) {
			final List<MediaMetadataCompat> tracks = new ArrayList<>(cursor.getCount());

			final int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
			final int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
			final int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
			final int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
			final int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
			final int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);

			while (!cancelled.get() && cursor.moveToNext()) {

				final String id = cursor.getString(idColumn);
				final String albumId = cursor.getString(albumIdColumn);
				final String title = cursor.getString(titleColumn);
				final String artist = cursor.getString(artistColumn);
				final String album = cursor.getString(albumColumn);

				final long duration = cursor.getLong(durationColumn);

				final Uri data = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
				final Uri art = Uri.withAppendedPath(ARTWORK_CONTENT_URI, albumId);

				final MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
					.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, album)
					.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, art.toString())
					.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
					.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
					.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, art.toString())
					.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, data.toString())
					.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
					.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
					.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
					.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
					.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
					.build();

				tracks.add(metadata);
			}

			return tracks;
		}
	}
}
