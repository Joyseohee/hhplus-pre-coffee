package com.hhplus.coffee.service;

import com.hhplus.coffee.user.User;
import com.hhplus.coffee.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class UserServiceTest {
	@Mock
	private UserRepository userRepository;
	
	@InjectMocks
	private UserService userService;
	
	// 2. 포인트 충전 하기 API
	//     - 결제는 포인트로만 가능하며, 포인트를 충전하는 API를 작성합니다.
	//     - 사용자 식별값, 충전금액을 입력 받아 포인트를 충전합니다. (1원=1P)
	@Test
	void 사용자에게_식별값과_충전금액을_입력_받아_정상적으로_충전한다(){
		Long userId = 1L;
		String username = "name";
		BigDecimal originMoney = new BigDecimal(3_000);
		BigDecimal chargeMoney = new BigDecimal(3_500);
		
		given(userRepository.findById(userId))
				.willReturn(Optional.of(new User(userId, username, originMoney)));

		userService.chargePoint(userId, chargeMoney);
		
		verify(userRepository).save(argThat(user -> user.getPoint() ==  (originMoney.add(chargeMoney))));
	}
	
	@Test
	void 충전금액이_음수거나_0이거나_null이면_충전_요청을_거부한다(){
		assertThrows(IllegalArgumentException.class, () -> userService.chargePoint(1L, new BigDecimal(-1000)));
		assertThrows(IllegalArgumentException.class, () -> userService.chargePoint(1L, new BigDecimal(0)));
		assertThrows(IllegalArgumentException.class, () -> userService.chargePoint(1L, null));
	}
	
	@Test
	void 사용자_정보가_null이거나_존재하지_않는_사용자면_충전_요청을_거부한다(){
		Long unExistUserId = 999L;
		given(userRepository.findById(unExistUserId)).willThrow(NotFoundException.class);
		
		assertThrows(NotFoundException.class, () -> userService.chargePoint(unExistUserId, new BigDecimal(1000)));
	}
	
	// 	포인트 차감하기
	@Test
	void 결제할_포인트가_현재_포인트보다_적다면_결제_요청을_거부한다() {
		Long userId = 1L;
		String username = "name";
		BigDecimal originMoney = new BigDecimal(1_000);
		BigDecimal payMoney = new BigDecimal(3_000);
		
		given(userRepository.findById(userId)).willReturn(Optional.of(new User(userId, username, originMoney)));
		
		assertThrows(IllegalArgumentException.class, () -> userService.deductPoint(userId, payMoney));
	}
}