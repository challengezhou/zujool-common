package priv.zujool.result;


/**
 * @author zujool  At 2019/11/27 10:19
 **/
public interface Resp {

    Resp SUCCESS_RESP = new UnmodifiableResp(success());
    Resp FAILED_RESP = new UnmodifiableResp(failed());

    /**
     * 获取错误码
     *
     * @return code
     */
    Integer getCode();

    /**
     * 获取提示消息
     *
     * @return msg
     */
    String getMsg();

    /**
     * 获取内容实体
     *
     * @return object
     */
    Object getBody();

    /**
     * setter
     *
     * @param code code
     */
    void setCode(Integer code);

    /**
     * setter
     *
     * @param msg msg
     */
    void setMsg(String msg);

    /**
     * setter
     *
     * @param body body
     */
    void setBody(Object body);

    /**
     * 当你明确知道body的类型，而且需要body事调用此方法
     * @param <T> 返回类型
     * @return 参数类型
     */
    @SuppressWarnings("unchecked")
    default <T> T cast(){
        Object body = getBody();
        if (null == body){
            throw new NullPointerException("Resp body can not be null");
        }
        return (T)body;
    }

    /**
     * custom build
     *
     * @param code code
     * @param msg  msg
     * @param body body
     * @return simpleResp
     */
    static SimpleResp build(Integer code, String msg, Object body) {
        SimpleResp simpleResp = new SimpleResp();
        simpleResp.setCode(code);
        simpleResp.setMsg(msg);
        simpleResp.setBody(body);
        return simpleResp;
    }

    /**
     * custom
     *
     * @return simpleResp
     */
    static SimpleResp failed() {
        return build(-1, "failed", null);
    }

    /**
     * custom
     *
     * @param msg msg
     * @return simpleResp
     */
    static SimpleResp failed(String msg) {
        return build(-1, msg, null);
    }

    /**
     * custom
     *
     * @param code code
     * @param msg  msg
     * @return simpleResp
     */
    static SimpleResp failed(Integer code, String msg) {
        return build(code, msg, null);
    }

    /**
     * custom
     *
     * @param body body
     * @return simpleResp
     */
    static SimpleResp failedBody(Object body) {
        return build(-1, "failed", body);
    }

    /**
     * custom
     *
     * @return simpleResp
     */
    static SimpleResp success() {
        return build(0, "success", null);
    }

    /**
     * custom
     *
     * @param msg msg
     * @return simpleResp
     */
    static SimpleResp success(String msg) {
        return build(0, msg, null);
    }

    /**
     * custom
     *
     * @param body body
     * @return simpleResp
     */
    static SimpleResp successBody(Object body) {
        return build(0, "success", body);
    }

}
