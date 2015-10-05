package edu.usd.jsouptest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws Exception {
        String baseURL = "https://toolshed.g2.bx.psu.edu";

        List<String> repoList = new ArrayList();
        List<String> toolList = new ArrayList();
        
        //Start off with the main page, browse_categories
        repoList.addAll(GetListOfRepos(baseURL));

        //check each repo in the list for a list of tools
        //for (String repo : repoList) {
            //better wait a bit so we don't hammer the site
            Random rand = new Random();
            Thread.sleep(rand.nextInt(5000));
            String repo = repoList.get(0);
            toolList.addAll(GetListOfToolsFromRepo(baseURL + repo));
        //}
            
        System.out.println(toolList);
    }

    /**
     * GetListOfRepos returns a list of repos partial url's that all have lists
     *  of tools in them.
     * @param baseURL
     * @return
     * @throws Exception 
     */
    private static List<String> GetListOfRepos(String baseURL) throws Exception {
        List<String> repoList = new ArrayList();
        Document doc = Jsoup.connect(baseURL + "/repository/browse_categories").get();

        Elements scriptTags = doc.select("script");
        for (Element tag : scriptTags) {
            for (DataNode node : tag.dataNodes()) {
                String data = node.getWholeData();
                Scanner scanner = new Scanner(data);

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    if (line.startsWith("{") && line.endsWith(" );")) {
                        line = line.substring(0, line.length() - 2);
                        JSONObject repos = new JSONObject(line);
                        JSONArray items = repos.getJSONArray("items");
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = (JSONObject) items.get(i);
                            repoList.add(item.getJSONObject("column_config").getJSONObject("Name").getString("link"));
                        }
                    }
                }
            }
        }

        return repoList;
    }
    
    /**
     * GetListOfTools takes a baseURL and a repos, then it goes through each
     *  that repo and gets a list of tools from it to return.
     * @param baseURL
     * @param repoList
     * @return
     * @throws Exception 
     */
    private static List<String> GetListOfToolsFromRepo(String repo)
            throws Exception {
        List<String> toolList = new ArrayList();
        
        Document doc = Jsoup.connect(repo).get();

        Elements scriptTags = doc.select("script");
        for (Element tag : scriptTags) {
            for (DataNode node : tag.dataNodes()) {
                String data = node.getWholeData();
                Scanner scanner = new Scanner(data);

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    if (line.startsWith("{") && line.endsWith(" );")) {
                        line = line.substring(0, line.length() - 2);
                        JSONObject tools = new JSONObject(line);
                        System.out.println(tools.toString(2));
                        JSONArray items = tools.getJSONArray("items");
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = (JSONObject) items.get(i);
                            toolList.add(item.getJSONObject("column_config").getJSONObject("Name").getString("link"));
                        }
                    }
                }
            }
        }
        
        return toolList;
    }
}