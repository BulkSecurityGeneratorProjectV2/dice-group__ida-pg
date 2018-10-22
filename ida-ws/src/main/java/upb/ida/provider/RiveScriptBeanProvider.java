package upb.ida.provider;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.rivescript.Config;
import com.rivescript.RiveScript;

import upb.ida.constant.IDALiteral;
import upb.ida.temp.ExampleMacro;


/**
 * Beans provider for the rivescript bot instance
 * 
 */

@Component
public class RiveScriptBeanProvider {

	@Autowired
	private ServletContext context;
	@Autowired
	private LoadDataContent loadDataContent;
	@Autowired
	private FdgHandler fdgHandler;
	@Autowired
	private BgdHandler bgdHandler;
	@Autowired
	private ClusterConHandler clusterConHandler;
	@Autowired
	private ParamsHandler paramsHandler;
	@Autowired
	private UserParamEntry userParamEntry;
	@Autowired
	private UserParamValueCollector userParamValueCollector;
	@Autowired
	private ClusterDataGetter clusterDataGetter;
	@Autowired
	private CheckParamCollected checkParamCollected;

	/**
	 * Method to provide a session scoped bean for the RiveScript bot
	 * @return - RiveScript Instance
	 */
	@Bean
	@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
	@Qualifier("sessionBotInstance")
	public RiveScript initBotInstance() {

		RiveScript bot = new RiveScript(Config.utf8());

		// Load the Rivescript directory.
		bot.loadDirectory(context.getRealPath(IDALiteral.RS_DIRPATH));

		// Sort the replies and set Subroutine calls for designated functionality
		bot.sortReplies();
		bot.setSubroutine("sayname", new ExampleMacro());
		bot.setSubroutine("loadDataset", loadDataContent);
		bot.setSubroutine("FdgHandler", fdgHandler);
		bot.setSubroutine("BgdHandler", bgdHandler);
		bot.setSubroutine("ClusterConHandler", clusterConHandler);
		bot.setSubroutine("ParamsHandler", paramsHandler);
		bot.setSubroutine("UserParamEntry", userParamEntry);
		bot.setSubroutine("UserParamValueCollector", userParamValueCollector);
		bot.setSubroutine("ClusterDataGetter", clusterDataGetter);
		bot.setSubroutine("CheckParamCollected", checkParamCollected);

		return bot;
	}

	

}