package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DesktopClient {
    private DesktopClient() {
    }

    public static void main(String[] args) throws IOException {
        String endpoint = args.length > 0 ? args[0] : "http://localhost:8080/Lab1_war_exploded/controller";
        String value = args.length > 1 ? args[1] : "1";
        URL url = new URL(endpoint + "?value=" + value + "&response=text");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            System.out.println(reader.readLine());
        } finally {
            connection.disconnect();
        }
    }
}