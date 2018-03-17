package org.histo.adaptors.clinic;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicCase {
	
	private String piz;
	private Date date;
	private String physician;
	private int caseNumber;
	private String location;
	private String value;
	private Date date2;
	
	public static List<ClinicCase> factory(String jsonArray){
		Type listType = new TypeToken<ArrayList<ClinicCase>>(){}.getType();
		List<ClinicCase> list = new Gson().fromJson(jsonArray, listType);
		
		return list;
	}
}
