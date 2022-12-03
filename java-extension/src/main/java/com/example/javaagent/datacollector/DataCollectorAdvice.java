package com.example.javaagent.datacollector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.example.javaagent.helper.DCConfigHelper;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.This;

public class DataCollectorAdvice {
	
	@SuppressWarnings("unchecked")
	@Advice.OnMethodExit(suppress = Throwable.class)
	public static void onExit(@Advice.AllArguments Object[] args,@This(optional = true) Object that,@Advice.Return Object ret, @Advice.Origin("#t") String type, @Advice.Origin("#m") String method) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
			
		//put check on ret null TODO
		Logger logger = DCConfigHelper.logger;
		logger.log(Level.INFO,"---------------Process: " + type + " " + method + "---------------");
		JSONObject rules = (JSONObject) ((JSONObject) DCConfigHelper.configJson.get("config")).get(type + ":" + method);
		JSONArray extract = (JSONArray) rules.get("extract");
		String dcName = (String) rules.get("name");
		Meter meter = GlobalOpenTelemetry.meterBuilder("myMetrics").build();
		Iterator<?> iteratorObj = extract.iterator();
    	Class paramOrReturnClass = null;
    	Object temp = null;
    	List<String> items = null;
    	DoubleHistogram doubleHistogram = null;
    	DoubleHistogram doubleHistogramDC = null;
    	Attributes attr = null;
    	AttributesBuilder attrBuilder = Attributes.builder();
    	Map<String, Double> metricMap = new HashMap<String, Double>();
    	Method tempMethod = null;
    	//metric map - to store all metrics from this extractor
    	//attributes - to store all attributes from this extractor
		while (iteratorObj.hasNext()) {
            	JSONObject o = (JSONObject)iteratorObj.next();
		        //Add tags
            	logger.log(Level.INFO,"---" + o.toJSONString());
            	Object tagValue = null;
				if (o.get("type").equals("param"))
				{
			        int ind = Integer.parseInt(o.get("index").toString());
					paramOrReturnClass = args[ind].getClass();
			        tagValue = args[ind];
				}
				else if (o.get("type").equals("return"))
				{
					paramOrReturnClass = ret.getClass();
					tagValue = ret;
				}
				else if (o.get("type").equals("instance"))
				{
					paramOrReturnClass = that.getClass();
					tagValue = that;
				}

				//object
				if (o.containsKey("getterChain"))	//working on param or return object
				{
					temp = tagValue;
				    items = Arrays.asList(o.get("getterChain").toString().replaceAll("\\(\\)","").split("\\."));
				    for (String m : items)
				    {
				    	if (DCConfigHelper.methodExist(paramOrReturnClass, m))
				    	{
				    		temp = paramOrReturnClass.getDeclaredMethod(m).invoke(temp);
				    	}
				    	else if(DCConfigHelper.variableExist(paramOrReturnClass, m))
				    	{
				    		temp = paramOrReturnClass.getDeclaredField(m).get(temp);
				    	}
				    	
				    }
				    paramOrReturnClass = temp.getClass();
				    tagValue = temp;
				}
				
				//set tags
				if (tagValue != null)
				{
					double val;
					
					logger.log(Level.INFO,"Adding Tag: " + o.get("name").toString() + " " + tagValue.toString());
					attr = Attributes.of(AttributeKey.stringKey(o.get("name").toString()), tagValue.toString());
					Java8BytecodeBridge.currentSpan().setAllAttributes(attr);
					if (o.containsKey("addTagToMetric") && o.get("addTagToMetric").equals(true))
					{
						attrBuilder.put(o.get("name").toString(), tagValue.toString());
					}
					//check if metric required..
					
					if (o.containsKey("createMetric") && o.get("createMetric").equals(true))
					{
			    		
			    		if (DCConfigHelper.isNumeric(tagValue.toString()))
			    		{
				    		val = Double.parseDouble(tagValue.toString());
			    		}
			    		else	//acts as a counter - workaround for aggregate
			    		{
				    		val = 1;
			    		}
			    		metricMap.put(dcName+"."+o.get("name").toString(), val);
					}
				}
				
				
		}
		
		
		for (Entry<String, Double> entry : metricMap.entrySet()) 
		{
			attr = attrBuilder.build();
			logger.log(Level.INFO,"Adding Metric: " + entry.getKey() + " " + entry.getValue() + ", attributes: " + attr.toString());
			doubleHistogram = meter.histogramBuilder(entry.getKey()).build();
			doubleHistogram.record(entry.getValue().doubleValue(),attr);
			//For Splunk analytics
			Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId();
			logger.log(Level.INFO,dcName + "," + entry.getKey()+":"+entry.getValue().doubleValue()+","+attr.toString()+","+Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId());
		}
		//create DC level metric anyways, for each DC, there will be 1 metric
		doubleHistogram = meter.histogramBuilder(dcName).build();
		doubleHistogram.record(1,attr);
		logger.log(Level.INFO,"---------------Done------------------------");
	}
}
