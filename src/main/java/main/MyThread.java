package main;

import java.io.File;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import tools.jackson.core.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;

public class MyThread extends Thread{
	public int P;
	public int id;
	List<String> articles_list;
	Statistics instance;
	CyclicBarrier barrier;

	public List<Article> articles_received;

	public MyThread(int P, List<String> articles_list, int id, Statistics instance, CyclicBarrier br) {
		this.P = P;
		this.articles_list = articles_list;
		this.id = id;
		this.instance = instance;
		barrier = br;
		articles_received = new ArrayList<>();
	}

	@Override
	public void run() {
		try {
			int start = id * articles_list.size() / P;
			int end = (id + 1) * articles_list.size() / P;

			if (end > articles_list.size()) {
				end = articles_list.size();
			}

			ObjectMapper mapper = new ObjectMapper();

			for (int i = start; i < end; i++) {
				File json = new File(articles_list.get(i));

				List<Article> articles = mapper.readValue(json,
						new TypeReference<>() {});

				for (Article article : articles) {
					boolean dup = instance.check_if_duplicated(article);

					if (dup) {
						Article aux = instance.found_match(article);
						if (aux != null) articles_received.remove(aux);
					} else {
						articles_received.add(article);
					}
				}
			}
			barrier.await();
			for (int i = 0; i < articles_received.size(); i++) {
				instance.add_article_to_category(articles_received.get(i));
				instance.add_article_to_language(articles_received.get(i));
				instance.add_most_recent_article(articles_received.get(i));
				instance.add_article_top_keyword(articles_received.get(i));
			}

			barrier.await();
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
