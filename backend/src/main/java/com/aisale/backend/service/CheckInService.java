package com.aisale.backend.service;

import com.aisale.backend.dto.CheckInResponse;
import com.aisale.backend.dto.CheckInStatusResponse;
import com.aisale.backend.entity.CheckIn;
import com.aisale.backend.entity.PointRecord;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.ErrorCode;
import com.aisale.backend.exception.business.ConflictException;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.repository.CheckInRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    /**
     * 用户签到
     */
    @Transactional
    public CheckInResponse checkIn(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        LocalDate today = LocalDate.now();

        // 检查今日是否已签到
        checkInRepository.findByUserIdAndCheckInDate(user.getId(), today)
                .ifPresent(existing -> {
                    throw new ConflictException(ErrorCode.CHECK_IN_ALREADY_DONE);
                });

        // 计算连续签到天数
        int continuousDays = calculateContinuousDays(user.getId(), today);

        // 计算奖励积分（连续签到天数越多，奖励越多）
        int rewardPoints = calculateRewardPoints(continuousDays);

        // 创建签到记录
        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(user.getId());
        checkIn.setCheckInDate(today);
        checkIn.setContinuousDays(continuousDays);
        checkIn.setRewardPoints(rewardPoints);

        checkIn = checkInRepository.save(checkIn);

        // 同步增加积分
        pointService.addPoints(user.getId(), PointRecord.PointType.CHECK_IN,
                rewardPoints, checkIn.getId(),
                "签到奖励：连续签到" + continuousDays + "天");

        log.info("用户 {} 签到成功，连续 {} 天，奖励 {} 积分", username, continuousDays, rewardPoints);

        long totalDays = checkInRepository.countByUserId(user.getId());
        return CheckInResponse.fromEntity(checkIn, true, (int) totalDays);
    }

    /**
     * 获取用户签到状态
     */
    @Transactional(readOnly = true)
    public CheckInStatusResponse getCheckInStatus(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        LocalDate today = LocalDate.now();

        // 检查今日是否已签到
        boolean todayCheckedIn = checkInRepository.findByUserIdAndCheckInDate(user.getId(), today).isPresent();

        // 获取最近一次签到记录
        CheckIn lastCheckIn = checkInRepository.findTopByUserIdOrderByCheckInDateDesc(user.getId())
                .orElse(null);

        int continuousDays = 0;
        LocalDate lastCheckInDate = null;

        if (lastCheckIn != null) {
            lastCheckInDate = lastCheckIn.getCheckInDate();
            // 如果今天已签到，或昨天签到了，连续天数有效
            if (todayCheckedIn || lastCheckInDate.equals(today.minusDays(1))) {
                continuousDays = lastCheckIn.getContinuousDays();
            }
        }

        // 累计签到天数
        long totalCheckInDays = checkInRepository.countByUserId(user.getId());

        // 最近30天签到日期列表
        LocalDate monthStart = today.minusDays(29);
        List<LocalDate> recentDates = checkInRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(user.getId(), monthStart, today)
                .stream()
                .map(CheckIn::getCheckInDate)
                .toList();

        return CheckInStatusResponse.builder()
                .todayCheckedIn(todayCheckedIn)
                .continuousDays(continuousDays)
                .totalCheckInDays((int) totalCheckInDays)
                .lastCheckInDate(lastCheckInDate)
                .recentCheckInDates(recentDates)
                .build();
    }

    /**
     * 获取用户签到日历（指定月份）
     */
    @Transactional(readOnly = true)
    public List<LocalDate> getCheckInCalendar(String username, int year, int month) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return checkInRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(user.getId(), start, end)
                .stream()
                .map(CheckIn::getCheckInDate)
                .toList();
    }

    /**
     * 计算连续签到天数
     */
    private int calculateContinuousDays(Long userId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);

        return checkInRepository.findByUserIdAndCheckInDate(userId, yesterday)
                .map(yesterdayCheckIn -> yesterdayCheckIn.getContinuousDays() + 1)
                .orElse(1);
    }

    /**
     * 计算奖励积分
     * 连续1天: 5分, 2-6天: 10分/天, 7天及以上: 20分/天
     */
    private int calculateRewardPoints(int continuousDays) {
        if (continuousDays >= 7) {
            return 20;
        } else if (continuousDays >= 2) {
            return 10;
        }
        return 5;
    }
}
