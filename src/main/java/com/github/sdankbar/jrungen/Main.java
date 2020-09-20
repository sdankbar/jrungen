package com.github.sdankbar.jrungen;

public class Main {
	public static void main(final String[] args) throws CompilationException {
		final RuntimeCompiler c = new RuntimeCompiler();
		c.compile("Test", getSource());
	}

	private static String getSource() {
		return "public class Test implements Runnable{" + //
				"public void run(){" + //
				"System.out.println(\"Hello World2\");}}";
	}
}
