package com.ui4j.webkit.browser;

import static java.lang.String.join;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.Map;

import com.ui4j.api.interceptor.Interceptor;
import com.ui4j.api.interceptor.Request;

public class WebKitURLHandler extends URLStreamHandler {

    private String context;

    private static final String UI4J_PROTOCOL = "ui4j";

    private Interceptor interceptor;
    
    private URLConnection contextConnection;

    private CookieHandler cookieHandler;

    public WebKitURLHandler(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String protocol = u.getProtocol();

        if (!protocol.startsWith(UI4J_PROTOCOL)) {
            return null;
        }

        // url without ui4j prefix
        String url = u.toString().substring(protocol.length() + 1, u.toString().length());

        boolean isContext = false;
        if (context == null && url.startsWith("http")) {
            context = url;
            isContext = true;
        }

        if (context != null &&
                        !url.startsWith("http") &&
                            !url.startsWith("/") &&
                            !context.endsWith("/")) {
            String f = u.getFile().replaceAll("https://", "");
            url = context + "/" + f;
        }

        URLConnection connection = new URL(url).openConnection();

        if (isContext) {
            contextConnection = connection;
        }

        Request request = new Request(url);
        interceptor.beforeLoad(request);
        if (request != null) {
            for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
                String key = entry.getKey();
                String value = join(",", entry.getValue());
                connection.setRequestProperty(key, value);
            }
        }
        
        return connection;
    }

    public URLConnection getConnection() {
        return contextConnection;
    }

    public CookieHandler getCookieHandler() {
        return cookieHandler;
    }
}
