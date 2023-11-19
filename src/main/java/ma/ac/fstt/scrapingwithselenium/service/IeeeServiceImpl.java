package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.models.Response;
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
    public List<Response> extractArticles() {
        System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        List<Response> links = new ArrayList<Response>();
        List<String> urls = new ArrayList<String>();

        List<String> offsets = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        for(int i=0; i<offsets.size(); i++) {
            urls.add("https://ieeexplore.ieee.org/search/searchresult.jsp?queryText=Blockchain&highlight=true&returnType=SEARCH&matchPubs=true&refinements=ContentType:Journals&returnFacets=ALL&pageNumber="+offsets.get(i));
        }

        for(int j=0; j<urls.size(); j++) {
            this.loadPage(driver, urls.get(j), "IEEE");

            List<WebElement> elements = driver.findElements(By.className("List-results-items"));

            elements.forEach(element -> {
                Response res = new Response();

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
    public List<Article> extractArticlesDetails() {
        List<Response> links=this.extractArticles();

        if(links.isEmpty()) {
            System.out.println("There is no new data");
            return null;
        } else {
            System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
            ChromeOptions options = new ChromeOptions();
            WebDriver driver = new ChromeDriver(options);

            List<String> urls=new ArrayList<String>();
            List<Article> articles=new ArrayList<Article>();

            links.forEach(link -> {
                this.loadPages(driver,link.getUrl(),"IEEE");
                urls.add(link.getUrl());

                Article article = new Article();

                List<String> auths = new ArrayList<String>();
                Set<String> univs = new HashSet<String>();
                List<String> keywords = new ArrayList<String>();
                Set<String> coutries = new HashSet<String>();

                WebElement date = driver.findElement(By.className("doc-abstract-pubdate"));
                String[] dates = date.getText().split(":")[1].trim().split(" ");
                article.setYear(dates[dates.length - 1].trim());
                article.setMonth(dates[dates.length - 2].trim());

                WebElement elt = driver.findElement(By.className("document-title"));
                article.setTitle(elt.findElement(By.tagName("span")).getText());

                WebElement doi = driver.findElement(By.className("stats-document-abstract-doi")).findElement(By.tagName("a"));
                article.setDoi(doi.getText());

                try {
                    int condition = driver.findElements(By.className("col-6")).get(0).findElements(By.className("u-pb-1")).get(2).findElements(By.tagName("div")).size();

                    if(condition ==1) {
                        WebElement element = driver.findElements(By.className("col-6")).get(0).findElements(By.className("u-pb-1")).get(2).findElement(By.tagName("div"));
                        JavascriptExecutor executor = (JavascriptExecutor)driver;
                        executor.executeScript("arguments[0].click();", element);

                        List<WebElement> issn = driver.findElement(By.className("abstract-metadata-indent")).findElements(By.tagName("div"));
                        for(int i=0; i<issn.size(); i++){
                            if(issn.get(i).getText().split(":")[0].contains("Electronic ISSN")){
                                article.setIssn(issn.get(i).getText().split(":")[1].trim());
                                break;
                            }
                        }

                    } else {
                        WebElement issn = driver.findElements(By.className("col-6")).get(0).findElements(By.className("u-pb-1")).get(2).findElement(By.tagName("div")).findElement(By.tagName("div"));
                        article.setIssn(issn.getText().split(":")[1]);
                    }
                    Thread.sleep(10000);
                } catch (InterruptedException e) {

                }

                try {
                    driver.findElement(By.id("authors")).sendKeys(Keys.ENTER);
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                List<WebElement> authors = driver.findElements(By.className("authors-accordion-container"));
                authors.forEach(author -> {
                    auths.add(author.findElement(By.tagName("a")).getText());
                });
                article.setAuthors(auths);

                authors.forEach(author -> {
                    List<WebElement> data = author.findElement(By.className("author-card")).findElement(By.className("row")).findElements(By.tagName("div"));

                    if(data.size() == 4 || data.size() == 6) {
                        univs.add(data.get(0).findElements(By.tagName("div")).get(1).findElement(By.tagName("div")).getText());
                    }else {
                        univs.add(data.get(1).findElements(By.tagName("div")).get(1).findElement(By.tagName("div")).getText());
                    }
                });
                article.setUniverseties(univs);

                univs.forEach(univ -> {
                    String[] parts = univ.split(",");
                    coutries.add(parts[parts.length - 1].trim());
                });
                article.setCountries(coutries);

                String newURL = driver.getCurrentUrl().replace("authors#authors","keywords#keywords");
                driver.get(newURL);

                List<WebElement> elms = driver.findElements(By.className("doc-keywords-list-item")).get(0).findElements(By.className("stats-keywords-list-item"));
                elms.forEach(elm -> {
                    keywords.add(elm.getAttribute("data-tealium_data").split("\"keyword:\"")[0].replace("}","").replace("\"","").split(",")[1].split(":")[1]);
                });
                article.setKeywords(keywords);

                article.setJournal("IEEE");

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
