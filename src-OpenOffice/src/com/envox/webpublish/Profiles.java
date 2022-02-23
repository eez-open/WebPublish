/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.sun.star.uno.XComponentContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author martin
 */
public class Profiles {
    XComponentContext m_xContext;
    private Map<String, Profile> m_profiles = new TreeMap<String, Profile>();

    public Profiles(XComponentContext xContext) {
        m_xContext = xContext;
        set(load());
    }

    public Map<String, Profile> get() { 
        return m_profiles;
    }

    public boolean hasProfile(String profileName) {
        return m_profiles.containsKey(profileName);
    }

    public Profile getProfile(String profileName) {
        return m_profiles.get(profileName);
    }

    private static void init(List<Profile> profiles) {
        profiles.add(new Profile(Profile.Type.EPUB));
        profiles.add(new Profile(Profile.Type.HTML));
        profiles.add(new Profile(Profile.Type.Joomla));
        profiles.add(new Profile(Profile.Type.WordPress));

    }

    private void set(List<Profile> profiles) {
        m_profiles.clear();
        for (Profile profile:profiles) {
            m_profiles.put(profile.getProfileName(), profile);
        }
    }

    final public List<Profile> load() {
        List<Profile> profiles = new ArrayList<Profile>();

        String profilesXML = Util.getStringConf("Options", "Options", "Profiles", "");
        if (!StringUtils.isBlank(profilesXML)) {
            try {
                profiles = loadFromXML(profilesXML);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(Profiles.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SAXException ex) {
                Logger.getLogger(Profiles.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Profiles.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            init(profiles);
        }

        return profiles;
    }

    final public void save(List<Profile> profiles) {
        String xmlProfiles = "";
        try {
            xmlProfiles = saveToXML(profiles);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Profiles.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(Profiles.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(Profiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        Util.setStringConf("Options", "Options", "Profiles", xmlProfiles);
        set(profiles);
    }

    private static List<Profile> loadFromXML(Document doc) {
        doc.getDocumentElement().normalize();

        List<Profile> profiles = new ArrayList();

        Element profilesElement = doc.getDocumentElement();
        NodeList nodeList = profilesElement.getElementsByTagName("Profile");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Element profileElement = (Element) nodeList.item(i);
            Profile profile = new Profile();
            profile.loadFromXML(profileElement);
            profiles.add(profile);
        }

        return profiles;
    }

    private static List<Profile> loadFromXML(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
        return loadFromXML(doc);
    }

    public static List<Profile> loadFromXML(URL url) throws ParserConfigurationException, SAXException, IOException {
        File file = Util.toFile(url);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);
        return loadFromXML(doc);
    }

    private static void saveToXML(List<Profile> profiles, StreamResult result) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element profilesElement = doc.createElement("Profiles");
        doc.appendChild(profilesElement);

        for (Profile profile:profiles) {
            Element profileElement = doc.createElement("Profile");
            profile.saveToXML(profileElement, doc);
            profilesElement.appendChild(profileElement);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
    }

    public static String saveToXML(List<Profile> profiles) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        StreamResult result =  new StreamResult(new StringWriter());
        saveToXML(profiles, result);
        return result.getWriter().toString();
    }

    public static void saveToXML(List<Profile> profiles, URL url) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        saveToXML(profiles, new StreamResult(Util.toFile(url)));
    }
}
