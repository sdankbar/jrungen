package com.github.sdankbar.jrungen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

import javax.tools.SimpleJavaFileObject;

public class InMemoryClassFile extends SimpleJavaFileObject {
	private static URI toURI(final String className) {
		Objects.requireNonNull(className, "className is null");
		final String cleanClassName = className.replaceAll("\\.", "/");
		return URI.create("bytes:///" + cleanClassName);
	}

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	protected InMemoryClassFile(final String name) {
		super(toURI(name), Kind.CLASS);
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		return outputStream;
	}

	public byte[] getBytes() {
		return outputStream.toByteArray();
	}
}
