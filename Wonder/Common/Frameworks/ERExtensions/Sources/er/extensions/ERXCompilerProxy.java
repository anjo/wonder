// ERXCompilerProxy
// Created by ak on Mon Mar 04 2002

package er.extensions;

import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import com.webobjects.foundation.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.*;
import com.webobjects.appserver._private.WOProjectBundle;
import com.webobjects.appserver._private.WODirectActionRequestHandler;

/* To find out about how to use this class, read the CompilerProxy.html in the Documentation folder */
/* WARNING: this is experimental and the results might be very confusing if you don�t understand what this class tries to do! */

public class ERXCompilerProxy {

    /* logging support */
    protected static final ERXLogger log = ERXLogger.getERXLogger(ERXCompilerProxy.class.getName());
    protected static final ERXLogger classLoaderLog = ERXLogger.getERXLogger(ERXCompilerProxy.class.getName()+".loading");

    /** Notification you can register to when the Compiler Proxy reloads classes.
     * <br/>
     * The object is an array of classes that did recompiled since the last time the notification was sent.
     */

    public static final String CompilerProxyDidCompileClassesNotification = "CompilerProxyDidCompileClasses";

    /** 
     * CPFileList is the file which describes in each line a path to java class to watch fo
     */
    protected static final String CPFileList = "CPFileList.txt";

    /** Path to the jikes binary.<br/>
     * Note that the Compilerproxy currently only works on unix based systems.
     */
    protected static final String _jikesPath = "/usr/bin/jikes";

    /** Holds the Compilerproxy singleton
     */
    protected static ERXCompilerProxy _defaultProxy;

    /**
     * Holds the classpath of the current app.
     */
    protected static String _classPath;

    /**
     * Holds a boolean that tells wether an error should raise an Exception or only log the error.
     */
    protected static boolean _raiseOnError = false;

    protected boolean initialized = false;
    
    /**
     * Holds the files to watch.
     */
    protected NSMutableDictionary _filesToWatch;
    protected String _className;
    
    /**
     * Holds the path where the compiled <code>.class</code> files go.
     * Default is <code>Contents/Resources/Java</code>.
     */
    protected String _destinationPath;
    
    /**
     * Currently compiled classes.
     */
    protected NSMutableSet classFiles = new NSMutableSet();

    protected NSArray _projectSearchPath;
    
    /** 
     * Returns the Compiler Proxy Singleton.<br/>
     * Creates one if needed.
     * 
     * @return compiler proxy singleton
     */
    public static ERXCompilerProxy defaultProxy() {
        if(_defaultProxy == null)
            _defaultProxy = new ERXCompilerProxy();
        return _defaultProxy;
    }
    public static void setDefaultProxy(ERXCompilerProxy p) {
        if(_defaultProxy != null) {
            NSNotificationCenter.defaultCenter().removeObserver(_defaultProxy);
        }
        _defaultProxy = p;
        _defaultProxy.initialize();
    }

    protected String pathForCPFileList(String directory) {
        return directory + File.separator + CPFileList;
    }
    
    /** 
     * Returns an array of paths to the opened projects that have a <code>CPFileList.txt</code>.<br/>
     * This code is pretty fragile and subject to changes between versions of the dev-tools.
     * You can get around it by setting <code>NSProjectSearchPath</code> to the paths to your projects:
     * <code>(/Users/ak/Wonder/Common/Frameworks,/Users/Work)</code>
     * will look for directories with a CPFileList.txt in all loaded bundles.
     * So when you link to <code>ERExtensions.framework</code>,
     * <code>/Users/ak/Wonder/Common/Frameworks/ERExtensions</code> will get found.
     * 
     * @return paths to opened projects
     */

    public String projectInSearchPath(String bundleName) {
        for(Enumeration e = _projectSearchPath.objectEnumerator(); e.hasMoreElements();) {
            String path = e.nextElement() + File.separator + bundleName;
            if(new File(pathForCPFileList(path)).exists()) {
                return path;
            }
        }
        return null;
    }

    protected NSArray projectPaths() {
	NSArray frameworkNames = (NSArray)NSBundle.frameworkBundles().valueForKey("name");
	NSMutableArray projectPaths = new NSMutableArray();
	String mainProject = null;
	String mainBundleName = NSBundle.mainBundle().name();

        WOProjectBundle mainBundle = WOProjectBundle.projectBundleForProject(mainBundleName,false);
        if(mainBundle == null) {
            mainProject = projectInSearchPath(mainBundleName);
            if(mainProject == null)
                mainProject = "../..";
        } else {
            mainProject = mainBundle.projectPath();
        }
	if((new File(pathForCPFileList(mainProject))).exists()) {
	    log.info("Found open project for app at path " + mainProject);
	    projectPaths.addObject(mainProject);
	}
	for(Enumeration e = frameworkNames.objectEnumerator(); e.hasMoreElements();) {
	    String name = (String)e.nextElement();
            WOProjectBundle bundle = WOProjectBundle.projectBundleForProject(name, true);
            String path;

            if(bundle != null) {
                path = bundle.projectPath();
            } else {
                path = projectInSearchPath(name);
            }
            if(path != null) {
                File f = new File(pathForCPFileList(path));
                if(f.exists()) {
                    log.info("Found open project for framework '" +name+ "' at path " + path);
                    projectPaths.addObject(path);
                }
            }
        }
	return projectPaths;
    }

    /** 
     * Returns the class registered for the name <code>className</code>.<br/>
     * Uses the private WebObjects class cache.
     * 
     * @param className class name
     * @return class for the registered name or null
     */
    public Class classForName(String className) {
        return com.webobjects.foundation._NSUtilities.classWithName(className);
    }

    /** 
     * Sets the class registered for the name <code>className</code> to the given class.<br/>
     * Changes the private WebObjects class cache.
     * 
     * @param clazz class object
     * @param className name for the class - normally clazz.getName()
     */
    public void setClassForName(Class clazz, String className) {
        com.webobjects.foundation._NSUtilities.setClassForName(clazz, className);
    }

    /** 
     * Initializes the CopilerProxy singleton.<br/>
     * Registers for ApplicationWillDispatchRequest notification.
     */
    public void initialize() {
	if (initialized) {
	    return;
	}
        log.info("initialize start");

        initialized = true;
        
        if(WOApplication.application()!=null && WOApplication.application().isCachingEnabled()) {
            log.info("I assume this is deployment mode, rapid-turnaround mode is disabled");
            _filesToWatch = new NSMutableDictionary();
            return;
        }

	if(!ERXProperties.booleanForKeyWithDefault("er.extensions.ERXCompilerProxyEnabled", false)) {
            log.info("Rapid-turnaround mode is disabled, set 'er.extensions.ERXCompilerProxyEnabled=true' in your WebObjects.properties to enable it.");
            _filesToWatch = new NSMutableDictionary();
            return;
	}


	//david teran: fixes a bug with some WebObjects version...
	_classPath = System.getProperty("com.webobjects.classpath");
	if ( _classPath != null && _classPath.length() > 0) {
	    if (System.getProperty("java.class.path") != null && System.getProperty("java.class.path").length() > 0) {
		System.setProperty("java.class.path", _classPath + ":" + System.getProperty("java.class.path"));
	    } else {
		System.setProperty("java.class.path", _classPath);
	    }
	    _classPath = System.getProperty("java.class.path");
	} else {
	    _classPath = System.getProperty("java.class.path");
	}
	//end of fix
	
        if(_classPath.indexOf("Classes/classes.jar") < 0) {
            // (ak) We need this when we do an Ant build, until WOProject is fixed to include classes.jar
            // This wouldn't work on windows of course, but then again, the rest of this class doesn't, too.
            String systemRoot = "/System/Library/Frameworks/JavaVM.framework/Classes/";
            _classPath += ":" + systemRoot + "classes.jar";
            _classPath += ":" + systemRoot + "ui.jar";
        }

        _raiseOnError = ERXProperties.booleanForKey("CPRaiseOnError");

	NSArray projectPaths = projectPaths();
        if(projectPaths.count() == 0) {
            log.info("No open projects found with a CPFileList.txt");
            _filesToWatch = new NSMutableDictionary();
	    return;
        }
        if(_classPath.indexOf(".woa") == -1) {
            log.info("Sorry, can't find the .woa wrapper of this application. There is no support for the CompilerProxy in servlet deployment, will try to get it via NSBundle.");
	    log.info("java.class.path="+_classPath);
        }

	NSBundle b = NSBundle.mainBundle();
	String path = b.resourcePath();
	if (path.indexOf(".woa") == -1) {
            log.info("Sorry, can't find the .woa wrapper of this application. There is no support for the CompilerProxy in servlet deployment.");
	    log.info("mainBundle.resourcePath="+path);
	}
	//_classPath = path;
	//_destinationPath = _classPath.substring(0,_classPath.indexOf(".woa")) + ".woa/Contents/Resources/Java/";
	_destinationPath = path.substring(0, path.indexOf(".woa")) + ".woa/Contents/Resources/Java/";
	_filesToWatch = new NSMutableDictionary();
	for(Enumeration e = projectPaths.objectEnumerator(); e.hasMoreElements();) {
	    String sourcePath = (String)e.nextElement();
	    String fileListPath = pathForCPFileList(sourcePath);
	    String fileList = _NSStringUtilities.stringFromFile(fileListPath, _NSStringUtilities.ASCII_ENCODING);

	    NSArray allFiles = NSArray.componentsSeparatedByString(fileList, "\n");
	    for(Enumeration sourceFiles = allFiles.objectEnumerator(); sourceFiles.hasMoreElements();) {
		String line = (String)sourceFiles.nextElement();
		String packageName = "";
		String sourceName = "";
                try {
                    NSArray items = NSArray.componentsSeparatedByString(line, ":");
                    if(items.count() > 1) {
                        sourceName = (String)items.objectAtIndex(0);
                        packageName = (String)items.objectAtIndex(1);
                        CacheEntry entry = new CacheEntry(sourcePath, sourceName, packageName);
                        _filesToWatch.setObjectForKey((Object)entry,entry.classNameWithPackage());
                        if(entry.classFile() != null)
                            classFiles.addObject(entry);
                        if(WOApplication.application().isDebuggingEnabled()) {
                            log.debug("fileToWatch:" + entry.classNameWithPackage());
                        }
                    }
                } catch (Exception ex) {
                    log.debug("initializeOnNotification: error parsing " +fileListPath+ " line '" + line +"':"+ ex);
		}
	    }
	}
	NSNotificationCenter.defaultCenter().addObserver(this, new NSSelector("checkAndCompileOnNotification", new Class[] { NSNotification.class } ), WOApplication.ApplicationWillDispatchRequestNotification, null);
        log.info("initialize end");
    }

    /** 
     * Contructor - does nothing special.
     */
    public ERXCompilerProxy() {
        _projectSearchPath = ERXProperties.arrayForKeyWithDefault("NSProjectSearchPath", NSArray.EmptyArray);
    }

    /** 
     * Method that will be called upon <code>ApplicationWillDispatchRequest</code>.<br/>
     * Checks if the request is not a resource request and then calls {$see checkAndCompileOnNotification()}
     * 
     * @param theNotification notification sent upon 
     *     ApplicationWillDispatchRequest
     */
    public void checkAndCompileOnNotification(NSNotification theNotification) {
        //log.debug("Received ApplicationWillDispatchRequestNotification");
        WORequest r = (WORequest)theNotification.object();
        String key = "/" + WOApplication.application().resourceRequestHandlerKey();
        // log.info(r.uri() + " - " + key);
        if(!(r.uri().indexOf(key) > 0))
            checkAndCompileAllClasses();
    }

    /** 
     * Main magic bullet routine.<br/>
     * 
     * You don't need to understand what it does, in fact you don't even want to...it will be certainly different tomorrow.
     */
    void checkAndCompileAllClasses() {
        CacheEntry cacheEntry;
        Enumeration e = _filesToWatch.objectEnumerator();
        NSMutableArray filesToCompile  = new NSMutableArray();
        while(e.hasMoreElements()) {
            cacheEntry = (CacheEntry)e.nextElement();
            if(cacheEntry.needsRefresh()) {
                filesToCompile.addObject(cacheEntry);
            }
        }
        if(filesToCompile.count() > 0) {
            Compiler compiler = new Compiler(filesToCompile.objects(), _destinationPath);
            if(compiler.compile()) {
                e = filesToCompile.objectEnumerator();
		log.debug("after compile: classFiles="+classFiles);
                while(e.hasMoreElements()) {
                    cacheEntry = (CacheEntry)e.nextElement();
                    classFiles.addObject(cacheEntry);
                }
                boolean didReset = false;
                boolean didResetModelGroup = false;
                CompilerClassLoader cl = null;
		log.debug("classFiles="+classFiles);
                e = classFiles.objectEnumerator();
                NSMutableDictionary kvcAccessors = new NSMutableDictionary();
                while(e.hasMoreElements()) {
                    cacheEntry = (CacheEntry)e.nextElement();
                    String className = cacheEntry.classNameWithPackage();
                    try {
                        if(cl == null)
			    cl = new CompilerClassLoader(_destinationPath, activeLoader);
                        //   Object o = Class.forName(className).newInstance();
                        Class class_ = cl.loadClass(className, true);

                        // the whole magic is in these lines
                        Class oldClass_ = classForName(cacheEntry.className());
                        setClassForName(class_, className);
                        if(oldClass_ != null && !cacheEntry.className().equals(className)) {
                            setClassForName(class_, cacheEntry.className());
                        }
                        if(!didReset) {
                            com.webobjects.appserver.WOApplication.application()._removeComponentDefinitionCacheContents();
                            NSKeyValueCoding.DefaultImplementation._flushCaches();
                            NSKeyValueCoding._ReflectionKeyBindingCreation._flushCaches();
                            NSKeyValueCoding.ValueAccessor._flushCaches();
                            didReset = true;
                        }

                        if(cacheEntry.packageName().length() > 0 && false) {
                            Object kvc = kvcAccessors.objectForKey(cacheEntry.packageName());
                            if(kvc == null) {
                                try {
                                    Class kvcAccessor = cl.reloadClass(cacheEntry.packageName() + ".KeyValueCodingProtectedAccessor");
                                    log.info("KVC for " + cacheEntry.packageName() + ": " + kvcAccessor);
                                    if(kvcAccessor != null) {
                                        log.info("Classloaders :" + kvcAccessor.getClassLoader() + " vs " + class_.getClassLoader());
                                        kvcAccessors.setObjectForKey(kvcAccessor, cacheEntry.packageName());
                                        NSKeyValueCoding.ValueAccessor.setProtectedAccessorForPackageNamed((NSKeyValueCoding.ValueAccessor)kvcAccessor.newInstance(), cacheEntry.packageName());
                                    } else {
                                        kvcAccessors.setObjectForKey("<no entry>", cacheEntry.packageName());
                                    }
                                } catch(Exception kvcException) {
                                    log.info("Error setting KVC accessor:" + kvcException);
                                }
                            }
                        }
                        
                        if(WODirectAction.class.isAssignableFrom(class_)) {
                            WOApplication app = WOApplication.application();
                            WORequestHandler currentDAHandler = app.requestHandlerForKey(app.directActionRequestHandlerKey());
                            WODirectActionRequestHandler handler = null;
                            if (currentDAHandler instanceof ERXDirectActionRequestHandler) 
                                handler = new ERXDirectActionRequestHandler(cacheEntry.className(), "default", false);
                            else 
                                handler = new WODirectActionRequestHandler(cacheEntry.className(), "default", false);
                            boolean directActionIsDefault = currentDAHandler == app.defaultRequestHandler();
                            app.registerRequestHandler(handler, app.directActionRequestHandlerKey());
                            if(directActionIsDefault)
                                app.setDefaultRequestHandler(handler);
                            log.info("WODirectAction loaded: "+ cacheEntry.className());
                        }
                        if(WOComponent.class.isAssignableFrom(class_)) {
                            WOContext context = new WOContext(new WORequest("GET", "/", "HTTP/1.1", null, null, null));
                            WOApplication.application().pageWithName(cacheEntry.className(), context)._componentDefinition().componentInstanceInContext(context); // mark an instance as locked
                        }
                        if(EOEnterpriseObject.class.isAssignableFrom(class_) && !didResetModelGroup) {
                            EOModelGroup.setDefaultGroup(ERXModelGroup.modelGroupForLoadedBundles());
                            didResetModelGroup = true;
                        }
                        if(ERXCompilerProxy.class.getName().equals(className)) {
                            try {
                                ERXCompilerProxy.setDefaultProxy((ERXCompilerProxy)class_.newInstance());
                            } catch(Exception ex) {
                                log.error("Can't reload compiler proxy", ex);
                            }
                        }
                        cacheEntry.update();
                        // sparkle dust ends here

                    } catch(ClassNotFoundException ex) {
                        throw new RuntimeException("Could not load the class "+ className + " with exception:" + ex.toString());
                    }
                }
                NSNotificationCenter.defaultCenter().postNotification(CompilerProxyDidCompileClassesNotification, classFiles);
            }
        }
    }

    /** 
     * Tests whether or not the class name with package is contained in the set of 
     * CacheEntry objects. Convenient to check if a specific class was recompiled 
     * in a CompilerProxyDidCompileClassesNotification. 
     *
     * @param classNameWithPackage   string of the class name
     * @param cacheEntries   NSSet contains CacheEntry objects; 
     *                       typically obtained by a notification's object() method 
     * @return if the classNameWithPackage is contained by cacheEntries
     */ 
    public static boolean isClassContainedBySet(String classNameWithPackage, NSSet cacheEntries) {
        boolean isContained = false;
        Enumeration e = cacheEntries.objectEnumerator();
        while (e.hasMoreElements()) {
            CacheEntry cacheEntry = (CacheEntry)e.nextElement();
            if (cacheEntry.classNameWithPackage().equals(classNameWithPackage)) {
                isContained = true;
                break;
            }
        }
        return isContained;
    }

    class CacheEntry {
        String _path;
        String _className;
        String _packageName;
        long _lastModified;
        File _sourceFile;
        File _classFile;

	/** @param basePath 
	 * @param path 
	 * @param packageName 
	 */
        public CacheEntry(String basePath, String path, String packageName) {
            _className = NSPathUtilities.lastPathComponent(path);
            _className = _className.substring(0,_className.indexOf("."));
            if(path.startsWith("/"))
                _path = path;
            else
                _path = basePath +"/"+ path;
            _sourceFile = new File(_path);
            _packageName = packageName;
            _lastModified = _sourceFile.lastModified();
        }

        public void update() {
            _lastModified = _sourceFile.lastModified();
        }

        public File classFile() {
            String fileName = _className.replace('.', File.separatorChar) + ".class";
            File f = new File( _destinationPath + File.separatorChar + fileName);
            if (f.exists() && f.isFile() && f.canRead()) {
                return f;
            }
            return null;
        }

        public String className() {
            return _className;
        }

        public String classNameWithPackage() {
            if(_packageName.length() == 0)
                return _className;
            return _packageName + "." + _className;
        }

        public String path() {
            return _path;
        }

        public String packageName() {
            return _packageName;
        }
        
        public boolean needsRefresh() {
            if(_sourceFile.exists()) {
                if(classFile() != null && classFile().lastModified() < _sourceFile.lastModified()) {
                    return true;
                }
                if(_lastModified < _sourceFile.lastModified()) {
                    return true;
                }
            }
            return false;
        }
        
        public void didRefresh() {
            _lastModified = _sourceFile.lastModified();
            log.info("Did refresh " + _path);
        }
        
        public String toString() {
            return "( className = '" + _className + "'; path = '" + _path + "';)\n";
        }
    }

    class Compiler {
        Object _files[];
        String _destinationPath;

        public Compiler(Object files[], String destinationPath) {
            _destinationPath = destinationPath;
            _files = files;
        }

        String[] commandArray() {
            String base[]  = new String[] { _jikesPath, "+E", "-g", "-d", _destinationPath, "-classpath", _destinationPath + ":" + _classPath};
            String commandLine [] =  new String [base.length+_files.length];
            for(int i = 0; i < base.length; i++ ) {
                commandLine[i] = base[i];
            }
            for(int i = base.length; i < base.length + _files.length; i++ ) {
                commandLine[i] = ((CacheEntry)_files[i-base.length]).path();
            }
            return commandLine;
        }

	String commandLine() {
            String base[]  = new String[] { _jikesPath, "+E", "-g", "-d", _destinationPath, "-classpath", _destinationPath + ":" + _classPath};
            String commandLine = "";

            for(int i = 0; i < base.length; i++ ) {
                commandLine = commandLine + " " + base[i];
            }
            for(int i = base.length; i < base.length + _files.length; i++ ) {
                commandLine = commandLine + " " + ((CacheEntry)_files[i-base.length]).path();
            }
            return commandLine;
        }

	public boolean compile() throws IllegalArgumentException {
            Process jikesProcess=null;
	    for (int i = 0; i < commandArray().length; i++) {
		log.debug("*** compiling:" + commandArray()[i]);
	    }
	    try {
                jikesProcess = Runtime.getRuntime().exec(commandArray());
                jikesProcess.waitFor();
                if (jikesProcess.exitValue() != 0) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(jikesProcess.getErrorStream()));
                    StringBuffer output = new StringBuffer();
                    String outputLine;
                    do {
                        outputLine = br.readLine();
                        if (outputLine != null && outputLine.length() > 0 )// && outputLine.startsWith("*** Caution"))
                            output.append(outputLine + "\n");
                    } while (outputLine != null);
                    if(_raiseOnError) {
                        throw new RuntimeException("Jikes returned an  error: " + output.toString());
                    } else {
                        log.error(output.toString());
                        jikesProcess.destroy();
                        return false;
                    }
                } else {
                    // We do nothing here because we except no output from jikes
                }
            } catch (IOException e) {
                log.error("Compiler: IOException: " + e.toString());
            } catch (InterruptedException e) {
                log.error("Compiler: Interrupted process: " + e.toString());
            } finally {
                if (jikesProcess != null)
                    jikesProcess.destroy();
            }
            return true;
        }
    }

    CompilerClassLoader activeLoader = null;
    
    class CompilerClassLoader extends ClassLoader {
        protected String _classpath;
        protected String _destinationPath;
        protected CompilerClassLoader _parent;
        
        public CompilerClassLoader(String destinationPath, CompilerClassLoader parent) {
            super();
            _destinationPath = destinationPath;
            _parent = parent;
            activeLoader = this;
	}

        public synchronized Class loadClass(String name, boolean resolveIt) throws ClassNotFoundException {
            try {
                return findClass(name);
            } catch(ClassNotFoundException ex) {
                if(_parent == null)
                    return super.loadClass(name, resolveIt);
                return _parent.loadClass(name, resolveIt);
            }
        }

        public File findClassFile(String name) {
            String fileName = name.replace('.', File.separatorChar) + ".class";
            File f = new File( _destinationPath + File.separatorChar + fileName);
            if (f.exists() && f.isFile() && f.canRead()) {
                classLoaderLog.debug("CompilerClassLoader.findClassFile:" + name + " found");
                return f;
            }
	    classLoaderLog.debug("CompilerClassLoader.findClassFile:" + _destinationPath + File.separatorChar + fileName + " NOT found");
            return null;
        }


	/** @param name 
	 * @exception ClassNotFoundException 
	 */
        protected Class findClass(String name) throws ClassNotFoundException {
	    //fix by david teran, cluster9
	    //this checks if the name from the class is one of the files that were
	    //compiled. if not, then this findClass method will fail, we must use the
	    //normal class loader to find the class.
            boolean contains = false;
            Enumeration e = classFiles.objectEnumerator();
            while (e.hasMoreElements()) {
                CacheEntry ce = (CacheEntry)e.nextElement();
                if (name.startsWith(ce.classNameWithPackage())) {
                    classLoaderLog.debug("CompilerClassLoader.findClass:" + name + " is in array");
                    contains = true;
                    break;
                }
            }
            
	    if (contains) {
		byte buffer[] = null;
		Class newClass;
		File classFile = findClassFile(name);
		if (classFile == null) {
		    throw new ClassNotFoundException(name);
		}
		try {
		    FileInputStream in = new FileInputStream(classFile);
		    int length = in.available();
		    if (length == 0) {
			throw new ClassNotFoundException(name);
		    }
		    buffer = new byte[length];
		    in.read(buffer);
		    in.close();
		} catch (IOException iox) {
		    classLoaderLog.debug(iox);
		    throw new ClassNotFoundException(iox.getMessage());
		}
		try {
		    newClass = defineClass(name, buffer, 0, buffer.length);
		    classLoaderLog.info("Did load class: " + name);
		} catch (Throwable t) {
		    classLoaderLog.debug(t);
		    throw new RuntimeException(t.getMessage());
		}
		return newClass;
            } else {
                //this class is a class that must be accessable with the normal classloader
                //it cannot be found in the filesystem because it was not compiled by CP
                Class clazz = null;
                clazz = Class.forName(name);

                if (clazz != null) {
		    return clazz;
		} else {
		    throw new IllegalStateException("could not get class "+name+" with classForName method!");
		}
	    }
	}
	/** @param name 
	 */
        public URL getResource(String name) {
            return ClassLoader.getSystemResource(name);
        }
        /** Tries to re-load the given class into the current class loader. It is used to push the KeyValueCodingProtectedAccessor class back into the system.*/
        public Class reloadClass(String className) {
            Class c = null;
            try {
                byte buffer[] = null;
                String fileName = className.replace('.', '/') + ".class";
                InputStream in = null;
                File classFile = findClassFile(className);
                if(classFile != null) {
                    in = new FileInputStream(classFile);
                } else {
                    in = getResource(fileName).openStream();
                }
                int length = in.available();
                if (length != 0) {
                    buffer = new byte[length];
                    in.read(buffer);
                    in.close();
                }
                if(buffer != null) {
                    log.info(className + ":" + buffer.length + "," + buffer);
                    c = defineClass(className, buffer, 0, buffer.length);
                }
            } catch (Throwable t) {
                log.warn("Error relaoding class "+className+": " + t);
            }
            return c;
        }

    }


    private static URL[] mangleClasspathForClassLoader(String s) {
	String s1 = File.pathSeparatorChar != ';' ? "file://" : "file:///";
	NSArray paths = NSArray.componentsSeparatedByString(_classPath, ":");
	URL aurl[] = new URL[paths.count()];
	for(int i = 0; i < paths.count(); i++) {
	    try {
		aurl[i] = new URL(s1 + paths.objectAtIndex(i).toString());
	    }
	    catch(Throwable throwable) {
		throw new RuntimeException("Error creating URL " + throwable);
	    }
	}

	return aurl;
    }
}
