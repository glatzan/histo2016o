package org.histo.util;

import java.util.ArrayList;
import java.util.List;

import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.ListChooser;

public class SlideUtil {

	/**
	 * Erstellt einen Liste mit Färbungen, die ausgewählt werden können um sie
	 * einem Block hinzuzufügen
	 * 
	 * @param stainingPrototypes
	 * @return
	 */
	public final static ArrayList<ListChooser<StainingPrototype>> getStainingListChooser(
			List<StainingPrototype> stainingPrototypes) {
		ArrayList<ListChooser<StainingPrototype>> res = new ArrayList<ListChooser<StainingPrototype>>();
		for (StainingPrototype staining : stainingPrototypes) {
			res.add(new ListChooser<StainingPrototype>(staining));
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

			lone: for (Slide slide : block.getSlides()) {

				// weiter, wenn slide archiviert wurde

				atLeastOneSlide = true;
				break lone;
			}
		}

		return atLeastOneSlide;
	}

}
