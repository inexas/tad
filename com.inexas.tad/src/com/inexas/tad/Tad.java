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

/**
 * Implement this class in a class that you want to associate with a Thread
 *
 * ?todo We could do away with the restriction of having to implement Tad
 * however I have found it useful to identify which classes are to be used as
 * TADs and it does provide some type safety.
 *
 * @author kwhittingham
 *
 */
public interface Tad {
	// Nothing to do, just a marker interface
}
