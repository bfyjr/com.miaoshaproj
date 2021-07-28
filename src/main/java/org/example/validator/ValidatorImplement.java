package org.example.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

@Component
public class ValidatorImplement implements InitializingBean {

    private Validator validator;

    //实现校验方法并返回校验结果
    public ValidationResult validate(Object bean){
        final ValidationResult result=new ValidationResult();
        Set<ConstraintViolation<Object>> validate = validator.validate(bean);
        if(validate.size()>0){
            result.setHasErrors(true);
            validate.forEach(item->{
                String errMsg=item.getMessage();
                String propertyName=item.getPropertyPath().toString();
                result.getErrMSgMap().put(propertyName,errMsg);
            });
        }
    return result;
    }

    //初始化Bean之后会回调这个方法
    @Override
    public void afterPropertiesSet() throws Exception {
        this.validator= Validation.buildDefaultValidatorFactory().getValidator();
    }
}
