package com.aisale.backend.service;

import com.aisale.backend.dto.AdminReputationAdjustRequest;
import com.aisale.backend.dto.ReputationAccountResponse;
import com.aisale.backend.dto.ReputationRecordResponse;
import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.entity.ReputationAccount;
import com.aisale.backend.entity.ReputationRecord;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.ErrorCode;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.exception.business.ValidationException;
import com.aisale.backend.repository.ReputationAccountRepository;
import com.aisale.backend.repository.ReputationRecordRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户信誉服务
 * 处理信誉分的计算、更新、查询等功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReputationService {

    private final ReputationAccountRepository reputationAccountRepository;
    private final ReputationRecordRepository reputationRecordRepository;
    private final UserRepository userRepository;

    // 信誉计算规则
    private static final int SCORE_FOR_5 = 10;  // 5分好评 +10
    private static final int SCORE_FOR_4 = 5;   // 4分好评 +5
    private static final int SCORE_FOR_3 = 0;   // 3分中评 +0
    private static final int SCORE_FOR_2 = -5;  // 2分差评 -5
    private static final int SCORE_FOR_1 = -10; // 1分极差 -10

    /**
     * 获取或创建用户信誉账户
     */
    @Transactional
    public ReputationAccount getOrCreateAccount(Long userId) {
        return reputationAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ReputationAccount account = new ReputationAccount();
                    account.setUserId(userId);
                    account.setTotalScore(100); // 初始100分
                    account.setLevel(3); // 初始一般等级
                    return reputationAccountRepository.save(account);
                });
    }

    /**
     * 获取用户信誉账户概览
     */
    @Transactional
    public ReputationAccountResponse getAccountOverview(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        ReputationAccount account = getOrCreateAccount(user.getId());
        return ReputationAccountResponse.fromEntity(account);
    }

    /**
     * 获取指定用户ID的信誉概览（公开信息）
     */
    @Transactional(readOnly = true)
    public ReputationAccountResponse getAccountByUserId(Long userId) {
        ReputationAccount account = reputationAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 如果用户没有信誉账户，返回默认值
                    ReputationAccount defaultAccount = new ReputationAccount();
                    defaultAccount.setUserId(userId);
                    defaultAccount.setTotalScore(100);
                    defaultAccount.setLevel(3);
                    return defaultAccount;
                });
        return ReputationAccountResponse.fromEntity(account);
    }

    /**
     * 获取信誉流水记录（分页）
     */
    @Transactional(readOnly = true)
    public Page<ReputationRecordResponse> getRecords(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        Page<ReputationRecord> records = reputationRecordRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));

        return records.map(ReputationRecordResponse::fromEntity);
    }

    /**
     * 根据评价更新用户信誉
     * 在评价完成后调用此方法
     */
    @Transactional
    public ReputationRecord updateReputationFromReview(OrderReview review) {
        Long revieweeId = review.getRevieweeId(); // 被评价人的信誉需要更新
        Integer rating = review.getRating();

        // 计算信誉变化
        int scoreChange = calculateScoreChange(rating);

        // 获取或创建信誉账户
        ReputationAccount account = getOrCreateAccount(revieweeId);

        // 更新统计
        updateStatistics(account, rating);

        // 更新信誉分
        int newScore = account.getTotalScore() + scoreChange;
        account.setTotalScore(Math.max(0, newScore)); // 信誉分不能为负

        // 计算平均评分
        if (account.getTotalReviews() > 0) {
            account.setAvgRating(calculateAvgRating(account));
        }

        // 计算等级
        account.setLevel(calculateLevel(account));

        reputationAccountRepository.save(account);

        // 创建信誉记录
        ReputationRecord record = new ReputationRecord();
        record.setUserId(revieweeId);
        record.setReviewId(review.getId());
        record.setType(ReputationRecord.RecordType.REVIEW_ADD);
        record.setScoreChange(scoreChange);
        record.setBalanceAfter(account.getTotalScore());
        record.setRating(rating);
        record.setContent(truncateContent(review.getContent(), 200));
        record.setDescription(String.format("收到%s评价，评分%d分",
                review.getReviewType() == OrderReview.ReviewType.BUYER_REVIEW ? "买家" : "卖家",
                rating));

        record = reputationRecordRepository.save(record);

        log.info("用户ID={} 收到评价，评分={}，信誉变化={}，新信誉分={}",
                revieweeId, rating, scoreChange, account.getTotalScore());

        return record;
    }

    /**
     * 管理员调整信誉分
     */
    @Transactional
    public ReputationRecordResponse adminAdjust(Long targetUserId, AdminReputationAdjustRequest request) {
        if (request.getScore() == null || request.getScore() == 0) {
            throw new ValidationException("调整分数不能为0");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ValidationException("调整原因不能为空");
        }

        ReputationAccount account = getOrCreateAccount(targetUserId);

        int newScore = account.getTotalScore() + request.getScore();
        account.setTotalScore(Math.max(0, newScore));

        // 重新计算等级
        account.setLevel(calculateLevel(account));

        reputationAccountRepository.save(account);

        // 创建调整记录
        ReputationRecord record = new ReputationRecord();
        record.setUserId(targetUserId);
        record.setType(ReputationRecord.RecordType.ADMIN_ADJUST);
        record.setScoreChange(request.getScore());
        record.setBalanceAfter(account.getTotalScore());
        record.setDescription("管理员调整：" + request.getReason());

        record = reputationRecordRepository.save(record);

        log.info("管理员调整用户ID={}信誉，变化={}，原因={}",
                targetUserId, request.getScore(), request.getReason());

        return ReputationRecordResponse.fromEntity(record);
    }

    /**
     * 根据评分计算信誉变化
     */
    private int calculateScoreChange(Integer rating) {
        if (rating == null) return 0;
        switch (rating) {
            case 5: return SCORE_FOR_5;
            case 4: return SCORE_FOR_4;
            case 3: return SCORE_FOR_3;
            case 2: return SCORE_FOR_2;
            case 1: return SCORE_FOR_1;
            default: return 0;
        }
    }

    /**
     * 更新评价统计
     */
    private void updateStatistics(ReputationAccount account, Integer rating) {
        account.setTotalReviews(account.getTotalReviews() + 1);

        if (rating >= 4) {
            account.setGoodReviews(account.getGoodReviews() + 1);
        } else if (rating == 3) {
            account.setNeutralReviews(account.getNeutralReviews() + 1);
        } else {
            account.setBadReviews(account.getBadReviews() + 1);
        }
    }

    /**
     * 计算平均评分
     */
    private double calculateAvgRating(ReputationAccount account) {
        // 需要从所有评价记录中计算真实平均评分
        // 这里简化处理：好评=5分，中评=3分，差评=1分的平均值
        int totalReviews = account.getTotalReviews();
        if (totalReviews == 0) return 0.0;

        int totalRatingScore = account.getGoodReviews() * 5
                + account.getNeutralReviews() * 3
                + account.getBadReviews() * 1;

        return (double) totalRatingScore / totalReviews;
    }

    /**
     * 计算信誉等级
     */
    private int calculateLevel(ReputationAccount account) {
        return ReputationAccount.ReputationLevel.calculate(
                account.getAvgRating(),
                account.getTotalReviews()
        ).getLevel();
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength);
    }
}