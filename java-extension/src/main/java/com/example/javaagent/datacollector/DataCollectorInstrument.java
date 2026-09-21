package com.example.javaagent.datacollector;

import static net.bytebuddy.matcher.ElementMatchers.isBridge;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.util.logging.Level;

import com.example.javaagent.helper.DCConfigHelper;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * One probe per class. Named {@code onMethod} hooks, or every method when a
 * block is instance-only (no {@code onMethod}).
 */
public class DataCollectorInstrument implements TypeInstrumentation {

	public String className;
	public String[] methodNames;
	public boolean allMethods;

	public DataCollectorInstrument(String className, String[] methodNames, boolean allMethods) {
		this.className = className;
		this.methodNames = methodNames;
		this.allMethods = allMethods;
	}

	@Override
	public ElementMatcher<TypeDescription> typeMatcher() {
		return named(this.className);
	}

	@Override
	public void transform(TypeTransformer typeTransformer) {
		DCConfigHelper.logger.log(Level.INFO, "transform: " + this.className + " "
				+ (this.allMethods ? "*" : join(this.methodNames)));
		if (this.allMethods) {
			typeTransformer.applyAdviceToMethod(
					isMethod().and(not(isConstructor())).and(not(isSynthetic())).and(not(isBridge())),
					"com.example.javaagent.datacollector.DataCollectorAdvice");
		} else {
			typeTransformer.applyAdviceToMethod(namedOneOf(this.methodNames),
					"com.example.javaagent.datacollector.DataCollectorAdvice");
		}
	}

	private static String join(String[] names) {
		if (names == null || names.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < names.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(names[i]);
		}
		return sb.toString();
	}
}
