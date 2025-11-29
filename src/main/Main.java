
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
	}
}
