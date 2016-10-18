package org.histo.model;

import java.util.ArrayList;
import java.util.List;

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
import org.histo.model.util.LogAble;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "signature_sequencegenerator", sequenceName = "signature_sequence")
public class Signature implements LogAble {

	private long id;

	private long version;

	private Physician physician;

	private String role;

	public Signature() {
	}

	public Signature(Physician physician) {
		this.physician = physician;
		this.role = physician.getClinicRole();
	}

	@Id
	@GeneratedValue(generator = "signature_sequencegenerator")
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
	public Physician getPhysician() {
		return physician;
	}

	public void setPhysician(Physician physician) {
		this.physician = physician;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * Generates a list of signatures.
	 * 
	 * @param physicians
	 * @return
	 */
	public static final ArrayList<Signature> getSignatureList(List<Physician> physicians) {
		ArrayList<Signature> signatures = new ArrayList<Signature>(physicians.size());
		for (Physician physician : physicians) {
			Signature signature = new Signature(physician);
			signatures.add(signature);
		}
		return signatures;
	}
}