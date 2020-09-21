/**
 * The MIT License
 * Copyright © 2020 Stephen Dankbar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

	public <T, R> Function<T, R> compileAndConstructFunctionalInterface(final Class<T> argType,
			final Class<R> returnType, final String body) throws CompilationException {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final String className = "Func" + UUID.randomUUID().toString().replace("-", "");

		final InMemoryClassFile classOuput = new InMemoryClassFile(className);
		final StandardJavaFileManager standardFileManager = compilerReference.getStandardFileManager(collector, null,
				null);

		final String sourceCode = getFunctionalSourceCode(className, argType, returnType, body);
		try (final JavaFileManager wrappedManager = new InMemoryFileManager(standardFileManager, classOuput)) {
			compileSynchronous(className, sourceCode, collector, wrappedManager);

			// Load the in memory bytecode as a Class.
			return constructInstance(loadClass(className, classOuput));
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
