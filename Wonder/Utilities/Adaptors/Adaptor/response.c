/*

Copyright � 2000-2007 Apple, Inc. All Rights Reserved.

The contents of this file constitute Original Code as defined in and are
subject to the Apple Public Source License Version 1.1 (the 'License').
You may not use this file except in compliance with the License. 
Please obtain a copy of the License at http://www.apple.com/publicsource 
and read it before usingthis file.

This Original Code and all software distributed under the License are
distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT. 
Please see the License for the specific language governing rights 
and limitations under the License.


*/
#include "config.h"
#include "response.h"
#include "log.h"
#include "errors.h"
#include "womalloc.h"

#include <stdlib.h>
#include <stdio.h>		/* sprintf() */
#include <string.h>
#include <ctype.h>

#ifdef WIN32
int strcasecmp(const char *, const char *);
#endif

#define	STATUS	"Status"
#define	DEFAULT_CONTENT	"text/html"

static const char * const apple = "Apple";

HTTPResponse *resp_new(char *statusStr, WOInstanceHandle instHandle, WOConnection *instanceConnection)
{
   HTTPResponse *resp;
   char *status = statusStr;

   /*
    *	reformat the status line from
    *	"HTTP/1.0 200 OK Apple WebObjects" to "OK Apple..."
    */
   while (status && *status && !isspace((int)*status))	status++;
   while (*status && !isdigit((int)*status))	status++;
   if ( !(status && *status) ) {
      WOLog(WO_ERR,"Invalid response!");
      return NULL;
   }

   resp = WOCALLOC(1,sizeof(HTTPResponse));
   resp->status = atoi(status);
   resp->statusMsg = (char *)apple;
   if (strncmp(statusStr, "HTTP/1.", 7) == 0)
   {
      if (statusStr[7] == '0')
         resp->flags |= RESP_HTTP10;
      else if (statusStr[7] == '1')
         resp->flags |= RESP_HTTP11;
   }

   resp->headers = st_new(10);
   resp->instanceConnection = instanceConnection;
   resp->instHandle = instHandle;

   return resp;
}

static const char errorRespTextBegin[] = "<html><body><strong>";
static const char errorRespTextEnd[] = "</strong></body></html>\n";

HTTPResponse *resp_errorResponse(const char *msg, int status)
{
   HTTPResponse *resp;
   char buf[12];
   String *html_msg;

   resp = WOCALLOC(1,sizeof(HTTPResponse));
   resp->status = status;
   resp->statusMsg = WOSTRDUP("Error WebObjects");
   resp->headers = st_new(2);
   st_add(resp->headers, CONTENT_TYPE, DEFAULT_CONTENT, 0);
   html_msg = str_create(errorRespTextBegin, sizeof(errorRespTextBegin) + sizeof(errorRespTextEnd) + strlen(msg));
   str_append(html_msg, msg);
   str_append(html_msg, errorRespTextEnd);
   resp->content_length = resp->content_valid = resp->content_read = html_msg->length;
   resp->content = html_msg->text;
   resp_addStringToResponse(resp, html_msg);
   resp->flags |= RESP_DONT_FREE_CONTENT;
   sprintf(buf,"%d",resp->content_length);
   st_add(resp->headers, CONTENT_LENGTH, buf, STR_COPYVALUE|STR_FREEVALUE);
   return resp;
}

/*
 *	return a redirect response to 'path'
 */
HTTPResponse *resp_redirectedResponse(const char *path)
{
   HTTPResponse *resp;

   resp = WOCALLOC(1,sizeof(HTTPResponse));
   resp->status = 302; /* redirected */
   resp->statusMsg = WOSTRDUP("OK Apple");
   resp->headers = st_new(2);
   st_add(resp->headers, LOCATION, path, STR_COPYVALUE|STR_FREEVALUE);
   return resp;
}



void resp_free(HTTPResponse *resp)
{
   if (resp->headers) {
      st_free(resp->headers);
   }
   if (resp->responseStrings)
      str_free(resp->responseStrings);
   if (resp->statusMsg && (resp->statusMsg != apple))
      WOFREE(resp->statusMsg);
   if (resp->content && !(resp->flags & RESP_DONT_FREE_CONTENT))
      WOFREE(resp->content);
   if (resp->instanceConnection && (resp->flags & RESP_CLOSE_CONNECTION))
      tr_close(resp->instanceConnection, resp->instHandle, resp->keepConnection);
   WOFREE(resp);
}


void resp_addHeader(HTTPResponse *resp, String *rawhdr)
{
   char *key, *value;

   /* keep track of the String */
   resp_addStringToResponse(resp, rawhdr);
   
   /*
    *	break into key/value, make key lowercase
    *   Note that this walks over the String's buffer, but
    *   at this point the response owns the String so that's ok.
    *   The String buffer will get reclaimed when responseStrings
    *   is freed, in resp_free().
    */
   for (key = rawhdr->text, value = key; *value != ':'; value++) {
      if (isupper((int)*value))
         *value = tolower((int)*value);				/* ... and change to lowercase. */
   }
   if (*value == ':') {
      *value++ = '\0';			/* terminate key string */
      while (*value && isspace((int)*value))	value++;
   } else {
      return;			/* Zounds ! something wrong with header... */
   }

   st_add(resp->headers, key, value, 0);

   /*
    *	scan inline for content-length
    */
   if ((resp->content_length == 0) && ((strcasecmp(CONTENT_LENGTH,key) == 0) || strcasecmp("content_length", key) == 0))
   {
      resp->content_length = atoi(value);
   }

   return;
}


HTTPResponse *resp_getResponseHeaders(WOConnection *instanceConnection, WOInstanceHandle instHandle)
{
   HTTPResponse *resp;
   String *response;

   /*
    *	get the status
    */
   response = transport->recvline(instanceConnection->fd);
   if (!response)
      return NULL;

   WOLog(WO_INFO,"New response: %s", response->text);
   resp = resp_new(response->text, instHandle, instanceConnection);

   str_free(response);
   if (resp == NULL)
      return NULL;
   
   /*
    *	followed by the headers...
    */
   while ((response = transport->recvline(instanceConnection->fd)) != NULL) {
      if (response->length == 0)
         break;
      resp_addHeader(resp, response);
   }
   if (response)
      str_free(response);
   else {
      /* recvline() must have failed */
      resp_free(resp);
      WOLog(WO_ERR, "Error receiving headers - response dropped");
      return NULL;
   }
   return resp;
}

int resp_getResponseContent(HTTPResponse *resp, int allowStreaming)
{
   int ret = 0;
   if (resp->content_length) {
      int count, amountToRead;

      if (resp->content == NULL)
      {
         resp->content_buffer_size = resp->content_length;
         if (allowStreaming && (resp->content_buffer_size > RESPONSE_STREAMED_SIZE))
            resp->content_buffer_size = RESPONSE_STREAMED_SIZE;
         resp->content = WOMALLOC(resp->content_buffer_size);
      }
      amountToRead = resp->content_length - resp->content_read;
      if (amountToRead > resp->content_buffer_size)
         amountToRead = resp->content_buffer_size;
      count = transport->recvbytes(resp->instanceConnection->fd, resp->content, amountToRead);
      if (count != amountToRead) {
         WOLog(WO_ERR, "Error receiving content (expected %d bytes, got %d)", amountToRead, count);
         resp->content_valid = 0;
         ret = -1;
      } else {
         resp->content_read += amountToRead;
         resp->content_valid = amountToRead;
      }
   }
   return ret;
}



/*
 *	callback for repackaging
 */
static void resp_appendHeader(const char *key, const char *val, String *headers)
{
   str_append(headers, key);
   str_appendLiteral(headers, ": ");
   str_append(headers, val);
   str_appendLiteral(headers, "\r\n");
}

String *resp_packageHeaders(HTTPResponse *resp)
{
   String *result;

   result = str_create(NULL, 1000);
   if (result)
      st_perform(resp->headers, (st_perform_callback)resp_appendHeader, result);
   return result;
}
