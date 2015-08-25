package br.com.fences.deicdivecarbatch.roubocarga;

import java.util.List;

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
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.rest.tratamentoerro.util.VerificarErro;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;

@Named
public class GravadorItemWriter extends AbstractItemWriter{

	@Inject
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;
	
	@Inject
	private Converter<Ocorrencia> ocorrenciaConverter;
	
	private String host;
	private String port;
	
	//private Gson gson = new GsonBuilder().create();
	
	@Override
	public void writeItems(List<Object> items) throws Exception {
		
		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();
		
		for (Object item : items)
		{
			Ocorrencia ocorrencia = (Ocorrencia) item;
			
			logger.info("Gravando... " + msgOcorrencia(ocorrencia));
			String json = ocorrenciaConverter.paraJson(ocorrencia);
			Client client = ClientBuilder.newClient();
			String servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
					"rouboCarga/adicionar";
			WebTarget webTarget = client
					.target(servico);
			
			//Entity.json(ocorrencia)
			Response response = webTarget
					.request(MediaType.APPLICATION_JSON)
					.put(Entity.json(json));
			json = response.readEntity(String.class);
			if (verificarErro.contemErro(response, json))
			{
				String msg = verificarErro.criarMensagem(response, json, servico);
				logger.error(msg);
				throw new RuntimeException(msg);
			}
			logger.info("Ocorrencia registrada com sucesso.");
		}
		
	}
	
	private String msgOcorrencia(Ocorrencia ocorrencia)
	{
		String msg = ocorrencia.getNumBo() + "/" + ocorrencia.getAnoBo() + "/"
				+ ocorrencia.getIdDelegacia() + "/"
				+ ocorrencia.getNomeDelegacia() + "/"
				+ ocorrencia.getDatahoraRegistroBo();
		return msg;
	}

}
