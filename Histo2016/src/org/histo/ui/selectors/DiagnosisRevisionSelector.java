package org.histo.ui.selectors;

import java.util.List;
import java.util.stream.Collectors;

import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisRevisionSelector extends AbstractSelector {

	private DiagnosisRevision diagnosisRevision;

	private DiagnosisRevisionSelector(DiagnosisRevision diagnosisRevision, boolean selected) {
		this.selected = selected;
		this.diagnosisRevision = diagnosisRevision;
	}

	public static List<DiagnosisRevisionSelector> factory(Task task) {
		return task.getDiagnosisRevisions().stream()
				.map(p -> new DiagnosisRevisionSelector(p, false)).collect(Collectors.toList());
	}
}
