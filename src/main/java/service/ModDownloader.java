package service;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import mods.ModPackage;
import mods.PackageVersion;
import database.Database;
import javafx.concurrent.Task;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.codehaus.plexus.util.FileUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModDownloader {
    private File gamePath;
    private File bepInDir;
    private File bepInPlug;
    private List<PackageVersion> recentlyInstalledMods = new ArrayList<>();
    private Task downloadTask;
    private String tempZips = "TempZips";
    private String tempExtractions = "TempExtraction";
    private DoubleProperty progressProperty = new SimpleDoubleProperty();

    public ModDownloader(){

        JsonReader jsonReader = new JsonReader();
        try {
            JSONObject jsonObject = jsonReader.readJsonFromFile("Config/Config.json");
            this.gamePath = new File(jsonObject.getString("directory"));
            this.bepInDir = new File(gamePath + "/BepInEx");
            this.bepInPlug = new File(bepInDir + "/plugins");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void removeModFiles(String modFileName, File file) throws IOException {
        if(modFileName.equals("bbepis-BepInExPack")){
            FileUtils.deleteDirectory(bepInDir);
            List<String> possibleFileNames = new ArrayList<>(List.of("changelog.txt", "preloader", "README.md", "winhttp.dll"));
            for(File file1 : gamePath.listFiles()){
                for(String fileName : possibleFileNames){
                    if(file1.getName().contains(fileName)){
                        FileUtils.fileDelete(file1.getAbsolutePath());
                    }
                }
            }
        }
        else {
            for (File inside : file.listFiles()) {
                if (inside.isDirectory()) {
                    if (modFileName.contains(inside.getName())) {
                        if (inside.isDirectory()) {
                            FileUtils.deleteDirectory(inside);
                        } else {
                            FileUtils.fileDelete(inside.getAbsolutePath());
                        }
                    } else {
                        removeModFiles(modFileName, inside);
                    }
                }
            }
        }
    }

    public DoubleProperty progressProperty(){
        return this.progressProperty;
    }

    private void setProgress(Double progress){
        this.progressProperty.set(progress);
    }

    public void downloadMod(ModPackage modPackage, String version, Map<String, Integer> modsMap, List<ModPackage> modPackages) throws SQLException, IOException {
        List<PackageVersion> packageVersions = new ArrayList<>();

        getDownloadUrls(modPackage, version, modsMap, modPackages, packageVersions);

        int progressCount = 0;
        for(PackageVersion packageVersion : packageVersions){
            installAndExtract(new URL(packageVersion.getDownload_url()), packageVersion);
            progressCount++;
            setProgress(((double) progressCount / (double)packageVersions.size()));
        }
    }

    public void getDownloadUrls(ModPackage modPackage, String version, Map<String, Integer> modsMap, List<ModPackage> modPackages, List<PackageVersion> packageVersions) throws IOException, SQLException {
        ModPackage bepInExPack = modPackages.get(modsMap.get("bbepis-BepInExPack"));
        if(!bepInExPack.isInstalled() && !modPackage.getFull_name().equals("bbepis-BepInExPack")){
            getDownloadUrls(bepInExPack, "", modsMap, modPackages, packageVersions);
            getDownloadUrls(modPackage, version, modsMap, modPackages, packageVersions);
        }
        else {
            if (!modPackage.isInstalled()) {
                PackageVersion installVersion;
                URL installURL;
                if (version.equals("")) {
                    installVersion = modPackage.getVersions().get(0);
                } else {
                    installVersion = modPackage.getVersionsMap().get(version);
                }

                packageVersions.add(installVersion);
                modPackage.setInstalled(true);
                modPackage.setInstalledPackageVersion(installVersion);

                if (installVersion.hasDependencies()) {
                    for (String dependency : installVersion.getDependencies()) {
                        List<String> fullName = new PackageGetter().parseFullName(dependency);
                        String parsedFullName = fullName.get(1) + "-" + fullName.get(0);
                        int dependencyPosition = -1;
                        if (modsMap.containsKey(parsedFullName)) {
                            dependencyPosition = modsMap.get(parsedFullName);
                        } else {
                        }
                        ModPackage desiredModPackage = modPackages.get(dependencyPosition);
                        getDownloadUrls(desiredModPackage, "", modsMap, modPackages, packageVersions);
                    }
                }
            }
        }
    }

    public Task getDownloadTask(){
        return this.downloadTask;
    }

    private void setRecentlyInstalledMod(PackageVersion packageVersion){
        this.recentlyInstalledMods.add(packageVersion);
    }

    public List<PackageVersion> getRecentlyInstalledMods(){
        return this.recentlyInstalledMods;
    }

//    private void getAllDownloadURL(PackageVersion packageVersion, List<URL> urlList, List<PackageVersion> packageVersions, List<String> alreadyGottenNames) throws IOException, SQLException {
//        if(!(packageVersion.getUuid4() == null)) {
//            packageVersion = new PackageGetter().buildVersionFromFullName(packageVersion.getFull_name(), packageVersion.getVersion_number());
//        }
//        URL thisURL = new URL(packageVersion.getDownload_url());
//        if(packageVersion.getDependencies() == null){
//            if(!alreadyGottenNames.contains(packageVersion.getName())) {
//                if(packageVersion.getName().equals("BepInExPack")){
//                    urlList.add(0, thisURL);
//                    packageVersions.add(0, packageVersion);
//                    alreadyGottenNames.add(0, packageVersion.getName());
//                }
//                else {
//                    urlList.add(thisURL);
//                    packageVersions.add(packageVersion);
//                    alreadyGottenNames.add(packageVersion.getName());
//                }
//            }
//        }
//        else{
//            PackageGetter packageGetter = new PackageGetter();
//
//            if(!alreadyGottenNames.contains(packageVersion.getName())){
//                if(packageVersion.getName().equals("BepInExPack")){
//                    urlList.add(0, thisURL);
//                    packageVersions.add(0, packageVersion);
//                    alreadyGottenNames.add(0, packageVersion.getName());
//                }
//                else {
//                    urlList.add(thisURL);
//                    packageVersions.add(packageVersion);
//                    alreadyGottenNames.add(packageVersion.getName());
//                    for (String dependency : packageVersion.getDependencies()) {
//                        getAllDownloadURL(packageGetter.buildVersionFromFullName(dependency, ""), urlList, packageVersions, alreadyGottenNames);
//                    }
//
//                }
//
//
//            }
//        }
//    }

    private void extractFile(File file, ZipFile zipFile, PackageVersion packageVersion) throws IOException {
        String finalFolderName = packageVersion.getNamespace() + "-" + packageVersion.getName();
        ArrayList<String> possibleNames = new ArrayList<>(List.of("plugins", "config", "core", "patchers", "monomod", "cache"));
        if (file.isDirectory()) {
            if (possibleNames.contains(file.getName())) {
                zipFile.extractFile(file.getName() + "/", bepInDir.getAbsolutePath(), file.getName() + "/" + finalFolderName);
            }
            else if(file.getName().equals("BepInEx")){
                for(File insideFile : file.listFiles()){
                    zipFile.extractFile("BepInEx/" + insideFile.getName() + "/", bepInDir.getAbsolutePath(), insideFile.getName() + "/" + finalFolderName);
                }
            }
            else{
                zipFile.extractFile(file.getName() + "/", bepInPlug.getAbsolutePath() + "/" + finalFolderName);
            }
            FileUtils.deleteDirectory(file);
        } else {
            zipFile.extractFile(file.getName(), bepInPlug.getAbsolutePath() + "/" + finalFolderName);
            FileUtils.fileDelete(file.getAbsolutePath());
        }
    }

    private void installBepInEx() throws IOException {

        File bepInTempFolder = new File(tempExtractions + "/BepInExPack/BepInExPack/");

        for(File file : bepInTempFolder.listFiles()){
            File existFile = new File(gamePath.getAbsolutePath() + "/" + file.getName());
            if(existFile.exists()){
                if(existFile.isDirectory()){
                    FileUtils.deleteDirectory(existFile);
                }
                else{
                    Files.delete(Paths.get(existFile.toString()));
                }

            }
            if(file.isDirectory()){
                org.apache.commons.io.FileUtils.moveDirectory(file, existFile);
            }
            else{
                org.apache.commons.io.FileUtils.moveFile(file, existFile);
            }
        }

        File testExt = new File(tempExtractions);
        for(File testFile : testExt.listFiles()){
            if(testFile.isDirectory()){
                FileUtils.deleteDirectory(testFile);
            }
            else{
                FileUtils.fileDelete(testFile.getAbsolutePath());
            }
        }
    }

    private void installAndExtract(URL url, PackageVersion packageVersion){
        try {
            String modName = packageVersion.getName();
            String modNamespace = packageVersion.getNamespace();
            String modFullname = packageVersion.getFull_name();

            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
            File tempZip = new File(tempZips + "/" + modFullname + ".zip");
            FileOutputStream fileOutputStream = new FileOutputStream(tempZip);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);


            ZipFile zipFile = new ZipFile(tempZip);

            zipFile.extractAll(tempExtractions + "/" + modName);

            File thisFile = new File(tempExtractions + "/" + modName);

            if (modName.equals("BepInExPack")) {
                installBepInEx();
            } else {
                for (File file : thisFile.listFiles()) {
                    extractFile(file, zipFile, packageVersion);
                }
            }

            FileUtils.deleteDirectory(thisFile);

            fileOutputStream.close();
            readableByteChannel.close();
            zipFile.close();

            File deletableZip = new File(tempZips + "/" + modFullname + ".zip");
            deletableZip.delete();
            setRecentlyInstalledMod(packageVersion);


        }catch (ZipException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getBepInDir(){
        return this.bepInDir;
    }

//    private Task initializeDownloadTask(ModPackage modPackage, String version, Map<String, Integer> allModsMap, List<ModPackage> allModsList){
//        Task downloadTask = new Task() {
//            @Override
//            protected Object call() throws Exception {
//
////            PackageVersion desiredVersion = modPackage.getVersions().get(modPackage.getVersionsMap().get(version));
////                System.out.println("INSTALLING " + modPackage.getName());
////            for(String dependency : desiredVersion.getDependencies()){
////
////                if(desiredModPackage.isInstalled()){
////                    System.out.println(desiredModPackage.getName() + " is already installed");
////                }
////                else{
////                    String url = desiredModPackage.getVersions().get(0).getDownload_url();
////                    System.out.println("installing " + desiredModPackage.getName() + " : " + url);
////
////                    //donwload and install mod
////                    try{
////                        PackageVersion currentPackageVersion = desiredModPackage.getVersions().get(0);
////                        String modName = currentPackageVersion.getName();
////                        String modFullname = currentPackageVersion.getFull_name();
////                        URL modDownloadURL = urlList.get(i);
////
////
////
////                        //MOVE
////                        db.addMod(packageVersions.get(i), dbConnection);
////
////
////
////                        ReadableByteChannel readableByteChannel = Channels.newChannel(modDownloadURL.openStream());
////                        File tempZip = new File(tempZips + "/" + modFullname + ".zip");
////                        FileOutputStream fileOutputStream = new FileOutputStream(tempZip);
////                        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
////
////                        ZipFile zipFile = new ZipFile(tempZip);
////
////                        zipFile.extractAll(tempExtractions + "/" + modName);
////
////                        File thisFile = new File(tempExtractions + "/" + modName);
////
////                        if (modName.equals("BepInExPack")) {
////                            installBepInEx(packageVersions.get(i));
////                        } else {
////                            for (File file : thisFile.listFiles()) {
////                                extractFile(file, zipFile, packageVersions.get(i));
////                            }
////                        }
////
////                        FileUtils.deleteDirectory(thisFile);
////
////                        fileOutputStream.close();
////                        readableByteChannel.close();
////                        zipFile.close();
////
////                        File deletableZip = new File(tempZips + "/" + packageVersions.get(finalI).getFull_name() + ".zip");
////                        deletableZip.delete();
////                        setRecentlyInstalledMod(packageVersions.get(i));
////
////
////
////
////
////                        dbConnection.close();
////                    } catch (ZipException e) {
////                        e.printStackTrace();
////                    } catch (FileNotFoundException e) {
////                        e.printStackTrace();
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    } catch (SQLException throwables) {
////                        throwables.printStackTrace();
////                    }
////
////                }
////                updateProgress(i + 1, packageVersions.size());
////            }
//
//
//            return null;
//            }
//        };
//        return downloadTask;
//    }

}
