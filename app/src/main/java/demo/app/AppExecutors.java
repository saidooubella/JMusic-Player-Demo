package demo.app;

import android.os.*;
import java.util.concurrent.*;

public final class AppExecutors {

	public final MainExecutor main;
	public final Executor io;

	public AppExecutors() {
		this.io = Executors.newFixedThreadPool(3);
		this.main = new MainExecutor();
	}

	public static final class MainExecutor implements Executor {

		public final Handler handler = new Handler(Looper.getMainLooper());

		@Override
		public void execute(Runnable runnable) {
			handler.post(runnable);
		}
	}
}
