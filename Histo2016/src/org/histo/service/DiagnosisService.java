package org.histo.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class DiagnosisService {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	/**
	 * Updates all diagnosisRevision of the given diagnosisContainer
	 * 
	 * @param diagnosisContainer
	 * @param samples
	 */
	public void synchronizeDiagnosesAndSamples(Task task) {
		logger.info("Synchronize all diagnoses of task " + task.getTaskID() + " with samples");

		for (DiagnosisRevision revision : task.getDiagnosisContainer().getDiagnosisRevisions()) {
			synchronizeDiagnosesAndSamples(revision, task.getSamples());
		}

		genericDAO.savePatientData(task.getDiagnosisContainer(), "log.patient.task.diagnosisContainer.update",
				task.getDiagnosisContainer());

	}

	/**
	 * Updates a diagnosisRevision with a sample list. Used for adding and
	 * removing samples after initial revision creation.
	 * 
	 * @param diagnosisRevision
	 * @param samples
	 */
	public void synchronizeDiagnosesAndSamples(DiagnosisRevision diagnosisRevision, List<Sample> samples) {
		logger.info("Synchronize diagnosis list with samples");

		List<Diagnosis> diagnosesInRevision = diagnosisRevision.getDiagnoses();

		List<Sample> samplesToAddDiagnosis = new ArrayList<Sample>(samples);

		List<Diagnosis> toRemoveDiagnosis = new ArrayList<Diagnosis>();

		outerLoop: for (Diagnosis diagnosis : diagnosesInRevision) {
			// sample already in diagnosisList, removing from to add array
			for (Sample sample : samplesToAddDiagnosis) {
				if (sample.getId() == diagnosis.getSample().getId()) {
					samplesToAddDiagnosis.remove(sample);
					logger.trace("Sample found, Removing sample " + sample.getId() + " from list.");
					continue outerLoop;
				}
			}
			logger.trace("Diagnosis has no sample, removing diagnosis " + diagnosis.getId());
			// not found within samples, so sample was deleted, deleting
			// diagnosis as well.
			toRemoveDiagnosis.add(diagnosis);
		}

		// removing diagnose if necessary
		for (Diagnosis diagnosis : toRemoveDiagnosis) {
			removeDiagnosis(diagnosis);
		}

		// adding new diagnoses if there are new samples
		for (Sample sample : samplesToAddDiagnosis) {
			logger.trace("Adding new diagnosis for sample " + sample.getId());
			createDiagnosis(diagnosisRevision, sample);
		}

		genericDAO.savePatientData(diagnosisRevision, "log.patient.task.diagnosisContainer.diagnosisRevision.new",
				diagnosisRevision);
	}

	/**
	 * Creates a new diagnosis and adds it to the given diagnosisRevision
	 * 
	 * @param revision
	 * @param sample
	 * @return
	 */
	public Diagnosis createDiagnosis(DiagnosisRevision revision, Sample sample) {
		logger.info("Creating new diagnosis");

		Diagnosis diagnosis = new Diagnosis();
		diagnosis.setSample(sample);
		diagnosis.setParent(revision);

		revision.getDiagnoses().add(diagnosis);

		genericDAO.savePatientData(diagnosis, "log.patient.task.diagnosisContainer.diagnosis.new",
				diagnosis.toString());

		return diagnosis;
	}

	/**
	 * Removes a diagnosis from the parent and deletes it.
	 * 
	 * @param diagnosis
	 * @return
	 */
	public Diagnosis removeDiagnosis(Diagnosis diagnosis) {
		logger.info("Removing diagnosis " + diagnosis.getName());

		diagnosis.setSample(null);

		diagnosis.getParent().getDiagnoses().remove(diagnosis);

		genericDAO.deletePatientData(diagnosis, "log.patient.task.diagnosisContainer.diagnosis.remove",
				diagnosis.toString());

		return diagnosis;
	}

	/**
	 * Creates a diagnosisRevision, adds it to the given DiagnosisContainer and
	 * creates also all needed diagnoses
	 * 
	 * @param parent
	 * @param type
	 * @return
	 */
	public DiagnosisRevision createDiagnosisRevision(DiagnosisContainer parent, DiagnosisRevisionType type) {
		logger.info("Creating new diagnosisRevision");

		DiagnosisRevision diagnosisRevision = new DiagnosisRevision();
		diagnosisRevision.setType(type);
		diagnosisRevision.setCreationDate(System.currentTimeMillis());
		diagnosisRevision.setName(
				TaskUtil.getDiagnosisRevisionName(parent.getDiagnosisRevisions(), diagnosisRevision, resourceBundle));

		addDiagnosisRevision(parent, diagnosisRevision);

		return diagnosisRevision;
	}

	/**
	 * Adds an diagnosis revision to the task
	 * 
	 * @param parent
	 * @param diagnosisRevision
	 */
	public void addDiagnosisRevision(DiagnosisContainer parent, DiagnosisRevision diagnosisRevision) {
		logger.info("Adding diagnosisRevision to task");

		diagnosisRevision.setParent(parent);
		parent.getDiagnosisRevisions().add(diagnosisRevision);
		diagnosisRevision.setSequenceNumber(parent.getDiagnosisRevisions().indexOf(diagnosisRevision));

		// saving to database
		genericDAO.savePatientData(diagnosisRevision, "log.patient.task.diagnosisContainer.diagnosisRevision.new",
				diagnosisRevision.toString());

		// creating a diagnosis for every sample
		for (Sample sample : parent.getParent().getSamples()) {
			createDiagnosis(diagnosisRevision, sample);
		}

	}

	/**
	 * Deleting a DiagnosisRevision and all included diagnoese
	 * 
	 * @param revision
	 * @return
	 */
	public DiagnosisRevision removeDiagnosisRevision(DiagnosisRevision revision) {
		logger.info("Removing diagnosisRevision " + revision.getName());

		revision.getParent().getDiagnosisRevisions().remove(revision);

		genericDAO.deletePatientData(revision, "log.patient.task.diagnosisContainer.diagnosisRevision.delete",
				revision.toString());

		return revision;
	}

}
