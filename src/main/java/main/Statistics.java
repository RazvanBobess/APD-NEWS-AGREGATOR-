package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
	private final AtomicInteger duplicates_found;
	private final AtomicInteger unique_articles;

	public Map<String, Integer> authors;

	// daca e duplicat, adaug un contor care verifica daca am gasit un dup sau nu

	public final Map<Article, Integer> articles_received;

	private final Map<String, String> seen_titles;

	private final Map<String, Set<String>> categories_list;
	private final Map<String, Set<String>> languages_list;
	private final Map<String, String> most_recent_articles;
	private final Set<String> english_linking_words;
	private final Map<String, Integer> top_keyword_en;

	public static volatile Statistics instance;

	private Statistics() {
		most_recent_articles = new ConcurrentHashMap<>();
		english_linking_words = ConcurrentHashMap.newKeySet();
		top_keyword_en = new ConcurrentHashMap<>();

		articles_received = new ConcurrentHashMap<>();
		seen_titles = new ConcurrentHashMap<>();

		categories_list = new ConcurrentHashMap<>();
		languages_list = new ConcurrentHashMap<>();

		duplicates_found = new AtomicInteger(0);
		unique_articles = new AtomicInteger(0);

		authors = new ConcurrentHashMap<>();
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

	public Map<Article, Integer> get_articles_received() {
		return articles_received;
	}

	public Map<String, String> get_seen_titles() {
		return seen_titles;
	}

	public Article get_article_by_uuid(String uuid) {
		Set<Article> aux_set = get_articles_received().keySet();

		return aux_set.stream().filter(art -> art.getUuid().equals(uuid)).findFirst().orElse(null);
	}

	public Article found_match(Article article) {
		if (get_articles_received().containsKey(article)) {
			return article;
		}

		String aux = get_seen_titles().get(article.title);
		Article aux_article = get_articles_received().keySet().stream().filter(art -> art.getUuid().equals(aux)).findFirst().orElse(null);
		if (aux_article != null) return aux_article;

		return null;
	}

	public boolean check_if_duplicated(Article article) {
		Article aux = found_match(article);

		if (aux != null) {
			if (get_articles_received().get(aux) == 1) {
				get_articles_received().put(aux, 2);
				add_duplicates_found();
				get_unique_articles().decrementAndGet();
			} else {
				get_articles_received().put(aux, get_articles_received().get(aux) + 1);
			}
			add_duplicates_found();
			return true;
		}

		get_articles_received().put(article, 1);
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

	public void remove_article_from_category(Article article) {
		Article aux_article = found_match(article);

		if (aux_article == null) return;

		if (get_articles_received().get(aux_article) > 2) return;

		for (String category : aux_article.categories) {
			if (!get_categories().containsKey(category)) continue;
			get_categories().get(category).remove(aux_article.getUuid());
		}
	}

	public void add_article_to_category(Article article) {
		if (get_articles_received().get(article) >= 2) return;

		for (String category : article.categories) {
			if (!get_categories().containsKey(category)) continue;
			get_categories().get(category).add(article.getUuid());
		}
	}

	public String normalize_category(String category) {
		category.replaceAll(" ", "_");
		category.replaceAll(",", "");

		return category;
	}

	public void print_categories() {
		for (String category : get_categories().keySet()) {
			if (get_categories().get(category).isEmpty()) continue;

			category = normalize_category(category);

			Path path = Paths.get(category + ".txt");

			List<String> aux_list = new ArrayList<>(get_categories().get(category));
			aux_list.sort(Comparator.naturalOrder());

			try (BufferedWriter bw = Files.newBufferedWriter(path)) {
				for (String uuid : aux_list) {
					bw.write(uuid + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void add_categories(String file_path) {
		logPath(file_path);
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

	public void remove_article_from_language(Article article) {
		Article aux_article = found_match(article);

		if (aux_article == null) {
			logPath("No article found for " + article.getUuid());
			return;
		}

		if (get_articles_received().get(aux_article) > 2) return;

		if (!get_languages().containsKey(article.language)) return;
		get_languages().get(article.language).remove(aux_article.getUuid());
	}

	public void add_article_to_language(Article article) {
		if (get_articles_received().get(article) >= 2) return;

		if (!get_languages().containsKey(article.language)) return;
		get_languages().get(article.language).add(article.getUuid());
	}

	public void print_languages() {
		for (String language : get_languages().keySet()) {
			if (get_languages().get(language).isEmpty()) continue;

			List<String> aux_list = new ArrayList<>(get_languages().get(language));
			aux_list.sort(Comparator.naturalOrder());

			Path path = Paths.get(language + ".txt");

			try (BufferedWriter bw = Files.newBufferedWriter(path)) {
				for (String uuid : aux_list) {
					bw.write(uuid + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void add_languages(String file_path) {
		logPath(file_path);
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

	public void remove_article_from_recent_articles(Article article) {
		Article aux_article = found_match(article);

		if (aux_article == null) return;

		if (get_articles_received().get(aux_article) > 2) return;

		get_recent_articles().remove(aux_article.getPublished());
	}

	public void add_most_recent_article(Article article) {
		if (get_articles_received().get(article) >= 2) return;

		get_recent_articles().put(article.getPublished(), article.getUuid());
	}

	public List<Map.Entry<String, String>> sort_recent_articles_list() {
		List<Map.Entry<String, String>> sort_list = new ArrayList<>(get_recent_articles().entrySet());
		sort_list.sort((entry1, entry2) -> {
			int dateComp = entry2.getKey().compareTo(entry1.getKey());

			if (dateComp != 0) {
				return dateComp;
			}

			return entry1.getValue().compareTo(entry2.getValue());
		});

		return sort_list;
	}

	public void print_most_recent_articles() {
		if (get_recent_articles().isEmpty()) return;

		List<Map.Entry<String, String>> aux_list = sort_recent_articles_list();

		String filepath = "all_articles.txt";
		Path path = Paths.get(filepath);

		try (BufferedWriter bw = Files.newBufferedWriter(path)) {
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
		logPath(filepath);
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

	public void remove_article_top_keyword(Article article) {
		Article aux_article = found_match(article);

		if (aux_article == null) return;

		if (!aux_article.language.equals("english")) return;

		if (get_articles_received().get(aux_article) > 2) return;

		String text = aux_article.text.toLowerCase();
		String []tokens = text.split(" ");

		for (String token : tokens) {
			String word = token.replaceAll("[^a-z]", "");

			if (!word.isEmpty()) {
				get_top_keyword_en().put(word, get_top_keyword_en().getOrDefault(word, 0) - 1);
			}
		}
	}

	public void add_article_top_keyword(Article article) {
		if (get_articles_received().get(article) >= 2) return;

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

	public List<Map.Entry<String, Integer>> sort_top_keyword_en_list() {
		List<Map.Entry<String, Integer>> sort_list = new ArrayList<>(get_top_keyword_en().entrySet());
		sort_list.sort((entry1, entry2) -> {
			int countComp = entry2.getValue().compareTo(entry1.getValue());

			if (countComp != 0) {
				return countComp;
			}

			return entry1.getKey().compareTo(entry2.getKey());
		});

		return sort_list;
	}

	public void print_top_keyword_en() {
		List<Map.Entry<String, Integer>> aux_list = sort_top_keyword_en_list();

		String filepath = "keywords_count.txt";
		Path path = Paths.get(filepath);

		try (BufferedWriter bw = Files.newBufferedWriter(path)) {
			for (Map.Entry<String, Integer> aux : aux_list) {
				if (aux.getValue() < 1) continue;
				bw.write(aux.getKey() + " " + aux.getValue() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// This part will print the other reports in "reports.txt"

	public void add_authors(String author) {
		if (!authors.containsKey(author)) {
			authors.put(author, 1);
		} else {
			int count = authors.get(author);
			count++;
			authors.put(author, count);
		}
	}

	public void update_authors() {
		for (Map.Entry<Article, Integer> entry : get_articles_received().entrySet()) {
			if (entry.getValue() > 1) continue;
			add_authors(entry.getKey().author);
		}
	}

	public Map.Entry<String, Integer> get_max_authors() {

		Map.Entry<String, Integer> max_entry = null;

		update_authors();

		for (Map.Entry<String, Integer> entry : authors.entrySet()) {
			if (max_entry == null || entry.getValue() > max_entry.getValue()) {
				max_entry = entry;
			}
			if (Objects.equals(entry.getValue(), max_entry.getValue()) && entry.getKey().compareTo(max_entry.getKey()) < 0) {
				max_entry = entry;
			}
		}

		return max_entry;
	}

	public Map.Entry<String, Integer> get_top_language() {
		Map.Entry<String, Integer> max_entry = null;

		for (Map.Entry<String, Set<String>> entry : get_languages().entrySet()) {
			if (max_entry == null || entry.getValue().size() > max_entry.getValue()) {
				max_entry = Map.entry(entry.getKey(), entry.getValue().size());
			}
			if (Objects.equals(entry.getValue().size(), max_entry.getValue()) && entry.getKey().compareTo(max_entry.getKey()) < 0) {
				max_entry = Map.entry(entry.getKey(), entry.getValue().size());
			}
		}

		return max_entry;
	}

	public Map.Entry<String, Integer> get_top_category() {
		Map.Entry<String, Integer> max_entry = null;

		for (Map.Entry<String, Set<String>> entry : get_categories().entrySet()) {
			if (max_entry == null || entry.getValue().size() > max_entry.getValue()) {
				max_entry = Map.entry(entry.getKey(), entry.getValue().size());
			}
			if (Objects.equals(entry.getValue().size(), max_entry.getValue()) && entry.getKey().compareTo(max_entry.getKey()) < 0) {
				max_entry = Map.entry(entry.getKey(), entry.getValue().size());
			}
		}

		return max_entry;
	}

	public void print_reports() {
		String filepath = "reports.txt";
		Path path = Paths.get(filepath);

		Map.Entry<String, Integer> max_entry = get_max_authors();
		Map.Entry<String, Integer> top_lang = get_top_language();
		Map.Entry<String, Integer> top_cat = get_top_category();
		Map.Entry<String, String> most_recent = sort_recent_articles_list().getFirst();

		Article recent_article = get_article_by_uuid(most_recent.getValue());

		Map.Entry<String, Integer> top_keyword_en = sort_top_keyword_en_list().getFirst();

		try (BufferedWriter bw = Files.newBufferedWriter(path)) {
			bw.write("duplicates_found - " + get_duplicates_found() + "\n");
			bw.write("unique_articles - " + get_unique_articles() + "\n");
			bw.write("best_author - " + max_entry.getKey() + " " + max_entry.getValue() + "\n");
			bw.write("top_language - " + top_lang.getKey() + " " + top_lang.getValue() + "\n");
			bw.write("top_category - " + normalize_category(top_cat.getKey()) + " " + top_cat.getValue() + "\n");
			bw.write("most_recent_article - " + most_recent.getKey() + " " + recent_article.getUrl() + "\n");
			bw.write("top_keyword_en - " + top_keyword_en.getKey() + " " + top_keyword_en.getValue() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// This part is for parsing the input file, which contains all the paths
	// to the files of interest

	private String extract_file_path(String line) {
		String[] tokens = line.split(" ");
		return tokens[tokens.length - 1];
	}

	public void parse_file(String file_path) {
		logPath(file_path);

		Path base_dir = Paths.get(file_path).getParent();

		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int count = Integer.parseInt(br.readLine());

			String line;

			for (int i = 0; i < count; i++) {
				line = br.readLine();

				if (line.contains("languages")) {
					String relative_path = extract_file_path(line);
					String abs_path = base_dir.resolve(relative_path).toString();
					add_languages(abs_path);
				} else if (line.contains("categories")) {
					String relative_path = extract_file_path(line);
					String abs_path = base_dir.resolve(relative_path).toString();
					add_categories(abs_path);
				} else if (line.contains("english_linking_words")) {
					String relative_path = extract_file_path(line);
					String abs_path = base_dir.resolve(relative_path).toString();
					read_linking_words(abs_path);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static final String PATH_TO_LOG = System.getProperty("user.dir") + "/logs.log";
	private final Object lock = new Object();

	public void logPath(String path) {
		synchronized (lock) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(PATH_TO_LOG, true))) {
				bw.write(path);
				bw.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
