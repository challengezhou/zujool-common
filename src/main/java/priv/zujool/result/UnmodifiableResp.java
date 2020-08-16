package priv.zujool.result;

import lombok.Getter;
import lombok.ToString;

/**
 * 
 * @author zujool  At 2020/4/28 16:06
**/
@Getter
@ToString
class UnmodifiableResp implements Resp {

    public UnmodifiableResp(SimpleResp resp){
        this.code = resp.getCode();
        this.msg = resp.getMsg();
        this.body = resp.getBody();
    }

    private final Integer code;

    private final String msg;

    private final Object body;

    @Override
    public void setCode(Integer code) {
        throw new RuntimeException("Cached Resp object unmodifiable");
    }

    @Override
    public void setMsg(String msg) {
        throw new RuntimeException("Cached Resp object unmodifiable");
    }

    @Override
    public void setBody(Object body) {
        throw new RuntimeException("Cached Resp object unmodifiable");
    }
}
