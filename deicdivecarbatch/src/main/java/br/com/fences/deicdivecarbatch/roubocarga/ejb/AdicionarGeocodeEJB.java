package br.com.fences.deicdivecarbatch.roubocarga.ejb;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import br.com.fences.deicdivecarbatch.config.AppConfig;
import br.com.fences.fencesutils.rest.tratamentoerro.util.VerificarErro;
import br.com.fences.geocodeentidade.geocode.Endereco;

@Stateless
@Asynchronous
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AdicionarGeocodeEJB {
	
	@Inject
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;

	public void adicionarCasoNaoExista(Endereco endereco)
	{
		String host = appConfig.getServerBackendHost();
		String port = appConfig.getServerBackendPort();
		
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + "/geocodebackend/rest/" + 
				"cacheGeocode/adicionarCasoNaoExista";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(endereco));
		String json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.warn(msg);
			//throw new RuntimeException(msg);
		}
	}
	
}
