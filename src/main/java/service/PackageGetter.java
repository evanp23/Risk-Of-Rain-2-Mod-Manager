package service;


import mods.PackageVersion;
import database.Database;
import mods.ModPackage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class PackageGetter {
    List<ModPackage> allPackages = new ArrayList<>();
    Database db = new Database();
    Connection conn = db.connect();


    public List<ModPackage> loadPackages(Map<String, Integer> gottenModPositions) throws IOException, SQLException, ClassNotFoundException {
        File jsonFile = new File("cached/thunderstorePackages.json");
        JSONArray rootFromWeb = JsonReader.readJsonArrayFromUrl("https://thunderstore.io/api/v1/package/");
        JSONArray rootFromFile = null;
        JSONArray finalRoot = null;

        if(jsonFile.exists()){
            rootFromFile = JsonReader.readJsonArrayFromFile(jsonFile);
            if(rootFromWeb.toString().equals(rootFromFile.toString())){
                System.out.println("reading from file");
                finalRoot = rootFromFile;
            }
            else{
                System.out.println("reading from web");
                finalRoot = rootFromWeb;
                JsonWriter jsonWriter = new JsonWriter();
                jsonWriter.writeJsonArrayToFile("cached/thunderstorePackages.json", finalRoot);
            }
        }

        for(int i = 0; i < finalRoot.length(); i++){
            JSONObject packObj = finalRoot.getJSONObject(i);

            //get all values
            String pkgName = packObj.getString("name");
            String fullName = packObj.getString("full_name");
            String pkgOwner = packObj.getString("owner");
            String pkgUrl = packObj.getString("package_url");
            String created = packObj.getString("date_created");
            String updated = packObj.getString("date_updated");
            String uuid4 = packObj.getString("uuid4");
            int rating = packObj.getInt("rating_score");
            boolean isPinned = packObj.getBoolean("is_pinned");
            boolean isDeprecated = packObj.getBoolean("is_deprecated");
            boolean hasNSFW = packObj.getBoolean("has_nsfw_content");

            //read in categories
            JSONArray categories = packObj.getJSONArray("categories");
            List<String> allCategories = new ArrayList<>();
            for(int j = 0; j < categories.length(); j++){
                String categoryName = categories.getString(j);
                allCategories.add(categoryName);
            }

            //read in versions
            JSONArray versions = packObj.getJSONArray("versions");
            List<PackageVersion> allVersions = new ArrayList<>();

            Map<String, PackageVersion> versionsMap = new HashMap<>();
            for(int j = 0; j < versions.length(); j ++){
                List<String> allDependencies = new ArrayList<>();
                JSONObject oneVersion = versions.getJSONObject(j);
                String vName = oneVersion.getString("name");
                String vFullName = oneVersion.getString("full_name");
                String vDescription = oneVersion.getString("description");
                String vIcon = oneVersion.getString("icon");
                String vNum = oneVersion.getString("version_number");

                List<String> vDependencies = new ArrayList<>();
                JSONArray dependArray = oneVersion.getJSONArray("dependencies");
                for(int k = 0; k < dependArray.length(); k++){
                    vDependencies.add(dependArray.getString(k));
                }

                String vDownloadUrl = oneVersion.getString("download_url");
                int vDownloads = oneVersion.getInt("downloads");
                String vCreated = oneVersion.getString("date_created");
                String vWebUrl = oneVersion.getString("website_url");
                boolean vIsActive = oneVersion.getBoolean("is_active");
                String vUuid4 = oneVersion.getString("uuid4");
                int vFileSize = oneVersion.getInt("file_size");

                PackageVersion onePackageVersion = new PackageVersion(pkgOwner, vName,
                        vFullName,
                        vDescription,
                        vIcon,
                        vNum,
                        vDependencies,
                        vDownloadUrl,
                        vDownloads,
                        vCreated,
                        vWebUrl,
                        vIsActive,
                        vUuid4,
                        vFileSize);
                allVersions.add(onePackageVersion);


                versionsMap.put(vNum, onePackageVersion);
            }

            PackageVersion installedPackageVersion = null;
            boolean isInstalled = db.modIsInstalled(pkgName, pkgOwner, conn);
            String installedVersionString;
            if(isInstalled){
                installedVersionString = db.getInstalledVersion(pkgOwner, pkgName, conn);
                installedPackageVersion = versionsMap.get(installedVersionString);
            }

            ModPackage oneModPackage = new ModPackage(pkgName,
                    fullName,
                    pkgOwner,
                    pkgUrl,
                    created,
                    updated,
                    uuid4,
                    rating,
                    isPinned,
                    isDeprecated,
                    hasNSFW,
                    allCategories,
                    allVersions,
                    isInstalled,
                    installedPackageVersion,
                    versionsMap);

            allPackages.add(oneModPackage);

            gottenModPositions.put(fullName, i);
        }
        return allPackages;
    }

    public PackageVersion buildVersionFromFullName(String fullName, String version) throws IOException, SQLException {
        List<String> parsedName = parseFullName(fullName);
        String parsedModName = parsedName.get(0);
        String parsedNameSpace = parsedName.get(1);


        String buildURL;
        JSONObject jsonGotten;
        JSONObject root;

        if(version.equals("")){
            buildURL = String.format("https://thunderstore.io/api/experimental/package/%s/%s", parsedNameSpace, parsedModName);
            jsonGotten = JsonReader.readJsonFromUrl(buildURL);
            root = jsonGotten.getJSONObject("latest");
        }
        else{
            buildURL = String.format("https://thunderstore.io/api/experimental/package/%s/%s/%s", parsedNameSpace, parsedModName, version);
            root = JsonReader.readJsonFromUrl(buildURL);
        }


        String namespace = root.getString("namespace");
        String name = root.getString("name");
        String version_number = root.getString("version_number");
        String full_name = root.getString("full_name");
        String description = root.getString("description");
        String icon = root.getString("icon");
        JSONArray dependencies = root.getJSONArray("dependencies");
        List<String> verDepen = new ArrayList<>();
        for(int i = 0; i < dependencies.length(); i++){
            verDepen.add(dependencies.getString(i));
        }

        String downloadURL = root.getString("download_url");
        int downloads = root.getInt("downloads");
        String dateCreated = root.getString("date_created");
        String webURL = root.getString("website_url");
        boolean isActive = root.getBoolean("is_active");

        int installedInt = db.modIsInstalled(name, namespace, version, conn);
        boolean installed;
        boolean needsUpdate;
        if(installedInt == 0){
            installed = true;
            needsUpdate = false;
        }
        else if(installedInt == 1){
            installed = true;
            needsUpdate = true;
        }
        else{
            installed = false;
            needsUpdate = false;
        }

        PackageVersion packageVersion = new PackageVersion(namespace, name, version_number, full_name, description, icon, verDepen, downloadURL, downloads, dateCreated, webURL, isActive, installed, needsUpdate);

        return packageVersion;
    }

    public List<String> parseFullName(String fullName){
        List<String> packageToReturn = new ArrayList<>();
        Scanner sc = new Scanner(fullName);
        sc.useDelimiter("-");

        List<String> inputArr = new ArrayList<>();

        for(String s: sc.nextLine().split("-")){
            inputArr.add(s);
        }
        String modName;
        String nameSpace;
        String version;
        if(inputArr.size() == 4){
            nameSpace = inputArr.get(0) + "-" + inputArr.get(1);
            modName = inputArr.get(2);
            version  = inputArr.get(3);
        }
        else{
            nameSpace = inputArr.get(0);
            modName = inputArr.get(1);
            version = inputArr.get(2);
        }

        packageToReturn.add(modName);
        packageToReturn.add(nameSpace);
        packageToReturn.add(version);

        return packageToReturn;
    }


}
