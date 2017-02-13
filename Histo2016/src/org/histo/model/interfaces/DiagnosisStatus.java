package org.histo.model.interfaces;

import java.util.List;

import org.histo.config.enums.DiagnosisStatusState;

public interface DiagnosisStatus<T extends DiagnosisStatus<?> & ArchivAble> {

	public DiagnosisStatusState getDiagnosisStatus();

	public default DiagnosisStatusState getDiagnosisStatus(List<T> diagnosisList) {
		if (diagnosisList.isEmpty())
			return DiagnosisStatusState.DIAGNOSIS_NEEDED;

		boolean diagnosisNeeded = false;

		for (T listObject : diagnosisList) {

			if (listObject.isArchived())
				continue;

			DiagnosisStatusState diagnosisStatusofChild = listObject.getDiagnosisStatus();

			// continue if no diangosis is needed
			if (diagnosisStatusofChild == DiagnosisStatusState.PERFORMED)
				continue;
			else {
				// check if restaining is needed (restaining > staining) so
				// return that it is needed
				if (diagnosisStatusofChild == DiagnosisStatusState.RE_DIAGNOSIS_NEEDED)
					return DiagnosisStatusState.RE_DIAGNOSIS_NEEDED;
				else
					diagnosisNeeded = true;
			}

		}
		return diagnosisNeeded ? DiagnosisStatusState.DIAGNOSIS_NEEDED : DiagnosisStatusState.PERFORMED;
	}
}
