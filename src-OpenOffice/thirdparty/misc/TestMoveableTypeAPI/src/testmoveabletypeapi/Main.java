/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package testmoveabletypeapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 *
 * @author martin
 */
public class Main {

    private XmlRpcClient m_client;

    private String m_blogURL = "http://www.oslobadjanje.com/xmlrpc/index.php";

    private String m_appkey = "";
    private String m_username = "martin";
    private String m_password = "maslina";

    private int BLOCK_SIZE = 4096;

    public Main() throws MalformedURLException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(m_blogURL));

        m_client = new XmlRpcClient();
        m_client.setConfig(config);

    }

    public static String mapToString(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String key : map.keySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            String value = map.get(key);
            try {
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return stringBuilder.toString();
    }

    public static Map<String, String> stringToMap(String input) {
        Map<String, String> map = new HashMap<String, String>();

        String[] nameValuePairs = input.split("&");
        for (String nameValuePair : nameValuePairs) {
            String[] nameValue = nameValuePair.split("=");
            try {
                map.put(URLDecoder.decode(nameValue[0], "UTF-8"), nameValue.length > 1 ? URLDecoder.decode(
                        nameValue[1], "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return map;
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws MalformedURLException, XmlRpcException, IOException {
        HashMap<String, String> a = new HashMap<String, String>();

        a.put("a", "http://www.google.hr/webhp?sourceid=chrome-instant&ie=UTF-8&ion=1&nord=1#sclient=psy&hl=hr&nord=1&site=webhp&source=hp&q=java%20HashMap%20string%20to%20String&aq=&aqi=&aql=&oq=&pbx=1&fp=c67a75d8f67b562&ion=1&ion=1&fp=c67a75d8f67b562&ion=1&biw=1325&bih=910");
        a.put("b", "2");

        System.out.println(mapToString(a));

        System.out.println(mapToString(stringToMap(mapToString(a))));

        /*

        DecimalFormat df = new DecimalFormat("#.#####");
        System.out.println(df.format(234.231457674));
        System.out.println(df.format(234.231000000));

        Main objMain = new Main();

        Object[] usersBlogs = (Object[]) objMain.getUsersBlogs();
        //Object newMediaObject = objMain.newMediaObject();
        Object categories = objMain.getCategories();
        Object o2 = categories;
        */
    }

    public Object getUsersBlogs() throws XmlRpcException {
        return m_client.execute("blogger.getUsersBlogs", new Object[]{
                    m_appkey,
                    m_username,
                    m_password
                });
    }

    public Object newMediaObject() throws XmlRpcException, IOException {
        HashMap file = new HashMap();
        file.put("name", "test_newMediaObject.png");
        file.put("bits", readFile("C:\\Users\\martin\\100002010000014B000000B3559B8AB1.png"));
        file.put("overwrite", Boolean.TRUE);
        return m_client.execute("metaWeblog.newMediaObject", new Object[]{
                    1,
                    m_username,
                    m_password,
                    file
                });
    }

    private byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[(int)file.length()];
        fis.read(buffer);
        fis.close();
        return buffer;
    }

    public Object getCategories() throws XmlRpcException {
        return m_client.execute("mt.getCategoryList", new Object[]{
                    1,
                    m_username,
                    m_password
                });
    }
}
