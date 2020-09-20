package com.github.sdankbar.jrungen;

import java.io.IOException;

public class Main {
	public static void main(final String[] args) throws IOException {
		final RuntimeCompiler c = new RuntimeCompiler();
		c.compile("Test", getSource());
	}

	private static String getSource() {
		return "public class Test implements Runnable{" + //
				"public void run(){" + //
				"System.out.println(\"Hello World\");}}";
	}
}
