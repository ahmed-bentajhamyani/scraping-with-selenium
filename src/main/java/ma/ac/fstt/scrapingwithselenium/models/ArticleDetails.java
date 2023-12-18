package ma.ac.fstt.scrapingwithselenium.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Setter
@Getter
@Data
@Document(collection="articles")
public class ArticleDetails {
	@Id
	private String id;
	String title;
	List<String> keywords;
	List<String> authors;
	List<String> universeties;
	List<String> countries;
	String year;
	String month;
	String journal;
	String doi;
	String issn;
	String quartile;
}
