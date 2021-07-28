package org.example.service;

import org.example.dataobject.UserDO;
import org.example.error.BusinessException;
import org.example.service.Model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public interface UserService {
    public UserModel getUserById(Integer id);
    public void register(UserModel userModel) throws BusinessException;
    UserModel getUserByIdInCache(Integer userId);
    public UserModel validateLogin(String telphone,String encrptPassword) throws BusinessException;
}
