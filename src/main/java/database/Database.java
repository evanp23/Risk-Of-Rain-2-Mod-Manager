package database;

import mods.ModPackage;
import mods.PackageVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {



    public Database(){

    }

    public Connection connect(){
        File dbFile = new File("DB/config.sqlite");
        String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        Connection conn = null;
        try{
            conn = DriverManager.getConnection(dbUrl);
            createTable(conn);
        }catch(SQLException s){
            s.printStackTrace();
        }
        return conn;
    }


    public void createTable(Connection conn){
        String createStatement = "CREATE TABLE IF NOT EXISTS installed_mods (\n"
                +"\tid integer PRIMARY KEY AUTOINCREMENT,\n"
                +"\tname text NOT NULL,\n"
                +"\towner text NOT NULL,\n"
                +"\tversion text NOT NULL,\n"
                +"\tfull_name text NOT NULL\n"
                +");";

        try(
            Statement stmt = conn.createStatement()){

            stmt.execute(createStatement);
        }catch(SQLException s){
            s.printStackTrace();
        }
    }

    public void addMod(ModPackage modPackage, Connection conn){
        String modName = modPackage.getName();
        String modOwner = modPackage.getOwner();
        PackageVersion installedVersion = modPackage.getInstalledPackageVersion();
        String version = installedVersion.getVersion_number();
        String fullName = installedVersion.getFull_name();

        String insertStmt = "INSERT INTO installed_mods(id, name, owner, version, full_name) VALUES (NULL,?,?,?,?);";

        try(PreparedStatement ps = conn.prepareStatement(insertStmt)){


            ps.setString(1, modName);
            ps.setString(2, modOwner);
            ps.setString(3, version);
            ps.setString(4, fullName);

            ps.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void removeMod(ModPackage modPackage, Connection conn) {
        String modName = modPackage.getName();
        String modOwner = modPackage.getOwner();

        String selectStatement = "DELETE FROM installed_mods WHERE name = ? AND owner = ?;";

        try(PreparedStatement ps = conn.prepareStatement(selectStatement)) {

            ps.setString(1, modName);
            ps.setString(2, modOwner);

            ps.executeUpdate();
        }catch(SQLException s){
            s.printStackTrace();
        }
    }

    public String getInstalledVersion(String namespace, String name, Connection conn){
        String selectStatement = "SELECT version FROM installed_mods WHERE owner = ? AND name = ?";
        String installedVersion = null;
        try(PreparedStatement ps = conn.prepareStatement(selectStatement)) {
            ps.setString(1, namespace);
            ps.setString(2, name);

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                installedVersion = rs.getString("version");
            }

        }catch(SQLException s){
            s.printStackTrace();
        }
        return installedVersion;

    }

    public boolean modIsInstalled(String name, String namespace, Connection conn) throws SQLException {
        String selectStatement = "SELECT * FROM installed_mods WHERE owner = ? AND name = ?";

        try(PreparedStatement ps = conn.prepareStatement(selectStatement)) {
            ps.setString(1, namespace);
            ps.setString(2, name);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return true;
            }
            else{
                return false;
            }

        }catch(SQLException s){
            s.printStackTrace();
        }
        return false;


    }

    public int modIsInstalled(String name, String namespace, String latestVersion, Connection conn) throws SQLException {

        String selectStatement = "SELECT * FROM installed_mods WHERE owner = ? AND name = ?";
        int returnVal = -1;

        try(PreparedStatement ps = conn.prepareStatement(selectStatement)) {
            ps.setString(1, namespace);
            ps.setString(2, name);
            String installedVersion = null;

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                returnVal += 1;
                installedVersion = rs.getString("version");
                DefaultArtifactVersion installed = new DefaultArtifactVersion(installedVersion);
                DefaultArtifactVersion latest = new DefaultArtifactVersion(latestVersion);

                if(latest.compareTo(installed) > 0){
                    returnVal += 1;
                }
            }
            else{
                return -1;
            }



        }catch(SQLException s){
            s.printStackTrace();
        }
        return returnVal;


    }

    public List<List<String>> getInstalledMods(Connection conn) throws SQLException{

        List<List<String>> installedMods = new ArrayList<>();
        String selectStatement = "SELECT name, owner, version, full_name FROM installed_mods";

        PreparedStatement ps = conn.prepareStatement(selectStatement);
        ResultSet rs = ps.executeQuery();

        while(rs.next()){
            List<String> resultToAdd = new ArrayList<>();

            resultToAdd.add(rs.getString("name"));
            resultToAdd.add(rs.getString("owner"));
            resultToAdd.add(rs.getString("version"));
            resultToAdd.add(rs.getString("full_name"));

            installedMods.add(resultToAdd);
        }



        return installedMods;
    }
}
