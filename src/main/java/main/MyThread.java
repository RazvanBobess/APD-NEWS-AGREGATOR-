package main;

import java.io.File;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;

public class MyThread extends Thread{
	public int P;
	public int id;
	List<String> articles_list;
	public static Statistics instance;
	CyclicBarrier barrier;

	public List<Article> articles_received;
	public Map<String, Article> articles_map;

	public static Map<String, Integer> seen_titles;
	public static Map<String, Integer> seen_uuids;

	public Map<String, Set<String>> categories_list;
	public Map<String, Set<String>> languages_list;
	public Map<String, Integer> keywords_list;
	public Map<String, String> most_recent_articles;
	public Map<String, Integer> authors_list;

	public static AtomicInteger unique_articles;
	public static AtomicInteger duplicates_global;

	public MyThread(int P, List<String> articles_list, int id, Statistics insta, CyclicBarrier br) {
		this.P = P;
		this.articles_list = articles_list;
		this.id = id;
		this.barrier = br;
		instance = insta;

		articles_received = new ArrayList<>();

		seen_titles = new ConcurrentHashMap<>();
		seen_uuids = new ConcurrentHashMap<>();
		articles_map = new HashMap<>();

		unique_articles = new AtomicInteger(0);
		duplicates_global = new AtomicInteger(0);

		categories_list = new HashMap<>();
		languages_list = new HashMap<>();
		keywords_list = new TreeMap<>();
		most_recent_articles = new HashMap<>();
		authors_list = new HashMap<>();
	}

	public void add_article(Article article) {
		if (seen_titles.containsKey(article.title) || seen_uuids.containsKey(article.uuid)) {
			if (seen_uuids.containsKey(article.uuid)) seen_uuids.put(article.uuid, seen_uuids.get(article.uuid) + 1);
			else seen_titles.put(article.title, seen_titles.get(article.title) + 1);
			articles_received.add(article);
			return;
		}

		seen_titles.put(article.title, 1);
		seen_uuids.put(article.uuid, 1);
		articles_received.add(article);
	}

	public boolean check_if_dup(Article article) {
		if (!seen_titles.containsKey(article.title) || !seen_uuids.containsKey(article.uuid)) return true;

		if (seen_titles.get(article.title) > 1 || seen_uuids.get(article.uuid) > 1) return true;

		return false;
	}

	@Override
	public void run() {
		try {
			int start = id * articles_list.size() / P;
			int end = (id + 1) * articles_list.size() / P;

			for (int i = start; i < end; i++) {
				ObjectMapper mapper = new ObjectMapper();
				File json = new File(articles_list.get(i));

				List<Article> articles = mapper.readValue(json,
						new TypeReference<>() {});

				articles.forEach(article -> add_article(article));
			}
			this.barrier.await();

			articles_received.forEach(article -> {
				if (check_if_dup(article)) {
					duplicates_global.incrementAndGet();
					return;
				}

				unique_articles.incrementAndGet();
				articles_map.put(article.uuid, article);

				for (String category : article.categories) {
					if (!instance.valid_categories.contains(category)) continue;

					categories_list.putIfAbsent(category, new HashSet<>());
					categories_list.get(category).add(article.uuid);
				}

				if (instance.valid_languages.contains(article.language)) {
					languages_list.putIfAbsent(article.language, new HashSet<>());
					languages_list.get(article.language).add(article.uuid);

					if (article.language.equals("english")) {
						Set<String> eng_words = instance.parse_text(article.text.toLowerCase());

						for (String word : eng_words) {
							if (instance.english_linking_words.contains(word)) continue;

							if (!keywords_list.containsKey(word)) keywords_list.put(word, 1);
							else keywords_list.put(word, keywords_list.get(word) + 1);
						}
					}
				}

				most_recent_articles.put(article.uuid, article.published);

				if (!authors_list.containsKey(article.author)) {
					authors_list.put(article.author, 1);
				} else {
					authors_list.put(article.author, authors_list.get(article.author) + 1);
				}
			});
			this.barrier.await();

			instance.merge_threads_categories(categories_list);
			instance.merge_threads_languages(languages_list);
			instance.merge_threads_keywords(keywords_list);
			instance.merge_threads_recent_articles(most_recent_articles);
			instance.merge_threads_authors(authors_list);
			instance.set_unique_articles(unique_articles);
			instance.set_duplicates_found(duplicates_global);
			instance.merge_all_articles(articles_map);

			this.barrier.await();
			if (id == 0) {
				instance.print_categories();
				instance.print_languages();
				instance.print_most_recent_articles();
				instance.print_top_keyword_en();
				instance.print_reports();
			}
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
}
