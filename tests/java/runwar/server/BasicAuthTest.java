package runwar.server;

import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.BASIC;
import static io.undertow.util.Headers.WWW_AUTHENTICATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.undertow.util.FlexBase64;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import testutils.DefaultServer;
import testutils.HttpClientUtils;
import testutils.TestHttpClient;

@RunWith(DefaultServer.class)
public class BasicAuthTest {

    private static final HashMap<String, String> users;
    static
    {
        users = new HashMap<String, String>();
        users.put("user1", "password1");
        users.put("bob", "password");
        users.put("alice", "12345");
        users.put("charsetUser", "password-ü");
    }
    
    @BeforeClass
    public static void beforeClass() {
        DefaultServer.getServerOptions().setBasicAuth(users);
        DefaultServer.getServerOptions().setEnableBasicAuth(true);
    }

    @Test
    public void testChallengeSent() throws Exception {
        TestHttpClient client = new TestHttpClient();
        String url = DefaultServer.getDefaultServerURL() + "/dumprunwarrequest";
        HttpGet get = new HttpGet(url);
        HttpResponse result = client.execute(get);
        HttpClientUtils.readResponse(result);
        assertEquals(StatusCodes.UNAUTHORIZED, result.getStatusLine().getStatusCode());
        Header[] values = result.getHeaders(WWW_AUTHENTICATE.toString());
        assertEquals(1, values.length);
        String value = values[0].getValue();
        assertTrue(value.startsWith("Basic"));
    }

    @Test
    public void testUserName() throws Exception {
        testCall("principle", "user1", StandardCharsets.UTF_8, "Chrome", "user1", "password1", 200);
    }

    @Test
    public void testAuthType() throws Exception {
        testCall("authType", "BASIC", StandardCharsets.UTF_8, "Chrome", "user1", "password1", 200);
    }

    @Test
    public void testBasicAuthNonAscii() throws Exception {
        testCall("authType", "BASIC", StandardCharsets.UTF_8, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36", "charsetUser", "password-ü", 200);
        testCall("authType", "BASIC", StandardCharsets.ISO_8859_1, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36", "charsetUser", "password-ü", 401);
    }

    public void testCall(final String path, final String expectedResponse, Charset charset, String userAgent, String user, String password, int expect) throws Exception {
        TestHttpClient client = new TestHttpClient();
        try {
            String url = DefaultServer.getDefaultServerURL() + "/dumprunwarrequest?userpath=" + path;
            HttpGet get = new HttpGet(url);
            get = new HttpGet(url);
            get.addHeader(Headers.USER_AGENT_STRING, userAgent);
            get.addHeader(AUTHORIZATION.toString(), BASIC + " " + FlexBase64.encodeString((user + ":" + password).getBytes(charset), false));
            HttpResponse result = client.execute(get);
            assertEquals(expect, result.getStatusLine().getStatusCode());

            final String response = HttpClientUtils.readResponse(result);
            System.out.println(response);
            if(expect == 200) {
                JSONObject responseData = (JSONObject) JSONValue.parse(response);
                String authType = responseData.get(path)!=null?responseData.get(path).toString():"";
                String authHeader = ((JSONObject) responseData.get("headers")).get(Headers.AUTHORIZATION_STRING).toString();
                assertEquals(expectedResponse, authType);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
}
}