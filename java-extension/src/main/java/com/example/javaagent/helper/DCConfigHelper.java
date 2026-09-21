package com.example.javaagent.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;

/**
 * Loads extractor JSON and reads method args, return values, instance fields,
 * and no-arg getters. Injected into the application class loader as a helper.
 *
 * Each instrumentation block is independent. Include only the extracts you want:
 * {@code return} only, {@code args} only, {@code instance} only, or any mix.
 * {@code onMethod} is required only for {@code args} / {@code return}.
 * {@code id} is optional (used as {@code extract.id} on metrics).
 */
public class DCConfigHelper {

	public static JSONObject configJson;
	public static Logger logger = Logger.getLogger("OTEL-Extension");

	private static final ThreadLocal IN_EXTRACT = new ThreadLocal();

	public static String resolveConfigPath() {
		String configFile = System.getProperty("instrumentation.config");
		if (configFile == null || configFile.equals("")) {
			configFile = System.getenv("INSTRUMENTATION_CONFIG");
		}
		return configFile;
	}

	static {
		try {
			configJson = loadConfig(resolveConfigPath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public static JSONObject loadConfig(String configFile) throws IOException, ParseException {
		InputStream inputStream;
		if (configFile == null || configFile.equals("")) {
			System.out.println("--Loading default custom instrumentation config file: " + configFile);
			inputStream = DCConfigHelper.class.getClassLoader().getResourceAsStream("instlocal.json");
		} else {
			System.out.println("--Loading custom instrumentation config file: " + configFile);
			inputStream = new FileInputStream(new File(configFile));
		}
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + System.lineSeparator());
		}
		br.close();
		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(sb.toString());
	}

	public static JSONArray instrumentations() {
		if (configJson == null) {
			return new JSONArray();
		}
		JSONArray instrumentations = (JSONArray) configJson.get("instrumentations");
		return instrumentations == null ? new JSONArray() : instrumentations;
	}

	public static boolean hasOnMethod(JSONObject inst) {
		Object onMethod = inst.get("onMethod");
		return onMethod != null && !onMethod.toString().trim().equals("");
	}

	public static boolean hasInstance(JSONObject inst) {
		return inst.get("instance") != null;
	}

	/** Method-level blocks for this class+method, plus class-level instance blocks. */
	public static List rulesForExit(String className, String methodName) {
		List matched = new ArrayList();
		JSONArray instrumentations = instrumentations();
		Iterator<?> iterator = instrumentations.iterator();
		while (iterator.hasNext()) {
			JSONObject inst = (JSONObject) iterator.next();
			if (!className.equals(inst.get("class"))) {
				continue;
			}
			if (hasOnMethod(inst) && methodName.equals(inst.get("onMethod").toString())) {
				matched.add(inst);
			} else if (!hasOnMethod(inst) && hasInstance(inst)) {
				matched.add(inst);
			}
		}
		return matched;
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

	public static boolean flag(JSONObject o, String key) {
		return o.containsKey(key) && Boolean.TRUE.equals(o.get(key));
	}

	/** No-arg method on {@code c} or a superclass, including private. */
	public static Method findMethod(Class c, String name) {
		Class current = c;
		while (current != null && current != Object.class) {
			try {
				Method method = current.getDeclaredMethod(name);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException e) {
				current = current.getSuperclass();
			} catch (SecurityException e) {
				return null;
			}
		}
		return null;
	}

	/** Field on {@code c} or a superclass, including private / static. */
	public static Field findField(Class c, String name) {
		Class current = c;
		while (current != null && current != Object.class) {
			try {
				Field field = current.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException e) {
				current = current.getSuperclass();
			} catch (SecurityException e) {
				return null;
			}
		}
		return null;
	}

	public static boolean methodExist(Class c, String m) {
		return findMethod(c, m) != null;
	}

	public static boolean variableExist(Class c, String v) {
		return findField(c, v) != null;
	}

	private static String callName(JSONObject step) {
		if (step.get("call") != null) {
			return step.get("call").toString();
		}
		if (step.get("getter") != null) {
			return step.get("getter").toString();
		}
		return null;
	}

	/**
	 * Walk {@code path} steps of getter/{@code call} (no-arg method) or {@code field}.
	 * A single {@code call} / {@code getter} / {@code field} on the item is one step.
	 */
	public static Object resolveValue(Object start, Class startClass, JSONObject extract) throws Exception {
		Object temp = start;
		Class current = startClass;
		List path = pathSteps(extract);
		for (int i = 0; i < path.size(); i++) {
			JSONObject step = (JSONObject) path.get(i);
			String methodName = callName(step);
			if (methodName != null) {
				Method method = findMethod(current, methodName);
				if (method == null) {
					return null;
				}
				Object target = Modifier.isStatic(method.getModifiers()) ? null : temp;
				temp = method.invoke(target);
			} else if (step.get("field") != null) {
				String fieldName = step.get("field").toString();
				Field field = findField(current, fieldName);
				if (field == null) {
					return null;
				}
				Object target = Modifier.isStatic(field.getModifiers()) ? null : temp;
				temp = field.get(target);
			}
			if (temp == null) {
				return null;
			}
			current = temp.getClass();
		}
		return temp;
	}

	private static List pathSteps(JSONObject extract) {
		List steps = new ArrayList();
		if (extract.containsKey("path") && extract.get("path") instanceof JSONArray) {
			JSONArray path = (JSONArray) extract.get("path");
			Iterator<?> iterator = path.iterator();
			while (iterator.hasNext()) {
				steps.add(iterator.next());
			}
			return steps;
		}
		if (callName(extract) != null) {
			JSONObject step = new JSONObject();
			step.put("call", callName(extract));
			steps.add(step);
		} else if (extract.get("field") != null) {
			JSONObject step = new JSONObject();
			step.put("field", extract.get("field"));
			steps.add(step);
		}
		return steps;
	}

	/** Start from a method param, return value, or {@code this}, then optional getter/field path. */
	public static Object extractOne(JSONObject extract, String from, Object[] args, Object that, Object ret,
			String typeName) throws Exception {
		Object tagValue = null;
		Class startClass = null;
		if ("param".equals(from)) {
			if (args == null || extract.get("index") == null) {
				return null;
			}
			int ind = Integer.parseInt(extract.get("index").toString());
			if (ind < 0 || ind >= args.length) {
				return null;
			}
			tagValue = args[ind];
			if (tagValue != null) {
				startClass = tagValue.getClass();
			}
		} else if ("return".equals(from)) {
			tagValue = ret;
			if (tagValue != null) {
				startClass = tagValue.getClass();
			}
		} else if ("instance".equals(from)) {
			tagValue = that;
			if (that != null) {
				startClass = that.getClass();
			} else if (typeName != null) {
				startClass = Class.forName(typeName);
			}
		}
		if (startClass == null) {
			return null;
		}
		if (extract.containsKey("path") || callName(extract) != null || extract.containsKey("field")) {
			return resolveValue(tagValue, startClass, extract);
		}
		return tagValue;
	}

	/** OTel-style name: extract.{attribute} in lowercase dotted form. */
	public static String metricName(String attribute) {
		if (attribute == null) {
			return "extract.value";
		}
		StringBuilder sb = new StringBuilder();
		String s = attribute.trim().toLowerCase();
		boolean lastDot = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
			if (ok) {
				sb.append(c);
				lastDot = false;
			} else if (!lastDot) {
				sb.append('.');
				lastDot = true;
			}
		}
		String n = sb.toString();
		if (n.startsWith(".")) {
			n = n.substring(1);
		}
		if (n.endsWith(".")) {
			n = n.substring(0, n.length() - 1);
		}
		if (n.equals("")) {
			n = "value";
		}
		if (n.startsWith("extract.")) {
			return n;
		}
		return "extract." + n;
	}

	public static void putExtracted(JSONObject extract, Object tagValue,
			AttributesBuilder attrBuilderSpan, AttributesBuilder attrBuilder, Map metricMap) {
		if (tagValue == null || extract.get("attribute") == null) {
			return;
		}
		String attribute = extract.get("attribute").toString();
		attrBuilderSpan.put(attribute, tagValue.toString());
		if (flag(extract, "addTagToMetric")) {
			attrBuilder.put(attribute, tagValue.toString());
		}
		if (flag(extract, "createMetric")) {
			double val = isNumeric(tagValue.toString()) ? Double.parseDouble(tagValue.toString()) : 1;
			metricMap.put(metricName(attribute), val);
		}
	}

	/** Process an {@code args}, {@code return}, or {@code instance} array from one instrumentation. */
	public static void collectSection(JSONObject rules, String section, String from, Object[] args, Object that,
			Object ret, String typeName, AttributesBuilder attrBuilderSpan,
			AttributesBuilder attrBuilder, Map metricMap) {
		Object sectionVal = rules.get(section);
		if (sectionVal == null) {
			return;
		}
		JSONArray items;
		if (sectionVal instanceof JSONArray) {
			items = (JSONArray) sectionVal;
		} else if (sectionVal instanceof JSONObject) {
			items = new JSONArray();
			items.add(sectionVal);
		} else {
			return;
		}
		Iterator<?> iterator = items.iterator();
		while (iterator.hasNext()) {
			Object next = iterator.next();
			if (!(next instanceof JSONObject)) {
				continue;
			}
			JSONObject extract = (JSONObject) next;
			try {
				Object tagValue = extractOne(extract, from, args, that, ret, typeName);
				putExtracted(extract, tagValue, attrBuilderSpan, attrBuilder, metricMap);
			} catch (Exception e) {
				logger.log(Level.WARNING, "extract failed for " + extract.get("attribute") + ": " + e);
			}
		}
	}

	public static boolean enterExtract() {
		if (Boolean.TRUE.equals(IN_EXTRACT.get())) {
			return false;
		}
		IN_EXTRACT.set(Boolean.TRUE);
		return true;
	}

	public static void exitExtract() {
		IN_EXTRACT.set(null);
	}

	/** Called from ByteBuddy advice after the hooked method returns. */
	public static void collectOnExit(Object[] args, Object that, Object ret, String type, String method) {
		if (!enterExtract()) {
			return;
		}
		try {
			List rulesList = rulesForExit(type, method);
			if (rulesList.isEmpty()) {
				return;
			}

			logger.log(Level.INFO, "---------------Invocation: " + type + " " + method + "---------------");
			Meter meter = GlobalOpenTelemetry.getMeter("otel.extension.extract");
			AttributesBuilder attrBuilderSpan = Attributes.builder();
			attrBuilderSpan.put("source", "otel-extension");

			Iterator<?> rulesIterator = rulesList.iterator();
			while (rulesIterator.hasNext()) {
				JSONObject rules = (JSONObject) rulesIterator.next();
				AttributesBuilder metricTags = Attributes.builder();
				metricTags.put("extract.class", type);
				metricTags.put("extract.method", method);
				if (rules.get("id") != null) {
					metricTags.put("extract.id", rules.get("id").toString());
				}
				Map metricMap = new HashMap();
				boolean methodLevel = hasOnMethod(rules);
				if (methodLevel) {
					collectSection(rules, "args", "param", args, that, ret, type, attrBuilderSpan, metricTags,
							metricMap);
					collectSection(rules, "return", "return", args, that, ret, type, attrBuilderSpan, metricTags,
							metricMap);
				}
				if (hasInstance(rules)) {
					collectSection(rules, "instance", "instance", args, that, ret, type, attrBuilderSpan, metricTags,
							metricMap);
				}

				Attributes attr = metricTags.build();
				Iterator metricIterator = metricMap.entrySet().iterator();
				while (metricIterator.hasNext()) {
					Entry entry = (Entry) metricIterator.next();
					logger.log(Level.INFO, "Adding Metric: " + entry.getKey() + " " + entry.getValue()
							+ ", attributes: " + attr.toString());
					DoubleHistogram doubleHistogram = meter.histogramBuilder(entry.getKey().toString()).build();
					doubleHistogram.record(((Double) entry.getValue()).doubleValue(), attr);
				}
			}

			Attributes attrSpans = attrBuilderSpan.build();
			Java8BytecodeBridge.currentSpan().setAllAttributes(attrSpans);
			logger.log(Level.INFO, attrSpans.toString());
			logger.log(Level.INFO, "---------------Done------------------------");
		} catch (Exception e) {
			logger.log(Level.WARNING, "collectOnExit failed: " + e);
		} finally {
			exitExtract();
		}
	}
}
