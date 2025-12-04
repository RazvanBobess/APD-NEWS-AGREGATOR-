package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
	private final AtomicInteger duplicates_found;
	private final AtomicInteger unique_articles;

	public Map<String, Integer> authors;

	public final Map<Article, Integer> articles_received;

	private final Map<String, String> seen_titles;
	private final Map<String, Article> seen_uuids;

	private final Map<String, Set<String>> categories_list;
	private final Map<String, Set<String>> languages_list;
	private final Map<String, String> most_recent_articles;
	private final Set<String> english_linking_words;
	private final Map<String, Integer> top_keyword_en;

	public static Statistics instance;

	private Statistics() {
		most_recent_articles = new ConcurrentHashMap<>();
		english_linking_words = ConcurrentHashMap.newKeySet();
		top_keyword_en = new ConcurrentHashMap<>();

		articles_received = new ConcurrentHashMap<>();
		seen_titles = new ConcurrentHashMap<>();
		seen_uuids = new ConcurrentHashMap<>();

		categories_list = new ConcurrentHashMap<>();
		languages_list = new ConcurrentHashMap<>();

		duplicates_found = new AtomicInteger(0);
		unique_articles = new AtomicInteger(0);

		authors = new ConcurrentHashMap<>();
	}

	public static Statistics getInstance() {
		if (instance == null) {
			instance = new Statistics();
		}
		return instance;
	}

	public Article found_match(Article article) {
		if (articles_received.containsKey(article)) {
			return article;
		}

		String aux = seen_titles.get(article.title);
		if (aux == null) return null;

		return seen_uuids.get(aux);
	}

	public synchronized boolean check_if_duplicated(Article article) {
		Article aux = found_match(article);

		if (aux != null) {
			if (articles_received.get(aux) == 1) {
				articles_received.put(aux, 2);
				duplicates_found.incrementAndGet();
				unique_articles.decrementAndGet();
			} else {
				articles_received.put(aux, articles_received.get(aux) + 1);
			}
			duplicates_found.incrementAndGet();
			return true;
		}

		articles_received.put(article, 1);
		unique_articles.incrementAndGet();
		seen_titles.put(article.title, article.uuid);
		seen_uuids.put(article.uuid, article);
		return false;
	}

	public synchronized void add_article_to_category(Article article) {
		if (articles_received.get(article) >= 2) return;

		for (String category : article.categories) {
			if (!categories_list.containsKey(category)) continue;
			categories_list.get(category).add(article.getUuid());
		}
	}

	public String normalize_category(String category) {
		category = category.replaceAll(" ", "_");
		category = category.replaceAll(",", "");

		return category;
	}

	public void print_categories() {
		for (String category : categories_list.keySet()) {
			if (categories_list.get(category).isEmpty()) continue;

			String output_category = normalize_category(category);

			Path path = Paths.get(output_category + ".txt");

			List<String> aux_list = new ArrayList<>(categories_list.get(category));
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
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int lines = Integer.parseInt(br.readLine());
			for (int i = 0; i < lines; i++) {
				String line = br.readLine();
				categories_list.putIfAbsent(line, ConcurrentHashMap.newKeySet());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void add_article_to_language(Article article) {
		if (articles_received.get(article) >= 2) return;

		if (!languages_list.containsKey(article.language)) return;
		languages_list.get(article.language).add(article.getUuid());
	}

	public void print_languages() {
		for (String language : languages_list.keySet()) {
			if (languages_list.get(language).isEmpty()) continue;

			List<String> aux_list = new ArrayList<>(languages_list.get(language));
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
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int count = Integer.parseInt(br.readLine());
			String line;

			for (int i = 0; i < count; i++) {
				line = br.readLine();
				languages_list.putIfAbsent(line, ConcurrentHashMap.newKeySet());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void add_most_recent_article(Article article) {
		int count = articles_received.get(article);

		if (count == 1) {
			most_recent_articles.put(article.getUuid(), article.getPublished());
		}
	}

	public List<Map.Entry<String, String>> sort_recent_articles_list() {
		List<Map.Entry<String, String>> sort_list = new ArrayList<>(most_recent_articles.entrySet());
		sort_list.sort((entry1, entry2) -> {
			int dateComp = entry2.getValue().compareTo(entry1.getValue());

			if (dateComp != 0) {
				return dateComp;
			}

			return entry1.getKey().compareTo(entry2.getKey());
		});

		return sort_list;
	}

	public void print_most_recent_articles() {
		if (most_recent_articles.isEmpty()) return;

		List<Map.Entry<String, String>> aux_list = sort_recent_articles_list();

		String filepath = "all_articles.txt";
		Path path = Paths.get(filepath);

		try (BufferedWriter bw = Files.newBufferedWriter(path)) {
			for (Map.Entry<String, String> entry : aux_list) {
				bw.write(entry.getKey() + " " + entry.getValue() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void read_linking_words(String filepath) {
		try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
			int count = Integer.parseInt(br.readLine());
			String line;

			for (int i = 0; i < count; i++) {
				line = br.readLine();
				english_linking_words.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Set<String> parse_text(String text) {
		String []tokens = text.split(" ");
		Set<String> words = new HashSet<>();

		for (String token : tokens) {
			String word = token.replaceAll("[^a-z]", "");
			if (!word.isEmpty()) words.add(word);
		}

		return words;
	}

	public synchronized void add_article_top_keyword(Article article) {
		if (articles_received.get(article) >= 2) return;

		if (!article.language.equals("english")) {
			return;
		}

		Set<String> aux_tokens = parse_text(article.text.toLowerCase());

		for (String token : aux_tokens) {
			if (english_linking_words.contains(token)) continue;
			top_keyword_en.put(token, top_keyword_en.getOrDefault(token, 0) + 1);
		}
	}

	public List<Map.Entry<String, Integer>> sort_top_keyword_en_list() {
		List<Map.Entry<String, Integer>> sort_list = new ArrayList<>(top_keyword_en.entrySet());

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
		for (Map.Entry<Article, Integer> entry : articles_received.entrySet()) {
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

		for (Map.Entry<String, Set<String>> entry : languages_list.entrySet()) {
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

		for (Map.Entry<String, Set<String>> entry : categories_list.entrySet()) {
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

		Article recent_article = seen_uuids.get(most_recent.getKey());

		Map.Entry<String, Integer> top_keyword_en = sort_top_keyword_en_list().getFirst();

		try (BufferedWriter bw = Files.newBufferedWriter(path)) {
			bw.write("duplicates_found - " + duplicates_found + "\n");
			bw.write("unique_articles - " + unique_articles + "\n");
			bw.write("best_author - " + max_entry.getKey() + " " + max_entry.getValue() + "\n");
			bw.write("top_language - " + top_lang.getKey() + " " + top_lang.getValue() + "\n");
			bw.write("top_category - " + normalize_category(top_cat.getKey()) + " " + top_cat.getValue() + "\n");
			bw.write("most_recent_article - " + most_recent.getValue() + " " + recent_article.getUrl() + "\n");
			bw.write("top_keyword_en - " + top_keyword_en.getKey() + " " + top_keyword_en.getValue() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String extract_file_path(String line) {
		String[] tokens = line.split(" ");
		return tokens[tokens.length - 1];
	}

	public void parse_file(String file_path) {
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
}
