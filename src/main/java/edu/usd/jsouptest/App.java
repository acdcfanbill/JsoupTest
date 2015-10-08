package edu.usd.jsouptest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tmatesoft.hg.core.HgCheckoutCommand;
import org.tmatesoft.hg.core.HgCloneCommand;
import org.tmatesoft.hg.core.HgRepoFacade;
import org.tmatesoft.hg.repo.HgLookup;
import org.tmatesoft.hg.repo.HgRemoteRepository;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws Exception {
        String baseURL = "https://toolshed.g2.bx.psu.edu";

//        List<String> repoList = new ArrayList();
//        List<String> toolList = new ArrayList();
//        
//        //Start off with the main page, browse_categories
//        repoList.addAll(GetListOfRepos(baseURL));
//
//        //check each repo in the list for a list of tools
//        //for (String repo : repoList) {
//            //better wait a bit so we don't hammer the site
//            Random rand = new Random();
//            Thread.sleep(rand.nextInt(5000));
//            String repo = repoList.get(0);
//            toolList.addAll(GetListOfToolsFromRepo(baseURL + repo));
//        //}
//            
//        System.out.println(toolList);
        
        
        //Assuming we have a tool list and a mercurial url for that tool...
        String hgCloneUrl = "hg clone https://toolshed.g2.bx.psu.edu/repos/iuc/abyss";

        HashMap<String, String> xmlFiles = new HashMap();
        xmlFiles.putAll(getToolDefinition(hgCloneUrl));
        
        for (String key : xmlFiles.keySet()) {
            System.out.println("Key: "+key);
            System.out.println(xmlFiles.get(key));
            System.out.println();
            System.out.println();
        }

    }

    /**
     * getToolDefinition clones a mercurial repository, reads the xml files into
     *  strings, deletes all the files and then returns those strings as the
     *  tool definitions.
     * @param hgCloneUrl
     * @return
     * @throws Exception 
     */
    private static HashMap<String, String> getToolDefinition(String hgCloneUrl) throws Exception {
        File tmpDir = new File("tmp");  //setup a temp directory for tool
        HashMap<String, String> tools = new HashMap(); //xml files and contents

        //clone the mercurial repo, this only gets the tree, it doesn't setup
        //  the working directory
        if (hgCloneUrl.substring(0, 9).equalsIgnoreCase("hg clone ")) {
            hgCloneUrl = hgCloneUrl.substring(9);
        }
        HgRemoteRepository hgRemote = new HgLookup().detect(new URL(hgCloneUrl));
        HgCloneCommand cmd = new HgCloneCommand();
        cmd.source(hgRemote);
        cmd.destination(tmpDir);
        cmd.execute();

        //treat the tmp directory as a local Hg repo
        HgRepoFacade hgRepo = new HgRepoFacade();
        if (!hgRepo.initFrom(tmpDir)) {
            System.err.printf("Can't find repository in: %s\n", hgRepo.getRepository().getLocation());
            throw new Exception("Can't find repo...");
        }

        //checkout all the files from the current tree into the working directory
        HgCheckoutCommand hgCheck = new HgCheckoutCommand(hgRepo.getRepository());
        hgCheck.changeset(0); //I think 0 is for latest node
        hgCheck.clean(true); //need to do clean checkout due to HG4J wonkyness
        hgCheck.execute();

        //lets find all the xml files
        FileFilter fileFilter = new WildcardFileFilter("*.xml");
        File[] xmlFiles = tmpDir.listFiles(fileFilter);
        for (File file : xmlFiles) {
            String filename = file.toPath().getFileName().toString();
            String contents = readFile(file.toString());
            tools.put(filename, contents);  //save filename and contents into List
        }

        //throw away all the tmp files
        deleteAllFiles(tmpDir);

        return tools;
    }

    /**
     * readFile reads an entire file and returns its contents as a string
     * @param path
     * @return
     * @throws IOException 
     */
    public static String readFile(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(path)));;
        StringBuilder sb;
        try {
            sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

    /**
     * getAllFiles takes a directory and returns a recursive list of all files
     *
     * @param curDir
     * @return
     */
    private static List<File> getAllFiles(File curDir) {
        List<File> files = new ArrayList();
        File[] filesList = curDir.listFiles();
        for (File f : filesList) {
            if (f.isDirectory()) {
                files.addAll(getAllFiles(f));
            }
            if (f.isFile()) {
                files.add(f);
            }
        }
        return files;
    }

    /**
     * deleteAllFiles recursively deletes all files and folders from the given
     * file/directory.
     *
     * @param curDir
     * @return
     */
    private static void deleteAllFiles(File curDir) {
        File[] filesList = curDir.listFiles();
        for (File f : filesList) {
            if (f.isDirectory()) {
                deleteAllFiles(f);
                //dir should be empty
                f.delete();
            }
            if (f.isFile()) {
                f.delete();
            }
        }
    }

    /**
     * GetListOfRepos returns a list of repos partial url's that all have lists
     * of tools in them.
     *
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
     * GetListOfTools takes a repo link, then it goes through each that repo and
     * gets a list of tools from it to return.
     *
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
