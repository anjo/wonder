package com.webobjects.monitor.application;

import java.util.Enumeration;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.xml.WOXMLException;
import com.webobjects.appserver.xml._JavaMonitorCoder;
import com.webobjects.appserver.xml._JavaMonitorDecoder;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation._NSCollectionReaderWriterLock;
import com.webobjects.monitor._private.MApplication;
import com.webobjects.monitor._private.MHost;
import com.webobjects.monitor._private.MInstance;
import com.webobjects.monitor._private.MObject;
import com.webobjects.monitor._private.MSiteConfig;
import com.webobjects.monitor._private.String_Extensions;

public class WOTaskdHandler {

    public interface ErrorCollector {
        public void addObjectsFromArrayIfAbsentToErrorMessageArray(NSArray<String> errors);
    }

    private static _NSCollectionReaderWriterLock _lock = new _NSCollectionReaderWriterLock();

    private static MSiteConfig _siteConfig;

    public static MSiteConfig siteConfig() {
        return _siteConfig;
    }

    public static void createSiteConfig() {

        _siteConfig = MSiteConfig.unarchiveSiteConfig(false);
        if (_siteConfig == null) {
            NSLog.err.appendln("The Site Configuration could not be loaded from the local filesystem");
            System.exit(1);
        }

        for (Enumeration e = _siteConfig.hostArray().objectEnumerator(); e.hasMoreElements();) {
            _siteConfig.hostErrorArray.addObjectIfAbsent(e.nextElement());
        }
        if (_siteConfig.localHost() != null)
            _siteConfig.hostErrorArray.removeObject(_siteConfig.localHost());
    }

    public Application _theApplication = (Application) WOApplication.application();

    ErrorCollector _session;

    public WOTaskdHandler(ErrorCollector session) {
        _session = session;
    }

    private ErrorCollector mySession() {
        return _session;
    }

    private static _NSCollectionReaderWriterLock lock() {
        return _lock;
    }

    public void startReading() {
        lock().startReading();
    }

    public void endReading() {
        lock().endReading();
    }

    public void startWriting() {
        lock().startWriting();
    }

    public void endWriting() {
        lock().endWriting();
    }

    /** ******** Common Functionality ********* */
    private static NSMutableDictionary createUpdateRequestDictionary(MSiteConfig _Config, MHost _Host,
            MApplication _Application, NSArray _InstanceArray, String requestType) {
        NSMutableDictionary monitorRequest = new NSMutableDictionary(1);
        NSMutableDictionary updateWotaskd = new NSMutableDictionary(1);
        NSMutableDictionary requestTypeDict = new NSMutableDictionary();

        if (_Config != null) {
            NSDictionary site = new NSDictionary(_Config.values());
            requestTypeDict.takeValueForKey(site, "site");
        }
        if (_Host != null) {
            NSArray hostArray = new NSArray(_Host.values());
            requestTypeDict.takeValueForKey(hostArray, "hostArray");
        }
        if (_Application != null) {
            NSArray applicationArray = new NSArray(_Application.values());
            requestTypeDict.takeValueForKey(applicationArray, "applicationArray");
        }
        if (_InstanceArray != null) {
            int instanceCount = _InstanceArray.count();
            NSMutableArray instanceArray = new NSMutableArray(instanceCount);
            for (int i = 0; i < instanceCount; i++) {
                MInstance anInst = (MInstance) _InstanceArray.objectAtIndex(i);
                instanceArray.addObject(anInst.values());
            }
            requestTypeDict.takeValueForKey(instanceArray, "instanceArray");
        }

        updateWotaskd.takeValueForKey(requestTypeDict, requestType);
        monitorRequest.takeValueForKey(updateWotaskd, "updateWotaskd");

        return monitorRequest;
    }

    public WOResponse[] sendRequest(NSDictionary monitorRequest, NSArray wotaskdArray, boolean willChange) {
        String encodedRootObjectForKey = (new _JavaMonitorCoder()).encodeRootObjectForKey(monitorRequest,
                "monitorRequest");
        NSData content = new NSData(encodedRootObjectForKey.getBytes());
        return MHost.sendRequestToWotaskdArray(content, wotaskdArray, willChange);
    }

    /** ******* */

    /** ******** ADDING (UPDATE) ********* */
    protected void sendAddInstancesToWotaskds(NSArray newInstancesArray, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, null, null, newInstancesArray, "add"),
                wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "add", false, false, true, false);
    }

    protected void sendAddApplicationToWotaskds(MApplication newApplication, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, null, newApplication, null, "add"),
                wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "add", false, true, false, false);
    }

    protected void sendAddHostToWotaskds(MHost newHost, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, newHost, null, null, "add"),
                wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "add", true, false, false, false);
    }

    /** ******* */

    /** ******** REMOVING (UPDATE) ********* */
    protected void sendRemoveInstancesToWotaskds(NSArray exInstanceArray, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(
                createUpdateRequestDictionary(null, null, null, exInstanceArray, "remove"), wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "remove", false, false, true, false);
    }

    protected void sendRemoveApplicationToWotaskds(MApplication exApplication, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, null, exApplication, null, "remove"),
                wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "remove", false, true, false, false);
    }

    protected void sendRemoveHostToWotaskds(MHost exHost, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, exHost, null, null, "remove"),
                wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "remove", true, false, false, false);
    }

    /** ******* */

    /** ******** CONFIGURE (UPDATE) ********* */
    protected void sendUpdateInstancesToWotaskds(NSArray<MInstance> changedInstanceArray, NSArray wotaskdArray) {
        if (wotaskdArray.count() != 0 && changedInstanceArray.count() != 0) {
            WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, null, null, changedInstanceArray,
                    "configure"), wotaskdArray, true);
            NSDictionary[] responseDicts = generateResponseDictionaries(responses);
            getUpdateErrors(responseDicts, "configure", false, false, true, false);
        }
    }

    protected void sendUpdateApplicationToWotaskds(MApplication changedApplication, NSArray wotaskdArray) {
        if (wotaskdArray.count() != 0) {
            WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, null, changedApplication, null,
                    "configure"), wotaskdArray, true);
            NSDictionary[] responseDicts = generateResponseDictionaries(responses);
            getUpdateErrors(responseDicts, "configure", false, true, false, false);
        }
    }

    protected void sendUpdateApplicationAndInstancesToWotaskds(MApplication changedApplication, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, null, changedApplication,
                changedApplication.instanceArray(), "configure"), wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "configure", false, true, true, false);
    }

    protected void sendUpdateHostToWotaskds(MHost changedHost, NSArray wotaskdArray) {
        WOResponse[] responses = sendRequest(createUpdateRequestDictionary(null, changedHost, null, null, "configure"),
                wotaskdArray, true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, "configure", true, false, false, false);
    }

    protected void sendUpdateSiteToWotaskds() {
        startReading();
        try {
            NSMutableArray hostArray = siteConfig().hostArray();
            if (hostArray.count() != 0) {
                NSMutableDictionary updateRequestDictionary = createUpdateRequestDictionary(siteConfig(), null, null,
                        null, "configure");
                WOResponse[] responses = sendRequest(updateRequestDictionary, hostArray, true);
                NSDictionary[] responseDicts = generateResponseDictionaries(responses);
                getUpdateErrors(responseDicts, "configure", false, false, false, true);
            }
        } finally {
            endReading();
        }
    }

    /** ******* */

    /** ******** OVERWRITE / CLEAR (UPDATE) ********* */
    protected void sendOverwriteToWotaskd(MHost aHost) {
        NSDictionary SiteConfig = siteConfig().dictionaryForArchive();
        NSMutableDictionary data = new NSMutableDictionary(SiteConfig, "SiteConfig");
        _sendOverwriteClearToWotaskd(aHost, "overwrite", data);
    }

    protected void sendClearToWotaskd(MHost aHost) {
        String data = new String("SITE");
        _sendOverwriteClearToWotaskd(aHost, "clear", data);
    }

    private void _sendOverwriteClearToWotaskd(MHost aHost, String type, Object data) {
        NSMutableDictionary updateWotaskd = new NSMutableDictionary(data, type);
        NSMutableDictionary monitorRequest = new NSMutableDictionary(updateWotaskd, "updateWotaskd");

        WOResponse[] responses = sendRequest(monitorRequest, new NSArray(aHost), true);
        NSDictionary[] responseDicts = generateResponseDictionaries(responses);
        getUpdateErrors(responseDicts, type, false, false, false, false);
    }

    /** ******* */

    /** ******** COMMANDING ********* */
    private static Object[] commandInstanceKeys = new Object[] { "applicationName", "id", "hostName", "port" };

    public static void sendCommandInstancesToWotaskds(String command, NSArray instanceArray, NSArray wotaskdArray,
            WOTaskdHandler collector) {
        if (instanceArray.count() > 0 && wotaskdArray.count() > 0) {
            int instanceCount = instanceArray.count();

            NSMutableDictionary monitorRequest = new NSMutableDictionary(1);
            NSMutableArray commandWotaskd = new NSMutableArray(instanceArray.count() + 1);

            commandWotaskd.addObject(command);

            NSMutableArray instanceValueArray = new NSMutableArray(instanceCount);
            for (int i = 0; i < instanceCount; i++) {
                MInstance anInst = (MInstance) instanceArray.objectAtIndex(i);
                commandWotaskd.addObject(new NSDictionary(new Object[] { anInst.applicationName(), anInst.id(),
                        anInst.hostName(), anInst.port() }, commandInstanceKeys));
            }
            monitorRequest.takeValueForKey(commandWotaskd, "commandWotaskd");

            WOResponse[] responses = collector.sendRequest(monitorRequest, wotaskdArray, false);
            NSDictionary[] responseDicts = collector.generateResponseDictionaries(responses);
            // System.out.println("OUT: " + NSPropertyListSerialization.stringFromPropertyList(monitorRequest) + "\n\nIN: " +  NSPropertyListSerialization.stringFromPropertyList(new NSArray(responseDicts)));
            collector.getCommandErrors(responseDicts);
        }
    }

    private void sendCommandInstancesToWotaskds(String command, NSArray<MInstance> instanceArray,
            NSArray<MHost> wotaskdArray) {
        sendCommandInstancesToWotaskds(command, instanceArray, wotaskdArray, this);
    }

    protected void sendQuitInstancesToWotaskds(NSArray<MInstance> instanceArray, NSArray<MHost> wotaskdArray) {
        sendCommandInstancesToWotaskds("QUIT", instanceArray, wotaskdArray, this);
    }

    protected void sendStartInstancesToWotaskds(NSArray<MInstance> instanceArray, NSArray<MHost> wotaskdArray) {
        sendCommandInstancesToWotaskds("START", instanceArray, wotaskdArray, this);
    }

    protected void sendClearDeathsToWotaskds(NSArray<MInstance> instanceArray, NSArray<MHost> wotaskdArray) {
        sendCommandInstancesToWotaskds("CLEAR", instanceArray, wotaskdArray, this);
    }

    protected void sendStopInstancesToWotaskds(NSArray<MInstance> instanceArray, NSArray<MHost> wotaskdArray) {
        sendCommandInstancesToWotaskds("STOP", instanceArray, wotaskdArray, this);
    }

    protected void sendRefuseSessionToWotaskds(NSArray<MInstance> instanceArray, NSArray<MHost> wotaskdArray,
            boolean doRefuse) {
        for (MInstance instance : instanceArray) {
            instance.setRefusingNewSessions(doRefuse);
        }
        sendCommandInstancesToWotaskds((doRefuse ? "REFUSE" : "ACCEPT"), instanceArray, wotaskdArray);
    }

    /** ******* */

    /** ******** QUERIES ********* */
    private NSMutableDictionary createQuery(String queryString) {
        NSMutableDictionary monitorRequest = new NSMutableDictionary(queryString, "queryWotaskd");
        return monitorRequest;
    }

    protected WOResponse[] sendQueryToWotaskds(String queryString, NSArray wotaskdArray) {
        return sendRequest(createQuery(queryString), wotaskdArray, false);
    }

    /** ******* */

    /** ******** Response Handling ********* */
    public static NSDictionary responseParsingFailed = new NSDictionary(new NSDictionary(new NSArray(
            "INTERNAL ERROR: Failed to parse response XML"), "errorResponse"), "monitorResponse");

    public static NSDictionary emptyResponse = new NSDictionary(new NSDictionary(new NSArray(
            "INTERNAL ERROR: Response returned was null or empty"), "errorResponse"), "monitorResponse");

    private NSDictionary[] generateResponseDictionaries(WOResponse[] responses) {
        NSDictionary[] responseDicts = new NSDictionary[responses.length];
        for (int i = 0; i < responses.length; i++) {
            if ((responses[i] != null) && (responses[i].content() != null)) {
                try {
                    responseDicts[i] = (NSDictionary) (new _JavaMonitorDecoder()).decodeRootObject(responses[i]
                            .content());
                } catch (WOXMLException wxe) {
                    responseDicts[i] = responseParsingFailed;
                }
            } else {
                responseDicts[i] = emptyResponse;
            }
        }
        return responseDicts;
    }

    /** ******* */

    /** ******** Error Handling ********* */
    public NSMutableArray getUpdateErrors(NSDictionary[] responseDicts, String updateType, boolean hasHosts,
            boolean hasApplications, boolean hasInstances, boolean hasSite) {
        NSMutableArray errorArray = new NSMutableArray();

        boolean clearOverwrite = false;
        if ((updateType.equals("overwrite")) || (updateType.equals("clear")))
            clearOverwrite = true;

        for (int i = 0; i < responseDicts.length; i++) {
            if (responseDicts[i] != null) {
                NSDictionary responseDict = responseDicts[i];
                getGlobalErrorFromResponse(responseDict, errorArray);

                NSDictionary updateWotaskdResponseDict = (NSDictionary) responseDict
                        .valueForKey("updateWotaskdResponse");

                if (updateWotaskdResponseDict != null) {
                    NSDictionary updateTypeResponse = (NSDictionary) updateWotaskdResponseDict.valueForKey(updateType);
                    if (updateTypeResponse != null) {
                        if (clearOverwrite) {
                            String errorMessage = (String) updateTypeResponse.valueForKey("errorMessage");
                            if (errorMessage != null) {
                                errorArray.addObject(errorMessage);
                            }
                        } else {
                            if (hasSite) {
                                NSDictionary aDict = (NSDictionary) updateTypeResponse.valueForKey("site");
                                String errorMessage = (String) aDict.valueForKey("errorMessage");
                                if (errorMessage != null) {
                                    errorArray.addObject(errorMessage);
                                }
                            }
                            if (hasHosts)
                                _addUpdateResponseToErrorArray(updateTypeResponse, "hostArray", errorArray);
                            if (hasApplications)
                                _addUpdateResponseToErrorArray(updateTypeResponse, "applicationArray", errorArray);
                            if (hasInstances)
                                _addUpdateResponseToErrorArray(updateTypeResponse, "instanceArray", errorArray);
                        }
                    }
                }
            }
        }
        if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupDeployment))
            NSLog.debug.appendln("##### getUpdateErrors: " + errorArray);
        mySession().addObjectsFromArrayIfAbsentToErrorMessageArray(errorArray);
        return errorArray;
    }

    protected void _addUpdateResponseToErrorArray(NSDictionary updateTypeResponse, String responseKey,
            NSMutableArray errorArray) {
        NSArray aResponse = (NSArray) updateTypeResponse.valueForKey(responseKey);
        if (aResponse != null) {
            for (Enumeration e = aResponse.objectEnumerator(); e.hasMoreElements();) {
                NSDictionary aDict = (NSDictionary) e.nextElement();
                String errorMessage = (String) aDict.valueForKey("errorMessage");
                if (errorMessage != null) {
                    errorArray.addObject(errorMessage);
                }
            }
        }
    }

    public NSMutableArray getCommandErrors(NSDictionary[] responseDicts) {
        NSMutableArray errorArray = new NSMutableArray();

        for (int i = 0; i < responseDicts.length; i++) {
            if (responseDicts[i] != null) {
                NSDictionary responseDict = responseDicts[i];
                getGlobalErrorFromResponse(responseDict, errorArray);

                NSArray commandWotaskdResponse = (NSArray) responseDict.valueForKey("commandWotaskdResponse");
                if ((commandWotaskdResponse != null) && (commandWotaskdResponse.count() > 0)) {
                    int count = commandWotaskdResponse.count();
                    for (int j = 1; j < count; j++) {
                        NSDictionary aDict = (NSDictionary) commandWotaskdResponse.objectAtIndex(j);
                        String errorMessage = (String) aDict.valueForKey("errorMessage");
                        if (errorMessage != null) {
                            errorArray.addObject(errorMessage);
                            if (j == 0)
                                break; // the command produced an error,
                            // parsing didn't finish
                        }
                    }
                }
            }
        }
        if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupDeployment))
            NSLog.debug.appendln("##### getCommandErrors: " + errorArray);
        mySession().addObjectsFromArrayIfAbsentToErrorMessageArray(errorArray);
        return errorArray;
    }

    protected NSMutableArray getQueryErrors(NSDictionary[] responseDicts) {
        NSMutableArray errorArray = new NSMutableArray();

        for (int i = 0; i < responseDicts.length; i++) {
            if (responseDicts[i] != null) {
                NSDictionary responseDict = responseDicts[i];
                getGlobalErrorFromResponse(responseDict, errorArray);

                NSArray commandWotaskdResponse = (NSArray) responseDict.valueForKey("commandWotaskdResponse");
                if ((commandWotaskdResponse != null) && (commandWotaskdResponse.count() > 0)) {
                    int count = commandWotaskdResponse.count();
                    for (int j = 1; j < count; j++) {
                        NSDictionary aDict = (NSDictionary) commandWotaskdResponse.objectAtIndex(j);
                        String errorMessage = (String) aDict.valueForKey("errorMessage");
                        if (errorMessage != null) {
                            errorArray.addObject(errorMessage);
                            if (j == 0)
                                break; // the command produced an error,
                            // parsing didn't finish
                        }
                    }
                }
            }
        }
        if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupDeployment))
            NSLog.debug.appendln("##### getQueryErrors: " + errorArray);
        mySession().addObjectsFromArrayIfAbsentToErrorMessageArray(errorArray);
        return errorArray;
    }

    protected void getGlobalErrorFromResponse(NSDictionary responseDict, NSMutableArray errorArray) {
        NSArray errorResponse = (NSArray) responseDict.valueForKey("errorResponse");
        if (errorResponse != null) {
            errorArray.addObjectsFromArray(errorResponse);
        }
    }

    public void getInstanceStatusForHosts(NSArray<MHost> hostArray) {
        if (hostArray.count() != 0) {

            WOResponse[] responses = sendQueryToWotaskds("INSTANCE", hostArray);

            NSMutableArray errorArray = new NSMutableArray();
            NSArray responseArray = null;
            NSDictionary responseDictionary = null;
            NSDictionary queryResponseDictionary = null;
            for (int i = 0; i < responses.length; i++) {
                if ((responses[i] == null) || (responses[i].content() == null)) {
                    responseDictionary = emptyResponse;
                } else {
                    try {
                        responseDictionary = (NSDictionary) new _JavaMonitorDecoder().decodeRootObject(responses[i]
                                .content());
                    } catch (WOXMLException wxe) {
                        NSLog.err.appendln("MonitorComponent pageWithName(AppDetailPage) Error decoding response: "
                                + responses[i].contentString());
                        responseDictionary = responseParsingFailed;
                    }
                }
                getGlobalErrorFromResponse(responseDictionary, errorArray);

                queryResponseDictionary = (NSDictionary) responseDictionary.valueForKey("queryWotaskdResponse");
                if (queryResponseDictionary != null) {
                    responseArray = (NSArray) queryResponseDictionary.valueForKey("instanceResponse");
                    if (responseArray != null) {
                        for (int j = 0; j < responseArray.count(); j++) {
                            responseDictionary = (NSDictionary) responseArray.objectAtIndex(j);

                            String host = (String) responseDictionary.valueForKey("host");
                            Integer port = (Integer) responseDictionary.valueForKey("port");
                            String runningState = (String) responseDictionary.valueForKey("runningState");
                            Boolean refusingNewSessions = (Boolean) responseDictionary
                                    .valueForKey("refusingNewSessions");
                            NSDictionary statistics = (NSDictionary) responseDictionary.valueForKey("statistics");
                            NSArray deaths = (NSArray) responseDictionary.valueForKey("deaths");
                            String nextShutdown = (String) responseDictionary.valueForKey("nextShutdown");

                            MInstance anInstance = siteConfig().instanceWithHostnameAndPort(host, port);
                            if (anInstance != null) {
                                for (int k = 0; k < MObject.stateArray.length; k++) {
                                    if (MObject.stateArray[k].equals(runningState)) {
                                        anInstance.state = k;
                                        break;
                                    }
                                }
                                anInstance.setRefusingNewSessions(String_Extensions.boolValue(refusingNewSessions));
                                anInstance.setStatistics(statistics);
                                anInstance.setDeaths(new NSMutableArray(deaths));
                                anInstance.setNextScheduledShutdownString_M(nextShutdown);
                            }
                        }
                    }
                }
            } // For Loop
            if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupDeployment))
                NSLog.debug.appendln("##### pageWithName(AppDetailPage) errors: " + errorArray);
            mySession().addObjectsFromArrayIfAbsentToErrorMessageArray(errorArray);
        }

    }

    public void getHostStatusForHosts(NSArray<MHost> hostArray) {
        WOResponse[] responses = sendQueryToWotaskds("HOST", hostArray);

        NSMutableArray errorArray = new NSMutableArray();
        NSDictionary responseDict = null;
        for (int i = 0; i < responses.length; i++) {
            MHost aHost = (MHost) siteConfig().hostArray().objectAtIndex(i);

            if ((responses[i] == null) || (responses[i].content() == null)) {
                responseDict = emptyResponse;
            } else {
                try {
                    responseDict = (NSDictionary) new _JavaMonitorDecoder().decodeRootObject(responses[i].content());
                } catch (WOXMLException wxe) {
                    NSLog.err.appendln("MonitorComponent pageWithName(HostsPage) Error decoding response: "
                            + responses[i].contentString());
                    responseDict = responseParsingFailed;
                }
            }
            getGlobalErrorFromResponse(responseDict, errorArray);

            NSDictionary queryResponse = (NSDictionary) responseDict.valueForKey("queryWotaskdResponse");
            if (queryResponse != null) {
                NSDictionary hostResponse = (NSDictionary) queryResponse.valueForKey("hostResponse");
                aHost._setHostInfo(hostResponse);
                aHost.isAvailable = true;
            } else {
                aHost.isAvailable = false;
            }
        } // for
        if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupDeployment))
            NSLog.debug.appendln("##### pageWithName(HostsPage) errors: " + errorArray);
        mySession().addObjectsFromArrayIfAbsentToErrorMessageArray(errorArray);

    }

    public void getApplicationStatusForHosts(NSArray<MHost> hostArray) {

        WOResponse[] responses = sendQueryToWotaskds("APPLICATION", hostArray);

        NSMutableArray errorArray = new NSMutableArray();
        NSDictionary applicationResponseDictionary;
        NSDictionary queryResponseDictionary;
        NSArray responseArray = null;
        NSDictionary responseDictionary = null;
        for (int i = 0; i < responses.length; i++) {
            if ((responses[i] == null) || (responses[i].content() == null)) {
                queryResponseDictionary = emptyResponse;
            } else {
                try {
                    queryResponseDictionary = (NSDictionary) new _JavaMonitorDecoder().decodeRootObject(responses[i]
                            .content());
                } catch (WOXMLException wxe) {
                    NSLog.err.appendln("MonitorComponent pageWithName(ApplicationsPage) Error decoding response: "
                            + responses[i].contentString());
                    queryResponseDictionary = responseParsingFailed;
                }
            }
            getGlobalErrorFromResponse(queryResponseDictionary, errorArray);

            applicationResponseDictionary = (NSDictionary) queryResponseDictionary.valueForKey("queryWotaskdResponse");
            if (applicationResponseDictionary != null) {
                responseArray = (NSArray) applicationResponseDictionary.valueForKey("applicationResponse");
                if (responseArray != null) {
                    for (int j = 0; j < responseArray.count(); j++) {
                        responseDictionary = (NSDictionary) responseArray.objectAtIndex(j);
                        String appName = (String) responseDictionary.valueForKey("name");
                        Integer runningInstances = (Integer) responseDictionary.valueForKey("runningInstances");
                        MApplication anApplication = siteConfig().applicationWithName(appName);
                        if (anApplication != null) {
                            // KH - this is massively suboptimal
                            anApplication.runningInstancesCount = new Integer(anApplication.runningInstancesCount
                                    .intValue()
                                    + runningInstances.intValue());
                        }
                    }
                }
            }
        } // for
        if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupDeployment))
            NSLog.debug.appendln("##### pageWithName(ApplicationsPage) errors: " + errorArray);
        mySession().addObjectsFromArrayIfAbsentToErrorMessageArray(errorArray);
    }
}
