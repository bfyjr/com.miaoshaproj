package org.example.validator;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ValidationResult {
    private boolean hasErrors=false;
    private Map<String,String> errMSgMap=new HashMap<>();

    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public Map<String, String> getErrMSgMap() {
        return errMSgMap;
    }

    public void setErrMSgMap(Map<String, String> errMSgMap) {
        this.errMSgMap = errMSgMap;
    }

    //通过通用的格式化字符串信息来获取错误结果的msg方法
    public String getErrorMsg(){
        return StringUtils.join(errMSgMap.values().toArray(),",");
    }
}
