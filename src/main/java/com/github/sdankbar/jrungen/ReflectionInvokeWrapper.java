/**
 * The MIT License
 * Copyright © 2020 Stephen Dankbar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

	public void forceCompilation() {
		try {
			func = compileFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new InvokationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public R invoke(final T obj, final Object[] args) {
		if (func != null) {
			return func.apply(obj, args);
		} else {
			if (compileFuture.isDone()) {
				try {
					func = compileFuture.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new InvokationException(e);
				}
			}

			try {
				return (R) method.invoke(obj, args);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new InvokationException(e);
			}
		}
	}

}
