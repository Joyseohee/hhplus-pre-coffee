package com.hhplus.coffee.service;

import com.hhplus.coffee.service.order.OrderRepository;
import com.hhplus.coffee.user.User;
import com.hhplus.coffee.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class OrderServiceRealTest {
	
	@Autowired
	private OrderService orderService;
	@Autowired
	private UserService userService;
	@Autowired
	private MenuService menuService;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private UserRepository userRepository;
	
	// 동시성 이슈 - 3번 API에서부터
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
		
		assertThat(orderRepository.count()).isBetween(0L, 1L);
		User updated = userRepository.findById(user.getId()).orElseThrow();
		assertThat(updated.getPoint()).isIn(userPoint.add(chargingPoint), userPoint.subtract(menuPrice));
	}
}
