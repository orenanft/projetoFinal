package br.ufes.readIaas;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;
//import java.util.Scanner;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.nmap4j.*;
import org.nmap4j.core.nmap.NMapExecutionException;
import org.nmap4j.core.nmap.NMapInitializationException;
import com.mysql.jdbc.Driver;


@SuppressWarnings("unused")
public class ReadOpenstack {

	private static ArrayList<Node> nodes;
	private static Node server;
	private static ArrayList<Image> snapshots;
	
	public static ArrayList<Image> getSnapshots() {
		return snapshots;
	}

	public static void setSnapshots(ArrayList<Image> snapshots) {
		ReadOpenstack.snapshots = snapshots;
	}

	public static ArrayList<Node> getNodes() {
		return nodes;
	}

	public static void setNodes(ArrayList<Node> nodesFound) {
		nodes = nodesFound;
	}

	public static Node getServer() {
		return server;
	}

	public static void setServer(Node serverFound) {
		server = serverFound;
	}

	public ReadOpenstack() throws Exception{
		//Find Openstack snapshots
		setKnownInstances();
		
		System.out.println("Finding the Server ");
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)){
        	//if (netint.isUp() && !(netint.isLoopback()) && )
        	if (netint.isUp() && !(netint.isLoopback())){
				Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
	       
	    		for (InetAddress inetAddress : Collections.list(inetAddresses)) {
	    			InetAddress ips = inetAddress;
	       			String ipvquatro = ips.toString();
	       			if (ipvquatro != null && ipvquatro.contains(".")){
	       				//System.out.println("IP: " + ipvquatro);
	       				String ip = ipvquatro.substring(1,ipvquatro.length());
						findHost(netint, ip);
	       			}
	       		}        		
			}
        }
        System.out.println("Finding the Nodes ");
        findOpenstackNodes();        
        
	}
	
	public static ResultSet readMysql(String selectSql, String user, String passwd, String database) throws ClassNotFoundException, SQLException{
		System.out.println ("\t");
		//SessionFactory factory; 
		  Connection connect = null;
		  Statement statement = null;
		  ResultSet resultSet = null;
		
		//Connection Properties
			Properties connectionProps = new Properties();
		    connectionProps.put("user", user);
		    connectionProps.put("password", passwd);
		 // Load the MySQL driver
		    Class.forName("com.mysql.jdbc.Driver");
		    
		    // Setup the connection with the DB
		    connect = DriverManager.getConnection(
	                  "jdbc:" + "mysql" + "://" +
	                  "localhost" +
	                  ":" + "3306" + "/" + database,
	                  connectionProps);
		    
		    // Statements allow to issue SQL queries to the database
		    statement = connect.createStatement();
		 // Result set get the result of the SQL query
		    resultSet = statement.executeQuery(selectSql);
		    return resultSet;
	  }
	  
	  public static void findOpenstackNodes() throws Exception{
		// TODO Auto-generated method stub
		System.out.println ("\t");

		int indexI, indexF;
		Node guest = null;
		String[] networkArray, ifaceArray, ipArray, macArray;
		String iface = null, ip = null, mac  = null;
		
		
	    //SQL Query
	    String selectSql = "select host, hostname, uuid, image_ref, network_info from instances i "+
				"join instance_info_caches ic on i.uuid = ic.instance_uuid "+
				"where vm_state='active';";
		
	    ResultSet resultSet = readMysql(selectSql,"root","kaio22","nova");  
	    
	    
	    ArrayList<Node> aux = new ArrayList<Node>();
	    
		while (resultSet.next()) {
			//verifica se instancia já está catalogada
			if(getNodes()!=null){
				for(Node searchNode: getNodes()){
					if(searchNode.getUuid().equals(resultSet.getString(3))){
						//existing instance
						continue;
					}
				}
			}
			if(getSnapshots()!=null){
				for(Image img: getSnapshots() ){
					if(img.getFromInstance().equals(resultSet.getString(3))){
						//found image from snapshot
						System.out.println("Image from snapshot");
					}
				}
			}
	    	//Tratar cache do nova (resultSet.getString(3))
    		//System.out.println(resultSet.getString(1) + resultSet.getString(2) + resultSet.getString(3));
			if (resultSet.getString(5).contains("bridge") && resultSet.getString(5).contains("address") && resultSet.getString(5).contains("network")){
	    		networkArray = resultSet.getString(5).split(",");
		    	/*No quarto(3) vetor encontramos a interface (iface) 
		    	 *No nono(8) vetor encontramos o ip (ip) 
		    	 *No vigesimo nono(29) vetor encontramos o MAC address (mac) 
		    	 */
	    		
	    		//Collect Iface
	    		ifaceArray = networkArray[3].split(":");
	    		indexI = ifaceArray[2].indexOf('"');
	    		indexF = ifaceArray[2].lastIndexOf('"');
	    		iface = ifaceArray[2].substring(indexI + 1,indexF);
	    		//Collect Ip
	    		ipArray = networkArray[8].split(": ");
	    		indexI = ipArray[1].indexOf('"');
	    		indexF = ipArray[1].lastIndexOf('"');
	    		ip = ipArray[1].substring(indexI + 1,indexF);
	    		//Collect MAC
	    		macArray = networkArray[28].split(": ");
	    		indexI = macArray[1].indexOf('"');
	    		indexF = macArray[1].lastIndexOf('"');
	    		mac = macArray[1].substring(indexI + 1,indexF);
	    	}
	    	
	    	//Set Node Properties
	    	guest = new Node();
	    	guest.setHost(resultSet.getString(1));
	    	guest.setHostname(resultSet.getString(2));
	    	guest.setIp(ip);
	    	guest.setIface(iface);
	    	guest.setMac(mac);
	    	guest.setCamada(72);
	    	guest.setType(1);
	    	guest.setNagiosMonitor(0);
	    	guest.setUuid(resultSet.getString(3));
	    	guest.setImage(resultSet.getString(4));
	    	//finding services
	    	//guest.setServices(findServices(guest.ip));
	    	guest.setServices(findServices(ip));	    	    	
		    //
			//add node to array if server 
			if(guest.getHost().equals(getServer().getHostname())){
			   	aux.add(guest);
			}
			else{
				System.out.println ("Instancia não pertence ao servidor" + guest.getHost() + getServer().getHostname());
			}
		}
		setNodes(aux);
	}
	
	private static ArrayList<Service> findServices(String ip) throws NMapInitializationException, NMapExecutionException {
		// TODO Auto-generated method stub
		//exec nmap
//	System.out.println(ip);
	System.out.println ("\t");
	ArrayList<Service> services = new ArrayList<Service>();
    	String serviceName = null;
    	int indexI, indexF;
    	Nmap4j nmap4j = new Nmap4j( "/usr" ) ;
    	
    	nmap4j.addFlags( "-T3 -sV" ) ;
    	//nmap4j.includeHosts(ip);
    	nmap4j.includeHosts( ip );
    	nmap4j.execute() ; 
    	if( !nmap4j.hasError() ) {
    		//NMapRun nmapRun = nmap4j.getResult();
    		//System.out.println(guest.ip +" "+ nmap4j.getOutput() );
    		String getOutput = nmap4j.getOutput();
    		String [] getOutputArray = getOutput.split("/>");
    		for(int i=0;i<getOutputArray.length;i++){
    			if(getOutputArray[i].contains("service name")){
    				String [] findServiceName = getOutputArray[i].split("=");
    				for(int j=0;j<findServiceName.length;j++){
    					if(findServiceName[j].contains("name")){
    						indexI = findServiceName[j+1].indexOf('"');
    			    		indexF = findServiceName[j+1].lastIndexOf('"');
    			    		serviceName = findServiceName[j+1].substring(indexI + 1,indexF);
    			    		Service serv = new Service();
    				    	serv.setName(serviceName);
    				    	serv.setNagiosMonitor(0);
    				    	serv.setUuid(UUID.randomUUID().toString());
    				    	serv.setType(2);
				//	System.out.println(serviceName);
    				    	// VERIFYING SERVICE PROPERTIES (only http and domain)
    				    	if(serv.getName().equalsIgnoreCase("domain")){
    				    		String dadoAvaliado = "Número de zonas";
        				    	String total = null;
        				    	//make a for if you want to get more data   				    	    
    				    		String s;
    				    	    Process p;
    				    	    String [] command = {"/bin/sh", "-c","ssh -i /home/renan/mypair.pem ubuntu@" + ip +" 'find /etc/bind/ -name db.* | wc -l'"};
    				    	    //String [] command = {"/bin/sh", "-c","ssh renan@localhost 'find /etc/bind/ -name db.* | wc -l'"};

    				    	    try {
    				    	        // run the command
    				    	        p = Runtime.getRuntime().exec(command);
    				    	        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    				    	        // get the result
    				    	        while ((s = br.readLine()) != null){
    				    	            //System.out.println("line: " + s);
    				    	        	total=s;
    				    	        }
    				    	        p.waitFor();
    				    	        // get the exit code
    				    	        //System.out.println ("exit: " + p.exitValue());
    				    	        if(p.exitValue() == 1 ){
    				    	        	System.out.println ("Erro ao executar comando " + command);
    				    	        }
    				    	        p.destroy();
    				    	    } catch (Exception e) {}
    				    	    serv.setDadoAvaliado(dadoAvaliado);
    				    		serv.setTotal(total);
						serv.setType(2);
    				    		//add service
    				    		//System.out.println( total);
						services.add(serv);
    				    	}
    				    	else if(serv.getName().equalsIgnoreCase("http")){
    				    		String dadoAvaliado = "Número de sites";
        				    	String total = null;
        				    	//make a for if you want to get more data
    				    		String s;
    				    	    Process p;
    				    	  String [] command ={"/bin/sh", "-c", "ssh -i /home/renan/mypair.pem ubuntu@" + ip +" 'find /etc/apache2/sites-enabled/ -maxdepth 1  -type l -ls | wc -l'"};
    				    	    //String [] command = {"/bin/sh", "-c","ssh renan@localhost 'find /etc/apache2/sites-enabled/ -maxdepth 1  -type l -ls | wc -l'"};
    				    	    try {
    				    	        // run the command
    				    	        p = Runtime.getRuntime().exec(command);
    				    	        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    				    	        // get the result
    				    	        while ((s = br.readLine()) != null){
    				    	            //System.out.println("line: " + s);
    				    	        	total = s;
    				    	        }
    				    	        p.waitFor();
    				    	        // get the exit code
    				    	        //System.out.println ("exit: " + p.exitValue());
    				    	        if(p.exitValue() == 1 ){
    				    	        	System.out.println ("Erro ao executar comando " + command);
    				    	        }
    				    	        p.destroy();
    				    	    } catch (Exception e) {}
    				    	    serv.setDadoAvaliado(dadoAvaliado);
    				    	    serv.setTotal(total);
    				    		//add service
    				    		services.add(serv);
    				    	}
    				    	else{
    				    		serv.setDadoAvaliado(null);
    				    		serv.setTotal(null);
    				    		//add service
						
    				    		services.add(serv);
    				    	}
    				    	break;
    					}
    				}
    			}
    		}
    		} else {
    			System.out.println( nmap4j.getExecutionResults().getErrors() ) ;
    		}
    	//nmap4j.excludeHosts(guest.ip) ;
    	nmap4j.excludeHosts( ip );	
	return services;
	}

	private static void findHost(NetworkInterface netint, String ip) throws SocketException, UnknownHostException {
		// TODO Auto-generated method stub
		System.out.println ("\t");

		Node serverFound = new Node();
		serverFound.setHost(null);
		serverFound.setServices(null);
		InetAddress hostname = InetAddress.getLocalHost();
		//System.out.println("Hostname: " + hostname.getHostName());
		serverFound.setHostname(hostname.getHostName());
		//System.out.printf("Interface: %s\n", netint.getDisplayName());
		serverFound.setIface(netint.getDisplayName());
		//System.out.printf("Name: %s\n", netint.getName());
		serverFound.setIp(ip);
	        
	    byte[] mac = netint.getHardwareAddress();
	
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < mac.length; i++) {
	        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));        
	    }
	    //System.out.println();
	    String macAd = sb.toString(); 
	    serverFound.setMac(macAd.toLowerCase());
	    serverFound.setCamada(4);
	    serverFound.setNagiosMonitor(0);
	    serverFound.setUuid(UUID.randomUUID().toString());
	    serverFound.setType(0);
		setServer(serverFound);
	}

	public static void setKnownInstances() throws ClassNotFoundException, SQLException{
		//SQL Query
	    ArrayList<Image> snapshots= new ArrayList<Image>();
		String selectSql = "select image_id, value from glance.image_properties "+
				"where name='instance_uuid';";
	     
	    ResultSet resultSet = readMysql(selectSql,"root","kaio22","glance");
	    Image snap= new Image();
	    while(resultSet.next()){
	    	//System.out.println(resultSet.getString(1) +"\t" + resultSet.getString(2));
	    	snap.setId(resultSet.getString(1));
	    	snap.setFromInstance(resultSet.getString(2));
	    	snapshots.add(snap);
	    }
	    
	    setSnapshots(snapshots);
	}
	
	public static void searchNewIntances() throws Exception{

		//SQL Query
	    String selectSql = "select uuid from instances "+
				"where vm_state='active';";
		
	    ResultSet resultSet = readMysql(selectSql,"root","kaio22","nova");
	    while(resultSet.next()){
	    	boolean hasInstance=false;
	    	for(Node newNode: getNodes()){
	    		if(newNode.getUuid().equals(resultSet.getString(1)))
	    			hasInstance=true;
	    			break;
	    	}
	    	if(!hasInstance){
	    		findOpenstackNodes();
	    	}
	    }
	    
	}
	
	public static boolean searchForIntance(String uuid) throws Exception{
		boolean queryOk=false;
		//SQL Query
	    String selectSql = "select uuid from instances "+
				"where uuid='"+uuid+"' AND vm_state='active';";
		
	    ResultSet resultSet = readMysql(selectSql,"root","kaio22","nova");
	    if(resultSet.next()){
	    	queryOk=true;
	    }
	    
	    return queryOk;
	}
	
	/*public static void main(String[] args) throws Exception {
			
			setKnownInstances(); //4522c61c-6ef4-4aba-b981-e227f95eeda9
			boolean ok=searchForIntance("1f54f649-5983-40df-89cc-c4c6b9853132");
			if(ok){
				System.out.println("Ok");
			}
			else{
				System.out.println("NOk");
			}
		//System.out.println(UUID.randomUUID().toString());
	}*/
}
