/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent.servlet;

import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

import java.util.Enumeration;

import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class ServletInstrument implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return AgentElementMatchers.hasSuperType(
        namedOneOf("javax.servlet.Filter", "javax.servlet.http.HttpServlet"));
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    typeTransformer.applyAdviceToMethod(
        namedOneOf("doFilter", "service")
            .and(
                ElementMatchers.takesArgument(
                    0, ElementMatchers.named("javax.servlet.ServletRequest")))
            .and(
                ElementMatchers.takesArgument(
                    1, ElementMatchers.named("javax.servlet.ServletResponse")))
            .and(ElementMatchers.isPublic()),
        this.getClass().getName() + "$DemoServlet3Advice");
  }

  @SuppressWarnings("unused")
  public static class DemoServlet3Advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.Argument(value = 1) ServletRequest request) {
      if (!(request instanceof HttpServletRequest)) {
        return;
      }

      HttpServletRequest httpServletRequest = (HttpServletRequest) request;
      //Add all headers as tags
      
      Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
      while (headerNames.hasMoreElements()) {
          String key = (String) headerNames.nextElement();
          String value = httpServletRequest.getHeader(key);
          System.out.println(key + " " + value);
          Java8BytecodeBridge.currentSpan().setAttribute(key, value);
      }
    }
  }
}