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
		logger.log(Level.INFO,"---------------Invocation: " + type + " " + method + "---------------");
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
    	Attributes attr,attrSpans = null;
    	AttributesBuilder attrBuilder = Attributes.builder();
    	AttributesBuilder attrBuilderSpan = Attributes.builder();
    	Map<String, Double> metricMap = new HashMap<String, Double>();
    	Method tempMethod = null;
    	//metric map - to store all metrics from this extractor
    	//attributes - to store all attributes from this extractor
    	
    	//add default attributes
    	attrBuilder.put("source", "otel-extension");
		attrBuilder.put("traceId", Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId());
		attrBuilderSpan.put("source", "otel-extension");
		attrBuilderSpan.put("traceId", Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId());
		while (iteratorObj.hasNext()) {
            	JSONObject o = (JSONObject)iteratorObj.next();
		        //Add tags
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

				//process getter chain if configured
				if (o.containsKey("getterChain"))	
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
					attrBuilderSpan.put(o.get("name").toString(), tagValue.toString());
					
					//add tags to metrics
					if (o.containsKey("addTagToMetric") && o.get("addTagToMetric").equals(true))
					{
						attrBuilder.put(o.get("name").toString(), tagValue.toString());
					}
					
					//check if metric required from any of the extracted object
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
		
		//flush tags to span
		attrSpans = attrBuilderSpan.build();
		Java8BytecodeBridge.currentSpan().setAllAttributes(attrSpans);
		logger.log(Level.INFO,attrSpans.toString());
		
		//flush metrics
		attr = attrBuilder.build();
		for (Entry<String, Double> entry : metricMap.entrySet()) 
		{
			
			logger.log(Level.INFO,"Adding Metric: " + entry.getKey() + " " + entry.getValue() + ", attributes: " + attr.toString());
			doubleHistogram = meter.histogramBuilder(entry.getKey()).build();
			doubleHistogram.record(entry.getValue().doubleValue(),attr);
			//For Splunk analytics/logs - Probably need specific logger
			logger.log(Level.INFO,dcName + "," + entry.getKey()+":"+entry.getValue().doubleValue()+","+attr.toString());
		}

		//test span event
//		Java8BytecodeBridge.currentSpan().addEvent("event."+dcName, attr);
		//create DC level metric anyways, for each DC, there will be 1 metric to send to HEC as event
		meter.counterBuilder("ext."+dcName+".invocation").build().add(1, attr);
		logger.log(Level.INFO,"ext."+dcName+".invocation"+","+attr.toString());
		logger.log(Level.INFO,"---------------Done------------------------");
	}
}
