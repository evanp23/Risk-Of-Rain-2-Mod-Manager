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
        List<ModPackage> modsToInstall = new ArrayList<>();

        getDownloadUrls(modPackage, version, modsMap, modPackages, modsToInstall);


        int progressCount = 0;
        for(ModPackage modToIntall : modsToInstall){
            PackageVersion packageVersionToInstall = modToIntall.getInstalledPackageVersion();
            installAndExtract(new URL(packageVersionToInstall.getDownload_url()), packageVersionToInstall);
            progressCount++;
            setProgress(((double) progressCount / (double)modsToInstall.size()) - 0.1);
        }

        for(ModPackage modToInstall : modsToInstall){
            modToInstall.setInstalled(true);
        }
        setProgress(1.0);
    }

    public void getDownloadUrls(ModPackage modPackage, String version, Map<String, Integer> modsMap, List<ModPackage> modPackages, List<ModPackage> modsToInstall) throws IOException, SQLException {
        ModPackage bepInExPack = modPackages.get(modsMap.get("bbepis-BepInExPack"));
        if(bepInExPack.getInstalledPackageVersion() == null && !modPackage.getFull_name().equals("bbepis-BepInExPack")){
            getDownloadUrls(bepInExPack, "", modsMap, modPackages, modsToInstall);
            getDownloadUrls(modPackage, version, modsMap, modPackages, modsToInstall);
        }
        else {
            if (!modsToInstall.contains(modPackage) && modPackage.getInstalledPackageVersion() == null) {
                PackageVersion installVersion;
                if (version.equals("")) {
                    installVersion = modPackage.getVersions().get(0);
                } else {
                    installVersion = modPackage.getVersionsMap().get(version);
                }
                modPackage.setInstalledPackageVersion(installVersion);
                modsToInstall.add(modPackage);
                if (installVersion.hasDependencies()) {
                    for (String dependency : installVersion.getDependencies()) {
                        List<String> fullName = new PackageGetter().parseFullName(dependency);
                        String parsedFullName = fullName.get(1) + "-" + fullName.get(0);
                        int dependencyPosition = -1;
                        if (modsMap.containsKey(parsedFullName)) {
                            dependencyPosition = modsMap.get(parsedFullName);
                        }
                        ModPackage desiredModPackage = modPackages.get(dependencyPosition);
                        getDownloadUrls(desiredModPackage, "", modsMap, modPackages, modsToInstall);
                    }
                }
            }
        }
    }

    public List<PackageVersion> getRecentlyInstalledMods(){
        return this.recentlyInstalledMods;
    }

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
                System.out.println("installandextract: " + modName);
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


    private ModPackage findModPackage(PackageVersion packageVersion, Map<String, Integer> gottenModPositions, List<ModPackage> modPackages){
        String packageName = packageVersion.getName();
        String packageAuthor = packageVersion.getNamespace();
        int modPosition = gottenModPositions.get(packageAuthor + "-" + packageName);
        return modPackages.get(modPosition);
    }

}
