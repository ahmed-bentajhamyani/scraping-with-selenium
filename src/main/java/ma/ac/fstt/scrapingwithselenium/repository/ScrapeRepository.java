package ma.ac.fstt.scrapingwithselenium.repository;

import ma.ac.fstt.scrapingwithselenium.models.ArticleDetails;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ScrapeRepository extends MongoRepository<ArticleDetails, String>{
    boolean existsByTitle(String title);

    @Query("{issn:'?0'}")
    List<ArticleDetails> findAllByIssn(String issn);
}
