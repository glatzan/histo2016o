package org.histo.action.view;

import org.apache.log4j.Logger;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.util.PDFUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.Getter;
import lombok.Setter;

@Controller
@Scope("session")
@Getter
@Setter
public class ReportViewHandlerAction {
	private static Logger logger = Logger.getLogger("org.histo");
	
	private boolean renderReport;
	
	private boolean taskNoCompleted;
	
	private PDFContainer container;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReportViewHandlerAction for task");
		
		if(task.getDiagnosisCompletionDate() == 0)
			setTaskNoCompleted(true);
		
		PDFUtil.getLastPDFofType(task, DocumentType.DIAGNOSIS_REPORT_COMPLETED);
	}
}


	