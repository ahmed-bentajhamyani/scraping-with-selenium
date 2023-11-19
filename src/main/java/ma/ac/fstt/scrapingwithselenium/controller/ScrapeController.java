package ma.ac.fstt.scrapingwithselenium.controller;

import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.models.Response;
import ma.ac.fstt.scrapingwithselenium.service.ACMService;
import ma.ac.fstt.scrapingwithselenium.service.IeeeService;
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

    @GetMapping("/dataSD")
    public List<Response> getData(){
        return scienceDirectService.extractArticles();
    }

    @GetMapping("/sd-scraper")
    @Scheduled(cron = "0 0 12 * * 0")
    public List<Article> getDetails(){
        return scienceDirectService.extractArticlesDetails();
    }

    @GetMapping("/dataAcm")
    public List<Response> getDataAcm(){
        return acmService.extractArticles();
    }

    @GetMapping("/acm-scraper")
    @Scheduled(cron = "0 0 14 * * 0")
    public List<Article> getDetailsAcm(){
        return acmService.extractArticlesDetails();
    }

    @GetMapping("/dataIeee")
    public List<Response> getDataIeee(){
        return ieeeService.extractArticles();
    }

    @GetMapping("/ieee-scraper")
    @Scheduled(cron = "0 0 16 * * 0")
    public List<Article> getDetailsIeee(){
        return ieeeService.extractArticlesDetails();
    }
}
