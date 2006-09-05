/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;
import com.webobjects.jdbcadaptor.*;

/** 
 * <code>Configuration Manager</code> handles rapid turnaround for 
 * system configuration as well as swizzling of the EOModel connection 
 * dictionaries. 
 * <p>
 * <strong>Placing configuration parameters</strong>
 * <p> 
 * You can provide the system configuration by the following ways:<br/>
 * Note: This is the standard way for WebObjects 5.x applications.
 * <ul>
 *   <li><code>Properties</code> file under the Resources group of the  
 *       application and framework project. 
 *       It's a {@link java.util.Properties} file and Project Wonder's 
 *       standard project templates include it. (The templates won't 
 *       be available on some platforms at this moment.)</li>
 * 
 *   <li><code>WebObjects.properties</code> under the user home directory; 
 *       same format to Properties. <br/> 
 *       Note that the user home directory depends on the user who 
 *       launch the application. They may change between the 
 *       developent and deployment time.</li>
 * 
 *   <li>Command line arguments<br/>
 *       For example: <code>-WOCachingEnabled false -com.webobjects.pid $$</code></br>
 *       Don't forget to put a dash "-" before the key.</li> 
 * </ul>
 * <p>
 * <strong>Loading order of the configuration parameters</strong>
 * <p>
 * When the application launches, configuration parameters will 
 * be loaded by the following order. ERXConfigurationManager trys 
 * to reload them by the exactly same order when one of those 
 * configuration files changes. 
 * <p>
 * 1. Properties in frameworks that the application links to<br/>
 * 2. Properties in the application<br/>
 * 3. WebObjects.properties under the home directory<br/>
 * 4. Command line arguments<br/>
 * <p>
 * If there is a conflicting parameter between the files and 
 * arguments, the latter one overrides the earlier one. 
 * <p>
 * Note that the order between frameworks does not seems 
 * to be specified. You should not put conflicting parameters 
 * between framework Properties files. On the other hand, 
 * the application Properties should be always loaded after 
 * all framework Properties are loaded. You can safely 
 * override parameters on the frameworks from the applications
 * Properties. 
 * 
 * 
 * 
 * <p>
 * <strong>Changing the connection dictionary</strong>
 * <p>
 * To do this for Oracle you can either specify on a per model basis 
 * or on a global basis.
 * <pre>
 * <strong>Global:</strong>
 * 		dbConnectServerGLOBAL = myDatabaseServer
 * 		dbConnectUserGLOBAL = me
 * 		dbConnectPasswordGLOBAL = secret
 * <strong>Per Model for say model ER:</strong>
 * 		ER.DBServer = myDatabaseServer
 * 		ER.DBUser = me
 * 		ER.DBPassword = secret
 * 
 * <strong>Openbase:</strong> same, with DBDatabase and DBHostname
 * 
 * <strong>JDBC:</strong> same with urlGlobal, or db.url
 * 
 * </pre>
 * <p>
 * Prototypes can be swapped globally or per model either by 
 * hydrating an archived prototype entity for a file or from 
 * another entity.
 */
public class ERXConfigurationManager {

    /** logging support */
    public static final Logger log = Logger.getLogger(ERXConfigurationManager.class);

    /** 
     * Notification posted when the configuration is updated.  
     * The Java system properties is the part of the configuration.
     */ 
    public static final String ConfigurationDidChangeNotification = "ConfigurationDidChangeNotification";        

    /** Configuration manager singleton */ 
    static ERXConfigurationManager defaultManager = null;
    
    private String[] _commandLineArguments; 
    private NSMutableArray _monitoredProperties;
    private boolean _isInitialized = false;
    private boolean _isRapidTurnAroundInitialized = false;

    /** Private constructor to prevent instantiation from outside the class */
    private ERXConfigurationManager() {
        /* empty */
    }

    /**
     * If set, touching this path will be used to signal a change to properties files.
     */
    private static String propertiesTouchFile() {
        return ERXProperties.stringForKey("er.extensions.ERXConfigurationManager.PropertiesTouchFile");
    }
    
    /**
     * Returns the single instance of this class
     * 
     * @return the configuration manager
     */
    public static ERXConfigurationManager defaultManager() {
        if (defaultManager == null)
            defaultManager = new ERXConfigurationManager();
        return defaultManager;
    }
    
    /** 
     * Returns the command line arguments. 
     * {@link ERXApplication#main} sets this value. 
     * 
     * @return the command line arguments as a String[]
     * @see #setCommandLineArguments
     */
    public String[] commandLineArguments() {
        return _commandLineArguments;
    }
    
    /** 
     * Sets the command line arguments. 
     * {@link ERXApplication#main} will call this method 
     * when the application starts up. 
     * 
     * @see #commandLineArguments
     */
    public void setCommandLineArguments(String [] newCommandLineArguments) {
        _commandLineArguments = newCommandLineArguments;
    }

    /**
     * Initializes the configuration manager. 
     * The framework principal {@link ERXExtensions} calls 
     * this method when the ERExtensions framework is loaded. 
     */
    public void initialize() {
        if (! _isInitialized) {
            _isInitialized = true;
            NSNotificationCenter.defaultCenter().addObserver(this,
                                                             new NSSelector("modelAddedHandler",
                                                                            ERXConstant.NotificationClassArray),
                                                             EOModelGroup.ModelAddedNotification,
                                                             null);
            loadOptionalConfigurationFiles();
        }
    }

    /**
     * Sets up the system for rapid turnaround mode. It will watch the 
     * changes on Properties files in application and framework bundles 
     * and WebObjects.properties under the home directory. 
     * Rapid turnaround mode will only be enabled if there are such 
     * files available and system has WOCaching disabled.
     */
    public void configureRapidTurnAround() {
        if (_isRapidTurnAroundInitialized)      return;

        _isRapidTurnAroundInitialized = true;
        
        boolean rapidTurnaround = true;
        
        if (WOApplication.application()!=null && WOApplication.application().isCachingEnabled()) {
            log.info("WOCachingEnabled is true. Disabling the rapid turnaround for Properties files");
            rapidTurnaround = false;
        }

        NSArray propertyPaths = ERXProperties.pathsForUserAndBundleProperties(/* logging */ true);
        _monitoredProperties = new NSMutableArray();

        for (Enumeration e = propertyPaths.objectEnumerator(); e.hasMoreElements();) {
            String path = (String) e.nextElement();

            _monitoredProperties.addObject(path);
            
            if (rapidTurnaround) {
                registerForFileNotification(path, "updateSystemProperties");
            }
        }
        
        if (!rapidTurnaround) {
            registerPropertiesTouchFiles();
        }
    }

    private void registerPropertiesTouchFiles() {
        String propertiesTouchFile = propertiesTouchFile();
        
        if (propertiesTouchFile != null) {
            String appNamePlaceHolder = "/{AppName}/";
            int appNamePlaceHolderIndex = propertiesTouchFile.lastIndexOf(appNamePlaceHolder);
            if (appNamePlaceHolderIndex == -1) {
                registerForFileNotification(propertiesTouchFile, "updateAllSystemProperties");
            }
            else {
                if (WOApplication.application() != null) {
                    StringBuffer appSpecificTouchFile = new StringBuffer();
                    
                    appSpecificTouchFile.append(propertiesTouchFile.substring(0, appNamePlaceHolderIndex + 1));
                    appSpecificTouchFile.append(WOApplication.application().name());
                    appSpecificTouchFile.append(propertiesTouchFile.substring(appNamePlaceHolderIndex + appNamePlaceHolder.length() - 1));
                    
                    registerForFileNotification(appSpecificTouchFile.toString(), "updateAllSystemProperties");
                }
                
                StringBuffer globalTouchFile = new StringBuffer();
                
                globalTouchFile.append(propertiesTouchFile.substring(0, appNamePlaceHolderIndex + 1));
                globalTouchFile.append(propertiesTouchFile.substring(appNamePlaceHolderIndex + appNamePlaceHolder.length()));
                
                registerForFileNotification(globalTouchFile.toString(), "updateAllSystemProperties");
            }            
        }
    }
    
    private void registerForFileNotification(String path, String callbackMethod) {
        try {
            ERXFileNotificationCenter.defaultCenter().addObserver(this,
                                                                  new NSSelector(callbackMethod, ERXConstant.NotificationClassArray),
                                                                  path);
            log.debug("Registered: " + path);
        } catch (Exception ex) {
            log.error("An exception occured while registering the observer for the "
                      + "logging configuration file: " 
                      + ex.getClass().getName() + " " + ex.getMessage());
        }            
    }

    /**
     * This will overlay the current system config files. It will then
     * re-load the command line args.
     */
    public void loadOptionalConfigurationFiles() {
        if (ERXProperties.optionalConfigurationFiles() != null
            && ERXProperties.optionalConfigurationFiles().count() > 0) {
            Properties systemProperties = System.getProperties();
            for (Enumeration configEnumerator = ERXProperties.optionalConfigurationFiles().objectEnumerator();
                 configEnumerator.hasMoreElements();) {
                String configFile = (String)configEnumerator.nextElement();
                File file = new File(configFile);
                if (file.exists()  &&  file.isFile()  &&  file.canRead()) {
                    try {
                        Properties props = ERXProperties.propertiesFromFile(file);
                        ERXProperties.transferPropertiesFromSourceToDest(props, systemProperties);
                        ERXSystem.updateProperties();
                    } catch (java.io.IOException ex) {
                        log.error("Unable to load optional configuration file: " + configFile, ex);
                    }
                }
            }
            if (_commandLineArguments != null  &&  _commandLineArguments.length > 0)
                _reinsertCommandLineArgumentsToSystemProperties(_commandLineArguments);
        }
    }
    
    /** 
     * Updates the configuration from the current configuration and 
     * posts {@link #ConfigurationDidChangeNotification}. It also  
     * calls {@link Logger#configureLogging} to reconfigure 
     * the logging system. 
     * <p>
     * The configuration files: Properties and WebObjects.properties 
     * files are reloaded to the Java system properties by the same 
     * order to the when the system starts up. Then the command line 
     * arguments will be applied to the properties again so that 
     * the configuration will be consistent during the application 
     * lifespan. 
     * <p>
     * This method is called when rapid turnaround is enabled and one 
     * of the configuration files changes.
     * 
     * @param  n NSNotification object for the event (null means load all files)
     */
    public synchronized void updateSystemProperties(NSNotification n) {
        _updateSystemPropertiesFromMonitoredProperties(n != null ? (File)n.object() : null, _monitoredProperties);
        if (_commandLineArguments != null  &&  _commandLineArguments.length > 0) 
            _reinsertCommandLineArgumentsToSystemProperties(_commandLineArguments);
        
        ERXLogger.configureLoggingWithSystemProperties();
        
        NSNotificationCenter.defaultCenter().postNotification(ConfigurationDidChangeNotification, null);
    }
    
    public synchronized void updateAllSystemProperties(NSNotification notification) {
        updateSystemProperties(null);
    }
    
    /**
     * If updatedFile is null, all files are reread.
     */
    private void _updateSystemPropertiesFromMonitoredProperties(File updatedFile, NSArray monitoredProperties) {
        if (monitoredProperties == null  ||  monitoredProperties.count() == 0)  return;
        
        int firstDirtyFile = 0;
        
        if (updatedFile != null) {
            try {
                // Find the position of the updatedFile in the monitoredProperties list, 
                // so that we can reload it and everything after it on the list. 
                firstDirtyFile = monitoredProperties.indexOfObject(updatedFile.getCanonicalPath());
                if (firstDirtyFile < 0) {
                    return;
                }
            } catch (IOException ex) {
                log.error(ex.toString());
                return; 
            }
        }
        

        Properties systemProperties = System.getProperties();
        for (int i = firstDirtyFile; i < _monitoredProperties.count(); i++) {
            String monitoredPropertiesPath = (String) _monitoredProperties.objectAtIndex(i);
            Properties loadedProperty = ERXProperties.propertiesFromPath(monitoredPropertiesPath);
            ERXProperties.transferPropertiesFromSourceToDest(loadedProperty, systemProperties);
            ERXSystem.updateProperties();
        }
    }

    private void _reinsertCommandLineArgumentsToSystemProperties(String[] commandLineArguments) {
        Properties commandLineProperties = ERXProperties.propertiesFromArgv(commandLineArguments);
        Properties systemProperties = System.getProperties(); 
        ERXProperties.transferPropertiesFromSourceToDest(commandLineProperties, systemProperties);
        ERXSystem.updateProperties();
        log.debug("Reinserted the command line arguments to the system properties.");
    }

    /**
     * Called when a model is loaded. This will reset the connection
     * dictionary and insert the correct EOPrototypes if those are used
     * @param n notification posted when a model is loaded. The object is
     * 		the model.
     */
    public void modelAddedHandler(NSNotification n) {
        resetConnectionDictionaryInModel((EOModel)n.object());
    }
    
    /**
     * Resets the connection dictionary to the specified values that are in the defaults.
     * This method will look for defaults in the form 
     * 		<MODELNAME>.DBServer
     * 		<MODELNAME>.DBUser
     * 		<MODELNAME>.DBPassword
     * 		<MODELNAME>.URL (for JDBC)        
     *   if the serverName and username both exists, we overwrite the connection dict
     *      (password is optional). Otherwise we fall back to what's in the model.
     *
     * Likewise default values can be specified of the form:
     * dbConnectUserGLOBAL
     * dbConnectPasswordGLOBAL
     * dbConnectURLGLOBAL
     * @param aModel to be reset
     */
    public void resetConnectionDictionaryInModel(EOModel aModel)  {
        if(aModel!=null) {
            String aModelName=aModel.name();
            log.debug("Adjusting "+aModelName);
            NSMutableDictionary newConnectionDictionary=null;
            if(aModel.adaptorName() == null) {
                log.info("Skipping model '" + aModel.name() + "', it has no adaptor name set");
                return;
            }

            // Support for EODatabaseConfig from EntityModeler.  The value of YourEOModelName.DBConfigName is
            // used to lookup the corresponding EODatabaseConfig name from user info.  The connection dictionary
            // defined in the databaseConfig section completely replaces the connection dictionary in the 
            // EOModel. After the initial replacement, all the additional PW model configurations are then 
            // applied to the new dictionary.
            String databaseConfigName = ERXSystem.getProperty(aModelName + ".DBConfigName");
            NSDictionary databaseConfig = null;
            if (databaseConfigName != null) {
              NSDictionary userInfo = aModel.userInfo();
              if (userInfo != null) {
                NSDictionary entityModelerDictionary = (NSDictionary) userInfo.objectForKey("_EntityModeler");
                if (entityModelerDictionary != null) {
                  NSDictionary databaseConfigsDictionary = (NSDictionary) entityModelerDictionary.objectForKey("databaseConfigs");
                  if (databaseConfigsDictionary != null) {
                    databaseConfig = (NSDictionary) databaseConfigsDictionary.objectForKey(databaseConfigName);
                    if (databaseConfig != null) {
                      NSDictionary connectionDictionary = (NSDictionary) databaseConfig.objectForKey("connectionDictionary");
                      aModel.setConnectionDictionary(connectionDictionary);
                    }
                  }
                }
              }
            }
            
            if (aModel.adaptorName().indexOf("Oracle")!=-1) {
                String serverName= ERXSystem.getProperty(aModelName + ".DBServer");
                serverName=serverName==null ? ERXSystem.getProperty("dbConnectServerGLOBAL") : serverName;
                String userName= ERXSystem.getProperty(aModelName + ".DBUser");
                userName= userName ==null ? ERXSystem.getProperty("dbConnectUserGLOBAL") : userName;
                String passwd= ERXSystem.getProperty(aModelName + ".DBPassword");
                passwd= passwd ==null ? ERXSystem.getProperty("dbConnectPasswordGLOBAL") : passwd;

                if((serverName!=null) || (userName!=null) || (passwd!=null)) {
                    newConnectionDictionary=new NSMutableDictionary(aModel.connectionDictionary());
                    if (serverName!=null) newConnectionDictionary.setObjectForKey(serverName,"serverId");
                    if (userName!=null) newConnectionDictionary.setObjectForKey(userName,"userName");
                    if (passwd!=null) newConnectionDictionary.setObjectForKey(passwd,"password");
                    aModel.setConnectionDictionary(newConnectionDictionary);
                }
                
            } else if (aModel.adaptorName().indexOf("Flat")!=-1) {
                String path= ERXSystem.getProperty(aModelName + ".DBPath");
                path = path ==null ? ERXSystem.getProperty("dbConnectPathGLOBAL") : path;
                if (path!=null) {                    
                    if (path.indexOf(" ")!=-1) {
                        NSArray a=NSArray.componentsSeparatedByString(path," ");
                        if (a.count()==2) {
                            path = ERXFileUtilities.pathForResourceNamed((String)a.objectAtIndex(0),                                                                                                     (String)a.objectAtIndex(1), null);
                        }
                    }
                } else {
                    // by default we take <modelName>.db in the directory we found the model
                    path=aModel.pathURL().getFile();
                    path=NSPathUtilities.stringByDeletingLastPathComponent(path);
                    path=NSPathUtilities.stringByAppendingPathComponent(path,aModel.name()+".db");                    
                }
                newConnectionDictionary=new NSMutableDictionary(aModel.connectionDictionary());
                if (path!=null) newConnectionDictionary.setObjectForKey(path,"path");
                if (operatingSystem()==WindowsOperatingSystem)
                    newConnectionDictionary.setObjectForKey("\r\n","rowSeparator");
                aModel.setConnectionDictionary(newConnectionDictionary);
            } else if (aModel.adaptorName().indexOf("OpenBase")!=-1) {
                String db= ERXSystem.getProperty(aModelName + ".DBDatabase");
                db = db ==null ? ERXSystem.getProperty("dbConnectDatabaseGLOBAL") : db;
                String h= ERXSystem.getProperty(aModelName + ".DBHostName");
                h = h ==null ? ERXSystem.getProperty("dbConnectHostNameGLOBAL") : h;
                if (h!=null || db!=null) {
                    newConnectionDictionary=new NSMutableDictionary(aModel.connectionDictionary());
                    if (db!=null) newConnectionDictionary.setObjectForKey(db, "databaseName");
                    if (h!=null) newConnectionDictionary.setObjectForKey(h, "hostName");
                    aModel.setConnectionDictionary(newConnectionDictionary);
                }
            } else if (aModel.adaptorName().indexOf("JDBC")!=-1) {
                if (aModel.adaptorName().equals("JDBC") && ERXProperties.booleanForKeyWithDefault("er.jdbcadaptor.ERJDBCAdaptor.poolModelConnections", false)) {
                    try {
                        // is the JavaERJDBCAdaptor framework available?
                        Class cl = Class.forName("er.jdbcadaptor.ERJDBCAdaptor");
                        // important if one compiles with jikes as jikes would eliminate the call above!
                        log.debug(cl);
                        aModel.setAdaptorName("ERJDBC");
                    } catch (ClassNotFoundException e1) {
                        log.error("cannot use Model Connection pooling because framework JavaERJDBCAdaptor is missing. Make sure the framework is loaded by the application");
                    }
                }
                NSDictionary jdbcInfoDictionary = null;
                String url = ERXSystem.getProperty(aModelName + ".URL");
                url = url == null ? ERXSystem.getProperty("dbConnectURLGLOBAL") : url;
                String userName= ERXSystem.getProperty(aModelName + ".DBUser");
                userName= userName ==null ? ERXSystem.getProperty("dbConnectUserGLOBAL") : userName;
                String passwd= ERXSystem.getProperty(aModelName + ".DBPassword");
                passwd= passwd ==null ? ERXSystem.getProperty("dbConnectPasswordGLOBAL") : passwd;
                String driver= ERXSystem.getProperty(aModelName + ".DBDriver");
                driver= driver ==null ? ERXSystem.getProperty("dbConnectDriverGLOBAL") : driver;
                String serverName= ERXSystem.getProperty(aModelName + ".DBServer");
                serverName=serverName==null ? ERXSystem.getProperty("dbConnectServerGLOBAL") : serverName;
                String h= ERXSystem.getProperty(aModelName + ".DBHostName");
                h = h ==null ? ERXSystem.getProperty("dbConnectHostNameGLOBAL") : h;
                String jdbcInfo= ERXSystem.getProperty(aModelName + ".DBJDBCInfo");
                
                // additional information used for ERXJDBCConnectionBroker
                String minConnections = ERXSystem.getProperty(aModelName + ".DBMinConnections");
                minConnections = minConnections == null ? ERXProperties.stringForKeyWithDefault("dbMinConnectionsGLOBAL", "20") : minConnections;
                String maxConnections = ERXSystem.getProperty(aModelName + ".DBMaxConnections");
                maxConnections = maxConnections == null ? ERXProperties.stringForKeyWithDefault("dbMaxConnectionsGLOBAL", "20") : maxConnections;
                String logPath = ERXSystem.getProperty(aModelName + ".DBLogPath");
                logPath = logPath == null ? ERXProperties.stringForKeyWithDefault("dbLogPathGLOBAL", "/tmp/ERXJDBCConnectionBroker_@@name@@_@@WOPort@@.log") : logPath;
                String connectionRecycle = ERXSystem.getProperty(aModelName + ".DBConnectionRecycle");
                connectionRecycle = connectionRecycle == null ? ERXProperties.stringForKeyWithDefault("dbConnectionRecycleGLOBAL", "1.0") : connectionRecycle;
                String maxCheckout = ERXSystem.getProperty(aModelName + ".DBMaxCheckout");
                maxCheckout = maxCheckout == null ? ERXProperties.stringForKeyWithDefault("dbMaxCheckoutGLOBAL", "86400") : maxCheckout;
                String debugLevel = ERXSystem.getProperty(aModelName + ".DBDebugLevel");
                debugLevel = debugLevel == null ? ERXProperties.stringForKeyWithDefault("dbDebugLevelGLOBAL", "1") : debugLevel;
                
                if (jdbcInfo != null && jdbcInfo.length() > 0 && jdbcInfo.charAt(0) == '^') {
                    String modelName = jdbcInfo.substring(1, jdbcInfo.length());
                    EOModel modelForCopy = aModel.modelGroup().modelNamed(modelName);
                    if (modelForCopy != null) {
                        jdbcInfoDictionary = (NSDictionary)modelForCopy.connectionDictionary().objectForKey("jdbc2Info");
                    } else {
                        log.warn("Unable to find model named \"" + modelName + "\"");
                        jdbcInfo = null;
                    }
                }

                jdbcInfo= jdbcInfo ==null ? ERXSystem.getProperty("dbConnectJDBCInfoGLOBAL") : jdbcInfo;
                String plugin= ERXSystem.getProperty(aModelName + ".DBPlugin");
                plugin= plugin ==null ? ERXSystem.getProperty("dbConnectPluginGLOBAL") : plugin;
                
                // build the URL if we have a Postgresql plugin
                if ("Postgresql".equals(plugin) 
                        && ERXStringUtilities.stringIsNullOrEmpty(url) 
                        && !ERXStringUtilities.stringIsNullOrEmpty(serverName)
                        && !ERXStringUtilities.stringIsNullOrEmpty(h)) {
                    url = "jdbc:postgresql://"+h+"/"+serverName;
                }
                
                if (url!=null || userName!=null || passwd!=null || driver!=null || jdbcInfo!=null || plugin!=null) {
                    newConnectionDictionary=new NSMutableDictionary(aModel.connectionDictionary());
                    if (url!=null) newConnectionDictionary.setObjectForKey(url, "URL");
                    if (userName!=null) newConnectionDictionary.setObjectForKey(userName,"username");
                    if (passwd!=null) newConnectionDictionary.setObjectForKey(passwd,"password");
                    if (driver!=null) newConnectionDictionary.setObjectForKey(driver,"driver");
                    if (jdbcInfoDictionary != null) {
                        newConnectionDictionary.setObjectForKey(jdbcInfoDictionary, "jdbc2Info");
                    } else if (jdbcInfo!=null) {
                        NSDictionary d=(NSDictionary)NSPropertyListSerialization.propertyListFromString(jdbcInfo);
                        if (d!=null)
                            newConnectionDictionary.setObjectForKey(d,"jdbc2Info");
                        else
                            newConnectionDictionary.removeObjectForKey("jdbc2Info");
                    }
                    if (plugin!=null) newConnectionDictionary.setObjectForKey(plugin,"plugin");    
                    
                    // set the information for ERXJDBCConnectionBroker
                    newConnectionDictionary.setObjectForKey(minConnections, "minConnections");
                    newConnectionDictionary.setObjectForKey(maxConnections, "maxConnections");
                    newConnectionDictionary.setObjectForKey(logPath, "logPath");
                    newConnectionDictionary.setObjectForKey(connectionRecycle, "connectionRecycle");
                    newConnectionDictionary.setObjectForKey(maxCheckout, "maxCheckout");
                    newConnectionDictionary.setObjectForKey(debugLevel, "debugLevel");
                    
                    aModel.setConnectionDictionary(newConnectionDictionary);
                }
            }

            boolean removeJdbc2Info = ERXProperties.booleanForKeyWithDefault(aModelName + ".removeJdbc2Info", false);
            if (removeJdbc2Info && newConnectionDictionary != null) {
              newConnectionDictionary.removeObjectForKey("jdbc2Info");
            }

            if (newConnectionDictionary!=null && log.isDebugEnabled()) {
                if (newConnectionDictionary.objectForKey("password")!=null)
                    newConnectionDictionary.setObjectForKey("<deleted for log>", "password");
                log.debug("New Connection Dictionary for "+aModelName+": "+newConnectionDictionary);                
            }
            
            
            // based on an idea from Stefan Apelt <stefan@tetlabors.de>
            String f = ERXSystem.getProperty(aModelName + ".EOPrototypesFile");
            f = f ==null ? ERXSystem.getProperty("EOPrototypesFileGLOBAL") : f;
            if(f != null) {
                NSDictionary dict = (NSDictionary)NSPropertyListSerialization.propertyListFromString(ERXStringUtilities.stringFromResource(f, "", null));
                if(dict != null) {
                    if (log.isDebugEnabled()) log.debug("Adjusting prototypes from " + f);
                    EOEntity proto = aModel.entityNamed("EOPrototypes");
                    if (proto == null) {
                        log.warn("No prototypes found in model named \"" + aModelName + "\", although the EOPrototypesFile default was set!");
                    } else {
                        aModel.removeEntity(proto);
                        proto = new EOEntity(dict, aModel);
                        proto.awakeWithPropertyList(dict);
                        aModel.addEntity(proto);
                    }
                }
            }
            String prototypeEntityName = ERXSystem.getProperty(aModelName + ".EOPrototypesEntity");
            if (prototypeEntityName == null && databaseConfig != null) {
            	prototypeEntityName = (String) databaseConfig.objectForKey("prototypeEntityName");
            }

            // If you don't explicitly set a prototype name, and you don't declare a preferred databaseConfig,
            // then attempt to load Wonder-style prototypes with the name EOJDBC(driverName)Prototypes.
            if (prototypeEntityName == null) {
            	if ("JDBC".equals(aModel.adaptorName())) {
            		NSDictionary connectionDictionary = aModel.connectionDictionary();
            		if (connectionDictionary != null) {
            			String jdbcUrl = (String) connectionDictionary.objectForKey("URL");
            			if (jdbcUrl != null) {
            				String pluginName = (String) connectionDictionary.objectForKey("plugin");
            				if(pluginName == null) {
            					pluginName = JDBCPlugIn.plugInNameForURL(jdbcUrl);
                				if(pluginName == null) {
                					int firstColon = jdbcUrl.indexOf(':');
                					int secondColon = jdbcUrl.indexOf(':', firstColon + 1);
                    				if (firstColon != -1 && secondColon != -1) {
                    					String driverName = jdbcUrl.substring(firstColon + 1, secondColon);
                    					String driverPrototypeEntityName = "EOJDBC" + driverName + "Prototypes";
                    					// This check isn't technically necessary since it doesn't down below, but since
                    					// we are guessing here, I don't want themt o get a warning about the prototype not
                    					// being found if they aren't even using Wonder prototypes.
                    					if (aModel.modelGroup().entityNamed(driverPrototypeEntityName) != null) {
                    						prototypeEntityName = driverPrototypeEntityName;
                    					}
                    				}
                				} else {
                					pluginName = ERXStringUtilities.lastPropertyKeyInKeyPath(pluginName);
                					pluginName = pluginName.replaceFirst("PlugIn", "");
                				}
            				}
            				if (pluginName != null) {
            					String pluginPrototypeEntityName = "EOJDBC" + pluginName + "Prototypes";
            					// This check isn't technically necessary since it doesn't down below, but since
            					// we are guessing here, I don't want themt o get a warning about the prototype not
            					// being found if they aren't even using Wonder prototypes.
            					if (aModel.modelGroup().entityNamed(pluginPrototypeEntityName) != null) {
            						prototypeEntityName = pluginPrototypeEntityName;
            					} else {
            						log.error("No prototypes found for plugin: " + pluginName + ", using EOPrototypes: " + aModelName);
            					}
            				}
            			}
            		}
            	}
            }

            // global prototype setting not supported yet
            //e = e ==null ? ERXSystem.getProperty("EOPrototypesEntityGLOBAL") : e;
            if(prototypeEntityName != null) {
                // we look for the entity globally so we can have one prototype entity
                EOEntity newPrototypeEntity = aModel.modelGroup().entityNamed(prototypeEntityName);
                if (newPrototypeEntity == null) {
                    log.warn("Prototype Entity named "+prototypeEntityName+" not found in model "+aModel.name());
                } else {
                    if (log.isDebugEnabled()) log.debug("Adjusting prototypes to those from entity " + prototypeEntityName);

                    EOEntity proto = aModel.entityNamed("EOPrototypes");
                    if(proto != null) aModel.removeEntity(proto);
                    
                    aModel.removeEntity(newPrototypeEntity);
                    newPrototypeEntity.setName("EOPrototypes");
                    aModel.addEntity(newPrototypeEntity);
                }
            }
        }
        
    }


    public final static int WindowsOperatingSystem=1;
    public final static int MacOSXOperatingSystem=2;
    public final static int SolarisOperatingSystem=3;
    public final static int UnknownOperatingSystem=3;

    private int _operatingSystem=0;
    public int operatingSystem() {
        if (_operatingSystem==0) {
            String osName=ERXSystem.getProperty("os.name").toLowerCase();
            if (osName.indexOf("windows")!=-1) _operatingSystem=WindowsOperatingSystem;
            else if (osName.indexOf("solaris")!=-1) _operatingSystem=SolarisOperatingSystem;
            else if (osName.indexOf("macos")!=-1 || osName.indexOf("mac os")!=-1) _operatingSystem=MacOSXOperatingSystem;
            else _operatingSystem=UnknownOperatingSystem;
        }
        return _operatingSystem;
    }

    /**
     * Path to the web server's document root.
     * This implementation tries first to resolve the
     * <code>application.name()+ "DocumentRoot"</code> property value,
     * then the <code>ERXDocumentRoot</> property before
     * getting the <code>DocumentRoot</code> key in your WebServerConfig.plist in the
     * JavaWebObjects bundle.
     * @return to the web server's document root.
     */
    protected String documentRoot; 
    public String documentRoot() {
        if (documentRoot == null) {
            // for WebObjects.properties
            documentRoot = ERXProperties.stringForKey(WOApplication.application().name() + "DocumentRoot");
            if(documentRoot == null) {
                // for command line and Properties
                documentRoot = ERXProperties.stringForKey("ERXDocumentRoot");
                if(documentRoot == null) {
                    // default value
                    NSDictionary dict = ERXDictionaryUtilities.dictionaryFromPropertyList("WebServerConfig", NSBundle.bundleForName("JavaWebObjects"));
                    if(dict != null)
                        documentRoot = (String)dict.objectForKey("DocumentRoot");
                }
            }
        }
        return documentRoot;
    }

    /** holds the host name */
    protected String _hostName;

    /**
     * Gets the default host name for the current local host.
     * @return host name or UnknownHost if the host is unknown.
     */
    public String hostName() {
        if (_hostName == null) {
            try {
                _hostName = java.net.InetAddress.getLocalHost().getHostName();
            } catch (java.net.UnknownHostException ehe) {
                log.warn("Caught unknown host exception: " + ehe.getMessage());
                _hostName = "UnknownHost";
            }
        }
        return _hostName;
    }    
    
    /**
     * Checks if the application is deployed as a servlet.
     * <p>
     * The current implementation only checks if the application  
     * is linked against <code>JavaWOJSPServlet.framework</code>. 
     * 
     * @return true if the application is deployed as a servlet
     */
    public boolean isDeployedAsServlet() {
        NSArray frameworkNames = (NSArray)NSBundle.frameworkBundles().valueForKey("name");
        return frameworkNames.containsObject("JavaWOJSPServlet");
    }
        
}
