package io.auklet.agent;

import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class Util {

    private Util(){ }

    protected static String getMacAddressHash() {
        String machash = "";
        NetworkInterface networkinterface = null;
        try {
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            for (; n.hasMoreElements();)
            {
                NetworkInterface e = n.nextElement();
                if (!e.isLoopback()) { // Check against network interface "127.0.0.1"
                    networkinterface = e;
                }
                if(e.getHardwareAddress() != null) {
                    break;
                }
            }

            byte[] mac = networkinterface.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }

            byte[] macBytes = String.valueOf(sb).getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] macHashByte = md.digest(macBytes);
            machash = Hex.encodeHexString(macHashByte);


        } catch (SocketException | NoSuchAlgorithmException | UnsupportedEncodingException e) {

            e.printStackTrace();

        }
        return machash;

    }

    protected static String getIpAddress(){
        String ipAddr = "";
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            ipAddr = in.readLine(); //you get the IP as a String

        } catch (IOException e) {
            e.printStackTrace();

        }
        return ipAddr;
    }

    protected static String createCustomFolder(String sysProperty) {

        String path = System.getProperty(sysProperty) + File.separator + "aukletFiles";
        File newfile = new File(path);
        if (newfile.exists()){
            System.out.println("folder already exists");
        } else if (newfile.mkdir()){
            System.out.println("folder created");
        } else {
            System.out.println("folder was not created for " + sysProperty);
            return null;
        }

        return path;
    }
}
