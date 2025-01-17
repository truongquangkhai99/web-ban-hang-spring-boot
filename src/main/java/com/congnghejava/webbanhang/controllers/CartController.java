package com.congnghejava.webbanhang.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.congnghejava.webbanhang.models.Cart;
import com.congnghejava.webbanhang.models.Product;
import com.congnghejava.webbanhang.models.User;
import com.congnghejava.webbanhang.payload.request.CartRequest;
import com.congnghejava.webbanhang.payload.response.CartResponse;
import com.congnghejava.webbanhang.payload.response.MessageResponse;
import com.congnghejava.webbanhang.security.CurrentUser;
import com.congnghejava.webbanhang.security.UserPrincipal;
import com.congnghejava.webbanhang.services.CartService;
import com.congnghejava.webbanhang.services.ProductService;
import com.congnghejava.webbanhang.services.UserService;

@RestController
@RequestMapping("/cart")
public class CartController {
	@Autowired
	UserService userService;

	@Autowired
	CartService cartService;

	@Autowired
	ProductService productService;

	@GetMapping
	@PreAuthorize("hasRole('ROLE_USER')")
	public ResponseEntity<?> getAllCart(@CurrentUser UserPrincipal userPrincipal) {
		User user = userService.getCurrentUser(userPrincipal);
		List<CartResponse> cartResponse = cartService.findByUser(user).stream().map(cart -> new CartResponse(cart))
				.collect(Collectors.toList());

		return ResponseEntity.status(HttpStatus.OK).body(cartResponse);
	}

	@PostMapping
	@PreAuthorize("hasRole('ROLE_USER')")
	public ResponseEntity<?> addCart(@RequestBody Long productId, @CurrentUser UserPrincipal userPrincipal) {
		Product product = productService.findById(productId);

		User user = userService.getCurrentUser(userPrincipal);

		if (product.getQuantity() == 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Sản phẩm đã hết"));
		}

		if (cartService.existsByProductAndUser(product, user)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new MessageResponse("Sản phẩm đã có trong giỏ hàng"));
		}

		product.setQuantity(product.getQuantity() - 1);

		Cart cart = new Cart(user, product, 1, true);
		cartService.save(cart);
		productService.save(product);

		return ResponseEntity.status(HttpStatus.OK).body("Đã thêm sản phẩm vào giỏ hàng");
	}

	@PutMapping("/{cartId}")
	@PreAuthorize("hasRole('ROLE_USER')")
	public ResponseEntity<?> updateQuantity(@PathVariable Long cartId, @RequestBody CartRequest cartRequest,
			@CurrentUser UserPrincipal userPrincipal) {

		Cart oldCart = cartService.findById(cartId);
		Product product = productService.findById(oldCart.getProduct().getId());
		User user = userService.getCurrentUser(userPrincipal);

		if (oldCart.getEnable() != cartRequest.getEnable()) {
			if (cartRequest.getEnable()) {
				if (product.getQuantity() < cartRequest.getQuantity()) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Không đủ sản phẩm"));
				}
				product.setQuantity(product.getQuantity() - cartRequest.getQuantity());
			} else {
				product.setQuantity(product.getQuantity() + cartRequest.getQuantity());
			}
		}

		if (oldCart.getQuantity() != cartRequest.getQuantity() && oldCart.getEnable() == cartRequest.getEnable()) {
			if (cartRequest.getEnable()) {
				if (product.getQuantity() < (cartRequest.getQuantity() - oldCart.getQuantity())) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Không đủ sản phẩm"));
				}
				product.setQuantity(product.getQuantity() + oldCart.getQuantity() - cartRequest.getQuantity());
			}
		}

		Cart cart = new Cart(user, product, cartRequest.getQuantity(), cartRequest.getEnable());
		cart.setId(cartId);

		cartService.save(cart);
		productService.save(product);

		return ResponseEntity.status(HttpStatus.OK).body("Cart item updated");
	}

	@DeleteMapping("/{cartId}")
	@PreAuthorize("hasRole('ROLE_USER')")
	public ResponseEntity<?> deleteCart(@PathVariable Long cartId) {
		Cart cart = cartService.findById(cartId);
		Product product = productService.findById(cart.getProduct().getId());

		if (cart.getEnable()) {
			product.setQuantity(product.getQuantity() + cart.getQuantity());
		}
		cartService.remove(cartId);
		productService.save(product);
		return ResponseEntity.status(HttpStatus.OK)
				.body(new MessageResponse("Đã xóa " + cart.getProduct().getName() + " khỏi giỏ hàng"));
	}
}
