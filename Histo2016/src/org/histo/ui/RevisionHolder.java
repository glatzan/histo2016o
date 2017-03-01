package org.histo.ui;

import org.histo.model.patient.DiagnosisRevision;

public class RevisionHolder {

	private DiagnosisRevision revision;

	private String name;

	public RevisionHolder(DiagnosisRevision revision, String name) {
		this.revision = revision;
		this.name = name;
	}

	public DiagnosisRevision getRevision() {
		return revision;
	}

	public String getName() {
		return name;
	}

	public void setRevision(DiagnosisRevision revision) {
		this.revision = revision;
	}

	public void setName(String name) {
		this.name = name;
	}

}
