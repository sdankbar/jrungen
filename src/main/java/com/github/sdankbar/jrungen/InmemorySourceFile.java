package com.github.sdankbar.jrungen;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import javax.tools.SimpleJavaFileObject;

public class InmemorySourceFile extends SimpleJavaFileObject {
	private static URI toURI(final String className) {
		Objects.requireNonNull(className, "className is null");
		final String cleanClassName = className.replaceAll("\\.", "/");
		return URI.create("string:///" + cleanClassName + Kind.SOURCE.extension);
	}

	private final String sourceCode;

	protected InmemorySourceFile(final String className, final String sourceCode) {
		super(toURI(className), Kind.SOURCE);
		this.sourceCode = Objects.requireNonNull(sourceCode, "sourceCode is null");
	}

	@Override
	public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
		return sourceCode;
	}
}
