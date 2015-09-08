package br.com.fences.deicdivecarbatch.indiciado.to;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import br.com.fences.deicdivecarentidade.indiciado.Indiciado;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;

public class IndiciadoCompostoTO implements Serializable{

	private static final long serialVersionUID = -7736911263042799416L;
	
	private Set<Indiciado> indiciados = new LinkedHashSet<>();
	private ControleOcorrencia controleOcorrencia;
	
	public IndiciadoCompostoTO(){}

	public IndiciadoCompostoTO(Set<Indiciado> indiciados, ControleOcorrencia controleOcorrencia) {
		super();
		this.indiciados = indiciados;
		this.controleOcorrencia = controleOcorrencia;
	}

	public Set<Indiciado> getIndiciados() {
		return indiciados;
	}

	public void setIndiciados(Set<Indiciado> indiciados) {
		this.indiciados = indiciados;
	}

	public ControleOcorrencia getControleOcorrencia() {
		return controleOcorrencia;
	}

	public void setControleOcorrencia(ControleOcorrencia controleOcorrencia) {
		this.controleOcorrencia = controleOcorrencia;
	}
	
	
}
