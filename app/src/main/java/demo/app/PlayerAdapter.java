package demo.app;

import android.content.*;
import android.media.*;
import android.os.*;
import android.support.v4.media.*;
import java.io.*;

public final class PlayerAdapter implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

	private final AudioFocus audioFocus;
	private final Callback callback;
	private final Handler handler;
	private final Context context;

	private MediaPlayer player;
	private boolean playWhenReady;

	private final AudioManager.OnAudioFocusChangeListener focusListener = new AudioManager.OnAudioFocusChangeListener() {

		private boolean playOnAudioFocus = false;
		
		@Override
		public void onAudioFocusChange(int focusGain) {
			switch (focusGain) {
				case AudioManager.AUDIOFOCUS_GAIN: {
					if (playOnAudioFocus && !isPlaying()) {
						start();
					} else if (isPlaying()) {
						setVolume(1F);
					}
					playOnAudioFocus = false;
					break;
				}
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
					setVolume(.2F);
					break;
				}
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
					if (isPlaying()) {
						playOnAudioFocus = true;
						pause();
					}
					break;
				}
				case AudioManager.AUDIOFOCUS_LOSS: {
					playOnAudioFocus = false;
					audioFocus.abandonFocus();
					pause();
					break;
				}
			}
		}
	};

	private final Runnable progressTask = new Runnable() {
		@Override
		public void run() {
			callback.onPlaybackStateChanged(playWhenReady, progress());
			handler.postDelayed(this, 1000);
		}
	};

	private final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context p1, Intent p2) {
			pause();
		}
	};

	public PlayerAdapter(Context context, Handler handler, Callback callback) {
		this.audioFocus = AudioFocus.of((AudioManager) context.getSystemService(Context.AUDIO_SERVICE), focusListener);
		this.callback = callback;
		this.context = context;
		this.handler = handler;
	}

	public void prepare(final MediaMetadataCompat metadata) {

		if (player != null) {
			release(false);
		}

		callback.onMediaChanged(metadata);

		try {
			player = new MediaPlayer();
			player.setDataSource(context, MediaUtils.mediaUri(metadata));
			player.setAudioAttributes(MediaUtils.DEFAULT_AUDIO_ATTRS);
			player.setOnCompletionListener(this);
			player.setOnPreparedListener(this);
			player.prepareAsync();
		} catch (SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void release(boolean force) {

		if (player == null) {
			return;
		}

		player.stop();
		player.reset();
		player.release();
		player = null;

		callback.onPlaybackStateChanged(force ? false : playWhenReady, 0);

		if (force) {
			callback.onMediaChanged(MediaUtils.EMPTY_METADATA);
			playWhenReady = false;
		}
	}

	public void start() {
		if (player != null && !player.isPlaying() && audioFocus.requestFocus()) {
			context.registerReceiver(noisyReceiver, MediaUtils.NOISY_ACTION_FILTER);
			callback.onPlaybackStateChanged(true, progress());
			handler.post(progressTask);
			playWhenReady = true;
			player.start();
		}
	}

	public void pause() {
		if (player != null && player.isPlaying()) {
			callback.onPlaybackStateChanged(false, progress());
			context.unregisterReceiver(noisyReceiver);
			handler.removeCallbacks(progressTask);
			audioFocus.abandonFocus();
			playWhenReady = false;
			player.pause();
		}
	}

	public void seekTo(final long msec) {
		if (player != null) {
			player.seekTo((int) msec);
		}
	}
	
	public void setVolume(final float volume) {
		if (player != null) {
			player.setVolume(volume, volume);
		}
	}

	public int progress() {
		return player != null ? player.getCurrentPosition() : 0;
	}

	public boolean isPlaying() {
		return playWhenReady;
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		callback.onCompletion();
	}

	@Override
	public void onPrepared(MediaPlayer player) {
		if (playWhenReady) {
			start();
		}
	}

	public interface Callback {
		void onPlaybackStateChanged(boolean isPlaying, int progress);
		void onMediaChanged(MediaMetadataCompat metadata);
		void onCompletion();
	}
}
