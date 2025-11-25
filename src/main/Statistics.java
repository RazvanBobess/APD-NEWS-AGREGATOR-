import java.sql.Time;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// This class must be updated by one thread at a time

public class Statistics {
	private int duplicates_found;
	public int unique_articles;
	public Map<String, Integer> authors;
	public Map<String, Integer> languages;
	public Map<String, Integer> categories;
	public Map<Time, String> most_recent_articles;
	public Map<String, Integer> top_keyword_en;

	public Statistics() {
		authors = Collections.synchronizedMap(new HashMap<>());
		languages = Collections.synchronizedMap(new HashMap<>());
		categories = Collections.synchronizedMap(new HashMap<>());
		most_recent_articles = Collections.synchronizedMap(new HashMap<>());
		top_keyword_en = Collections.synchronizedMap(new HashMap<>());
	}

	public void add_duplicates_found() {
		duplicates_found++;
	}

	public int get_duplicates_found() {
		return duplicates_found;
	}

	public void update_unique_articles(int unique_articles) {
		this.unique_articles = unique_articles;
	}

	public int get_unique_articles() {
		return unique_articles;
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

	public int get_max_authors() {
		int max = 0;
		for (int i : authors.values()) {
			if (i > max) {
				max = i;
			}
		}
		return max;
	}
}
