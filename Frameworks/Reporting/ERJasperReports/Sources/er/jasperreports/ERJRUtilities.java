package er.jasperreports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.util.JRLoader;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.exception.NestableRuntimeException;

import com.webobjects.appserver.WOApplication;

import er.extensions.appserver.ERXResourceManager;
import er.extensions.foundation.ERXFileUtilities;

/**
 * 8/24/2010: All methods related to on the fly compilation of jrxml templates have been removed since this does not make sense
 * 
 * @author kieran
 *
 */
public class ERJRUtilities {

	/**
	 * It is important to use the JasperRunManager method called below and avoid the use of InputStream etc because
	 * by providing the file pathname of the report to the JasperRunManager, it will then look for images defined by String
	 * as filenames and paths relative to the file path of the report template itself. Thus making it easy for us to implement
	 * report images by just dropping them into the Resources directory alongside the jasper report template.
	 * 
	 * @param compiledReportName
	 * @param frameworkName
	 * @param parameters
	 * @param dataSource
	 * @return the completed report PDF file
	 */
	public static File runCompiledReportToPDFFile(String compiledReportName, String frameworkName, Map parameters, JRDataSource dataSource) {
		try {
			// TODO: Verify that the compiled report file exists so we can
			// give a better error message, ie., fail early!

			File destFile = File.createTempFile(compiledReportName + System.currentTimeMillis(), ".pdf");
			
			String inputFileName = ERXFileUtilities.pathURLForResourceNamed(compiledReportName, frameworkName, null).getFile();
			JasperRunManager.runReportToPdfFile(inputFileName, destFile.getPath(), parameters, dataSource);

			return destFile;
			
		} catch (IOException e) {
			throw new NestableRuntimeException("Failed to generate report " + compiledReportName, e);
		} catch (JRException e) {
			throw new NestableRuntimeException("Failed to generate report " + compiledReportName, e);
		}
	}

	/**
	 * @param jrField
	 * @return something more useful than the built-in toString method for debugging
	 */
	public static String toString(JRField jrField) {
		ToStringBuilder t = new ToStringBuilder(jrField);
		t.append("description", jrField.getDescription());
		t.append("name", jrField.getName());
		t.append("valueClass", jrField.getValueClass());
		t.append("valueClassName", jrField.getValueClassName());
		t.append("propertiesMap", jrField.getPropertiesMap());
		return t.toString();
	}
}
