package org.histo.model.interfaces;

import java.util.List;

import org.histo.config.enums.DiagnosisStatus;

public interface DiagnosisInfo<T extends DiagnosisInfo<?> & ArchivAble> {

	public DiagnosisStatus getDiagnosisStatus();

	public default DiagnosisStatus getDiagnosisStatus(List<T> diagnosisList) {
		if (diagnosisList.isEmpty())
			return DiagnosisStatus.DIAGNOSIS_NEEDED;

		boolean diagnosisNeeded = false;

		for (T listObject : diagnosisList) {

			if (listObject.isArchived())
				continue;

			DiagnosisStatus diagnosisStatusofChild = listObject.getDiagnosisStatus();

			// continue if no diangosis is needed
			if (diagnosisStatusofChild == DiagnosisStatus.PERFORMED)
				continue;
			else {
				// check if restaining is needed (restaining > staining) so
				// return that it is needed
				if (diagnosisStatusofChild == DiagnosisStatus.RE_DIAGNOSIS_NEEDED)
					return DiagnosisStatus.RE_DIAGNOSIS_NEEDED;
				else
					diagnosisNeeded = true;
			}

		}
		return diagnosisNeeded ? DiagnosisStatus.DIAGNOSIS_NEEDED : DiagnosisStatus.PERFORMED;
	}
}
