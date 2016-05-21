package br.ufes.readOpenstack;

import java.util.ArrayList;

public class Node{
	private String hostname;
	private String host;
	//public String descricao;
	private String ip;
	private String iface;
	private String mac;
	private ArrayList<Service> services;
	private int camada;
	private int type;
	private int nagiosMonitor;
	private String uuid;
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	//getters and setters
	public String getHostname() {
		return hostname;
	}
	
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	/*public String getDescricao() {
		return descricao;
	}
	
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}*/
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getIface() {
		return iface;
	}
	
	public void setIface(String iface) {
		this.iface = iface;
	}
	
	public String getMac() {
		return mac;
	}
	
	public void setMac(String mac) {
		this.mac = mac;
	}
	
	public ArrayList<Service> getServices() {
		return services;
	}
	
	public void setServices(ArrayList<Service> services) {
		this.services = services;
	}
	
	public int getCamada() {
		return camada;
	}
	
	public void setCamada(int camada) {
		this.camada = camada;
	}

	public int getNagiosMonitor() {
		return nagiosMonitor;
	}

	public void setNagiosMonitor(int nagiosMonitor) {
		this.nagiosMonitor = nagiosMonitor;
	}
}
