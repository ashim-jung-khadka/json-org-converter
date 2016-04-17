package com.github.ashim.json.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utility {

	public static String getJsonAsString(String name) {

		InputStream input = Utility.class.getClassLoader().getResourceAsStream(name);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
			String line;
			StringBuilder resultBuilder = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				resultBuilder.append(line);
			}

			return resultBuilder.toString();
		} catch (IOException ex) {
			System.out.println("Exception while reading resource");
			System.out.println(ex);
		}

		return null;
	}

}
