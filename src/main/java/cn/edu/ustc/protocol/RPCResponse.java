package cn.edu.ustc.protocol;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class RPCResponse extends MyProtocolMessage{
    public RPCResponse(RPCRequest rpcRequest) {
        super.setId(rpcRequest.getId());
    }


    /**
     * 返回值
     */
    private Object returnValue;
    /**
     * 异常值
     */
    private Exception exceptionValue;

    @Override
    public int getMessageType() {
        return RPCReponse;
    }
}
