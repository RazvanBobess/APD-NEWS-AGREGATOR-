package main;

import tools.jackson.databind.*;
import tools.jackson.core.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

public class Main {
	// number of threads that will be used
	private static int P;

	public static void process_file(String file_path, ArrayList<String> file_content) {
		Path base_dir = Paths.get(file_path).getParent();

		try (BufferedReader br = new BufferedReader(new FileReader(file_path))){
			String line;

			line = br.readLine();

			int files_count = Integer.parseInt(line);

			for (int i = 0; i < files_count; i++) {
				line = br.readLine();
				file_content.add(base_dir.resolve(line).normalize().toString());
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
		statistics.logPath(articles_txt);
		process_file(articles_txt, articles_content);

		statistics.parse_file(inputs_txt);

		MyThread[] threads = new MyThread[P];
		CyclicBarrier barrier = new CyclicBarrier(P);

		for (int i = 0; i < P; i++) {
			threads[i] = new MyThread(P, articles_content, i, statistics, barrier);
			threads[i].start();
		}

		for (int i = 0; i < P; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
