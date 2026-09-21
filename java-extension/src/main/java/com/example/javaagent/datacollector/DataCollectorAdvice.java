package com.example.javaagent.datacollector;

import com.example.javaagent.helper.DCConfigHelper;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.This;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * After the hooked method returns, extract configured args, return, fields, and getters.
 */
public class DataCollectorAdvice {

	@Advice.OnMethodExit(suppress = Throwable.class)
	public static void onExit(@Advice.AllArguments Object[] args, @This(optional = true) Object that,
			@Advice.Return(typing = Assigner.Typing.DYNAMIC) Object ret, @Advice.Origin("#t") String type,
			@Advice.Origin("#m") String method) {
		DCConfigHelper.collectOnExit(args, that, ret, type, method);
	}
}
