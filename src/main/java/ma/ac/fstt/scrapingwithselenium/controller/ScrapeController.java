package ma.ac.fstt.scrapingwithselenium.controller;

import ma.ac.fstt.scrapingwithselenium.models.ArticleDetails;
import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.service.ACMService;
import ma.ac.fstt.scrapingwithselenium.service.IeeeService;
import ma.ac.fstt.scrapingwithselenium.service.QuartileService;
import ma.ac.fstt.scrapingwithselenium.service.ScienceDirectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScrapeController {

    @Autowired
    ScienceDirectService scienceDirectService;
    @Autowired
    ACMService acmService;
    @Autowired
    IeeeService ieeeService;
    @Autowired
    QuartileService quartileService;

    @GetMapping("/sciencedirect")
    public List<Article> getDataSD(){
        return scienceDirectService.extractArticles();
    }

    @GetMapping("/sd-scraper")
    @Scheduled(cron = "0 0 12 * * 0")
    public List<ArticleDetails> getDetailsSD(){
        return scienceDirectService.extractArticlesDetails();
    }

    @GetMapping("/acm")
    public List<Article> getDataAcm(){
        return acmService.extractArticles();
    }

    @GetMapping("/acm-scraper")
    @Scheduled(cron = "0 0 14 * * 0")
    public List<ArticleDetails> getDetailsAcm(){
        return acmService.extractArticlesDetails();
    }

    @GetMapping("/ieee")
    public List<Article> getDataIeee(){
        return ieeeService.extractArticles();
    }

    @GetMapping("/ieee-scraper")
    @Scheduled(cron = "0 0 16 * * 0")
    public List<ArticleDetails> getDetailsIeee(){
        return ieeeService.extractArticlesDetails();
    }

    @GetMapping("/article-quartile")
    @Scheduled(cron = "0 0 16 * * 0")
    public List<ArticleDetails> getArticlesQuartile(){
        return quartileService.extractArticlesQuartile();
    }
}
