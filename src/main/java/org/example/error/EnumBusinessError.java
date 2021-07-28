package org.example.error;

public enum EnumBusinessError implements CommonError{
    USER_NOT_EXIST(10001,"用户不存在"),
    UNKNOWN_ERROR(20001,"未知错误"),
    USER_LOGIN_FAIL(20002,"用户名或者密码错误"),
    USER_NOT_LOGIN(20003,"用户名未登录"),
    PARAMETER_VALIDATION_ERROR(30001,"参数不合法"),
    STOCK_NOT_ENOUGH(40001,"库存不足"),
    MQ_SEND_FAIL(40002,"库存异步消息发送失败");

    private int errCode;
    private String errMsg;

    private EnumBusinessError(int errCode,String errMsg){
        this.errCode=errCode;
        this.errMsg=errMsg;
    }
    @Override
    public int getErrorCode() {
        return this.errCode;
    }

    @Override
    public String getErrorMsg() {
        return this.errMsg;
    }

    @Override
    public EnumBusinessError setErrorMsg(String errMsg) {
        this.errMsg=errMsg;
        return this;
    }
}
