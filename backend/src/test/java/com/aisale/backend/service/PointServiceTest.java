package com.aisale.backend.service;

import com.aisale.backend.dto.AdminPointAdjustRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 单元测试")
class PointServiceTest {

    @Mock
    private PointAccountRepository pointAccountRepository;

    @Mock
    private PointRecordRepository pointRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointService pointService;

    private final Long USER_ID = 1L;
    private final String USERNAME = "testuser";

    private User testUser;
    private PointAccount testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);

        testAccount = new PointAccount();
        testAccount.setId(1L);
        testAccount.setUserId(USER_ID);
        testAccount.setTotalPoints(100);
        testAccount.setAvailablePoints(100);
        testAccount.setUsedPoints(0);
    }

    @Nested
    @DisplayName("获取或创建账户")
    class GetOrCreateAccountTests {

        @Test
        @DisplayName("新用户自动创建账户，初始积分0")
        void getOrCreateAccount_NewUser() {
            // Given
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(pointAccountRepository.save(any(PointAccount.class))).thenAnswer(invocation -> {
                PointAccount account = invocation.getArgument(0);
                account.setId(1L);
                return account;
            });

            // When
            PointAccount account = pointService.getOrCreateAccount(USER_ID);

            // Then
            assertNotNull(account);
            assertEquals(USER_ID, account.getUserId());
            assertEquals(0, account.getTotalPoints());
            assertEquals(0, account.getAvailablePoints());
        }

        @Test
        @DisplayName("已有账户直接返回")
        void getOrCreateAccount_ExistingUser() {
            // Given
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));

            // When
            PointAccount account = pointService.getOrCreateAccount(USER_ID);

            // Then
            assertEquals(testAccount.getId(), account.getId());
            verify(pointAccountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("增加积分")
    class AddPointsTests {

        @Test
        @DisplayName("增加积分成功")
        void addPoints_Success() {
            // Given
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));
            when(pointAccountRepository.save(any(PointAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(pointRecordRepository.save(any(PointRecord.class))).thenAnswer(invocation -> {
                PointRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });

            // When
            PointRecord record = pointService.addPoints(USER_ID, PointRecord.PointType.CHECK_IN, 10, 1L, "签到奖励");

            // Then
            assertEquals(10, record.getPoints());
            assertEquals(110, record.getBalanceAfter());
            assertEquals(PointRecord.PointType.CHECK_IN, record.getType());

            verify(pointAccountRepository).save(argThat(a ->
                a.getTotalPoints() == 110 &&
                a.getAvailablePoints() == 110
            ));
        }

        @Test
        @DisplayName("增加积分<=0抛出 ValidationException")
        void addPoints_ZeroOrNegative() {
            // When & Then
            assertThrows(ValidationException.class, () -> {
                pointService.addPoints(USER_ID, PointRecord.PointType.CHECK_IN, 0, null, "测试");
            });

            assertThrows(ValidationException.class, () -> {
                pointService.addPoints(USER_ID, PointRecord.PointType.CHECK_IN, -5, null, "测试");
            });
        }
    }

    @Nested
    @DisplayName("扣减积分")
    class DeductPointsTests {

        @Test
        @DisplayName("扣减积分成功")
        void deductPoints_Success() {
            // Given
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));
            when(pointAccountRepository.save(any(PointAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(pointRecordRepository.save(any(PointRecord.class))).thenAnswer(invocation -> {
                PointRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });

            // When
            PointRecord record = pointService.deductPoints(USER_ID, PointRecord.PointType.EXCHANGE, 30, null, "兑换商品");

            // Then
            assertEquals(-30, record.getPoints());
            assertEquals(70, record.getBalanceAfter());

            verify(pointAccountRepository).save(argThat(a ->
                a.getAvailablePoints() == 70 &&
                a.getUsedPoints() == 30
            ));
        }

        @Test
        @DisplayName("余额不足抛出 ValidationException")
        void deductPoints_InsufficientBalance() {
            // Given
            testAccount.setAvailablePoints(20);
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                pointService.deductPoints(USER_ID, PointRecord.PointType.EXCHANGE, 50, null, "兑换");
            });

            assertEquals(ErrorCode.POINT_INSUFFICIENT.getCode(), exception.getErrorCode());
        }

        @Test
        @DisplayName("账户不存在抛出 NotFoundException")
        void deductPoints_AccountNotFound() {
            // Given
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () -> {
                pointService.deductPoints(USER_ID, PointRecord.PointType.EXCHANGE, 10, null, "兑换");
            });
        }

        @Test
        @DisplayName("扣减积分<=0抛出 ValidationException")
        void deductPoints_ZeroOrNegative() {
            // When & Then
            assertThrows(ValidationException.class, () -> {
                pointService.deductPoints(USER_ID, PointRecord.PointType.EXCHANGE, 0, null, "测试");
            });
        }
    }

    @Nested
    @DisplayName("管理员调整积分")
    class AdminAdjustPointsTests {

        @Test
        @DisplayName("正向调整调用 addPoints")
        void adminAdjustPoints_AddPositive() {
            // Given
            AdminPointAdjustRequest request = new AdminPointAdjustRequest();
            request.setPoints(50);
            request.setReason("活动奖励");

            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));
            when(pointAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(pointRecordRepository.save(any())).thenAnswer(invocation -> {
                PointRecord r = invocation.getArgument(0);
                r.setId(1L);
                return r;
            });

            // When
            PointRecordResponse response = pointService.adminAdjustPoints(USER_ID, request);

            // Then
            assertEquals(50, response.getPoints());
            assertEquals("ADMIN_ADD", response.getType());
        }

        @Test
        @DisplayName("负向调整调用 deductPoints")
        void adminAdjustPoints_DeductNegative() {
            // Given
            AdminPointAdjustRequest request = new AdminPointAdjustRequest();
            request.setPoints(-30);
            request.setReason("违规处罚");

            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));
            when(pointAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(pointRecordRepository.save(any())).thenAnswer(invocation -> {
                PointRecord r = invocation.getArgument(0);
                r.setId(1L);
                return r;
            });

            // When
            PointRecordResponse response = pointService.adminAdjustPoints(USER_ID, request);

            // Then
            assertEquals(-30, response.getPoints());
            assertEquals("ADMIN_DEDUCT", response.getType());
        }

        @Test
        @DisplayName("调整为0抛出 ValidationException")
        void adminAdjustPoints_ZeroPoints() {
            // Given
            AdminPointAdjustRequest request = new AdminPointAdjustRequest();
            request.setPoints(0);
            request.setReason("测试");

            // When & Then
            assertThrows(ValidationException.class, () -> {
                pointService.adminAdjustPoints(USER_ID, request);
            });
        }

        @Test
        @DisplayName("原因为空抛出 ValidationException")
        void adminAdjustPoints_EmptyReason() {
            // Given
            AdminPointAdjustRequest request = new AdminPointAdjustRequest();
            request.setPoints(10);
            request.setReason("");

            // When & Then
            assertThrows(ValidationException.class, () -> {
                pointService.adminAdjustPoints(USER_ID, request);
            });
        }
    }

    @Nested
    @DisplayName("获取可用积分")
    class GetAvailablePointsTests {

        @Test
        @DisplayName("返回正确可用积分")
        void getAvailablePoints_Success() {
            // Given
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));

            // When
            int points = pointService.getAvailablePoints(USER_ID);

            // Then
            assertEquals(100, points);
        }

        @Test
        @DisplayName("无账户返回0")
        void getAvailablePoints_NoAccount() {
            // Given
            when(pointAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            // When
            int points = pointService.getAvailablePoints(USER_ID);

            // Then
            assertEquals(0, points);
        }
    }
}