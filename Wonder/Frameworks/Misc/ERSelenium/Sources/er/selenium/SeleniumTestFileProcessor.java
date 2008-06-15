package er.selenium;

import java.io.File;

import org.apache.log4j.Logger;

import er.extensions.foundation.ERXFileUtilities;
import er.extensions.foundation.ERXUtilities;
import er.selenium.filters.SeleniumTestFilter;
import er.selenium.io.SeleniumImporterExporterFactory;
import er.selenium.io.SeleniumTestImporter;

public class SeleniumTestFileProcessor {
	private static final Logger log = Logger.getLogger(SeleniumTestFileProcessor.class);
	
	private final File testFile;
	private final SeleniumTestFilter filter;

	public SeleniumTestFileProcessor(File testFile, SeleniumTestFilter filter) {
		this.testFile = testFile;
		this.filter = filter;
	}
	
	public SeleniumTest process() {
		String extension = "." + ERXFileUtilities.fileExtension(testFile.getName()); 
		
		SeleniumTestImporter importer = SeleniumImporterExporterFactory.instance().findImporterByExtension(extension);
		if (importer == null) {
			throw new RuntimeException("Can't process '" + testFile.getAbsolutePath() + "': unsupported file type ('" + extension + "')");
		}
		
    	try {
    		String fileContents = ERXFileUtilities.stringFromFile(testFile, "UTF-8");
    		SeleniumTest result = importer.process(fileContents);
    		if (filter != null) {
    			result = filter.processTest(result);
    		}
    		return result;
    	} catch (Exception e) {
    		log.debug(ERXUtilities.stackTrace(e));
    		throw new RuntimeException("Test import for '" + testFile.getAbsolutePath() + "' failed.", e);
    	}

	}
}
