package ma.ac.fstt.scrapingwithselenium.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;

@Setter
@Getter
@Data
@Document(collection="articles")
public class Article {
	@Id
	private String id;
	String title;
	List<String> keywords;
	List<String> authors;
	Set<String> universeties;
	Set<String> countries;
	String year;
	String month;
	String journal;
	String doi;
	String issn;
}
