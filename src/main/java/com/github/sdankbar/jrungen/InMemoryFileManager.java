package com.github.sdankbar.jrungen;

import java.io.IOException;
import java.util.Objects;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

final class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
	private final InMemoryClassFile byteObject;

	InMemoryFileManager(final StandardJavaFileManager fileManager, final InMemoryClassFile byteObject) {
		super(fileManager);
		this.byteObject = Objects.requireNonNull(byteObject, "byteObject is null");
	}

	@Override
	public JavaFileObject getJavaFileForOutput(final Location location, final String className,
			final JavaFileObject.Kind kind, final FileObject sibling) throws IOException {
		return byteObject;
	}
}