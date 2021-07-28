package org.example.service;

import org.apache.commons.lang3.StringUtils;
import org.example.dao.UserDOMapper;
import org.example.dao.UserPasswordMapper;
import org.example.dataobject.UserDO;
import org.example.dataobject.UserPassword;
import org.example.error.BusinessException;
import org.example.error.EnumBusinessError;
import org.example.service.Model.UserModel;
import org.example.validator.ValidationResult;
import org.example.validator.ValidatorImplement;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImp implements UserService{
    @Autowired
    UserDOMapper userDOMapper;
    @Autowired
    UserPasswordMapper userPasswordMapper;
    @Autowired
    ValidatorImplement validatorImplement;
    @Autowired
    RedisTemplate redisTemplate;
    @Override
    public UserModel getUserById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if(userDO==null) return null;
//        通过用户id获取加密后的密码信息
        UserPassword userPassword = userPasswordMapper.selectByUserId(id);
        UserModel model = convertFromData(userDO, userPassword);
        return  model;


    }

    @Transactional
    @Override
    public void register(UserModel userModel) throws BusinessException {
        if(userModel==null){
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"验证码错误");
        }
//        if(StringUtils.isEmpty(userModel.getName())||
//        userModel.getGender()==null||
//        userModel.getAge()==null||StringUtils.isEmpty(userModel.getTelphone())){
//            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR);
//        }
        ValidationResult validateResult = validatorImplement.validate(userModel);
        if(validateResult.isHasErrors()){
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,validateResult.getErrorMsg());
        }
        UserDO userDO=convertFromModel(userModel);

        try {
            userDOMapper.insertSelective(userDO);

        }catch (DuplicateKeyException e){
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR.setErrorMsg("手机号已经存在"));
        }


        userModel.setId(userDO.getId());
        UserPassword userPassword=convertPasswordFromModel(userModel);
        userPasswordMapper.insertSelective(userPassword);


    }

    @Override
    public UserModel getUserByIdInCache(Integer userId) {
        UserModel userModel= (UserModel) redisTemplate.opsForValue().get("user_validate_"+userId);
        if(userModel==null)
        {
            userModel=this.getUserById(userId);
            redisTemplate.opsForValue().set("user_validate_"+userId,userModel);
            redisTemplate.expire("user_validate_"+userId,10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    @Override
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException {
        //通过用户的手机获取用户信息，比对加密的密码
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if(userDO==null)
        {
            throw new BusinessException(EnumBusinessError.USER_LOGIN_FAIL);
        }
        UserPassword userPassword = userPasswordMapper.selectByUserId(userDO.getId());
        UserModel model = convertFromData(userDO, userPassword);
        if(!StringUtils.equals(encrptPassword,model.getEncrptPassword())){
            throw new BusinessException(EnumBusinessError.USER_LOGIN_FAIL);
        }
        return model;
    }

    private UserDO convertFromModel(UserModel userModel){
        if(userModel==null) return null;
        UserDO userDO=new UserDO();
        BeanUtils.copyProperties(userModel,userDO);
        return userDO;
    }
    private UserPassword convertPasswordFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserPassword userPassword=new UserPassword();
        userPassword.setEncrptPassword(userModel.getEncrptPassword());
        userPassword.setUserId(userModel.getId());
        return userPassword;
    }

    private UserModel convertFromData(UserDO userDO, UserPassword userPassword){
        if(userDO==null) return null;
        UserModel model=new UserModel();
        BeanUtils.copyProperties(userDO,model);
        if(userPassword!=null){
            model.setEncrptPassword(userPassword.getEncrptPassword());
        }
        return model;
    }
}
