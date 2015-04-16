/*
 * Copyright (C) 2015 Processwide AG. All Rights Reserved. DO NOT ALTER OR
 * REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is provided as-is without warranty of merchantability or fitness for a
 * particular purpose.
 *
 * See http://www.inexas.com/license for license details.
 */
package com.inexas.tad;

import static org.junit.Assert.*;
import java.io.*;
import org.junit.Test;

/**
 * @author kwhittingham
 */
public class TestTadContextErrors {
	public static class Started {
		public static void main(String[] args) {
			final SampleTad tad = new SampleTad("test");
			Context.attach(tad);
			if(args.length == 0) {
				Context.detach(tad);
			}
		}
	}

	/**
	 * To test the cleaner function in the Context we need to spawn a
	 * separate process as JUnit calls System.exit() and so testing won't work.
	 */
	private boolean spawn(boolean causeError) throws Exception {
		final boolean result;

		final String javaBin = System.getProperty("java.home")
				+ File.separator + "bin"
				+ File.separator + "java";
		final String classpath = System.getProperty("java.class.path");
		final String className = Started.class.getName();

		final ProcessBuilder builder;
		if(causeError) {
			builder = new ProcessBuilder(javaBin, "-cp", classpath, className, "error");
		} else {
			builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
		}

		final Process process = builder.start();
		process.waitFor();

		final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		final StringBuilder sb = new StringBuilder();
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append('\n');
		}
		final String output = sb.toString();
		result = output.indexOf("TadRuntimeException") >= 0;

		return result;
	}

	/**
	 * Check that the cleaner thread makes sure that all the TADs have been
	 * detached.
	 */
	@Test
	public void cleaner() throws Exception {
		assertTrue(spawn(true));
		assertFalse(spawn(false));
	}

	@Test
	public void testSingleAttach() {
		final SampleTad tad = new SampleTad("test");
		Context.attach(tad);
		Context.detach(tad);
	}

	@Test
	public void testDoubleAttach() {
		final SampleTad tad = new SampleTad("test");
		try {
			Context.attach(tad);
			Context.attach(tad);
			fail();
		} catch(final TadRuntimeException e) {
			Context.detach(tad);
		}
	}
}
