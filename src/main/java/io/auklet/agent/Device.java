package io.auklet.agent;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Scanner;
import java.nio.file.Files;

public final class Device {

    private Device(){ }

    private static String filename = "/.AukletAuth";

    // AppId is 22 bytes but AES is a 128-bit block cipher supporting keys of 128, 192, and 256 bits.
    private static final Key aesKey = new SecretKeySpec(Auklet.AppId.substring(0,16).getBytes(), "AES");

    private static String client_id;
    private static String client_username;
    private static String client_password;
    private static String organization;

    public static boolean register_device(String folderPath){

        try {
            Path fileLocation = Paths.get(folderPath + filename);
            byte[] data = Files.readAllBytes(fileLocation);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            String decrypted = new String(cipher.doFinal(data));
            setCreds(new JSONObject(decrypted));

        } catch (FileNotFoundException | NoSuchFileException e) {
            JSONObject newObject = create_device();
            if (newObject != null) {
                setCreds(newObject);
                writeCreds(folderPath + filename);
            }
            else return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static JSONObject create_device(){
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            JSONObject obj = new JSONObject();
            obj.put("mac_address_hash", Util.getMacAddressHash());
            obj.put("application", Auklet.AppId);
            HttpPost request = new HttpPost(Auklet.getBaseUrl() + "/private/devices/");
            StringEntity params = new StringEntity(obj.toString());
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "JWT "+Auklet.ApiKey);
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            if(response.getStatusLine().getStatusCode() == 201) {
                String text;
                try (Scanner scanner = new Scanner(response.getEntity().getContent(), StandardCharsets.UTF_8.name())) {
                    text = scanner.useDelimiter("\\A").next();
                } catch (Exception e) {
                    System.out.println("Exception during reading contents of create device: " + e.getMessage());
                    return null;
                }

                return new JSONObject(text);
            } else {
                System.out.println("could not create a device and status code is: " +
                        response.getStatusLine().getStatusCode());
            }
        } catch (Exception ex) {

            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    private static void setCreds(JSONObject jsonObject) {

        client_password = (String) jsonObject.get("client_password");

        client_username = (String) jsonObject.get("id");

        client_id = (String) jsonObject.get("client_id");

        organization = (String) jsonObject.get("organization");
    }

    private static void writeCreds(String filename){

        JSONObject obj = new JSONObject();
        obj.put("client_password", client_password);
        obj.put("id", client_username);
        obj.put("client_id", client_id);
        obj.put("organization", organization);

        try (FileOutputStream file = new FileOutputStream(filename)) {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encrypted = cipher.doFinal(obj.toString().getBytes());

            file.write(encrypted);
            file.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String getClient_Username(){
        return client_username;
    }

    public static String getClient_Password(){
        return client_password;
    }

    public static String getClient_Id(){
        return client_id;
    }

    public static String getOrganization(){
        return organization;
    }

    public static boolean get_Certs(String folderPath) {
        try {
            File file = new File(folderPath + "/CA");
            if (file.createNewFile()) {
                HttpResponse response = httpGet("/private/devices/certificates/");
                if (response.getStatusLine().getStatusCode() == 200) {
                    InputStream ca = response.getEntity().getContent();
                    String text;
                    try (Scanner scanner = new Scanner(ca, StandardCharsets.UTF_8.name())) {
                        text = scanner.useDelimiter("\\A").next();
                    }

                    FileWriter writer = new FileWriter(folderPath + "/CA");
                    writer.write(text);
                    writer.close();
                    System.out.println("CA File is created!");
                } else {
                    System.out.println("Get cert response code: " + response.getStatusLine().getStatusCode());
                    if(file.delete()){
                        System.out.println("CA file deleted");
                        return false;
                    }
                }
            } else {
                System.out.println("CA File already exists.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean initLimitsConfig(String folderPath) {
        try {
            String limits = folderPath + "/limits";
            File limitsFile = new File(limits);
            limitsFile.createNewFile();
            DataRetention.setUsageFile(folderPath + "/usage");

            HttpResponse response = httpGet(String.format("/private/devices/%s/config/", Auklet.AppId));
            if (response.getStatusLine().getStatusCode() == 200) {
                InputStream config = response.getEntity().getContent();
                String text;
                try (Scanner scanner = new Scanner(config, StandardCharsets.UTF_8.name())) {
                    text = scanner.useDelimiter("\\A").next();
                }
                JSONObject conf = new JSONObject(text).getJSONObject("config");
                DataRetention.initDataRetention(conf);

                FileWriter writer = new FileWriter(limits);
                writer.write(conf.toString());
                writer.close();
                System.out.println("Config File was stored");
            } else {
                System.out.println("Get config response code: " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    public static HttpResponse httpGet(String link) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        URL newUrl = new URL(Auklet.getBaseUrl() + link);
        HttpURLConnection con = (HttpURLConnection) newUrl.openConnection();

        con.setRequestProperty("Authorization", "JWT " + Auklet.ApiKey);
        con.setDoInput(true);
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);

        con.getResponseCode();

        HttpGet request = new HttpGet(con.getURL().toURI());
        return httpClient.execute(request);
    }
}
