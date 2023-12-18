package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.ArticleDetails;
import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.repository.ScrapeRepository;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IeeeServiceImpl implements IeeeService {

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

        List<String> offsets = Arrays.asList("93", "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111", "112");
        for(int i=0; i<offsets.size(); i++) {
            urls.add("https://ieeexplore.ieee.org/search/searchresult.jsp?queryText=Blockchain&highlight=true&returnType=SEARCH&matchPubs=true&refinements=ContentType:Journals&returnFacets=ALL&pageNumber=" + offsets.get(i));
        }

        for(int j=0; j<urls.size(); j++) {
            this.loadPage(driver, urls.get(j));

            List<WebElement> elements = driver.findElements(By.className("List-results-items"));

            elements.forEach(element -> {
                Article res = new Article();

                if(this.exists(element.findElement(By.tagName("a")).getText())) {
                    System.out.println("Already exists in DB");
                } else {
                    res.setTitle(element.findElement(By.tagName("a")).getText());
                    res.setUrl(element.findElement(By.className("result-item-title")).findElement(By.tagName("a")).getAttribute("href"));
                    links.add(res);
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

                List<String> auths = new ArrayList<String>();
                List<String> univs = new ArrayList<String>();
                List<String> keywords = new ArrayList<String>();
                List<String> coutries = new ArrayList<String>();

                try {
                    WebElement date = driver.findElement(By.className("doc-abstract-pubdate"));
                    String[] dates = date.getText().split(":")[1].trim().split(" ");
                    if(dates.length > 0) articleDetails.setYear(dates[dates.length - 1].trim());
                    if(dates.length > 1) articleDetails.setMonth(dates[dates.length - 2].trim());
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    WebElement elt = driver.findElement(By.className("document-title"));
                    articleDetails.setTitle(elt.findElement(By.tagName("span")).getText());
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    WebElement doi = driver.findElement(By.className("stats-document-abstract-doi")).findElement(By.tagName("a"));
                    articleDetails.setDoi(doi.getText());
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    int condition = driver.findElements(By.className("col-6")).get(0).findElements(By.className("u-pb-1")).get(2).findElements(By.tagName("div")).size();

                    if(condition ==1) {
                        WebElement element = driver.findElements(By.className("col-6")).get(0).findElements(By.className("u-pb-1")).get(2).findElement(By.tagName("div"));
                        JavascriptExecutor executor = (JavascriptExecutor)driver;
                        executor.executeScript("arguments[0].click();", element);

                        List<WebElement> issn = driver.findElement(By.className("abstract-metadata-indent")).findElements(By.tagName("div"));
                        for(int i=0; i<issn.size(); i++){
                            if(issn.get(i).getText().split(":")[0].contains("Electronic ISSN")){
                                articleDetails.setIssn(issn.get(i).getText().split(":")[1].trim());
                                break;
                            }
                        }

                    } else {
                        WebElement issn = driver.findElements(By.className("col-6")).get(0).findElements(By.className("u-pb-1")).get(2).findElement(By.tagName("div")).findElement(By.tagName("div"));
                        articleDetails.setIssn(issn.getText().split(":")[1]);
                    }
                    Thread.sleep(10000);
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    driver.findElement(By.id("authors")).sendKeys(Keys.ENTER);
                    Thread.sleep(10000);
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                try {
                    List<WebElement> authors = driver.findElements(By.className("authors-accordion-container"));
                    authors.forEach(author -> {
                        auths.add(author.findElement(By.tagName("a")).getText());
                    });
                    articleDetails.setAuthors(auths);

                    authors.forEach(author -> {
                        List<WebElement> data = author.findElement(By.className("author-card")).findElement(By.className("row")).findElements(By.tagName("div"));

                        if((data.size() == 4 || data.size() == 6) && data.get(0).findElements(By.tagName("div")).size() > 1) {
                            univs.add(data.get(0).findElements(By.tagName("div")).get(1).findElement(By.tagName("div")).getText());
                        }else if (data.size() > 1 && data.get(1).findElements(By.tagName("div")).size() > 1){
                            univs.add(data.get(1).findElements(By.tagName("div")).get(1).findElement(By.tagName("div")).getText());
                        }
                        else {

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
                    String newURL = driver.getCurrentUrl().replace("authors#authors","keywords#keywords");
                    driver.get(newURL);

                    List<WebElement> elms = driver.findElements(By.className("doc-keywords-list-item"));
                    if (!elms.isEmpty()) {
                        List<WebElement> statsKeywords = elms.get(0).findElements(By.className("stats-keywords-list-item"));

                        statsKeywords.forEach(elm -> {
                            keywords.add(elm.getAttribute("data-tealium_data").split("\"keyword:\"")[0].replace("}","").replace("\"","").split(",")[1].split(":")[1]);
                        });
                        articleDetails.setKeywords(keywords);
                    } else {
                        System.out.println("No elements with class 'doc-keywords-list-item' found.");
                    }
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }

                articleDetails.setJournal("IEEE");

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