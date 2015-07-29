package br.com.fences.deicdivecarbatch.config;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppConfig implements Serializable{

	private static final long serialVersionUID = 9159464655935948935L;

	private String logConsole;
	private String logLevel;
	private String logDiretorio;
	private String serverBackendHost;
	private String serverBackendPort;
	private String ocorrenciaRdoBackendHost;
	private String ocorrenciaRdoBackendPort;
	
	public String getLogConsole() {
		return logConsole;
	}
	public void setLogConsole(String logConsole) {
		this.logConsole = logConsole;
	}
	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}
	public String getLogDiretorio() {
		return logDiretorio;
	}
	public void setLogDiretorio(String logDiretorio) {
		this.logDiretorio = logDiretorio;
	}
	public String getServerBackendHost() {
		return serverBackendHost;
	}
	public void setServerBackendHost(String serverBackendHost) {
		this.serverBackendHost = serverBackendHost;
	}
	public String getServerBackendPort() {
		return serverBackendPort;
	}
	public void setServerBackendPort(String serverBackendPort) {
		this.serverBackendPort = serverBackendPort;
	}
	public String getOcorrenciaRdoBackendHost() {
		return ocorrenciaRdoBackendHost;
	}
	public void setOcorrenciaRdoBackendHost(String ocorrenciaRdoBackendHost) {
		this.ocorrenciaRdoBackendHost = ocorrenciaRdoBackendHost;
	}
	public String getOcorrenciaRdoBackendPort() {
		return ocorrenciaRdoBackendPort;
	}
	public void setOcorrenciaRdoBackendPort(String ocorrenciaRdoBackendPort) {
		this.ocorrenciaRdoBackendPort = ocorrenciaRdoBackendPort;
	}
	
	
}
