package com.aisale.backend.service;

import com.aisale.backend.dto.AdminPointAdjustRequest;
import com.aisale.backend.dto.PointAccountResponse;
import com.aisale.backend.dto.PointRecordResponse;
import com.aisale.backend.entity.PointAccount;
import com.aisale.backend.entity.PointRecord;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.ErrorCode;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.exception.business.ValidationException;
import com.aisale.backend.repository.PointAccountRepository;
import com.aisale.backend.repository.PointRecordRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointService {

    private final PointAccountRepository pointAccountRepository;
    private final PointRecordRepository pointRecordRepository;
    private final UserRepository userRepository;

    /**
     * 获取用户积分账户（不存在则自动创建）
     */
    @Transactional
    public PointAccount getOrCreateAccount(Long userId) {
        return pointAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointAccount account = new PointAccount();
                    account.setUserId(userId);
                    return pointAccountRepository.save(account);
                });
    }

    /**
     * 获取用户积分账户概览
     */
    @Transactional
    public PointAccountResponse getAccountOverview(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        PointAccount account = getOrCreateAccount(user.getId());

        long checkInCount = pointRecordRepository.countByUserIdAndType(user.getId(), PointRecord.PointType.CHECK_IN);
        long orderCount = pointRecordRepository.countByUserIdAndType(user.getId(), PointRecord.PointType.ORDER_COMPLETE);

        return PointAccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .totalPoints(account.getTotalPoints())
                .availablePoints(account.getAvailablePoints())
                .usedPoints(account.getUsedPoints())
                .checkInCount((int) checkInCount)
                .orderCount((int) orderCount)
                .build();
    }

    /**
     * 获取积分流水记录（分页）
     */
    @Transactional(readOnly = true)
    public Page<PointRecordResponse> getPointRecords(String username, PointRecord.PointType type, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Page<PointRecord> records;
        PageRequest pageable = PageRequest.of(page, size);

        if (type != null) {
            records = pointRecordRepository.findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), type, pageable);
        } else {
            records = pointRecordRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }

        return records.map(PointRecordResponse::fromEntity);
    }

    /**
     * 增加积分（通用方法）
     */
    @Transactional
    public PointRecord addPoints(Long userId, PointRecord.PointType type, int points, Long relatedId, String description) {
        if (points <= 0) {
            throw new ValidationException("增加积分必须为正数");
        }

        PointAccount account = getOrCreateAccount(userId);
        account.setTotalPoints(account.getTotalPoints() + points);
        account.setAvailablePoints(account.getAvailablePoints() + points);

        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setPoints(points);
        record.setBalanceAfter(account.getAvailablePoints());
        record.setRelatedId(relatedId);
        record.setDescription(description);

        pointAccountRepository.save(account);
        record = pointRecordRepository.save(record);

        log.info("用户ID={} 增加{}积分，类型={}，描述={}", userId, points, type, description);
        return record;
    }

    /**
     * 扣减积分
     */
    @Transactional
    public PointRecord deductPoints(Long userId, PointRecord.PointType type, int points, Long relatedId, String description) {
        if (points <= 0) {
            throw new ValidationException("扣减积分必须为正数");
        }

        PointAccount account = pointAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POINT_ACCOUNT_NOT_FOUND));

        if (account.getAvailablePoints() < points) {
            throw new ValidationException(ErrorCode.POINT_INSUFFICIENT);
        }

        account.setAvailablePoints(account.getAvailablePoints() - points);
        account.setUsedPoints(account.getUsedPoints() + points);

        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setPoints(-points);
        record.setBalanceAfter(account.getAvailablePoints());
        record.setRelatedId(relatedId);
        record.setDescription(description);

        pointAccountRepository.save(account);
        record = pointRecordRepository.save(record);

        log.info("用户ID={} 扣减{}积分，类型={}，描述={}", userId, points, type, description);
        return record;
    }

    /**
     * 管理员调整积分
     */
    @Transactional
    public PointRecordResponse adminAdjustPoints(Long targetUserId, AdminPointAdjustRequest request) {
        if (request.getPoints() == null || request.getPoints() == 0) {
            throw new ValidationException("调整积分不能为0");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ValidationException("调整原因不能为空");
        }

        PointRecord record;
        if (request.getPoints() > 0) {
            record = addPoints(targetUserId, PointRecord.PointType.ADMIN_ADD,
                    request.getPoints(), null, "管理员调整：" + request.getReason());
        } else {
            record = deductPoints(targetUserId, PointRecord.PointType.ADMIN_DEDUCT,
                    Math.abs(request.getPoints()), null, "管理员调整：" + request.getReason());
        }

        return PointRecordResponse.fromEntity(record);
    }

    /**
     * 获取用户可用积分（供其他服务调用）
     */
    @Transactional(readOnly = true)
    public int getAvailablePoints(Long userId) {
        return pointAccountRepository.findByUserId(userId)
                .map(PointAccount::getAvailablePoints)
                .orElse(0);
    }
}
