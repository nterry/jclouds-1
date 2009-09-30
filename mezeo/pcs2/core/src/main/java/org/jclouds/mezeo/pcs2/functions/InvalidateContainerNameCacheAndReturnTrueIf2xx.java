package org.jclouds.mezeo.pcs2.functions;

import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.rest.RestContext;

import com.google.common.base.Function;

/**
 * invalidates cache and returns true when the http response code is in the range 200-299.
 * 
 * @author Adrian Cole
 */
public class InvalidateContainerNameCacheAndReturnTrueIf2xx implements Function<HttpResponse, Boolean>,
         RestContext {
   private final ConcurrentMap<String, String> cache;
   private HttpRequest request;
   private Object[] args;

   @Inject
   public InvalidateContainerNameCacheAndReturnTrueIf2xx(ConcurrentMap<String, String> cache) {
      this.cache = cache;
   }

   public Boolean apply(HttpResponse from) {
      IOUtils.closeQuietly(from.getContent());
      int code = from.getStatusCode();
      if (code >= 300 || code < 200) {
         throw new IllegalStateException("incorrect code for this operation: " + from);
      }
      cache.remove(getArgs()[0]);
      return true;
   }

   public Object[] getArgs() {
      return args;
   }

   public HttpRequest getRequest() {
      return request;
   }

   public void setContext(HttpRequest request, Object[] args) {
      this.args = args;
      this.request = request;
   }

}