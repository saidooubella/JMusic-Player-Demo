package demo.app;

import android.content.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import java.io.*;

public final class Utils {

	private Utils() { }

	public static Bitmap bitmapFromUri(final ContentResolver resolver, final Uri uri) {
		try {
			try (final ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r")) {
				return BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
			}
		} catch (IOException e) {
			return null;
		}
	}

	public static boolean isOreo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
	}
}

