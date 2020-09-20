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

	public void compile(final String className, final String sourceCode) {
		final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		final InmemoryClassFile classOuput = new InmemoryClassFile(className);
		final JavaFileManager wrappedManager = createFileManager(collector, classOuput);

		final JavaCompiler.CompilationTask task = compilerReference.getTask(null, wrappedManager, collector, null, null,
				getCompilationUnits(className, sourceCode));

		final long s = System.currentTimeMillis();
		if (!task.call()) {
			collector.getDiagnostics().forEach(System.out::println);
		}
		final long e = System.currentTimeMillis();
		System.out.println("Compiling took " + (e - s) + " milliseconds");

		// loading and using our compiled class
		final ClassLoader inMemoryClassLoader = createClassLoader(classOuput);
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

	private JavaFileManager createFileManager(final DiagnosticCollector<JavaFileObject> collector,
			final InmemoryClassFile byteObject) {
		final StandardJavaFileManager standardFileManager = compilerReference.getStandardFileManager(collector, null,
				null);

		return new ForwardingJavaFileManager<StandardJavaFileManager>(standardFileManager) {
			@Override
			public JavaFileObject getJavaFileForOutput(final Location location, final String className,
					final JavaFileObject.Kind kind, final FileObject sibling) throws IOException {
				return byteObject;
			}
		};
	}

	private static ClassLoader createClassLoader(final InmemoryClassFile byteObject) {
		return new ClassLoader() {
			@Override
			public Class<?> findClass(final String name) throws ClassNotFoundException {
				final byte[] bytes = byteObject.getBytes();
				return defineClass(name, bytes, 0, bytes.length);
			}
		};
	}

	public static Iterable<? extends JavaFileObject> getCompilationUnits(final String className,
			final String sourceCode) {
		final InmemorySourceFile stringObject = new InmemorySourceFile(className, sourceCode);
		return Arrays.asList(stringObject);
	}

}
