package com.github.sdankbar.jrungen;

public class Main {
	public static void main(final String[] args) {
		final RuntimeCompiler c = new RuntimeCompiler();
		c.compile(getSource());
	}

	private static String getSource() {
		return "public class Test implements Runnable{" + //
				"public void run(){" + //
				"System.out.println(\"testing\");}}";
	}
}
