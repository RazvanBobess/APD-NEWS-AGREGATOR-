package main;

import tools.jackson.databind.*;
import tools.jackson.core.*;
import java.io.*;
import java.util.ArrayList;

public class Main {
	private static final String SRC_PATH = "";
	// number of threads that will be used
	private static int P;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java Main <P> <input_file> <additional_file>");
			System.exit(1);
		}

		P = Integer.parseInt(args[0]);
		String articles_txt = args[1];
		String inputs_txt = args[2];

		ArrayList<String> inputs_content = new ArrayList<>();
		ArrayList<String> articles_content = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(inputs_txt))){
			String line;

			line = br.readLine();

			int files_count = Integer.parseInt(line);

			for (int i = 0; i < files_count; i++) {
				line = br.readLine();
				inputs_content.add(SRC_PATH + line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedReader br = new BufferedReader(new FileReader(articles_txt))) {
			String line;

			line = br.readLine();
			int articles_count = Integer.parseInt(line);

			for (int i = 0; i < articles_count; i++) {
				line = br.readLine();
				articles_content.add(SRC_PATH + line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
