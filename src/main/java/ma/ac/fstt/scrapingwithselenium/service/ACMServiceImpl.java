package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.ArticleDetails;
import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.repository.ScrapeRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ACMServiceImpl implements ACMService {

    @Autowired
    private Environment env;
    @Autowired
    private ScrapeRepository scrapeRepository;

    @Override
    public List<Article> extractArticles() {
        System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        List<Article> links = new ArrayList<Article>();
        List<String> urls = new ArrayList<String>();

        List<String> offsets = Arrays.asList("23", "24", "25", "26", "27", "28", "29", "30", "31", "32");
        for(int i=0; i<offsets.size(); i++) {
            urls.add("https://dl.acm.org/action/doSearch?AllField=Blockchain&startPage="+offsets.get(i)+"&pageSize=50");
        }
        for(int j=0; j<urls.size(); j++) {
            this.loadPage(driver,urls.get(j));

            List<WebElement> elements = driver.findElements(By.className("hlFld-Title"));

            elements.forEach(element -> {
                Article res = new Article();
                if(this.exists(element.getText())) {
                    System.out.println("Already exists in DB");
                }else {
                    if(driver.findElement(By.className("issue-heading")).getText().contains("RESEARCH-ARTICLE")) {
                        res.setTitle(element.getText());
                        res.setUrl(element.findElement(By.tagName("a")).getAttribute("href"));
                        links.add(res);
                    }
                }
            });
        }
        driver.close();
        return links;
    }

    @Override
    public List<ArticleDetails> extractArticlesDetails() {
        List<Article> links = this.extractArticles();

        if(links.isEmpty()) {
            System.out.println("There is no new data");
            return null;
        } else {

            System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
            ChromeOptions options = new ChromeOptions();
            WebDriver driver = new ChromeDriver(options);

            List<String> urls = new ArrayList<String>();
            List<ArticleDetails> articles = new ArrayList<ArticleDetails>();

            links.forEach(link -> {
                this.loadPage(driver, link.getUrl());
                urls.add(link.getUrl());

                ArticleDetails articleDetails = new ArticleDetails();
                articleDetails.setTitle(link.getTitle());

                List<String> univs = new ArrayList<String>();
                List<String> auths = new ArrayList<String>();
                List<String> keywords = new ArrayList<String>();
                List<String> coutries = new ArrayList<String>();

                try {
                    WebElement date = driver.findElement(By.className("epub-section__date"));
                    String[] dates = date.getText().split(" ");
                    if (dates.length > 0) articleDetails.setYear(dates[dates.length - 1].trim());
                    if (dates.length > 1) articleDetails.setMonth(dates[dates.length - 2].trim());
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    WebElement doi = driver.findElement(By.className("issue-item__doi"));
                    articleDetails.setDoi(doi.getText());
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    List<WebElement> authors = driver.findElements(By.className("loa__author-name"));
                    authors.forEach(author -> {
                        auths.add(author.findElement(By.tagName("span")).getText());
                    });
                    articleDetails.setAuthors(auths);
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    WebElement button = driver.findElement(By.className("loa__link"));
                    button.sendKeys(Keys.ENTER);
                    Thread.sleep(10000);
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    List<WebElement> universeties = driver.findElements(By.className("auth-info"));
                    universeties.forEach(unv -> {
                        if(unv.findElements(By.tagName("span")).size() >1) {
                            univs.add(unv.findElements(By.tagName("span")).get(1).getText());
                        }else {
                            univs.add(unv.findElement(By.tagName("span")).findElement(By.tagName("p")).getText());
                        }
                    });
                    articleDetails.setUniverseties(univs);

                    univs.forEach(univ -> {
                        String[] parts = univ.split(",");
                        if(parts.length > 0) coutries.add(parts[parts.length - 1].trim());
                    });
                    articleDetails.setCountries(coutries);
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    WebElement button2 = driver.findElement(By.id("pill-information__contentcon"));
                    button2.sendKeys(Keys.ENTER);
                    Thread.sleep(10000);
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    List<WebElement> flexContainers = driver.findElement(By.className("cover-image__details-extra"))
                            .findElements(By.className("flex-container"));

                    if (!flexContainers.isEmpty()) {
                        WebElement issn = flexContainers.get(0).findElement(By.className("space"));
                        articleDetails.setIssn(issn.getText());
                    } else {
                        System.out.println("No flex containers found");
                    }
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    List<WebElement> elms = driver.findElements(By.className("badge-type"));
                    elms.forEach(elm -> {
                        keywords.add(elm.getText());
                    });
                    articleDetails.setKeywords(keywords);
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                articleDetails.setJournal("ACM");

                articles.add(articleDetails);
                scrapeRepository.save(articleDetails);
            });

            driver.close();
            return articles;
        }
    }

    boolean loadPage(WebDriver driver, String url){
        driver.get(url);
        try {
            Thread.sleep(3000);  // Let the user see something!
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    boolean exists(String tiltle) {
        return scrapeRepository.existsByTitle(tiltle);
    }
}