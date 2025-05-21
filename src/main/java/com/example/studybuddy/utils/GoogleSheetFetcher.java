package com.example.studybuddy.utils;


import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GoogleSheetFetcher {

    public static String getGidFromSheetName(String pubHtmlUrl, String sheetName) {
        try {
            Document doc = Jsoup.connect(pubHtmlUrl).get();
            Elements tabs = doc.getElementsByAttributeValueContaining("href", "gid=");

            System.out.println("🧾 DEBUG Tabs disponibile:");
            for(Element tab : tabs) {
                String name = tab.text().trim();
                String href = tab.attr("href");

                System.out.println("🧾 Tab găsit: [" + name + "] - href=" + href);

                if(name.equalsIgnoreCase(sheetName.trim())) {
                    Matcher matcher = Pattern.compile("gid=(\\d+)").matcher(href);
                    if(matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }

            throw new RuntimeException("Sheet name '" + sheetName + "' not found");
        } catch(IOException e) {
            throw new RuntimeException("Failed to fetch or parse Google Sheets HTML", e);
        }
    }

    public static void printAllSheetTabs(String pubHtmlUrl) {
        try {
            Document doc = Jsoup.connect(pubHtmlUrl).get();
            Elements tabs = doc.select("a[href*='gid=']");

            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!Available sheet tabs:");
            for (Element tab : tabs) {
                System.out.println(" - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + tab.text());
            }
        } catch (Exception e) {
            System.out.println("Eroare la citirea tab-urilor: " + e.getMessage());
        }
    }


    public Document fetchSheetAsHtml(String sheetUrl, String sheetName) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
       // opts.addArguments("--headless=new");
       // opts.addArguments("--disable-gpu");
        WebDriver driver = new ChromeDriver(opts);

        try {
            driver.get(sheetUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("ul#sheet-menu a")
            ));

            System.out.println("🔔 Te rog fă click pe tab-ul „" + sheetName + "” în fereastra Chrome, apoi apasă ENTER aici...");
            new Scanner(System.in).nextLine();

            wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.cssSelector("table.waffle"))
            );

            String tableHtml = driver
                    .findElement(By.cssSelector("table.waffle"))
                    .getAttribute("outerHTML");

            return Jsoup.parse(tableHtml);


        } finally {
            driver.quit();
        }
    }

}
