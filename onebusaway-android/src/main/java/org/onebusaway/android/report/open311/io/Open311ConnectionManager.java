/*
* Copyright (C) 2014 University of South Florida (sjbarbeau@gmail.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.onebusaway.android.report.open311.io;

import android.graphics.Bitmap;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.report.open311.constants.Open311Constants;
import org.onebusaway.android.report.open311.utils.Open311UrlUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Manager that manages all http connections
 * while communicating any open311 server.
 *
 * @author Cagri Cetin
 */
public class Open311ConnectionManager {

    private static CookieStore cookieStore = new BasicCookieStore();
    private DefaultHttpClient httpClient = null;

    String attachmentName = "bitmap";
    String attachmentFileName = "bitmap.bmp";
    String crlf = "\r\n";
    String twoHyphens = "--";
    String boundary = "*****";

    public Open311ConnectionManager() {
        BasicHttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        SSLSessionCache sslSession = new SSLSessionCache(Application.get().getApplicationContext());
        schemeRegistry.register(new Scheme("https", SSLCertificateSocketFactory.getHttpSocketFactory(10 * 60 * 1000, sslSession), 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        HttpConnectionParams.setConnectionTimeout(params, Open311Constants.WS_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, Open311Constants.WS_TIMEOUT);

        httpClient = new DefaultHttpClient();

        httpClient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-8");
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Open311Constants.WS_TIMEOUT);
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Open311Constants.WS_TIMEOUT);
    }

    /**
     * Makes connection to any server.
     *
     * @param url           Destination url
     * @param requestMethod request method Post or Get
     * @param httpEntity    http entity contains parameters for the request
     * @return Returns the result of the request as string
     */
    public String getStringResult(String url, Open311UrlUtil.RequestMethod requestMethod, HttpEntity httpEntity) {

        HttpResponse response;
        String result = null;
        try {

            if (requestMethod == Open311UrlUtil.RequestMethod.POST) {
                response = postMethod(url, httpEntity);
            } else {
                response = getMethod(url, httpEntity);
            }

            HttpEntity resEntity = response.getEntity();

            if (resEntity != null) {
                result = EntityUtils.toString(resEntity);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String getStringResult(String dest, HttpEntity entity) {
        String response = "";
        try {
            URL url = new URL(dest);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(Open311UrlUtil.nameValuePairsToParams(entity));


            ////////////
            ////////////

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            Bitmap bitmap = Application.get().btm;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;

            try {
                File sourceFile = new File(Application.get().getCacheDir(), "armut.png");
                sourceFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(sourceFile);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                DataOutputStream dos = null;
                dos = new DataOutputStream(conn.getOutputStream());


                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"media\";filename=\"" + "Armut.png" + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                dos.flush();
//                dos.close();
            } catch (Exception e){
                e.printStackTrace();
            }


            ////////////
            ///////////
            writer.flush();
            writer.close();
            os.close();


            conn.connect();

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public int uploadFile(final String urlx, HttpEntity httpEntity) throws IOException {

        String fileName = "test.png";

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        Bitmap bitmap = Application.get().btm;

        File sourceFile = new File(Application.get().getCacheDir(), fileName);
        sourceFile.createNewFile();

//Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
        FileOutputStream fos = new FileOutputStream(sourceFile);
        fos.write(bitmapdata);
        fos.flush();
        fos.close();

        if (!sourceFile.isFile()) {

            return 0;

        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(urlx);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                //  conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data");
                conn.setRequestProperty("uploaded_file", fileName);


                dos = new DataOutputStream(conn.getOutputStream());


                dos.writeBytes(twoHyphens + boundary + lineEnd);

//Adding Parameter name

                List<NameValuePair> valuePairs = URLEncodedUtils.parse(httpEntity);
                for (NameValuePair nvp : valuePairs) {

                    dos.writeBytes("Content-Disposition: form-data; name=\"" + nvp.getName() + "\"" + lineEnd);
                    dos.writeBytes("Content-Type: text/plain; charset=US-ASCII" + lineEnd);
                    dos.writeBytes("Content-Transfer-Encoding: 8bit" + lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(nvp.getValue()); // mobile_no is String variable
                    dos.writeBytes(lineEnd);

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                }

//                Adding Parameter media file(audio,video and image)

                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"media\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                // create a buffer of maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                String serverResponseMessage = conn.getResponseMessage();


                // close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();


            } catch (MalformedURLException ex) {

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (final Exception e) {

                e.printStackTrace();
            }
            return 1;
        }
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * Creates requests without any parameters.
     *
     * @param url
     * @param requestMethod
     * @return
     */
    public String getStringResult(String url, Open311UrlUtil.RequestMethod requestMethod) {
        return getStringResult(url, requestMethod, null);
    }

    /**
     * Internal method for post method
     *
     * @param url
     * @param httpEntity
     * @return
     * @throws org.apache.http.client.ClientProtocolException
     * @throws java.io.IOException
     */
    private HttpResponse postMethod(String url, HttpEntity httpEntity) throws ClientProtocolException, IOException {

        HttpPost httppost = new HttpPost(url);
        httppost.setHeader("Content-Type", "multipart/form-data;charset=UTF-8");
        if (httpEntity != null) {
            httppost.setEntity(httpEntity);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        httpEntity.writeTo(bytes);
        String content = bytes.toString("UTF-8");
        return httpClient.execute(httppost);
    }

    /**
     * Internal method for get methods.
     *
     * @param url
     * @param httpEntity
     * @return
     * @throws org.apache.http.client.ClientProtocolException
     * @throws java.io.IOException
     */
    private HttpResponse getMethod(String url, HttpEntity httpEntity) throws ClientProtocolException, IOException {

        if (httpEntity != null) {
            url += Open311UrlUtil.nameValuePairsToParams(httpEntity);
        }

        HttpGet httpGet = new HttpGet(url);
        HttpContext ctx = new BasicHttpContext();
        return httpClient.execute(httpGet, ctx);
    }
}
