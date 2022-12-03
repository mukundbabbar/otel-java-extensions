/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent.datacollector;

import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

import java.util.logging.Level;

import com.example.javaagent.helper.DCConfigHelper;
import com.google.auto.service.AutoService;

import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class DataCollectorInstrument implements TypeInstrumentation {

	public String className;
	public String methodName;
	
	public DataCollectorInstrument(String className, String methodName) {
		super();
		this.className = className;
		this.methodName = methodName;
	}

	//Match class name
	@Override
	public ElementMatcher<TypeDescription> typeMatcher() {
	    return named(this.className);
	}

	//Inject advice to the method
	@Override
	public void transform(TypeTransformer typeTransformer) {
		DCConfigHelper.logger.log(Level.INFO,"transform: " + this.className + " " + this.methodName);
		typeTransformer.applyAdviceToMethod(namedOneOf(this.methodName), "com.example.javaagent.datacollector.DataCollectorAdvice");

	}
	
}
