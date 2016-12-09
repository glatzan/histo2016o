package org.histo.model.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import org.histo.config.enums.DiagnosisStatus;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.interfaces.DiagnosisInfo;
import org.histo.model.interfaces.Parent;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "signatureContainer_sequencegenerator", sequenceName = "signatureContainer_sequence")
public class Report implements DiagnosisInfo, Parent<Task> {

	private long id;

	private long version;

	private Task parent;

	private int reportOrder;
	
	/**
	 * Selected physician to sign the report
	 */
	private Signature physicianToSign;

	/**
	 * Selected consultant to sign the report
	 */
	private Signature consultantToSign;

	/**
	 * Text containing the histological record for all samples.
	 */
	private String histologicalRecord = "";

	private long signatureDate;
	
	/**
	 * All diagnoses
	 */
	private List<Diagnosis> diagnoses;

	public Report() {
		physicianToSign = new Signature();
		consultantToSign = new Signature();
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
	@GeneratedValue(generator = "signatureContainer_sequencegenerator")
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

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Signature getPhysicianToSign() {
		return physicianToSign;
	}

	public void setPhysicianToSign(Signature physicianToSign) {
		this.physicianToSign = physicianToSign;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Signature getConsultantToSign() {
		return consultantToSign;
	}

	public void setConsultantToSign(Signature consultantToSign) {
		this.consultantToSign = consultantToSign;
	}

	public long getSignatureDate() {
		return signatureDate;
	}

	public void setSignatureDate(long signatureDate) {
		this.signatureDate = signatureDate;
	}

	@Transient
	public Date getSignatureDateAsDate() {
		return new Date(signatureDate);
	}

	public void setSignatureDateAsDate(Date signatureDateAsDate) {
		this.signatureDate = signatureDateAsDate.getTime();
	}

	@Column(columnDefinition = "text")
	public String getHistologicalRecord() {
		return histologicalRecord;
	}

	public void setHistologicalRecord(String histologicalRecord) {
		this.histologicalRecord = histologicalRecord;
	}

	@OneToMany(cascade = { CascadeType.REFRESH, CascadeType.ALL }, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("diagnosisOrder ASC")
	public List<Diagnosis> getDiagnoses() {
		if (diagnoses == null)
			diagnoses = new ArrayList<>();
		return diagnoses;
	}

	public void setDiagnoses(List<Diagnosis> diagnoses) {
		this.diagnoses = diagnoses;
	}

	/**
	 * Updates the physician and the consultant of the report
	 * 
	 * @param physician
	 * @param consultant
	 */
	public void updatePhysiciansToSign(Physician physician, Physician consultant) {
		getPhysicianToSign().updateSignature(physician);
		getConsultantToSign().updateSignature(consultant);
	}

	/********************************************************
	 * Interface DiagnosisInfo
	 ********************************************************/
	/**
	 * Overwrites the {@link DiagnosisInfo} interfaces, and returns the status
	 * of the diagnoses.
	 */
	@Override
	@Transient
	public DiagnosisStatus getDiagnosisStatus() {
		if (getDiagnoses().isEmpty())
			return DiagnosisStatus.DIAGNOSIS_NEEDED;

		boolean diagnosisNeeded = false;

		for (Diagnosis diagnosis : getDiagnoses()) {

			if (diagnosis.isArchived())
				continue;

			// continue if no diangosis is needed
			if (diagnosis.isFinalized())
				continue;
			else {
				// check if restaining is needed (restaining > staining) so
				// return that it is needed
				if (diagnosis.isDiagnosisRevision())
					return DiagnosisStatus.RE_DIAGNOSIS_NEEDED;
				else
					diagnosisNeeded = true;
			}

		}

		// if there is more then one diagnosis a revision was created
		if (getDiagnoses().size() > 1 && diagnosisNeeded) {
			return DiagnosisStatus.RE_DIAGNOSIS_NEEDED;
		} else {
			return diagnosisNeeded ? DiagnosisStatus.DIAGNOSIS_NEEDED : DiagnosisStatus.PERFORMED;
		}
	}

	/********************************************************
	 * Interface DiagnosisInfo
	 ********************************************************/
	
	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne(targetEntity = Task.class)
	public Task getParent() {
		return parent;
	}

	public void setParent(Task parent) {
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

	/********************************************************
	 * Interface Parent
	 ********************************************************/
}
