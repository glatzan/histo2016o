package org.histo.template;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.ui.documents.DocumentUi;
import org.histo.util.FileUtil;
import org.histo.util.StreamUtils;
import org.histo.util.pdf.PDFGenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;
import lombok.Setter;

//@Entity
@Getter
@Setter
public class DocumentTemplate extends Template {

	private String fileContent;

	/**
	 * Patient
	 */
	protected Patient patient;

	/**
	 * Task
	 */
	protected Task task;

	/**
	 * If true the pdf generator will call onAfterPDFCreation to allow the template
	 * to change or attach other things to the pdf
	 */
	protected boolean afterPDFCreationHook;

	/**
	 * Document copies
	 */
	protected int copies = 1;

	/**
	 * If true the template should be printed in duplex mode
	 */
	protected boolean printDuplex = false;

	/**
	 * Is called if afterPDFCreationHook is set and the pdf was created.
	 * 
	 * @param container
	 * @return
	 */
	public PDFContainer onAfterPDFCreation(PDFContainer container) {
		return container;
	}

	public void initData(Task task) {
		this.patient = task.getPatient();
		this.task = task;
	}

	public void fillTemplate(PDFGenerator generator) {

	}

	public DocumentUi<?> getDocumentUi() {
		return new DocumentUi(this);
	}

	public DocumentType getDocumentType() {
		return DocumentType.fromString(this.type);
	}

	public void setDocumentType(DocumentType type) {
		this.type = type.name();
	}

	public static List<DocumentTemplate> getTemplates(DocumentType... type) {
		return loadTemplates(type);
	}

	public static List<DocumentTemplate> getTemplates(List<DocumentType> type) {
		return loadTemplates(type.toArray(new DocumentType[type.size()]));
	}

	public static List<DocumentTemplate> getTemplates(List<DocumentTemplate> templates, DocumentType... type) {
		logger.debug("Getting templates out of " + templates.size());

		return templates.stream().filter(p -> {
			for (int y = 0; y < type.length; y++)
				if (p.getDocumentType() == type[y])
					return true;
			return false;
		}).collect(Collectors.toList());
	}

	public static <T extends DocumentTemplate> T getTemplateByID(long id) {
		return getTemplateByID(loadTemplates(), id);
	}

	public static <T extends DocumentTemplate> T getTemplateByID(List<DocumentTemplate> templates, long id) {

		logger.debug("Getting templates out of " + templates.size());

		for (DocumentTemplate documentTemplate : templates) {
			if (documentTemplate.getId() == id)
				return (T) documentTemplate;
		}

		return null;
	}

	public static List<DocumentTemplate> loadTemplates(DocumentType... types) {

		// TODO move to Database
		Type type = new TypeToken<List<DocumentTemplate>>() {
		}.getType();

		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(type, new TemplateDeserializer<DocumentTemplate>());

		Gson gson = gb.create();

		ArrayList<DocumentTemplate> jsonArray = gson.fromJson(FileUtil.getContentOfFile(GlobalSettings.PRINT_TEMPLATES),
				type);

		List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();

		// if templates should be constrained
		if (types != null && types.length > 0) {
			for (DocumentTemplate documentTemplate : jsonArray) {
				for (DocumentType documentType : types) {
					if (documentTemplate.getDocumentType() == documentType) {
						result.add(documentTemplate);
					}
				}
			}
		} else
			// returning all templates
			result = jsonArray;

		return result;
	}

	public static <T extends DocumentTemplate> T getDefaultTemplate(List<DocumentTemplate> templates,
			DocumentType ofType) {
		if (templates == null)
			return null;

		return (T) templates.stream().filter(p -> {
			if (p.isDefaultOfType() && ofType == p.getDocumentType())
				return true;
			return false;
		}).collect(StreamUtils.singletonCollector());

	}
}
