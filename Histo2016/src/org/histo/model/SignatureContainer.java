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
public class SignatureContainer {

	private long id;

	private long version;

	private long signatureDate;

	private Siganture signatureLeft;

	private Siganture sigantureRight;

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
	public Siganture getSignatureLeft() {
		return signatureLeft;
	}

	public void setSignatureLeft(Siganture signatureLeft) {
		this.signatureLeft = signatureLeft;
	}

	@OneToOne
	public Siganture getSigantureRight() {
		return sigantureRight;
	}

	public void setSigantureRight(Siganture sigantureRight) {
		this.sigantureRight = sigantureRight;
	}

	public long getSignatureDate() {
		return signatureDate;
	}

	public void setSignatureDate(long signatureDate) {
		this.signatureDate = signatureDate;
	}

}
