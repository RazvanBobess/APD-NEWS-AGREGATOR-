package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// This class must be updated by one thread at a time

public class Statistics {
	private static final SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ");

	private final AtomicInteger duplicates_found;
	private final AtomicInteger unique_articles;

	public Map<String, Integer> authors;
	public Map<String, Integer> top_keyword_en;

	public Set<Article> duplicated_articles;
	public Set<Article> unique_articles_list;

	public Map<String, Set<Article>> categories_list;
	public Map<String, Set<Article>> languages_list;
	public Map<String, String> most_recent_articles;

	public static volatile Statistics instance;

	private Statistics() {
		authors = new ConcurrentHashMap<>();
		most_recent_articles = new ConcurrentHashMap<>();
		top_keyword_en = new ConcurrentHashMap<>();

		duplicated_articles = ConcurrentHashMap.newKeySet();
		unique_articles_list = ConcurrentHashMap.newKeySet();

		categories_list = new ConcurrentHashMap<>();
		languages_list = new ConcurrentHashMap<>();

		duplicates_found = new AtomicInteger(0);
		unique_articles = new AtomicInteger(0);
	}

	public static Statistics getInstance() {
		if (instance == null) {
			synchronized (Statistics.class) {
				if (instance == null) {
					instance = new Statistics();
				}
			}
			return instance;
		}
		return instance;
	}

	// These methods deal with the articles which will be added into the database

	public AtomicInteger get_duplicates_found() {
		return duplicates_found;
	}

	public AtomicInteger get_unique_articles() {
		return unique_articles;
	}

	public void add_duplicates_found() {
		get_duplicates_found().incrementAndGet();
	}

	public static boolean check_if_duplicated(Article article) {
		if (instance.duplicated_articles.contains(article)) {
			instance.add_duplicates_found();
			return true;
		}

		if (instance.unique_articles_list.remove(article)) {
			instance.get_unique_articles().decrementAndGet();
			instance.duplicated_articles.add(article);
			instance.add_duplicates_found();
			return true;
		}

		instance.unique_articles_list.add(article);
		instance.unique_articles.incrementAndGet();
		return false;
	}

	// This part is for categories, adding a new category to the database
	// and adding an article / removing an article from a category
	// Also, printing the categories

	public void update_categories(String category) {
		categories_list.putIfAbsent(category, ConcurrentHashMap.newKeySet());
	}

	public void add_article_to_category(Article article) {
		if (check_if_duplicated(article)) {
			for (String category : article.categories) {
				if (!categories_list.containsKey(category)) continue;

				categories_list.get(category).remove(article);
			}
			return;
		}

		for (String category : article.categories) {
			if (!categories_list.containsKey(category)) continue;
			categories_list.get(category).add(article);
		}
	}

	public void print_categories() {
		String filepath = "../output/categories";
		for (String category : categories_list.keySet()) {
			if (categories_list.get(category).isEmpty()) continue;

			if (category.contains(" ")) {
				category.replaceAll(" ", "_");
			}
			if (category.contains(",")) {
				category.replaceAll(",", "");
			}
			String new_filepath = filepath + "/" + category + ".txt";

			List<Article> aux_list = new ArrayList<>(categories_list.get(category));
			aux_list.sort(Comparator.comparing(Article::getUuid));

			// urmeaza partea de printare in fisierul de output
		}
	}

	public void add_categories(String file_path) {
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int lines = Integer.parseInt(br.readLine());
			for (int i = 0; i < lines; i++) {
				String line = br.readLine();
				update_categories(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<String, Set<Article>> getCategories_list() {
		return categories_list;
	}

	// This part is for languages, adding a new language to the database
	// and adding an article / removing an article from a language list
	// Also, printing the languages

	public void update_languages(String lannguage) {
		languages_list.putIfAbsent(lannguage, ConcurrentHashMap.newKeySet());
	}

	public void add_article_to_language(Article article) {
		if (check_if_duplicated(article)) {
			if (!languages_list.containsKey(article.language)) return;
			languages_list.get(article.language).remove(article);
		}

		if (!languages_list.containsKey(article.language)) return;
		languages_list.get(article.language).add(article);
	}

	public void print_languages() {
		String filepath = "../output/languages";
		for (String language : languages_list.keySet()) {
			if (languages_list.get(language).isEmpty()) continue;

			String new_filepath = filepath + "/" + language + ".txt";

			List<Article> aux_list = new ArrayList<>(languages_list.get(language));
			aux_list.sort(Comparator.comparing(Article::getUuid));

			// urmeaza partea de printare in fisierul de output
		}
	}

	// This part is for the most recent articles -> will print the content of the list
	// in "all_articles.txt"

	public void update_most_recent_article(Article article) {
		most_recent_articles.put(date_format.format(article.getPublished()), article.getUuid());
	}

	public void print_most_recent_articles() {
		if (most_recent_articles.isEmpty()) return;

		List<Map.Entry<String, String>> aux_list = new ArrayList<>(most_recent_articles.entrySet());
		aux_list.sort(Comparator.comparing(Map.Entry<String, String>::getKey).thenComparing(Map.Entry::getValue));

		for (Map.Entry<String, String> date : aux_list) {
			System.out.println(date.getValue() + " " + date_format.format(date.getKey()));
		}
	}

	public void update_authors(String author) {
		if (!authors.containsKey(author)) {
			authors.put(author, Integer.valueOf(1));
		} else {
			int count = authors.get(author);
			count++;
			authors.put(author, Integer.valueOf(count));
		}
	}

	public int get_max_authors() {
		int max = 0;
		for (int i : authors.values()) {
			max = Math.max(max, i);
		}
		return max;
	}

	// This part is for parsing the input file, which contains all the paths
	// to the files of interest

	public void parse_file(String file_path) {
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int count = Integer.parseInt(br.readLine());

			String line;

			for (int i = 0; i < count; i++) {
				line = br.readLine();

				if (line.contains("languages")) {

				} else if (line.contains("categories")) {
					add_categories(line);
				} else if (line.contains("english_linking_words")) {

				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
