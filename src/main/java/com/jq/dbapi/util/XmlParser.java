package com.jq.dbapi.util;

import com.jq.dbapi.entity.DataSource;
import com.jq.dbapi.entity.Sql;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: dbApi-starter
 * @description:
 * @author: jiangqiang
 * @create: 2021-03-11 13:56
 **/
public class XmlParser {

    public static void main(String[] args) throws Exception {
/*        String text = "<sql>\n" +
                "    <select id=\"getUser\" db=\"mysql\">\n" +
                "        select * from user\n" +
                "        <where>\n" +
                "            <if test = \"id != null\">\n" +
                "                id = #{id}\n" +
                "            </if>\n" +
                "        </where>\n" +
                "    </select>\n" +
                "\n" +
                "</sql>";
        parseSql(text);*/

        String text = "<datasource>\n" +
                "    <ds id=\"mysql\">\n" +
                "        <url>jdbc:mysql://localhost:3306/story?useSSL=false&amp;characterEncoding=UTF-8</url>\n" +
                "        <username>root</username>\n" +
                "        <password>root</password>\n" +
                "    </ds>\n" +
                "\n" +
                "</datasource>";
        parseDatasource(text);
    }

    public static Map<String, DataSource> parseDatasource(String text) throws ParserConfigurationException, IOException, SAXException {
        Map<String, DataSource> map = new HashMap<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document document = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes()));
        Element documentElement = document.getDocumentElement();
        NodeList children = documentElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap attributes = child.getAttributes();
                Node idAttr = attributes.getNamedItem("id");
                String id = idAttr.getTextContent();
                DataSource dataSource = new DataSource();
                dataSource.setId(id);

                NodeList childNodes = child.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node node = childNodes.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String nodeName = node.getNodeName();
                        if ("url".equals(nodeName)) {
                            dataSource.setUrl(node.getTextContent());
                        } else if ("username".equals(nodeName)) {
                            dataSource.setUsername(node.getTextContent());
                        } else if ("password".equals(nodeName)) {
                            dataSource.setPassword(node.getTextContent());
                        }
                    }
                }

                map.put(id,dataSource);

            }
        }
        return map;
    }

    public static Map<String, Sql> parseSql(String text) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document document = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes()));
        Element documentElement = document.getDocumentElement();
        NodeList children = documentElement.getChildNodes();

        Map<String, Sql> map = new HashMap<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodeName = child.getNodeName();

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap attributes = child.getAttributes();
                Node idAttr = attributes.getNamedItem("id");
                String id = idAttr.getTextContent();

                Node dbAttr = attributes.getNamedItem("db");
                String db = dbAttr.getTextContent();

                String txt = nodeContentToString(child);

                Sql sql = new Sql();
                sql.setDatasourceId(db);
                sql.setText(txt);
                sql.setId(id);
                sql.setType(nodeName);
                map.put(id, sql);

            }
        }
        return map;

    }

    private static String nodeToString(Node node) throws TransformerException {
        StringWriter sw = new StringWriter();

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));

        return sw.toString();
    }

    private static String nodeContentToString(Node node) throws TransformerException {
        NodeList nodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < nodes.getLength(); j++) {
            Node n = nodes.item(j);
            String txt = nodeToString(n);
            sb.append(txt);
        }
        return sb.toString().trim();
    }
}
