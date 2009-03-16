package er.selenium.rc;

import org.apache.log4j.Logger;

import com.thoughtworks.selenium.HttpCommandProcessor;
import com.thoughtworks.selenium.SeleniumException;
import com.webobjects.foundation.NSArray;

import er.selenium.SeleniumTest;

public class SeleniumTestRCRunner {
	private static final Logger log = Logger.getLogger(SeleniumTestRCRunner.class);
	
	private HttpCommandProcessor browser;
	
	public SeleniumTestRCRunner(String host, int port, String browserType, String browserStartUrl) {
		browser = new HttpCommandProcessor(host, port, browserType, browserStartUrl);		
	}

	public void prepare() {
		browser.start();		
	}

	public void run(SeleniumTest test) {
		int processedCommands = 0;
		try {
			for (SeleniumTest.Element element : (NSArray<SeleniumTest.Element>)test.elements()) {
				if (element instanceof SeleniumTest.Command) {
					SeleniumTest.Command command = (SeleniumTest.Command)element;
					log.debug("original command: " + command);
					if (!command.getName().equals("pause")) {
						browser.doCommand(command.getName(), new String[] {command.getTarget(), command.getValue()} );
					} else {
						try {
							Thread.sleep(new Integer(command.getTarget()));
						} catch (NumberFormatException e) {
							log.warn("invalid argument for pause command: " + command.getTarget());
							throw new SeleniumException(e);
						} catch (InterruptedException e) {
							log.warn("pause command interrupted");
						}
					}
					++processedCommands;
				}
			}
		} catch (SeleniumException e) {
			throw new SeleniumTestFailureException(e, test, processedCommands);
		}
	}
	
	public void finish() {
		browser.stop();
	}
}
