package org.example.lab2.reader;

import org.example.lab2.model.Product;

import java.util.List;

/**
 Common interface for file-based data access.
 The active implementation is selected via @Profile + @ConditionalOnExpression.
 */
public interface FileReader {

    /**
     * Load all products from the configured data file.
     *
     * @return unmodifiable list of products loaded from the file
     */
    List<Product> loadProducts();

    /**
     * Returns a human-readable description of the reader type,
     * used by the custom Actuator endpoint.
     */
    String getReaderType();
}
