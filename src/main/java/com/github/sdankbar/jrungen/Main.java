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
		final Class<Function<String, Integer>> classObj = c.compileFunctionalInterface(String.class, Integer.class,
				"return Integer.valueOf(arg, 16);");

		final Function<String, Integer> f = RuntimeCompiler.constructInstance(classObj);
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
