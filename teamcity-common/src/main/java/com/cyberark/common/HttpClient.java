package com.cyberark.common;

import java.io.*;

import javax.net.ssl.*;

import com.cyberark.common.exceptions.*;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

public class HttpClient {
    private static ByteArrayInputStream getInputStreamFromString(String input) throws IOException {
        return new ByteArrayInputStream(input.getBytes());
    }

    public static SSLSocketFactory getSSLSocketFactory(String certificateContent) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(getInputStreamFromString(certificateContent));

        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null);
        ks.setCertificateEntry("conjurTlsCaPath", cert);
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext conjurSSLContext = SSLContext.getInstance("TLS");
        conjurSSLContext.init(null, tmf.getTrustManagers(), null);

        return conjurSSLContext.getSocketFactory();
    }


    public static HttpResponse get(String urlString, String authHeader, String certificateContent) {
        return request(urlString, "GET", authHeader, null, certificateContent);
    }

    public static HttpResponse post(String urlString, String authHeader, String body, String certificateContent) {
        return request(urlString, "POST", authHeader, body, certificateContent);
    }

    public static HttpResponse patch(String urlString, String authHeader, String body, String certificateContent) {
        return request(urlString, "PATCH", authHeader, body, certificateContent);
    }

    public static HttpResponse put(String urlString, String authHeader, String body, String certificateContent) {
        return request(urlString, "PUT", authHeader, body, certificateContent);
    }

    public static HttpResponse request(String urlString, String method, String authHeader, String body, String certificateContent) {
        String output = "";
        int statusCode = 0;
        try {
            // Create http connection with Authorization header and correct Content-Type
            URL url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            if (certificateContent != null && !certificateContent.trim().isEmpty()) {
                conn.setSSLSocketFactory(getSSLSocketFactory(certificateContent));
            }

            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", authHeader);

            // java doesn't allow some http verbs so you have to make the method POST and then create an override property
            if (!method.equals("GET") && !method.equals("POST") && !method.equals("DELETE") && !method.equals("PUT")) {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", method);
            } else {
                conn.setRequestMethod(method);
            }

            // Do not write body to request if body is empty or null or method is GET
            if (body != null && !method.equals("GET") && !body.equals("")) {
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.flush();
            }

            statusCode = conn.getResponseCode();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String tmp;
            while ((tmp = br.readLine()) != null) {
                output = output + tmp;
            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            statusCode = -1;
        } catch (IOException e) {
            output = "";
        } catch (CertificateException e) {
            statusCode = -2;
        } catch (NoSuchAlgorithmException e) {
            statusCode = -3;
        } catch (KeyStoreException e) {
            statusCode = -4;
        } catch (KeyManagementException e) {
            statusCode = -5;
        }

        return new HttpResponse(output, statusCode);
    }


    // ===============================================================
    // String base64Encode() - base64 encodes argument and returns encoded string
    //
    public static String base64Encode(String input) {
        String encodedString = "";
        try {
            encodedString = Base64.getEncoder().encodeToString(input.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return encodedString;
        }
        return encodedString;
    } // base64Encode

}