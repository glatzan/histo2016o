package org.histo.model.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
import org.histo.model.interfaces.Parent;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosisInfo_sequencegenerator", sequenceName = "diagnosisInfo_sequence")
public class DiagnosisInfo implements Parent<Task> {

	private static Logger logger = Logger.getLogger(DiagnosisInfo.class);

	private long id;

	private Task parent;

	private List<DiagnosisRevision> diagnosisRevisions;

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
	/******** Interface Parent ********/
}
