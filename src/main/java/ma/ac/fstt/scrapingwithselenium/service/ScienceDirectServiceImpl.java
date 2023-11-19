package ma.ac.fstt.scrapingwithselenium.service;

import ma.ac.fstt.scrapingwithselenium.models.Article;
import ma.ac.fstt.scrapingwithselenium.models.Response;
import ma.ac.fstt.scrapingwithselenium.repository.ScrapeRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScienceDirectServiceImpl implements ScienceDirectService {

    @Autowired
    private Environment env;
    @Autowired
    private ScrapeRepository scrapeRepository;

    @Override
    public List<Response> extractArticles(){
        System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        List<Response> links = new ArrayList<Response>();
        List<String> urls = new ArrayList<String>();
        List<String> offsets = Arrays.asList("0","25","50","75","100","125","150");

        for(int i=0; i<offsets.size(); i++) {
            urls.add("https://www.sciencedirect.com/search?qs=blockchain&articleTypes=FLA&lastSelectedFacet=articleTypes&offset="+offsets.get(i));
        }

        for(int j=0; j<urls.size(); j++) {
            this.loadPage(driver, urls.get(j), "SD");

            List<WebElement> elements = driver.findElements(By.className("result-list-title-link"));

            elements.forEach(element -> {
                Response res = new Response();

                if(this.exists(element.getText())) {
                    System.out.println("Already exists in DB");
                } else {
                    res.setTitle(element.getText());
                    res.setUrl(element.getAttribute("href"));
                    links.add(res);
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
        }else {
            System.setProperty("webdriver.chrome.driver", env.getProperty("webdriver"));
            ChromeOptions options = new ChromeOptions();
            WebDriver driver = new ChromeDriver(options);
            List<String> urls = new ArrayList<String>();
            List<Article> articles = new ArrayList<Article>();

            links.forEach(link -> {
                this.loadPages(driver,link.getUrl(),"SD");
                urls.add(link.getUrl());

                WebElement issn = driver.findElement(By.className("publication-title-link"));
                WebElement elt = driver.findElement(By.className("title-text"));

                WebElement doi = driver.findElement(By.id("article-identifier-links")).findElement(By.className("doi"));

                List<WebElement> elms = driver.findElements(By.className("keyword"));
                List<WebElement> authors = driver.findElements(By.className("content"));
                List<WebElement> tests = driver.findElements(By.className("content"));
                List<String> auths = new ArrayList<String>();
                authors.forEach(author -> {
                    System.out.println("siiiiiizeeee"+author.findElements(By.tagName("span")).size());
                    if(author.findElements(By.tagName("span")).size()==2) {
                        auths.add(author.findElement(By.className("surname")).getText());
                    }else{
                        auths.add(author.findElement(By.className("given-name")).getText() + " " + author.findElement(By.className("surname")).getText());
                    }
                });
                WebElement button = driver.findElement(By.id("show-more-btn"));
                button.click();

                List<WebElement> universeties = driver.findElements(By.className("affiliation"));
                WebElement date = driver.findElement(By.tagName("p"));

                Article article = new Article();
                article.setTitle(elt.getText());

                List<String> keywords=new ArrayList<String>();
                Set<String> univs=new HashSet<String>();
                elms.forEach(elm -> {
                    keywords.add(elm.getText());
                });
                article.setKeywords(keywords);


                universeties.forEach(unv -> {
                    univs.add(unv.findElement(By.tagName("dd")).getText());

                });
                article.setAuthors(auths);
                article.setUniverseties(univs);

                String[] splits = date.getText().split(",");
                String datePub=null;
                for(int i=0;i<splits.length;i++){
                    if(splits[i].contains("Available online")) {
                        datePub=splits[i];
                    }

                }

                String dateTrue = null;
                String[] tokens = datePub.split(" ");
                dateTrue=tokens[tokens.length - 3]+" "+tokens[tokens.length - 2]+" "+tokens[tokens.length - 1];

                article.setIssn(issn.getText());
                article.setYear(dateTrue);
                article.setJournal("SD");
                article.setDoi(doi.getText());
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
