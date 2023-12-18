package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.ArticleDetails;
import ma.ac.fstt.scrapingwithselenium.repository.ScrapeRepository;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class QuartileServiceImpl implements QuartileService{

    @Autowired
    private Environment env;
    @Autowired
    private ScrapeRepository scrapeRepository;
    
    @Override
    public List<ArticleDetails> extractArticlesQuartile() {
        List<ArticleDetails> articles = scrapeRepository.findAll();

        if(articles.isEmpty()) {
            System.out.println("There is no new data");
            return null;
        } else {
            System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
            ChromeOptions options = new ChromeOptions();
            WebDriver driver = new ChromeDriver(options);

            articles.forEach(article -> {
                if(article.getQuartile() != null)
                    System.out.println("Quartile already exists for " + article.getIssn());
                else {
                    // System.out.println("Issn : " + article.getIssn());
                    this.loadPage(driver, "https://www.scimagojr.com/journalsearch.php?q=" + article.getIssn());

                    try {
                        WebElement element = driver.findElement(By.className("search_results"));
                        String url = element.findElement(By.tagName("a")).getAttribute("href");
                        // System.out.println(url);

                        this.loadPage(driver, url);

                        WebElement cellContainer = driver.findElement(By.className("cell100x1"));
                        WebElement table = cellContainer.findElement(By.tagName("table"));
                        List<WebElement> td = table.findElements(By.tagName("td"));

                        String quartile = td.get(2).getText();

                        List<ArticleDetails> articlesByIssn = scrapeRepository.findAllByIssn(article.getIssn());
                        articlesByIssn.forEach(articleByIssn -> {
                            System.out.println("Quartile " + quartile + " for " + articleByIssn.getIssn());

                            articleByIssn.setQuartile(quartile);
                            scrapeRepository.save(articleByIssn);
                        });
                    } catch (Exception e) {
                        System.out.println("Sorry, no results were found for " + article.getIssn());
                    }
                }
            });
        }
        return articles;
    }

    boolean loadPage(WebDriver driver, String url) {
        driver.get(url);
        try {
            Thread.sleep(1000);  // Let the user actually see something!
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
