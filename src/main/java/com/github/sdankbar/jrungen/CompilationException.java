package com.github.sdankbar.jrungen;

public class CompilationException extends Exception {

	public CompilationException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public CompilationException(final Throwable cause) {
		super(cause);
	}

	public CompilationException(final String msg) {
		super(msg);
	}

}
