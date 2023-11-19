package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.models.Response;
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
    public List<Response> extractArticles() {
        System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        List<Response> links = new ArrayList<Response>();
        List<String> urls = new ArrayList<String>();

        List<String> offsets = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        for(int i=0; i<offsets.size(); i++) {
            urls.add("https://dl.acm.org/action/doSearch?AllField=Blockchain&startPage="+offsets.get(i)+"&pageSize=50");
        }
        for(int j=0; j<urls.size(); j++) {
            this.loadPage(driver,urls.get(j),"ACM");

            List<WebElement> elements = driver.findElements(By.className("hlFld-Title"));

            elements.forEach(element -> {
                Response res = new Response();
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
    public List<Article> extractArticlesDetails() {
        List<Response> links = this.extractArticles();

        if(links.isEmpty()) {
            System.out.println("There is no new data");
            return null;
        } else {

            System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
            ChromeOptions options = new ChromeOptions();
            WebDriver driver = new ChromeDriver(options);

            List<String> urls = new ArrayList<String>();
            List<Article> articles = new ArrayList<Article>();

            links.forEach(link -> {
                this.loadPages(driver, link.getUrl(), "ACM");
                urls.add(link.getUrl());

                Article article = new Article();
                article.setTitle(link.getTitle());

                Set<String> univs = new HashSet<String>();
                List<String> auths = new ArrayList<String>();
                List<String> keywords = new ArrayList<String>();
                Set<String> coutries = new HashSet<String>();

                try {
                    WebElement date = driver.findElement(By.className("epub-section__date"));
                    String[] dates = date.getText().split(" ");
                    article.setYear(dates[dates.length - 1].trim());
                    article.setMonth(dates[dates.length - 2].trim());

                    WebElement doi = driver.findElement(By.className("issue-item__doi"));
                    article.setDoi(doi.getText());

                    List<WebElement> authors = driver.findElements(By.className("loa__author-name"));
                    authors.forEach(author -> {
                        auths.add(author.findElement(By.tagName("span")).getText());
                    });
                    article.setAuthors(auths);

                    WebElement button = driver.findElement(By.className("loa__link"));
                    button.sendKeys(Keys.ENTER);
                    Thread.sleep(10000);

                    List<WebElement> universeties = driver.findElements(By.className("auth-info"));
                    universeties.forEach(unv -> {
                        if(unv.findElements(By.tagName("span")).size() >1) {
                            univs.add(unv.findElements(By.tagName("span")).get(1).getText());
                        }else {
                            univs.add(unv.findElement(By.tagName("span")).findElement(By.tagName("p")).getText());
                        }
                    });
                    article.setUniverseties(univs);

                    univs.forEach(univ -> {
                        String[] parts = univ.split(",");
                        coutries.add(parts[parts.length - 1].trim());
                    });
                    article.setCountries(coutries);

                    WebElement button2 = driver.findElement(By.id("pill-information__contentcon"));
                    button2.sendKeys(Keys.ENTER);
                    Thread.sleep(10000);

                    List<WebElement> flexContainers = driver.findElement(By.className("cover-image__details-extra"))
                            .findElements(By.className("flex-container"));

                    if (!flexContainers.isEmpty()) {
                        WebElement issn = flexContainers.get(0).findElement(By.className("space"));
                        article.setIssn(issn.getText());
                    } else {
                        System.out.println("No flex containers found");
                    }

                    List<WebElement> elms = driver.findElements(By.className("badge-type"));
                    elms.forEach(elm -> {
                        keywords.add(elm.getText());
                    });
                    article.setKeywords(keywords);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                article.setJournal("ACM");

                articles.add(article);
                scrapeRepository.save(article);
            });

            driver.close();
            return articles;
        }
    }

    boolean loadPages(WebDriver driver, String url, String journal) {
        driver.get(url);
        try {
            Thread.sleep(3000);  // Let the user actually see something!
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    boolean loadPage(WebDriver driver, String url, String journal){
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
