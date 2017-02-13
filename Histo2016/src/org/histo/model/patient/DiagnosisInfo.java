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

	/******** Transient ********/

	/**
	 * Adds a new default revision to the revision list
	 */
	@Transient
	public DiagnosisRevision addDiagnosisRevision() {
		return addDiagnosisRevision(DiagnosisRevisionType.DIAGNOSIS);
	}

	@Transient
	public DiagnosisRevision addDiagnosisRevision(DiagnosisRevisionType type) {
		// init array if not done
		if (getDiagnosisRevisions() == null) {
			setDiagnosisRevisions(new ArrayList<DiagnosisRevision>());
		}

		logger.info("Creating new DiagnosisRevision");
		return new DiagnosisRevision(this, parent.getSamples(), type);
	}

	@Transient
	public DiagnosisRevision removeDiagnosisRevision(DiagnosisRevision revision) {
		logger.info("Creating new DiagnosisRevision");
		getDiagnosisRevisions().remove(revision);
		return revision;
	}

	public void updateDiagnosisRevisionToSampleCount(DiagnosisRevision diagnosisRevision, List<Sample> samples){
		logger.info("Updating diagnosis list with sample list");
		List<Diagnosis> diagnosesInRevision = diagnosisRevision.getDiagnoses();

		List<Sample> samplesToAddDiagnosis = new ArrayList<Sample>(samples);
		ArrayList<Diagnosis> diagnosesToDelete = new ArrayList<Diagnosis>();
		
		outerLoop: for (Diagnosis diagnosis : diagnosesToDelete) {
			// sample already in diagnosisList, removing from to add array
			for (Sample sample : samplesToAddDiagnosis) {
				if(sample.getId() == diagnosis.getSample().getId()){
					samplesToAddDiagnosis.remove(sample);
					logger.trace("Sample found, Removing sample " + sample.getId() + " from list.");
					continue outerLoop;
				}
			}
			logger.trace("Diagnosis has no sample, removing diagnosis " + diagnosis.getId());
			// not found within samples, so sample was deleted, deleting diagnosis as well.
			diagnosesToDelete.add(diagnosis);
			diagnosesInRevision.remove(diagnosis);
		}
		
		// adding new diagnoses if there are new samples
		for (Sample sample : samplesToAddDiagnosis) {
			new Diagnosis(diagnosesInRevision, sample, )));
		}
	}

	/******** Transient ********/

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
	@OrderBy("reportOrder ASC")
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
