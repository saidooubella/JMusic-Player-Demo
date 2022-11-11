package demo.app;

import android.*;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.os.*;
import android.support.v4.content.*;
import android.support.v4.media.*;
import android.support.v4.media.session.*;
import android.support.v7.graphics.*;
import android.text.format.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.*;
import com.bumptech.glide.load.*;
import com.bumptech.glide.load.engine.*;
import com.bumptech.glide.request.*;
import com.bumptech.glide.request.target.*;

import com.bumptech.glide.request.target.Target;

public final class MainActivity extends Activity {

	private static final int REQUEST_PERMISSION_CODE = 1;
	private static final int DEFAULT_COLOR = 0xFF_212121;

	private MediaBrowserCompat.ConnectionCallback connectionCallbacks;
	private MediaBrowserCompat mediaBrowser;

	private MediaControllerCompat.Callback controllerCallback;
	private MediaControllerCompat mediaController;

	private BindingActivityMain binding;

	private boolean canUpdateProgress = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		binding = new BindingActivityMain(getLayoutInflater());
        setContentView(binding.root);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setSystemUiFlags(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						 View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
						 View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		
		getWindow().setNavigationBarColor(Color.TRANSPARENT);
		getWindow().setStatusBarColor(Color.TRANSPARENT);
		
		applyWindowInsetsPadding(binding.root);

		binding.artistText.setSelected(true);
		binding.titleText.setSelected(true);

		binding.artImage.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View p1) {
					startActivity(new Intent(MainActivity.this, TracksActivity.class));
				}
			});

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			requestPermissions(new String[] {
								   Manifest.permission.WRITE_EXTERNAL_STORAGE,
								   Manifest.permission.READ_EXTERNAL_STORAGE,
							   }, REQUEST_PERMISSION_CODE);
		} else {
			initMediaBrowserClient();
		}
    }

	private void initMediaBrowserClient() {

		connectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {

			@Override
			public void onConnected() {
				try {
					final MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
					mediaController = new MediaControllerCompat(MainActivity.this, token);
					buildTransportControls();
				} catch (RemoteException e) {}
			}

			@Override
			public void onConnectionSuspended() {
				setControlEnabled(false);
			}

			@Override
			public void onConnectionFailed() {

			}
		};

		mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaPlaybackService.class), connectionCallbacks, null);
		mediaBrowser.connect();
	}

	private void setControlEnabled(final boolean enabled) {
		binding.progressBar.setEnabled(enabled);
		binding.playButton.setEnabled(enabled);
		binding.prevButton.setEnabled(enabled);
		binding.nextButton.setEnabled(enabled);
	}

	private void buildTransportControls() {

		setControlEnabled(true);

		final MediaControllerCompat.TransportControls controls = mediaController.getTransportControls();
		final MediaMetadataCompat metadata = mediaController.getMetadata();
		final PlaybackStateCompat state = mediaController.getPlaybackState();

		binding.durationText.setText(DateUtils.formatElapsedTime(MediaUtils.mediaDurationInSeconds(metadata)));
		binding.progressText.setText(DateUtils.formatElapsedTime(state.getPosition() / 1000));
		binding.artistText.setText(MediaUtils.mediaArtist(metadata));
		binding.titleText.setText(MediaUtils.mediaTitle(metadata));

		binding.progressBar.setMax((int) MediaUtils.mediaDurationInSeconds(metadata));
		binding.playButton.setImageResource(MediaUtils.isPlaying(state) ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);

		updateUIColors(DEFAULT_COLOR);

		final RequestListener<Bitmap> requestListener = new RequestListener<Bitmap>() {

			@Override
			public boolean onLoadFailed(GlideException p1, Object p2, Target<Bitmap> p3, boolean p4) {
				updateUIColors(DEFAULT_COLOR);
				return false;
			}

			@Override
			public boolean onResourceReady(Bitmap bitmap, Object p2, Target<Bitmap> p3, DataSource p4, boolean p5) {
				final Palette palette = Palette.from(bitmap).generate();
				updateUIColors(dominantColor(palette));
				return false;
			}

			private int dominantColor(final Palette palette) {
				final Palette.Swatch swatch = palette.getDominantSwatch();
				return swatch != null ? swatch.getRgb() : DEFAULT_COLOR;
			}
		};

		Glide.with(this).asBitmap().load(MediaUtils.mediaArt(metadata))
			.placeholder(R.drawable.baseline_audiotrack_24)
			.error(R.drawable.baseline_audiotrack_24)
			.addListener(requestListener)
			.into(binding.artImage);

		binding.progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
					binding.progressText.setText(DateUtils.formatElapsedTime(p1.getProgress()));
				}

				@Override
				public void onStartTrackingTouch(SeekBar p1) {
					canUpdateProgress = false;
				}

				@Override
				public void onStopTrackingTouch(SeekBar p1) {
					controls.seekTo(p1.getProgress() * 1000);
					canUpdateProgress = true;
				}
			});

		binding.prevButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View p1) {
					controls.skipToPrevious();
				}
			});

		binding.nextButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View p1) {
					controls.skipToNext();
				}
			});

		binding.playButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View p1) {
					if (MediaUtils.isPlaying(mediaController.getPlaybackState())) {
						controls.pause();
					} else {
						controls.play();
					}
				}
			});

		controllerCallback = new MediaControllerCompat.Callback() {

			@Override
			public void onPlaybackStateChanged(PlaybackStateCompat state) {
				binding.playButton.setImageResource(MediaUtils.isPlaying(state) ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
				if (canUpdateProgress) {
					binding.progressBar.setProgress((int) (state.getPosition() / 1000));
				}
			}

			@Override
			public void onMetadataChanged(MediaMetadataCompat metadata) {

				binding.durationText.setText(DateUtils.formatElapsedTime(MediaUtils.mediaDurationInSeconds(metadata)));
				binding.progressBar.setMax((int) MediaUtils.mediaDurationInSeconds(metadata));
				binding.artistText.setText(MediaUtils.mediaArtist(metadata));
				binding.titleText.setText(MediaUtils.mediaTitle(metadata));

				Glide.with(MainActivity.this).asBitmap()
					.load(MediaUtils.mediaArt(metadata))
					.placeholder(R.drawable.baseline_audiotrack_24)
					.error(R.drawable.baseline_audiotrack_24)
					.addListener(requestListener)
					.into(binding.artImage);
			}

			@Override
			public void onSessionDestroyed() {
				mediaBrowser.disconnect();
				// maybe schedule a reconnection using a new MediaBrowser instance
			}
		};

		mediaController.registerCallback(controllerCallback);
	}

	private void updateUIColors(final int backgroundColor) {

		final boolean isBgLight = Color.luminance(backgroundColor) > .4f;
		final int secondayOnBg = isBgLight ? 0xAA_000000 : 0xAA_FFFFFF;
		final int primaryOnBg = isBgLight ? 0xBB_000000 : 0xBB_FFFFFF;

		binding.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(secondayOnBg));
		binding.progressBar.setProgressTintList(ColorStateList.valueOf(primaryOnBg));
		binding.progressBar.setThumbTintList(ColorStateList.valueOf(primaryOnBg));
		binding.prevButton.setImageTintList(ColorStateList.valueOf(primaryOnBg));
		binding.playButton.setImageTintList(ColorStateList.valueOf(primaryOnBg));
		binding.nextButton.setImageTintList(ColorStateList.valueOf(primaryOnBg));
		// binding.root.setBackgroundColor(backgroundColor);
		ObjectAnimator.ofArgb(binding.root, BackgroundColorProperty.instance(), backgroundColor).start();
		binding.progressText.setTextColor(primaryOnBg);
		binding.durationText.setTextColor(primaryOnBg);
		binding.artistText.setTextColor(secondayOnBg);
		binding.titleText.setTextColor(primaryOnBg);

		final int flags = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
		if (isBgLight) setSystemUiFlags(flags); else unsetSystemUiFlags(flags);
	}

	private void setSystemUiFlags(int flags) {
		final View decorView = getWindow().getDecorView();
		final int systemUiVisibility = decorView.getSystemUiVisibility();
		decorView.setSystemUiVisibility(systemUiVisibility | flags);
	}

	private void unsetSystemUiFlags(int flags) {
		final View decorView = getWindow().getDecorView();
		final int systemUiVisibility = decorView.getSystemUiVisibility();
		decorView.setSystemUiVisibility(systemUiVisibility & ~flags);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mediaController != null) {
			mediaController.unregisterCallback(controllerCallback);
		}

		if (mediaBrowser != null && mediaBrowser.isConnected()) {
			mediaBrowser.disconnect();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode != REQUEST_PERMISSION_CODE) {
			return;
		}

		if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
			Toast.makeText(this, "Tracks art might not show up", Toast.LENGTH_SHORT).show();
		}

		if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
			Toast.makeText(this, "The app cannot load music files", Toast.LENGTH_SHORT).show();
		} else {
			initMediaBrowserClient();
		}
	}
	
	private static void applyWindowInsetsPadding(final View view) {

		final int left = view.getPaddingLeft();
		final int top = view.getPaddingTop();
		final int right = view.getPaddingRight();
		final int bottom = view.getPaddingBottom();

		view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
				@Override
				public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
					view.setPadding(
						left + insets.getSystemWindowInsetLeft(),
						top + insets.getSystemWindowInsetTop(),
						right + insets.getSystemWindowInsetRight(),
						bottom + insets.getSystemWindowInsetBottom()
					);
					return insets;
				}
			});

		requestApplyInsetsWhenAttached(view);
	}

	private static void requestApplyInsetsWhenAttached(final View view) {
		if (view.isAttachedToWindow()) {
			view.requestApplyInsets();
		} else {
			view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

					@Override
					public void onViewDetachedFromWindow(View view) {}

					@Override
					public void onViewAttachedToWindow(View view) {
						view.removeOnAttachStateChangeListener(this);
						view.requestApplyInsets();
					}
				});
		}
	}
}

final class BackgroundColorProperty extends Property<View, Integer> {

	private static final Property<View, Integer> INSTANCE = new BackgroundColorProperty();

	private BackgroundColorProperty() {
		super(Integer.class, "backgroundColor");
	}

	@Override
	public void set(View view, Integer value) {
		view.setBackgroundColor(value);
	}

	@Override
	public Integer get(View view) {
		if (view.getBackground() instanceof ColorDrawable)
			return ((ColorDrawable) view.getBackground()).getColor();
		return Color.TRANSPARENT;
	}

	public static Property<View, Integer> instance() {
		return INSTANCE;
	}
}
