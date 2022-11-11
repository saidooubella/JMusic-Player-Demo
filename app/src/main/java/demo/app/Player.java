package demo.app;

import android.content.*;
import android.support.v4.media.*;
import android.support.v4.media.session.*;
import demo.app.*;
import java.util.*;

public final class Player {

	private List<MediaMetadataCompat> metadataList = Collections.emptyList();

	private ContentResolver resolver;
	private PlayerAdapter player;

	private int mediaIndex = -1;

	public Player(
		final AppExecutors executors,
		final Context context,
		final MediaSessionCompat mediaSession,
		final PlaybackStateCompat.Builder stateBuilder
	) {

		this.resolver = context.getContentResolver();

		this.player = new PlayerAdapter(context, executors.main.handler, new PlayerAdapter.Callback() {

				@Override
				public void onPlaybackStateChanged(boolean isPlaying, int progress) {
					final int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
					mediaSession.setPlaybackState(stateBuilder.setState(state, progress, 1F).build());
				}

				@Override
				public void onMediaChanged(MediaMetadataCompat metadata) {
					mediaSession.setMetadata(metadata);
				}

				@Override
				public void onCompletion() {
					apply(Action.SkipToNext);
				}
			});
	}

	public void playFromMediaId(String mediaId) {
		for (int i = 0; i < metadataList.size(); i++) {
			if (MediaUtils.mediaId(metadataList.get(i)).equals(mediaId)) {
				player.prepare(metadataList.get(i));
				mediaIndex = i;
				break;
			}
		}
	}

	public void updateTracksList(List<MediaMetadataCompat> value) {

		if (value.isEmpty()) {

			player.release(true);
			mediaIndex = -1;

		} else {

			int index = mediaIndex;

			final String oldMediaId;

			if (index != -1) {

				oldMediaId = MediaUtils.mediaId(metadataList.get(index));

				while (!metadataList.isEmpty() && !MediaUtils.exists(resolver, MediaUtils.mediaUri(metadataList.get(index)))) {
					metadataList.remove(index);
					if (index >= metadataList.size()) {
						index -= 1;
					}
				}

			} else {
				oldMediaId = null;
			}

			if (index < 0) {
				mediaIndex = 0;
			} else {
				final String target = MediaUtils.mediaId(metadataList.get(index));
				for (int i = 0; i < value.size(); i++) {
					if (MediaUtils.mediaId(value.get(i)).equals(target)) {
						mediaIndex = i;
						break;
					}
				}
			}

			if (!Objects.equals(oldMediaId, MediaUtils.mediaId(value.get(mediaIndex)))) {
				player.prepare(value.get(mediaIndex));
			}
		}

		metadataList = value;
	}

	public void apply(Action action) {

		if (notAvailable()) {
			return;
		}

		switch (action) {
			case SkipToPrev: {
				mediaIndex = --mediaIndex >= 0 ? mediaIndex : metadataList.size() - 1;
				player.prepare(metadataList.get(mediaIndex));
				break;
			}
			case SkipToNext: {
				mediaIndex = ++mediaIndex < metadataList.size() ? mediaIndex : 0;
				player.prepare(metadataList.get(mediaIndex));
				break;
			}
			case Pause: {
				player.pause();
				break;
			}
			case Play: {
				player.start();
				break;
			}
		}
	}

	public void seekTo(long pos) {

		if (notAvailable()) {
			return;
		}

		player.seekTo((int) pos);
	}

	public void release() {
		player.release(true);
	}

	public boolean isPaused() {
		return !player.isPlaying();
	}

	private boolean notAvailable() {
		return mediaIndex == -1;
	}

	public List<MediaBrowserCompat.MediaItem> mediaItems() {

		final List<MediaBrowserCompat.MediaItem> mediaItemList = new ArrayList<>(metadataList.size());

		for (final MediaMetadataCompat metadata : metadataList) {
			mediaItemList.add(new MediaBrowserCompat.MediaItem(metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
		}

		return mediaItemList;
	}

	public enum Action {
		SkipToPrev, SkipToNext, Play, Pause,
	}
}
