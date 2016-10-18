package org.histo.action;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.histo.config.enums.DiagnosisType;
import org.histo.config.enums.Dialog;
import org.histo.dao.GenericDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.util.ResourceBundle;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class DiagnosisHandlerAction implements Serializable {

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private HelperHandlerAction helper;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private TaskHandlerAction taskHandlerAction;

	private static final long serialVersionUID = -1214161114824263589L;

	private Diagnosis tmpDiagnosis;

	/**
	 * Checks if a diagnosis revision can be created. This in only possible if
	 * all other diagnoses are finalized.
	 * 
	 * @param sample
	 * @return
	 */
	public boolean isDiagonsisRevisionCreationPossible(Sample sample) {
		List<Diagnosis> diagnoses = sample.getDiagnoses();

		if (diagnoses.size() < 1)
			return false;

		for (Diagnosis diagnosis : diagnoses) {
			if (!diagnosis.isFinalized()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an new Diagnosis with the given type. Adds it to the passed
	 * sample and saves the sample in the database.
	 * 
	 * @param sample
	 * @param type
	 */
	public void createDiagnosisFromGui(Sample sample, DiagnosisType type) {
		createDiagnosis(sample, type);
		genericDAO.save(sample.getPatient(), resourceBundle.get("log.patient.save"), sample.getPatient());
	}

	public void createDiagnosis(Sample sample, DiagnosisType type) {
		Diagnosis diagnosis = TaskUtil.createNewDiagnosis(sample, type, resourceBundle);
		genericDAO.save(diagnosis, resourceBundle.get("log.patient.task.sample.diagnosis.new",
				sample.getParent().getTaskID(), sample.getSampleID(), diagnosis.getName()), diagnosis.getPatient());
		// TODO change to sample
		helper.updateRevision(diagnosis);
	}

	/**
	 * Shows a waring dialog before finalizing a diagnosis.
	 */
	public void prepareFinalizeDiagnosisDialog(Diagnosis diagnosis) {
		setTmpDiagnosis(diagnosis);
		mainHandlerAction.showDialog(Dialog.DIAGNOSIS_FINALIZE);
	}

	/**
	 * Hides the waring dialog for finalizing diagnoses
	 */
	public void hideFinalizeDiangosisDialog() {
		setTmpDiagnosis(null);
		mainHandlerAction.hideDialog(Dialog.DIAGNOSIS_FINALIZE);
	}

	/**
	 * Finalizes all diagnoses of the task.
	 */
	public void finalizeDiagnoses(Task task) {
		for (Sample sample : task.getSamples()) {
			for (Diagnosis diagnosis : sample.getDiagnoses()) {
				finalizeDiagnosis(diagnosis);
			}
		}
		task.setDiagnosisCompleted(true);
		task.setDiagnosisCompletionDate(System.currentTimeMillis());
	}

	/**
	 * Finalizes the passed diagnosis.
	 * 
	 * @param diagnosis
	 */
	public void finalizeDiagnosis(Diagnosis diagnosis) {
		diagnosis.setFinalized(true);
		diagnosis.setFinalizedDate(System.currentTimeMillis());

		genericDAO.save(diagnosis, resourceBundle.get("log.patient.diagnosis.finaziled"), diagnosis.getPatient());
		genericDAO.refresh(diagnosis.getPatient());
	}

	/**
	 * Shows a waring dialog before unfinalizing a diagnosis.
	 */
	public void prepareUnfinalizeDiagnosisDialog(Diagnosis diagnosis) {
		setTmpDiagnosis(diagnosis);
		mainHandlerAction.showDialog(Dialog.DIAGNOSIS_UNFINALIZE);
	}

	/**
	 * Hides the waring dialog for unfinalizing diagnoses
	 */
	public void hideUnfinalizeDiangosisDialog() {
		setTmpDiagnosis(null);
		mainHandlerAction.hideDialog(Dialog.DIAGNOSIS_UNFINALIZE);
	}

	/**
	 * Makes a diagnosis editable again.
	 * 
	 * @param diagnosis
	 */
	public void unfinalizeDiagnosis(Diagnosis diagnosis) {
		diagnosis.setFinalized(false);
		diagnosis.setFinalizedDate(0);
		genericDAO.save(diagnosis, resourceBundle.get("log.patient.diagnosis.unfinalize"), diagnosis.getPatient());

		hideUnfinalizeDiangosisDialog();
	}

	public void diagnosisDataChanged(Diagnosis diagnosis) {
		diagnosisDataChanged(diagnosis, null);
	}

	public void diagnosisDataChanged(Diagnosis diagnosis, String detailedInfoResourcesKey,
			Object... detailedInfoParams) {
		String detailedInfoString = "";

		if (detailedInfoResourcesKey != null)
			detailedInfoString = resourceBundle.get(detailedInfoResourcesKey, detailedInfoParams);

		genericDAO.save(diagnosis,
				resourceBundle.get("log.patient.task.sample.diagnosis.changed",
						diagnosis.getParent().getParent().getTaskID(), diagnosis.getParent().getSampleID(),
						detailedInfoString),
				diagnosis.getPatient());
	}

	public void onDiagnosisPrototypeChanged(Diagnosis diagnosis) {
		Task task = diagnosis.getParent().getParent();

		// only setting diagnosis text if one sample and no text has been added
		// jet
		if (task.getSamples().size() == 1 && (task.getReport().getHistologicalRecord() != null
				|| task.getReport().getHistologicalRecord().isEmpty()))
			task.getReport().setHistologicalRecord(diagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());

		genericDAO.save(diagnosis, "hallo", diagnosis.getPatient());
		System.out.println("changed");
	}

	public void prepareCopyHistologicalRecord(Diagnosis tmpDiagnosis) {
		setTmpDiagnosis(tmpDiagnosis);

		// setting diagnosistext if no text is set
		if (tmpDiagnosis.getParent().getParent().getReport().getHistologicalRecord().isEmpty()
				&& tmpDiagnosis.getDiagnosisPrototype() != null) {
			copyHistologicalRecord(tmpDiagnosis);
			return;
		}

		if (tmpDiagnosis.getDiagnosisPrototype() != null)
			mainHandlerAction.showDialog(Dialog.DIAGNOSIS_RECORD_OVERWRITE);

	}

	public void copyHistologicalRecord(Diagnosis tmpDiagnosis) {
		tmpDiagnosis.getParent().getParent().getReport()
				.setHistologicalRecord(tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());
		setTmpDiagnosis(null);

		taskHandlerAction.taskDataChanged(tmpDiagnosis.getParent().getParent(),
				"log.patient.task.dataChange.histologicalRecord",
				tmpDiagnosis.getParent().getParent().getReport().getHistologicalRecord());
		System.out.println("---------!!!!!!!!!");
	}
	/*
	 * log.patient.task.sample.diagnosis.finalized=Diangose finalisiert.
	 * (Auftrag: {0}, Probe: {1}, Diangose: {2})
	 * log.patient.task.sample.diagnosis.changed.diagnosis=Diagnose {0}
	 * log.patient.task.sample.diagnosis.changed.diagnosis.text=Diangose Text
	 * {0} log.patient.task.sample.diagnosis.changed.commentary=Kommentar{0}
	 * log.patient.task.sample.diagnosis.unfinalize=Diagnose wieder freigegeben.
	 * (Auftrag: {0}, Probe: {1}, Diangose: {2})
	 */

	public void print() {
		// PdfReader pdfTemplate;
		// try {
		// pdfTemplate = new
		// PdfReader("Q:\\AUG-T-HISTO\\Formulare\\ergebniss20.pdf");
		//
		// ByteArrayOutputStream out = new ByteArrayOutputStream();
		// PdfStamper stamper = new PdfStamper(pdfTemplate, out);
		//
		// stamper.setFormFlattening(true);
		//
		// stamper.getAcroFields().setField("date", "Daniel Reuter");
		// stamper.close();
		// pdfTemplate.close();
		//
		// FileOutputStream fos = new
		// FileOutputStream("Q:\\AUG-T-HISTO\\Formulare\\ergebnis-test.pdf");
		//
		// fos.write(out.toByteArray());
		//
		// fos.close();
		//
		// } catch (IOException | DocumentException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		System.out.println(resourceBundle.get("log.patient.diagnosis.changed"));

		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			System.out.println(url.getFile());
		}

		ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver(new DefaultResourceLoader());
		Resource[] resources;
		try {
			resources = patternResolver.getResources("classpath*:messages/messages*");
			for (Resource resource : resources) {
				System.out.println(resource.getDescription());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public Diagnosis getTmpDiagnosis() {
		return tmpDiagnosis;
	}

	public void setTmpDiagnosis(Diagnosis tmpDiagnosis) {
		this.tmpDiagnosis = tmpDiagnosis;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
