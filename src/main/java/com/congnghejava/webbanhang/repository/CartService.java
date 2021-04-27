package com.congnghejava.webbanhang.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.congnghejava.webbanhang.models.Cart;
import com.congnghejava.webbanhang.models.Product;
import com.congnghejava.webbanhang.models.User;

@Service
public class CartService {
	@Autowired
	CartRepository cartRepository;

	public Cart findById(Long id) {
		return cartRepository.findById(id).get();
	}

	public List<Cart> findAllByUser(User user) {
		return cartRepository.findAllByUser(user);
	}

	public boolean existsByProductAndUser(Product product, User user) {
		return cartRepository.existsByProductAndUser(product, user);
	}

	public Cart save(Cart cart) {
		return cartRepository.save(cart);
	}

	public void remove(Long id) {
		cartRepository.deleteById(id);
	}
}