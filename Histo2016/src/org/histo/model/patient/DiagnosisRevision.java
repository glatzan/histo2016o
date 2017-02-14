package org.histo.model.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
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
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DiagnosisStatusState;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.DiagnosisStatus;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.SaveAble;
import org.histo.util.TaskUtil;

import com.google.gson.annotations.Expose;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosisRevision_sequencegenerator", sequenceName = "diagnosisRevision_sequence")
public class DiagnosisRevision implements DiagnosisStatus, Parent<DiagnosisInfo>, DeleteAble, LogAble, SaveAble {

	private long id;

	/**
	 * Name of this revision
	 */
	private String name;

	private long version;

	/**
	 * Parent of the Diagnosis
	 */
	private DiagnosisInfo parent;

	/**
	 * Number of the revision in the revision sequence
	 */
	private int sequenceNumber;

	/**
	 * True if archived
	 */
	private boolean archived;

	/**
	 * Type of the revison @see {@link DiagnosisRevisionType}
	 */
	private DiagnosisRevisionType type;

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
		for (Diagnosis diagnosis : getDiagnoses()) {
			if (diagnosis.isMalign())
				return true;
		}
		return false;
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
	
	@OneToMany(cascade = CascadeType.ALL , mappedBy = "parent", fetch = FetchType.EAGER)
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

	/********************************************************
	 * Interface DiagnosisStatusState
	 ********************************************************/
	/**
	 * Overwrites the {@link DiagnosisStatusState} interfaces, and returns the
	 * status of the diagnoses.
	 */
	@Transient
	@Override
	public DiagnosisStatusState getDiagnosisStatus() {
		// if (getDiagnoses().isEmpty())
		// return DiagnosisStatusState.DIAGNOSIS_NEEDED;
		//
		// boolean diagnosisNeeded = false;
		//
		// for (Diagnosis diagnosis : getDiagnoses()) {
		//
		// if (diagnosis.isArchived())
		// continue;
		//
		// // continue if no diangosis is needed
		// if (diagnosis.isFinalized())
		// continue;
		// else {
		// // check if restaining is needed (restaining > staining) so
		// // return that it is needed
		// if (diagnosis.isDiagnosisRevision())
		// return DiagnosisStatusState.RE_DIAGNOSIS_NEEDED;
		// else
		// diagnosisNeeded = true;
		// }
		//
		// }

		// // if there is more then one diagnosis a revision was created
		// if (getDiagnoses().size() > 1 && diagnosisNeeded) {
		// return DiagnosisStatusState.RE_DIAGNOSIS_NEEDED;
		// } else {
		// return diagnosisNeeded ? DiagnosisStatusState.DIAGNOSIS_NEEDED :
		// DiagnosisStatusState.PERFORMED;
		// }

		// TODO: rework
		return DiagnosisStatusState.DIAGNOSIS_NEEDED;
	}

	/********************************************************
	 * Interface DiagnosisStatusState
	 ********************************************************/

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne
	public DiagnosisInfo getParent() {
		return parent;
	}

	public void setParent(DiagnosisInfo parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
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
	 * Interface ArchiveAble
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
	 * Interface ArchiveAble
	 ********************************************************/

	/********************************************************
	 * Interface SaveAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return getParent().getLogPath() + ", Revision-ID: " + getName() + " (" + getId() + ")";
	}
	/********************************************************
	 * Interface SaveAble
	 ********************************************************/
}
