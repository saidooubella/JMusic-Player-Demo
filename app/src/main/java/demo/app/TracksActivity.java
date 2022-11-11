package demo.app;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.support.v4.content.*;
import android.support.v4.media.*;
import android.support.v4.media.session.*;
import android.support.v7.recyclerview.extensions.*;
import android.support.v7.util.*;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.*;
import java.util.*;

import android.support.v7.recyclerview.extensions.ListAdapter;

public final class TracksActivity extends Activity {

	private static final int REQUEST_PERMISSION_CODE = 1;

	private MediaBrowserCompat.ConnectionCallback connectionCallbacks;
	private MediaBrowserCompat mediaBrowser;

	private MediaControllerCompat mediaController;
	private TracksAdapter tracksAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracks);

		final RecyclerView tracksList = findViewById(R.id.tracks_list);
		tracksList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
		tracksList.setLayoutManager(new LinearLayoutManager(this));

		tracksAdapter = new TracksAdapter() {
			@Override
			public void onItemClick(String mediaId) {
				if (mediaController != null) {
					mediaController.getTransportControls().playFromMediaId(mediaId, null);
				}
			}
		};
		
		tracksList.setAdapter(tracksAdapter);

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
					mediaController = new MediaControllerCompat(TracksActivity.this, token);
					loadTracks();
				} catch (RemoteException e) {}
			}

			@Override
			public void onConnectionSuspended() {

			}

			@Override
			public void onConnectionFailed() {

			}
		};

		mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaPlaybackService.class), connectionCallbacks, null);
		mediaBrowser.connect();
	}

	private void loadTracks() {
		mediaBrowser.subscribe(mediaBrowser.getRoot(), new MediaBrowserCompat.SubscriptionCallback() {
				@Override
				public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
					tracksAdapter.submitList(children);
				}
			});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaBrowser != null && mediaBrowser.isConnected()) {
			mediaBrowser.unsubscribe(mediaBrowser.getRoot());
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
}

abstract class TracksAdapter extends ListAdapter<MediaBrowserCompat.MediaItem, TracksAdapter.ViewHolder> {

	private static final DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem> DiffItemCallback = new DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {

		@Override
		public boolean areItemsTheSame(MediaBrowserCompat.MediaItem p1, MediaBrowserCompat.MediaItem p2) {
			return p1.getMediaId().equals(p2.getMediaId());
		}

		@Override
		public boolean areContentsTheSame(MediaBrowserCompat.MediaItem p1, MediaBrowserCompat.MediaItem p2) {
			final MediaDescriptionCompat d1 = p1.getDescription();
			final MediaDescriptionCompat d2 = p2.getDescription();
			return Objects.equals(d1.getDescription(), d2.getDescription()) &&
				Objects.equals(d1.getSubtitle(), d2.getSubtitle()) &&
				Objects.equals(d1.getMediaId(), d2.getMediaId()) &&
				Objects.equals(d1.getTitle(), d2.getTitle());
		}
	};

	public TracksAdapter() {
		super(DiffItemCallback);
	}
	
	public abstract void onItemClick(String mediaId);

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
		return new ViewHolder(LayoutInflater.from(p1.getContext()).inflate(R.layout.track_item, p1, false));
	}

	@Override
	public void onBindViewHolder(ViewHolder p1, int p2) {
		p1.bind(getItem(p2).getDescription());
	}

	public final class ViewHolder extends RecyclerView.ViewHolder {

		private final TextView trackArtist;
		private final TextView trackTitle;

		private final ImageView trackArt;

		public ViewHolder(View itemView) {
			super(itemView);
			
			trackArtist = itemView.findViewById(R.id.track_artist);
			trackTitle = itemView.findViewById(R.id.track_title);
			trackArt = itemView.findViewById(R.id.track_art);
			
			trackArtist.setSelected(true);
			trackTitle.setSelected(true);
			
			itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View p1) {
						onItemClick(getItem(getAdapterPosition()).getMediaId());
					}
				});
		}

		public void bind(MediaDescriptionCompat item) {

			trackArtist.setText(item.getSubtitle());
			trackTitle.setText(item.getTitle());

			Glide.with(trackArt)
				.load(item.getIconUri())
				.placeholder(R.drawable.baseline_audiotrack_24)
				.error(R.drawable.baseline_audiotrack_24)
				.into(trackArt);
		}
	}
}

