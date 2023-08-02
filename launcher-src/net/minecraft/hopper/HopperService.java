package net.minecraft.hopper;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public final class HopperService {

	private static final String BASE_URL = "http://hopper.minecraft.net/crashes/";

	private static final URL ROUTE_SUBMIT = Util.constantURL(BASE_URL + "submit_report/");

	private static final URL ROUTE_PUBLISH = Util.constantURL(BASE_URL + "publish_report/");

	private static final String[] INTERESTING_SYSTEM_PROPERTY_KEYS = new String[] { "os.version", "os.name", "os.arch", "java.version", "java.vendor", "sun.arch.data.model" };

	private static final Gson GSON = new Gson();

	public static SubmitResponse submitReport(Proxy proxy, String report, String product, String version) throws IOException {
		
		return submitReport(proxy, report, product, version, null);
	}

	public static SubmitResponse submitReport(Proxy proxy, String report, String product, String version, Map<String, String> env) throws IOException {
		
		Map<String, String> environment = new HashMap<String, String>();
		
		if (env != null) {
			
			environment.putAll(env); 
		}
		
		for (String key : INTERESTING_SYSTEM_PROPERTY_KEYS) {
		
			String value = System.getProperty(key);
			
			if (value != null) environment.put(key, value); 
		}
		
		SubmitRequest request = new SubmitRequest(report, product, version, environment);
		
		return makeRequest(proxy, ROUTE_SUBMIT, request, SubmitResponse.class);
	}

	public static PublishResponse publishReport(Proxy proxy, Report report) throws IOException {
		
		PublishRequest request = new PublishRequest(report);
		
		return makeRequest(proxy, ROUTE_PUBLISH, request, PublishResponse.class);
	}

	private static <T extends Response> T makeRequest(Proxy proxy, URL url, Object input, Class<T> classOfT) throws IOException {
		
		String jsonResult = Util.performPost(url, GSON.toJson(input), proxy, "application/json", true);
		
		Response response = (Response)GSON.fromJson(jsonResult, classOfT);
		
		if (response == null) return null; 
		
		if (response.getError() != null) throw new IOException(response.getError()); 
		
		return (T)response;
	}
}
