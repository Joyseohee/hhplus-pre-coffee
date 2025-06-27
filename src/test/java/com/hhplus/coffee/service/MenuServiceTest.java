package com.hhplus.coffee.service;

import com.hhplus.coffee.domain.menu.Menu;
import com.hhplus.coffee.service.menu.MenuRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MenuServiceTest {
	@Mock
	private MenuRepository menuRepository;
	@InjectMocks
	private MenuService menuService;
	
	// 1. 커피 메뉴 목록 조회 API
	//     - 커피 정보(메뉴ID, 이름, 가격)을 조회하는 API를 작성합니다.
	@Test
	void 메뉴_목록을_조회한다() {
		// given
		List<Menu> mockMenus = List.of(
				new Menu(1L, "아메리카노", new BigDecimal(3000)),	// 메뉴id, 이름, 가격
				new Menu(2L, "카페라떼", new BigDecimal(3500))
		);
		when(menuRepository.findAll()).thenReturn(mockMenus);
		
		// when
		List<Menu> result = menuService.getWholeMenus();
		
		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("아메리카노");
		assertThat(result.get(1).getPrice()).isEqualTo(new BigDecimal(3500));
		verify(menuRepository, times(1)).findAll();
	}
}
