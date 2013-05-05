package com.izforge.izpack.installer;

//does conversion and validates results
//see Option for result description
public abstract class ValidatingConverter<F, T> {
	public abstract Option<T> convert(F from);
}

