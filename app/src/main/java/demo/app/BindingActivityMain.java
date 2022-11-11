package demo.app;

import android.view.*;
import android.widget.*;

public final class BindingActivityMain {

	public final ImageView artImage;

	public final TextView progressText;
	public final TextView durationText;
	public final TextView artistText;
	public final TextView titleText;

	public final SeekBar progressBar;

	public final ImageView prevButton;
	public final ImageView playButton;
	public final ImageView nextButton;

	public final View root;

	public BindingActivityMain(final LayoutInflater inflater) {
		this.root = inflater.inflate(R.layout.activity_main, null, false);
		this.progressText = root.findViewById(R.id.progress_text);
		this.durationText = root.findViewById(R.id.duration_text);
		this.progressBar = root.findViewById(R.id.progress_bar);
		this.artistText = root.findViewById(R.id.artist_text);
		this.prevButton = root.findViewById(R.id.prev_button);
		this.playButton = root.findViewById(R.id.play_button);
		this.nextButton = root.findViewById(R.id.next_button);
		this.titleText = root.findViewById(R.id.title_text);
		this.artImage = root.findViewById(R.id.art_image);
	}
}
