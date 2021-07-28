package org.example.error;

import org.example.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public Object handleBusinessException(HttpServletRequest request,Exception e){
        Map<String,Object> responseMap=new HashMap<>();
        if(e instanceof BusinessException){
            BusinessException businessException= (BusinessException) e;
            responseMap.put("errCode",businessException.getErrorCode());
            responseMap.put("errMsg",businessException.getErrorMsg());
        }else if(e instanceof ServletRequestBindingException){
            responseMap.put("errCode",EnumBusinessError.UNKNOWN_ERROR.getErrorCode());
            responseMap.put("errMsg","路由绑定问题，参数错误");
        }else if(e instanceof NoHandlerFoundException){
            responseMap.put("errCode",EnumBusinessError.UNKNOWN_ERROR.getErrorCode());
            responseMap.put("errMsg","没有对应访问路径");
        }
        else{
            responseMap.put("errCode",EnumBusinessError.UNKNOWN_ERROR.getErrorCode());
            responseMap.put("errMsg",EnumBusinessError.UNKNOWN_ERROR.getErrorMsg());
        }

        return CommonReturnType.create(responseMap,"fail");
    }
}
