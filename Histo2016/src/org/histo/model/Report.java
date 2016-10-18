package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
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

	private Signature signatureLeft;

	private Signature signatureRight;

	/**
	 * Text containing the histological record for all samples.
	 */
	private String histologicalRecord = "";

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

	@OneToOne
	public Signature getSignatureLeft() {
		return signatureLeft;
	}

	public void setSignatureLeft(Signature signatureLeft) {
		this.signatureLeft = signatureLeft;
	}

	@OneToOne
	public Signature getSignatureRight() {
		return signatureRight;
	}

	public void setSignatureRight(Signature signatureRight) {
		this.signatureRight = signatureRight;
	}

	public long getSignatureDate() {
		return signatureDate;
	}

	public void setSignatureDate(long signatureDate) {
		this.signatureDate = signatureDate;
	}

	@Column(columnDefinition = "text")
	public String getHistologicalRecord() {
		return histologicalRecord;
	}

	public void setHistologicalRecord(String histologicalRecord) {
		this.histologicalRecord = histologicalRecord;
	}

}
