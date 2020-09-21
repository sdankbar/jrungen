package com.github.sdankbar.jrungen;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class RuntimeCompiler {

	@SuppressWarnings("unchecked")
	private static <T> Class<T> loadClass(final String className, final InMemoryClassFile classOuput)
			throws ClassNotFoundException {
		final ClassLoader inMemoryClassLoader = new InMemoryClassLoader(classOuput);
		return (Class<T>) inMemoryClassLoader.loadClass(className);
	}

	private static void compilationError(final DiagnosticCollector<JavaFileObject> collector)
			throws CompilationException {
		final String errorMsg = collector.getDiagnostics().stream().map(Diagnostic::toString)
				.collect(Collectors.joining("\n"));
		throw new CompilationException(errorMsg);
	}

	/**
	 * Attempts to use the default, no argument constructor for the class
	 * represented by Class<T> c to construct an instance of that class.
	 *
	 * @param <T> Type of the object to return.
	 * @param c   Class of the class to instantiate. <T> should be an instance of
	 *            the class or an interface implemented by the class.
	 * @return The instantiated object.
	 * @throws CompilationException Thrown if unable to instantiate the object.
	 */
	public static <T> T constructInstance(final Class<T> c) throws CompilationException {
		try {
			return c.getConstructor().newInstance();
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new CompilationException("Error constructing instance of class " + c.getSimpleName(), e);
		}
	}

	private final JavaCompiler compilerReference = javax.tools.ToolProvider.getSystemJavaCompiler();

	public <T> Class<T> compile(final String className, final String sourceCode) throws CompilationException {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final InMemoryClassFile classOuput = new InMemoryClassFile(className);
		final StandardJavaFileManager standardFileManager = compilerReference.getStandardFileManager(collector, null,
				null);
		try (final JavaFileManager wrappedManager = new InMemoryFileManager(standardFileManager, classOuput)) {
			compileSynchronous(className, sourceCode, collector, wrappedManager);

			// Load the in memory bytecode as a Class.
			return loadClass(className, classOuput);
		} catch (final ClassNotFoundException excp) {
			throw new CompilationException("Error loading compiled class", excp);
		} catch (final IOException excp) {
			throw new CompilationException("Error loading compiled class", excp);
		}
	}

	public <T, R> Class<Function<T, R>> compileFunctionalInterface(final Class<T> argType, final Class<R> returnType,
			final String body) throws CompilationException {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final String className = "Func" + UUID.randomUUID().toString().replace("-", "");

		final InMemoryClassFile classOuput = new InMemoryClassFile(className);
		final StandardJavaFileManager standardFileManager = compilerReference.getStandardFileManager(collector, null,
				null);

		final String sourceCode = getFunctionalSourceCode(className, argType, returnType, body);
		try (final JavaFileManager wrappedManager = new InMemoryFileManager(standardFileManager, classOuput)) {
			compileSynchronous(className, sourceCode, collector, wrappedManager);

			// Load the in memory bytecode as a Class.
			return loadClass(className, classOuput);
		} catch (final ClassNotFoundException excp) {
			throw new CompilationException("Error loading compiled class", excp);
		} catch (final IOException excp) {
			throw new CompilationException("Error loading compiled class", excp);
		}
	}

	private String getFunctionalSourceCode(final String className, final Class<?> argType, final Class<?> returnType,
			final String body) {
		final String argT = argType.getSimpleName();
		final String retT = returnType.getSimpleName();
		return "import java.util.function.Function;\n" + //
				"import " + argType.getName() + ";\n" + //
				"import " + returnType.getName() + ";\n" + //
				"public class " + className + " implements Function<" + argT + "," + retT + "> {\n" + //
				"public " + retT + " apply(" + argT + " arg) {\n" + //
				body + //
				"}\n" + //
				"}\n";
	}

	private void compileSynchronous(final String className, final String sourceCode,
			final DiagnosticCollector<JavaFileObject> collector, final JavaFileManager wrappedManager)
			throws CompilationException {
		final InMemorySourceFile stringObject = new InMemorySourceFile(className, sourceCode);
		final List<InMemorySourceFile> sourceUnits = Arrays.asList(stringObject);

		final JavaCompiler.CompilationTask task = compilerReference.getTask(null, wrappedManager, collector, null, null,
				sourceUnits);

		if (!task.call()) {
			compilationError(collector);
		}
	}

}
