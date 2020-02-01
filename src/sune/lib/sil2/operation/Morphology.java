package sune.lib.sil2.operation;

import static sune.lib.sil2.StructuresConfiguration.BACKGROUND;
import static sune.lib.sil2.StructuresConfiguration.BLANK;
import static sune.lib.sil2.StructuresConfiguration.FOREGROUND;

import java.nio.Buffer;
import java.util.Arrays;

import sune.lib.sil2.IImageContext;
import sune.lib.sil2.IImageOperation;
import sune.lib.sil2.StructuresConfiguration;

public final class Morphology {
	
	// TODO: Update JavaDoc
	
	// Forbid anyone to create an instance of this class
	private Morphology() {
	}
	
	private static final boolean checkStructure(int[] structure) {
		int sqrt = (int) Math.sqrt(structure.length);
		return sqrt * sqrt == structure.length;
	}
	
	public static final class Binarize<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int threshold;
		
		public Binarize(int threshold) {
			this.threshold = threshold;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.applyOperation(new Adjustments.Grayscale<>());
			context.applyOperation(new Adjustments.Threshold<>(threshold));
			return null;
		}
	}
	
	// https://homepages.inf.ed.ac.uk/rbf/HIPR2/hitmiss.htm
	public static final class HitAndMiss<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int[] structure;
		
		public HitAndMiss(int[] structure) {
			if(!checkStructure(structure))
				throw new IllegalArgumentException("Non-square structure");
			this.structure = structure;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.structureConvolute2d(structure, StructuresConfiguration.getDefault());
			return null;
		}
	}
	
	// https://homepages.inf.ed.ac.uk/rbf/HIPR2/thin.htm
	public static final class Thin<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int[] structure;
		
		public Thin(int[] structure) {
			if(!checkStructure(structure))
				throw new IllegalArgumentException("Non-square structure");
			this.structure = structure;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			// thin(img, struct) = img - hitAndMiss(struct)
			// thin(img, struct) = AND(img, NOT(hitAndMiss(struct)))
			context.opSave();
			context.applyOperation(new HitAndMiss<>(structure));
			context.opNot();
			context.opAnd();
			return null;
		}
	}
	
	// https://homepages.inf.ed.ac.uk/rbf/HIPR2/dilate.htm
	public static final class Dilation<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int[] structure;
		private final StructuresConfiguration config;
		
		public Dilation(int size) {
			if((size & 1) == 0)
				throw new IllegalArgumentException("Size must be odd");
			this.structure = new int[size * size];
			Arrays.fill(structure, FOREGROUND);
			config = new StructuresConfiguration(BACKGROUND, FOREGROUND,
					(a, b) -> a.equals(b), (a) -> a.equals(BACKGROUND));
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.structureConvolute2d(structure, config);
			return null;
		}
	}
	
	// https://homepages.inf.ed.ac.uk/rbf/HIPR2/erode.htm
	public static final class Erosion<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int[] structure;
		private final StructuresConfiguration config;
		
		public Erosion(int size) {
			if((size & 1) == 0)
				throw new IllegalArgumentException("Size must be odd");
			this.structure = new int[size * size];
			Arrays.fill(structure, BACKGROUND);
			config = new StructuresConfiguration(FOREGROUND, BACKGROUND,
				(a, b) -> a.equals(b), (a) -> a.equals(FOREGROUND));
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.structureConvolute2d(structure, config);
			return null;
		}
	}
	
	public static final class Skeletonize<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BLANK,
       			FOREGROUND, FOREGROUND, FOREGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			FOREGROUND, FOREGROUND, FOREGROUND,
       			BLANK,      FOREGROUND, BLANK,
       			BACKGROUND, BACKGROUND, BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			FOREGROUND, BLANK,      BACKGROUND,
       			FOREGROUND, FOREGROUND, BACKGROUND,
       			FOREGROUND, BLANK,      BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BLANK,      FOREGROUND,
       			BACKGROUND, FOREGROUND, FOREGROUND,
       			BACKGROUND, BLANK,      FOREGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BLANK,      BACKGROUND, BACKGROUND,
       			FOREGROUND, FOREGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BLANK
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BLANK,      FOREGROUND, BLANK,
       			BACKGROUND, FOREGROUND, FOREGROUND,
       			BACKGROUND, BACKGROUND, BLANK
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BLANK,      FOREGROUND, BLANK,
       			FOREGROUND, FOREGROUND, BACKGROUND,
       			BLANK,      BACKGROUND, BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BLANK,
       			BACKGROUND, FOREGROUND, FOREGROUND,
       			BLANK,      FOREGROUND, BLANK
       		}));
    		return null;
    	}
	}

	public static final class Skeletonize45deg<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.applyOperation(new Thin<>(new int[] {
     			BLANK,      BACKGROUND, BACKGROUND,
     			FOREGROUND, FOREGROUND, BACKGROUND,
     			FOREGROUND, FOREGROUND, BLANK
     		}));
     		context.applyOperation(new Thin<>(new int[] {
     			FOREGROUND, FOREGROUND, BLANK,
     			FOREGROUND, FOREGROUND, BACKGROUND,
     			BLANK,      BACKGROUND, BACKGROUND
     		}));
     		context.applyOperation(new Thin<>(new int[] {
     			BLANK,      FOREGROUND, FOREGROUND,
     			BACKGROUND, FOREGROUND, FOREGROUND,
     			BACKGROUND, BACKGROUND, BLANK
     		}));
     		context.applyOperation(new Thin<>(new int[] {
     			BACKGROUND, BACKGROUND, BLANK,
     			BACKGROUND, FOREGROUND, FOREGROUND,
     			BLANK,      FOREGROUND, FOREGROUND
     		}));
     		return null;
     	}
	}
	
	public static final class Prune<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BACKGROUND,
       			BACKGROUND, BLANK,      BLANK
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BACKGROUND,
       			BLANK,      BLANK,      BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BLANK,
       			BACKGROUND, BACKGROUND, BLANK
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BACKGROUND,
       			BLANK,      BACKGROUND, BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BACKGROUND,
       			BLANK,      BLANK,      BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BLANK,      BLANK,      BACKGROUND,
       			BLANK,      FOREGROUND, BACKGROUND,
       			BACKGROUND, BACKGROUND, BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BLANK,      BLANK,
       			BACKGROUND, FOREGROUND, BLANK,
       			BACKGROUND, BACKGROUND, BACKGROUND
       		}));
			context.applyOperation(new Thin<>(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BLANK,
       			BACKGROUND, BLANK,      BLANK
       		}));
    		return null;
    	}
	}
}