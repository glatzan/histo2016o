package org.histo.config.enums;

/**
 * The status of a diagnosis, either performed, diagnosis needed or re diagnosis
 * needed.
 * 
 * @author andi
 *
 */
public enum DiagnosisStatus {
	PERFORMED(1), STAY_IN_PHASE(2), DIAGNOSIS_NEEDED(3), RE_DIAGNOSIS_NEEDED(4);

	private final int level;

	DiagnosisStatus(int level) {
		this.level = level;
	}

	public static final DiagnosisStatus getDiagnosisStatusByLevel(int level) {
		if (level == DiagnosisStatus.PERFORMED.getLevel())
			return DiagnosisStatus.PERFORMED;
		
		if (level == DiagnosisStatus.DIAGNOSIS_NEEDED.getLevel())
			return DiagnosisStatus.DIAGNOSIS_NEEDED;
		
		if (level == DiagnosisStatus.STAY_IN_PHASE.getLevel())
			return DiagnosisStatus.STAY_IN_PHASE;
		
		if (level == DiagnosisStatus.RE_DIAGNOSIS_NEEDED.getLevel())
			return DiagnosisStatus.RE_DIAGNOSIS_NEEDED;
		
		return null;
	}

	public int getLevel() {
		return level;
	}

}
