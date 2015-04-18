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
 * TAD runtime exception is thrown in critical situations where the application
 * cannot recover. Situations are typically the results of programmer errors
 * such as adding the same TAD twice, expecting TADs to be there when they
 * aren't, etc.
 *
 * @author kwhittingham
 */
public class TadRuntimeException extends RuntimeException {
	private static final long serialVersionUID = -2534617191607626521L;

	public TadRuntimeException(String message) {
		super(message);
	}

	public TadRuntimeException(String message, Exception chained) {
		super(message, chained);
	}
}
