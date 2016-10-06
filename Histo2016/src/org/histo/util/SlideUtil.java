package org.histo.util;

import java.util.ArrayList;
import java.util.List;

import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.StainingListChooser;

public class SlideUtil {

	/**
	 * Erstellt einen Liste mit Färbungen, die ausgewählt werden können um sie
	 * einem Block hinzuzufügen
	 * 
	 * @param stainingPrototypes
	 * @return
	 */
	public final static ArrayList<StainingListChooser> getStainingListChooser(
			List<StainingPrototype> stainingPrototypes) {
		ArrayList<StainingListChooser> res = new ArrayList<StainingListChooser>();
		for (StainingPrototype staining : stainingPrototypes) {
			res.add(new StainingListChooser(staining));
		}
		return res;
	}

	/**
	 * Überprüft einen Task ob alle Objektträger gefärbt wurden
	 * 
	 * @param task
	 * @return
	 */
	public final static boolean checkIfAtLeastOnSlide(Task task) {
		for (Sample sample : task.getSamples()) {
			if (!checkIfAtLeastOnSlide(sample))
				return false;
		}
		return true;
	}

	/**
	 * Überprüft eine Probe ob alle Objektträger gefärbt wurden
	 * 
	 * @param sample
	 * @return
	 */
	public final static boolean checkIfAtLeastOnSlide(Sample sample) {
		boolean atLeastOneSlide = false;

		for (Block block : sample.getBlocks()) {
			// weiter, wenn block archiviert wurde
			if (block.isArchived())
				continue;

			lone: for (Slide slide : block.getSlides()) {

				// weiter, wenn slide archiviert wurde
				if (slide.isArchived())
					continue;

				atLeastOneSlide = true;
				break lone;
			}
		}

		return atLeastOneSlide;
	}
	
	/**
	 * Checks if all slides are staind and stets the allStainingsPerformed flag
	 * in the task object to true.
	 * 
	 * @param sample
	 */
	public static final boolean checkIfAllSlidesAreStained(Task task) {
		boolean allPerformed = true;
		for (Sample sample : task.getSamples()) {
			// übersprinen, wenn sample archiviert wurde
			if (sample.isArchived())
				continue;

			// nur als completed markieren wenn mindestens eine Färbung
			// vorhanden ist
			if (sample.isStainingPerformed() && SlideUtil.checkIfAtLeastOnSlide(sample))
				sample.setReStainingPhase(true);
			else {
				allPerformed = false;
			}

		}

		if (allPerformed) {
			task.setStainingCompleted(true);
			task.setStainingCompletionDate(System.currentTimeMillis());
		} else
			task.setStainingCompleted(false);

		return allPerformed;
	}

}
