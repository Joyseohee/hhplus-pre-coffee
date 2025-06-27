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
}
