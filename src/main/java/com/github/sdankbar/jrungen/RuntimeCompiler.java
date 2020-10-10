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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeCompiler {

	private static ExecutorService COMPILER_THREADS = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(final Runnable r) {
			final Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			return t;
		}
	});
	private static final Logger log = LoggerFactory.getLogger(RuntimeCompiler.class);

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
			compile(className, sourceCode, collector, wrappedManager);

			// Load the in memory bytecode as a Class.
			return loadClass(className, classOuput);
		} catch (final IOException | ClassNotFoundException excp) {
			throw new CompilationException("Error loading compiled class", excp);
		}
	}

	public <T> Future<Class<T>> compileAsync(final String className, final String sourceCode) {
		return COMPILER_THREADS.submit(() -> {
			try {
				return compile(className, sourceCode);
			} catch (final CompilationException e) {
				log.warn("Error loading compiled class", e);
				return null;
			}
		});
	}

	public <T, R> Function<T, R> compileAndConstructFunctionalInterface(final Class<T> argType,
			final Class<R> returnType, final String body, final Class<?>... extraImports) throws CompilationException {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final String className = "Func" + UUID.randomUUID().toString().replace("-", "");

		final InMemoryClassFile classOuput = new InMemoryClassFile(className);
		final StandardJavaFileManager standardFileManager = compilerReference.getStandardFileManager(collector, null,
				null);

		final String sourceCode = getFunctionalSourceCode(className, argType, returnType, extraImports, body);
		try (final JavaFileManager wrappedManager = new InMemoryFileManager(standardFileManager, classOuput)) {
			compile(className, sourceCode, collector, wrappedManager);

			// Load the in memory bytecode as a Class.
			return constructInstance(loadClass(className, classOuput));
		} catch (final ClassNotFoundException | IOException excp) {
			throw new CompilationException("Error loading compiled class", excp);
		}
	}

	public <T, R> Future<Function<T, R>> compileAndConstructFunctionalInterfaceAsync(final Class<T> argType,
			final Class<R> returnType, final String body, final Class<?>... extraImports) {
		return COMPILER_THREADS.submit(() -> {
			try {
				return compileAndConstructFunctionalInterface(argType, returnType, body, extraImports);
			} catch (final CompilationException e) {
				log.warn("Error loading compiled class", e);
				return null;
			}
		});
	}

	public <T, U, R> BiFunction<T, U, R> compileAndConstructBiFunctionalInterface(final Class<T> arg1Type,
			final Class<U> arg2Type, final Class<R> returnType, final String body, final Class<?>... extraImports)
			throws CompilationException {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final String className = "BiFunc" + UUID.randomUUID().toString().replace("-", "");

		final InMemoryClassFile classOuput = new InMemoryClassFile(className);
		final StandardJavaFileManager standardFileManager = compilerReference.getStandardFileManager(collector, null,
				null);

		final String sourceCode = getBiFunctionalSourceCode(className, arg1Type, arg2Type, returnType, extraImports,
				body);
		try (final JavaFileManager wrappedManager = new InMemoryFileManager(standardFileManager, classOuput)) {
			compile(className, sourceCode, collector, wrappedManager);

			// Load the in memory bytecode as a Class.
			return constructInstance(loadClass(className, classOuput));
		} catch (final ClassNotFoundException | IOException excp) {
			throw new CompilationException("Error loading compiled class", excp);
		}
	}

	public <T, U, R> Future<BiFunction<T, U, R>> compileAndConstructBiFunctionalInterfaceAsync(final Class<T> arg1Type,
			final Class<U> arg2Type, final Class<R> returnType, final String body, final Class<?>... extraImports) {
		return COMPILER_THREADS.submit(() -> {
			try {
				return compileAndConstructBiFunctionalInterface(arg1Type, arg2Type, returnType, body, extraImports);
			} catch (final CompilationException e) {
				log.warn("Error loading compiled class", e);
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T, R> BiFunction<T, Object[], R> compileMethodCaller(final Method m) throws CompilationException {
		final StringBuilder body = new StringBuilder();
		final boolean hasReturn = !m.getReturnType().equals(void.class);
		if (hasReturn) {
			body.append("return ");
		}
		body.append("arg1." + m.getName() + "(");
		final Class<?>[] extraImports = new Class[m.getParameterCount()];
		int i = 0;
		for (final Class<?> argType : m.getParameterTypes()) {
			extraImports[i] = argType;
			body.append("(" + argType.getSimpleName() + ") arg2[" + i + "]");
			++i;
			if (i < m.getParameterCount()) {
				body.append(", ");
			}
		}
		body.append(");");
		if (!hasReturn) {
			body.append("return null;");
		}

		return compileAndConstructBiFunctionalInterface((Class<T>) m.getDeclaringClass(), Object[].class,
				(Class<R>) toReferenceType(m.getReturnType()), body.toString(), extraImports);
	}

	public <T, R> Future<BiFunction<T, Object[], R>> compileMethodCallerAsync(final Method m) {
		return COMPILER_THREADS.submit(() -> {
			try {
				return compileMethodCaller(m);
			} catch (final CompilationException e) {
				log.warn("Error loading compiled class", e);
				return null;
			}
		});
	}

	private Class<?> toReferenceType(final Class<?> c) {
		if (c.isPrimitive()) {
			if (c.equals(boolean.class)) {
				return Boolean.class;
			} else if (c.equals(byte.class)) {
				return Byte.class;
			} else if (c.equals(char.class)) {
				return Character.class;
			} else if (c.equals(short.class)) {
				return Short.class;
			} else if (c.equals(int.class)) {
				return Integer.class;
			} else if (c.equals(long.class)) {
				return Long.class;
			} else if (c.equals(float.class)) {
				return Float.class;
			} else if (c.equals(double.class)) {
				return Double.class;
			} else {
				return Void.class;
			}
		} else {
			return c;

		}
	}

	private String getImportName(final Class<?> c) {
		final String name;
		if (c.getComponentType() != null) {
			name = c.getComponentType().getName();
		} else {
			name = c.getName();
		}
		// Handle inner class definitions
		return name.replace("$", ".");
	}

	private String getFunctionalSourceCode(final String className, final Class<?> argType, final Class<?> returnType,
			final Class<?>[] extraImports, final String body) {
		final String argT = argType.getSimpleName();
		final String retT = returnType.getSimpleName();
		final StringBuilder builder = new StringBuilder();
		builder.append("import java.util.function.Function;\n");
		if (isImportable(argType)) {
			builder.append("import " + getImportName(argType) + ";\n");
		}
		if (isImportable(returnType)) {
			builder.append("import " + getImportName(returnType) + ";\n");
		}
		for (final Class<?> c : extraImports) {
			if (isImportable(returnType)) {
				builder.append("import " + getImportName(c) + ";\n");
			}
		}
		builder.append("public class " + className + " implements Function<" + argT + "," + retT + "> {\n");
		builder.append("public " + retT + " apply(" + argT + " arg) {\n");
		builder.append(body);
		builder.append("}\n");
		builder.append("}\n");
		return builder.toString();
	}

	private boolean isImportable(final Class<?> c) {
		if (c.isPrimitive()) {
			return false;
		} else if (c.equals(Boolean.class) || c.equals(Byte.class) || c.equals(Character.class) || c.equals(Short.class)
				|| c.equals(Integer.class) || c.equals(Long.class) || c.equals(Float.class) || c.equals(Double.class)
				|| c.equals(Void.class)) {
			return false;
		} else {
			return true;
		}
	}

	private String getBiFunctionalSourceCode(final String className, final Class<?> arg1Type, final Class<?> arg2Type,
			final Class<?> returnType, final Class<?>[] extraImports, final String body) {
		final String arg1T = arg1Type.getSimpleName();
		final String arg2T = arg2Type.getSimpleName();
		final String retT = returnType.getSimpleName();

		final StringBuilder b = new StringBuilder();
		b.append("import java.util.function.BiFunction;\n");
		if (isImportable(arg1Type)) {
			b.append("import " + getImportName(arg1Type) + ";\n");
		}
		if (isImportable(arg2Type)) {
			b.append("import " + getImportName(arg2Type) + ";\n");
		}
		if (isImportable(returnType)) {
			b.append("import " + getImportName(returnType) + ";\n");
		}
		for (final Class<?> c : extraImports) {
			if (isImportable(returnType)) {
				b.append("import " + getImportName(c) + ";\n");
			}
		}
		b.append("public class " + className + " implements BiFunction<" + arg1T + "," + arg2T + "," + retT + "> {\n");
		b.append("public " + retT + " apply(" + arg1T + " arg1, " + arg2T + " arg2) {\n");
		b.append(body);
		b.append("}\n");
		b.append("}\n");

		return b.toString();
	}

	private void compile(final String className, final String sourceCode,
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
