package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.ArticleDetails;
import ma.ac.fstt.scrapingwithselenium.models.Article;

import java.util.List;

public interface IeeeService {
    List<Article> extractArticles();
    List<ArticleDetails> extractArticlesDetails();
}
