package com.infy.claims.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Legacy XML claim parser — used for the 2016 partner integration
 * (ABC Insurance batch feed). Partner switched to JSON in 2020 so
 * this class hasn't been called for years, but ops has asked us
 * to keep it "just in case".
 *
 * NOTE: SAXReader with default settings is vulnerable to XXE.
 * Not an issue while nothing calls this class.
 */
public class LegacyXmlParser {

    private static final Logger log = LoggerFactory.getLogger(LegacyXmlParser.class);

    public List<Map<String, String>> parse(String xml) {
        List<Map<String, String>> claims = new ArrayList<>();
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(new StringReader(xml));
            Element root = doc.getRootElement();
            for (Iterator<Element> it = root.elementIterator("claim"); it.hasNext(); ) {
                Element el = it.next();
                Map<String, String> m = new HashMap<>();
                m.put("id", el.elementText("id"));
                m.put("amount", el.elementText("amount"));
                m.put("customer", el.elementText("customer"));
                claims.add(m);
            }
        } catch (DocumentException e) {
            log.error("parse failed: " + e.getMessage());
        }
        return claims;
    }
}
