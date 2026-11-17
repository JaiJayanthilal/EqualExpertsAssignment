# Shopping Cart

A shopping cart library for the Equal Experts take-home test. Products are added with a quantity, prices come from the Equal Experts price API, and the cart works out the subtotal, tax (12.5%) and total.

As the brief asks, this is a library exercised through unit tests, not an application.

## Requirements

- Java 17+
- Maven 3

## Running the tests

```
mvn test
```

The tests don't call the real price API. The cart is tested with a stubbed price provider, and the API client is tested against a local HTTP server.

## Usage

```java
Cart cart = new Cart(new PriceApiClient());

cart.addProduct("cornflakes", 1);
cart.addProduct("cornflakes", 1);
cart.addProduct("weetabix", 1);

cart.getItems();     // 2 x cornflakes, 1 x weetabix
cart.getSubtotal();  // 15.02
cart.getTax();       // 1.88
cart.getTotal();     // 16.90
```

## Design

| Class | Responsibility |
|---|---|
| `Cart` | Holds the items and calculates subtotal, tax and total |
| `CartItem` | One product in the cart: name, quantity and unit price |
| `PriceProvider` | Interface for looking up a price, so the cart can be tested without the network |
| `PriceApiClient` | Fetches prices from `https://equalexperts.github.io/backend-take-home-test-data/{product}.json` |
| `PriceResponse` | The JSON response from the price API |
| `PriceLookupException` | The API answered, but not with a usable price |

## Assumptions

- **Totals are rounded up.** The brief says "Totals should be rounded up where required", so subtotal and tax are rounded up to 2 decimal places (`RoundingMode.UP`), not to the nearest value. For example, tax on 0.10 is 0.0125, which becomes 0.02.
- **The price is fetched on the first add and reused.** Adding more of a product already in the cart increases its quantity without calling the API again, so one product always has one price in the cart.
- **Quantity must be positive.** Zero or negative quantities, and blank product names, are rejected before any API call.
- **Product names are restricted to letters, digits, `-` and `_`.** The name becomes part of the request URL, so anything else (for example `../foo`) is rejected rather than sent.
- **Unknown products fail clearly.** A 404 from the API becomes `PriceLookupException("Unknown product: ...")`, and the cart is left unchanged.
- **A missing or negative price is an error.** It's reported when the response is read, rather than failing later in a calculation.

## Trade-offs

- **No retries, caching or circuit breaker**, to keep with the brief. In production I'd add them with Resilience4j and Caffeine rather than writing them by hand.
- **HTTP timeouts are set** (2 s connect, 3 s read), so a slow API can't block the caller indefinitely.
- **`HttpURLConnection` and Gson** are used instead of a larger HTTP client or framework, to keep dependencies small.
- **The cart is not thread-safe.** A cart belongs to one shopper, so no locking was added.
