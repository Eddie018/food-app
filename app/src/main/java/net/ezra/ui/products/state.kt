package net.ezra.ui.products

import androidx.compose.runtime.mutableStateListOf

object CartState {
    // Use mutableStateListOf to create a list that triggers recomposition
    // when items are added or removed.
    val cartItems = mutableStateListOf<CartItem>()

    fun addToCart(cartItem: CartItem) {
        cartItems.add(cartItem) // Adds the cart item to the mutable list
    }
}

