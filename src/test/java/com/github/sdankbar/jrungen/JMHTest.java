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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import com.github.sdankbar.jrungen.helper.InvokeObject;

/**
 * Performance benchmarks.
 */
public class JMHTest {

	/**
	 * Shared state.
	 */
	@State(Scope.Thread)
	public static class BenchmarkState {
		InvokeObject obj = new InvokeObject();
		Function<InvokeObject, Long> func;
		Method reflectMethod;

		/**
		 * Sets up shared state.
		 *
		 * @throws SecurityException
		 * @throws NoSuchMethodException
		 */
		@Setup(Level.Trial)
		public void setup() throws NoSuchMethodException, SecurityException {
			final RuntimeCompiler c = new RuntimeCompiler();
			try {
				func = c.compileAndConstructFunctionalInterface(InvokeObject.class, Long.class, "return arg.call();");
			} catch (final CompilationException e) {
				e.printStackTrace();
			}

			reflectMethod = InvokeObject.class.getMethod("call");
		}
	}

	/**
	 * @param state
	 * @param bh
	 */
	@Benchmark
	public void benchmark_generatedCode(final BenchmarkState state, final Blackhole bh) {
		bh.consume(state.func.apply(state.obj));
	}

	/**
	 * @param state
	 * @param bh
	 */
	@Benchmark
	public void benchmark_native(final BenchmarkState state, final Blackhole bh) {
		bh.consume(state.obj.call());
	}

	/**
	 * @param state
	 * @param bh
	 */
	@Benchmark
	public void benchmark_reflect(final BenchmarkState state, final Blackhole bh) {
		try {
			bh.consume(state.reflectMethod.invoke(state.obj));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @throws RunnerException
	 */
	@Test
	public void runBenchmarks() throws RunnerException {
		final Options options = new OptionsBuilder().include(JMHTest.class.getName() + ".*").mode(Mode.Throughput)
				.timeUnit(TimeUnit.MICROSECONDS).warmupTime(TimeValue.seconds(1)).warmupIterations(5).threads(1)
				.measurementIterations(5).forks(1).shouldFailOnError(false).shouldDoGC(true).build();

		new Runner(options).run();
	}

}
