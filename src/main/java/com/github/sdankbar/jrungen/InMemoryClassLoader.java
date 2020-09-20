package com.github.sdankbar.jrungen;

import java.util.Objects;

public final class InMemoryClassLoader extends ClassLoader {

	private final InMemoryClassFile classOutput;

	protected InMemoryClassLoader(final InMemoryClassFile classOutput) {
		this.classOutput = Objects.requireNonNull(classOutput, "classOutput is null");
	}

	@Override
	public Class<?> findClass(final String name) throws ClassNotFoundException {
		final byte[] bytes = classOutput.getBytes();
		return defineClass(name, bytes, 0, bytes.length);
	}
}