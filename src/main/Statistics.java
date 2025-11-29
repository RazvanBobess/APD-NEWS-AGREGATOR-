import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// This class must be updated by one thread at a time

public class Statistics {
	private final AtomicInteger duplicates_found;
	private final AtomicInteger unique_articles;

	public Map<String, Integer> authors;
	public Map<Time, String> most_recent_articles;
	public Map<String, Integer> top_keyword_en;

	public Set<Article> duplicated_articles;
	public Set<Article> unique_articles_list;

	public Map<String, Set<Article>> categories_list;
	public Map<String, Set<Article>> languages_list;

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

	public int get_max_authors() {
		int max = 0;
		for (int i : authors.values()) {
			max = Math.max(max, i);
		}
		return max;
	}

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

	public void update_authors(String author) {
		if (!authors.containsKey(author)) {
			authors.put(author, 1);
		} else {
			int count = authors.get(author);
			count++;
			authors.put(author, count);
		}
	}

}
