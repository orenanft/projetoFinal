package br.ufes.readOpenstack;

//import java.util.ArrayList;

public class Service {
	
	private String name;
	private String dadoAvaliado;
	private String total;
	private int type;
	private String uuid;
	

	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDadoAvaliado() {
		return dadoAvaliado;
	}
	public void setDadoAvaliado(String dadoAvaliado) {
		this.dadoAvaliado = dadoAvaliado;
	}
	public String getTotal() {
		return total;
	}
	public void setTotal(String total) {
		this.total = total;
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
