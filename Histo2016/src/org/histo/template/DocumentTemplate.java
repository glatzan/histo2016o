package org.histo.template;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.util.HistoUtil;
import org.histo.util.interfaces.FileHandlerUtil;
import org.histo.util.pdf.LazyPDFReturnHandler;
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

	@Transient
	private String fileContent;

	@Transient
	private String file2Content;

	/**
	 * If true the pdf generator will call onAfterPDFCreation to allow the template
	 * to change or attach other things to the pdf
	 */
	protected boolean afterPDFCreationHook;

	public void initializeTempalte(Object... objects) {

	}

	public void fillTemplate(PDFGenerator generator) {

	}

	/**
	 * Is called if afterPDFCreationHook is set and the pdf was created.
	 * 
	 * @param container
	 * @return
	 */
	public PDFContainer onAfterPDFCreation(PDFContainer container) {
		return container;
	}

	@Transient
	public DocumentType getDocumentType() {
		return DocumentType.fromString(this.type);
	}

	public void setDocumentType(DocumentType type) {
		this.type = type.name();
	}

	public static DocumentTemplate[] getTemplates(DocumentType... type) {
		return getTemplates(loadTemplates(type), type);
	}

	public static DocumentTemplate[] getTemplates(DocumentTemplate[] tempaltes, DocumentType... type) {
		List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();

		logger.debug("Getting templates out of " + tempaltes.length);

		for (int i = 0; i < tempaltes.length; i++) {
			for (int y = 0; y < type.length; y++) {
				if (tempaltes[i].getDocumentType() == type[y]) {
					logger.debug("Found Template type " + type + " name: " + tempaltes[i].getName());
					result.add(tempaltes[i]);
					break;
				}
			}
		}

		DocumentTemplate[] resultArr = new DocumentTemplate[result.size()];

		return result.toArray(resultArr);
	}

	public static DocumentTemplate getTemplateByID(long id) {
		return getTemplateByID(loadTemplates(), id);
	}

	public static DocumentTemplate getTemplateByID(DocumentTemplate[] tempaltes, long id) {

		logger.debug("Getting templates out of " + tempaltes.length);

		for (int i = 0; i < tempaltes.length; i++) {
			if (tempaltes[i].getId() == id)
				return tempaltes[i];
		}

		return null;
	}

	public static <T extends DocumentTemplate> T getTemplateByIDC(Class<T> tClass, long id) {
		return getTemplateByIDC(tClass, loadTemplates(), id);
	}

	public static <T extends DocumentTemplate> T getTemplateByIDC(Class<T> tClass, DocumentTemplate[] tempaltes,
			long id) {

		logger.debug("Getting templates out of " + tempaltes.length);

		for (int i = 0; i < tempaltes.length; i++) {
			if (tempaltes[i].getId() == id)
			//	if (tClass.getClass().isAssignableFrom(tempaltes[i].getClass()))
					return (T) tempaltes[i];
		}

		return null;
	}

	public static DocumentTemplate[] loadTemplates(DocumentType... types) {

		// TODO move to Database
		Type type = new TypeToken<List<DocumentTemplate>>() {
		}.getType();

		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(type, new TemplateDeserializer<DocumentTemplate>());

		Gson gson = gb.create();

		ArrayList<DocumentTemplate> jsonArray = gson
				.fromJson(FileHandlerUtil.getContentOfFile(GlobalSettings.PRINT_TEMPLATES), type);

		if (types != null && types.length > 0) {
			List<DocumentTemplate> result = new ArrayList<DocumentTemplate>();

			for (DocumentTemplate documentTemplate : jsonArray) {
				for (DocumentType documentType : types) {
					if (documentTemplate.getDocumentType() == documentType) {
						result.add(documentTemplate);
					}
				}
			}

			DocumentTemplate[] resultArr = new DocumentTemplate[result.size()];

			return result.toArray(resultArr);
		} else {
			DocumentTemplate[] resultArr = new DocumentTemplate[jsonArray.size()];

			return jsonArray.toArray(resultArr);
		}
	}

	public static DocumentTemplate getDefaultTemplate(DocumentTemplate[] array) {
		return getDefaultTemplate(array, null);
	}

	public static DocumentTemplate getDefaultTemplate(DocumentTemplate[] array, DocumentType ofType) {
		if (array == null)
			return null;

		for (int i = 0; i < array.length; i++) {
			if (array[i].isDefaultOfType()) {
				if (ofType == null || array[i].getDocumentType() == ofType) {
					return array[i];
				}
			}
		}
		return null;
	}
}
