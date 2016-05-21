package br.ufes.nagios;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import br.ufes.neo4j.WriteNeo4j;
import br.ufes.readOpenstack.Node;
import br.ufes.readOpenstack.ReadDatabase;
import br.ufes.readOpenstack.Service;

public class NagiosMonitor {

	private final static String nmap="/usr/lib/nagios/plugins/check_host_nmap";
	private final static String http="/usr/lib/nagios/plugins/check_http";
	//private final static String dns="/usr/lib/nagios/plugins/check_dns";
	private final static String ssh="/usr/lib/nagios/plugins/check_ssh";
	
	public NagiosMonitor(String nodeIp){
		//check instances
		if(nagiosIntance(nodeIp))
			nagiosService(nodeIp);//check services
		
	}
	/*public static void main(String[] args){
		new NagiosMonitor();
	}*/

	private void nagiosService(String nodeIp) {
		// TODO Auto-generated method stub
		System.out.println ("\t");
		String serviceName, nagiosOutput=null;
		
    	for(Node node: ReadDatabase.getNodes()){ 
    		if(node.getIp().equals(nodeIp)){
    			ArrayList<Service> servicesList= node.getServices();
    			for(Service service : servicesList){
    				String name= service.getName();
    				if(name.equalsIgnoreCase("http")){
    					serviceName=http;
    				}
    				/*else if(name.equalsIgnoreCase("domain")){
    					serviceName=dns;
    				}*/
    				else if(name.equalsIgnoreCase("ssh")){
    					serviceName=ssh;
    				}
    				else{
    					System.out.println("Command not supported");
    					continue;
    				}
    			//System.out.println(serviceName);
    			    Process p;
    		        String [] command = {serviceName, "-H", nodeIp};
    		        try {
    			        // run the command
    			        p = Runtime.getRuntime().exec(command);
    			        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    			        // get the result
    			        nagiosOutput = br.readLine();
    			        p.waitFor();
				//System.out.println (nagiosOutput);
    			        // get the exit code
    			        //System.out.println ("exit: " + p.exitValue());
    			        if(p.exitValue() == 1 ){
    			        	System.out.println ("Erro ao executar comando " + command);
    			        }
    			        p.destroy();
    			    } catch (Exception e) {}
    		        if (nagiosOutput.contains("OK")){
    		        	//crawl names in service neo4j
    		        	ArrayList <String> result= WriteNeo4j.queryNeo4j("SERVICE", "Name", "IP", nodeIp);
    		        	boolean serviceExists=false;
    		        	for(String nameSNeo4j: result){
    		        		if(nameSNeo4j.equals(name)){
    		        			serviceExists = true;
    		        		}
    		        	}
    		        	if(!serviceExists){
        		        	//if not exists create service
    		        		/*CREATE NODE AND RELATIONSHIPS AND SERVICES
    		        		 * Ex.: CREATE SERVICE
    		        		 * CREATE (a:Service{Name:'PostgreSql',description:'Banco de Dados',status:'Ativo',Type:2})
    		        		 * MATCH (a:Instance),(b:Service) WHERE a.Name='Web' AND a.uuid='45e6803a1c6411e692cde374a1521a23' AND b.Name='PostgreSql'
    		        		 * CREATE (a)-[r:RUN]->(b)
    		        		 */
    		        		if(service.getDadoAvaliado()==null){
	    		        		WriteNeo4j.addNeo4j("CREATE (a:Service{Name:'"+service.getName()+"',Type:2})");
						//get new uuid
						service.setUuid(WriteNeo4j.queryUuidNeo4j("Service"));
	    		        		WriteNeo4j.addNeo4j("MATCH (a:Instance),(b:Service) WHERE a.Name='"+node.getHostname()+"' AND a.uuid='"+node.getUuid()+"' AND b.Name='"+service.getName()+"' CREATE (a)-[r:Provides]->(b)");
    		        		}else{
    		        			WriteNeo4j.addNeo4j("CREATE (a:Service{Name:'"+service.getName()+"', `"+service.getDadoAvaliado()+"`:'"+service.getTotal()+"',Type:2})");
						//get new uuid
						service.setUuid(WriteNeo4j.queryUuidNeo4j("Service"));
	    		        		WriteNeo4j.addNeo4j("MATCH (a:Instance),(b:Service) WHERE a.Name='"+node.getHostname()+"' AND a.uuid='"+node.getUuid()+"' AND b.Name='"+service.getName()+"' CREATE (a)-[r:Provides]->(b)");
    		        		}
    		        	}
    		        }else{
    		        	//remove service
    		        	WriteNeo4j.deleteNeo4j("service","uuid", service.getUuid());
    		        	/*ArrayList <String> result= WriteNeo4j.queryNeo4j("SERVICE", "Name", "IP", nodeIp);
    		        	for(String nameSNeo4j: result){
    		        		if(nameSNeo4j.equals(serviceName)){
    		        			
    				    		System.out.println("I've removed service "+nameSNeo4j+" from neo4j");
    				    		break;
    		        		}
    		        	}*/
    		        }
    			}
    		break;
    		}
    	}
		
	}

	private boolean nagiosIntance(String nodeIp) {
		System.out.println ("\t");
		// TODO Auto-generated method stub
		boolean checkService=false;
		//Exec the nmap plugin(check-nmap) as a linux process
		String nagiosOutput=null;
		//String remove = null;
	    Process p;
        String [] command = {"/bin/sh",nmap, nodeIp};
	    
	    try {
	        // run the command
	        p = Runtime.getRuntime().exec(command);
	        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        // get the result
	        nagiosOutput = br.readLine();
	        p.waitFor();
	        // get the exit code
	        //System.out.println ("exit: " + p.exitValue());
	        if(p.exitValue() == 1 ){
	        	System.out.println ("Erro ao executar comando " + command);
	        }
	        p.destroy();
	    } catch (Exception e) {}
		//Evaluate the output
	    if (nagiosOutput.contains("OK")){
	    	//set nagios monitor para zero
	    	//TO-DO
	    	for(Node node: ReadDatabase.getNodes()){ 
	    		if(node.getIp().equals(nodeIp)){
	    			node.setNagiosMonitor(0);
	    			System.out.println(node.getHostname() + " Nagios monitor = 0");
				checkService=true;
				//crawl ips in neo4j
    		        ArrayList <String> result= WriteNeo4j.queryNeo4j("INSTANCE", "IP");
    		        boolean hasInstance=false;
    		        for(String ipNeo4j: result){
    		        	if(ipNeo4j.equals(nodeIp)){
    		        		hasInstance = true;
    		        	}
    		        }
    		        	if(!hasInstance){
        		        	//if not exists create instance
    		        		WriteNeo4j.addNeo4j("CREATE (a:Instance{Name:'"+node.getHostname()+
	           		"',IP:'"+node.getIp()+
	           		"',MAC:'"+node.getMac()+
	           		"',Interface:'"+node.getIface()+
	           		"',Layer:"+node.getCamada()+
	           		",Type:"+node.getType()+"})");
	       	//get uuid
	       	node.setUuid(WriteNeo4j.queryUuidNeo4j("Instance"));
	        //create relationship
	       	WriteNeo4j.addNeo4j("MATCH (a:Hypervisor),(b:Instance) WHERE a.uuid='"+ReadDatabase.getServer().getUuid()+
	       			"' AND b.uuid='"+node.getUuid()+"' CREATE (a)-[r:Hosts]->(b)");

	        //getting services
	       	for (Service service :  node.getServices()){
	        	if (service.getDadoAvaliado() != null){
	        		WriteNeo4j.addNeo4j("CREATE (a:Service{Name:'"+service.getName()+"',`"+
	        				service.getDadoAvaliado()+"`:'"+
	        				service.getTotal()+"',Type:"+
	        				service.getType()+"})");
	        	}else{
	        		WriteNeo4j.addNeo4j("CREATE (a:Service{Name:'"+service.getName()+
	        				"',Type:"+service.getType()+"})");
	        	}
	        	//get uuid
	        	service.setUuid(WriteNeo4j.queryUuidNeo4j("Service"));
	        	//create relationship
	        	WriteNeo4j.addNeo4j("MATCH (a:Instance),(b:Service) WHERE a.uuid='"+node.getUuid()+
	       			"' AND b.uuid='"+service.getUuid()+"'CREATE (a)-[r:Provides]->(b)");
    		        	}
    		        }
	    			break;
	    		}
	    	}
	    }
	    else if (nagiosOutput.contains("CRITICAL")){
	    	//If the output return a error
			//Remove node and their services to neo4j 
	    	//increase nagios monitor
	    	//TO-DO
	    	for(Node node: ReadDatabase.getNodes()){ 
	    		if(node.getIp().equals(nodeIp)){
				//verify if the node is on neo4j
			    	ArrayList <String> result= WriteNeo4j.queryNeo4j("INSTANCE", "IP");
    		        boolean hasInstance=false;
    		        for(String ipNeo4j: result){
    		        	if(ipNeo4j.equals(nodeIp)){
    		        		hasInstance = true;
    		        	}
    		        }
    		        if(!hasInstance){
						break;
					}else{
			    	if (node.getNagiosMonitor() < 3 ){
			    	  	node.setNagiosMonitor(node.getNagiosMonitor()+1);
			    	  	System.out.println(node.getHostname()  +"nagiosMonitor++");
			    	  }
			    	  else{
			    	  	//delete node
			    		//ArrayList <String> result= WriteNeo4j.queryNeo4j("instance", "uuid", "IP", nodeIp);
			    		//if(result.size()==1){
			    		WriteNeo4j.deleteNeo4j("instance","uuid", node.getUuid());
			    		System.out.println("I've removed the nodes and their services from neo4j");
			    		//}
			    		//else{
			    		//	System.out.println("I can't remove the node because the query returns more than one value");
			    		//}
			    		//remove= node.getHostname();
			    	  }
				}
	    		}
	    	}
	    }
	    else if (nagiosOutput.contains("UNKNOWN")){
	    	System.out.println("There is a problem to run check_host_nmap");
	    }
	    return checkService;
	}
}
