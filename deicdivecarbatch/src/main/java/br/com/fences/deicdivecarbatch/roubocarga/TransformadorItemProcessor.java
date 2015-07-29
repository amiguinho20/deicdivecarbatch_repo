package br.com.fences.deicdivecarbatch.roubocarga;

import javax.batch.api.chunk.ItemProcessor;
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
import br.com.fences.deicdivecarbatch.roubocarga.tratamentoerro.VerificarErro;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.geocodeentidade.geocode.Endereco;
import br.com.fences.ocorrenciaentidade.chave.OcorrenciaChave;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.auxiliar.Auxiliar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Named
public class TransformadorItemProcessor implements ItemProcessor{
	
	@Inject
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;

	
	Gson gson = new GsonBuilder().create();

	private String host;
	private String port;
	
	@Override
	public Ocorrencia processItem(Object item) throws Exception 
	{
		OcorrenciaChave ocorrenciaChave = (OcorrenciaChave) item;
		
		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();
		
		logger.info("Extraindo... " + ocorrenciaChave);
		Client client = ClientBuilder.newClient();
		String servico = "http://"
				+ appConfig.getOcorrenciaRdoBackendHost()
				+ ":"
				+ appConfig.getOcorrenciaRdoBackendPort()
				+ "/ocorrenciardobackend/rest/"
				+ "rdoextrair/consultarOcorrencia/{idDelegacia}/{anoBo}/{numBo}";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.resolveTemplate("idDelegacia", ocorrenciaChave.getIdDelegacia())
				.resolveTemplate("anoBo", ocorrenciaChave.getAnoBo())
				.resolveTemplate("numBo", ocorrenciaChave.getNumBo())
				.request(MediaType.APPLICATION_JSON)
				.get();
		String json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.error(msg);
			throw new RuntimeException(msg);
		}

		Ocorrencia ocorrencia = gson.fromJson(json, Ocorrencia.class);
		
		if (!Verificador.isValorado(ocorrencia.getLatitude()))
		{
			logger.info("Ocorrencia sem geocode original, consultando o cacheGeocode...");
			
			Endereco endereco = copiarEndereco(ocorrencia);
			client = ClientBuilder.newClient();
			servico = "http://" + host + ":"+ port + "/geocodebackend/rest/" + 
					"cacheGeocode/consultar";
			webTarget = client.target(servico);
			response = webTarget
					.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(endereco));
			json = response.readEntity(String.class);
			if (verificarErro.contemErro(response, json))
			{
				String msg = verificarErro.criarMensagem(response, json, servico);
				logger.error(msg);
				throw new RuntimeException(msg);
			}

			endereco = gson.fromJson(json, Endereco.class);
			atribuirRetorno(endereco, ocorrencia);
			logger.info("Geocode retornado com status [" + ocorrencia.getAuxiliar().getGoogleGeocoderStatus() + "].");
		}
		else
		{
			logger.info("ocorrencia com geocode original");
		}
		return ocorrencia;
	}
	
	
	private Endereco copiarEndereco(Ocorrencia ocorrencia)
	{
		Endereco endereco = new Endereco();
		endereco.setLogradouro(ocorrencia.getLogradouro());
		endereco.setNumero(ocorrencia.getNumeroLogradouro());
		endereco.setComplemento(ocorrencia.getComplemento());
		endereco.setBairro(ocorrencia.getBairro());
		endereco.setCidade(ocorrencia.getCidade());
		endereco.setUf(ocorrencia.getIdUf());
		endereco.setCep(ocorrencia.getCep());
		
		return endereco;
	}
	
	private void atribuirRetorno(Endereco endereco, Ocorrencia ocorrencia)
	{
		Auxiliar auxiliar = ocorrencia.getAuxiliar();
		auxiliar.setGoogleGeocoderStatus(endereco.getGeocodeStatus());
		if (auxiliar.getGoogleGeocoderStatus().equalsIgnoreCase("OK"))
		{
			auxiliar.setGoogleLatitude(endereco.getLatitude());
			auxiliar.setGoogleLongitude(endereco.getLongitude());
		}
	}

}
