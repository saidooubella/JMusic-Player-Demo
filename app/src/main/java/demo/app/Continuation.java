package demo.app;

import java.util.concurrent.*;

public interface Continuation<T> {
	void resume(T value);
}
