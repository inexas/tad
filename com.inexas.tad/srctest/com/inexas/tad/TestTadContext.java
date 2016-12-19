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
import org.junit.Test;

/**
 * Unit tests and example use of all the TAD features
 *
 * Note the join() in the Cleaner thread can not be unit tested because JUnit
 * calls System.exit() before the thread has had chance to run
 */

public class TestTadContext implements Tad {

	private class SomeOtherThread extends Thread {
		boolean ran;

		@Override
		public void run() {
			synchronized(this) {
				if(TadContext.getButDontThrow(SampleTad.class) != null) {
					fail("Another Thread saw the TAD");
				}
				ran = true;
				notify();
			}
		}
	}

	private void checkThisThreadThreadSeesTheTad(String name) {
		final SampleTad found = (SampleTad)TadContext.get(SampleTad.class);
		assertEquals(name, found.getName());
	}

	private void checkThisThreadThreadSeesTheTad(Class<? extends Tad> keyClass, String name) {
		final SampleTad found = (SampleTad)TadContext.get(keyClass);
		assertEquals(name, found.getName());
	}

	private void checkSomeOtherThreadDoesntSeeTheTad() {
		final SomeOtherThread thread = new SomeOtherThread();
		thread.start();

		synchronized(thread) {
			try {
				thread.wait();
			} catch(final InterruptedException e) {
				e.printStackTrace();
			}

			assertTrue(thread.ran);
		}
	}

	/**
	 * This test demonstrates the basic operation of the Context. In most
	 * situations, this is what you'll use.
	 */
	@Test
	public void basicOperation() {
		final String name = "basic";

		/*
		 * Create a TAD class. The TAD can be any class and contain anything you
		 * want it to as long as it implements the Tad interface
		 */
		final SampleTad testTad = new SampleTad(name);

		/*
		 * If we were to try and retrieve testTad in this (or any) Thread
		 * without attaching it first, getButDontThrow(...) will return null but
		 * get(...) with throw a RuntimeException
		 */
		assertNull(TadContext.getButDontThrow(SampleTad.class));
		checkSomeOtherThreadDoesntSeeTheTad();

		/*
		 * Now we attach testTad
		 */
		TadContext.attach(testTad);
		checkThisThreadThreadSeesTheTad(name);
		checkSomeOtherThreadDoesntSeeTheTad();

		/*
		 * A Thread that attaches a TAD class <b>must</b> detach it too and it's
		 * good practice to do it in the same method.
		 */
		TadContext.detach(testTad);
		assertNull(TadContext.getButDontThrow(SampleTad.class));
	}

	@Test
	public void basicTwoTads() {
		final String name1 = "tad1";
		final String name2 = "tad2";

		/*
		 * You can attach as many TAD classes as you want to the Thread.
		 * Normally they would be of different classes but in this case we'll
		 * add two instances of the same class twice.
		 *
		 * The simple attach(Tad) method uses the class of the TAD object as the
		 * key to retrieve it. Since we can't use the same key for two objects
		 * we'll use an explicit key for tad2. You can use any class you like,
		 * we'll arbitrarily use TestTadContext.class.
		 */
		final SampleTad tad1 = new SampleTad(name1);
		TadContext.attach(tad1);
		final SampleTad tad2 = new SampleTad(name2);
		TadContext.attach(TestTadContext.class, tad2);

		checkThisThreadThreadSeesTheTad(name1);
		checkThisThreadThreadSeesTheTad(TestTadContext.class, name2);
		checkSomeOtherThreadDoesntSeeTheTad();

		TadContext.detach(tad1);
		TadContext.detach(TestTadContext.class, tad2);
	}

	@Test
	public void basicServiceLikeAttach() {
		final String name = "tad1";

		/*
		 * When we want to advertise services we will have an interface and an
		 * implementation of that interface which will depending on the
		 * circumstances. For example, in production we might have a credit card
		 * checker implementation, when we are testing we might want to use a
		 * mock credit card checker so both will implement the a service
		 * interface and we will want to associate the implementation with the
		 * interface.
		 */
		final SampleTad serviceImpl = new SampleTad(name);
		TadContext.attach(NamingService.class, serviceImpl);

		// Elsewhere in the code...
		assertTrue(TadContext.get(NamingService.class) == serviceImpl);

		// Once we're done, dismantle..
		TadContext.detach(serviceImpl);
	}

	@Test(expected = TadRuntimeException.class)
	public void testGetWithNoTad() {
		TadContext.get(SampleTad.class);
	}

	@Test
	public void testGetWithNoTadWithoutThrow() {
		assertNull(TadContext.getButDontThrow(SampleTad.class));
	}

	/**
	 * <b>Note: </b> <i>TAD stacks are experimental. Play with them. If you like
	 * then and can think of a convincing argument why they should stay then let
	 * us know. Otherwise don't depend on them too much as they might disappear
	 * in the future.</b>
	 *
	 * The idea behind TAD stacks is that you can push configuration onto a
	 * stack so that subordinate code will use that configuration instead of the
	 * 'default' configuration. The same could be achieved by detaching the
	 * current value for a given key, replace it, then when done, attaching the
	 * original value but it's a bit messy.
	 */
	@Test
	public void stack() {
		final String name1 = "tad1";
		final String name2 = "tad2";

		final SampleTad tad1 = new SampleTad(name1);
		TadContext.pushAttach(tad1);

		final SampleTad peeked1 = (SampleTad)TadContext.get(SampleTad.class);
		assertNotNull(peeked1);
		assertEquals(name1, peeked1.getName());

		final SampleTad tad2 = new SampleTad(name2);
		TadContext.pushAttach(tad2);

		final SampleTad peeked2 = (SampleTad)TadContext.get(SampleTad.class);
		assertNotNull(peeked2);
		assertEquals(name2, peeked2.getName());

		TadContext.detach(peeked2);
		final SampleTad peeked3 = (SampleTad)TadContext.get(SampleTad.class);
		assertEquals(name1, peeked3.getName());

		TadContext.detach(peeked3);
		assertNull(TadContext.getButDontThrow(SampleTad.class));
	}

	@Test
	public void globals() {
		final SampleTad threadTad = new SampleTad("thread");
		final SampleTad globalTad = new SampleTad("global");

		// Publish a global
		TadContext.publish(globalTad);
		assertEquals(globalTad, TadContext.get(SampleTad.class));

		// Attach a TAD, this should hide the global
		TadContext.attach(threadTad);
		assertEquals(threadTad, TadContext.get(SampleTad.class));

		// Detach the TAD, this should expose the global again
		TadContext.detach(threadTad);
		assertEquals(globalTad, TadContext.get(SampleTad.class));

		TadContext.unpublish(globalTad);
	}

}
