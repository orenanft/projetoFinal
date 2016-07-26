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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.nmap4j.*;
import org.nmap4j.core.nmap.NMapExecutionException;
import org.nmap4j.core.nmap.NMapInitializationException;
import com.mysql.jdbc.Driver;


@SuppressWarnings("unused")
public class DescobreOpenstack {

	private static ArrayList<Node> nos;
	private static ArrayList<Node> servidores;
	private static ArrayList<Image> imagens;

	//getters and setters
	public static ArrayList<Node> getNos() {
		return nos;
	}

	public static void setNos(ArrayList<Node> nos) {
		DescobreOpenstack.nos = nos;
	}

	public static ArrayList<Node> getServidores() {
		return servidores;
	}

	public static void setServidores(ArrayList<Node> servidores) {
		DescobreOpenstack.servidores = servidores;
	}

	public static ArrayList<Image> getImagens() {
		return imagens;
	}

	public static void setImagens(ArrayList<Image> imagens) {
		DescobreOpenstack.imagens = imagens;
	}

	//construtor
	public DescobreOpenstack() throws Exception{
		//Find Openstack images
		procuraImagens();
		
		System.out.println("Finding the Server ");
		procuraServidores();
	
        System.out.println("Finding the Nodes ");
        procuraNos();        
        
	}
	
	public static ResultSet lerMysql(String selectSql, String user, String passwd, String database) throws ClassNotFoundException, SQLException{
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
	  
	  public static void procuraNos() throws Exception{
		// TODO Auto-generated method stub
		System.out.println ("\t");

		int indexI, indexF;
		Node guest = null;
		String[] networkArray, ifaceArray, ipArray, macArray, routerArray;
		String iface = null, ip = null, mac  = null, roteador=null;
		
		
		//Consulta SQL para a Coleta das Informações dos Nós Feita pelo Método procuraNos
		String selectSql = "select host, hostname, uuid, image_ref, project_id,"+ 
				   "user_id, network_info from instances i "+
				   "join instance_info_caches ic on i.uuid = ic.instance_uuid "+
				   "where vm_state='active';";
		
	    ResultSet resultSet = lerMysql(selectSql,"root","kaio22","nova");  
	    
	    
	    ArrayList<Node> aux = new ArrayList<Node>();
	    
		while (resultSet.next()) {
			//verifica se instancia já está catalogada
			if(getNos()!=null){
				for(Node searchNode: getNos()){
					if(searchNode.getUuid().equals(resultSet.getString(3))){
						//existing instance
						continue;
					}
				}
			}
			if(getImagens()!=null){
				for(Image img: getImagens() ){
					if(img.getFromInstance().equals(resultSet.getString(3))){
						//found image from snapshot
						System.out.println("Image from snapshot");
					}
				}
			}
	    	//Tratar cache do nova (resultSet.getString(7))

			
			if (resultSet.getString(7).contains("bridge") && resultSet.getString(7).contains("address") && resultSet.getString(5).contains("network")){
	    		networkArray = resultSet.getString(7).split(",");
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
	    		//Collect Router
	    		routerArray = networkArray[1].split(": ");
	    		indexI = routerArray[2].indexOf('"');
	    		indexF = routerArray[2].lastIndexOf('"');
	    		roteador = routerArray[2].substring(indexI + 1,indexF);
	    	}
	    	
	    	//Set Node Properties
	    	guest = new Node();
	    	guest.setHost(resultSet.getString(1));
	    	guest.setHostname(resultSet.getString(2));
	    	guest.setIp(ip);
	    	guest.setIface(iface);
	    	guest.setMac(mac);
	    	guest.setCamada(72);
	    	guest.setNagiosMonitor(0);
	    	guest.setUuid(resultSet.getString(3));
	    	guest.setImage(resultSet.getString(4));
	    	guest.setProjeto(resultSet.getString(5));
	    	guest.setLocatario(resultSet.getString(6));
	    	guest.setRoteador(roteador);
	    	//finding services
	    	guest.setServices(procuraServicos(ip));	    	    	
		    //
	    	for(Node server: getServidores()){
				//add node to array if belongs to server 
				if(guest.getHost().equals(server.getHostname())){
				   	aux.add(guest);
				}
				else{
					System.out.println ("Instancia não pertence a nenhum servidor" + guest.getHost() + server.getHostname());
				}
	    	}
		}
		setNos(aux);
	}
	
	private static ArrayList<Service> procuraServicos(String ip) throws NMapInitializationException, NMapExecutionException {
		// TODO Auto-generated method stub
		//exec nmap to find services
	System.out.println ("\t");
	ArrayList<Service> services = new ArrayList<Service>();
    	String serviceName = null;
    	int indexI, indexF;
    	Nmap4j nmap4j = new Nmap4j( "/usr" ) ;
    	
    	nmap4j.addFlags( "-T3 -sV" ) ;
    	nmap4j.includeHosts( ip );
    	nmap4j.execute() ; 
    	if( !nmap4j.hasError() ) {
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
    				    	serv.setDadoAvaliado(null);    				    	 
    				    	//add service
    				    	services.add(serv);
    				    	}
    				    	break;
    					}
    				}
    			}
    		}
    		else {
    			System.out.println( nmap4j.getExecutionResults().getErrors() ) ;
    		}
    	nmap4j.excludeHosts( ip );	
	return services;
	}

	@SuppressWarnings("null")
	private static void procuraServidores() throws SocketException, UnknownHostException, ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub
		System.out.println ("\t");
		ArrayList<Node> servers=null;
		
		//Consulta SQL para a Coleta das Informações dos Nós de Computação
		String selectSql = "select host, host_ip from compute_nodes;";
		
		ResultSet resultSet = lerMysql(selectSql,"root","kaio22","nova"); 
		
		while (resultSet.next()) {
			Node serverFound = new Node();
			
		    serverFound.setHostname(resultSet.getString(1));
		    serverFound.setIp(resultSet.getString(2));
		    serverFound.setCamada(4);
		    serverFound.setNagiosMonitor(0);
		    serverFound.setUuid(UUID.randomUUID().toString());
		    servers.add(serverFound);
		}
		setServidores(servers);
	}

	public static void procuraImagens() throws ClassNotFoundException, SQLException{
		//Consulta SQL para a Coleta das Informações das Imagens pelo Método procuraImagens
	    ArrayList<Image> snapshots= new ArrayList<Image>();
		String selectSql = "select image_id, value from glance.image_properties "+
				"where name='instance_uuid';";
	     
	    ResultSet resultSet = lerMysql(selectSql,"root","kaio22","glance");
	    Image snap= new Image();
	    while(resultSet.next()){
	    	snap.setId(resultSet.getString(1));
	    	snap.setFromInstance(resultSet.getString(2));
	    	snapshots.add(snap);
	    }
	    
	    setImagens(snapshots);
	}
	
	public static void procuraNovasIntancias() throws Exception{

		//Consulta SQL para a Coleta do UUID das Instancias ativas pelo Método procuraNovasInstancias
	    String selectSql = "select uuid from instances "+
				"where vm_state='active';";
		
	    ResultSet resultSet = lerMysql(selectSql,"root","kaio22","nova");
	    while(resultSet.next()){
	    	boolean hasInstance=false;
	    	for(Node newNode: getNos()){
	    		if(newNode.getUuid().equals(resultSet.getString(1)))
	    			hasInstance=true;
	    			break;
	    	}
	    	if(!hasInstance){
	    		procuraNos();
	    	}
	    }
	    
	}
	
	public static boolean buscaIntancia(String uuid) throws Exception{
		boolean queryOk=false;
		//Consulta SQL para a Coleta do UUID das Instancias pelo Método buscaInstancia
	    String selectSql = "select uuid from instances "+
				"where uuid='"+uuid+"' AND vm_state='active';";
		
	    ResultSet resultSet = lerMysql(selectSql,"root","kaio22","nova");
	    if(resultSet.next()){
	    	queryOk=true;
	    }
	    
	    return queryOk;
	}
	
}
