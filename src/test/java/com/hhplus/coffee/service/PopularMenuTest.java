package com.hhplus.coffee.service;

import com.hhplus.coffee.domain.menu.Menu;
import com.hhplus.coffee.service.order.Order;
import com.hhplus.coffee.service.order.OrderRepository;
import com.hhplus.coffee.user.User;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
public class PopularMenuTest {
	@Autowired
	private MenuService menuService;
	
	@Autowired
	private OrderRepository orderRepository;
	
	private User user;
	private Menu americano;
	private Menu latte;
	private Menu mocha;
	
	@BeforeEach
	void setUp() {
		user = userRepository.save(new User("tester", new BigDecimal(20_000)));
		
		americano = menuRepository.save(new Menu("아메리카노", new BigDecimal(3_000)));
		latte = menuRepository.save(new Menu("라떼", new BigDecimal(3_500)));
		mocha = menuRepository.save(new Menu("모카", new BigDecimal(4_000)));
		
		// 주문 생성
		createOrder(americano, 5);
		createOrder(latte, 3);
		createOrder(mocha, 1);
	}
	
	private void createOrder(Menu menu, int count) {
		for (int i = 0; i < count; i++) {
			orderRepository.save(new Order(user.getId(), menu.getId()));
		}
	}
	
	@Test
	void 인기메뉴_조회는_주문수_내림차순으로_반환된다() {
		// When
		List<PopularMenu> result = menuService.getPopularMenus();
		
		// Then
		assertThat(result).hasSize(3);
		assertThat(result.get(0)).extracting(PopularMenu::menuName, PopularMenu::orderCount)
				.containsExactly("아메리카노", 5);
		assertThat(result.get(1)).extracting(PopularMenu::menuName, PopularMenu::orderCount)
				.containsExactly("라떼", 3);
		assertThat(result.get(2)).extracting(PopularMenu::menuName, PopularMenu::orderCount)
				.containsExactly("모카", 1);
	}
	
	
	@Test
	void 주문과_인기메뉴_조회가_동시에_이뤄져도_둘다_성공한다() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch latch = new CountDownLatch(2);
		
		List<String> result = Collections.synchronizedList(new ArrayList<>());
		
		executor.submit(() -> {
			try {
				orderService.placeOrder(user.getId(), americano.getId());
				result.add("주문 성공");
			} catch (Exception e) {
				result.add("주문 실패");
			} finally {
				latch.countDown();
			}
		});
		
		executor.submit(() -> {
			try {
				List<PopularMenu> popularMenus = menuService.getPopularMenus();
				System.out.println("인기메뉴 조회 결과: " + popularMenus);
				result.add("인기메뉴 조회 성공");
			} catch (Exception e) {
				result.add("인기메뉴 조회 실패");
			} finally {
				latch.countDown();
			}
		});
		
		latch.await();
		
		System.out.println("결과: " + result);
		
		assertThat(result).containsExactlyInAnyOrder("주문 성공", "인기메뉴 조회 성공");
		assertThat(orderRepository.count()).isEqualTo(3);
		List<PopularMenu> popularMenus = menuService.getPopularMenus();
		assertThat(popularMenus.get(0).orderCount()).isEqualTo(3);
	}
	
}
