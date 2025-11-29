import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Article {
	public String uuid;
	public String title;
	public String url;
	public String text;
	public String published;
	public String language;
	public Set<String> categories;

	public Article(String uuid, String title, String url, String text, String published, String language) {
		this.uuid = uuid;
		this.title = title;
		this.url = url;
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
		return Objects.equals(uuid, article.uuid) ||
				Objects.equals(title, article.title);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, title);
	}

	public void addCategory(String category) {
		this.categories.add(category);
	}
}
