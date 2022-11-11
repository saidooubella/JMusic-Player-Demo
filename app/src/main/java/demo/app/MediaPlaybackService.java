package demo.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v4.content.*;
import android.support.v4.media.*;
import android.support.v4.media.session.*;
import java.util.*;

public final class MediaPlaybackService extends MediaBrowserServiceCompat {

	private static final String MEDIA_ROOT_ID = "demo.app.MEDIA_ROOT_ID";
	private static final String CHANNEL_NAME = "Media Playback";
	private static final String CHANNEL_ID = "demo.app.MediaPlayback";

	private static final int NOTIFICATION_ID = 1;

	private MediaControllerCompat.Callback controllerCallback;
	private MediaControllerCompat controller;

	private PlaybackStateCompat.Builder stateBuilder;
	private MediaSessionCompat mediaSession;

	private TracksLoader tracksLoader;
	private AppExecutors executors;
	private Player player;

	private Task tracksTask;

	private NotificationManager manager;
	private boolean inForeground;

	@Override
	public void onCreate() {
		super.onCreate();

        mediaSession = new MediaSessionCompat(this, "Demo");

        mediaSession.setFlags(
			MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
			MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        stateBuilder = new PlaybackStateCompat.Builder();
		stateBuilder.setActions(
			PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
			PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
			PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
			PlaybackStateCompat.ACTION_PLAY_PAUSE |
			PlaybackStateCompat.ACTION_SEEK_TO |
			PlaybackStateCompat.ACTION_PAUSE |
			PlaybackStateCompat.ACTION_PLAY |
			PlaybackStateCompat.ACTION_STOP);

		mediaSession.setSessionActivity(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
        mediaSession.setPlaybackState(stateBuilder.build());
		mediaSession.setMetadata(MediaUtils.EMPTY_METADATA);
        mediaSession.setCallback(new SessionCallback());
		mediaSession.setActive(true);

        setSessionToken(mediaSession.getSessionToken());

		executors = new AppExecutors();
		tracksLoader = new TracksLoader(executors, this);

		player = new Player(executors, this, mediaSession, stateBuilder);

		tracksTask = tracksLoader.loadAsync(new Continuation<List<MediaMetadataCompat>>() {
				@Override
				public void resume(List<MediaMetadataCompat> value) {
					player.updateTracksList(value);
					notifyChildrenChanged(MEDIA_ROOT_ID);
				}
			});

		manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (Utils.isOreo() && manager.getNotificationChannel(CHANNEL_ID) == null) {
			final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			manager.createNotificationChannel(channel);
		}

		controller = new MediaControllerCompat(this, mediaSession);

		controllerCallback = new MediaControllerCompat.Callback() {

			private int oldState = controller.getPlaybackState().getState();

			@Override
			public void onPlaybackStateChanged(PlaybackStateCompat state) {

				if (state.getState() != oldState) {

					out: {
						
						if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
							if (!inForeground) {
								ContextCompat.startForegroundService(MediaPlaybackService.this, new Intent(MediaPlaybackService.this, MediaPlaybackService.class));
								startForeground(NOTIFICATION_ID, buildNotification(controller.getMetadata(), state));
								inForeground = true;
								break out;
							}
						} else {
							inForeground = false;
							stopForeground(false);
						}

						manager.notify(NOTIFICATION_ID, buildNotification(controller.getMetadata(), state));
					}
					
					oldState = state.getState();
				}
			}

			@Override
			public void onMetadataChanged(MediaMetadataCompat metadata) {
				manager.notify(NOTIFICATION_ID, buildNotification(metadata, controller.getPlaybackState()));
			}
		};

		controller.registerCallback(controllerCallback);
	}

	private Notification buildNotification(MediaMetadataCompat metadata, PlaybackStateCompat state) {

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
			.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
					  .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
					  .setMediaSession(mediaSession.getSessionToken())
					  .setShowActionsInCompactView(0, 1, 2)
					  .setShowCancelButton(true))
			.setShowWhen(false)
			.setSmallIcon(R.drawable.baseline_audiotrack_24)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setContentTitle(MediaUtils.mediaTitle(metadata))
			.setContentText(MediaUtils.mediaArtist(metadata))
			.setContentIntent(controller.getSessionActivity())
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setLargeIcon(Utils.bitmapFromUri(getContentResolver(), MediaUtils.mediaArt(metadata)))
			.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP));

		builder.addAction(new NotificationCompat.Action(R.drawable.baseline_skip_previous_24, "Prev", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

		if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
			builder.addAction(new NotificationCompat.Action(R.drawable.baseline_pause_24, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)));
		} else {
			builder.addAction(new NotificationCompat.Action(R.drawable.baseline_play_arrow_24, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)));
		}

		builder.addAction(new NotificationCompat.Action(R.drawable.baseline_skip_next_24, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

		return builder.build();
	}

	@Override
	public BrowserRoot onGetRoot(String p1, int p2, Bundle p3) {
		return new BrowserRoot(MEDIA_ROOT_ID, null);
	}

	@Override
	public void onLoadChildren(String p1, final Result<List<MediaBrowserCompat.MediaItem>> result) {
		result.detach();
		executors.io.execute(new Runnable() {
				@Override
				public void run() {
					result.sendResult(player.mediaItems());
				}
			});
	}

	@Override
	public void onDestroy() {

		controller.unregisterCallback(controllerCallback);

		mediaSession.setActive(false);
		mediaSession.release();
		mediaSession = null;

		tracksTask.cancel();
		player.release();

		super.onDestroy();
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		if (player.isPaused()) {
			stopSelf();
		}
	}

	private final class SessionCallback extends MediaSessionCompat.Callback {

		@Override
		public void onPlay() {
			player.apply(Player.Action.Play);
		}

		@Override
		public void onPause() {
			player.apply(Player.Action.Pause);
		}

		@Override
		public void onStop() {
			player.apply(Player.Action.Pause);
			if (inForeground) {
				inForeground = false;
				stopForeground(true);
				stopSelf();
			}
		}

		@Override
		public void onSkipToNext() {
			player.apply(Player.Action.SkipToNext);
		}

		@Override
		public void onSkipToPrevious() {
			player.apply(Player.Action.SkipToPrev);
		}

		@Override
		public void onSeekTo(long pos) {
			player.seekTo((int) pos);
		}

		@Override
		public void onPlayFromMediaId(String mediaId, Bundle extras) {
			player.apply(Player.Action.Play);
			player.playFromMediaId(mediaId);
		}
	}
}

