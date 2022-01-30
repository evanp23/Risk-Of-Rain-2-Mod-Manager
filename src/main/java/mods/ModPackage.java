package mods;

import controllers.PackageItemController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ModPackage {
    private String name;
    private String full_name;
    private String owner;
    private String package_url;
    private String date_created;
    private String date_updated;
    private String uuid4;
    private int rating_score;
    private boolean is_pinned;
    private boolean is_deprecated;
    private boolean has_nsfw_content;
    private List<String> categories;
    private List<PackageVersion> versions;
    private Map<String, PackageVersion> versionsMap;
    private BooleanProperty installed = new SimpleBooleanProperty();
    private PackageVersion installedPackageVersion;
    private PackageItemController storedController;
    private Node storedPackageItemNode;
    private boolean isDrawn;
    private boolean flaggedForInstall;
    private boolean flaggedForUpdate;
    private boolean flaggedForUninstall;

    public ModPackage(){

    }

    public ModPackage(String name, String full_name, String owner, String package_url, String date_created,
                      String date_updated, String uuid4, int rating_score, boolean is_pinned, boolean is_deprecated,
                      boolean has_nsfw_content, List<String> categories, List<PackageVersion> versions, boolean isInstalled,
                      PackageVersion installedPackageVersion, Map<String, PackageVersion> packageVersionMap) {
        this.name = name;
        this.full_name = full_name;
        this.owner = owner;
        this.package_url = package_url;
        this.date_created = date_created;
        this.date_updated = date_updated;
        this.uuid4 = uuid4;
        this.rating_score = rating_score;
        this.is_pinned = is_pinned;
        this.is_deprecated = is_deprecated;
        this.has_nsfw_content = has_nsfw_content;
        this.categories = categories;
        this.versions = versions;
        this.installed.set(isInstalled);
        this.installedPackageVersion = installedPackageVersion;
        this.versionsMap = packageVersionMap;
        this.storedController = null;
        this.storedPackageItemNode = null;
        this.isDrawn = false;
        this.flaggedForInstall = false;
        this.flaggedForUpdate = false;
        this.flaggedForUninstall = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPackage_url() {
        return package_url;
    }

    public void setPackage_url(String package_url) {
        this.package_url = package_url;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getDate_updated() {
        return date_updated;
    }

    public void setDate_updated(String date_updated) {
        this.date_updated = date_updated;
    }

    public String getUuid4() {
        return uuid4;
    }

    public void setUuid4(String uuid4) {
        this.uuid4 = uuid4;
    }

    public int getRating_score() {
        return rating_score;
    }

    public void setRating_score(int rating_score) {
        this.rating_score = rating_score;
    }

    public boolean isIs_pinned() {
        return is_pinned;
    }

    public void setIs_pinned(boolean is_pinned) {
        this.is_pinned = is_pinned;
    }

    public boolean isIs_deprecated() {
        return is_deprecated;
    }

    public void setIs_deprecated(boolean is_deprecated) {
        this.is_deprecated = is_deprecated;
    }

    public boolean isHas_nsfw_content() {
        return has_nsfw_content;
    }

    public void setHas_nsfw_content(boolean has_nsfw_content) {
        this.has_nsfw_content = has_nsfw_content;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<PackageVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<PackageVersion> versions) {
        this.versions = versions;
    }

    public void setVersionsMap(Map<String, PackageVersion> versionsMap){
        this.versionsMap = versionsMap;
    }

    public BooleanProperty installedProperty() {
        return installed;
    }

    public boolean isInstalled(){
        return this.installed.get();
    }

    public void setInstalled(boolean installed){
        this.installed.set(installed);
    }

    public PackageVersion getInstalledPackageVersion(){
        return this.installedPackageVersion;
    }

    public void setInstalledPackageVersion(PackageVersion packageVersion){
        this.installedPackageVersion = packageVersion;
    }

    public boolean dependsOn(ModPackage modPackage){
        if(installedPackageVersion.hasDependencies()) {
            for (String dependency : this.installedPackageVersion.getDependencies()) {
                if (dependency.contains(modPackage.getFull_name())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean needsUpdate(){
        DefaultArtifactVersion installed = new DefaultArtifactVersion(installedPackageVersion.getVersion_number());
        DefaultArtifactVersion latest = new DefaultArtifactVersion(this.versions.get(0).getVersion_number());

        if(latest.compareTo(installed) > 0){
            return true;
        }
        return false;
    }


    public Map<String, PackageVersion> getVersionsMap(){
        return this.versionsMap;
    }

    public void setStoredController(PackageItemController storedController) {
        this.storedController = storedController;
    }

    public Node getStoredPackageItemNode() {
        return storedPackageItemNode;
    }

    public PackageItemController getStoredController() {
        return storedController;
    }

    public boolean isDrawn() {
        return this.isDrawn;
    }

    public void setDrawn(boolean isDrawn){
        this.isDrawn = true;
    }

    public void flagForInstall(boolean flag){
        this.flaggedForInstall = flag;
    }

    public boolean isFlaggedForInstall(){
        return this.flaggedForInstall;
    }

    public void flagForUpdate(boolean flag){
        this.flaggedForUpdate = flag;
    }

    public boolean isFlaggedForUpdate(){
        return this.flaggedForUpdate;
    }

    public void flagForUninstall(boolean flag){
        this.flaggedForUninstall = flag;
    }

    public boolean isFlaggedForUninstall() {
        return this.flaggedForUninstall;
    }

    @Override
    public String toString() {
        return "Package{" +
                "name='" + name + '\'' +
                ", full_name='" + full_name + '\'' +
                ", owner='" + owner + '\'' +
                ", package_url='" + package_url + '\'' +
                ", date_created='" + date_created + '\'' +
                ", date_updated='" + date_updated + '\'' +
                ", uuid4='" + uuid4 + '\'' +
                ", rating_score=" + rating_score +
                ", is_pinned=" + is_pinned +
                ", is_deprecated=" + is_deprecated +
                ", has_nsfw_content=" + has_nsfw_content +
                ", versions='" + versions + '\'' +
                '}';
    }
}
