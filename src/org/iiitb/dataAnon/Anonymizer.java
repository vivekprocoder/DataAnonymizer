package org.iiitb.dataAnon;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import javax.xml.parsers.*;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.DataSource;

import java.nio.charset.Charset;
import java.sql.*;
import java.text.DecimalFormat;


import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data.DefaultData;
import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.criteria.*;
import org.deidentifier.arx.metric.Metric;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.deidentifier.arx.AttributeType.Hierarchy;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@MultipartConfig
public class Anonymizer extends HttpServlet  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2218891792103799574L;
	/**
	 * 
	 */
	public DataSource input_source=null;
	public String input_format=null;
	private String file_path;
	private int maxFileSize = 50 * 1024;
	private int maxMemSize = 4 * 1024;
	private File file;
	
	private String config_path;
	public DataDefinition def=null;
	public Data data = null;

	public void init( ){
		      // Get the file location where it would be stored.
		      file_path = getServletContext().getInitParameter("file-upload"); 
		   }
		   
	
	@SuppressWarnings("rawtypes")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		 response.setContentType("text/html");
		 input_format = request.getParameter("format");
		
	try {
			 
		DiskFileItemFactory factory = new DiskFileItemFactory();
		   
	      // maximum size that will be stored in memory
	      factory.setSizeThreshold(maxMemSize);
	   
	      // Location to save data that is larger than maxMemSize.
	      factory.setRepository(new File("/tempfiles"));

	      // Create a new file upload handler
	      ServletFileUpload upload = new ServletFileUpload(factory);
	   
	      // maximum file size to be uploaded.
	      upload.setSizeMax( maxFileSize );
	      
	      // Parse the request to get file items.
	      List fileItems = upload.parseRequest(request);
		
	      // Process the uploaded file items
	      Iterator it = fileItems.iterator();
	      
	      while ( it.hasNext () ) {
	            FileItem fi = (FileItem)it.next();
	            if ( !fi.isFormField () ) {
	               // Get the uploaded file parameters
	               String fieldName = fi.getFieldName();
	               String fileName = fi.getName();
	               String contentType = fi.getContentType();
	               boolean isInMemory = fi.isInMemory();
	               long sizeInBytes = fi.getSize();
	            
	               // Write the file
	               if( fileName.lastIndexOf("\\") >= 0 ) {
	                  file = new File( file_path + fileName.substring( fileName.lastIndexOf("\\"))) ;
	               } else {
	                  file = new File( file_path + fileName.substring(fileName.lastIndexOf("\\")+1)) ;
	               }
	               fi.write( file ) ;
	               System.out.println("Uploaded Filename: " + fileName);
	            }
	      }


	      if(input_format.equalsIgnoreCase("csv"))
	    	  input_source= DataSource.createCSVSource(file,Charset.defaultCharset(),',' , true);
	      else if(input_format.equals("xls"))
	    	  input_source = DataSource.createExcelSource(file,';' , true);
	      else
	      {
	    	  String URL = request.getParameter("URL");
	    	  String User = request.getParameter("username");
	    	  String Password = request.getParameter("password");
	    	  String Table  =request.getParameter("tableName");
	    	  input_source = DataSource.createJDBCSource(URL, User, Password, Table);
	      }
		
	      String Config_file = "/Users/pranithreddy/Desktop/PE/data/config.xml";
		
		
	      ARXConfiguration config = ARXConfiguration.create();
		
		
			
		
			
			
			
			//Reading the config file to import attributes and their types
			File configFile = new File(Config_file);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
			Document doc = dbBuilder.parse(configFile);
			doc.getDocumentElement().normalize();//Normalize the root element
			System.out.println("Root : " + doc.getDocumentElement().getNodeName());
			
			NodeList att_data = doc.getElementsByTagName("AttributeData");
			Node att = att_data.item(0);
			Element eAtt_data = (Element) att;
			NodeList att_list = eAtt_data.getElementsByTagName("Attribute");
			
			//adding attributes to datasource
			for(int i=0;i<att_list.getLength(); i++)
			{
				Node att_node = att_list.item(i);
				System.out.println("attribute :" + att_node.getAttributes().item(0).getNodeValue());
				input_source.addColumn(att_node.getAttributes().item(0).getNodeValue());
			}
			data = Data.create(input_source);
			//print(data.getHandle());
			
			
			def = data.getDefinition();
			
			String h_path = "/Users/pranithreddy/Desktop/PE/data/heirarchies/adult_hierarchy_";
			//Setting up attribute types to data
			for(int i=0;i<att_list.getLength(); i++)
			{
				Node att_node = att_list.item(i);
				Element mnode  = (Element) att_node;
				String attribute = att_node.getAttributes().item(0).getNodeValue();
				NodeList type_list = mnode.getElementsByTagName("AttributeType");
				String  att_type =  type_list.item(0).getTextContent();
				h_path = h_path+attribute+".csv";
				Hierarchy heirarchy = Hierarchy.create(h_path,Charset.defaultCharset(),';');
				if(att_type.equals("QUASI_IDENTIFYING"))
				{
					def.setAttributeType(attribute, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
					def.setAttributeType(attribute, heirarchy);
				}	
				else if(att_type.equals("IDENTIFYING"))
					def.setAttributeType(attribute, AttributeType.IDENTIFYING_ATTRIBUTE);
				else if(att_type.equals("SENSITIVE"))
					def.setAttributeType(attribute, AttributeType.SENSITIVE_ATTRIBUTE);
				else
					def.setAttributeType(attribute, AttributeType.INSENSITIVE_ATTRIBUTE);
				
				h_path = "/Users/pranithreddy/Desktop/PE/data/heirarchies/adult_hierarchy_";
				
			}
			//Importing the privacy criterion
			NodeList pmodel_data = doc.getElementsByTagName("PrivacyModel");
			Node pmodel = pmodel_data.item(0);
			Element eP_model = (Element) pmodel;
			NodeList model_list = eP_model.getElementsByTagName("Model");
			NodeList surpression_list = eP_model.getElementsByTagName("SurpressionRate");
			
			String model = model_list.item(0).getTextContent();
			String surpressionrate = surpression_list.item(0).getTextContent();
			if(model.equals("KAnonymity"))
			{
				String k = model_list.item(0).getAttributes().item(0).getNodeValue();
				config.addPrivacyModel(new KAnonymity(Integer.parseInt(k)));
			}
			config.setSuppressionLimit(Double.parseDouble(surpressionrate));
	        
			System.out.println("model :" + model +" rate: "+ Double.parseDouble(surpressionrate));
			 // Create an instance of the anonymizer
	        ARXAnonymizer anonymizer = new ARXAnonymizer();
	       
	        config.setQualityModel(Metric.createLossMetric());

	        // Now anonymize
	        ARXResult result = anonymizer.anonymize(data, config);

	        DataHandle out_view = result.getOutput();
	        printResult(result, data);
	        out_view.save("/Users/pranithreddy/Desktop/PE/data/output.csv",';');

	        //response.sendRedirect("/anonymizer");
			}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
            // Print input
       /* System.out.println(" - Input data:");
        print(data.getHandle().iterator());

        
        // Print info
        printResult(result, data);

        // Print results
        System.out.println(" - Transformed data:");
        print(result.getOutput(false).iterator());

        
	*/	
		response.setStatus(1);
      
	}
	
   

	
	/**
     * Prints a given data handle.
     *
     * @param handle
     */
    protected static void print(DataHandle handle) {
        final Iterator<String[]> itHandle = handle.iterator();
        print(itHandle);
    }

    /**
     * Prints a given iterator.
     *
     * @param iterator
     */
    protected static void print(Iterator<String[]> iterator) {
        while (iterator.hasNext()) {
            System.out.print("   ");
            System.out.println(Arrays.toString(iterator.next()));
        }
    }

    /**
     * Prints java array.
     *
     * @param array
     */
    protected static void printArray(String[][] array) {
        System.out.print("{");
        for (int j=0; j<array.length; j++){
            String[] next = array[j];
            System.out.print("{");
            for (int i = 0; i < next.length; i++) {
                String string = next[i];
                System.out.print("\"" + string + "\"");
                if (i < next.length - 1) {
                    System.out.print(",");
                }
            }
            System.out.print("}");
            if (j<array.length-1) {
                System.out.print(",\n");
            }
        }
        System.out.println("}");
    }

    /**
     * Prints a given data handle.
     *
     * @param handle
     */
    protected static void printHandle(DataHandle handle) {
        final Iterator<String[]> itHandle = handle.iterator();
        printIterator(itHandle);
    }
    
    /**
     * Prints java array.
     *
     * @param iterator
     */
    protected static void printIterator(Iterator<String[]> iterator) {
        while (iterator.hasNext()) {
            String[] next = iterator.next();
            System.out.print("[");
            for (int i = 0; i < next.length; i++) {
                String string = next[i];
                System.out.print(string);
                if (i < next.length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
        }
    }
    
    /**
     * Prints the result.
     *
     * @param result
     * @param data
     */
    protected static void printResult(final ARXResult result, final Data data) {

        // Print time
        final DecimalFormat df1 = new DecimalFormat("#####0.00");
        final String sTotal = df1.format(result.getTime() / 1000d) + "s";
        System.out.println(" - Time needed: " + sTotal);

        // Extract
        final ARXNode optimum = result.getGlobalOptimum();
        final List<String> qis = new ArrayList<String>(data.getDefinition().getQuasiIdentifyingAttributes());

        if (optimum == null) {
            System.out.println(" - No solution found!");
            return;
        }

        // Initialize
        final StringBuffer[] identifiers = new StringBuffer[qis.size()];
        final StringBuffer[] generalizations = new StringBuffer[qis.size()];
        int lengthI = 0;
        int lengthG = 0;
        for (int i = 0; i < qis.size(); i++) {
            identifiers[i] = new StringBuffer();
            generalizations[i] = new StringBuffer();
            identifiers[i].append(qis.get(i));
            generalizations[i].append(optimum.getGeneralization(qis.get(i)));
            if (data.getDefinition().isHierarchyAvailable(qis.get(i)))
                generalizations[i].append("/").append(data.getDefinition().getHierarchy(qis.get(i))[0].length - 1);
            lengthI = Math.max(lengthI, identifiers[i].length());
            lengthG = Math.max(lengthG, generalizations[i].length());
        }

        // Padding
        for (int i = 0; i < qis.size(); i++) {
            while (identifiers[i].length() < lengthI) {
                identifiers[i].append(" ");
            }
            while (generalizations[i].length() < lengthG) {
                generalizations[i].insert(0, " ");
            }
        }

        // Print
        System.out.println(" - Information loss: " + result.getGlobalOptimum().getLowestScore() + " / " + result.getGlobalOptimum().getHighestScore());
        System.out.println(" - Optimal generalization");
        for (int i = 0; i < qis.size(); i++) {
            System.out.println("   * " + identifiers[i] + ": " + generalizations[i]);
        }
        System.out.println(" - Statistics");
        System.out.println(result.getOutput(result.getGlobalOptimum(), false).getStatistics().getEquivalenceClassStatistics());
    }
}
