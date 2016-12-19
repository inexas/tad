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

import java.io.IOException;
import java.util.*;

/**
 * This class is used to generated documentation, ignore it.
 *
 * @author kwhittingham
 */
@SuppressWarnings("unused")
public class WorkfowServletHandler extends HttpServlet {
	private ProcessArchiver processArchiverImpl;
	private WorkflowServer workflowServer;

	protected void doGet(
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		final String action = request.getParameter("action");

		final SessionTad sessionTad = new SessionTad(
				request.getParameter("user"),
				UUID.randomUUID().toString());
		TadContext.attach(sessionTad);

		TadContext.attach(ProcessArchiver.class, processArchiverImpl);

		workflowServer.handleRequest(action);

		TadContext.detach(ProcessArchiver.class, processArchiverImpl);
		TadContext.detach(sessionTad);
	}
}

class DeepInsideTheServer {
	private EventLog eventLog;

	// ...

	public void terminate(BusinessProcess process) {
		final SessionTad sessionTad = TadContext.get(SessionTad.class);
		eventLog.log(
				sessionTad.userId,
				sessionTad.timestamp,
				"Process completed: " + process.getName());

		final ProcessArchiver archiver = TadContext.get(ProcessArchiver.class);
		archiver.archive(process);
	}
}

class HttpServlet {
	//
}

@SuppressWarnings("unused")
class HttpServletRequest {
	public String getParameter(String string) {
		throw new RuntimeException();
	}
}

class HttpServletResponse {
	//
}

interface WorkflowServer {
	void handleRequest(String action);
}

@SuppressWarnings("unused")
class EventLog {
	public void log(String userId, Date timestamp, String string) {
		throw new RuntimeException();
	}

}

interface ProcessArchiver extends Tad {
	void archive(BusinessProcess process);
}

class BusinessProcess {
	public String getName() {
		throw new RuntimeException();
	}
}
