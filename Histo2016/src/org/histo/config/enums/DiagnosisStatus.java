package org.histo.config.enums;

/**
 * The status of a diagnosis, either performed, diagnosis needed or re diagnosis
 * needed.
 * 
 * @author andi
 *
 */
public enum DiagnosisStatus {
	PERFORMED(1), DIAGNOSIS_NEEDED(2), RE_DIAGNOSIS_NEEDED(3);

	private final int level;

	DiagnosisStatus(int level) {
		this.level = level;
	}

	public static final DiagnosisStatus getDiagnosisStatusByLevel(int level) {
		if (level == 1)
			return DiagnosisStatus.PERFORMED;
		if (level == 2)
			return DiagnosisStatus.DIAGNOSIS_NEEDED;

		return DiagnosisStatus.RE_DIAGNOSIS_NEEDED;
	}

	public int getLevel() {
		return level;
	}

}
