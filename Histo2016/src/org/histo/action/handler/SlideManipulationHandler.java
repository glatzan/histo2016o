package org.histo.action.handler;

import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.StainingTableChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SlideManipulationHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private PatientDao patientDao;

	/**
	 * Sets all slides of a task to staining completed/not completed
	 * 
	 * @param task
	 * @param completed
	 * @return
	 * @throws CustomDatabaseInconsistentVersionException 
	 */
	public boolean setStainingCompletedForAllSlides(Task task, boolean completed) throws CustomDatabaseInconsistentVersionException {

		boolean changed = false;

		for (Sample sample : task.getSamples()) {
			for (Block block : sample.getBlocks()) {
				for (Slide slide : block.getSlides()) {
					if (slide.isStainingCompleted() != completed) {
						slide.setStainingCompleted(completed);
						slide.setCompletionDate(System.currentTimeMillis());

						patientDao
								.savePatientAssociatedDataFailSave(slide,
										completed ? "log.patient.task.sample.blok.slide.stainingPerformed"
												: "log.patient.task.sample.blok.slide.stainingNotPerformed",
										slide.toString());
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	/**
	 * Sets all selected slides to chosen/unchosen an returns true is something
	 * was altered.
	 * 
	 * @param stainingTableChoosers
	 * @param completed
	 * @return
	 * @throws CustomDatabaseInconsistentVersionException 
	 */
	public boolean setStainingCompletedForSelectedSlides(List<StainingTableChooser> stainingTableChoosers,
			boolean completed) throws CustomDatabaseInconsistentVersionException {

		boolean changed = false;
		for (StainingTableChooser stainingTableChooser : stainingTableChoosers) {
			if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()
					&& stainingTableChooser.getStaining().isStainingCompleted() != completed) {
				Slide slide = stainingTableChooser.getStaining();
				slide.setStainingCompleted(completed);
				slide.setCompletionDate(System.currentTimeMillis());

				patientDao.savePatientAssociatedDataFailSave(slide,
						completed ? "log.patient.task.sample.blok.slide.stainingPerformed"
								: "log.patient.task.sample.blok.slide.stainingNotPerformed",
						slide.toString());

				changed = true;
			}
		}

		return changed;
	}

}
