package com.lvhui.mianshiOj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lvhui.mianshiOj.common.ErrorCode;
import com.lvhui.mianshiOj.constant.CommonConstant;
import com.lvhui.mianshiOj.exception.BusinessException;
import com.lvhui.mianshiOj.exception.ThrowUtils;
import com.lvhui.mianshiOj.mapper.QuestionBankQuestionMapper;
import com.lvhui.mianshiOj.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.lvhui.mianshiOj.model.entity.Question;
import com.lvhui.mianshiOj.model.entity.QuestionBank;
import com.lvhui.mianshiOj.model.entity.QuestionBankQuestion;
import com.lvhui.mianshiOj.model.entity.User;
import com.lvhui.mianshiOj.model.vo.QuestionBankQuestionVO;
import com.lvhui.mianshiOj.model.vo.UserVO;
import com.lvhui.mianshiOj.service.QuestionBankQuestionService;
import com.lvhui.mianshiOj.service.QuestionBankService;
import com.lvhui.mianshiOj.service.QuestionService;
import com.lvhui.mianshiOj.service.UserService;
import com.lvhui.mianshiOj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题库题目关联服务实现
 *
  
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    @Lazy
    private QuestionService questionService;

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);
        // 题目和题库必须存在
        Long questionId = questionBankQuestion.getQuestionId();
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        Question question = questionService.getById(questionId);
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        if(question == null || questionBank == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();
        // todo 补充需要的查询条件
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题库题目关联封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);
//        // 2. 已登录，获取用户点赞、收藏状态
//        long questionBankQuestionId = questionBankQuestion.getId();
//        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            // 获取点赞
//            QueryWrapper<QuestionBankQuestionThumb> questionBankQuestionThumbQueryWrapper = new QueryWrapper<>();
//            questionBankQuestionThumbQueryWrapper.in("questionBankQuestionId", questionBankQuestionId);
//            questionBankQuestionThumbQueryWrapper.eq("userId", loginUser.getId());
//            QuestionBankQuestionThumb questionBankQuestionThumb = questionBankQuestionThumbMapper.selectOne(questionBankQuestionThumbQueryWrapper);
//            questionBankQuestionVO.setHasThumb(questionBankQuestionThumb != null);
//            // 获取收藏
//            QueryWrapper<QuestionBankQuestionFavour> questionBankQuestionFavourQueryWrapper = new QueryWrapper<>();
//            questionBankQuestionFavourQueryWrapper.in("questionBankQuestionId", questionBankQuestionId);
//            questionBankQuestionFavourQueryWrapper.eq("userId", loginUser.getId());
//            QuestionBankQuestionFavour questionBankQuestionFavour = questionBankQuestionFavourMapper.selectOne(questionBankQuestionFavourQueryWrapper);
//            questionBankQuestionVO.setHasFavour(questionBankQuestionFavour != null);
//        }
        // endregion

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题库题目关联封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
//        // 2. 已登录，获取用户点赞、收藏状态
//        Map<Long, Boolean> questionBankQuestionIdHasThumbMap = new HashMap<>();
//        Map<Long, Boolean> questionBankQuestionIdHasFavourMap = new HashMap<>();
//        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            Set<Long> questionBankQuestionIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getId).collect(Collectors.toSet());
//            loginUser = userService.getLoginUser(request);
//            // 获取点赞
//            QueryWrapper<QuestionBankQuestionThumb> questionBankQuestionThumbQueryWrapper = new QueryWrapper<>();
//            questionBankQuestionThumbQueryWrapper.in("questionBankQuestionId", questionBankQuestionIdSet);
//            questionBankQuestionThumbQueryWrapper.eq("userId", loginUser.getId());
//            List<QuestionBankQuestionThumb> questionBankQuestionQuestionBankQuestionThumbList = questionBankQuestionThumbMapper.selectList(questionBankQuestionThumbQueryWrapper);
//            questionBankQuestionQuestionBankQuestionThumbList.forEach(questionBankQuestionQuestionBankQuestionThumb -> questionBankQuestionIdHasThumbMap.put(questionBankQuestionQuestionBankQuestionThumb.getQuestionBankQuestionId(), true));
//            // 获取收藏
//            QueryWrapper<QuestionBankQuestionFavour> questionBankQuestionFavourQueryWrapper = new QueryWrapper<>();
//            questionBankQuestionFavourQueryWrapper.in("questionBankQuestionId", questionBankQuestionIdSet);
//            questionBankQuestionFavourQueryWrapper.eq("userId", loginUser.getId());
//            List<QuestionBankQuestionFavour> questionBankQuestionFavourList = questionBankQuestionFavourMapper.selectList(questionBankQuestionFavourQueryWrapper);
//            questionBankQuestionFavourList.forEach(questionBankQuestionFavour -> questionBankQuestionIdHasFavourMap.put(questionBankQuestionFavour.getQuestionBankQuestionId(), true));
//        }
        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));
//            questionBankQuestionVO.setHasThumb(questionBankQuestionIdHasThumbMap.getOrDefault(questionBankQuestionVO.getId(), false));
//            questionBankQuestionVO.setHasFavour(questionBankQuestionIdHasFavourMap.getOrDefault(questionBankQuestionVO.getId(), false));
        });
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    @Override
    public void batchAddQuestionsToBank(List<Long> questionIdList, long questionBankId, User LoginUser) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR,"题目id列表不能为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR,"题库id不能为空");
        ThrowUtils.throwIf(LoginUser == null, ErrorCode.PARAMS_ERROR,"用户不能为空");
        // 检查题目id是否存在
        LambdaQueryWrapper<Question> questionLambdaQueryWrapper = Wrappers.lambdaQuery(Question.class)
                .select(Question::getId)
                .in(Question::getId, questionIdList);
        // List<Question> questionList = questionService.list(questionLambdaQueryWrapper);
        // 合法的题目id 列表
        //List<Long> validQuestionIdList = questionList.stream().map(Question::getId).collect(Collectors.toList());
        List<Long> validQuestionIdList = questionService.listObjs(questionLambdaQueryWrapper,obj->(Long) obj);
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR,"题目id列表中存在不合法的题目id");
        // 检查那些题目还不存在于题库中，避免重复插入
        LambdaQueryWrapper<QuestionBankQuestion> eq = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                        .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                                .in(QuestionBankQuestion::getQuestionId, validQuestionIdList);
        List<QuestionBankQuestion> notExistQuesiontList = this.list(eq);
        Set<Long> existQuestionIdSet = notExistQuesiontList.stream().map(QuestionBankQuestion::getQuestionId).collect(Collectors.toSet());
        validQuestionIdList = validQuestionIdList.stream().filter(questionId -> !existQuestionIdSet.contains(questionId)).collect(Collectors.toList());
        List<Long> notExistQuestionIdList = notExistQuesiontList.stream().map(QuestionBankQuestion::getQuestionId).collect(Collectors.toList());
        ThrowUtils.throwIf(CollUtil.isNotEmpty(notExistQuestionIdList), ErrorCode.PARAMS_ERROR,"所有题目都已存在与题目列表中");
        // 题库id是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR,"题库id不存在");

        // 自定义线程池
        ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
                4,                         // 核心线程数
                10,                        // 最大线程数
                60L,                       // 线程空闲存活时间
                TimeUnit.SECONDS,           // 存活时间单位
                new LinkedBlockingQueue<>(1000),  // 阻塞队列容量
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程处理任务
        );

        // 保存所有批次任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 分批处理 避免长事务 每次处理1000条
        int batchSize = 1000;
        int totalQuestionListSize = validQuestionIdList.size();
        for(int i = 0; i < totalQuestionListSize; i += batchSize) {
            List<Long> subList = validQuestionIdList.subList(i, Math.min(i + batchSize, totalQuestionListSize));
            List<QuestionBankQuestion> questionBankQuestions = subList.stream().map(questionId -> {
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                questionBankQuestion.setQuestionId(questionId);
                questionBankQuestion.setQuestionBankId(questionBankId);
                questionBankQuestion.setUserId(LoginUser.getId());
                return questionBankQuestion;
            }).collect(Collectors.toList());
            // 使用事务处理每批数据
            // 注意获取代理
            QuestionBankQuestionService questionBankQuestionService = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();
            // 异步处理每批数据 将任务添加到异步任务列表
            futures.add(CompletableFuture.runAsync(() -> questionBankQuestionService.batchAddQuestionToBankInner(questionBankQuestions), customExecutor));
        }
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        // 关闭线程池
        customExecutor.shutdown();
    }

/**
 * 批量向题库添加题目(内部调用)
 * @param questionBankQuestionList
 */
@Transactional(rollbackFor = Exception.class)
@Override
public void batchAddQuestionToBankInner(List<QuestionBankQuestion> questionBankQuestionList) {
        for(QuestionBankQuestion questionBankQuestion : questionBankQuestionList) {
            Long questionBankId = questionBankQuestion.getQuestionBankId();
            Long questionId = questionBankQuestion.getQuestionId();
            try {
                // 可以改为批量 saveBatch ，缺点拿不到 一些信息
                boolean result = this.save(questionBankQuestion);
                if (!result) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
                }
            } catch (DataIntegrityViolationException e) {
                log.error("数据库唯一键冲突或违反其他完整性约束，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
            } catch (DataAccessException e) {
                log.error("数据库连接问题、事务问题等导致操作失败，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
            } catch (Exception e) {
                // 捕获其他异常，做通用处理
                log.error("添加题目到题库时发生未知错误，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
            }
        }
    }


    /**
     * 批量移除题目
     * @param questionIdList
     * @param questionBankId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestionsFromBank(List<Long> questionIdList, long questionBankId) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR,"题目id列表不能为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR,"题库id不能为空");
        // 执行删除
        for(long questionId : questionIdList) {
            LambdaQueryWrapper<QuestionBankQuestion> eq = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            boolean result = this.remove(eq);
            ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR,"题目删除失败");
        }
    }


}
