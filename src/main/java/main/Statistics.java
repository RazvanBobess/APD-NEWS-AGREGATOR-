package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
	public final AtomicInteger duplicates_found;
	public final AtomicInteger unique_articles;

	public final Map<String, Article> articles_received;

	public final Map<String, String> seen_titles;
	public final Map<String, Article> seen_uuids;

	public final List<String> valid_categories;
	public final List<String> valid_languages;

	public final Map<String, Set<String>> categories_list;
	public final Map<String, Set<String>> languages_list;
	public final Map<String, Integer> top_keyword_en;
	public Map<String, Integer> authors;

	public final Map<String, String> most_recent_articles;
	public final Set<String> english_linking_words;

	public Statistics instance;

	public Statistics() {
		most_recent_articles = new ConcurrentHashMap<>();
		english_linking_words = ConcurrentHashMap.newKeySet();
		top_keyword_en = new ConcurrentHashMap<>();

		articles_received = new ConcurrentHashMap<>();
		seen_titles = new ConcurrentHashMap<>();
		seen_uuids = new ConcurrentHashMap<>();

		categories_list = new ConcurrentHashMap<>();
		valid_categories = new ArrayList<>();

		languages_list = new ConcurrentHashMap<>();
		valid_languages = new ArrayList<>();

		duplicates_found = new AtomicInteger(0);
		unique_articles = new AtomicInteger(0);

		authors = new ConcurrentHashMap<>();
	}

	public synchronized void merge_all_articles(Map<String, Article> aux_articles_list) {
		articles_received.putAll(aux_articles_list);
	}

	public synchronized void merge_threads_categories(Map<String, Set<String>> categories_aux_list) {
		for (Map.Entry<String, Set<String>> entry : categories_aux_list.entrySet()) {
			this.categories_list.merge(entry.getKey(), entry.getValue(), (set1, set2) -> {
				set1.addAll(set2);
				return set1;
			});
		}
	}

	public synchronized void merge_threads_languages(Map<String, Set<String>> languages_aux_list) {
		for (Map.Entry<String, Set<String>> entry : languages_aux_list.entrySet()) {
			this.languages_list.merge(entry.getKey(), entry.getValue(), (set1, set2) -> {
				set1.addAll(set2);
				return set1;
			});
		}
	}

	public synchronized void merge_threads_keywords(Map<String, Integer> keywords_aux_list) {
		for (Map.Entry<String, Integer> entry : keywords_aux_list.entrySet()) {
			this.top_keyword_en.merge(entry.getKey(), entry.getValue(), Integer::sum);
		}
	}

	public synchronized void merge_threads_recent_articles(Map<String, String> most_aux_recent_list) {
		this.most_recent_articles.putAll(most_aux_recent_list);
	}

	public synchronized void merge_threads_authors(Map<String, Integer> authors_aux_list) {
		for (Map.Entry<String, Integer> entry : authors_aux_list.entrySet()) {
			this.authors.merge(entry.getKey(), entry.getValue(), Integer::sum);
		}
	}

	public void set_unique_articles(AtomicInteger unique_articles) {
		this.unique_articles.set(unique_articles.get());
	}

	public void set_duplicates_found(AtomicInteger duplicates_found) {
		this.duplicates_found.set(duplicates_found.get());
	}

	public void print_categories() {
		for (String category : categories_list.keySet()) {
			if (categories_list.get(category).isEmpty()) continue;

			String output_category = category.replaceAll(" ", "_");
			output_category = output_category.replaceAll(",", "");

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

	public void add_categories(String file_path) {
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int lines = Integer.parseInt(br.readLine());
			for (int i = 0; i < lines; i++) {
				String line = br.readLine();
				categories_list.putIfAbsent(line, ConcurrentHashMap.newKeySet());
				valid_categories.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void add_languages(String file_path) {
		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			int count = Integer.parseInt(br.readLine());
			String line;

			for (int i = 0; i < count; i++) {
				line = br.readLine();
				languages_list.putIfAbsent(line, ConcurrentHashMap.newKeySet());
				valid_languages.add(line);
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

	public Set<String> parse_text(String text) {
		String []tokens = text.replaceAll("[^a-z\\s]", "").split("\\s+");
		Set<String> words = new HashSet<>();

		for (String token : tokens) {
			if (!english_linking_words.contains(token) && !token.isBlank()) {
				words.add(token);
			}
		}

		return words;
	}

	public Map.Entry<String, Integer> get_max_authors() {
		Map.Entry<String, Integer> max_entry = null;

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
		Map.Entry<String, Integer> top_keyword_en = sort_top_keyword_en_list().getFirst();
		Map.Entry<String, String> most_recent = sort_recent_articles_list().getFirst();

		Article recent_article = articles_received.get(most_recent.getKey());

		String output_category = top_cat.getKey().replaceAll(" ", "_");
		output_category = output_category.replaceAll(",", "");

		try (BufferedWriter bw = Files.newBufferedWriter(path)) {
			bw.write("duplicates_found - " + duplicates_found + "\n");
			bw.write("unique_articles - " + unique_articles + "\n");
			bw.write("best_author - " + max_entry.getKey() + " " + max_entry.getValue() + "\n");
			bw.write("top_language - " + top_lang.getKey() + " " + top_lang.getValue() + "\n");
			bw.write("top_category - " + output_category + " " + top_cat.getValue() + "\n");
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
