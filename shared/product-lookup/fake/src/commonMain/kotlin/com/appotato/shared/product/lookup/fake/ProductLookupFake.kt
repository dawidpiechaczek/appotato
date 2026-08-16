package com.appotato.shared.product.lookup.fake

import com.appotato.shared.product.lookup.api.Product
import com.appotato.shared.product.lookup.api.ProductLookup

/**
 * An in-memory [ProductLookup]. [result] is what the next call returns, so a test can set up a hit,
 * a miss (`Result.success(null)`) or a failure without touching the network.
 */
public class ProductLookupFake(
    public var result: Result<Product?> = Result.success(null)
) : ProductLookup {

    /** Every barcode asked for, in order — enough to assert that a scan triggered exactly one call. */
    public val requestedBarcodes: MutableList<String> = mutableListOf()

    override suspend fun byBarcode(barcode: String): Result<Product?> {
        requestedBarcodes += barcode
        return result
    }
}
