package com.example.javaagent.datacollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.javaagent.helper.DCConfigHelper;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * SPI entry. One ByteBuddy probe per class. Each JSON block is independent:
 * return-only, args-only, instance-only, or any mix in one block.
 */
@AutoService(InstrumentationModule.class)
public class DataCollectorInstrumentationModule extends InstrumentationModule {

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
		Map methodsByClass = new HashMap();
		Set allMethodClasses = new HashSet();
		JSONArray instrumentations = DCConfigHelper.instrumentations();
		Iterator<?> iterator = instrumentations.iterator();
		while (iterator.hasNext()) {
			JSONObject inst = (JSONObject) iterator.next();
			String className = (String) inst.get("class");
			if (className == null) {
				continue;
			}
			if (DCConfigHelper.hasOnMethod(inst)) {
				Set methods = (Set) methodsByClass.get(className);
				if (methods == null) {
					methods = new HashSet();
					methodsByClass.put(className, methods);
				}
				methods.add(inst.get("onMethod").toString());
			} else if (DCConfigHelper.hasInstance(inst)) {
				allMethodClasses.add(className);
			}
		}

		Set classes = new HashSet();
		classes.addAll(methodsByClass.keySet());
		classes.addAll(allMethodClasses);

		List<TypeInstrumentation> list = new ArrayList<TypeInstrumentation>();
		Iterator<?> classIterator = classes.iterator();
		while (classIterator.hasNext()) {
			String className = (String) classIterator.next();
			boolean allMethods = allMethodClasses.contains(className);
			Set methodSet = (Set) methodsByClass.get(className);
			String[] methodNames = toArray(methodSet);
			list.add(new DataCollectorInstrument(className, methodNames, allMethods));
		}
		return list;
	}

	private static String[] toArray(Set methodSet) {
		if (methodSet == null || methodSet.isEmpty()) {
			return new String[0];
		}
		String[] names = new String[methodSet.size()];
		Iterator<?> iterator = methodSet.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			names[i++] = iterator.next().toString();
		}
		return names;
	}
}
