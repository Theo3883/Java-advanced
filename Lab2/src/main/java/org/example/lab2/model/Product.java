package org.example.lab2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a product record loaded from a data file.
 */

public class Product {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("price")
    private double price;

    @JsonProperty("category")
    private String category;

    public Product() {}

    public Product(int id, String name, double price, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%.2f, category='%s'}",
                id, name, price, category);
    }
}
