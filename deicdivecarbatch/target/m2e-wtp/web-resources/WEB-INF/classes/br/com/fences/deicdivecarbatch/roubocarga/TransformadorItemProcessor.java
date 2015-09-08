package br.com.fences.deicdivecarbatch.roubocarga;

import javax.batch.api.chunk.ItemProcessor;
import javax.ejb.EJB;
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
import br.com.fences.deicdivecarbatch.roubocarga.ejb.AdicionarGeocodeEJB;
import br.com.fences.deicdivecarbatch.roubocarga.to.OcorrenciaCompostaTO;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.rest.tratamentoerro.util.VerificarErro;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.geocodeentidade.geocode.Endereco;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.auxiliar.Auxiliar;
import br.com.fences.ocorrenciaentidade.ocorrencia.auxiliar.Point;

@Named
public class TransformadorItemProcessor implements ItemProcessor{
	
	@Inject
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;

	@EJB
	private AdicionarGeocodeEJB adicionarGeocodeEJB; //-- assincrono
	
	@Inject
	private Converter<Ocorrencia> ocorrenciaConverter;
	
	@Inject
	private Converter<Endereco> enderecoConverter;
	
	private String host;
	private String port;
	
	@Override
	public OcorrenciaCompostaTO processItem(Object item) throws Exception 
	{
		ControleOcorrencia controleOcorrencia = (ControleOcorrencia) item;
		
		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();
		
		logger.info("Extraindo... " + controleOcorrencia);
		Client client = ClientBuilder.newClient();
		String servico = "http://"
				+ appConfig.getOcorrenciaRdoBackendHost()
				+ ":"
				+ appConfig.getOcorrenciaRdoBackendPort()
				+ "/ocorrenciardobackend/rest/"
				+ "rdoextrair/consultarOcorrencia/{idDelegacia}/{anoBo}/{numBo}";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.resolveTemplate("idDelegacia", controleOcorrencia.getIdDelegacia())
				.resolveTemplate("anoBo", controleOcorrencia.getAnoBo())
				.resolveTemplate("numBo", controleOcorrencia.getNumBo())
				.request(MediaType.APPLICATION_JSON)
				.get();
		String json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.error(msg);
			throw new RuntimeException(msg);
		}

		Ocorrencia ocorrencia = ocorrenciaConverter.paraObjeto(json, Ocorrencia.class);
		
		if (Verificador.isValorado(ocorrencia.getLatitude()) && Verificador.isValorado(ocorrencia.getLongitude()))
		{
			double longitude = Double.parseDouble(ocorrencia.getLongitude());
			double latitude = Double.parseDouble(ocorrencia.getLatitude());
			ocorrencia.getAuxiliar().setGeocoderStatus("OK");
			atribuirGeometry(ocorrencia, longitude, latitude);
			
			//-- envia para o eventual inclusao -- EJB assincrono
			Endereco endereco = copiarEndereco(ocorrencia);
			adicionarGeocodeEJB.adicionarCasoNaoExista(endereco);
		}
		else
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

			endereco = enderecoConverter.paraObjeto(json, Endereco.class);
			atribuirRetorno(endereco, ocorrencia);
			logger.info("Geocode retornado com status [" + ocorrencia.getAuxiliar().getGeocoderStatus() + "].");
		}
		
		OcorrenciaCompostaTO ocorrenciaCompostaTO = new OcorrenciaCompostaTO(ocorrencia, controleOcorrencia);
		return ocorrenciaCompostaTO;
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
		if (ocorrencia.getAuxiliar().getGeometry() != null)
		{
			endereco.getGeometry().setLongitude(ocorrencia.getAuxiliar().getGeometry().getLongitude());
			endereco.getGeometry().setLatitude(ocorrencia.getAuxiliar().getGeometry().getLatitude());
		}
		endereco.setGeocodeStatus(ocorrencia.getAuxiliar().getGeocoderStatus());
		return endereco;
	}
	
	private void atribuirRetorno(Endereco endereco, Ocorrencia ocorrencia)
	{
		Auxiliar auxiliar = ocorrencia.getAuxiliar();
		auxiliar.setGeocoderStatus(endereco.getGeocodeStatus());
		if (auxiliar.getGeocoderStatus().equalsIgnoreCase("OK"))
		{
			atribuirGeometry(ocorrencia, endereco.getGeometry().getLongitude(), endereco.getGeometry().getLatitude());
		}
	}
	
	private void atribuirGeometry(Ocorrencia ocorrencia, Double longitude, Double latitude)
	{
		Auxiliar auxiliar = ocorrencia.getAuxiliar();
		if (auxiliar.getGeometry() == null)
		{
			auxiliar.setGeometry(new Point());
		}
		auxiliar.getGeometry().setLngLat(longitude, latitude);
	}

}
