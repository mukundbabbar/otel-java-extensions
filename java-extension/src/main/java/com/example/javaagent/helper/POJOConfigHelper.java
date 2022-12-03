package com.example.javaagent.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class POJOConfigHelper {
	
	public static JSONObject configJson;
	static  {
		InputStream inputStream;
		try {
			String configFile = System.getProperty("pojo.config");
			//default config file for compile time only
			if(configFile == null || configFile.equals(""))
			{
				System.out.println("--Loading default custom instrumentation config file: " + configFile);
				inputStream = POJOConfigHelper.class.getClassLoader().getResourceAsStream("instlocal2.json");
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
	

}
