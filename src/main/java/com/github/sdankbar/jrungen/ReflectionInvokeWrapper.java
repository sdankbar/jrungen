package com.github.sdankbar.jrungen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

public class ReflectionInvokeWrapper<T, R> {

	private static final RuntimeCompiler COMPILER = new RuntimeCompiler();

	private final Method method;
	private final Future<BiFunction<T, Object[], R>> compileFuture;
	private BiFunction<T, Object[], R> func = null;

	public ReflectionInvokeWrapper(final Method m) {
		method = Objects.requireNonNull(m, "m is null");
		compileFuture = COMPILER.compileMethodCallerAsync(m);
	}

	@SuppressWarnings("unchecked")
	public R invoke(final T obj, final Object[] args) {
		if (func != null) {
			return func.apply(obj, args);
		} else {
			if (compileFuture.isDone()) {
				try {
					func = compileFuture.get();
					return func.apply(obj, args);
				} catch (InterruptedException | ExecutionException e) {
					throw new IllegalStateException(e);
				}
			} else {
				try {
					return (R) method.invoke(obj, args);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

}
