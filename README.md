# auctions
Microservice to manage auctions. The main functions of this Service is to provide bidding and stop the auction functionality.
# Access to the service
The service can be accessed as follows:

## Bid :
The person can bid any number of times till the auction is stopped. The bid price has to be greater than the minimum price of the product.
- URL : [BID](http://localhost:8080/api/auctions/bid)
- Header: The header should contain a valid token which the user will get as response from user service when they register. This token must be passed in the header like a key value pair
  _token:f621f84b-d3f1-4dab-bcf5-6cbd7746a8bf_

```
Sample Request: token has to be provided in the header.
{
    "productId": 2,
    "bidPrice": 11.23

}
```

## Stop Auction:
This method is used to stop the auction, update the product option status and get the name of the winner.
The auction can be stopped only by the product owner. An owner is the one who registered the product.
Again, the header has to be present with token like _token:f621f84b-d3f1-4dab-bcf5-6cbd7746a8bf_
- URL: [STOP AUCTION](http://localhost:8080/api/auctions/stopAuction)
- Header: The header should contain a valid token which the user will get as response from user service when they register. This token must be passed in the header like a key value pair
    _token:f621f84b-d3f1-4dab-bcf5-6cbd7746a8bf_
```
{
    "productId": 2

}

```

## Set up

- Clone the project on you local machine IDE(Eclipse or IntelliJ)
- Build the Project
- Start the project

## How can all of it can be done
- Start all the projects .. Users, Products, auctions
- Register some users
- Register some products
- Do some bids
- Stop the Auction
- The result will be the product price and name of the winner

## More on Eureka Server Project folder


