/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.log4j.Logger;

import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import er.extensions.*;

/**
 * Overhaul of the caching system.
 */
public class ERD2WModel extends D2WModel {

    /** logging support */
    public static final Logger log = Logger.getLogger(ERD2WModel.class);

    /** logs rules being decoded */
    public static final Logger ruleDecodeLog = Logger.getLogger("er.directtoweb.rules.decode");

    /** main category for enabling or disabling tracing of rules */
    public static final Logger ruleTraceEnabledLog = Logger.getLogger("er.directtoweb.rules.ERD2WTraceRuleFiringEnabled");

    //	===========================================================================
    //	Notification Title(s)
    //	---------------------------------------------------------------------------    
    
    // Register for this notification to have the hook in place to load non-d2wmodel based rules
    public static final String WillSortRules = "WillSortRules";
    public static final String ModelWillReset = "ModelWillReset";

    /** null refernced used to represent null in the caching system */
    private final static Object NULL_VALUE="<NULL>";
    
    private Hashtable _cache=new Hashtable(10000);
    private Hashtable _systemCache=new Hashtable(10000);
    private Hashtable _significantKeysPerKey=new Hashtable(500);

    private static D2WModel _defaultModel;
    
    // put here the keys than can either provided as input or computed
    // FIXME should add API from clients to add to this array
    static NSMutableArray BACKSTOP_KEYS=new NSMutableArray(new Object[] { "pageConfiguration", "entity", "task" });
    static {
        Class c=D2WFastModel.class; // force initialization
        _defaultModel = new ERD2WModel(NSArray.EmptyArray);
        D2WModel.setDefaultModel(_defaultModel);
    }

    /**
     * Gets the default D2W model cast as an ERD2WModel.
     * @return the default ERD2Model
     */
    public static ERD2WModel erDefaultModel() {
        if(!(D2WModel.defaultModel() instanceof ERD2WModel)) {
            D2WModel.setDefaultModel(_defaultModel);
            log.warn("erDefaultModel had wrong class, fixing to ERD2WModel");
        }
        return (ERD2WModel)D2WModel.defaultModel();
    }

    private final static EOSortOrdering _prioritySortOrdering=new EOSortOrdering("priority",EOSortOrdering.CompareDescending);
    private final static EOSortOrdering _descriptionSortOrdering=new EOSortOrdering("toString",EOSortOrdering.CompareDescending);
    private static NSArray ruleSortOrderingKeyArray() {
        NSMutableArray result=new NSMutableArray();
        result.addObject(_prioritySortOrdering);
        result.addObject(_descriptionSortOrdering);

        return result;
    }
    private final static NSArray _ruleSortOrderingKeyVector=ruleSortOrderingKeyArray();

    /**
     * Main constructor. Builds a model for a given
     * set of rules.
     * @param rules array of rules
     */
    protected ERD2WModel(NSArray rules) {
    	super(rules);
    	NSNotificationCenter.defaultCenter().addObserver(this, 
    			ERXSelectorUtilities.notificationSelector("applicationDidFinishLaunching"), 
    			WOApplication.ApplicationDidFinishLaunchingNotification, null);
    }
    protected ERD2WModel(File file) {
        super(file);
    }
    protected ERD2WModel(EOKeyValueUnarchiver unarchiver) {
        super(unarchiver);
    }
    
    public void clearD2WRuleCache() {
        invalidateCaches();
        sortRules();
    }
    
    protected void sortRules() {
        // This allows other non-d2wmodel file based rules to be loaded.
        // but we only post for the main model
        if(D2WModel.defaultModel() == this) {
            log.debug("posting WillSortRules.");
            NSNotificationCenter.defaultCenter().postNotification(WillSortRules, this);
            log.debug("posted WillSortRules.");
        }
        // We don't want dynamically loaded rules to cause rapid-turnaround to not work.
        setDirty(false);
        super.sortRules();
        log.debug("called super sortRules.");
        /*
         the following sort call was to attempt to make assistant generated files more CVS compatible
         by preserving the rule order better. Commenting out since it's very memory hungry (calling description on every rule)
         and we are not using the Assistant
        EOSortOrdering.sortArrayUsingKeyOrderArray((NSMutableArray)rules(),
                                                   _ruleSortOrderingKeyVector);
        log.debug("Finished sorting.");
        */
        if (rules() !=null && rules().count() > 0) prepareDataStructures();
    }
    
    public void applicationWillDispatchRequest(NSNotification n) {
    	checkRules();
    }

    public void applicationDidFinishLaunching(NSNotification n) {
    	if(!WOApplication.application().isCachingEnabled()) {
    		NSNotificationCenter.defaultCenter().addObserver(this, 
    				ERXSelectorUtilities.notificationSelector("applicationWillDispatchRequest"), 
    				WOApplication.ApplicationWillDispatchRequestNotification, null);
    	}
    }
    
    public NSArray rules() {
        return super.rules();
    }

    public void addRule(Rule rule) {
        super.addRule(rule);
    }

    public void removeRule(Rule rule) {
        super.removeRule(rule);
    }
    
    protected String descriptionForRuleSet(NSArray set) {
        StringBuffer buffer = new StringBuffer();
        for (Enumeration e = set.objectEnumerator(); e.hasMoreElements();)
            buffer.append("\t" + descriptionForRule((Rule)e.nextElement()) + "\n");
        return buffer.toString();
    }

    protected String descriptionForRule(Rule r) {
        String suffix = null;
        if (_filePathRuleTraceCache != null) {
            suffix = (String)_filePathRuleTraceCache.get(r);
            if (suffix == null) suffix = "Dynamic";
        }
        return r.toString() + (suffix != null ? " From: " + suffix : "");
    }

    protected Hashtable _filePathRuleTraceCache;
    public void addRules(NSArray rules) {
        super.addRules(rules);
        if (!WOApplication.application().isCachingEnabled() && currentFile() != null) {
            String path=currentFile().getAbsolutePath();
            NSArray components = NSArray.componentsSeparatedByString(path, "/");
            int count=components.count();
            String filePath = count > 2 ?
                (String)components.objectAtIndex(count-3)+"/"+ (String)components.objectAtIndex(count-2) :
                path;
            if (_filePathRuleTraceCache == null)
                _filePathRuleTraceCache = new Hashtable();
            for (Enumeration e = rules.objectEnumerator(); e.hasMoreElements();) {
                _filePathRuleTraceCache.put(e.nextElement(), filePath);
            }
        }
    }

    protected Object fireSystemRuleForKeyPathInContext(String keyPath, D2WContext context) {
        return fireRuleForKeyPathInContext(_systemCache, keyPath,context);
    }

    protected Object fireRuleForKeyPathInContext(String keyPath, D2WContext context) {
        return fireRuleForKeyPathInContext(_cache, keyPath, context);
    }
    
    private Object fireRuleForKeyPathInContext(Map cache, String keyPath, D2WContext context) {
        String[] significantKeys=(String[])_significantKeysPerKey.get(keyPath);
        if (significantKeys==null) return null;
        short s=(short)significantKeys.length;
        ERXMultiKey k=new ERXMultiKey((short)(s+1));
        Object[] lhsKeys=k.keys();
        for (short i=0; i<s; i++) {
            //lhsKeys[i]=context.valueForKeyPathNoInference(significantKeys[i]);
            lhsKeys[i]=ERD2WUtilities.contextValueForKeyNoInferenceNoException(context, significantKeys[i]);
        } 
        lhsKeys[s]=keyPath;
        Object result=cache.get(k);
        if (result==null) {
            boolean resetTraceRuleFiring = false;
            Logger ruleFireLog=null;
            if (ruleTraceEnabledLog.isDebugEnabled()) {
                Logger ruleCandidatesLog = Logger.getLogger("er.directtoweb.rules." + keyPath + ".candidates");
                ruleFireLog = Logger.getLogger("er.directtoweb.rules." + keyPath + ".fire");
                if (ruleFireLog.isDebugEnabled() && !NSLog.debugLoggingAllowedForGroups(NSLog.DebugGroupRules)) {
                    NSLog.allowDebugLoggingForGroups(NSLog.DebugGroupRules);
                    //NSLog.setAllowedDebugLevel(NSLog.DebugLevelDetailed);
                    resetTraceRuleFiring = true;
                }
                if (ruleCandidatesLog.isDebugEnabled()) {
                    ruleFireLog.debug("CANDIDATES for keyPath: " + keyPath + "\n" +
                                      descriptionForRuleSet(canidateRuleSetForRHSInContext(keyPath, context)));
                }
            }
            if(cache == _systemCache) {
                result=super.fireSystemRuleForKeyPathInContext(keyPath,context);
            } else {
                result=super.fireRuleForKeyPathInContext(keyPath,context);
            }
            cache.put(k,result==null ? NULL_VALUE : result);
            if (ruleTraceEnabledLog.isDebugEnabled()) {
                if (ruleFireLog.isDebugEnabled())
                ruleFireLog.debug("FIRE: " +keyPath +  " depends on: "  + new NSArray(significantKeys) + " = " + k
                              + " value: " + (result==null ? "<NULL>" : (result instanceof EOEntity ? ((EOEntity)result).name() : result)));
            }
            if (resetTraceRuleFiring) {
                NSLog.refuseDebugLoggingForGroups(NSLog.DebugGroupRules);
            }
        } else {
            if (ruleTraceEnabledLog.isDebugEnabled()) {
                Logger ruleLog = Logger.getLogger("er.directtoweb.rules." + keyPath + ".cache");
                if (ruleLog.isDebugEnabled())
                    ruleLog.debug("CACHE: " + keyPath +  " depends on: "  + new NSArray(significantKeys) + " = " + k
                                  + " value: " + (result==NULL_VALUE ? "<NULL>" : (result instanceof EOEntity ? ((EOEntity)result).name() : result)));
            }
            if (result==NULL_VALUE)
                result=null;
        }
        if (result != null && result instanceof ERDDelayedAssignment) {
            result=((ERDDelayedAssignment)result).fireNow(context);
        }
        return result;
    }

    /** Means to dump the cache. You shouldn't use this unless you know what you are doing. */
    public void dumpCache(String fileName) {
        fileName = fileName == null ? "dmp.cache": fileName;
        synchronized(this) {
            try {
                ERXFileUtilities.writeInputStreamToFile(new ByteArrayInputStream(cacheToBytes(_cache)), new File(fileName));
            } catch(IOException ex) {
                log.error(ex);
            }
        }
    }

    /** Means to restore the cache. You shouldn't use this unless you know what you are doing. */
    public void restoreCache(String fileName) {
        fileName = fileName == null ? "dmp.cache": fileName;
        synchronized(this) {
            try {
                _cache = cacheFromBytes(ERXFileUtilities.bytesFromFile(new File(fileName)));
            } catch(IOException ex) {
                log.error(ex);
            }
        }
    }
    
    public NSArray canidateRuleSetForRHSInContext(String rhs, D2WContext context) {
        NSMutableSet canidateSet = new NSMutableSet();
        for (Enumeration e = rules().objectEnumerator(); e.hasMoreElements();) {
            Rule r = (Rule)e.nextElement();
            if (r.rhsKeyPath().equals(rhs) && r.canFireInContext(context))
                canidateSet.addObject(r);
        }
        return canidateSet.count() == 0 ? canidateSet.allObjects() :
            EOSortOrdering.sortedArrayUsingKeyOrderArray(canidateSet.allObjects(), ruleSortOrderingKeyArray());
    }


    static class _LhsKeysCallback extends ERDQualifierTraversalCallback {
        public NSMutableArray keys=new NSMutableArray();
        public boolean traverseKeyValueQualifier (EOKeyValueQualifier q) {
            if (!keys.containsObject(q.key()))
                keys.addObject(q.key());
            return true;
        }
        public boolean traverseKeyComparisonQualifier (EOKeyComparisonQualifier q) {
            if (!keys.containsObject(q.leftKey()))
                keys.addObject(q.leftKey());
            if (!keys.containsObject(q.rightKey()))
                keys.addObject(q.rightKey());
            return true;
        }
    }

    static void addKeyToVector(String key, Vector vector) {
        if (key.indexOf(".")!=-1) {
            // we only take the first atom, unless it's object or session
            NSArray a=NSArray.componentsSeparatedByString(key,".");
            String firstAtom=(String)a.objectAtIndex(0);
            if (!firstAtom.equals("object") && !firstAtom.equals("session"))
                key=firstAtom;
        }
        if (!vector.contains(key))
            vector.addElement(key);
    }

    public void prepareDataStructures() {
        log.debug("prepareDataStructures");
        boolean localizationEnabled = ERXLocalizer.isLocalizationEnabled();
        // is a dictionary which will contain for each rhs key, which other keys it depends
        // on, for single rule hops
        Hashtable dependendKeysPerKey=new Hashtable();
        // here we put per key the keys which this key depends on, when computed by a delayed
        // assignment. When this is the case, we only need to add those keys to the main dictionary
        // when the rhs key itself shows up on the lhs
        Hashtable delayedDependendKeysPerKey=new Hashtable();
        _LhsKeysCallback c=new _LhsKeysCallback();

        // we first put all those implicit depedencies introduced by the computedKey business
        Vector v=new Vector();
        v.addElement("propertyKey");
        dependendKeysPerKey.put(D2WModel.PropertyIsKeyPathKey,v.clone());
        v.addElement("entity");
        dependendKeysPerKey.put(D2WModel.RelationshipKey,v.clone());
        dependendKeysPerKey.put(D2WModel.AttributeKey,v.clone());
        dependendKeysPerKey.put(D2WModel.PropertyTypeKey,v.clone());
        dependendKeysPerKey.put(D2WModel.PropertyKeyPortionInModelKey,v.clone());

        // then enumerate through all the rules; h 
        for (Enumeration e=rules().objectEnumerator(); e.hasMoreElements();) {
            Rule r=(Rule)e.nextElement();
            String rhsKey=r.rhs().keyPath();
            Vector dependendantKeys=(Vector)dependendKeysPerKey.get(rhsKey);
            if (dependendantKeys==null) {
                dependendantKeys=new Vector();
                dependendKeysPerKey.put(rhsKey,dependendantKeys);
            }
            ERDQualifierTraversal.traverseQualifier(r.lhs(),c);
            for (Enumeration e2=c.keys.objectEnumerator(); e2.hasMoreElements(); ) {
                String k=(String)e2.nextElement();
                addKeyToVector(k,dependendantKeys);
            }
            // also add those from the assignment
            // if the assignment is delayed, do not add them here;
            // they only need to be added if the key that the assignment computes is itself used
            // on the left hand side
            if (r.rhs() instanceof ERDComputingAssignmentInterface) {
                Vector recipientForNewKeys=dependendantKeys;
                if (r.rhs() instanceof ERDDelayedAssignment) {
                    // put those keys away, needed when reducing the graph and
                    recipientForNewKeys=(Vector)delayedDependendKeysPerKey.get(rhsKey);
                    if (recipientForNewKeys ==null) {
                        recipientForNewKeys =new Vector();
                        delayedDependendKeysPerKey.put(rhsKey, recipientForNewKeys);
                    }                    
                }
                NSArray extraKeys=((ERDComputingAssignmentInterface)r.rhs()).dependentKeys(rhsKey);
                if (extraKeys!=null) {
                    for (Enumeration e6=extraKeys.objectEnumerator(); e6.hasMoreElements(); ) {
                        String k=(String)e6.nextElement();
                        addKeyToVector(k, recipientForNewKeys);
                    }
                }
            } else if (r.rhs() instanceof DefaultAssignment) {
                // special treatment for the only custom assignment coming for the D2W default rule set
                // since it does not implement ERDComputingAssignmentInterface, we add the required keys explicitely here
                // another way to do this would be to introduce a rule with the required keys in their LHS, but that is
                // quite a few rules and this is a bit more self contained
                addKeyToVector("task", dependendantKeys);
                addKeyToVector("entity", dependendantKeys);
                addKeyToVector("propertyKey", dependendantKeys);
            }
            if(localizationEnabled && r.rhs() instanceof ERDLocalizableAssignmentInterface) {
                addKeyToVector("session.language", dependendantKeys);
            }
            c.keys=new NSMutableArray();
        }
        // we then reduce the graph
        log.debug("reducing graph");
        boolean touched=true;
        while (touched) {
            touched=false;
            for (Enumeration e3=dependendKeysPerKey.keys(); e3.hasMoreElements();) {
                String rk=(String)e3.nextElement();
                Vector keys=(Vector)dependendKeysPerKey.get(rk);
                for (Enumeration e4=keys.elements(); e4.hasMoreElements();) {
                    String k=(String)e4.nextElement();
                    if (!BACKSTOP_KEYS.containsObject(k)) {
                        Vector newKeys=(Vector)dependendKeysPerKey.get(k);
                        Vector keyFromDelayedAssignment=(Vector)delayedDependendKeysPerKey.get(k);                        
                        if (newKeys!=null || keyFromDelayedAssignment!=null) {
                            keys.removeElement(k);
                            touched=true;
                            if (newKeys!=null) {
                                for (Enumeration e5=newKeys.elements(); e5.hasMoreElements();) {
                                    String s=(String)e5.nextElement();
                                    addKeyToVector(s, keys);
                                }
                            }
                            if (keyFromDelayedAssignment!=null) {
                                for (Enumeration e5=keyFromDelayedAssignment.elements(); e5.hasMoreElements();) {
                                    String s=(String)e5.nextElement();
                                    addKeyToVector(s, keys);
                                }
                            }                            
                        }
                    }
                }
            }
        }
        // transfer all this into
        for (Enumeration e7=dependendKeysPerKey.keys(); e7.hasMoreElements(); ) {
            String key=(String)e7.nextElement();
            Vector keys=(Vector)dependendKeysPerKey.get(key);
            if (log.isDebugEnabled()) log.debug("Rhs key "+key+" <-- " + keys);
            String[] a=new String[keys.size()];
            for (int i=0; i<keys.size();i++) a[i]=(String)keys.elementAt(i);
            if(_significantKeysPerKey != null)
                _significantKeysPerKey.put(key,a);
        }
    }

    protected void invalidateCaches() {
        log.debug("Invalidating cache");
        if (_cache!=null)
            _cache.clear();
        if (_systemCache!=null)
            _systemCache.clear();
        if (_significantKeysPerKey!=null)
            _significantKeysPerKey.clear();
        super.invalidateCaches();
    }

    public void resetModel() {
        log.info("Resetting Model");
        if (_filePathRuleTraceCache!=null)
            _filePathRuleTraceCache.clear();
        NSNotificationCenter.defaultCenter().postNotification(ModelWillReset, this);
        setRules(new NSArray());
        initializeClientConfiguration();
        loadRules();
        //invalidateCaches();
        //sortRules();
    }

    protected File _currentFile;
    protected void setCurrentFile(File currentFile) { _currentFile = currentFile; }
    protected File currentFile() { return _currentFile; }

    /**
     * Rule class that works around two problems:
     * <ul>
     * <li>when you have an assignment class that is not present in the classpath
     * then the model will not load, making for very strange errors. We replace the
     * missing class with the normal assignment class and log the error.
     * <li>when evaluating rule priorities, the default is to place rules containing <code>pageConfiguration</code>
     * keys so high up that they will get prefered over rules without such a condition, but with a higher author setting.
     * This is pretty ridiculous and leads to having to set <code>... AND (pageConfigurstion like '*')</code> 
     * in all the conditions.<br>
     * We place rules with a <code>pageConfiguration</code> so high that they will be higher than rules with the same author setting
     * but lower than a rule with a higher setting.
     * </ul>
     * <br>In order to be usable with the D2WClient and Rule editor, we also patch the encoded 
     * dictionary so these tools find no trace of the patched rules.
     * @author ak
     */
    public static class _PatchedRule extends Rule {
        private int _priority = -1;
        private String _assignmentClassName;
      
        public _PatchedRule() {
            super();
        }
 
        public _PatchedRule(EOKeyValueUnarchiver eokeyvalueunarchiver) {
            super(eokeyvalueunarchiver.decodeIntForKey("author"),
                    ((EOQualifier) eokeyvalueunarchiver.decodeObjectForKey("lhs")),
                    ((Assignment) eokeyvalueunarchiver.decodeObjectForKey("rhs")));
            _assignmentClassName = (String)eokeyvalueunarchiver.decodeObjectForKey("assignmentClassName");
        }
        
        public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver eokeyvalueunarchiver) {
            _PatchedRule rule = null;
            try {
                rule = new _PatchedRule(eokeyvalueunarchiver);
            } catch(Throwable t) {
                // AK: this occurs mostly when we want to load a rule that contains an assigment class which can't be found
                //HACK cheesy way to get at the encoded rule dictionary
                NSMutableDictionary dict = (NSMutableDictionary)NSKeyValueCoding.Utility.valueForKey(eokeyvalueunarchiver,"propertyList");
                String ruleString = dict.toString();
                // now store the old assignment class
                dict.takeValueForKeyPath(dict.valueForKeyPath("rhs.class"), "assignmentClassName");
                // and push in the default class
                dict.takeValueForKeyPath(Assignment.class.getName(), "rhs.class");
                // try again
                try {
                    rule = new _PatchedRule(eokeyvalueunarchiver);
                    ruleString = rule.toString();
                    
                } finally {
                    log.error("Problems with this rule: \n" +  t.getMessage() + "\n" + ruleString);
                }
            }
            return rule;
        }
        
        /**
         * Overridden to patch the normal rule class name into the generated dictionary.
         * @see com.webobjects.eocontrol.EOKeyValueArchiving#encodeWithKeyValueArchiver(com.webobjects.eocontrol.EOKeyValueArchiver)
         */
        public void encodeWithKeyValueArchiver (EOKeyValueArchiver eokeyvaluearchiver) {
            super.encodeWithKeyValueArchiver(eokeyvaluearchiver);
            ((NSMutableDictionary)eokeyvaluearchiver.dictionary()).setObjectForKey(Rule.class.getName(), "class");
        }
        
        /**
         * Overridden to work around 
         * @see com.webobjects.directtoweb.Rule#priority()
         */
        public int priority() {
            if(_priority == -1) {
                
                EOQualifier lhs = lhs();
                String lhsString = "";
                               
                _priority = 1000 * author();

                if(lhs != null) {
                    lhsString = lhs.toString();
                    if(lhsString.indexOf("dummyTrue") == -1) {
                        if(lhsString.indexOf("pageConfiguration") != -1) {
                            _priority += 500;
                        }
                        if(lhs() instanceof EOAndQualifier) {
                            _priority += ((EOAndQualifier)lhs()).qualifiers().count();
                        } else {
                            _priority ++;
                        }
                    }
                }
            }
            return _priority;
        }

        public String assignmentClassName() {
            if(_assignmentClassName == null) {
                _assignmentClassName = rhs().getClass().getName();
            }
            return _assignmentClassName;
        }
        
        public _PatchedRule cloneRule() {
            EOKeyValueArchiver archiver = new EOKeyValueArchiver();
            encodeWithKeyValueArchiver(archiver);
            EOKeyValueUnarchiver unarchiver = new EOKeyValueUnarchiver(archiver.dictionary());
            
            return new _PatchedRule(unarchiver);
        }
        
        /**
         * Builds a string like:<br>
         * <pre><code>   100: ((entity.name = 'Bug') and (task = 'edit')) => isInspectable = true [com.directtowen.BooleanAssignment]</code></pre>
         * @return a nice description of the rule
         */
        public String toString() {
            String prefix = "      ";
            String authorString = "" + author();
            String rhsClass = assignmentClassName();
            return (
                    prefix.substring(0, prefix.length() - ("" + author()).length()) + author() + " : " + 
                    (lhs() == null ? "*true*" : lhs().toString()) +
                    " => " +
                    (rhs() == null ? "<NULL>" : rhs().keyPath() + " = " + rhs().value() +
                            ( rhsClass.equals(Assignment.class.getName()) ? "" : " [" + rhsClass + "]")
                    ));
        }
    }
    
    protected static NSDictionary dictionaryFromFile(File file) {
        NSDictionary model = null;
        try {
            log.info("Loading file: " + file);
            if(file != null && !file.isDirectory() && file.exists()) {
                model = Services.dictionaryFromFile(file);
                NSArray rules = (NSArray)model.objectForKey("rules");
                Enumeration e = rules.objectEnumerator();
                boolean patchRules = ERXProperties.booleanForKeyWithDefault("er.directtoweb.ERXD2WModel.patchRules", true);
                while(e.hasMoreElements()) {
                    NSMutableDictionary dict = (NSMutableDictionary)e.nextElement();
                    if(patchRules) {
                        if(Rule.class.getName().equals(dict.objectForKey("class"))) {
                            dict.setObjectForKey(_PatchedRule.class.getName(), "class");
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            NSLog.err.appendln("****** DirectToWeb: Problem reading file "
                               + file + " reason:" + throwable);
            if (NSLog.debugLoggingAllowedForLevelAndGroups(1, 40L)) {
                NSLog.err.appendln("STACKTRACE:");
                NSLog.err.appendln(throwable);
            }
            throw NSForwardException._runtimeExceptionForThrowable(throwable);
        }
        return model;
    }
   
    protected void mergePathURL(URL modelURL) {
        if(modelURL != null) {

            File modelFile = new File(modelURL.getFile());
            if (log.isDebugEnabled()) log.debug("Merging rule file \"" + modelFile.getPath()
                                                + "\"");
            setCurrentFile(modelFile);
            NSDictionary dic = dictionaryFromFile(modelFile);
            if(dic != null) {
                if (ruleDecodeLog.isDebugEnabled()) {
                    ruleDecodeLog.debug("Got dictionary for file: " + modelFile + "\n\n");
                    for (Enumeration e = ((NSArray)dic.objectForKey("rules")).objectEnumerator(); e.hasMoreElements();) {
                        NSDictionary aRule = (NSDictionary)e.nextElement();
                        NSMutableDictionary aRuleDictionary = new NSMutableDictionary(aRule, "rule");
                        EOKeyValueUnarchiver archiver = new EOKeyValueUnarchiver(aRuleDictionary);
                        try {
                            addRule((Rule)archiver.decodeObjectForKey("rule"));
                        } catch (Exception ex) {
                            ruleDecodeLog.error("Bad rule: " + aRule);
                        }
                    }
                } else {
                    ERD2WModel model = new ERD2WModel(new EOKeyValueUnarchiver(dic));
                    addRules(model.rules());
                }
            }
            setDirty(false);
        }
        setCurrentFile(null);
    }

    protected void mergeFile(File modelFile) {
        mergePathURL(ERXFileUtilities.URLFromFile(modelFile));
    }
    
    protected Hashtable _uniqueAssignments = new Hashtable();
    protected void uniqueRuleAssignments(NSArray rules) {
        if (rules != null && rules.count() > 0) {
            int uniquedRules = 0;
            if (log.isDebugEnabled()) log.debug("Starting Assignment uniquing for " + rules.count() + " rules");
            //Vector uniqueAssignments = new Vector();
            //Hashtable _uniqueAssignments=new Hashtable();
            for (int c = 0; c < rules.count() - 1; c++) {
                //if (c % 100 == 0)
                //    log.debug("Out of : " + c + " rules, duplicates: " + uniquedRules);
                Rule r = (Rule)rules.objectAtIndex(c);
                if (r != null && r.rhs() != null) {
                    Vector v = (Vector)_uniqueAssignments.get(r.rhs().keyPath());
                    if (v != null) {
                        Assignment unique = assignmentContainedInVector(r.rhs(), v);
                        if (unique == null) {
                            v.addElement(r.rhs());
                        } else if (!(unique == r.rhs())) {
                            r.setRhs(unique);
                            uniquedRules++;
                        }
                    } else {
                        Vector m = new Vector();
                        m.addElement(r.rhs());
                        _uniqueAssignments.put(r.rhs().keyPath(), m);
                    }
                } else {
                    log.warn("Rule is null: " + r + " or rhs: " + (r != null ? r.rhs() : null));
                }
            }
            //h = null;
            //if (uniquedRules > 0)
            //    ERXExtensions.forceGC(0);
            if (log.isDebugEnabled())
                log.debug("Finished Assignment uniquing, got rid of " + uniquedRules + " duplicate assignment(s)");
        }
    }

    protected Assignment assignmentContainedInVector(Assignment a1, Vector v) {
        Assignment containedAssignment = null;
        for (Enumeration e = v.elements(); e.hasMoreElements();) {
            Assignment a2 = (Assignment)e.nextElement();
            if (ERD2WUtilities.assignmentsAreEqual(a1, a2)) {
                containedAssignment = a2; break;
            }
        }
        return containedAssignment;
    }

    protected int uniquedQualifiers = 0;
    protected int totalQualifiers = 0;
    protected void uniqueQualifiers(NSArray rules) {
        if (rules != null && rules.count() > 0) {
            uniquedQualifiers = 0;
            totalQualifiers = 0;
            int replacedQualifiers = 0;
            if (log.isDebugEnabled()) log.debug("Starting Qualifier uniquing for " + rules.count() + " rules");
            //Vector uniqueAssignments = new Vector();
            //Hashtable _uniqueAssignments=new Hashtable();
            for (int c = 0; c < rules.count() - 1; c++) {
                if (c % 100 == 0)
                    log.debug("Out of : " + c + " rules, qualifiers: " + totalQualifiers + " duplicates: "
                              + uniquedQualifiers + " replaced: " + replacedQualifiers);
                Rule r = (Rule)rules.objectAtIndex(c);
                if (r != null && r.lhs() != null) {
                    EOQualifierEvaluation q = r.lhs();
                    try {
                        EOQualifier cache = qualifierInCache((EOQualifier)q);
                        if (cache != null && cache != q) {
                            r.setLhs((EOQualifier)cache);
                            //r.setLhs((EOQualifierEvaluation)cache);
                            replacedQualifiers++;
                            //uniquedQualifiers++;
                        }
                    } catch (NullPointerException npe) {
                        log.warn("Caught NPE for rule: " + r);
                    }
                }
            }
            flushUniqueCache();
            if (uniquedQualifiers > 0)
                ERXExtensions.forceGC(0);
            if (log.isDebugEnabled()) log.debug("Finished Qualifier uniquing, for: " + totalQualifiers
                                                + " got rid of " + uniquedQualifiers + " duplicate qualifiers, replaced: " + replacedQualifiers);
        }
    }

    private boolean _hasAddedExtraModelFile=false;
    public Vector modelFilesInBundles () {
        Vector modelFiles = super.modelFilesInBundles();
        if (!_hasAddedExtraModelFile) {
            String extraModelFilePath = System.getProperty("ERExtraD2WModelFile");
            // it appears super cache's the Vector, so only add the extraModelFile if we haven't already done it
            if (extraModelFilePath != null) {
                if (log.isDebugEnabled()) log.debug("ERExtraD2WModelFile = \"" + extraModelFilePath + "\"");
                File extraModelFile = new java.io.File(extraModelFilePath);
                if (extraModelFile.exists() && extraModelFile.isFile() && extraModelFile.canRead()) {
                    extraModelFilePath = extraModelFile.getAbsolutePath();
                    if (log.isDebugEnabled()) log.debug("ERExtraD2WModelFile (absolute) = \"" + extraModelFilePath + "\"");
                    modelFiles.addElement(extraModelFile);
                    _hasAddedExtraModelFile = true;
                } else
                    log.warn("Can't read the ERExtraD2WModelFile file.");
            }
        }
        return modelFiles;
    }
    protected EOQualifier qualifierContainedInEnumeration(EOQualifierEvaluation q1, Enumeration e) {
        EOQualifier containedQualifier = null;
        while (e.hasMoreElements()) {
            EOQualifierEvaluation q2 = (EOQualifierEvaluation)e.nextElement();
            if (q1.equals(q2)) {
                containedQualifier = (EOQualifier)q2; break;
            }
        }
        if (containedQualifier != null && q1 != containedQualifier)
            uniquedQualifiers++;
        return containedQualifier;
    }

    protected EOQualifier qualifierInCache(EOQualifier q) {
        EOQualifier cacheQualifier = null;
        totalQualifiers++;
        if (q != null) {
            if (q instanceof EOKeyValueQualifier) {
                cacheQualifier = keyValueQualifierInCache((EOKeyValueQualifier)q);
            } else if (q instanceof EONotQualifier) {
                cacheQualifier = notQualifierInCache((EONotQualifier)q);
            } else if (q instanceof EOAndQualifier) {
                cacheQualifier = andQualifierInCache((EOAndQualifier)q);
            } else if (q instanceof EOOrQualifier) {
                cacheQualifier = orQualifierInCache((EOOrQualifier)q);
            } else {
                log.warn("Unknown qualifier type: " + q.getClass().getName());
            }
        } else {
            log.warn("Asking caceh for a null qualifier.");
        }
        return cacheQualifier;
    }

    protected Hashtable _uniqueAndQualifiers = new Hashtable();
    protected EOAndQualifier andQualifierInCache(EOAndQualifier q) {
        EOAndQualifier cachedQualifier = null;
        String hashEntryName = nameForSet(q.allQualifierKeys());
        Vector v = (Vector)_uniqueAndQualifiers.get(hashEntryName);
        if (v != null) {
            EOQualifier cache = qualifierContainedInEnumeration(q, v.elements());
            if (cache != null)
                cachedQualifier = (EOAndQualifier)cache;
        } else {
            v = new Vector();
            _uniqueAndQualifiers.put(hashEntryName, v);
        }
        if (cachedQualifier == null) {
            NSMutableArray qualifiers = null;
            for (int c = 0; c < q.qualifiers().count(); c++) {
                EOQualifier q1 = (EOQualifier)q.qualifiers().objectAtIndex(c);
                EOQualifier cache = qualifierInCache(q1);
                if (cache != null) {
                    if (qualifiers == null) {
                        qualifiers = new NSMutableArray();
                        qualifiers.addObjectsFromArray(q.qualifiers());
                    }
                    if (cache == q1)
                        log.warn("Found sub-qualifier: " + cache + " in cache when parent qualifier is not?!?!");
                    else
                        qualifiers.replaceObjectAtIndex(cache, c);
                }
            }
            if (qualifiers != null) {
                // Need to reconstruct
                cachedQualifier = new EOAndQualifier(qualifiers);
                v.addElement(cachedQualifier);
            } else {
                v.addElement(q);
            }
        }
        return cachedQualifier;
    }

    protected Hashtable _uniqueOrQualifiers = new Hashtable();
    protected EOOrQualifier orQualifierInCache(EOOrQualifier q) {
        EOOrQualifier cachedQualifier = null;
        String hashEntryName = nameForSet(q.allQualifierKeys());
        Vector v = (Vector)_uniqueOrQualifiers.get(hashEntryName);
        if (v != null) {
            EOQualifier cache = qualifierContainedInEnumeration(q, v.elements());
            if (cache != null)
                cachedQualifier = (EOOrQualifier)cache;
        } else {
            v = new Vector();
            _uniqueOrQualifiers.put(hashEntryName, v);
        }
        if (cachedQualifier == null) {
            NSMutableArray qualifiers = null;
            for (int c = 0; c < q.qualifiers().count(); c++) {
                EOQualifier q1 = (EOQualifier)q.qualifiers().objectAtIndex(c);
                EOQualifier cache = qualifierInCache(q1);
                if (cache != null) {
                    if (qualifiers == null) {
                        qualifiers = new NSMutableArray();
                        qualifiers.addObjectsFromArray(q.qualifiers());
                    }
                    if (cache == q1)
                        log.warn("Found sub-qualifier: " + cache + " in cache when parent qualifier is not?!?!");
                    else
                        qualifiers.replaceObjectAtIndex(cache, c);
                }
            }
            if (qualifiers != null) {
                // Need to reconstruct
                cachedQualifier = new EOOrQualifier(qualifiers);
                v.addElement(cachedQualifier);
            } else {
                v.addElement(q);
            }
        }
        return cachedQualifier;
    }
    
    protected Hashtable _uniqueNotQualifiers = new Hashtable();
    protected EONotQualifier notQualifierInCache(EONotQualifier q) {
        EONotQualifier cachedQualifier = null;
        String hashEntryName = nameForSet(q.allQualifierKeys());
        Vector v = (Vector)_uniqueNotQualifiers.get(hashEntryName);
        if (v != null) {
            EOQualifier cache = qualifierContainedInEnumeration(q, v.elements());
            if (cache != null)
                cachedQualifier = (EONotQualifier)cache;
        } else {
            v = new Vector();
            _uniqueNotQualifiers.put(hashEntryName, v);
        }
        if (cachedQualifier == null) {
            EOQualifier cache = qualifierInCache(q.qualifier());
            if (cache != null) {
                if (cache == q.qualifier()) {
                    log.warn("Found sub-qualifier in cache: " + cache + " when qualifier not in cache?!?! " + q);
                    v.addElement(q);                    
                } else {
                    // Need to construct a new EONotQualifier with the cached value..
                    cachedQualifier = new EONotQualifier(cache);
                    v.addElement(cachedQualifier);
                }
            } else {
                v.addElement(q);                
            }
        }
        return cachedQualifier;
    }
    
    protected Hashtable _uniqueKeyValueQualifiers = new Hashtable();
    protected EOKeyValueQualifier keyValueQualifierInCache(EOKeyValueQualifier q) {
        EOKeyValueQualifier cachedQualifier = null;
        Vector v = (Vector)_uniqueKeyValueQualifiers.get(q.key());
        if (v != null) {
            EOQualifier cache = qualifierContainedInEnumeration(q, v.elements());
            if (cache != null) {
                cachedQualifier = (EOKeyValueQualifier)cache;
            }
        } else {
            v = new Vector();
            _uniqueKeyValueQualifiers.put(q.key(), v);
        }
        if (cachedQualifier == null)
            v.addElement(q);            
        return cachedQualifier;
    }

    protected void flushUniqueCache() {
        _uniqueKeyValueQualifiers = new Hashtable();
        _uniqueNotQualifiers = new Hashtable();
        _uniqueOrQualifiers = new Hashtable();
        _uniqueNotQualifiers = new Hashtable();
    }
    
    // FIXME: Must be a better way of doing 
    public String nameForSet(NSSet set) {
        NSMutableArray stringObjects = new NSMutableArray();
        stringObjects.addObjectsFromArray(set.allObjects());
        ERXArrayUtilities.sortArrayWithKey(stringObjects, "description");
        return stringObjects.componentsJoinedByString(".");
    }


    // stuff to dump and restore the cache. This should increase the experience
    // for the first folks after an app needed to be restarted and the rules have not been fired yet
    // the schema is very simplicistic, though, and needs improvement

    protected static final String ENTITY_PREFIX = "::ENTITY::";
    protected static final String RELATIONSHIP_PREFIX = "::RELATIONSHIP::";
    protected static final String ATTRIBUTE_PREFIX = "::ATTRIBUTE::";

    protected Object encodeObject(Object o) {
        if(o instanceof EOEntity) {
            o = ENTITY_PREFIX + ((EOEntity)o).name();
        } else if(o instanceof EORelationship) {
            o = RELATIONSHIP_PREFIX + ((EORelationship)o).name() + ENTITY_PREFIX + ((EORelationship)o).entity().name();
        } else if(o instanceof EOAttribute) {
            o = ATTRIBUTE_PREFIX + ((EOAttribute)o).name() + ENTITY_PREFIX + ((EOAttribute)o).entity().name();
        }
        return o;
    }
    protected Object decodeObject(Object o) {
        if(o instanceof String) {
            String s = (String)o;
            if(s.indexOf(ENTITY_PREFIX) == 0) {
                String entityName = s.substring(ENTITY_PREFIX.length());
                o = EOModelGroup.defaultGroup().entityNamed(entityName);
            } else if(s.indexOf(RELATIONSHIP_PREFIX) == 0) {
                int entityOffset = s.indexOf(ENTITY_PREFIX);
                String entityName = s.substring(entityOffset + ENTITY_PREFIX.length());
                String relationshipName = s.substring(RELATIONSHIP_PREFIX.length(), entityOffset);
                EOEntity entity = EOModelGroup.defaultGroup().entityNamed(entityName);
                o = entity.relationshipNamed(relationshipName);
            } else if(s.indexOf(ATTRIBUTE_PREFIX) == 0) {
                int entityOffset = s.indexOf(ENTITY_PREFIX);
                String entityName = s.substring(entityOffset + ENTITY_PREFIX.length());
                String attributeName = s.substring(ATTRIBUTE_PREFIX.length(), entityOffset);
                EOEntity entity = EOModelGroup.defaultGroup().entityNamed(entityName);
                o = entity.attributeNamed(attributeName);
            }
        }
        return o;
    }

    protected boolean writeEntry(ERXMultiKey key, Object value, ObjectOutputStream out)  throws IOException {
        value=encodeObject(value);
        if((value != null) && !(value instanceof Serializable)) {
            return false;
        }
        Object keyKeys[] = key.keys();
        Object keys[]=new Object[keyKeys.length];
        for (short i=0; i<keys.length; i++) {
            Object o=keyKeys[i];
            o=encodeObject(o);
            if((o != null) && !(o instanceof Serializable)) {
                return false;
            }
            keys[i]=o;
        }
        out.writeObject(keys);
        out.writeObject(value);
        return true;
    }

    protected ERXMultiKey readEntry(Hashtable cache, ObjectInputStream in) throws IOException, ClassNotFoundException {
        Object keys[]=(Object[])in.readObject();
        Object value = decodeObject(in.readObject());
        for (short i=0; i<keys.length; i++) {
            Object o=decodeObject(keys[i]);
            keys[i]=o;
        }
        ERXMultiKey key = new ERXMultiKey(keys);
        cache.put(key,value);
        return key;
    }

    protected byte[] cacheToBytes(Hashtable cache) {
        try {
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(ostream);
            for(Enumeration keys = cache.keys(); keys.hasMoreElements();) {
                ERXMultiKey key = (ERXMultiKey)keys.nextElement();
                Object o = cache.get(key);
                if(writeEntry(key,o,out)) {
                    if(log.isDebugEnabled()) {
                        log.debug("Wrote: " + key + " -- " + o);
                    }
                } else {
                    log.info("Can't write: " + key + " -- " + o);
                }
            }
            out.flush();
            ostream.close();
            return ostream.toByteArray();
        } catch(Exception ex) {
            log.error(ex,ex);
        }
        return null;
    }

    protected Hashtable cacheFromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(istream);
            Hashtable newCache = new Hashtable(10000);
            try {
                //FIXME ak how do I do without the EOFException?
                for(;;) {
                    ERXMultiKey key = readEntry(newCache,in);
                    Object o = newCache.get(key);
                    if(log.isDebugEnabled()) {
                        log.debug("Read: " + key + " -- " + o);
                    }
                }
            } catch(EOFException ex) {
            }
            istream.close();
            return newCache;
        } catch(Exception ex) {
            log.error(ex,ex);
        }
        return null;
    }
}