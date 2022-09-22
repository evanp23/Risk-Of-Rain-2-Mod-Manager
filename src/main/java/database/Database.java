package database;

import mods.ModPackage;
import mods.PackageVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    public static Connection connection;

    public static void connect(){
        File dbFile = new File("DB/config.sqlite");
        String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try{
            Database.connection = DriverManager.getConnection(dbUrl);
            createTable();
        }catch(SQLException s){
            s.printStackTrace();
        }
    }


    public static void createTable(){
        String createStatement = "CREATE TABLE IF NOT EXISTS installed_mods (\n"
                +"\tid integer PRIMARY KEY AUTOINCREMENT,\n"
                +"\tname text NOT NULL,\n"
                +"\towner text NOT NULL,\n"
                +"\tversion text NOT NULL,\n"
                +"\tfull_name text NOT NULL\n"
                +");";

        try(
            Statement stmt = Database.connection.createStatement()){

            stmt.execute(createStatement);
        }catch(SQLException s){
            s.printStackTrace();
        }
    }

    public static void addMod(ModPackage modPackage){
        String modName = modPackage.getName();
        String modOwner = modPackage.getOwner();
        PackageVersion installedVersion = modPackage.getInstalledPackageVersion();
        String version = installedVersion.getVersion_number();
        String fullName = installedVersion.getFull_name();

        String insertStmt = "INSERT INTO installed_mods(id, name, owner, version, full_name) VALUES (NULL,?,?,?,?);";

        try(PreparedStatement ps = connection.prepareStatement(insertStmt)){


            ps.setString(1, modName);
            ps.setString(2, modOwner);
            ps.setString(3, version);
            ps.setString(4, fullName);

            ps.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void removeMod(ModPackage modPackage) {
        String modName = modPackage.getName();
        String modOwner = modPackage.getOwner();

        String selectStatement = "DELETE FROM installed_mods WHERE name = ? AND owner = ?;";

        try(PreparedStatement ps = connection.prepareStatement(selectStatement)) {

            ps.setString(1, modName);
            ps.setString(2, modOwner);

            ps.executeUpdate();
        }catch(SQLException s){
            s.printStackTrace();
        }
    }

    public static String getInstalledVersion(String namespace, String name){
        String selectStatement = "SELECT version FROM installed_mods WHERE owner = ? AND name = ?";
        String installedVersion = null;
        try(PreparedStatement ps = connection.prepareStatement(selectStatement)) {
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

    public static boolean modIsInstalled(String name, String namespace) throws SQLException {
        String selectStatement = "SELECT * FROM installed_mods WHERE owner = ? AND name = ?";

        try(PreparedStatement ps = connection.prepareStatement(selectStatement)) {
            ps.setString(1, namespace);
            ps.setString(2, name);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        }catch(SQLException s){
            s.printStackTrace();
        }
        return false;


    }

    public static int modIsInstalled(String name, String namespace, String latestVersion) throws SQLException {

        String selectStatement = "SELECT * FROM installed_mods WHERE owner = ? AND name = ?";
        int returnVal = -1;

        try(PreparedStatement ps = connection.prepareStatement(selectStatement)) {
            ps.setString(1, namespace);
            ps.setString(2, name);
            String installedVersion = null;

            ResultSet rs = ps.executeQuery();

            if(!rs.next()) return -1;

            returnVal += 1;
            installedVersion = rs.getString("version");
            DefaultArtifactVersion installed = new DefaultArtifactVersion(installedVersion);
            DefaultArtifactVersion latest = new DefaultArtifactVersion(latestVersion);
            if(latest.compareTo(installed) > 0){
                returnVal += 1;
            }
        }catch(SQLException s){
            s.printStackTrace();
        }
        return returnVal;


    }

    public static List<List<String>> getInstalledMods() throws SQLException{

        List<List<String>> installedMods = new ArrayList<>();
        String selectStatement = "SELECT name, owner, version, full_name FROM installed_mods";

        PreparedStatement ps = connection.prepareStatement(selectStatement);
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
