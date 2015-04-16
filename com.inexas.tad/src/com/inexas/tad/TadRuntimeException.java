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
