package org.example.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.security.MD5Encoder;
import org.example.controller.viewobject.UserVO;
import org.example.error.BusinessException;
import org.example.error.EnumBusinessError;
import org.example.response.CommonReturnType;
import org.example.service.Model.UserModel;
import org.example.service.UserServiceImp;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*",originPatterns ="*")
public class UserController {

    public final static String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";

    @Autowired
    UserServiceImp userServiceImp;
    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    RedisTemplate redisTemplate;

    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id")Integer id) throws BusinessException {
        UserModel userById = userServiceImp.getUserById(id);
        if(userById==null){
            throw new BusinessException(EnumBusinessError.USER_NOT_EXIST);
//            userById.setEncrptPassword("dff");
        }
        UserVO userVO = convertFromUserModel(userById);
        return CommonReturnType.create(userVO);
    }

    public UserVO convertFromUserModel(UserModel model){
        if(model==null){
            return null;
        }
        UserVO userVO=new UserVO();
        BeanUtils.copyProperties(model,userVO);
        return userVO;
    }

    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam("telphone")String telphone){
        //按照一定规则生成OTP验证码
        Random random=new Random();
        int i = random.nextInt(99999);
        i+=10000;
        String otpCode=String.valueOf(i);
        httpServletRequest.getSession().setAttribute(telphone,otpCode);
        System.out.println("telphone="+telphone+" "+"otpCode:"+otpCode);

        //将otp验证码和用户手机关联,使用HttpSSession方式，实际上分布式一般使用Redis
        return CommonReturnType.create("dfs","success");
    }


    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam("telphone")String telphone,
                                     @RequestParam("otpCode")String otpCode,
                                     @RequestParam("name")String name,
                                     @RequestParam("gender")Integer gender,
                                     @RequestParam("age")Integer age,
                                     @RequestParam("password")String password) throws Exception {
        //验证手机号和对应的otpCode相符合
        String sessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        if(!com.alibaba.druid.util.StringUtils.equals(otpCode,sessionOtpCode)){
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"验证码错误");
        }
        //进入用户注册流程
        UserModel userModel=new UserModel();
        userModel.setName(name);
        userModel.setAge(age);
        userModel.setGender(gender);
        userModel.setTelphone(telphone);
        userModel.setEncrptPassword(encodeByMD5(password));
        userServiceImp.register(userModel);

        return CommonReturnType.create("注册成功","success");

    }

    public String encodeByMD5(String s) throws Exception {
        MessageDigest md5=MessageDigest.getInstance("MD5");
        return new String(Base64Coder.encode(md5.digest(s.getBytes("utf-8"))));
    }

    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam("telphone")String telphone,
                                  @RequestParam("password")String password) throws Exception {
        //校验非空
        if(StringUtils.isEmpty(telphone)||StringUtils.isEmpty(password)){
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        UserModel model = userServiceImp.validateLogin(telphone, encodeByMD5(password));
        //将登陆凭证加入session

        //基于token,修改成若登陆验证成功，将登录信息和凭证一起放入redis
        //生成登陆凭证
        String token= UUID.randomUUID().toString();
        token=token.replace("-","");
        //建立登录状态和token之间的联系

        redisTemplate.opsForValue().set(token,model);
        redisTemplate.expire(token,1, TimeUnit.HOURS);

//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",model);

        return CommonReturnType.create(token);

    }

}
