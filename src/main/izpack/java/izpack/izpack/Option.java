package com.izforge.izpack.installer;

// Optional parameter (makes optional parameters handling explicit)
// it has two states:
//   defined -> isDefined == true, value holds value
//   undefined -> isDefined == false, msg holds reason of emptiness
public class Option<T> {
	public final boolean isDefined;
	public final T value;
	public final String msg;
	
	private Option(boolean isDefined, T value, String msg) {
		this.isDefined = isDefined;
		this.value = value;
		this.msg = msg;
	}
	
	public static <T> Option<T> full(T value) {
		return new Option<T>(true, value, null);
	}
	
	public static <T> Option<T> empty(String msg) {
		return new Option<T>(false, null, msg);
	}
}
