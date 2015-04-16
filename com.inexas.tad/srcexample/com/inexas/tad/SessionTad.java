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

public class SessionTad implements Tad {
	public final String userId, requestId;
	public final Date timestamp;
	private final List<String> messages = new ArrayList<>();

	public SessionTad(String userId, String requestId) {
		this.userId = userId;
		this.requestId = requestId;
		timestamp = new Date();
	}

	public void addMessage(String message) {
		messages.add(message);
	}

	public String[] getMessages() {
		return messages.toArray(new String[messages.size()]);
	}
}