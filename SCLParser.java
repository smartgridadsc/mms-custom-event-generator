import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SCLParser {

    // target file to write to
    private static final String csvFilename = "./user_configuration.csv";

    private static String getIcdDomainItemID(Node node, Node iedName, Node logicalDeviceName) {
        String attributeFC = "";
        String attributeName = "";
        String objID = "";
        String objectName = "";
        String logicalName = "";
        String serverName = "";
        String deviceName = "";

        Node currentNode = node;
        while (currentNode.getNodeName() != "DA") {
            currentNode = currentNode.getParentNode();
        }

        // retrieve attribute name and functional constraint
        Element elementDA = (Element) currentNode;
        attributeName = elementDA.getAttribute("name");
        attributeFC = elementDA.getAttribute("fc");
        
        while (currentNode.getNodeName() != "DOType") {
            currentNode = currentNode.getParentNode();
        }

        // retrieve data object id
        Element elementDOid = (Element) currentNode;
        objID = elementDOid.getAttribute("id");

        while (currentNode.getNodeName() != "DataTypeTemplates") {
            currentNode = currentNode.getParentNode();
        }
        
        // retrieve data object name and logical node name
        NodeList childList = currentNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node childListNode = childList.item(i);
            if (childListNode.getNodeName() == "LNodeType") {
                NodeList childchildList = childListNode.getChildNodes();
                for (int j = 0; j < childchildList.getLength(); j++) {
                    Node childchildListNode = childchildList.item(j);
                    if (childchildListNode.getNodeName() == "DO") {
                        Element elementDO = (Element) childchildListNode;
                        String typeVal = elementDO.getAttribute("type");
                        if (typeVal.contains(objID)) {
                            objectName = elementDO.getAttribute("name");
                            Element elementLN = (Element) childListNode;
                            logicalName = elementLN.getAttribute("id").replace("_","");
                        }
                    }
                    childchildListNode = null;
                }
                childchildList = null;
            }
            childListNode = null;
        }
        childList = null;

        // retrieve IED name
        Element elementIED = (Element) iedName;
        serverName = elementIED.getAttribute("name");

        // retrieve logical device name
        Element elementLD = (Element) logicalDeviceName;
        deviceName = elementLD.getAttribute("inst");

        return serverName + deviceName + "_" + logicalName + "$" + attributeFC + "$" + objectName + "$" + attributeName;
    }

    private static String getIcdIndexDataType(Node node, NodeList icdDataType) {
        String index = "";
        String dataType = "";
        String attributeType = "";

        List<String> dataAttributesStringList = new ArrayList<String>();

        Node currentNode = node;

        while (currentNode.getNodeName() != "Private") {
            currentNode = currentNode.getParentNode();
        }

        Element structName = (Element) currentNode;
        String  privateName = structName.getAttribute("name");

        while (currentNode.getNodeName() != "DA") {
            currentNode = currentNode.getParentNode();
        }

        // retrieve attribute Btype
        Element element = (Element) currentNode;
        String attributeBtype = element.getAttribute("bType");
        String dataAttributeName = element.getAttribute("name");

        if (attributeBtype.contains("Struct")) {
            // retrieve attribute type
            attributeType = element.getAttribute("type");
            for (int i = 0; i < icdDataType.getLength(); i++) {
                Node icdDataTypeList = icdDataType.item(i);
                Element elementId = (Element) icdDataTypeList;
                String dataTypeId = elementId.getAttribute("id");
                if (dataTypeId.contains(attributeType)) {
                    NodeList childList = icdDataTypeList.getChildNodes();
                    for (int j = 0; j < childList.getLength(); j++) {
                        Node childListNode = childList.item(j);
                        if (childListNode.getNodeName() == "BDA") {
                            Element bda = (Element) childListNode;
                            String bdaId = bda.getAttribute("name");
                            if (bdaId.contains(privateName)) {
                                dataType = bda.getAttribute("bType");
                                break;
                            }
                            else {
                                dataType = bda.getAttribute("bType");
                            }
                        }
                    }
                }
            }
        }else {
            dataType = attributeBtype;
        }

        NodeList dataAttributesList = currentNode.getChildNodes();
        for (int i = 0; i < dataAttributesList.getLength(); i++) {
            Node dataAttributesNode = dataAttributesList.item(i);
            if (dataAttributesNode.getNodeName() == "Private") {
                Element dAelem = (Element) dataAttributesNode;
                String elemstring = dAelem.getAttribute("name");
                if (!elemstring.contains(privateName)) {
                    NodeList privateNodeList = dataAttributesNode.getChildNodes();
                    for (int j = 0; j < privateNodeList.getLength(); j++) {
                        Node privElement = privateNodeList.item(j);
                        if (privElement.getNodeName() == "Private") {
                            Element elem = (Element) privElement;
                            dataAttributesStringList.add(elem.getAttribute("name"));
                        }
                    }
                }
                else{
                    dataAttributesStringList.add(dAelem.getAttribute("name"));
                }    
            }
        }
		// retrieve the index position of the variable 
        for (String string : dataAttributesStringList) {
            if (string.contains(privateName)) {
                index = String.valueOf(dataAttributesStringList.indexOf(string));
            }
        }

        return "," + index + "," + dataType;
    }

    public static boolean ParseDOM(String targetConfigurationFile) {
        // configuration file input stream
        File inputFile = new File(targetConfigurationFile);
        Scanner inputStream;

        try {
            inputStream = new Scanner(inputFile);

            // mms.icd DOM
            DocumentBuilderFactory icdFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder icdBuilder = icdFactory.newDocumentBuilder();
            Document icdDocument = icdBuilder.parse("sample.icd");

            // pointer to the IED name tag
            NodeList icdIed = icdDocument.getElementsByTagName("IED");
            Node iedName = icdIed.item(0);

            // pointer to the logical device tag
            NodeList icdLogicalDevice = icdDocument.getElementsByTagName("LDevice");
            Node logicalDeviceName = icdLogicalDevice.item(0);

            // pointer to the data attribute type tag
            NodeList icdDataType = icdDocument.getElementsByTagName("DAType");

            // pointer to the data attribute private tag
            NodeList icdAttributeTextList = icdDocument.getElementsByTagName("Property");

            // file writer 
            FileWriter csvFileWriter = new FileWriter(csvFilename);
            PrintWriter csvOutputStream = new PrintWriter(csvFileWriter);
            csvOutputStream.println("device_variable_name,domainID_itemID,index,data_type");

            // each line
            String csvLine;
            String domainItemID = "";
            String indexDataType = "";

            // Skip configuration file first line
            String attributeNames = inputStream.nextLine();

            while (inputStream.hasNext()) {
                String attribute = inputStream.nextLine();
                
                // scanning mms DOM
                for (int i = 0; i < icdAttributeTextList.getLength(); i++) {
                    Node node = icdAttributeTextList.item(i);
                    Element element = (Element) node;
                    String propertyVal = element.getAttribute("Value");
                    if (propertyVal.contains(attribute)) {
                        domainItemID = getIcdDomainItemID(node, iedName, logicalDeviceName);
                        indexDataType = getIcdIndexDataType(node, icdDataType);
                    }
                }

                csvLine = attribute + "," + domainItemID + indexDataType;
                csvOutputStream.println(csvLine);
                csvLine = "";
                domainItemID = "";
                indexDataType = "";
            }

            // write to file
            csvOutputStream.close();

            // free memory
            icdFactory = null;
            icdBuilder = null;
            icdDocument = null;
            icdAttributeTextList = null;
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static void main(String[] args) {
        // run csv 
        String configurationFile = "./mms_configuration_partial.csv";
        ParseDOM(configurationFile);
    }

}