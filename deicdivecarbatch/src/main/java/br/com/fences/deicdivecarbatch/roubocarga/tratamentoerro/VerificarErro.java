package br.com.fences.deicdivecarbatch.roubocarga.tratamentoerro;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;

import br.com.fences.fencesutils.conversor.JsonParaErro;
import br.com.fences.fencesutils.rest.tratamentoerro.entidade.Erro;
import br.com.fences.fencesutils.verificador.Verificador;

@ApplicationScoped
public class VerificarErro {

	private static final int HTTP_OK = 200;
	private static final int HTTP_CREATED = 201;
	private static final int HTTP_ACCEPTED = 202;
	private static final int HTTP_NAI = 203;
	private static final int HTTP_NO_CONTENT = 204;
	private static final int HTTP_RESET_CONTENT = 205;
	private static final int HTTP_PARTIAL_CONTENT = 206;
	private static final int HTTP_MULTI_STATUS = 207;
	private static final int HTTP_ALREADY_REPORTED = 208;
	private static final int HTTP_IM_USED = 226;
	
	public boolean contemErro(Response response, String json)
	{
		boolean contem = false;
		if (response.getStatus() < HTTP_OK && response.getStatus() > HTTP_IM_USED)
		{
			contem = true;
		}
		else
		{
			if (Verificador.isPossuiErroRest(json))
			{
				contem = true;
			}
		}
		return contem;
	}
	
	public String criarMensagem(Response response, String json, String servico)
	{
		Erro erro = new Erro();
		if (response.getStatus() != HTTP_OK)
		{
			erro.setCodigoErro(response.getStatus());
			erro.setMensagemErro(response.getStatusInfo().toString());
		}
		else
		{
			erro = JsonParaErro.converter(json);
		}
		String msg = "Erro na chamada do servico[" + servico + "] codigo["
				+ erro.getCodigoErro() + "] mensagem["
				+ erro.getMensagemErro() + "].";
		return msg;
	}
	
}
