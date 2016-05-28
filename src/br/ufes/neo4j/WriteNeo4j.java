package br.ufes.neo4j;

import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.neo4j.jdbc.Driver;
import org.neo4j.jdbc.Neo4jConnection;

import br.ufes.readIaas.ReadOpenstack;
import br.ufes.readIaas.Service;



public class WriteNeo4j {

	// START SNIPPET: createReltype
    /*private enum RelTypes implements RelationshipType {
        Hospeda, Possui;
    }*/

    public WriteNeo4j() {
		// TODO Auto-generated constructor stub
    	System.out.println ("Writing in NEO4J...");
	createDb();
	}

	private void createDb() {
        System.out.println ("\t");
	//DELETING DATA
	addNeo4j("MATCH (n)-[r]-(ns) delete n,r,ns"); 
    addNeo4j("MATCH (n) delete n");

	br.ufes.readIaas.Node server = ReadOpenstack.getServer();
        ArrayList<br.ufes.readIaas.Node> virtuals = ReadOpenstack.getNodes(); 

        //create physical server
        addNeo4j("CREATE (a:Hypervisor{Name:'"+server.getHostname()+
        		"',IP:'"+server.getIp()+
        		"',MAC:'"+server.getMac()+
        		"',Interface:'"+server.getIface()+
        		"',Layer:"+server.getCamada()+
        		",uuid:'"+server.getUuid()+
        		"',Type:"+server.getType()+"})");
        	//get uuid
        	//server.setUuid(queryUuidNeo4j("Hypervisor"));
        	
        //create virtual servers 	
        for (br.ufes.readIaas.Node virtual : virtuals ){
	       	addNeo4j("CREATE (a:Instance{Name:'"+virtual.getHostname()+
	           		"',IP:'"+virtual.getIp()+
	           		"',MAC:'"+virtual.getMac()+
	           		"',Interface:'"+virtual.getIface()+
	           		"',Layer:"+virtual.getCamada()+
	           		",uuid:'"+virtual.getUuid()+
	           		"',Type:"+virtual.getType()+"})");
	       	//get uuid
	       	//virtual.setUuid(queryUuidNeo4j("Instance"));
	        //create relationship
	       	addNeo4j("MATCH (a:Hypervisor),(b:Instance) WHERE a.uuid='"+server.getUuid()+
	       			"' AND b.uuid='"+virtual.getUuid()+"' CREATE (a)-[r:Hosts]->(b)");

	        //getting services
	       	for (Service service :  virtual.getServices()){
	        	if (service.getDadoAvaliado() != null){
	        		addNeo4j("CREATE (a:Service{Name:'"+service.getName()+
        					"',uuid:'"+service.getUuid()+
        					"', `"+service.getDadoAvaliado()+"`:'"+service.getTotal()+
        					"',Type:"+service.getType()+"})");
	        	}else{
	        		addNeo4j("CREATE (a:Service{Name:'"+service.getName()+
	        				"',uuid:'"+service.getUuid()+
	        				"',Type:"+service.getType()+"})");
	        	}
	        	//get uuid
	        	//service.setUuid(queryUuidNeo4j("Service"));
	        	//create relationship
	        	addNeo4j("MATCH (a:Instance),(b:Service) WHERE a.uuid='"+virtual.getUuid()+
	       			"' AND b.uuid='"+service.getUuid()+"'CREATE (a)-[r:Provides]->(b)");
		   	}
	    }
    }
    
	public static void neo4jServer(String status){
    	System.out.println ("\t");
	String [] command = {"/neo4j/bin/neo4j",status};
	    Process p;
	    try {
	        // run the command
	        p = Runtime.getRuntime().exec(command);
	        // get the result
	        p.waitFor();
	        // get the exit code
	        //System.out.println ("exit: " + p.exitValue());
	        if(p.exitValue() == 1 ){
	        	System.out.println ("Erro ao executar comando " + command);
	        }
	        p.destroy();
	    } catch (Exception e) {}
    }
	
	public static String queryUuidNeo4j(String mode) {
		// TODO Auto-generated method stub
	System.out.println ("\t");
    	ArrayList <String> result= new ArrayList<String>();
    	Properties props = new Properties();
    	props.setProperty("user","neo4j");
    	props.setProperty("password","kaio22");
    	
    	// Connect		
    	Neo4jConnection con=null;
		try {
			con = new Driver().connect("jdbc:neo4j://localhost:7474/", props);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	// Querying
     	ResultSet rs=null;
		try {
			rs = con.createStatement().executeQuery("MATCH (a:"+mode+") WHERE not ((a)-[]-()) RETURN a.uuid");
		   	while(rs.next()){
		   		//System.out.println(rs.getString("n."+property));
		    	result.add(rs.getString("a.uuid"));
		        }
			}catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(result.size()>1){
				System.out.println("ERROR: There are two similar nodes!!!");
				for(String res: result)
					System.out.println(res);
			}else if(result.isEmpty())
				System.out.println("ERROR: Query is empty");
    	
		return result.get(0);
	}
        
    public static ArrayList <String> queryNeo4j(String mode, String property){
    	// Make sure Neo4j Driver is registered
    	//Class.forName("org.neo4j.jdbc.Driver");
	System.out.println ("\t");
    	ArrayList <String> result= new ArrayList<String>();
    	Properties props = new Properties();
    	props.setProperty("user","neo4j");
    	props.setProperty("password","kaio22");
    	
    	// Connect		
    	Neo4jConnection con=null;
		try {
			con = new Driver().connect("jdbc:neo4j://localhost:7474/", props);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	// Querying
     	ResultSet rs=null;
		try {
			if(mode.equalsIgnoreCase("instance")){
				rs = con.createStatement().executeQuery("MATCH (n) WHERE n.Type=1 RETURN n."+property);
			}
			else if(mode.equalsIgnoreCase("service")){
				rs = con.createStatement().executeQuery("MATCH (n) WHERE n.Type=2 RETURN n."+property);
			}
			else{
				System.out.println("QueryNeo4j: Unavailable Mode");
			}
		   	while(rs.next()){
		   		//System.out.println(rs.getString("n."+property));
		    	result.add(rs.getString("n."+property));
		        }
			}catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		if(result.isEmpty())
			System.out.println("CAUTION! Query is empty");
			
		return result;	
    }

public static ArrayList <String> queryNeo4j(String mode, String property, String propertyParam, String valueParam){
	System.out.println ("\t");
    	ArrayList <String> result= new ArrayList<String>();
    	Properties props = new Properties();
    	props.setProperty("user","neo4j");
    	props.setProperty("password","kaio22");
    	
    	// Connect		
    	Neo4jConnection con=null;
		try {
			con = new Driver().connect("jdbc:neo4j://localhost:7474/", props);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	// Querying
     	ResultSet rs=null;
		try {
			if(mode.equalsIgnoreCase("instance")){
				rs = con.createStatement().executeQuery("MATCH (n) WHERE n.Type=1 AND n."+propertyParam+"='"+valueParam+"' RETURN n."+property);
			while(rs.next()){
                                //System.out.println(rs.getString("n."+property));
                        result.add(rs.getString("n."+property));
                        }
			}
			else if(mode.equalsIgnoreCase("service")){
				rs = con.createStatement().executeQuery("MATCH (n)-[]-(ns) WHERE ns.Type=2 AND n."+propertyParam+"='"+valueParam+"' RETURN ns."+property);
			while(rs.next()){
                                //System.out.println(rs.getString("n."+property));
                        result.add(rs.getString("ns."+property));
                        }
			}else if((valueParam != null && propertyParam == null)||(valueParam == null && propertyParam != null)){
				System.out.println("Param invalid");
			}
			else{
				System.out.println("QueryNeo4j: Unavailable Mode");
			}
		}catch (SQLException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }
		
			if(result.isEmpty())
				System.out.println("CAUTION! Query is empty");
			
		return result;	
    }   
 
    @SuppressWarnings("unused")
    public static void addNeo4j(String query){  
		/*CREATE NODE AND RELATIONSHIPS AND SERVICES
		 * Ex.: CREATE SERVICE
		 * CREATE (a:Service{Name:'PostgreSql',description:'Banco de Dados',status:'Ativo',Type:2})
		 * MATCH (a:Instance),(b:Service) WHERE a.Name='Web' AND a.uuid='45e6803a1c6411e692cde374a1521a23' AND b.Name='PostgreSql'
		 * CREATE (a)-[r:RUN]->(b)
		 */
	System.out.println ("\t");
    	Properties props = new Properties();
    	props.setProperty("user","neo4j");
    	props.setProperty("password","kaio22");
    	
    	// Connect		
    	Neo4jConnection con=null;
		try {
			con = new Driver().connect("jdbc:neo4j://localhost:7474/", props);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	// Querying
     	ResultSet rs;
    	try {
    		rs = con.createStatement().executeQuery(query);
    	} catch (SQLException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    }
  
    @SuppressWarnings("unused")
    public static void deleteNeo4j(String mode, String property, String value){
		/* dELETE NODE, RELATIONSHIPS AND SERVICES 
		* (MATCH (n)-[r]-(rn) WHERE n.IP="?" AND rn.Type=2 DELETE n,r,rn)
		*  DELETE SERVICE
		*  MATCH (n) WHERE n.Type=2 AND Labels(n)= DELETE n
		*  
		*/
		System.out.println ("\t");
		Properties props = new Properties();
		props.setProperty("user","neo4j");
		props.setProperty("password","kaio22");
		
    	// Connect		
    	Neo4jConnection con=null;
		try {
			con = new Driver().connect("jdbc:neo4j://localhost:7474/", props);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	// Querying
     	ResultSet rs;
		try {
			if(mode.equalsIgnoreCase("instance")){
				rs = con.createStatement().executeQuery("MATCH ()-[r1]-(n) MATCH (n)-[r2]-(ns) WHERE n."+property+"='"+value+"' AND ns.Type=2 DELETE n,r1,r2,ns");
			}	
			else if(mode.equalsIgnoreCase("service")){
				rs = con.createStatement().executeQuery("MATCH ()-[r]-(n) WHERE n."+property+"='"+value+"' AND n.Type=2 DELETE n,r");
			}
			else{
				System.out.println("DelNeo4j: Unavailable Mode");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }	
  
    @SuppressWarnings("unused")
	public static void setNeo4j(String uuid, String property, String oldValue, String newValue){
		/* dELETE NODE, RELATIONSHIPS AND SERVICES 
		* (MATCH (n)-[r]-(rn) WHERE n.IP="?" AND rn.Type=2 DELETE n,r,rn)
		*  DELETE SERVICE
		*  MATCH (n) WHERE n.Type=2 AND Labels(n)= DELETE n  
		*/
		System.out.println ("\t");
		Properties props = new Properties();
		props.setProperty("user","neo4j");
		props.setProperty("password","kaio22");
		
    	// Connect		
    	Neo4jConnection con=null;
		try {
			con = new Driver().connect("jdbc:neo4j://localhost:7474/", props);
			
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	// Querying
     	ResultSet rs;
		try {
			rs = con.createStatement().executeQuery("MATCH (n) WHERE n."+property+"='"+oldValue+"' AND n.uuid='"+uuid+"' SET n."+property+"='"+newValue+"'");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

