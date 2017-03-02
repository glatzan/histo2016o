package org.histo.config.enums;

/**
 * This status determines if a staining , a restaining is needed or if the
 * staining process has been finished.
 * 
 * @author andi
 *
 */
public enum StainingStatus {
	PERFORMED(1), STAINING_NEEDED(2), RE_STAINING_NEEDED(3);

	private final int level;

	StainingStatus(int level) {
		this.level = level;
	}

	public static final StainingStatus getStainingStatusByLevel(int level) {
		if (level == 1)
			return StainingStatus.PERFORMED;
		if (level == 2)
			return StainingStatus.STAINING_NEEDED;

		return StainingStatus.RE_STAINING_NEEDED;
	}

	public int getLevel() {
		return level;
	}
}
