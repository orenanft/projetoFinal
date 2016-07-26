package br.ufes.nagios;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import br.ufes.neo4j.Neo4j;
import br.ufes.readIaas.Node;
import br.ufes.readIaas.DescobreOpenstack;
import br.ufes.readIaas.Service;

public class NagiosMonitor {

	private final static String nmap="/usr/lib/nagios/plugins/check_host_nmap";
	private final static String http="/usr/lib/nagios/plugins/check_http";
	//private final static String dns="/usr/lib/nagios/plugins/check_dns";
	private final static String ssh="/usr/lib/nagios/plugins/check_ssh";
	
	//construtor
	public NagiosMonitor(String nodeIp) throws Exception{
		//check instances
		if(verificaNo(nodeIp))
			verificaServico(nodeIp);//check services
		
	}

	//monitora serviços de rede
	private void verificaServico(String nodeIp) {
		// TODO Auto-generated method stub
		System.out.println ("\t");
		String serviceName, nagiosOutput=null;
		
    	for(Node node: DescobreOpenstack.getNos()){ 
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
    					System.out.println("Service "+ name +" not monitored, please contact the admin");
    					continue;
    				}

    			    Process p;
    		        String [] command = {serviceName, "-H", nodeIp};
    		        try {
    			        // run the command
    			        p = Runtime.getRuntime().exec(command);
    			        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    			        // get the result
    			        nagiosOutput = br.readLine();
    			        p.waitFor();

    			        // get the exit code

    			        if(p.exitValue() == 1 ){
    			        	System.out.println ("Erro ao executar comando " + command);
    			        }
    			        p.destroy();
    			    } catch (Exception e) {}
    		        if (nagiosOutput.contains("OK") || nagiosOutput.contains("UNKNOWN")){
    		        	service.setNagiosMonitor(0);
    		        	/*verificar o dado avaliado por ssh
    		        	 * se dado mudar
    		        	 * setProperty("ServiceStats",service.getName(),"true");
    		        	 */    		        	
    		        	//crawl names in service neo4j
    		        	ArrayList <String> result= Neo4j.consulta("SERVICE", "Name", "IP", nodeIp);
    		        	boolean serviceExists=false;
    		        	for(String nameSNeo4j: result){
    		        		if(nameSNeo4j.equals(name)){
    		        			serviceExists = true;
    		        		}
    		        	}
    		        	if(!serviceExists){
    		        		
	    		        	Neo4j.insere("CREATE (a:Service{Name:'"+service.getName()+
	    		        				"',uuid:'"+service.getUuid()+"})");
	    		        		//get new uuid
	    		        		//service.setUuid(Neo4j.queryUuidNeo4j("Service"));
	    		        	Neo4j.insere(node.getHostname()+"' AND a.uuid='"+node.getUuid()+"' AND b.uuid='"+
	    		        	service.getUuid()+"' CREATE (a)-[r:Provides]->(b)");
    		        	}
    		        }else{
    		        	//remove service
    		        	
    		        	if (service.getNagiosMonitor() < 3 ){
    			    	  	service.setNagiosMonitor(node.getNagiosMonitor()+1);
    			    	  	System.out.println("Service "+service.getName()  +" is not responding");
    			    	  }
    			    	  else{
    			    	  	//delete node
    			    		  Neo4j.deleteNeo4j("service","uuid", service.getUuid());
    			    		System.out.println("Node "+service.getName()+" has been removed");

    			    	  }
    		        }
    		}
          break;
    	}

      }
   }
  
	//monitora instancias
	private boolean verificaNo(String nodeIp) throws Exception {
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
	        
	        if(p.exitValue() == 1 ){
	        	System.out.println ("Erro ao executar comando " + command);
	        }
	        p.destroy();
	    } catch (Exception e) {}
		//Evaluate the output
	    if (nagiosOutput.contains("OK")){
	    	//set nagios monitor para zero
	    	//TO-DO
	    	for(Node node: DescobreOpenstack.getNos()){ 
	    		if(node.getIp().equals(nodeIp)){
	    			node.setNagiosMonitor(0);
	    			System.out.println(node.getHostname() + " Ok");
				checkService=true;
				break;
	    		}
	    	}
	    }
	    else if (nagiosOutput.contains("CRITICAL")){
	    	//If the output return a error
			//Remove node and their services to neo4j 
	    	//increase nagios monitor
	    	//TO-DO
	    	for(Node node: DescobreOpenstack.getNos()){ 
	    		if(node.getIp().equals(nodeIp)){
				  	if (node.getNagiosMonitor() < 3 ){
			    	  	node.setNagiosMonitor(node.getNagiosMonitor()+1);
			    	  	System.out.println("Node "+node.getHostname()  +" is not responding");
			    	  }
			    	  else{
			    	  	//delete node
			    		  if(!DescobreOpenstack.buscaIntancia(node.getUuid())){
			    			  Neo4j.deleteNeo4j("instance","uuid", node.getUuid());
			    			  System.out.println("Node "+node.getHostname()+" has been removed");
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
	
	//função para captura de caracteristicas (Trabalho de Graduação do Kaio Simonassi)
	@SuppressWarnings("unused")
	private void capturaCaracteristica(String ip){
		ArrayList <String> result= Neo4j.consulta("SERVICE", "Name", "IP", ip);	
		for(String serv : result){
			// VERIFYING SERVICE PROPERTIES (only http and domain)
			if(serv.equalsIgnoreCase("domain")){
				String dadoAvaliado = "Número de zonas";
		    	String total = null;
		    	//make a for if you want to get more data   				    	    
				String s;
			    Process p;
			    String [] command = {"/bin/sh", "-c","ssh -i /home/renan/mypair.pem ubuntu@" + ip +" 'find /etc/bind/ -name db.* | wc -l'"};
			  		
			    try {
			        // run the command
			        p = Runtime.getRuntime().exec(command);
			        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			        // get the result
			        while ((s = br.readLine()) != null){

			        	total=s;
			        }
			        p.waitFor();
			        // get the exit code

			        if(p.exitValue() == 1 ){
			        	System.out.println ("Erro ao executar comando " + command);
			        }
			        p.destroy();
			    } catch (Exception e) {}
			}
			else if(serv.equalsIgnoreCase("http")){
				String dadoAvaliado = "Número de sites";
		    	String total = null;
		    	//make a for if you want to get more data
				String s;
			    Process p;
			  String [] command ={"/bin/sh", "-c", "ssh -i /home/renan/mypair.pem ubuntu@" + ip +" 'find /etc/apache2/sites-enabled/ -maxdepth 1  -type l -ls | wc -l'"};
			   
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

			        if(p.exitValue() == 1 ){
			        	System.out.println ("Erro ao executar comando " + command);
			        }
			        p.destroy();
			    } catch (Exception e) {}
			}
		}
	}
}
