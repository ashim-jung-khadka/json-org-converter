package com.github.ashim.json.error;

/**
 * An object containing references to the source of the error.
 *
 * @author Ashim Jung Khadka
 */
public class Source {

	private String pointer;
	private String parameter;

	public String getPointer() {
		return pointer;
	}

	public void setPointer(String pointer) {
		this.pointer = pointer;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

}