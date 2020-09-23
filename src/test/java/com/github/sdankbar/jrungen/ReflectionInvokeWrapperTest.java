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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

import com.github.sdankbar.jrungen.helper.InvokeObject;

/**
 * Tests the ReflectionInvokeWrapper class.
 */
public class ReflectionInvokeWrapperTest {

	/**
	 * @throws SecurityException     e
	 * @throws NoSuchMethodException e
	 * @throws CompilationException  e
	 *
	 */
	@Test
	public void test_wrapReflectMethod() throws NoSuchMethodException, SecurityException, CompilationException {
		final Method m = InvokeObject.class.getMethod("call");
		final ReflectionInvokeWrapper<InvokeObject, Void> c = new ReflectionInvokeWrapper(m);

		final InvokeObject t = new InvokeObject();
		{
			c.invoke(t, null);
			assertEquals(1, t.i);
		}
		c.forceCompilation();
		{
			c.invoke(t, null);
			assertEquals(2, t.i);
		}
	}
}
