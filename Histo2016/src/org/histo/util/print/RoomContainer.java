package org.histo.util.print;

import java.util.ArrayList;
import java.util.List;

import org.primefaces.json.JSONArray;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomContainer {
	private String printer;
	private String tuerschild;
	private String ip;
	private String bemerkung;
	private int roomid;
	private String aktiv;
	private String aufrufwartekreis;
	private String laufrichtung;
	private String zeitpunkt;
	private String aufruftext;
	
	public static List<RoomContainer> factory(String json){
		
		JSONArray arr = new JSONArray(json);
		
		ArrayList<RoomContainer> container = new ArrayList<RoomContainer>();
		Gson gson = new Gson();

		for (int i = 0; i < arr.length(); i++) {
			container.add(gson.fromJson(arr.getString(i), RoomContainer.class));
		}

		return container;
	}
}