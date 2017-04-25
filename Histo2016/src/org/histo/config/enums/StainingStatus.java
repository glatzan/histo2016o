package org.histo.config.enums;

/**
 * This status determines if a staining , a restaining is needed or if the
 * staining process has been finished.
 * 
 * @author andi
 *
 */
public enum StainingStatus {
	PERFORMED(1), STAY_IN_PHASE(2), STAINING_NEEDED(3), RE_STAINING_NEEDED(4);

	private final int level;

	StainingStatus(int level) {
		this.level = level;
	}

	public static final StainingStatus getStainingStatusByLevel(int level) {
		if (level == StainingStatus.PERFORMED.getLevel())
			return StainingStatus.PERFORMED;
		
		if (level == StainingStatus.STAINING_NEEDED.getLevel())
			return StainingStatus.STAINING_NEEDED;
		
		if (level == DiagnosisStatus.STAY_IN_PHASE.getLevel())
			return StainingStatus.STAY_IN_PHASE;
		
		if (level == StainingStatus.RE_STAINING_NEEDED.getLevel())
			return StainingStatus.RE_STAINING_NEEDED;		
		
		return null; 
	}

	public int getLevel() {
		return level;
	}
}
