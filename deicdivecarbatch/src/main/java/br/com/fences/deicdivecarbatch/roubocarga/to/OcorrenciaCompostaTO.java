package br.com.fences.deicdivecarbatch.roubocarga.to;

import java.io.Serializable;

import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;

public class OcorrenciaCompostaTO implements Serializable{

	private static final long serialVersionUID = -4259345737959269692L;

	private Ocorrencia ocorrencia;
	private ControleOcorrencia controleOcorrencia;
	
	public OcorrenciaCompostaTO(){};
	
	public OcorrenciaCompostaTO(Ocorrencia ocorrencia, ControleOcorrencia controleOcorrencia) {
		super();
		this.ocorrencia = ocorrencia;
		this.controleOcorrencia = controleOcorrencia;
	}
	
	public Ocorrencia getOcorrencia() {
		return ocorrencia;
	}
	public void setOcorrencia(Ocorrencia ocorrencia) {
		this.ocorrencia = ocorrencia;
	}
	public ControleOcorrencia getControleOcorrencia() {
		return controleOcorrencia;
	}
	public void setControleOcorrencia(ControleOcorrencia controleOcorrencia) {
		this.controleOcorrencia = controleOcorrencia;
	}
	
}
