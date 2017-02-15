package org.histo.model.transitory.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.PrintDocumentTyp;
import org.histo.model.interfaces.HasID;
import org.histo.util.HistoUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class TexTemplate implements HasID {

	@Expose
	private long id;

	@Expose
	private String name;

	@Expose
	private String file;

	@Expose
	private String file2;

	@Expose
	private PrintDocumentTyp documentTyp;

	@Expose
	private boolean defaultDocument;

	/**
	 * Creates an array of texTample objects
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final TexTemplate[] factroy(String jsonFile) {

		Type type = new TypeToken<TexTemplate[]>() {
		}.getType();

		Gson gson = new Gson();
		TexTemplate[] result = gson.fromJson(HistoUtil.loadTextFile(jsonFile), type);
		return result;
	}

	/**
	 * Returns templates matching the given types
	 * 
	 * @param array
	 * @param type
	 * @return
	 */
	public static final TexTemplate[] getTemplatesByType(TexTemplate[] tempaltes, PrintDocumentTyp type) {
		return getTemplatesByTypes(tempaltes, new PrintDocumentTyp[] { type });
	}

	/**
	 * Returns templates matching the given types
	 * @param tempaltes
	 * @param type
	 * @return
	 */
	public static final TexTemplate[] getTemplatesByTypes(TexTemplate[] tempaltes, PrintDocumentTyp[] type) {
		List<TexTemplate> result = new ArrayList<TexTemplate>();

		for (int y = 0; y < tempaltes.length; y++) {
			for (int i = 0; i < type.length; i++) {
				if (type[i] == tempaltes[y].getDocumentTyp()) {
					result.add(tempaltes[y]);
					break;
				}
			}
		}

		TexTemplate[] resultArr = new TexTemplate[result.size()];

		return result.toArray(resultArr);
	}

	/**
	 * Returns the defaultTempalte of an list;
	 * 
	 * @param array
	 * @return
	 */
	public static final TexTemplate getDefaultTemplate(TexTemplate[] array) {
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

	public PrintDocumentTyp getDocumentTyp() {
		return documentTyp;
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

	public void setDocumentTyp(PrintDocumentTyp documentTyp) {
		this.documentTyp = documentTyp;
	}

	public void setDefaultDocument(boolean defaultDocument) {
		this.defaultDocument = defaultDocument;
	}

}
