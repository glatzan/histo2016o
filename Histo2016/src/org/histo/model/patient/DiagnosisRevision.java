package org.histo.model.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosisRevision_sequencegenerator", sequenceName = "diagnosisRevision_sequence")
@Getter
@Setter
public class DiagnosisRevision
		implements Parent<DiagnosisContainer>, DeleteAble, LogAble, PatientRollbackAble<DiagnosisContainer>, HasID {

	@Id
	@GeneratedValue(generator = "diagnosisRevision_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	/**
	 * Name of this revision
	 */
	@Column
	private String name;

	/**
	 * Version
	 */
	@Version
	private long version;

	/**
	 * Parent of the Diagnosis
	 */
	@ManyToOne
	private DiagnosisContainer parent;

	/**
	 * Number of the revision in the revision sequence
	 */
	@Column
	private int sequenceNumber;

	/**
	 * Type of the revison @see {@link DiagnosisRevisionType}
	 */
	@Enumerated(EnumType.STRING)
	private DiagnosisRevisionType type;

	/**
	 * Date of diagnosis creation.
	 */
	@Column
	private long creationDate;

	/**
	 * Date of diagnosis finalization.
	 */
	@Column
	private long compleationDate;

	/**
	 * 
	 */
	@Column
	private boolean reDiagnosis;

	/**
	 * All diagnoses
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("sample.id ASC")
	private List<Diagnosis> diagnoses = new ArrayList<>();

	/**
	 * Text containing the histological record for all samples.
	 */
	@Column(columnDefinition = "text")
	private String text = "";

	/**
	 * Standardt consutructor
	 */
	public DiagnosisRevision() {
	}

	public DiagnosisRevision(DiagnosisContainer parent, DiagnosisRevisionType type) {
		this.parent = parent;
		this.type = type;
	}

	/********************************************************
	 * Transient
	 ********************************************************/

	@Transient
	public Diagnosis getLastRelevantDiagnosis() {
		return getDiagnoses().get(getDiagnoses().size() - 1);
	}

	/**
	 * Returns true if a diagnosis is marked as malign.
	 * 
	 * @return
	 */
	@Transient
	public boolean isMalign() {
		return diagnoses.stream().anyMatch(p -> p.isMalign());
	}

	@Override
	public String toString() {
		return "Diagnosis-Revision: " + getName() + (getId() != 0 ? ", ID: " + getId() : "");
	}


	/********************************************************
	 * Interface Parent
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/**
	 * Returns the parent task
	 */
	@Override
	@Transient
	public Task getTask() {
		return getParent().getTask();
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/
	/**
	 * Overwrites Interface ArchiveAble
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "";
	}

	/**
	 * Overwrites Interface ArchiveAble
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return null;
	}

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/
}
