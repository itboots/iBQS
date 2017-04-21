package com.bq.shuo.core.base;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.bq.core.Constants;
import com.bq.core.util.ExceptionUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;

public abstract class BaseProviderImpl implements ApplicationContextAware, BaseProvider {
    protected static Logger logger = LogManager.getLogger();
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Parameter execute(Parameter parameter) {
        String no = parameter.getNo();
        logger.info("{} request：{}", no, JSON.toJSONString(parameter));
        Object service = applicationContext.getBean(parameter.getService());
        try {
            String id = parameter.getId();
            BaseModel model = parameter.getModel();
            List<?> list = parameter.getList();
            Map<?, ?> map = parameter.getMap();
            Object[] objects = parameter.getObjects();
            Object result = null;
            MethodAccess methodAccess = MethodAccess.get(service.getClass());
            if (id != null) {
                result = methodAccess.invoke(service, parameter.getMethod(), parameter.getId());
            } else if (model != null) {
                result = methodAccess.invoke(service, parameter.getMethod(), parameter.getModel());
            } else if (list != null) {
                result = methodAccess.invoke(service, parameter.getMethod(), parameter.getList());
            } else if (map != null) {
                result = methodAccess.invoke(service, parameter.getMethod(), parameter.getMap());;
            } else if (objects != null) {
                result = methodAccess.invoke(service, parameter.getMethod(), parameter.getObjects());
            } else {
                result = methodAccess.invoke(service, parameter.getMethod());
            }
            if (result != null) {
                Parameter response = new Parameter(result);
                logger.info("{} response：{}", no, JSON.toJSONString(response));
                return response;
            }
            logger.info("{} response empty.", no);
            return new Parameter();
        } catch (Exception e) {
            String msg = ExceptionUtil.getStackTraceAsString(e);
            logger.error(no + " " + Constants.Exception_Head + msg, e);
            throw e;
        }
    }
}
