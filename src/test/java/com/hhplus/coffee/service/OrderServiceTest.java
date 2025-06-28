package com.hhplus.coffee.service;

import com.hhplus.coffee.domain.menu.Menu;
import com.hhplus.coffee.service.order.Order;
import com.hhplus.coffee.service.order.OrderRepository;
import com.hhplus.coffee.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderServiceTest {
	@Mock
	private OrderRepository orderRepository;
	
	@Mock
	private UserService userService;
	
	@Mock
	private MenuService menuService;
	
	@InjectMocks
	private OrderService orderService;
	
	private User overPointUser = new User(1L, "rich", new BigDecimal(5000));
	private User lowerPointUser = new User(2L, "poor", new BigDecimal(1000));
	private Menu menu = new Menu(10L, "아메리카노", new BigDecimal(3000));
	
	// 3. 커피 주문/결제 하기 API
	//     - 사용자 식별값, 메뉴ID를 입력 받아 주문을 하고 결제를 진행합니다.
	//     - 결제는 포인트로만 가능하며, 충전한 포인트에서 주문금액을 차감합니다.
	//     - 주문 내역을 데이터 수집 플랫폼으로 실시간 전송하는 로직을 추가합니다.
	//     (Mock API 등을 사용하여 사용자 식별값, 메뉴ID, 결제금액을 전송합니다.)
	@Test
	void 사용자_식별값과_메뉴ID를_입력받아_정상적으로_주문_결제한다() {
		Long menuId = menu.getId();
		BigDecimal menuPrice = menu.getPrice();
		Long userId = overPointUser.getId();
		BigDecimal userPoint = overPointUser.getPoint();
		
		given(menuService.getMenuPrice(menuId)).willReturn(menuPrice);
		given(userService.deductPoint(userId, menuPrice)).willReturn(userPoint.subtract(menuPrice));
		
		orderService.placeOrder(userId, menuId);
		
		// 주문 저장 여부
		verify(menuService).getMenuPrice(menuId);
		verify(userService).deductPoint(userId, menuPrice);
		verify(orderRepository).save(any(Order.class));
	}
	
	@Test
	void 포인트가_부족하면_결제를_거부한다(){
		Long menuId = menu.getId();
		BigDecimal menuPrice = menu.getPrice();
		Long userId = lowerPointUser.getId();
		BigDecimal userPoint = lowerPointUser.getPoint();
		
		given(menuService.getMenuPrice(menuId)).willReturn(menuPrice);
		given(userService.deductPoint(userId, menuPrice)).willThrow(IllegalArgumentException.class);
		
		orderService.placeOrder(userId, menuId);
		
		verify(menuService).getMenuPrice(menuId);
		verify(userService, never()).deductPoint(userId, menuPrice);
		verify(orderRepository, never()).save(any(Order.class));
	}
	
	@Test
	void 존재하지_않는_유저의_정보로는_결제를_거부한다(){
		Long menuId = menu.getId();
		BigDecimal menuPrice = menu.getPrice();
		Long unExistUserId = 0L;
		
		given(menuService.getMenuPrice(menuId)).willReturn(menuPrice);
		given(userService.deductPoint(unExistUserId)).willThrow(IllegalArgumentException.class);
		
		orderService.placeOrder(unExistUserId, menuId);
		
		verify(menuService).getMenuPrice(menuId);
		verify(userService).deductPoint(userId, menuPrice);
		verify(orderRepository, never()).save(any(Order.class));
	}
	
	@Test
	void 존재하지_않는_메뉴로는_주문을_거부한다(){
		Long unExistMenuId = 0L;
		Long userId = overPointUser.getId();
		BigDecimal userPoint = overPointUser.getPoint();
		
		given(menuService.getMenuPrice(unExistMenuId)).willThrow(IllegalArgumentException.class);
		given(userService.deductPoint(userId, null)).willThrow(IllegalArgumentException.class);
		
		orderService.placeOrder(userId, unExistMenuId);
		
		verify(menuService).getMenuPrice(unExistMenuId);
		verify(userService, never()).deductPoint(userId, null);
		verify(orderRepository, never()).save(any(Order.class));
	}
	
	@Test
	void 같은_유저가_동시에_주문을_하면_한쪽은_실패한다() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch latch = new CountDownLatch(2);
		List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
		
		Long menuId = menu.getId();
		Long userId = overPointUser.getId();
		
		for (int i = 0; i < 2; i++) {
			executor.submit(() -> {
				try {
					orderService.placeOrder(userId, menuId);
				} catch (Throwable t) {
					errors.add(t);
				} finally {
					latch.countDown();
				}
			});
		}
		
		latch.await();
		
		assertEquals(1, orderRepository.count());
		assertEquals(1, errors.size()); // 하나는 실패
	}
	
	
	
	@Test
	void 같은_유저가_동시에_주문과_충전을_하면_한쪽은_실패한다() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch latch = new CountDownLatch(2);
		
		List<String> result = Collections.synchronizedList(new ArrayList<>());
		
		Long menuId = menu.getId();
		BigDecimal menuPrice = menu.getPrice();
		Long userId = overPointUser.getId();
		BigDecimal userPoint = overPointUser.getPoint();
		
		BigDecimal chargingPoint = new BigDecimal(5000);
		
		
		executor.submit(() -> {
			try {
				userService.chargePoint(userId, chargingPoint);
				result.add("충전 성공");
			} catch (Exception e) {
				result.add("충전 실패");
			} finally {
				latch.countDown();
			}
		});
		
		executor.submit(() -> {
			try {
				orderService.placeOrder(userId, menuId);
				result.add("결제 성공");
			} catch (Exception e) {
				result.add("결제 실패");
			} finally {
				latch.countDown();
			}
		});
		
		latch.await();
		executor.shutdown();
		
		// Then
		System.out.println("결과: " + result);
		assertThat(result).containsExactlyInAnyOrder("충전 성공", "결제 실패")
				.or()
				.containsExactlyInAnyOrder("충전 실패", "결제 성공");
		
		// assertThat(orderRepository.count()).isBetween(0L, 1L);
		// User updated = userRepository.findById(user.getId()).orElseThrow();
		// assertThat(updated.getPoint()).isIn(userPoint.add(chargingPoint), userPoint.subtract(menuPrice));
	}
	
	// 	7일간 가장 많이 ORDER
}