package org.histo.action;

import java.io.IOException;
import java.io.Serializable;

import org.histo.config.HistoSettings;
import org.histo.config.enums.Display;
import org.histo.dao.TaskDAO;
import org.histo.model.Task;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope(value = "session")
public class TaskHandlerAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1460063099758733063L;

	@Autowired
	HelperHandlerAction helper;
	
	@Autowired
	TaskDAO taskDAO;

	public void preparePrintDialog() {
		helper.showDialog(HistoSettings.DIALOG_PRINT, 1024, 600, false, false, true);
	}

	public void hidePrintDialog() {
		helper.hideDialog(HistoSettings.DIALOG_PRINT);
	}

	public StreamedContent getPDF(Task task, Display display) throws IOException {
		taskDAO.initializeTask(task);
		// FacesContext context = FacesContext.getCurrentInstance();
		//
		// if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
		// // So, we're rendering the HTML. Return a stub StreamedContent so
		// that it will generate right URL.
		// return new DefaultStreamedContent();
		// } else {
		// // So, browser is requesting the media. Return a real StreamedContent
		// with the media bytes.
		// String id =
		// context.getExternalContext().getRequestParameterMap().get("id");
		// Media media = service.find(Long.valueOf(id));
		// return new DefaultStreamedContent(new
		// ByteArrayInputStream(media.getBytes()));
		// }
		return null;
	}
}
//p:importEnum type="javax.faces.application.ProjectStage" var="JsfProjectStages" allSuffix="ALL_ENUM_VALUES" />  