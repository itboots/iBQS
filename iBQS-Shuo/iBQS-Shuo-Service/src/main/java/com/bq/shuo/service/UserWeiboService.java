package com.bq.shuo.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.bq.core.Constants;
import com.bq.core.util.InstanceUtil;
import com.bq.shuo.mapper.UserWeiboMapper;
import com.bq.shuo.model.User;
import com.bq.shuo.model.UserWeibo;
import com.bq.shuo.core.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *   服务实现类
 * </p>
 *
 * @author Harvey.Wei
 * @since 2017-04-13
 */
@Service
@CacheConfig(cacheNames = Constants.CACHE_SHUO_NAMESPACE+"userWeibo")
public class UserWeiboService extends BaseService<UserWeibo> {
    @Autowired
    private UserWeiboMapper userWeiboMapper;

    public Page<UserWeibo> queryBeans(Map<String, Object> params) {
        Page<String> idPage = this.getPage(params);
        idPage.setRecords(mapper.selectIdPage(idPage, params));
        Page<UserWeibo> page = getPage(idPage);
        return page;
    }

    public UserWeibo queryByOpenId(String openId,String userId) {
        return queryById(userWeiboMapper.queryByOpenId(openId,userId));
    }

    public Page<UserWeibo> queryFollow(Map<String,Object> params) {
        Page<String> idPage = this.getPage(params);
        idPage.setRecords(userWeiboMapper.selectIdPageByFollow(idPage, params));
        Page<UserWeibo> page = getPage(idPage);
        return page;
    }


}