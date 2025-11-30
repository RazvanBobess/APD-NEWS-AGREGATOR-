package main;

import tools.jackson.databind.*;
import tools.jackson.core.*;
import java.io.*;
import java.util.ArrayList;

public class Main {
	private static final String SRC_PATH = "";
	// number of threads that will be used
	private static int P;

	public static void process_file(String file_path, ArrayList<String> file_content) {
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))){
			String line;

			line = br.readLine();

			int files_count = Integer.parseInt(line);

			for (int i = 0; i < files_count; i++) {
				line = br.readLine();
				file_content.add(SRC_PATH + line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java Main <P> <input_file> <additional_file>");
			System.exit(1);
		}

		Statistics statistics = Statistics.getInstance();

		P = Integer.parseInt(args[0]);
		String articles_txt = args[1];
		String inputs_txt = args[2];

		ArrayList<String> articles_content = new ArrayList<>();

		process_file(articles_txt, articles_content);

		statistics.parse_file(inputs_txt);

	}
}
