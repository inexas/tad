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
 * This is a test TAD class that is used in tests. See the TestXxx files.
 */
public class SampleTad implements NamingService {
	private final String name;

	public SampleTad(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}