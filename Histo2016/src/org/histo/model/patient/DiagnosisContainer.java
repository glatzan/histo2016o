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

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosisContainer_sequencegenerator", sequenceName = "diagnosisContainer_sequence")
@Getter
@Setter
public class DiagnosisContainer implements Parent<Task>, LogAble, PatientRollbackAble<Task>, HasID {

	@Id
	@GeneratedValue(generator = "diagnosisContainer_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@ManyToOne(targetEntity = Task.class)
	private Task parent;

	/**
	 * List of diagnosis revisions
	 */
	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("sequenceNumber ASC")
	private List<DiagnosisRevision> diagnosisRevisions;

	/**
	 * Selected physician to sign the report
	 */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Signature signatureOne;

	/**
	 * Selected consultant to sign the report
	 */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Signature signatureTwo;

	/**
	 * Date of the signature
	 */
	@Column
	private long signatureDate;

	public DiagnosisContainer() {
	}

	public DiagnosisContainer(Task parent) {
		this.parent = parent;
	}
	

	@Override
	public String toString() {
		return "Diagnosis-Container";
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
		signature.setRole(physician != null ?physician.getClinicRole() : "");
		setSignatureTwo(signature);
	}

	@Transient
	public boolean isMalign() {
		return diagnosisRevisions.stream().anyMatch(p -> p.isMalign());
	}

	/******** Transient ********/


	/******** Interface Parent ********/
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
}
