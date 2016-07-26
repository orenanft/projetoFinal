package br.ufes.readIaas;

import java.util.Map;

public class Service {
	
	private String nome;
	private Map<String, Integer> dadoAvaliado;
	private int nagiosMonitor;
	private String uuid;
	
	//getters and setters
	public int getNagiosMonitor() {
		return nagiosMonitor;
	}
	public void setNagiosMonitor(int nagiosMonitor) {
		this.nagiosMonitor = nagiosMonitor;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return nome;
	}
	public void setName(String name) {
		this.nome = name;
	}
	public Map<String, Integer> getDadoAvaliado() {
		return dadoAvaliado;
	}
	public void setDadoAvaliado(Map<String, Integer> dadoAvaliado) {
		this.dadoAvaliado = dadoAvaliado;
	}


}
