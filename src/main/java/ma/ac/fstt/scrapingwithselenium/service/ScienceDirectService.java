package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.models.Response;

import java.util.List;

public interface ScienceDirectService {
    List<Response> extractArticles();
    List<Article> extractArticlesDetails();
}
