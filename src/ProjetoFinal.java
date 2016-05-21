import java.util.ArrayList;
//import java.util.Scanner;

import br.ufes.nagios.NagiosMonitor;
import br.ufes.neo4j.WriteNeo4j;
import br.ufes.readOpenstack.*;

public class ProjetoFinal {
	
	public static void main(String[] args) throws Exception {
		
		//Scanner scanner = new Scanner(System.in);
		System.out.println("Select Cloud Computing...");
		System.out.println("For now, only OPENSTACK is supported");
		new ReadDatabase();
		WriteNeo4j.neo4jServer("start");
        new WriteNeo4j();
	System.out.println ("NAGIOS MONITOR");        
	createShutDownHook();
		while(true){
			//Catch ctrl+c
			//createShutDownHook();

			try {
				//exec nagios monitor
				for (Node node: ReadDatabase.getNodes())
					new NagiosMonitor(node.getIp());					
				    Thread.sleep(30000);                 //1000 milliseconds is one second.	
			}catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
			}			
		}
	
	}
	
	private static void createShutDownHook()
	{
	    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
	    {
	        @Override
	        public void run()
	        {
	            System.out.println();
	            System.out.println("Thanks for using the application");
	            System.out.println("Exiting...");
	            
	        }
	    }));
	}

}
