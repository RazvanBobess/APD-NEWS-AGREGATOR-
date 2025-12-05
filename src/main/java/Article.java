
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

	public Article() {}

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

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setPublished(String published) {
		this.published = published;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}
}
