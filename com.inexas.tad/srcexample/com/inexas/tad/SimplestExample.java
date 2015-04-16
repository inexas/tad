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
public class SimplestExample {
	public static class MyTad implements Tad {
		public final String userName;

		public MyTad(String userName) {
			this.userName = userName;
		}
	}

	public static void main(String[] args) {
		final MyTad myTad = new MyTad("hhughs");
		Context.attach(myTad);

		whatever();

		Context.detach(myTad);
	}

	private static void whatever() {
		final MyTad myTad = Context.get(MyTad.class);
		System.out.println(myTad.userName);
	}
}