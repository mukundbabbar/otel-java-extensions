package com.example.javaagent.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DCConfigHelper {
	
	public static JSONObject configJson;
	public static Logger logger = Logger.getLogger("OTEL-Extension");
	static  {
		InputStream inputStream;
		try {
			String configFile = System.getProperty("instrumentation.config");
			//default config file for compile time only
			if(configFile == null || configFile.equals(""))
			{
				System.out.println("--Loading default custom instrumentation config file: " + configFile);
				inputStream = DCConfigHelper.class.getClassLoader().getResourceAsStream("instlocal.json");
			}
			else
			{
				System.out.println("--Loading custom instrumentation config file: " + configFile);
				inputStream = new FileInputStream(new File(configFile));
			}
	        StringBuilder sb = new StringBuilder();
	        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
	        String line;
	        while ((line = br.readLine()) != null) {
	            sb.append(line + System.lineSeparator());
	        }
			JSONParser parser = new JSONParser();  
			configJson = (JSONObject) parser.parse(sb.toString());  

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public static boolean isNumeric(String str) {
	    if (str == null) {
	        return false;
	    }
	    try {
	        Double.parseDouble(str);
	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	public static boolean methodExist(Class c, String m) {
		boolean methodExists = false;
		try {
			c.getDeclaredMethod(m);
		  methodExists = true;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
		}
		return methodExists;
	}
	public static boolean variableExist(Class c, String v) {
		boolean methodExists = false;
		try {
			c.getDeclaredField(v);
		  methodExists = true;
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
		}
		return methodExists;
	}
	


}
