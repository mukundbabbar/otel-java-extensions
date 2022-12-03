///*
// * Copyright The OpenTelemetry Authors
// * SPDX-License-Identifier: Apache-2.0
// */
//
//package com.example.javaagent.pojotracer;
//
//import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
//
//import static net.bytebuddy.matcher.ElementMatchers.named;
//
//import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
//import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
//import net.bytebuddy.description.type.TypeDescription;
//import net.bytebuddy.matcher.ElementMatcher;
//
//public class POJOInstrument implements TypeInstrumentation {
//
//	public String className;
//	public String methodName;
//
//	public POJOInstrument(String className, String methodName) {
//		super();
//		this.className = className;
//		this.methodName = methodName;
//	}
//
//	//Match class name
//	@Override
//	public ElementMatcher<TypeDescription> typeMatcher() {
////		return AgentElementMatchers.hasSuperType(namedOneOf(this.superClassName));
//	    return named(this.className);
//	}
//
//	//Inject advice to the method
//	@Override
//	public void transform(TypeTransformer typeTransformer) {
//		System.out.println("transform: " + this.className + " " + this.methodName);
////		typeTransformer.applyAdviceToMethod(namedOneOf(this.methodName), this.getClass().getName() + "$ExtractorAdvice");
//		typeTransformer.applyAdviceToMethod(namedOneOf(this.methodName), "com.example.javaagent.pojotracer.POJOAdvice");
//
//	}
//
//}
