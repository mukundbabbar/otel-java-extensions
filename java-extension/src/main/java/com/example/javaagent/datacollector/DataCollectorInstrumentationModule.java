package com.example.javaagent.datacollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import com.example.javaagent.helper.DCConfigHelper;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@AutoService(InstrumentationModule.class)
public class DataCollectorInstrumentationModule extends InstrumentationModule {

	public static JSONObject configJson;
	static  {
		InputStream inputStream;
		try {
			String configFile = System.getProperty("instrumentation.config");
			//default config file for compile time only
			if(configFile == null || configFile.equals(""))
			{
				inputStream = DCConfigHelper.class.getClassLoader().getResourceAsStream("instlocal.json");
			}
			else
			{
				System.out.println("--Custom instrumentation config file: " + configFile);
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
	
	
	
	public DataCollectorInstrumentationModule() {
		super("mb-demo", "method-interceptor");
	}
	
	@Override
	public boolean isHelperClass(String className) {
	  return className.startsWith("com.example.javaagent.helper") 
			  || className.startsWith("org.json.simple");
	}

	@Override
	public List<TypeInstrumentation> typeInstrumentations() {
		// Class,Method
		List<TypeInstrumentation> list = new ArrayList<TypeInstrumentation>();
		
		
		JSONObject config_object = (JSONObject) configJson.get("config");
		for(Iterator iterator = config_object.keySet().iterator(); iterator.hasNext();) {
		    String key = (String) iterator.next();
		    list.add(new DataCollectorInstrument(key.split(":")[0],key.split(":")[1]));
		}
		
		return list;
	}
}
