package sune.lib.sil2;

import java.nio.Buffer;

public interface IImageOperation<T extends Buffer, R> {
	
	R execute(IImageContext<T> context);
}