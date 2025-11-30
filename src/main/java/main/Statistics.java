package main;

import java.io.*;
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

	public Set<Article> duplicated_articles;
	public Set<Article> unique_articles_list;

	private final Map<String, String> seen_titles;

	private final Map<String, Set<String>> categories_list;
	private final Map<String, Set<String>> languages_list;
	private final Map<String, String> most_recent_articles;
	private final Set<String> english_linking_words;
	private final Map<String, Integer> top_keyword_en;

	public static volatile Statistics instance;

	private Statistics() {
		authors = new ConcurrentHashMap<>();
		most_recent_articles = new ConcurrentHashMap<>();
		english_linking_words = ConcurrentHashMap.newKeySet();
		top_keyword_en = new ConcurrentHashMap<>();

		duplicated_articles = ConcurrentHashMap.newKeySet();
		unique_articles_list = ConcurrentHashMap.newKeySet();
		seen_titles = new ConcurrentHashMap<>();

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

	public Set<Article> get_dupl_articles() {
		return duplicated_articles;
	}

	public Set<Article> get_unique_articles_list() {
		return unique_articles_list;
	}

	public Map<String, String> get_seen_titles() {
		return seen_titles;
	}

	public String found_match(Article article) {
		if (get_dupl_articles().contains(article))
			return article.uuid;

		if (get_unique_articles_list().contains(article))
			return article.uuid;

		String aux = get_seen_titles().get(article.title);
		if (aux != null) return aux;

		return null;
	}

	public boolean check_if_duplicated(Article article) {
		String aux = found_match(article);

		if (aux != null) {
			if (!get_dupl_articles().contains(article)) {
				get_dupl_articles().add(article);
				get_unique_articles().decrementAndGet();
				get_unique_articles_list().remove(article);
			}
			add_duplicates_found();
			return true;
		}

		get_unique_articles_list().add(article);
		get_unique_articles().incrementAndGet();
		get_seen_titles().put(article.title, article.uuid);
		return false;
	}

	// This part is for categories, adding a new category to the database
	// and adding an article / removing an article from a category
	// Also, printing the categories

	public Map<String, Set<String>> get_categories() {
		return categories_list;
	}

	public void update_categories(String category) {
		get_categories().putIfAbsent(category, ConcurrentHashMap.newKeySet());
	}

	public void add_article_to_category(Article article) {
		if (check_if_duplicated(article)) {
			for (String category : article.categories) {
				if (!get_categories().containsKey(category)) continue;

				String aux_uuid = found_match(article);
				get_categories().get(category).remove(aux_uuid);
			}
			return;
		}

		for (String category : article.categories) {
			if (!get_categories().containsKey(category)) continue;
			get_categories().get(category).add(article.getUuid());
		}
	}

	public void print_categories() {
		for (String category : get_categories().keySet()) {
			if (get_categories().get(category).isEmpty()) continue;

			if (category.contains(" ")) {
				category.replaceAll(" ", "_");
			}
			if (category.contains(",")) {
				category.replaceAll(",", "");
			}
			String new_filepath = category + ".txt";

			List<String> aux_list = new ArrayList<>(get_categories().get(category));
			aux_list.sort(Comparator.naturalOrder());

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new_filepath))) {
				for (String uuid : aux_list) {
					bw.write(uuid + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	// This part is for languages, adding a new language to the database
	// and adding an article / removing an article from a language list
	// Also, printing the languages

	public Map<String, Set<String>> get_languages() {
		return languages_list;
	}

	public void update_languages(String language) {
		get_languages().putIfAbsent(language, ConcurrentHashMap.newKeySet());
	}

	public void add_article_to_language(Article article) {
		if (check_if_duplicated(article)) {
			if (!get_languages().containsKey(article.language)) return;

			String aux_uuid = found_match(article);
			get_languages().get(article.language).remove(aux_uuid);
		}

		if (!get_languages().containsKey(article.language)) return;
		get_languages().get(article.language).add(article.getUuid());
	}

	public void print_languages() {
		for (String language : get_languages().keySet()) {
			if (get_languages().get(language).isEmpty()) continue;

			String new_filepath = language + ".txt";

			List<String> aux_list = new ArrayList<>(get_languages().get(language));
			aux_list.sort(Comparator.naturalOrder());

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new_filepath))) {
				for (String uuid : aux_list) {
					bw.write(uuid + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void add_languages(String file_path) {
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int count = Integer.parseInt(br.readLine());
			String line;

			for (int i = 0; i < count; i++) {
				line = br.readLine();
				update_languages(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// This part is for the most recent articles -> will print the content of the list
	// in "all_articles.txt"

	public Map<String, String> get_recent_articles() {
		return most_recent_articles;
	}

	public void update_most_recent_article(Article article) {
		get_recent_articles().put(date_format.format(article.getPublished()), article.getUuid());
	}

	public void print_most_recent_articles() {
		if (get_recent_articles().isEmpty()) return;

		List<Map.Entry<String, String>> aux_list = new ArrayList<>(get_recent_articles().entrySet());
		aux_list.sort((entry1, entry2) -> {
			int dateComp = entry2.getKey().compareTo(entry1.getKey());

			if (dateComp != 0) {
				return dateComp;
			}

			return entry1.getValue().compareTo(entry2.getValue());
		});

		String filepath = "all_articles.txt";

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath))) {
			for (Map.Entry<String, String> entry : aux_list) {
				bw.write(entry.getValue() + " " + entry.getKey() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// This part will be for creating the list with the most used English words
	// and printing it in "keywords_count.txt"

	public Set<String> get_linking_words() {
		return english_linking_words;
	}

	public void add_linking_word(String word) {
		get_linking_words().add(word);
	}

	public void read_linking_words(String filepath) {
		try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
			int count = Integer.parseInt(br.readLine());
			String line;

			for (int i = 0; i < count; i++) {
				line = br.readLine();
				add_linking_word(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean check_linking_word(String word) {
		return get_linking_words().contains(word);
	}

	public Map<String, Integer> get_top_keyword_en() {
		return top_keyword_en;
	}

	public void update_top_keyword_en(String word) {
		if (check_linking_word(word))
			return;

		get_top_keyword_en().put(word, get_top_keyword_en().getOrDefault(word, 0) + 1);
	}

	public void add_article_top_keyword(Article article) {
		if (!article.language.equals("english")) {
			return;
		}

		String text = article.text.toLowerCase();
		String []tokens = text.split(" ");

		for (String token : tokens) {
			String word = token.replaceAll("[^a-z]", "");

			if (!word.isEmpty()) {
				update_top_keyword_en(word);
			}
		}
	}

	public void print_top_keyword_en() {
		List<Map.Entry<String, Integer>> aux_list;
		aux_list = new ArrayList<>(get_top_keyword_en().entrySet());

		aux_list.sort((entry1, entry2) -> {
			int countComp = entry2.getValue().compareTo(entry1.getValue());

			if (countComp != 0) {
				return countComp;
			}

			return entry1.getKey().compareTo(entry2.getKey());
		});

		String filepath = "keywords_count.txt";

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath))) {
			for (Map.Entry<String, Integer> aux : aux_list) {
				bw.write(aux.getKey() + " " + aux.getValue() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
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
					add_languages(line);
				} else if (line.contains("categories")) {
					add_categories(line);
				} else if (line.contains("english_linking_words")) {
					read_linking_words(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
