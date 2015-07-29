package br.com.fences.deicdivecarbatch.config.listener;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import br.com.fences.deicdivecarbatch.config.AppConfig;


/**
 * Depende da configuracao no servidor da propriedade JNDI 'prototipo02'
 *
 */
@WebListener
public class ConfiguracaoDaAplicacaoContextListener implements ServletContextListener {

	private Logger logger = LogManager.getLogger(ConfiguracaoDaAplicacaoContextListener.class);
		
	private static final String APP_NOME = "deicdivecarbatch";
	
	//-- configuracao no JNDI do Glassfish
	@Resource(mappedName=APP_NOME)
	private Properties appServerConfig;
	
	@Inject
	private AppConfig appConfig;
	
	@PostConstruct
    public void contextInitialized(ServletContextEvent sce)  { 
		
    	configurarLogDaAplicacao();
    	
    	carregarAppConfig();
    	
    	logger.info("*** aplicacao instalada com sucesso ***");
    }

    public void contextDestroyed(ServletContextEvent sce)  { 
    	logger.info("*** aplicacao desinstalada com sucesso ***");
    }
    
    /**
     * Configura o log da aplicacao.
     * Parametros JNDI: logLevel, logConsole, logDiretorio
     */
    private void configurarLogDaAplicacao()
    {
		try
		{
	    	//-- configuracao geral do log
	        //String pattern = "%d{[yyyy-MM-dd HH:mm:ss]} [%5t] [%-5p] [%c] %m %n";
			String pattern = "%d{yyyy-MM-dd HH:mm:ss} [%t] [%-5p] [%c] %m %n";
			
	        PatternLayout	patternLayout	= new PatternLayout(pattern);
	        Logger rootLogger = LogManager.getRootLogger();
	        
	        boolean logConsole = new Boolean(appServerConfig.getProperty("logConsole", "true"));
	        if (logConsole){
	        	rootLogger.addAppender( new ConsoleAppender(patternLayout) );
	        }
	        
	        String logLevel = appServerConfig.getProperty("logLevel", "INFO");
	        if (logLevel.equalsIgnoreCase("FATAL")){
	        	rootLogger.setLevel(Level.FATAL);	
	        }else if(logLevel.equalsIgnoreCase("ERROR")){ 
	        	rootLogger.setLevel(Level.ERROR);
	        }else if(logLevel.equalsIgnoreCase("WARN")){
	        	rootLogger.setLevel(Level.WARN);
	        }else if(logLevel.equalsIgnoreCase("INFO")){
	        	rootLogger.setLevel(Level.INFO);
	        }else if(logLevel.equalsIgnoreCase("DEBUG")){
	        	rootLogger.setLevel(Level.DEBUG);
	        }else if(logLevel.equalsIgnoreCase("TRACE")){
	        	rootLogger.setLevel(Level.TRACE);
	        }else if(logLevel.equalsIgnoreCase("ALL")){
	        	rootLogger.setLevel(Level.ALL);
	        }
	
	        String logDiretorio	= appServerConfig.getProperty("logDiretorio");
	        if (logDiretorio != null && !logDiretorio.trim().isEmpty())
	        {
		        Appender appender = null;
		        try {
					appender = new DailyRollingFileAppender(patternLayout, logDiretorio + "/" + APP_NOME + ".log", "'.'yyyy-MM-dd'.log'");
					rootLogger.addAppender(appender);  
		        } catch (IOException e) {
		        	throw new RuntimeException("Erro na inicializacao do DailyRollingFileAppender", e);
		        } 
	        }
		}
		catch(Exception e)
		{
			String msg = "ERRO NA INICIALIZACAO DO LOG DA APLICACAO !!! [" + e.getMessage() + "].";
			System.out.println(msg);
			e.printStackTrace();
			throw new RuntimeException(e);
		}

    }
    
    private void carregarAppConfig()
    {
    	appConfig.setLogConsole(verificarCarregar("logConsole"));
    	appConfig.setLogLevel(verificarCarregar("logLevel"));
    	appConfig.setLogDiretorio(verificarCarregar("logDiretorio"));
    	appConfig.setServerBackendHost(verificarCarregar("serverBackendHost"));
    	appConfig.setServerBackendPort(verificarCarregar("serverBackendPort"));
    	appConfig.setOcorrenciaRdoBackendHost(verificarCarregar("ocorrenciaRdoBackendHost"));
    	appConfig.setOcorrenciaRdoBackendPort(verificarCarregar("ocorrenciaRdoBackendPort"));
    }
    
    private String verificarCarregar(String propriedade)
    {
    	boolean propDefinida = appServerConfig.containsKey(propriedade);
    	String valor = "";
    	if (propDefinida)
    	{
    		valor = appServerConfig.getProperty(propriedade);
    		if (valor != null && !valor.trim().isEmpty())
    		{
    			String msg = "A propriedade [" + propriedade + "] foi carregada com o valor [" + valor + "].";
    			logger.info(msg);
    		}
    		else
    		{
    			String msg = "A propriedade [" + propriedade + "] definida no JNDI NAO possui valor.";
    			logger.warn(msg);
    		}
    	}
    	else
    	{
			String msg = "A propriedade [" + propriedade + "] NAO esta definida no JNDI.";
			logger.warn(msg);
    	}
    	return valor;
    }
	
}
