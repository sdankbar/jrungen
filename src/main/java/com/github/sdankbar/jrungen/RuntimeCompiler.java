package com.github.sdankbar.jrungen;

import java.io.IOException;
import java.util.Arrays;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class RuntimeCompiler {

	private final JavaCompiler compilerReference = javax.tools.ToolProvider.getSystemJavaCompiler();

	public void compile(final String sourceCode) {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final InmemoryClassFile classOuput = new InmemoryClassFile("Test");

		final StandardJavaFileManager standardManager = compilerReference.getStandardFileManager(collector, null, null);
		final JavaFileManager wrappedManager = createFileManager(standardManager, classOuput);

		final JavaCompiler.CompilationTask task = compilerReference.getTask(null, wrappedManager, collector, null, null,
				getCompilationUnits(sourceCode));

		if (!task.call()) {
			collector.getDiagnostics().forEach(System.out::println);
		}
	}

	private static Iterable<? extends JavaFileObject> getCompilationUnits(final String sourceCode) {
		final InmemorySourceFile stringObject = new InmemorySourceFile("Test", sourceCode);
		return Arrays.asList(stringObject);
	}

	private static JavaFileManager createFileManager(final StandardJavaFileManager fileManager,
			final InmemoryClassFile byteObject) {
		return new ForwardingJavaFileManager<StandardJavaFileManager>(fileManager) {
			@Override
			public JavaFileObject getJavaFileForOutput(final Location location, final String className,
					final JavaFileObject.Kind kind, final FileObject sibling) throws IOException {
				return byteObject;
			}
		};
	}

}
