package com.mcl.service;

import com.mcl.RpcService;

/**
* @Description:    普通业务窗口，提供普通业务服务
* @Author:         MaiChengLin
* @CreateDate:     2019/1/21 9:38
* @UpdateUser:     MaiChengLin
* @UpdateDate:     2019/1/21 9:38
* @UpdateRemark:   修改内容
* @Version:        1.0
*/
@RpcService(Service.class) // 指定远程接口
public class ComServiceImpl implements Service {
 
    @Override
    public String provideService(String type) {
        return "complete "+type+" business! ";
    }
}