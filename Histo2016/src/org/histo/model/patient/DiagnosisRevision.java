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

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosisRevision_sequencegenerator", sequenceName = "diagnosisRevision_sequence")
public class DiagnosisRevision
		implements Parent<DiagnosisContainer>, DeleteAble, LogAble, PatientRollbackAble, HasID {

	private long id;

	/**
	 * Name of this revision
	 */
	private String name;

	/**
	 * Version
	 */
	private long version;

	/**
	 * Parent of the Diagnosis
	 */
	private DiagnosisContainer parent;

	/**
	 * Number of the revision in the revision sequence
	 */
	private int sequenceNumber;

	/**
	 * Type of the revison @see {@link DiagnosisRevisionType}
	 */
	private DiagnosisRevisionType type;

	/**
	 * Date of diagnosis creation.
	 */
	private long creationDate;

	/**
	 * Date of diagnosis finalization.
	 */
	private long compleationDate;

	/**
	 * True if finalized.
	 */
	private boolean diagnosisCompleted;

	/**
	 * 
	 */
	private boolean reDiagnosis;

	/**
	 * All diagnoses
	 */
	private List<Diagnosis> diagnoses;

	/**
	 * Text containing the histological record for all samples.
	 */
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

	/******************************************************** Transient ********************************************************/

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
		return "ID: " + getId() + " Name: " + getName();
	}
	/******************************************************** Transient ********************************************************/

	@Id
	@GeneratedValue(generator = "diagnosisRevision_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Column(columnDefinition = "text")
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("sample.id ASC")
	public List<Diagnosis> getDiagnoses() {
		if (diagnoses == null)
			diagnoses = new ArrayList<>();
		return diagnoses;
	}

	@Enumerated(EnumType.STRING)
	public DiagnosisRevisionType getType() {
		return type;
	}

	public void setType(DiagnosisRevisionType type) {
		this.type = type;
	}

	public void setDiagnoses(List<Diagnosis> diagnoses) {
		this.diagnoses = diagnoses;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public long getCompleationDate() {
		return compleationDate;
	}

	public boolean isDiagnosisCompleted() {
		return diagnosisCompleted;
	}

	public boolean isReDiagnosis() {
		return reDiagnosis;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public void setCompleationDate(long compleationDate) {
		this.compleationDate = compleationDate;
	}

	public void setDiagnosisCompleted(boolean diagnosisCompleted) {
		this.diagnosisCompleted = diagnosisCompleted;
	}

	public void setReDiagnosis(boolean reDiagnosis) {
		this.reDiagnosis = reDiagnosis;
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne
	public DiagnosisContainer getParent() {
		return parent;
	}

	public void setParent(DiagnosisContainer parent) {
		this.parent = parent;
	}

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

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return getParent().getLogPath() + ", Revision-ID: " + getName() + " (" + getId() + ")";
	}
	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/

}
