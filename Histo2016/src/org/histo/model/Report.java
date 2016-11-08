package org.histo.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "signatureContainer_sequencegenerator", sequenceName = "signatureContainer_sequence")
public class Report {

	private long id;

	private long version;

	private long signatureDate;

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

	public Report() {
		physicianToSign = new Signature();
		consultantToSign = new Signature();
	}

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

	/**
	 * Updates the physician and the consultant of the report
	 * @param physician
	 * @param consultant
	 */
	public void updatePhysiciansToSign(Physician physician, Physician consultant){
		getPhysicianToSign().updateSignature(physician);
		getConsultantToSign().updateSignature(consultant);
	}
}
