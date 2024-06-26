package priv.zujool.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * http模拟请求等工具
 * 默认连接，读取超时3000ms
 *
 * @author zujool on 2015/10/20.
 */
@Slf4j
public class OkHttpUtils {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_XML = MediaType.parse("application/xml; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int WRITE_TIMEOUT = 0;
    private static final int READ_TIMEOUT = 0;

    private static final GlobalConfig globalConfig = new GlobalConfig();

    @Getter
    @Setter
    private static class GlobalConfig {
        private Proxy proxy = null;
    }

    public static void globalSetProxy(Proxy proxy) {
        globalConfig.proxy = proxy;
    }

    @Setter
    @Getter
    private static class ReqContext {
        boolean logRequestParam = true;
        boolean logResult = true;
    }

    private static final ThreadLocal<ReqContext> REQ_CONTEXT_PROVIDER = ThreadLocal.withInitial(ReqContext::new);

    public static void setLogResult(boolean logResult) {
        REQ_CONTEXT_PROVIDER.get().setLogResult(logResult);
    }

    public static byte[] getFileBytes(String url) {
        String reqId = getRequestId();
        log.info("==> Request getFile [{}][req_id:{}]", url, reqId);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();
        try (Response response = defaultClient().newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            byte[] bytes = body.bytes();
            log.info("==> Response [req_id:{}] size {}", reqId, bytes.length);
            return bytes;
        } catch (IOException e) {
            log.error("==> getFileBytes [{}] error", url, e);
            return null;
        }
    }

    private static OkHttpClient defaultClient() {
        return getClientWithParams(CONNECT_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, globalConfig.proxy);
    }

    /**
     * get请求
     *
     * @param url 请求的链接
     * @return 响应字符串
     */
    public static String getResult(String url) throws IOException {
        return getResultWithClient(url, null, defaultClient());
    }

    public static String getResult(String url, Map<String, String> headers) throws IOException {
        return getResultWithClient(url, headers, defaultClient());
    }

    /**
     * json格式数据的post请求
     *
     * @param url  链接
     * @param json json字符串
     * @return 响应字符串
     */
    public static String postJson(String url, String json) throws IOException {
        return postJsonWithClient(url, json, null, defaultClient());
    }

    public static String postJson(String url, String json, Map<String, String> headers) throws IOException {
        return postJsonWithClient(url, json, headers, defaultClient());
    }

    /**
     * post请求
     *
     * @param url    链接
     * @param params 参数 map
     * @return 响应字符串
     */
    public static String postForm(String url, Map<String, String> params) throws IOException {
        return postFormWithClient(url, params, null, defaultClient());
    }

    public static String postForm(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        return postFormWithClient(url, params, headers, defaultClient());
    }

    public static String postMultipart(String url, List<FileBody> fileBodies, Map<String, String> dataMap) throws IOException {
        return postMultipartWithClient(url, fileBodies, dataMap, null, defaultClient());
    }

    public static String postMultipart(String url, List<FileBody> fileBodies, Map<String, String> dataMap, Map<String, String> headers) throws IOException {
        return postMultipartWithClient(url, fileBodies, dataMap, headers, defaultClient());
    }

    public static String postMultipartWithClient(String url, List<FileBody> fileBodies, Map<String, String> dataMap, Map<String, String> headers, OkHttpClient okHttpClient) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        StringBuilder sb = new StringBuilder();
        if (null != fileBodies) {
            for (FileBody fileBody : fileBodies) {
                RequestBody requestBody = RequestBody.create(fileBody.fileBytes, MediaType.parse(fileBody.getMediaType()));
                builder.addFormDataPart(fileBody.name, fileBody.fileName, requestBody);
                sb.append("[file]").append(fileBody.name).append(":").append(fileBody.fileName).append(",");
            }
        }
        if (null != dataMap) {
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (null != value) {
                    builder.addFormDataPart(key, value);
                    sb.append(key).append("=").append(value).append(",");
                }
            }
        }
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(builder.build());
        if (null != headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        return processCall(okHttpClient, requestBuilder.build(), sb.toString());
    }

    /**
     * get请求
     *
     * @param url 请求的链接
     * @return 响应字符串
     */
    public static String getResultWithClient(String url, Map<String, String> headers, OkHttpClient okHttpClient) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);
        if (null != headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        return processCall(okHttpClient, requestBuilder.build(), null);
    }

    public static String getResultWithClient(String url, OkHttpClient okHttpClient) throws IOException {
        return getResultWithClient(url, null, okHttpClient);
    }

    /**
     * post请求
     *
     * @param url    链接
     * @param params 参数 map
     * @return 响应字符串
     */
    public static String postFormWithClient(String url, Map<String, String> params, Map<String, String> headers, OkHttpClient okHttpClient) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        StringBuilder sb = new StringBuilder();
        if (null != params) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue();
                builder.add(key, value);
                sb.append(key).append("=").append(value).append(",");
            }
        }
        RequestBody body = builder.build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);
        if (null != headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        return processCall(okHttpClient, requestBuilder.build(), sb.toString());
    }

    public static String postFormWithClient(String url, Map<String, String> params, OkHttpClient okHttpClient) throws IOException {
        return postFormWithClient(url, params, null, okHttpClient);
    }

    /**
     * json格式数据的post请求
     *
     * @param url  链接
     * @param json json字符串
     * @return 响应字符串
     */
    public static String postJsonWithClient(String url, String json, Map<String, String> headers, OkHttpClient okHttpClient) throws IOException {
        return postBodyWithClient(url, json, MEDIA_TYPE_JSON, headers, okHttpClient);
    }

    public static String postJsonWithClient(String url, String json, OkHttpClient okHttpClient) throws IOException {
        return postBodyWithClient(url, json, MEDIA_TYPE_JSON, null, okHttpClient);
    }

    /**
     * xml格式数据的post请求
     *
     * @param url 链接
     * @param xml xml字符串
     * @return 响应字符串
     */
    public static String postXmlWithClient(String url, String xml, OkHttpClient okHttpClient) throws IOException {
        return postBodyWithClient(url, xml, MEDIA_TYPE_XML, null, okHttpClient);
    }

    public static String postXmlWithClient(String url, String xml, Map<String, String> headers, OkHttpClient okHttpClient) throws IOException {
        return postBodyWithClient(url, xml, MEDIA_TYPE_XML, headers, okHttpClient);
    }

    private static String postBodyWithClient(String url, String body, MediaType mediaType, Map<String, String> headers, OkHttpClient okHttpClient) throws IOException {
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        RequestBody requestBody = RequestBody.create(body, mediaType);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);
        if (null != headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }
        return processCall(okHttpClient, requestBuilder.build(), body);
    }

    private static String processCall(OkHttpClient client, Request request, String bodyStr) throws IOException {
        boolean logResult = REQ_CONTEXT_PROVIDER.get().isLogResult();
        boolean logRequestParam = REQ_CONTEXT_PROVIDER.get().isLogRequestParam();
        String reqId = getRequestId();
        HttpUrl url = request.url();
        String method = request.method();
        if (logRequestParam) {
            log.info("==> Request [{}] to [{}][{}],body:{}", method, url, reqId, "POST".equals(method) ? "\n" + bodyStr : "");
        } else {
            log.info("==> Request [{}] to [{}][{}]", method, url, reqId);
        }
        try (Response response = client.newCall(request).execute()) {
            String errorPrefix = "Unexpected code ";
            if (!response.isSuccessful()) {
                throw new IOException(errorPrefix + response);
            }
            ResponseBody body = response.body();
            if (body == null) {
                return "";
            }
            String resp = body.string();
            if (logResult) {
                log.info("==> Response [{}] is {}", reqId, resp);
            } else {
                log.info("==> Response [{}] size {}", reqId, resp.length());
            }
            return resp;
        } catch (IOException e) {
            log.error("==> Error when process [{}][{}],msg:{}", url, reqId, e.getMessage());
            throw e;
        } finally {
            REQ_CONTEXT_PROVIDER.get().setLogResult(true);
        }
    }

    /**
     * set timeout  in millis
     *
     * @param connTimeout  connTimeout
     * @param writeTimeout writeTimeout
     * @param readTimeout  readTimeout
     * @param proxy        proxy
     * @return OkHttpClient
     */
    public static OkHttpClient getClientWithParams(long connTimeout, long writeTimeout, long readTimeout, Proxy proxy) {
        OkHttpClient.Builder builder = CLIENT.newBuilder();
        builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
        builder.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        if (null != proxy) {
            builder.proxy(proxy);
        }
        return builder.build();
    }

    /**
     * all field one minute timeout
     *
     * @param proxy proxy
     * @return OkHttpClient
     */
    public static OkHttpClient oneMinuteTimeoutClient(Proxy proxy) {
        OkHttpClient.Builder builder = CLIENT.newBuilder();
        builder.connectTimeout(1, TimeUnit.MINUTES);
        builder.writeTimeout(1, TimeUnit.MINUTES);
        builder.readTimeout(1, TimeUnit.MINUTES);
        if (null != proxy) {
            builder.proxy(proxy);
        }
        return builder.build();
    }

    public static InputStream downLoadFile(String fileUrl, Proxy proxy) throws IOException {
        final Request request = new Request.Builder().url(fileUrl).build();
        final Call call = getClientWithParams(CONNECT_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, proxy).newCall(request);
        Response response = call.execute();
        return Optional.of(response)
                .map(Response::body)
                .map(ResponseBody::byteStream)
                .orElseThrow(() -> new RuntimeException("InputStream is null"));
    }

    private static String getRequestId() {
        return "" + System.currentTimeMillis();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileBody {

        private String name;

        private byte[] fileBytes;

        private String fileName;

        /**
         * true will override mediaType
         */
        private boolean mediaTypeFileNameDetect;

        private String mediaType;

        public String getMediaType() {
            String defaultMediaType = "application/octet-stream";
            if (mediaTypeFileNameDetect) {
                if (StringUtils.isBlank(fileName)) {
                    return defaultMediaType;
                }
                String fileSuffix = fileName.substring(fileName.lastIndexOf('.') + 1);
                if (StringUtils.isBlank(fileSuffix)) {
                    return defaultMediaType;
                } else {
                    switch (fileSuffix) {
                        case "png":
                            return "image/png";
                        case "jpg":
                        case "jpeg":
                            return "image/jpeg";
                        case "gif":
                            return "image/gif";
                        default:
                            return defaultMediaType;
                    }
                }
            } else {
                if (StringUtils.isBlank(mediaType)) {
                    return defaultMediaType;
                }
                return mediaType;
            }
        }
    }

}
