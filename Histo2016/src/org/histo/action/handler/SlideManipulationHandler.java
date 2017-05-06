package org.histo.action.handler;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SlideManipulationHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private MainHandlerAction mainHandlerAction;

	public void setStainingCompletedForAllSlidesTo(Task task, boolean completed) {

		for (Sample sample : task.getSamples()) {
			for (Block block : sample.getBlocks()) {
				for (Slide slide : block.getSlides()) {
					if (slide.isStainingCompleted() != completed) {
						slide.setStainingCompleted(completed);

						mainHandlerAction.saveDataChange(slide,
								completed ? "log.patient.task.sample.blok.slide.stainingPerformed"
										: "log.patient.task.sample.blok.slide.stainingNotPerformed",
								String.valueOf(slide.getId()));
					}
				}
			}
		}

	}
}
