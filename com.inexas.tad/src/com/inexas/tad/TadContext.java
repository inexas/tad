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

import java.util.*;
import java.util.Map.Entry;

/**
 * The Thread Attached Data (TAD) Context provides the interface that allows
 * data to be attached to, retrieved and removed from the current thread's
 * context.
 *
 */
public class TadContext {
	// todo Add searches ClassThatImplementsInterface getService(Interface)

	/**
	 * Elements are used to implement the experimental stacked-TAD capability
	 */
	private static class Link implements Tad {
		final Tad tad, nextInChain;

		Link(Tad tad, Tad nextInChain) {
			this.tad = tad;
			this.nextInChain = nextInChain;
		}
	}

	/**
	 * This stores all TADs as a Map of Maps. The 'exterior' Map's keys are the
	 * thread IDs. The interior Map maps a Class, typically the class that
	 * implements Tad, to a TAD object.
	 *
	 * The TAD objects may be either user defined objects or instances of
	 * Element if the user has elected to use a stack of TADs.
	 */

	private final static Map<Long, Map<Class<?>, Tad>> threadMap = new HashMap<>();

	/**
	 * This stores all published TADs in a simple Map.
	 */

	private final static Map<Class<?>, Tad> globalMap = new HashMap<>();
	/** In honor of Pulp Fiction. */
	
	private final static String winstonWolfe = "Winston Wolfe";

	private final static Set<Thread> cleaners = new HashSet<>();

	private static boolean terminating = false;

	/**
	 * Thread ID's can and probably will be reused so the user must make sure
	 * that all TAD object references for a Thread are detached before the
	 * Thread terminates. This class makes sure that the user has detached all
	 * TADs.
	 */
	private static class Cleaner implements Runnable {
		private final Long threadId;
		private final Thread thread;

		public Cleaner(Thread thread, Long threadId) {
			this.thread = thread;
			this.threadId = threadId;
		}

		@Override
		public synchronized void run() {
			try {
				thread.join();
				checkTadsEmpty();
			} catch(final InterruptedException e) {
				if(terminating) {
					checkTadsEmpty();
				} else {
					throw new TadRuntimeException("Join interrupted", e);
				}
			}
		}

		private void checkTadsEmpty() {
			final Map<Class<?>, Tad> tads = threadMap.get(threadId);
			if(!tads.isEmpty()) {
				throw new TadRuntimeException("TADs still attached: " + tads.keySet());
			}
			threadMap.remove(threadId);

		}
	}

	/**
	 * Used to flag if a shutdown hook has been set up.
	 */
	private static boolean hooked;

	/**
	 * Attach a TAD class to the current thread using the TAD's class as a key.
	 *
	 * @param tad
	 *            Any class implementing the Tad interface.
	 */
	public static synchronized void attach(Tad tad) {
		assert tad != null : "Null TAD";

		attach(tad.getClass(), tad, false);
	}

	/**
	 * Attach a TAD class to the current thread mapped to an explicit key.
	 *
	 * @param keyClass
	 *            The key class to attached the data object with. Typically this
	 *            will be an interface of the Tad class.
	 * @param tad
	 *            Any class implementing the Tad interface.
	 */
	public static synchronized void attach(Class<?> keyClass, Tad tad) {
		attach(keyClass, tad, false);
	}

	/**
	 * Attach a TAD class to the current thread using a stack metaphor. Stacks
	 * must either be used all the time or not used at all for a given TAD
	 * key/thread. That is don't mix calls to attach() and pushAttach().
	 *
	 * This is also useful when the code is reentrant.
	 *
	 * @param tad
	 *            Any class implementing the Tad interface.
	 */
	public static synchronized void pushAttach(Tad tad) {
		assert tad != null : "Null TAD";

		/*
		 * KW One use of this is when it is not know if the TAD has already been
		 * attached then it can be pushAttached and and removed safely. Set
		 * Oak.java for an example
		 */
		attach(tad.getClass(), tad, true);
	}

	/**
	 * Get TAD mapped to a given TAD key class.
	 *
	 * @param keyClass
	 *            The key of the TAD object to retrieve.
	 * @param <T>
	 *            A type implementing the Tad interface.
	 * @return The TAD attached with the given keyClass.
	 * @throws TadRuntimeException
	 *             Thrown if no corresponding object for the keyClass is found.
	 */
	public static <T extends Tad> T get(Class<? extends Tad> keyClass) throws TadRuntimeException {
		return get(keyClass, true);
	}

	/**
	 * Return an object attached with a TAD class. If the object is not found
	 * then null is returned
	 *
	 * @see #get(Class)
	 * @param keyClass
	 *            The key class of the TAD to search for.
	 * @param <T>
	 *            A type implementing the Tad interface.
	 * @return The TAD attached to the given keyClass or null.
	 */
	public static <T extends Tad> T getButDontThrow(Class<? extends Tad> keyClass) {
		return get(keyClass, false);
	}

	/**
	 * Detach Thread Attached Data.
	 *
	 * @param tad
	 *            A class implementing the Tad interface previously attached
	 *            with the current thread.
	 */
	public static synchronized void detach(Tad tad) {
		assert tad != null : "Null TAD??";

		final Long threadId = new Long(Thread.currentThread().getId());
		final Map<Class<?>, Tad> tads = threadMap.get(threadId);
		if(tads == null) {
			throw new TadRuntimeException("No TADs stored for this class");
		}

		// Assume that the keyClass is that of the TAD
		final Class<? extends Tad> keyClass = tad.getClass();
		Tad removed = tads.remove(keyClass);
		if(removed == null) {
			// Not found, assume it was a attach(keyClass, tad)...
			boolean found = false;
			for(final Entry<Class<?>, Tad> candidate : tads.entrySet()) {
				if(candidate.getValue() == tad) {
					tads.remove(candidate.getKey());
					found = true;
					break;
				}
			}
			if(!found) {
				throw new TadRuntimeException("No TAD for " + keyClass.getName());
			}
		} else if(removed.getClass() == Link.class) {
			// The TAD has been pushAttached...
			final Link link = (Link)removed;

			if(link.tad != tad) {
				throw new TadRuntimeException("TAD push ordering problem");
			}

			final Link nextInChain = (Link)link.nextInChain;
			if(nextInChain != null) {
				tads.put(keyClass, nextInChain);
			}
			removed = link.tad;
		} else {
			assert removed == tad;
		}
	}

	/**
	 * Detach Thread Attached Data from current Thread.
	 *
	 * @param keyClass
	 *            The key class used to attach the TAD.
	 * @param tad
	 *            An Object implementing the Tad interface previously attached
	 *            with the current thread.
	 */
	public static synchronized void detach(Class<?> keyClass, Tad tad) {
		assert keyClass != null;
		assert tad != null : "Null TAD??";

		final Long threadId = new Long(Thread.currentThread().getId());

		final Map<Class<?>, Tad> tads = threadMap.get(threadId);
		if(tads == null) {
			throw new TadRuntimeException(
					"This thread has no Thread Attached Data for key: " + keyClass.getName());
		}

		Tad removed = tads.remove(keyClass);
		if(removed == null) {
			throw new TadRuntimeException("No TAD for " + keyClass.getName());
		}

		if(removed.getClass() == Link.class) {
			final Link link = (Link)removed;
			final Link nextInChain = (Link)link.nextInChain;
			if(nextInChain != null) {
				tads.put(keyClass, nextInChain);
			}
			removed = link.tad;
		}

		if(removed != tad) {
			throw new TadRuntimeException(
					"Another object of the same type was attached: " + tad.getClass().getName());
		}
	}

	/**
	 * Detach all TADs for current thread.
	 *
	 * @deprecated Deprecated because it's bad practice but you can trust it
	 *             will always be here. The method was added for use in unit
	 *             tests.
	 */
	@Deprecated
	public static synchronized void detachAll() {
		final Long threadId = new Long(Thread.currentThread().getId());
		final Map<Class<?>, Tad> tads = threadMap.get(threadId);
		if(tads != null) {
			// Leave the Map in place but empty
			tads.clear();
		}
	}

	/**
	 * Publish a TAD class globally using the TAD's class as a key.
	 *
	 * @param tad
	 *            Any class implementing the Tad interface.
	 */
	public static synchronized void publish(Tad tad) {
		assert tad != null : "Null TAD";

		publish(tad.getClass(), tad);
	}

	/**
	 * Publish a TAD class globally mapped to an explicit key.
	 *
	 * @param keyClass
	 *            The key class to be published. Typically this will be an
	 *            interface of the Tad class.
	 * @param tad
	 *            Any class implementing the Tad interface.
	 */
	public static synchronized void publish(Class<?> keyClass, Tad tad) {
		assert keyClass != null : "Null keyClass";
		assert tad != null : "Null TAD";

		if(globalMap.containsKey(keyClass)) {
			throw new TadRuntimeException(
					"Thread already has TAD published for key: " + keyClass.getName());
		}

		globalMap.put(keyClass, tad);

		if(!hooked) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					if(!globalMap.isEmpty()) {
						throw new TadRuntimeException("TADs still published: " + globalMap.keySet());
					}
				}
			});
			hooked = true;
		}
	}

	/**
	 * Unpublish global data object.
	 *
	 * @param tad
	 *            A class implementing the Tad interface previously published.
	 */
	public static synchronized void unpublish(Tad tad) {
		assert tad != null : "Null TAD??";

		unpublish(tad.getClass(), tad);
	}

	/**
	 * Unpublish global data object.
	 *
	 * @param keyClass
	 *            The key class used to publish the TAD.
	 * @param tad
	 *            An Object implementing the Tad interface previously published.
	 */
	public static synchronized void unpublish(Class<?> keyClass, Tad tad) {
		assert keyClass != null;
		assert tad != null : "Null TAD??";

		final Tad removed = globalMap.remove(keyClass);
		if(removed == null) {
			throw new TadRuntimeException("No TAD published for " + keyClass.getName());
		}

		if(removed != tad) {
			throw new TadRuntimeException(
					"Another object of the same type was published: " + tad.getClass().getName());
		}
	}

	/**
	 * In some cases you may need to manually terminate the cleaner threads. For
	 * example Tomcat pools exec threads and reuses them. When it shuts down the
	 * cleaner threads have not been terminated and so Tomcat issues a warning.
	 * This method can be called to terminate all cleaner threads - in Tomcats
	 * case in the servlet destroy method() for example.
	 */
	public static void terminate() {
		terminating = true;
		for(final Thread cleaner : cleaners) {
			cleaner.interrupt();
		}
		cleaners.clear();
	}

	/**
	 * This is the main workhorse for the three attach methods above.
	 */
	private static synchronized void attach(Class<?> keyClass, Tad tad, boolean stack) {
		assert keyClass != null : "Null keyClass";
		assert tad != null : "Null TAD";

		final Thread thread = Thread.currentThread();
		final Long threadId = new Long(thread.getId());
		Map<Class<?>, Tad> tads = threadMap.get(threadId);
		if(tads == null) {
			// First attach for this thread

			// Create a new Map for this Thread...
			tads = new HashMap<>();
			threadMap.put(threadId, tads);

			// Start a Cleaner...
			final Thread cleaner = new Thread(new Cleaner(thread, threadId), winstonWolfe);
			final boolean added = cleaners.add(cleaner);
			assert added;
			cleaner.start();

			// Store the new TAD
			tads.put(keyClass, tad);
		} else {
			// TADs previously attached with this Thread...

			// Make sure the key has not been used yet...
			if(tads.containsKey(keyClass)) {
				// TAD exists for this keyClass
				if(stack) {
					// Add the new Element as first in chain...
					final Link firstInChain = (Link)tads.get(keyClass);
					final Link nextInChain = new Link(tad, firstInChain);
					tads.put(keyClass, nextInChain);
				} else {
					throw new TadRuntimeException(
							"Thread already has TAD for key: " + keyClass.getName());
				}
			} else { // No previous data
				if(stack) {
					// Add new first-in-chain element...
					final Link firstInChain = new Link(tad, null);
					tads.put(keyClass, firstInChain);
				} else {
					tads.put(keyClass, tad);
				}
			}
		}
	}

	private static <T extends Tad> T getPublished(Class<? extends Tad> keyClass, boolean strict)
			throws TadRuntimeException {

		@SuppressWarnings("unchecked")
		final T result = (T)globalMap.get(keyClass);
		if(result == null && strict) {
			throw new TadRuntimeException(
					"Missing thread attached or published data for: " + keyClass.getName());
		}

		return result;
	}

	private static <T extends Tad> T get(Class<? extends Tad> keyClass, boolean strict) {
		final T result;

		final Long threadId = new Long(Thread.currentThread().getId());
		final Map<Class<?>, Tad> tads = threadMap.get(threadId);
		if(tads == null) {
			result = getPublished(keyClass, strict);
		} else {
			final Tad tad = tads.get(keyClass);
			if(tad == null) {
				result = getPublished(keyClass, strict);
			} else {
				if(tad.getClass() == Link.class) {
					@SuppressWarnings("unchecked")
					final T t = (T)((Link)tad).tad;
					result = t;
				} else {
					@SuppressWarnings("unchecked")
					final T t = (T)tad;
					result = t;
				}
			}
		}

		return result;
	}

}
