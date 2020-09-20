package com.github.sdankbar.jrungen;

public class Main {
	public static void main(final String[] args)
			throws CompilationException, InstantiationException, IllegalAccessException {
		final RuntimeCompiler c = new RuntimeCompiler();
		final Class<Runnable> classObj = c.compile("Test", getSource());

		final Runnable r = RuntimeCompiler.constructInstance(classObj);
		r.run();
	}

	private static String getSource() {
		return "public class Test implements Runnable {" + //
				"public void run() {" + //
				"System.out.println(\"Hello World\");" + //
				"}" + //
				"}";
	}
}
