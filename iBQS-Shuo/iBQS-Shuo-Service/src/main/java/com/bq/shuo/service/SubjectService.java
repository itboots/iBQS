package com.bq.shuo.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.bq.core.Constants;
import com.bq.core.util.CacheUtil;
import com.bq.shuo.core.base.BaseService;
import com.bq.shuo.core.helper.CounterHelper;
import com.bq.shuo.mapper.SubjectMapper;
import com.bq.shuo.model.*;
import com.bq.shuo.support.SubjectHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 主题表  服务实现类
 * </p>
 *
 * @author Harvey.Wei
 * @since 2017-04-13
 */
@Service
@CacheConfig(cacheNames = Constants.CACHE_SHUO_NAMESPACE+"subject")
public class SubjectService extends BaseService<Subject> {
    @Autowired
    private UserService userService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private AlbumLikedService albumLikedService;

    @Autowired
    private SubjectLikedService subjectLikedService;

    @Autowired
    private UserFollowingService userFollowingService;

    @Autowired
    private ForwardService forwardService;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private DynamicService dynamicService;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private TopicsService topicsService;

    // 线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public Page<Subject> queryByHot(Map<String,Object> params) {
        Page<Subject> page = super.query(params);
        page.setRecords(getSubjectListInfo(page.getRecords(),params));
        return page;
    }

    public Page queryByNew(Map<String,Object> params) {
        Page<Subject> page = super.query(params);
        page.setRecords(getSubjectListInfo(page.getRecords(),params));
        return page;
    }

    public Page<Subject> queryBeans(Map<String, Object> params) {
        Page<Subject> page = super.query(params);
        page.setRecords(getSubjectListInfo(page.getRecords(),params));
        return page;
    }

    public Page<Subject> selectByKeyword(Map<String, Object> params) {
        Page<String> idPage = this.getPage(params);
        idPage.setRecords(subjectMapper.selectByKeyword(idPage,params));
        Page<Subject> page = getPage(idPage);
        getSubjectListInfo(page.getRecords(),params);
        return page;
    }



    public Integer selectCountByUserId(String userId) {
        return subjectMapper.selectCountByUserId(userId);
    }

    public Integer selectSubjectCounter(String subjectId,String field) {
        String key =  CounterHelper.Subject.SUBJECT_COUNTER_KEY+ subjectId;

        if (CacheUtil.getCache().hexists(key,field)) {
            String counter = (String) CacheUtil.getCache().hget(key,field);
            return Integer.parseInt(counter);
        }
        int counter = 0;
        if(StringUtils.equals(CounterHelper.Subject.VIEW,field)) {
            counter = subjectMapper.selectSubjectCounter(subjectId,field);
        } else if(StringUtils.equals(CounterHelper.Subject.FORWARD,field)) {
            counter = forwardService.selectCountBySubjectId(subjectId);
        } else if(StringUtils.equals(CounterHelper.Subject.COMMENTS,field)) {
            counter = commentsService.selectCountBySubjectId(subjectId);
        } else if(StringUtils.equals(CounterHelper.Subject.LIKED,field)) {
            counter = subjectLikedService.selectCountBySubjectId(subjectId);
        }
        CacheUtil.getCache().hset(key,field,String.valueOf(counter));
        return counter;
    }



    public void incrSubjectCounter(String subjectId, String field) {
        setSubjectCounter(subjectId,field,+1);
    }

    public void decrSubjectCounter(String subjectId, String field) {
        setSubjectCounter(subjectId,field,-1);
    }

    public void setSubjectCounter(String subjectId, String field,int cal) {
        if (StringUtils.isNotBlank(subjectId)) {
            executorService.submit(new Runnable() {
                public void run() {
                    String key = CounterHelper.Subject.SUBJECT_COUNTER_KEY + subjectId;
                    String lockKey = new StringBuilder(Constants.CACHE_NAMESPACE).append(CounterHelper.Subject.SUBJECT_COUNTER_KEY).append("LOCK:").append(subjectId).toString();
                    while (!CacheUtil.getLock(lockKey)) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            logger.error("", e);
                        }
                    }

                    try {
                        Integer number = selectSubjectCounter(subjectId, field) + cal;

                        if (number < 0) {
                            number = selectSubjectCounter(subjectId, field);
                        }

                        logger.info("表情（Subject） key:" + key + " field:" + field + " " + number);

                        CacheUtil.getCache().hset(key, field, String.valueOf(number));

                        subjectMapper.updateCounter(subjectId, field, number);
                    } finally {
                        CacheUtil.unlock(lockKey);
                    }
                }
            });
        }
    }

    public List<Subject> getSubjectListInfo(List<Subject> records, Map<String,Object> params) {
        for (Subject record:records) {
            String currUserId = null;
            if (params.containsKey("currUserId")) {
                currUserId = (String)params.get("currUserId");
            }
            if (params.containsKey("myWorks")) {
                // 获取专辑信息
                record.setAlbums(albumService.querySubjectIdByList(record.getId(),currUserId));
            }


            record.setLikedNum(selectSubjectCounter(record.getId(), CounterHelper.Subject.LIKED));

            record.setViewNum(selectSubjectCounter(record.getId(), CounterHelper.Subject.VIEW));

            record.setCommentsNum(selectSubjectCounter(record.getId(), CounterHelper.Subject.COMMENTS));

            getUserStatus(record,currUserId);

//            getSubjectDetail(record,currUserId);
        }
        return  records;
    }

    public Subject getSubjectDetail(Subject record,String currUserId) {

        if (record != null && StringUtils.isNotBlank(record.getId())) {
            // 增加浏览数
            incrSubjectCounter(record.getId(), CounterHelper.Subject.VIEW);

            record.setAlbums(albumService.querySubjectIdByList(record.getId(),currUserId));

            record.setCommentsNum(selectSubjectCounter(record.getId(), CounterHelper.Subject.COMMENTS));
            record.setForwardNum(selectSubjectCounter(record.getId(), CounterHelper.Subject.FORWARD));
            record.setLikedNum(selectSubjectCounter(record.getId(), CounterHelper.Subject.LIKED));
//            record.setViewNum(selectSubjectCounter(record.getId(), CounterHelper.Subject.VIEW));
        }

        record = getUserStatus(record,currUserId);

        return record;
    }

    public Subject getUserStatus(Subject record,String currUserId) {
        // 判断当前登录用户ID 是否不等空
        if (StringUtils.isNotBlank(record.getUserId())) {
            User user = userService.queryById(record.getUserId());
            // 判断当前登录用户ID 是否不等空
            if (StringUtils.isNotBlank(currUserId)) {
                // 当前登录用户是否喜欢了该作品
                record.setLiked(subjectLikedService.selectByIsLiked(record.getId(),currUserId));

                user.setFollow(userFollowingService.selectByIsFollow(currUserId,record.getUserId()));

                // 判断作品作者是否关注了当前登录用户
                boolean isFolow = userFollowingService.selectByIsFollow(record.getUserId(),currUserId);

                // 是否允许改图
                boolean isWorks = true;
                if (!user.getConfig().getIsWorks()) { // 是否只允许作品作者所关注的用户改图
                    isWorks = isFolow;
                }
                record.setWorks(isWorks);

                // 是否允许评论
                boolean isComment = true;
                if (!user.getConfig().getIsComment()) { // 是否只允许作品作者所关注的用户评论
                    isComment = isFolow;
                }
                record.setComment(isComment);
            } else {
                record.setWorks(user.getConfig().getIsWorks());
                record.setComment(user.getConfig().getIsComment());
            }

            record.setUser(user);
        }
        return record;
    }

    public Subject queryBeanById(String id, String currUserId) {
        Subject record = queryById(id);
        if (record != null && record.getEnable()) {
            return getSubjectDetail(record, currUserId);
        }
        return null;
    }

    public List<Subject> queryByRecommentNew(Map<String, Object> params) {
        if (!params.containsKey("startLimit")) {
            params.put("startLimit",0);
        }
        if (!params.containsKey("endLimit")) {
            params.put("endLimit",10);
        }
        return getList(subjectMapper.queryByNew(params));
    }


    public List<Subject> queryRandByRecomment(Map<String, Object> params) {
        if (!params.containsKey("startLimit")) {
            params.put("startLimit",0);
        }
        if (!params.containsKey("endLimit")) {
            params.put("endLimit",10);
        }
        List<String> ids =  subjectMapper.selectRandIdByMap(params);
        return getSubjectListInfo(getList(ids),params);
    }

    public int selectRowByMap(Map<String, Object> params) {
        return subjectMapper.selectRowByMap(params);
    }


    @Override
    public void delete(String id) {
        Subject record = queryById(id);
        if (record != null && record.getEnable()) {
            // 删除动态
            dynamicService.deleteByValId(id,"1");

            del(id);

            // 作品喜欢数量
            Integer likedNum = selectSubjectCounter(id,CounterHelper.Subject.LIKED);
            // 减去用户作品喜欢数量
            userService.setUserCounter(record.getUserId(),CounterHelper.User.WORKS_LIKE,-likedNum);
            // 减去用户作品数量
            userService.decrUserCounter(record.getUserId(),CounterHelper.User.WORKS);

            Notify notify = new Notify();
            notify.setSubjectId(id);
            notifyService.delete(notify);
        }
    }

    public String selectHashById(String hashCode) {
        String id = subjectMapper.selectHashById(hashCode);
        return id;
    }

    @Override
    public Subject update(Subject record) {
        if (StringUtils.isBlank(record.getId())) {
            userService.incrUserCounter(record.getUserId(),CounterHelper.User.WORKS);

            executorService.submit(new Runnable() {
                public void run() {
                    List<String> topics = SubjectHelper.findTopic(record.getContent());
                    if (topics !=  null && topics.size() > 0) {
                        for (String topic:topics) {
                            String topicId = topicsService.selectIdByName(topic);
                            if (StringUtils.isBlank(topicId)) {
                                Topics topicRecord = new Topics();
                                topicRecord.setName(topic);
                                topicRecord.setAudit("2");
                                topicRecord.setOwnerStatus(1);
                                topicsService.update(topicRecord);
                            }
                        }
                    }
                }
            });
        }
        return super.update(record);
    }

    public Boolean selectIsReleaseSubject(String topic,String userId) {
        int count = subjectMapper.selectIsReleaseSubject(topic,userId);
        if (count > 0) {
            return true;
        }
        return false;
    }
}