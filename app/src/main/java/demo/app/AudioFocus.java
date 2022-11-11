package demo.app;

import android.content.*;
import android.media.*;

public abstract class AudioFocus {

	private AudioFocus() { }

	public abstract boolean requestFocus();

	public abstract void abandonFocus();

	public static AudioFocus of(AudioManager manager, AudioManager.OnAudioFocusChangeListener listener) {
		if (Utils.isOreo())
			return new AudioFocusOreoImpl(manager, listener);
		return new AudioFocusDefaultImpl(manager, listener);
	}

	private static final class AudioFocusDefaultImpl extends AudioFocus {

		private final AudioManager.OnAudioFocusChangeListener listener;
		private final AudioManager manager;

		public AudioFocusDefaultImpl(AudioManager manager, AudioManager.OnAudioFocusChangeListener listener) {
			this.listener = listener;
			this.manager = manager;
		}

		@Override
		public boolean requestFocus() {
			return manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
		}

		@Override
		public void abandonFocus() {
			manager.abandonAudioFocus(listener);
		}
	}

	private static final class AudioFocusOreoImpl extends AudioFocus {

		private final AudioManager manager;

		private final AudioFocusRequest focusRequest;

		public AudioFocusOreoImpl(AudioManager manager, AudioManager.OnAudioFocusChangeListener listener) {
			this.manager = manager;
			this.focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
				.setAudioAttributes(MediaUtils.DEFAULT_AUDIO_ATTRS)
				.setOnAudioFocusChangeListener(listener)
				.build();
		}

		@Override
		public boolean requestFocus() {
			return manager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
		}

		@Override
		public void abandonFocus() {
			manager.abandonAudioFocusRequest(focusRequest);
		}
	}
}
