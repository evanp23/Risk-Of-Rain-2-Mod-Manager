package mods;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.List;

public class PackageVersion {
    private String namespace;
    private String name;
    private String full_name;
    private String description;
    private String icon;
    private String version_number;
    private List<String> dependencies;
    private boolean hasDependencies;
    private String download_url;
    private int downloads;
    private String created;
    private String webUrl;
    private boolean is_active;
    private String uuid4;
    private int file_size;
    private boolean installed;
    private boolean needsUpdate;

    public PackageVersion(){

    }

    public PackageVersion(String namespace, String name, String version_number,
                          String full_name,String description, String icon, List<String> dependencies,
                          String download_url, int downloads, String created, String webUrl, boolean is_active, boolean installed,
                          boolean needsUpdate){
        this.namespace = namespace;
        this.name = name;
        this.version_number = version_number;
        this.full_name = full_name;
        this.description = description;
        this.icon = icon;
        this.dependencies = dependencies;
        if(dependencies.isEmpty()){
            hasDependencies = false;
            this.dependencies = null;
        }
        else{
            hasDependencies = true;
        }
        this.download_url = download_url;
        this.downloads = downloads;
        this.created = created;
        this.webUrl = webUrl;
        this.is_active = is_active;
        this.uuid4 = null;
        this.file_size = -1;
        this.installed = installed;
        this.needsUpdate = needsUpdate;
    }

    public PackageVersion(String namespace, String name, String full_name, String description, String icon,
                          String version_number, List<String> dependencies, String download_url,
                          int downloads, String created, String webUrl, boolean is_active, String uuid4,
                          int file_size) {
        this.namespace = namespace;
        this.name = name;
        this.full_name = full_name;
        this.description = description;
        this.icon = icon;
        this.version_number = version_number;
        this.dependencies = dependencies;
        if(this.dependencies.isEmpty()){
            this.hasDependencies = false;
            this.dependencies = null;
        }
        else{
            this.hasDependencies = true;
        }
        this.download_url = download_url;
        this.downloads = downloads;
        this.created = created;
        this.webUrl = webUrl;
        this.is_active = is_active;
        this.uuid4 = uuid4;
        this.file_size = file_size;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getVersion_number() {
        return version_number;
    }

    public void setVersion_number(String version_number) {
        this.version_number = version_number;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public boolean isIs_active() {
        return is_active;
    }

    public void setIs_active(boolean is_active) {
        this.is_active = is_active;
    }

    public String getUuid4() {
        return uuid4;
    }

    public void setUuid4(String uuid4) {
        this.uuid4 = uuid4;
    }

    public int getFile_size() {
        return this.file_size;
    }

    public void setFile_size(int file_size) {
        this.file_size = file_size;
    }

    public String getNamespace(){
        return this.namespace;
    }

    public boolean isInstalled(){
        return this.installed;
    }

    public void setInstalled(boolean installed){
        this.installed = installed;
    }

    public boolean needsUpdate(){
        return this.needsUpdate;
    }

    public boolean hasDependencies(){
        return this.hasDependencies;
    }

    public boolean isNewerThan(PackageVersion packageVersion){
        DefaultArtifactVersion installed = new DefaultArtifactVersion(this.version_number);
        DefaultArtifactVersion given = new DefaultArtifactVersion(packageVersion.getVersion_number());

        return installed.compareTo(given) > 0;
    }

    @Override
    public String toString() {
        return "PackageVersion{" +
                "name='" + name + '\'' +
                ", full_name='" + full_name + '\'' +
                ", description='" + description + '\'' +
                ", icon='" + icon + '\'' +
                ", version_number='" + version_number + '\'' +
                ", dependencies=" + dependencies +
                ", download_url='" + download_url + '\'' +
                ", downloads=" + downloads +
                ", created='" + created + '\'' +
                ", webUrl='" + webUrl + '\'' +
                ", is_active=" + is_active +
                ", uuid4='" + uuid4 + '\'' +
                ", file_size=" + file_size +
                '}';
    }
}
