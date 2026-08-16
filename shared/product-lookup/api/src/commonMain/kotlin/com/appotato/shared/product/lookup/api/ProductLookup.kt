package com.appotato.shared.product.lookup.api

/**
 * Turns a barcode into what is known about the product behind it.
 *
 * The contract names no database on purpose — which one the app queries is a decision that lives in
 * the implementation module, and swapping it must not reach any caller.
 */
public interface ProductLookup {

    /**
     * A `null` success is a barcode nobody has catalogued yet — an ordinary outcome for a local
     * shop's own-brand item, and not the same thing as the lookup failing. A `Result.failure` means
     * the question could not be asked at all: no connection, a timeout, a broken response.
     */
    public suspend fun byBarcode(barcode: String): Result<Product?>
}
