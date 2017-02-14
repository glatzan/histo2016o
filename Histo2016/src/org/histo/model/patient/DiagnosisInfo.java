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

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.model.Signature;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.SaveAble;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosisInfo_sequencegenerator", sequenceName = "diagnosisInfo_sequence")
public class DiagnosisInfo implements Parent<Task>, LogAble, SaveAble {

	private static Logger logger = Logger.getLogger(DiagnosisInfo.class);

	private long id;

	private Task parent;

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

	public DiagnosisInfo() {
	}

	public DiagnosisInfo(Task parent) {
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

	/******** Transient ********/

	/******** Interface Parent ********/
	@Id
	@GeneratedValue(generator = "diagnosisInfo_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("sequenceNumber ASC")
	public List<DiagnosisRevision> getDiagnosisRevisions() {
		return diagnosisRevisions;
	}

	public void setDiagnosisRevisions(List<DiagnosisRevision> diagnosisRevisions) {
		this.diagnosisRevisions = diagnosisRevisions;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Signature getSignatureOne() {
		return signatureOne;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
	 * Interface SaveAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return getParent().getLogPath();
	}
	/********************************************************
	 * Interface SaveAble
	 ********************************************************/
}
