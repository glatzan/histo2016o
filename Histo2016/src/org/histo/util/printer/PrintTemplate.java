package org.histo.util.printer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.histo.config.HistoSettings;
import org.histo.config.enums.DocumentType;
import org.histo.model.interfaces.HasID;
import org.histo.util.HistoUtil;
import org.histo.util.interfaces.FileHandlerUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class PrintTemplate implements HasID, FileHandlerUtil {

	@Expose
	private long id;

	@Expose
	private String name;

	@Expose
	private String file;

	@Expose
	private String file2;

	@Expose
	private DocumentType documentType;

	@Expose
	private boolean defaultDocument;

	@Expose
	private boolean doNotSave;

	/**
	 * Creates an array of texTample objects
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final PrintTemplate[] factroy(String jsonFile) {
		return factroy(jsonFile, null);
	}

	/**
	 * Returns a filtered template list
	 * 
	 * @param jsonFile
	 * @param types
	 * @return
	 */
	public static final PrintTemplate[] factroy(String jsonFile, DocumentType[] types) {

		Type type = new TypeToken<PrintTemplate[]>() {
		}.getType();

		Gson gson = new Gson();
		PrintTemplate[] result = gson.fromJson(FileHandlerUtil.getContentOfFile(jsonFile), type);

		if (types != null)
			result = PrintTemplate.getTemplatesByTypes(result, types);

		return result;
	}

	/**
	 * Loads the default template list an returns a subselection containing the
	 * given type
	 * 
	 * @param types
	 * @return
	 */
	public static final PrintTemplate[] getTemplatesByType(DocumentType types) {
		return getTemplatesByTypes(new DocumentType[] { types });
	}

	/**
	 * Loads the default list an returns a subselection containing the given
	 * types
	 * 
	 * @param types
	 * @return
	 */
	public static final PrintTemplate[] getTemplatesByTypes(DocumentType[] types) {
		PrintTemplate[] templates = PrintTemplate.factroy(HistoSettings.TEX_TEMPLATE_JSON);
		return getTemplatesByTypes(templates, types);
	}

	/**
	 * Returns templates matching the given types
	 * 
	 * @param tempaltes
	 * @param type
	 * @return
	 */
	public static final PrintTemplate getTemplateByType(PrintTemplate[] tempaltes, DocumentType type) {
		PrintTemplate[] result = getTemplatesByType(tempaltes, type);
		if (result.length > 0)
			return result[0];
		return null;
	}
	
	/**
	 * Returns templates matching the given types
	 * 
	 * @param array
	 * @param type
	 * @return
	 */
	public static final PrintTemplate[] getTemplatesByType(PrintTemplate[] tempaltes, DocumentType type) {
		return getTemplatesByTypes(tempaltes, new DocumentType[] { type });
	}

	/**
	 * Returns templates matching the given types
	 * 
	 * @param tempaltes
	 * @param type
	 * @return
	 */
	public static final PrintTemplate[] getTemplatesByTypes(PrintTemplate[] tempaltes, DocumentType[] type) {
		List<PrintTemplate> result = new ArrayList<PrintTemplate>();

		logger.debug("Getting templates out of " + tempaltes.length);

		for (int i = 0; i < tempaltes.length; i++) {
			for (int y = 0; y < type.length; y++) {
				logger.debug("Template type one: " + tempaltes[i].getDocumentTyp() + ", template type two " + type[y]);
				if (tempaltes[i].getDocumentTyp() == type[y]) {
					logger.debug("Found Template type " + type + " name: " + tempaltes[i].getName());
					result.add(tempaltes[i]);
					break;
				}
			}
		}

		logger.debug("Found " + result.size() + " templates");

		PrintTemplate[] resultArr = new PrintTemplate[result.size()];

		return result.toArray(resultArr);
	}

	/**
	 * Returns the defaultTempalte of an list;
	 * 
	 * @param array
	 * @return
	 */
	public static final PrintTemplate getDefaultTemplate(PrintTemplate[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].isDefaultDocument())
				return array[i];
		}
		return null;
	}
	
	/**
	 * Returns the defaultTempalte of an list;
	 * 
	 * @param array
	 * @return
	 */
	public static final PrintTemplate getDefaultTemplate(PrintTemplate[] array, DocumentType ofType) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].isDefaultDocument() && array[i].getDocumentTyp() == ofType)
				return array[i];
		}
		return null;
	}
	

	public String getContentOfFile(){
		return FileHandlerUtil.getContentOfFile(getFile());
	}
	
	public String getContentOfFile2(){
		return FileHandlerUtil.getContentOfFile(getFile2());
	}
	
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getFile() {
		return file;
	}

	public String getFile2() {
		return file2;
	}

	public DocumentType getDocumentTyp() {
		return documentType;
	}

	public boolean isDefaultDocument() {
		return defaultDocument;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public void setFile2(String file2) {
		this.file2 = file2;
	}

	public void setDocumentTyp(DocumentType documentType) {
		this.documentType = documentType;
	}

	public void setDefaultDocument(boolean defaultDocument) {
		this.defaultDocument = defaultDocument;
	}

	public boolean isDoNotSave() {
		return doNotSave;
	}

	public void setDoNotSave(boolean doNotSave) {
		this.doNotSave = doNotSave;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
