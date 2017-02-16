package org.histo.model.transitory.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.DocumentType;
import org.histo.model.interfaces.HasID;
import org.histo.util.HistoUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class PrintTemplate implements HasID {

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

	/**
	 * Creates an array of texTample objects
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final PrintTemplate[] factroy(String jsonFile) {

		Type type = new TypeToken<PrintTemplate[]>() {
		}.getType();

		Gson gson = new Gson();
		PrintTemplate[] result = gson.fromJson(HistoUtil.loadTextFile(jsonFile), type);
		return result;
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
	 * @param tempaltes
	 * @param type
	 * @return
	 */
	public static final PrintTemplate[] getTemplatesByTypes(PrintTemplate[] tempaltes, DocumentType[] type) {
		List<PrintTemplate> result = new ArrayList<PrintTemplate>();

		for (int i = 0; i < tempaltes.length; i++) {
			for (int y = 0; y < type.length; y++) {
				if (tempaltes[y].getDocumentTyp() == type[i]) {
					result.add(tempaltes[y]);
					break;
				}
			}
		}

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

}
