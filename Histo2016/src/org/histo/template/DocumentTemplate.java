package org.histo.template;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.util.HistoUtil;
import org.histo.util.PDFGenerator;
import org.histo.util.interfaces.FileHandlerUtil;

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

	public static final DocumentTemplate[] getTemplates(DocumentType... type) {
		return getTemplates(loadTemplates(type), type);
	}

	public static final DocumentTemplate[] getTemplates(DocumentTemplate[] tempaltes, DocumentType... type) {
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

	public static DocumentTemplate[] loadTemplates(DocumentType... types) {

		// TODO move to Database
		Type type = new TypeToken<List<DocumentTemplate>>() {
		}.getType();

		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(type, new TemplateDeserializer<DocumentTemplate>());

		Gson gson = gb.create();

		ArrayList<DocumentTemplate> jsonArray = gson
				.fromJson(FileHandlerUtil.getContentOfFile(GlobalSettings.PRINT_TEMPLATES), type);

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
	}

	public static final DocumentTemplate getDefaultTemplate(DocumentTemplate[] array) {
		return getDefaultTemplate(array, null);
	}

	public static final DocumentTemplate getDefaultTemplate(DocumentTemplate[] array, DocumentType ofType) {
		if(array == null)
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

	@Override
	public void prepareTemplate() {
		if (HistoUtil.isNotNullOrEmpty(getContent()))
			setFileContent(FileHandlerUtil.getContentOfFile(getContent()));

		if (HistoUtil.isNotNullOrEmpty(getContent2()))
			setFile2Content(FileHandlerUtil.getContentOfFile(getContent2()));
	}

	public PDFContainer generatePDF(PDFGenerator generator) {
		return null;
	}

	@Transient
	public DocumentType getDocumentType() {
		return DocumentType.fromString(this.type);
	}

	public void setDocumentType(DocumentType type) {
		this.type = type.name();
	}
}
