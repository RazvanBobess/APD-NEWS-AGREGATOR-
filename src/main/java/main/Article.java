package main;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Article {
	public String uuid;
	public String url;
	public String author;
	public String title;
	public String text;
	public String published;
	public String language;
	public Set<String> categories;

	public Article(String uuid, String url, String author, String title, String text, String published, String language) {
		this.uuid = uuid;
		this.url = url;
		this.author = author;
		this.title = title;
		this.text = text;
		this.published = published;
		this.language = language;
		this.categories = new HashSet<>();
	}

	public String getUuid() {
		return uuid;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public String getAuthor() {
		return author;
	}

	public String getPublished() {
		return published;
	}

	public String getLanguage() {
		return language;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Article)) return false;
		Article article = (Article) o;
		return Objects.equals(uuid, article.uuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid);
	}

	public void addCategory(String category) {
		this.categories.add(category);
	}
}
