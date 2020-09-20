package com.github.sdankbar.jrungen;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class RuntimeCompiler {

	private final JavaCompiler compilerReference = javax.tools.ToolProvider.getSystemJavaCompiler();

	public void compile(final String className, final String sourceCode) throws CompilationException {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final InMemoryClassFile classOuput = new InMemoryClassFile(className);
		final StandardJavaFileManager standardFileManager = compilerReference.getStandardFileManager(collector, null,
				null);
		final JavaFileManager wrappedManager = new InMemoryFileManager(standardFileManager, classOuput);

		final JavaCompiler.CompilationTask task = compilerReference.getTask(null, wrappedManager, collector, null, null,
				getCompilationUnits(className, sourceCode));

		if (!task.call()) {
			compilationError(collector);
		}

		// Load the in memory bytecode as a Class.
		final ClassLoader inMemoryClassLoader = new InMemoryClassLoader(classOuput);
		Class<Runnable> test;
		try {
			test = (Class<Runnable>) inMemoryClassLoader.loadClass(className);
			final Runnable iTest = test.newInstance();

			wrappedManager.close();

			iTest.run();
		} catch (final ClassNotFoundException excp) {
			excp.printStackTrace();
		} catch (final InstantiationException excp) {
			excp.printStackTrace();
		} catch (final IllegalAccessException excp) {
			excp.printStackTrace();
		} catch (final IOException excp) {
			excp.printStackTrace();
		}
	}

	private void compilationError(final DiagnosticCollector<JavaFileObject> collector) throws CompilationException {
		final String errorMsg = collector.getDiagnostics().stream().map(Diagnostic::toString)
				.collect(Collectors.joining("\n"));
		throw new CompilationException(errorMsg);
	}

	private static Iterable<? extends JavaFileObject> getCompilationUnits(final String className,
			final String sourceCode) {
		final InMemorySourceFile stringObject = new InMemorySourceFile(className, sourceCode);
		return Arrays.asList(stringObject);
	}

}
