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
@DisplayName("ReputationService 单元测试")
class ReputationServiceTest {

    @Mock
    private ReputationAccountRepository reputationAccountRepository;

    @Mock
    private ReputationRecordRepository reputationRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReputationService reputationService;

    private final Long USER_ID = 1L;
    private final String USERNAME = "testuser";
    private final Long REVIEW_ID = 100L;
    private final Long REVIEWEE_ID = 2L;

    private User testUser;
    private ReputationAccount testAccount;
    private OrderReview testReview;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);

        testAccount = new ReputationAccount();
        testAccount.setId(1L);
        testAccount.setUserId(USER_ID);
        testAccount.setTotalScore(100);
        testAccount.setLevel(3);
        testAccount.setTotalReviews(0);
        testAccount.setGoodReviews(0);
        testAccount.setNeutralReviews(0);
        testAccount.setBadReviews(0);
        testAccount.setAvgRating(0.0);

        testReview = new OrderReview();
        testReview.setId(REVIEW_ID);
        testReview.setRevieweeId(REVIEWEE_ID);
        testReview.setRating(5);
        testReview.setContent("非常好的交易");
        testReview.setReviewType(OrderReview.ReviewType.BUYER_REVIEW);
    }

    @Nested
    @DisplayName("获取或创建账户")
    class GetOrCreateAccountTests {

        @Test
        @DisplayName("新用户自动创建账户，初始100分，等级3")
        void getOrCreateAccount_NewUser() {
            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(reputationAccountRepository.save(any(ReputationAccount.class))).thenAnswer(invocation -> {
                ReputationAccount account = invocation.getArgument(0);
                account.setId(1L);
                return account;
            });

            ReputationAccount account = reputationService.getOrCreateAccount(USER_ID);

            assertNotNull(account);
            assertEquals(USER_ID, account.getUserId());
            assertEquals(100, account.getTotalScore());
            assertEquals(3, account.getLevel());

            verify(reputationAccountRepository).save(argThat(a ->
                a.getUserId().equals(USER_ID) &&
                a.getTotalScore() == 100 &&
                a.getLevel() == 3
            ));
        }

        @Test
        @DisplayName("已有账户直接返回")
        void getOrCreateAccount_ExistingUser() {
            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));

            ReputationAccount account = reputationService.getOrCreateAccount(USER_ID);

            assertNotNull(account);
            assertEquals(testAccount.getId(), account.getId());
            assertEquals(100, account.getTotalScore());

            verify(reputationAccountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("获取账户概览")
    class GetAccountOverviewTests {

        @Test
        @DisplayName("成功获取账户概览")
        void getAccountOverview_Success() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));

            ReputationAccountResponse response = reputationService.getAccountOverview(USERNAME);

            assertNotNull(response);
            assertEquals(USER_ID, response.getUserId());
            assertEquals(100, response.getTotalScore());
            assertEquals(3, response.getLevel());
        }

        @Test
        @DisplayName("用户不存在抛出 NotFoundException")
        void getAccountOverview_UserNotFound() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                reputationService.getAccountOverview(USERNAME);
            });

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("评价更新信誉")
    class UpdateReputationFromReviewTests {

        @Test
        @DisplayName("5分好评增加10信誉分")
        void updateReputationFromReview_Rating5() {
            // Given
            ReputationAccount revieweeAccount = new ReputationAccount();
            revieweeAccount.setId(2L);
            revieweeAccount.setUserId(REVIEWEE_ID);
            revieweeAccount.setTotalScore(100);
            revieweeAccount.setTotalReviews(0);
            revieweeAccount.setGoodReviews(0);
            revieweeAccount.setNeutralReviews(0);
            revieweeAccount.setBadReviews(0);
            revieweeAccount.setAvgRating(0.0);
            revieweeAccount.setLevel(3);

            testReview.setRating(5);

            when(reputationAccountRepository.findByUserId(REVIEWEE_ID)).thenReturn(Optional.of(revieweeAccount));
            when(reputationAccountRepository.save(any(ReputationAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(reputationRecordRepository.save(any(ReputationRecord.class))).thenAnswer(invocation -> {
                ReputationRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });

            // When
            ReputationRecord record = reputationService.updateReputationFromReview(testReview);

            // Then
            assertEquals(10, record.getScoreChange());
            assertEquals(110, record.getBalanceAfter());
            assertEquals(ReputationRecord.RecordType.REVIEW_ADD, record.getType());
            assertEquals(5, record.getRating());

            verify(reputationAccountRepository).save(argThat(a ->
                a.getTotalScore() == 110 &&
                a.getTotalReviews() == 1 &&
                a.getGoodReviews() == 1
            ));
        }

        @Test
        @DisplayName("4分好评增加5信誉分")
        void updateReputationFromReview_Rating4() {
            ReputationAccount revieweeAccount = createDefaultAccount(REVIEWEE_ID);
            testReview.setRating(4);

            mockAccountAndRecordSave(revieweeAccount);

            ReputationRecord record = reputationService.updateReputationFromReview(testReview);

            assertEquals(5, record.getScoreChange());
            assertEquals(105, record.getBalanceAfter());

            verify(reputationAccountRepository).save(argThat(a ->
                a.getTotalScore() == 105 &&
                a.getTotalReviews() == 1 &&
                a.getGoodReviews() == 1
            ));
        }

        @Test
        @DisplayName("3分中评不变信誉分")
        void updateReputationFromReview_Rating3() {
            ReputationAccount revieweeAccount = createDefaultAccount(REVIEWEE_ID);
            testReview.setRating(3);

            mockAccountAndRecordSave(revieweeAccount);

            ReputationRecord record = reputationService.updateReputationFromReview(testReview);

            assertEquals(0, record.getScoreChange());
            assertEquals(100, record.getBalanceAfter());
        }

        @Test
        @DisplayName("2分差评扣减5信誉分")
        void updateReputationFromReview_Rating2() {
            ReputationAccount revieweeAccount = createDefaultAccount(REVIEWEE_ID);
            testReview.setRating(2);

            mockAccountAndRecordSave(revieweeAccount);

            ReputationRecord record = reputationService.updateReputationFromReview(testReview);

            assertEquals(-5, record.getScoreChange());
            assertEquals(95, record.getBalanceAfter());

            verify(reputationAccountRepository).save(argThat(a ->
                a.getTotalScore() == 95 &&
                a.getTotalReviews() == 1 &&
                a.getBadReviews() == 1
            ));
        }

        @Test
        @DisplayName("1分极差扣减10信誉分")
        void updateReputationFromReview_Rating1() {
            ReputationAccount revieweeAccount = createDefaultAccount(REVIEWEE_ID);
            testReview.setRating(1);

            mockAccountAndRecordSave(revieweeAccount);

            ReputationRecord record = reputationService.updateReputationFromReview(testReview);

            assertEquals(-10, record.getScoreChange());
            assertEquals(90, record.getBalanceAfter());

            verify(reputationAccountRepository).save(argThat(a ->
                a.getTotalScore() == 90 &&
                a.getTotalReviews() == 1 &&
                a.getBadReviews() == 1
            ));
        }

        @Test
        @DisplayName("信誉分扣减后不能为负（最小0）")
        void updateReputationFromReview_NegativeScoreProtection() {
            ReputationAccount revieweeAccount = createDefaultAccount(REVIEWEE_ID);
            revieweeAccount.setTotalScore(5);
            testReview.setRating(1);

            mockAccountAndRecordSave(revieweeAccount);

            ReputationRecord record = reputationService.updateReputationFromReview(testReview);

            assertEquals(-10, record.getScoreChange());
            assertEquals(0, record.getBalanceAfter());

            verify(reputationAccountRepository).save(argThat(a -> a.getTotalScore() == 0));
        }
    }

    // Helper methods
    private ReputationAccount createDefaultAccount(Long userId) {
        ReputationAccount account = new ReputationAccount();
        account.setUserId(userId);
        account.setTotalScore(100);
        account.setTotalReviews(0);
        account.setGoodReviews(0);
        account.setNeutralReviews(0);
        account.setBadReviews(0);
        account.setAvgRating(0.0);
        account.setLevel(3);
        return account;
    }

    private void mockAccountAndRecordSave(ReputationAccount account) {
        when(reputationAccountRepository.findByUserId(account.getUserId())).thenReturn(Optional.of(account));
        when(reputationAccountRepository.save(any(ReputationAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reputationRecordRepository.save(any(ReputationRecord.class))).thenAnswer(invocation -> {
            ReputationRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });
    }

    @Nested
    @DisplayName("管理员调整信誉")
    class AdminAdjustTests {

        @Test
        @DisplayName("正向调整成功")
        void adminAdjust_AddPositive() {
            // Given
            AdminReputationAdjustRequest request = new AdminReputationAdjustRequest();
            request.setScore(50);
            request.setReason("活动奖励");

            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));
            when(reputationAccountRepository.save(any(ReputationAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(reputationRecordRepository.save(any(ReputationRecord.class))).thenAnswer(invocation -> {
                ReputationRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });

            // When
            ReputationRecordResponse response = reputationService.adminAdjust(USER_ID, request);

            // Then
            assertEquals(50, response.getScoreChange());
            assertEquals(150, response.getBalanceAfter());
            assertEquals("ADMIN_ADJUST", response.getType());
            assertTrue(response.getDescription().contains("活动奖励"));
        }

        @Test
        @DisplayName("负向调整成功")
        void adminAdjust_DeductNegative() {
            // Given
            AdminReputationAdjustRequest request = new AdminReputationAdjustRequest();
            request.setScore(-30);
            request.setReason("违规处罚");

            when(reputationAccountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testAccount));
            when(reputationAccountRepository.save(any(ReputationAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(reputationRecordRepository.save(any(ReputationRecord.class))).thenAnswer(invocation -> {
                ReputationRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });

            // When
            ReputationRecordResponse response = reputationService.adminAdjust(USER_ID, request);

            // Then
            assertEquals(-30, response.getScoreChange());
            assertEquals(70, response.getBalanceAfter());
            assertEquals("ADMIN_ADJUST", response.getType());
            assertTrue(response.getDescription().contains("违规处罚"));
        }

        @Test
        @DisplayName("调整分数为0抛出 ValidationException")
        void adminAdjust_ZeroScore() {
            // Given
            AdminReputationAdjustRequest request = new AdminReputationAdjustRequest();
            request.setScore(0);
            request.setReason("测试");

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                reputationService.adminAdjust(USER_ID, request);
            });

            assertTrue(exception.getMessage().contains("不能为0"));
        }

        @Test
        @DisplayName("原因为空抛出 ValidationException")
        void adminAdjust_EmptyReason() {
            // Given
            AdminReputationAdjustRequest request = new AdminReputationAdjustRequest();
            request.setScore(10);
            request.setReason("");

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                reputationService.adminAdjust(USER_ID, request);
            });

            assertTrue(exception.getMessage().contains("不能为空"));
        }
    }

    @Nested
    @DisplayName("获取信誉流水")
    class GetRecordsTests {

        @Test
        @DisplayName("分页查询成功")
        void getRecords_Success() {
            // Given
            ReputationRecord record1 = new ReputationRecord();
            record1.setId(1L);
            record1.setUserId(USER_ID);
            record1.setType(ReputationRecord.RecordType.REVIEW_ADD);
            record1.setScoreChange(10);
            record1.setBalanceAfter(110);
            record1.setRating(5);
            record1.setDescription("收到买家评价");

            ReputationRecord record2 = new ReputationRecord();
            record2.setId(2L);
            record2.setUserId(USER_ID);
            record2.setType(ReputationRecord.RecordType.ADMIN_ADJUST);
            record2.setScoreChange(50);
            record2.setBalanceAfter(160);
            record2.setDescription("管理员调整：活动奖励");

            List<ReputationRecord> records = List.of(record1, record2);
            Page<ReputationRecord> page = new PageImpl<>(records);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(reputationRecordRepository.findByUserIdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 20)))
                    .thenReturn(page);

            // When
            Page<ReputationRecordResponse> response = reputationService.getRecords(USERNAME, 0, 20);

            // Then
            assertNotNull(response);
            assertEquals(2, response.getContent().size());
            assertEquals("REVIEW_ADD", response.getContent().get(0).getType());
            assertEquals(10, response.getContent().get(0).getScoreChange());
        }

        @Test
        @DisplayName("用户不存在抛出 NotFoundException")
        void getRecords_UserNotFound() {
            // Given
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () -> {
                reputationService.getRecords(USERNAME, 0, 20);
            });
        }
    }
}