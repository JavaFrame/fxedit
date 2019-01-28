package ch.sebi.fxedit.model.syntax;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class SyntaxModel {
	private Map<String, Paint> colors = new HashMap<>();
	
	public SyntaxModel() {
		
	}
	
	public void addRule(String regex, Paint paint) {
		colors.put(regex, paint);
	}
	
	public static SyntaxModel loadSyntaxFile(File f) throws FileNotFoundException, IOException {
		Properties syntax = new Properties();
		syntax.load(new FileInputStream(f));
		
		SyntaxModel model = new SyntaxModel();
		for(Object keyObj : syntax.keySet()) {
			String key = (String) keyObj;
			model.addRule(key, getColor(key));
		}
		return model;
	}
	
	public static Color getColor(String color) {
		if(color.startsWith("#")) {
			if(color.length() != 7) {
				throw new IllegalArgumentException("couldn't parse color \"" + color + "\"");
			}
			int r = Integer.parseInt(color.substring(1, 3), 16);
			int g = Integer.parseInt(color.substring(3, 5), 16);
			int b = Integer.parseInt(color.substring(5, 7), 16);
			return new Color((double) r, (double) g, (double) b, 1d);
		}
		return null;
	}
}
