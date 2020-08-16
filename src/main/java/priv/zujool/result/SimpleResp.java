package priv.zujool.result;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author zujool  At 2019/11/27 10:19
**/
@ToString
@Getter
@Setter
public class SimpleResp implements Resp {

    SimpleResp(){}


    private Integer code = -1;

    private String msg;

    private Object body;

}
