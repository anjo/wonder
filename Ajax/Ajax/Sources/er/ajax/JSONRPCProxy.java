package er.ajax;

import java.text.ParseException;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.metaparadigm.jsonrpc.JSONRPCBridge;
import com.metaparadigm.jsonrpc.JSONRPCResult;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResourceManager;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableDictionary;

/**
 * Ce composant, sans persistance d'�tat, g�re la communication, via le protocol
 * HTTP, entre le monde Javascript sur le poste du client et le monde Java sur
 * le serveur WebObjects, via une conception <site>Remote Procedure Call</site>
 * implant� par la librairie JSON-RPC.
 * <p>
 * Ce composant g�n�re du code javascript qui initialise une variable javascript
 * qui sera utilis� comme variable d'amorce pour la communication rpc. Le nom de
 * cette variable est donn�e via le <em>binding</em> <code>nomJsVar</code>.
 * Il y a un objet, c�t� serveur qui se charge de la coersion et de
 * l'acheminement de la communication, c'est un objet de type JSONRPCBridge.<br>
 * Une instance de JSONRPCBridge est automatiquement cr�� si elle n'existe pas
 * d�j� (voir le binding). Il est recommand� de la fournir en binding afin
 * d'acc�l�rer le traitement des demandes.<br>
 * Si la variable assign� au <em>binding</em> <code>JSONRPCBridge</code> est
 * null, alors un objet est instanci� et assign� � cette variable. Il est ainsi
 * possible d'avoir une seul 'passerelle' pour un ensemble d'objets.
 * </p>
 * <p>
 * L'objet assign� en <em>proxy</em> sera l'objet serveur qui sera acc�d� via
 * l'attribut <em>nomProxy</em> de la variable <em>nomJsVar</em> sur le
 * poste client (<code>javascript</code>). <br>
 * le binding suivant: <code>
 * JSProxyRPC : JSProxyRPC {<br>
 *      nomProxy = "wopage";<br>
 *      nomJsVar = "jsonrpc";<br>
 * }</code> sera utilis� comme suit : <table
 * border="1">
 * <tr>
 * <th width="50%">JavaScript (client)</th>
 * <th width="50%">Java (serveur)</th>
 * </tr>
 * <tr>
 * <td><code>
 *      <em>// index d'une s�lection sur le client</em>;<br/>
 *      var idx = 3;<br/>
 *      <em>// via rpc, demande � notre proxy (par d�fault la page web) nom� 'wopage' quel est le nom du client pour cet index.</em><br/>
 *      var nom = jsonrpc.wopage.nomClient(idx);<br/>
 * </code></td>
 * <td><code>
 * // on re�oit la variable donn�e en argument et on retourne la valeur, comme tout appel java.
 *      public String nomClient(int i) { <br/>
 *          return <em>"quelquechose!"+i</em>;<br/>
 *  }<br/>
 * </code></td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * Notez que si aucun objet proxy n'est donn�e, il utilise le composant parent,
 * c'est a dire le composant dans lequel se trouve le composant JSProxyRPC.
 * </p>
 * <h3>Probl�me potentiel</h3>
 * L'utilisation de composant peut g�n�rer plusieurs requetes au serveur
 * WebObjects, comme toute ces requetes sont des � Component-Action � ceux-ci
 * augmente le context-id � chaque invocation. Ce qui a le probleme potentiel de
 * vider la cache des pages et ainsi engendrer un � page backtrack too far �.
 * <br>
 * Afin d'�viter ceci, on peut �viter de remplir inutiliement la cache des pages
 * avec les requetes provenant de l'utilisation du proxy JSProxyRPC. <br>
 * Dans la sous-classe de WOSession (Session.java), ajouter la logique suivante :
 * 
 * <pre>
 * public void savePage(WOComponent page) {
 *     NSDictionary ui = context().request().userInfo();
 *     if (ui == null
 *             || ui.objectForKey(er.ajax.JSONRPCProxy.AJAX_REQUEST_KEY) == null) {
 *         super.savePage(page);
 *     }
 * }
 * </pre>
 * 
 * <h2>Synopsis</h2>
 * 
 * JSProxyRPC { nomProxy=<em>uneString</em>; nomJsVar=<em>uneString</em>;
 * [proxy=<em>unObjetJava</em>;] [JSONRPCBridge=<em>unObjetJSONRPCBridge</em>;] }
 * 
 * <h2>Liens (<cite>bindings</cite>)</h2>
 * 
 * <blockquote>
 * <dl>
 * <dt>proxy</dt>
 * <dd>Objet du c�t� serveur (Java) qui sera repr�sent� du c�t� client
 * (Javascript). Si aucun objet n'est sp�cifi�, l'objet parent de ce composant
 * sera assign� comme proxy par d�faut.</dd>
 * <dt>nomProxy</dt>
 * <dd>Nom donn�e, en attribut de la variable jsrpc sur le c�t� client
 * (Javascript), pour acc�der au proxy de l'objet java c�t� serveur (Java).</dd>
 * <dt>nomJsVar</dt>
 * <dd>Nom de la variable javascript qui repr�sentera le lien jsrpc.</dd>
 * <dt>JSONRPCBridge</dt>
 * <dd>Objet du c�t� serveur (Java) qui sera effectuera le traitement de la
 * requete JSONRPC. Si aucuve valeur n'est donn�e ici, une nouvelle instance de
 * <code>com.metaparadigm.jsonrpc.JSRPCBridge</code> sera cr�er � chaque
 * traitement d'une requete. Si une valeur est donn�e i�i mais qu'elle n'est pas
 * encore initialis�, alors on en fera la cr�ation et l'assignation afin que
 * cette valeur puisse �tre partag�. </dd>
 * </dl>
 * </blockquote>
 * 
 * <h2>Todo</h2>
 * <ul>
 * <li> Complete the JSON-RPC integration to be able to leverage all possibilities of that library (foreignt references,
 * etc.).
 * </ul>
 * 
 * @author Jean-Fran�ois Veillette <jfveillette@os.ca>
 * @version $Revision $, $Date $ <br>
 *          &copy; 2005 OS communications informatiques, inc. http://www.os.ca
 *          Tous droits r�serv�s.
 */

public class JSONRPCProxy extends WOComponent {

    /** Logger Log4J du nom "er.ajax.JSONRPCProxy" */
    private static Logger log = Logger.getLogger(JSONRPCProxy.class);

    /** 
     * Key that flags the session to not save the page in the cache.
     */
    public static final String AJAX_REQUEST_KEY = "AJAX_REQUEST_KEY";

    public JSONRPCProxy(WOContext context) {
        super(context);
    }

    /** 
     * Overridden because the component is stateless
     */
    public boolean isStateless() {
        return true;
    }


    /** 
     * Overridden because the component does not synch with the bindings.
     */
    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    private String htmlCloseHead() {
        return "</head>";
    }

    private NSMutableDictionary mutableUserInfo(WOMessage message) {
        NSDictionary dict = message.userInfo();
        NSMutableDictionary result = null;
        if(dict == null) {
            result = new NSMutableDictionary();
            context().response().setUserInfo(result);
        } else {
            if(dict instanceof NSMutableDictionary) {
                result = (NSMutableDictionary)dict;
            } else {
                result = dict.mutableClone();
                message.setUserInfo(result);
            }
        }
        return result;
    }

    private void insertInResponseBeforeTag(WOResponse res, String content, String tag) {
        String stream = res.contentString();
        int idx = stream.indexOf(tag);
        String pre = stream.substring(0,idx);
        String post = stream.substring(idx, stream.length());
        res.setContent(pre+content+post);
    }

    private void addJSResource(WOResponse res, String fileName) {
        NSMutableDictionary userInfo = mutableUserInfo(context().response());
        if(userInfo.objectForKey(fileName) == null) {
            userInfo.setObjectForKey(fileName, fileName);
            WOResourceManager rm = application().resourceManager();
            String url = rm.urlForResourceNamed(fileName, "Ajax", session().languages(), context().request());
            String js = "<script type=\"text/javascript\" src=\""+ url +"\"></script>";
            insertInResponseBeforeTag(res, js, htmlCloseHead());
        }
    }

    public void appendToResponse(WOResponse res, WOContext ctx) {
        super.appendToResponse(res, ctx);
        addRequiredWebResources(res);
    }
   
    /**
     * Adds the jsonrpc.js script to the head in the response if not already present and also adds a
     * javascript proxy for the supplied bridge under the name "JSONRPC_<variableName>".  
     * @param res
     */
    private void addRequiredWebResources(WOResponse res) {
        addJSResource(res, "jsonrpc.js");
        
        NSMutableDictionary userInfo = mutableUserInfo(context().response());
        String name = (String)valueForBinding("name");
        String key = "JSONRPC_"+name;
        Object oldValue = userInfo.objectForKey(key);
        Object bridge = valueForBinding("JSONRPCBridge");
        if (bridge == null) {
            bridge = NSKeyValueCoding.NullValue;
        }
        if(oldValue == null) {
            // ajoute la variable javascript 'nomJsVar' que si celle-ci n'est pas d�j� dans la r�ponse
            userInfo.setObjectForKey(bridge, key);
            insertInResponseBeforeTag(res, "<script type=\"text/javascript\">\nvar "+ name +" = new JSONRpcClient(\""+ context().componentActionURL() +"\");\n</script>", htmlCloseHead());
        } else {
            // la variable javascript 'nomJsVar' est d�j� dans la r�ponse, est-ce qu'elle fait r�f�rence au m�me objet JSONRPCBridge ?
            if (bridge != oldValue) {
                // il y a un fort potentiel de probleme !  une m�me variable se fait assigner plusieurs valeurs
                log.warn("il semble y avoir un conflit, une m�me variable javascript '"+name+"' est li� a plusieurs objets proxy: <"+bridge+"> et <"+oldValue+">");
            }
        }
    }

    /** Execute la requete, si la requete correspond bien � ce composant.  Si c'est une requete de traitement de proxy, on place la cl� <code>AJAX_REQUEST_KEY</code> dans le dictionnaire userInfo de la requete (<code>requestUserInfo()</code>). */
    public WOActionResults invokeAction(WORequest request, WOContext context) {
        Object result = null;
        String elementID = context.elementID();
        String senderID = context.senderID();
        WOComponent wocomponent = context.component();
        if (elementID != null && elementID.equals(senderID)) {
            log.debug("JSONRPC requete:"+ request.contentString());
            result = handleRequest(request, context);
            NSMutableDictionary dict = mutableUserInfo(context().request());
            dict.takeValueForKey(AJAX_REQUEST_KEY, AJAX_REQUEST_KEY);
        }
        return (WOActionResults)result;
    }

    /** S'assure de l'execution de la requete par un objet JSONRPCBridge. */
    private WOResponse handleRequest(WORequest request, WOContext context) {
        WOApplication app = WOApplication.application();
        WOResponse response = app.createResponseInContext(null);

        // Encode using UTF-8, although We are actually ASCII clean as all unicode data is JSON escaped using backslash u. This is less data efficient for foreign character sets but it is needed to support naughty browsers such as Konqueror and Safari which do not honour the charset set in the response
        response.setHeader("text/plain;charset=utf-8", "content-type");

        String inputString = request.contentString();
        log.debug("JSONRPCServlet.service recv: " + inputString);

       // Process the request
        JSONObject input = null;
        Object output = null;
        try {
            input = new JSONObject(inputString);

            // Get method name and arguments
            String methodName = null;
            JSONArray arguments = null;

            try { 
                methodName = input.getString("method");
            } catch (NoSuchElementException ne) {
                // nothing
            }

            // Back compatibility for <= 0.7 clients
            if (methodName != null) {
                arguments = input.getJSONArray("params");
            }
            // Is this a CallableReference it will have a non-zero objectID
            int reference = input.optInt("objectID");
            
            if(reference != 0) {
                log.debug("JSONRPCServlet.service call objectID=" + reference + " " + methodName + "(" + arguments + ")");
            } else {
                log.debug("JSONRPCServlet.service call " + methodName + "(" + arguments + ")");
            }
            
            Object proxy;
            if(canGetValueForBinding("proxy")) {
                proxy = valueForBinding("proxy");
            } else {
                proxy = parent();
            }
            String proxyName = (String) valueForBinding("proxyName");
            
            
            JSONRPCBridge bridge = null;
            if(canGetValueForBinding("JSONRPCBridge")) {
                bridge = (JSONRPCBridge)valueForBinding("JSONRPCBridge");
            } else {
                bridge = new JSONRPCBridge();
                if(canSetValueForBinding("JSONRPCBridge")) {
                    setValueForBinding(bridge, "JSONRPCBridge");
                }
            }

            bridge.setDebug(log.isDebugEnabled());
            bridge.registerObject( proxyName , proxy);
            output = bridge.call(new Object[] {proxy}, input);
        } catch (ParseException e) {
            log.error("JSONRPCServlet.service can't parse call: " + inputString);
            output = JSONRPCResult.MSG_ERR_PARSE;
        } catch (NoSuchElementException e) {
            log.error("JSONRPCServlet.service no method in request");
            output = JSONRPCResult.MSG_ERR_NOMETHOD;
        }

        // Write the response
        log.debug("JSONRPCServlet.service send: " + output.toString());
        response.setHeader("Connection", "keep-alive");
        response.appendContentString(output.toString());
        return response;
    }
}
