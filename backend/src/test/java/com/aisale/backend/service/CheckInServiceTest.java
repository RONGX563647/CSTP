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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckInService 单元测试")
class CheckInServiceTest {

    @Mock
    private CheckInRepository checkInRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointService pointService;

    @InjectMocks
    private CheckInService checkInService;

    private final Long USER_ID = 1L;
    private final String USERNAME = "testuser";
    private final LocalDate TODAY = LocalDate.now();
    private final LocalDate YESTERDAY = TODAY.minusDays(1);

    private User testUser;
    private CheckIn testCheckIn;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);

        testCheckIn = new CheckIn();
        testCheckIn.setId(1L);
        testCheckIn.setUserId(USER_ID);
        testCheckIn.setCheckInDate(TODAY);
        testCheckIn.setContinuousDays(1);
        testCheckIn.setRewardPoints(5);
    }

    @Nested
    @DisplayName("用户签到")
    class CheckInTests {

        @Test
        @DisplayName("首次签到成功，连续1天，奖励5分")
        void checkIn_FirstTime() {
            // Given
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, YESTERDAY)).thenReturn(Optional.empty());
            when(checkInRepository.save(any(CheckIn.class))).thenAnswer(invocation -> {
                CheckIn checkIn = invocation.getArgument(0);
                checkIn.setId(1L);
                return checkIn;
            });
            when(checkInRepository.countByUserId(USER_ID)).thenReturn(1L);
            when(pointService.addPoints(eq(USER_ID), eq(PointRecord.PointType.CHECK_IN), eq(5), any(), any()))
                    .thenReturn(new PointRecord());

            // When
            CheckInResponse response = checkInService.checkIn(USERNAME);

            // Then
            assertTrue(response.getTodayCheckedIn());
            assertEquals(1, response.getContinuousDays());
            assertEquals(5, response.getRewardPoints());
            assertEquals(1, response.getTotalCheckInDays());

            verify(checkInRepository).save(argThat(c ->
                c.getUserId().equals(USER_ID) &&
                c.getContinuousDays() == 1 &&
                c.getRewardPoints() == 5
            ));
            verify(pointService).addPoints(USER_ID, PointRecord.PointType.CHECK_IN, 5, 1L, "签到奖励：连续签到1天");
        }

        @Test
        @DisplayName("连续签到（昨天已签），连续天数+1，奖励10分")
        void checkIn_Continuous() {
            // Given
            CheckIn yesterdayCheckIn = new CheckIn();
            yesterdayCheckIn.setUserId(USER_ID);
            yesterdayCheckIn.setCheckInDate(YESTERDAY);
            yesterdayCheckIn.setContinuousDays(1);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, YESTERDAY)).thenReturn(Optional.of(yesterdayCheckIn));
            when(checkInRepository.save(any(CheckIn.class))).thenAnswer(invocation -> {
                CheckIn checkIn = invocation.getArgument(0);
                checkIn.setId(2L);
                return checkIn;
            });
            when(checkInRepository.countByUserId(USER_ID)).thenReturn(2L);

            // When
            CheckInResponse response = checkInService.checkIn(USERNAME);

            // Then
            assertEquals(2, response.getContinuousDays());
            assertEquals(10, response.getRewardPoints());
        }

        @Test
        @DisplayName("连续7天及以上签到，奖励20分")
        void checkIn_Day7Plus() {
            // Given
            CheckIn yesterdayCheckIn = new CheckIn();
            yesterdayCheckIn.setCheckInDate(YESTERDAY);
            yesterdayCheckIn.setContinuousDays(6);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, YESTERDAY)).thenReturn(Optional.of(yesterdayCheckIn));
            when(checkInRepository.save(any(CheckIn.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(checkInRepository.countByUserId(USER_ID)).thenReturn(7L);

            // When
            CheckInResponse response = checkInService.checkIn(USERNAME);

            // Then
            assertEquals(7, response.getContinuousDays());
            assertEquals(20, response.getRewardPoints());
        }

        @Test
        @DisplayName("今日已签到抛出 ConflictException")
        void checkIn_AlreadyCheckedIn() {
            // Given
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.of(testCheckIn));

            // When & Then
            ConflictException exception = assertThrows(ConflictException.class, () -> {
                checkInService.checkIn(USERNAME);
            });

            assertEquals(ErrorCode.CHECK_IN_ALREADY_DONE.getCode(), exception.getErrorCode());
        }

        @Test
        @DisplayName("用户不存在抛出 NotFoundException")
        void checkIn_UserNotFound() {
            // Given
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () -> {
                checkInService.checkIn(USERNAME);
            });
        }

        @Test
        @DisplayName("签到中断后连续天数重置为1")
        void checkIn_BrokenContinuous() {
            // Given - 昨天未签到，但有更早的签到记录
            LocalDate twoDaysAgo = TODAY.minusDays(2);
            CheckIn twoDaysAgoCheckIn = new CheckIn();
            twoDaysAgoCheckIn.setCheckInDate(twoDaysAgo);
            twoDaysAgoCheckIn.setContinuousDays(5);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, YESTERDAY)).thenReturn(Optional.empty());
            when(checkInRepository.save(any(CheckIn.class))).thenAnswer(invocation -> {
                CheckIn checkIn = invocation.getArgument(0);
                checkIn.setId(1L);
                return checkIn;
            });
            when(checkInRepository.countByUserId(USER_ID)).thenReturn(1L);

            // When
            CheckInResponse response = checkInService.checkIn(USERNAME);

            // Then
            assertEquals(1, response.getContinuousDays()); // 中断后重置为1
            assertEquals(5, response.getRewardPoints());
        }
    }

    @Nested
    @DisplayName("获取签到状态")
    class GetCheckInStatusTests {

        @Test
        @DisplayName("今日已签到，返回正确状态")
        void getCheckInStatus_TodayCheckedIn() {
            // Given
            testCheckIn.setContinuousDays(3);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.of(testCheckIn));
            when(checkInRepository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.of(testCheckIn));
            when(checkInRepository.countByUserId(USER_ID)).thenReturn(3L);
            when(checkInRepository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(USER_ID, TODAY.minusDays(29), TODAY))
                    .thenReturn(List.of(testCheckIn));

            // When
            CheckInStatusResponse response = checkInService.getCheckInStatus(USERNAME);

            // Then
            assertTrue(response.getTodayCheckedIn());
            assertEquals(3, response.getContinuousDays());
            assertEquals(3, response.getTotalCheckInDays());
            assertEquals(TODAY, response.getLastCheckInDate());
        }

        @Test
        @DisplayName("今日未签到但昨天签到，连续天数有效")
        void getCheckInStatus_NotTodayButYesterday() {
            // Given
            CheckIn yesterdayCheckIn = new CheckIn();
            yesterdayCheckIn.setCheckInDate(YESTERDAY);
            yesterdayCheckIn.setContinuousDays(2);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
            when(checkInRepository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.of(yesterdayCheckIn));
            when(checkInRepository.countByUserId(USER_ID)).thenReturn(2L);
            when(checkInRepository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(USER_ID, TODAY.minusDays(29), TODAY))
                    .thenReturn(List.of(yesterdayCheckIn));

            // When
            CheckInStatusResponse response = checkInService.getCheckInStatus(USERNAME);

            // Then
            assertFalse(response.getTodayCheckedIn());
            assertEquals(2, response.getContinuousDays());
            assertEquals(YESTERDAY, response.getLastCheckInDate());
        }

        @Test
        @DisplayName("从未签到，返回默认状态")
        void getCheckInStatus_NeverCheckedIn() {
            // Given
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
            when(checkInRepository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.empty());
            when(checkInRepository.countByUserId(USER_ID)).thenReturn(0L);
            when(checkInRepository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(USER_ID, TODAY.minusDays(29), TODAY))
                    .thenReturn(List.of());

            // When
            CheckInStatusResponse response = checkInService.getCheckInStatus(USERNAME);

            // Then
            assertFalse(response.getTodayCheckedIn());
            assertEquals(0, response.getContinuousDays());
            assertEquals(0, response.getTotalCheckInDays());
            assertNull(response.getLastCheckInDate());
        }
    }

    @Nested
    @DisplayName("获取签到日历")
    class GetCheckInCalendarTests {

        @Test
        @DisplayName("获取指定月份签到日期列表")
        void getCheckInCalendar_Success() {
            // Given
            LocalDate date1 = LocalDate.of(2026, 4, 5);
            LocalDate date2 = LocalDate.of(2026, 4, 10);
            LocalDate date3 = LocalDate.of(2026, 4, 15);

            CheckIn checkIn1 = new CheckIn();
            checkIn1.setCheckInDate(date1);
            CheckIn checkIn2 = new CheckIn();
            checkIn2.setCheckInDate(date2);
            CheckIn checkIn3 = new CheckIn();
            checkIn3.setCheckInDate(date3);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(checkInRepository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(
                    USER_ID,
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 30)))
                    .thenReturn(List.of(checkIn1, checkIn2, checkIn3));

            // When
            List<LocalDate> dates = checkInService.getCheckInCalendar(USERNAME, 2026, 4);

            // Then
            assertEquals(3, dates.size());
            assertTrue(dates.contains(date1));
            assertTrue(dates.contains(date2));
            assertTrue(dates.contains(date3));
        }
    }
}