package br.com.fences.deicdivecarbatch.roubocarga;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.batch.api.chunk.AbstractItemReader;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import br.com.fences.deicdivecarbatch.config.AppConfig;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.rest.tratamentoerro.util.VerificarErro;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;


@Named
public class ExtratorItemReader extends AbstractItemReader {

	@Inject 
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;
	
	@Inject
	private Converter<ControleOcorrencia> converterControleOcorrencia;
	
	private Set<ControleOcorrencia> controleOcorrencias = new LinkedHashSet<>();
	private Iterator<ControleOcorrencia> iteratorControleOcorrencias;
	
	private String host;
	private String port;
	
	@Override
	public void open(Serializable checkpoint) throws Exception {
		
		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();

		
		logger.info("Recuperar registros para PROCESSAR e REPROCESSAR do controle...");
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
				"controleOcorrencia/pesquisarProcessarReprocessar";
		WebTarget webTarget = client
				.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.get();
		String json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.error(msg); 
			throw new RuntimeException(msg);
		}
		Type collectionType = new TypeToken<Set<ControleOcorrencia>>(){}.getType();
		if (Verificador.isValorado(json) && !json.equalsIgnoreCase("null"))
		{
			controleOcorrencias = (Set<ControleOcorrencia>) converterControleOcorrencia.paraObjeto(json, collectionType);
		}
		iteratorControleOcorrencias = controleOcorrencias.iterator();
		logger.info("Foram lidos [" + controleOcorrencias.size() + "] registros de controle para carga.");
		
	}
	
	/**
	 * O container ira parar de chamar esse metodo quando retornar nulo.
	 */
	@Override
	public ControleOcorrencia readItem() throws Exception 
	{
		ControleOcorrencia controleOcorrencia = null;
		if (iteratorControleOcorrencias.hasNext())
		{
			controleOcorrencia = iteratorControleOcorrencias.next();
		}
		if (controleOcorrencia == null)
		{
			logger.info("Nao existe mais registro para leitura. Termino do Job.");
		}
		
		return controleOcorrencia;
	}

}
