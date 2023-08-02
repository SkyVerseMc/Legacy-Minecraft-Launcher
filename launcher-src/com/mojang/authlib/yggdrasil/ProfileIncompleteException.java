package com.mojang.authlib.yggdrasil;

public class ProfileIncompleteException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProfileIncompleteException() {}

	public ProfileIncompleteException(String message) {

		super(message);
	}

	public ProfileIncompleteException(String message, Throwable cause) {

		super(message, cause);
	}

	public ProfileIncompleteException(Throwable cause) {

		super(cause);
	}
}
