package ma.ac.fstt.scrapingwithselenium.repository;

import ma.ac.fstt.scrapingwithselenium.models.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScrapeRepository extends MongoRepository<Article, String>{
    boolean existsByTitle(String title);
}
