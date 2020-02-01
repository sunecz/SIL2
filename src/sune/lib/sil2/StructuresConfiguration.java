package sune.lib.sil2;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class StructuresConfiguration {
	
	private static StructuresConfiguration DEFAULT;
	
	public static final int BACKGROUND = 0x000;
	public static final int FOREGROUND = 0x0ff;
	public static final int BLANK      = 0x100;
	
	public final int valueTrue;
	public final int valueFalse;
	public final BiFunction<Integer, Integer, Boolean> conditionHas;
	public final Function<Integer, Boolean> conditionCan;
	
	public StructuresConfiguration(int valueTrue, int valueFalse,
			BiFunction<Integer, Integer, Boolean> conditionHas,
			Function<Integer, Boolean> conditionCan) {
		this.valueTrue    = valueTrue;
		this.valueFalse   = valueFalse;
		this.conditionHas = conditionHas;
		this.conditionCan = conditionCan;
	}
	
	public static final StructuresConfiguration getDefault() {
		return (DEFAULT == null
					? DEFAULT = new StructuresConfiguration(FOREGROUND, BACKGROUND, (a, b) -> !a.equals(b), (a) -> true)
					: DEFAULT);
	}
}