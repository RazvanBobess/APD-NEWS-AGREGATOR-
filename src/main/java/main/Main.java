package main;

import tools.jackson.databind.*;
import tools.jackson.core.*;
import java.io.*;

public class Main {
	private static final String CATEGORIES_FILE_PATH = "../input/files/categories.txt";
	private static final String LANGUAGES_FILE_PATH = "../input/files/languages.txt";

	// number of threads that will be used
	private static int P;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java Main <P> <input_file> <additional_file>");
		}

		P = Integer.parseInt(args[0]);
		String articles_txt = args[1];
		String inputs_txt = args[2];

		try (BufferedReader br = new BufferedReader(new FileReader(inputs_txt))){
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		P = Integer.parseInt(args[0]);
	}
}
