package com.example.studybuddy.utils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleSheetUtil {

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

}
