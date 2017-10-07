package org.histo.model.patient;

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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosisContainer_sequencegenerator", sequenceName = "diagnosisContainer_sequence")
public class DiagnosisContainer implements Parent<Task>, LogAble, PatientRollbackAble, HasID {

	private long id;

	private Task parent;

	/**
	 * List of diagnosis revisions
	 */
	private List<DiagnosisRevision> diagnosisRevisions;

	/**
	 * Selected physician to sign the report
	 */
	private Signature signatureOne;

	/**
	 * Selected consultant to sign the report
	 */
	private Signature signatureTwo;

	/**
	 * Date of the signature
	 */
	private long signatureDate;

	public DiagnosisContainer() {
	}

	public DiagnosisContainer(Task parent) {
		this.parent = parent;
	}

	/******** Transient ********/
	@Transient
	public Date getSignatureDateAsDate() {
		return new Date(signatureDate);
	}

	public void setSignatureDateAsDate(Date signatureDateAsDate) {
		this.signatureDate = signatureDateAsDate.getTime();
	}

	/**
	 * Creates a signature object with the given physician
	 * 
	 * @param physician
	 */
	public void setPhysicianAsSignatureOne(Physician physician) {
		Signature signature = new Signature(physician);
		signature.setRole(physician.getClinicRole());
		setSignatureOne(signature);
	}

	/**
	 * Creates a signature object with the given physician
	 * 
	 * @param physician
	 */
	public void setPhysicianAsSignatureTwo(Physician physician) {
		Signature signature = new Signature(physician);
		signature.setRole(physician.getClinicRole());
		setSignatureTwo(signature);
	}

	/******** Transient ********/

	/******** Interface Parent ********/
	@Id
	@GeneratedValue(generator = "diagnosisContainer_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade=CascadeType.ALL)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("sequenceNumber ASC")
	public List<DiagnosisRevision> getDiagnosisRevisions() {
		return diagnosisRevisions;
	}

	public void setDiagnosisRevisions(List<DiagnosisRevision> diagnosisRevisions) {
		this.diagnosisRevisions = diagnosisRevisions;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Signature getSignatureOne() {
		return signatureOne;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Signature getSignatureTwo() {
		return signatureTwo;
	}

	public long getSignatureDate() {
		return signatureDate;
	}

	public void setSignatureOne(Signature signatureOne) {
		this.signatureOne = signatureOne;
	}

	public void setSignatureTwo(Signature signatureTwo) {
		this.signatureTwo = signatureTwo;
	}

	public void setSignatureDate(long signatureDate) {
		this.signatureDate = signatureDate;
	}

	/******** Interface Parent ********/
	/**
	 * Overwrites method from parent interface
	 */
	@ManyToOne(targetEntity = Task.class)
	public Task getParent() {
		return parent;
	}

	public void setParent(Task parent) {
		this.parent = parent;
	}

	/**
	 * Overwrites method from parent interface
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

	/******** Interface Parent ********/

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return getParent().getLogPath();
	}

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/

}
