package testutils.mock;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

public class MockResponse implements HttpServletResponse {

    private Hashtable responseHeaders = new Hashtable();
    private int status = 200;
    private String redirectedUrl;
    private List cookies = new ArrayList();
    private Locale locale;
    MockSerlvetOutputStream mockSerlvetOutputStream = new MockSerlvetOutputStream();
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public boolean containsHeader(String s) {
        return false;
    }

    public String encodeURL(String s) {
        if (s == null) return null;
        if (s.indexOf("http:") == 0 ) return s;
        if (s.indexOf("?") != -1) {
            return s.substring(0, s.indexOf("?")) + ";mockencoded=test" + s.substring(s.indexOf("?"), s.length());
        } else {
            return s.concat(";mockencoded=test");
        }
    }

    public String encodeRedirectURL(String s) {
        return encodeURL(s);
    }

    /**
     * @deprecated
     */
    public String encodeUrl(String s) {
        return encodeURL(s);
    }

    /**
     * @deprecated
     */
    public String encodeRedirectUrl(String s) {
        return encodeURL(s);
    }

    public void sendError(int i, String s) throws IOException {

    }

    public void sendError(int i) throws IOException {

    }

    public void sendRedirect(String s) throws IOException {
        redirectedUrl = s;
    }

    public void setDateHeader(String s, long l) {
        responseHeaders.put(s, l + "");
    }

    public void addDateHeader(String s, long l) {
        responseHeaders.put(s, l + "");
    }

    public void setHeader(String s, String s1) {
        responseHeaders.put(s, s1);
    }

    public void addHeader(String s, String s1) {
        responseHeaders.put(s, s1);
    }

    public void setIntHeader(String s, int i) {
        responseHeaders.put(s, i + "");
    }

    public void addIntHeader(String s, int i) {
        responseHeaders.put(s, i + "");
    }

    public void setStatus(int i) {
        status = i;
    }

    /**
     * @deprecated
     */
    public void setStatus(int i, String s) {

    }

    public String getCharacterEncoding() {
        return null;
    }

    public String getContentType() {
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return mockSerlvetOutputStream;
    }

    public String getOutputStreamAsString() {
        return mockSerlvetOutputStream.getAsString();
    }

    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    public String getWriterAsString() {
        writer.flush();
        return stringWriter.toString();
    }

    public void setCharacterEncoding(String s) {

    }

    public void setContentLength(int i) {

    }

    public void setContentType(String s) {

    }

    public void setBufferSize(int i) {

    }

    public int getBufferSize() {
        return 0;
    }

    public void flushBuffer() throws IOException {

    }

    public void resetBuffer() {

    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {

    }

    public void setLocale(Locale l) {
        locale = l;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getHeader(String s) {
        return (String) responseHeaders.get(s);
    }

    public int getStatus() {
        return status;
    }

    public String getRedirectedUrl() {
        return redirectedUrl;
    }

    public List getCookies() {
        return cookies;
    }

    @Override
    public void setContentLengthLong(long arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Collection<String> getHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getHeaders(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}

class MockSerlvetOutputStream extends ServletOutputStream {

    ByteArrayOutputStream baos;
    WriteListener writeListener;

    public MockSerlvetOutputStream() {
        this.baos = new ByteArrayOutputStream();
    }

    public void write(int b) throws IOException {
        baos.write(b);
    }

    public String getAsString() {
        return new String(baos.toByteArray());
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener arg0) {
        writeListener = arg0;
    }

}