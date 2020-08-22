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
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * http模拟请求等工具
 * 默认连接，读取超时3000ms
 *
 * @author zujool on 2015/10/20.
 */
@Slf4j
public class OkHttpUtils {

    private static final Random REQUEST_ID_GENERATOR = new Random();

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_XML = MediaType.parse("application/xml; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int WRITE_TIMEOUT = 0;
    private static final int READ_TIMEOUT = 0;


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
        Response response = null;
        try {
            response = defaultClient().newCall(request).execute();
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
        }finally {
            if (null != response){
                response.close();
            }
        }
    }

    private static OkHttpClient defaultClient() {
        return getClientWithParams(CONNECT_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, null);
    }

    /**
     * get请求
     *
     * @param url 请求的链接
     * @return 响应字符串
     */
    public static String getResult(String url) throws IOException {
        return getResultWithClient(url, defaultClient());
    }

    /**
     * json格式数据的post请求
     *
     * @param url  链接
     * @param json json字符串
     * @return 响应字符串
     */
    public static String postJson(String url, String json) throws IOException {
        return postJsonWithClient(url, json, defaultClient());
    }

    /**
     * post请求
     *
     * @param url    链接
     * @param params 参数 map
     * @return 响应字符串
     */
    public static String postForm(String url, Map<String, String> params) throws IOException {
        return postFormWithClient(url, params, defaultClient());
    }

    public static String postMultipart(String url, List<FileBody> fileBodies, Map<String, String> dataMap) throws IOException {
        return postMultipartWithClient(url, fileBodies, dataMap, defaultClient());
    }

    public static String postMultipartWithClient(String url, List<FileBody> fileBodies, Map<String, String> dataMap, OkHttpClient okHttpClient) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        StringBuilder sb = new StringBuilder();
        if (null != fileBodies) {
            for (FileBody fileBody : fileBodies) {
                RequestBody requestBody = RequestBody.create(MediaType.parse(fileBody.getMediaType()), fileBody.fileBytes);
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
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        return processCall(okHttpClient, request, sb.toString());
    }

    /**
     * get请求
     *
     * @param url 请求的链接
     * @return 响应字符串
     */
    public static String getResultWithClient(String url, OkHttpClient okHttpClient) throws IOException {
        Request request = new Request.Builder().url(url).build();
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        return processCall(okHttpClient, request, null);
    }

    /**
     * post请求
     *
     * @param url    链接
     * @param params 参数 map
     * @return 响应字符串
     */
    public static String postFormWithClient(String url, Map<String, String> params, OkHttpClient okHttpClient) throws IOException {
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
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        return processCall(okHttpClient, request, sb.toString());
    }

    /**
     * json格式数据的post请求
     *
     * @param url  链接
     * @param json json字符串
     * @return 响应字符串
     */
    public static String postJsonWithClient(String url, String json, OkHttpClient okHttpClient) throws IOException {
        return postBodyWithClient(url, json, MEDIA_TYPE_JSON, okHttpClient);
    }

    /**
     * xml格式数据的post请求
     *
     * @param url 链接
     * @param xml xml字符串
     * @return 响应字符串
     */
    public static String postXmlWithClient(String url, String xml, OkHttpClient okHttpClient) throws IOException {
        return postBodyWithClient(url, xml, MEDIA_TYPE_XML, okHttpClient);
    }

    private static String postBodyWithClient(String url, String body, MediaType mediaType, OkHttpClient okHttpClient) throws IOException {
        if (null == okHttpClient) {
            okHttpClient = CLIENT;
        }
        RequestBody requestBody = RequestBody.create(mediaType, body);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        return processCall(okHttpClient, request, body);
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
            log.info("==> Request [{}] to [{}][{}],METHOD:{}", method, url, reqId);
        }
        Response response = null;
        try {
            response = client.newCall(request).execute();
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
                log.info("==> Response [{}] is \n{}", reqId, resp);
            } else {
                log.info("==> Response [{}] size {}", reqId, resp.length());
            }
            return resp;
        } catch (IOException e) {
            log.error("==> Error when process [{}][{}],msg:{}", url, reqId, e.getMessage());
            throw e;
        } finally {
            if (null != response) {
                response.close();
            }
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
        return Optional.ofNullable(response)
                .map(Response::body)
                .map(ResponseBody::byteStream)
                .orElseThrow(() -> new RuntimeException("InputStream is null"));
    }

    private static String getRequestId() {
        return "" + REQUEST_ID_GENERATOR.nextInt(Integer.MAX_VALUE);
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
