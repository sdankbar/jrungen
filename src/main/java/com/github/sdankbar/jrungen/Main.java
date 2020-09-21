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
import java.util.function.Function;

public class Main {
	public static void main(final String[] args)
			throws CompilationException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException {
		final RuntimeCompiler c = new RuntimeCompiler();

		testBasicCompile(c);

		testFunctionalInterface(c);
	}

	private static void testFunctionalInterface(final RuntimeCompiler c) throws CompilationException {
		final Function<String, Integer> f = c.compileAndConstructFunctionalInterface(String.class, Integer.class,
				"return Integer.valueOf(arg, 16);");

		System.out.print("Converted \"F\" to " + f.apply("F"));
	}

	private static void testBasicCompile(final RuntimeCompiler c) throws CompilationException {
		Class<Runnable> classObj = null;
		final long s = System.currentTimeMillis();

		classObj = c.compile("Test", getSource(Integer.toString(0)));

		final Runnable r = RuntimeCompiler.constructInstance(classObj);
		r.run();

		final long e = System.currentTimeMillis();
		System.out.println("Took " + (e - s) + " milliseconds");
	}

	private static String getSource(final String text) {
		return "public class Test implements Runnable {" + //
				"public void run() {" + //
				"System.out.println(\" " + text + "\");" + //
				"}" + //
				"}";
	}
}
