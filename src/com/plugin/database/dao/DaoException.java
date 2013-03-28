package com.plugin.database.dao;

import android.database.SQLException;

/**
 * @Date 2012-08-23
 */
public class DaoException extends SQLException {

	private static final long serialVersionUID = 1L;

	public DaoException() {
	}

	public DaoException(String error) {
		super(error);
	}

	public DaoException(String error, Throwable cause) {
		super(error);
	}

	public DaoException(Throwable th) {
	}

}