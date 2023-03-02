package gr.forth.ics.isl.xmlify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class Transformer {
    private static int OUTPUT_PART_COUNTER=1;
    
    private static Map<Integer,String> getHeadersNormalized(File csvFile) throws IOException{
        Map<Integer,String> headerMap=new HashMap<>();
        String headerLine=FileUtils.readLines(csvFile, "UTF-8").get(0);
        String[] headersRaw=headerLine.split(",");
        for(int i=0;i<headersRaw.length;i++){
            headerMap.put(i, normalizeHeader(headersRaw[i]));
        }
        return headerMap;
    }
    
    private static String normalizeHeader(String headerRaw){
        return headerRaw.trim()
                        .replaceAll(" ", "_")
                        .replaceAll("/", "_");
    }
    
    private static void transformToXml(File csvFile, long lineSplitter) throws IOException, ParserConfigurationException, TransformerException{
        CSVParser csvParser=CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(csvFile), "UTF-8"));
        Map<Integer,String> headersMap=getHeadersNormalized(csvFile);
        long currentLine=0;
        
        Document document=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement=document.createElement("root");
        document.appendChild(rootElement);
                
        for(CSVRecord record : csvParser){
            currentLine+=1;
            if(record.getRecordNumber()==1){    //This is the header row
                continue;
            }
            Element rowElement=document.createElement("row");
            for(int i=0;i<headersMap.keySet().size();i++){
                rowElement.appendChild(createElementWithText(document, headersMap.get(i), record.get(i)));
            }
            rootElement.appendChild(rowElement);
            if(lineSplitter>0 && currentLine>=lineSplitter){
                exportXmlToFile(document, getOutputFile(csvFile, true));
                currentLine=0;
                document=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                rootElement=document.createElement("root");
                document.appendChild(rootElement);
            }
        }
        exportXmlToFile(document, getOutputFile(csvFile, lineSplitter>0));
    }
    
    private static Element createElementWithText(Document document, String elementName, String text){
        System.out.println(elementName);
        Element element=document.createElement(elementName);
        if(!text.isEmpty() && !text.equalsIgnoreCase("NULL")){
            element.setTextContent(text);
        }
        return element;
    }
    
    private static void exportXmlToFile(Document document, File file) throws TransformerException {
        javax.xml.transform.Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StreamResult result = new StreamResult(file);
        DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
    }
    
    private static File getOutputFile(File inputFile, boolean splitFiles){
        String folderPath=inputFile.getParent();
        String fileName=FilenameUtils.getBaseName(inputFile.getAbsolutePath());
        String extension="xml";
        if(splitFiles){
            return new File(folderPath+"/"+fileName+"-part"+(OUTPUT_PART_COUNTER++)+"."+extension);
        }else{
            return new File(folderPath+"/"+fileName+"."+extension);
        }
    }
    
    public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerException{
        transformToXml(new File("D:/input.csv"), -1);
    }

}