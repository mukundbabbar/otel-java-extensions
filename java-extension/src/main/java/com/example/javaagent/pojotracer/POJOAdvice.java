//package com.example.javaagent.pojotracer;
//
//import java.lang.reflect.InvocationTargetException;
//
//import io.opentelemetry.api.GlobalOpenTelemetry;
//import io.opentelemetry.api.trace.Span;
//import io.opentelemetry.api.trace.Tracer;
//import net.bytebuddy.asm.Advice;
//import net.bytebuddy.asm.Advice.This;
//
//public class POJOAdvice {
//	
//	@Advice.OnMethodEnter(suppress = Throwable.class)
//	public static void onEnter(@Advice.AllArguments Object[] args, @Advice.Origin("#t") String type, @Advice.Origin("#m") String method,@Advice.This Object thiz) {
//		System.out.println("OnMethodEnter " + " " + type + " " + method);
//		Tracer tracer = GlobalOpenTelemetry
//		        .getTracerProvider().get("instrumentation-library-name", "1.0.0");
//
//		Span span = tracer
//		        .spanBuilder(method) 
//		        .setNoParent()
//		        .startSpan();
//	}
//
//	@Advice.OnMethodExit(suppress = Throwable.class)
//	public static void onExit(@Advice.AllArguments Object[] args,@This(optional = true) Object that,@Advice.Return Object ret, @Advice.Origin("#t") String type, @Advice.Origin("#m") String method) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
//		System.out.println("OnMethodExist " + " " + that + " " + method);
//		Span span = Span.current();
//		span.end();
//		
//		
//	}
//}
