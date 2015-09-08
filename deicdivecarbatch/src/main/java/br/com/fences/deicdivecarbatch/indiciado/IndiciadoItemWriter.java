package br.com.fences.deicdivecarbatch.indiciado;

import java.util.List;
import java.util.Set;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import br.com.fences.deicdivecarbatch.config.AppConfig;
import br.com.fences.deicdivecarbatch.indiciado.to.IndiciadoCompostoTO;
import br.com.fences.deicdivecarentidade.indiciado.Indiciado;
import br.com.fences.fencesutils.constante.EstadoProcessamento;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.rest.tratamentoerro.util.VerificarErro;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;

@Named
public class IndiciadoItemWriter extends AbstractItemWriter{

	@Inject
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;
	
	@Inject
	private Converter<Indiciado> indiciadoConverter;
	
	@Inject
	private Converter<ControleOcorrencia> controleOcorrenciaConverter;
	
	private String host;
	private String port;
	
	//private Gson gson = new GsonBuilder().create();
	
	@Override
	public void writeItems(List<Object> items) throws Exception {
		
		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();
		
		for (Object item : items)
		{
			IndiciadoCompostoTO indiciadoCompostoTO = (IndiciadoCompostoTO) item;
			Set<Indiciado> indiciados = indiciadoCompostoTO.getIndiciados();
			ControleOcorrencia controleOcorrencia = indiciadoCompostoTO.getControleOcorrencia();

			boolean possuiErro = false;
			for (Indiciado indiciado : indiciados)
			{
				String json = indiciadoConverter.paraJson(indiciado);
				Client client = ClientBuilder.newClient();
				
				if (Verificador.isValorado(indiciado.getId()))
				{
					String servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
							"indiciado/substituir";
					WebTarget webTarget = client.target(servico);
					Response response = webTarget
							.request(MediaType.APPLICATION_JSON)
							.post(Entity.json(json));
					json = response.readEntity(String.class);
					if (verificarErro.contemErro(response, json))
					{
						possuiErro = true;
					}
				}
				else
				{
					String servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
							"indiciado/adicionar";
					WebTarget webTarget = client.target(servico);
					Response response = webTarget
							.request(MediaType.APPLICATION_JSON)
							.put(Entity.json(json));
					json = response.readEntity(String.class);
					indiciado = indiciadoConverter.paraObjeto(json, Indiciado.class);
					if (verificarErro.contemErro(response, json) || !Verificador.isValorado(indiciado.getId()))
					{
						possuiErro = true;
					}
					
				}
			}
			if (possuiErro)
			{
				registrarControle(controleOcorrencia, false);
			}
			else
			{
				registrarControle(controleOcorrencia, true);
			}
		}
		
	}
	
	private void registrarControle(ControleOcorrencia controleOcorrencia, boolean sucesso)
	{
		if (sucesso)
		{
			controleOcorrencia.setEstadoProcessamentoIndiciados(EstadoProcessamento.OK);
		}
		else
		{
			controleOcorrencia.setEstadoProcessamentoIndiciados(EstadoProcessamento.ERRO);
		}
		
		String json = controleOcorrenciaConverter.paraJson(controleOcorrencia);
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
				"controleOcorrencia/substituir";
		WebTarget webTarget = client.target(servico);
		
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(json));
		json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.error(msg);
			throw new RuntimeException(msg);
		}
	}

}
