package com.prototype.auctions.service;

import com.netflix.discovery.EurekaClient;
import com.prototype.auctions.dto.BidInputDTO;
import com.prototype.auctions.dto.BidOutputDTO;
import com.prototype.auctions.dto.ProductOutputDTO;
import com.prototype.auctions.dto.StartAuctionDTO;
import com.prototype.auctions.dto.UserInputDTO;
import com.prototype.auctions.dto.UserOutputDTO;
import com.prototype.auctions.dto.WinnerInputDTO;
import com.prototype.auctions.dto.WinnerOutputDTO;
import com.prototype.auctions.entity.BidEntity;
import com.prototype.auctions.exception.BidException;
import com.prototype.auctions.exception.WinnerException;
import com.prototype.auctions.mapper.BidMapper;
import com.prototype.auctions.repository.BidRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionsServiceImpl implements AuctionsService {

    @Value("${config.products.get}")
    private String getProductApiUrlBase;
    @Value("${config.products.updateInAuctionStatus}")
    private String updateProductStatusApiUrl;
    @Value("${config.userUrl.get}")
    private String userApiUrl;
    private final BidRepository bidRepository;
    private final BidMapper bidMapper;
    private final RestTemplate restTemplate;
    private final EurekaClient eurekaClient;

    @Autowired
    public AuctionsServiceImpl(BidRepository bidRepository,
                               BidMapper bidMapper,
                               RestTemplate restTemplate,
                               EurekaClient eurekaClient) {

        this.bidRepository = bidRepository;
        this.bidMapper = bidMapper;
        this.restTemplate = restTemplate;
        this.eurekaClient = eurekaClient;
    }


    @Override
    public BidOutputDTO bid(final BidInputDTO bidInputDTO, final String userToken) throws BidException {

        //Validate User
        UserOutputDTO userOutputDTO = getUserFromToken(userToken);
        if (userOutputDTO == null || (userOutputDTO.error() != null)) {
            throw new BidException("Unable to validate User");
        }

        //Update Bid data with User
        BidInputDTO decoratedBidDTO = bidInputDTO.withUpdatedOwner(userToken);

        //Get Product Details
        var productOutputDTOFromProductDB = getProductFromId(bidInputDTO.productId(), userToken);

        //If product Exist
        if (productOutputDTOFromProductDB != null && (productOutputDTOFromProductDB.error() == null || productOutputDTOFromProductDB.error().isEmpty())) {
            if (!productOutputDTOFromProductDB.inAuction()) {
                throw new BidException("Bid Process Finished for: " + productOutputDTOFromProductDB.productId());
            } else if (productOutputDTOFromProductDB.minimumPrice() > bidInputDTO.bidPrice()) {
                throw new BidException("Bid Should be more tha Minimum Price : " + productOutputDTOFromProductDB.minimumPrice());
            }
            //Save the bid
            BidEntity bidEntity = bidMapper.bidInputDTOToBidEntity(decoratedBidDTO);
            var result = bidRepository.save(bidEntity);
            BidOutputDTO responseDTO = bidMapper.bidEntityToBidOutputDTO(result);
            return responseDTO;
        } else {
            throw new BidException("Could not find Product: " + bidInputDTO.productId());
        }
    }

    @Override
    public void startAuction(final StartAuctionDTO productDTO) {
        throw new UnsupportedOperationException("Need to implement");
    }

    @Override
    @Transactional
    public WinnerOutputDTO stopAuction(final WinnerInputDTO winnerInputDTO, final String userToken) throws WinnerException {
        //Validate User
        UserOutputDTO userOutputDTO = getUserFromToken(userToken);
        if (userOutputDTO == null || (userOutputDTO.error() != null)) {
            throw new WinnerException("Unable to validate User");
        }

        var productOutputDTOFromProductDB = getProductFromId(winnerInputDTO.productId(), userToken);
        if (productOutputDTOFromProductDB != null && (productOutputDTOFromProductDB.error() == null || productOutputDTOFromProductDB.error().isEmpty())) {
            if (!productOutputDTOFromProductDB.inAuction()) {
                throw new WinnerException("Auction already stopped for: " + productOutputDTOFromProductDB.productId());
            } else if (!productOutputDTOFromProductDB.owner().equals(userOutputDTO.token())) {
                throw new WinnerException("You do not have rights to stop bid for this product: " + productOutputDTOFromProductDB.productId());
            }
            var result = bidRepository.findByProductId(productOutputDTOFromProductDB.productId());
            if (result.isPresent() && !(result.get().isEmpty())) {
                List<BidEntity> allBidsForProductId = result.get();
                var priceComparator = Comparator.comparingDouble(BidEntity::getBidPrice);
                var dateComparator = Comparator.comparing(BidEntity::getUpdatedDate).reversed();
                Optional<BidEntity> maxBidEntity = allBidsForProductId.stream().max(priceComparator.thenComparing(dateComparator));

                var updatedProduct = updateProductAuctionStatus(productOutputDTOFromProductDB, userToken);
                if (updatedProduct == null || (updatedProduct.error() != null && !updatedProduct.error().isEmpty())) {
                    throw new WinnerException("Unable to update product status.");
                }
                BidOutputDTO bidResponseDTO = bidMapper.bidEntityToBidOutputDTO(maxBidEntity.get());
                UserOutputDTO userDTO = getUserFromToken(maxBidEntity.get().getOwner());
                return new WinnerOutputDTO(bidResponseDTO.productId(), bidResponseDTO.bidPrice(), userDTO.name(), null);
            } else {
                throw new WinnerException("No Bids found");
            }

        } else {
            throw new WinnerException("Could not find Product: " + winnerInputDTO.productId());
        }
    }

    private UserOutputDTO getUserFromToken(final String userToken) {
        UserInputDTO userInputDTO = new UserInputDTO(userToken);
        var eurekaInstance = eurekaClient.getNextServerFromEureka("USERS", false);
        var retrievedUser = restTemplate.postForObject(eurekaInstance.getHomePageUrl()+userApiUrl, userInputDTO, UserOutputDTO.class);
        return retrievedUser;
    }

    private ProductOutputDTO getProductFromId(final Long productId, final String userToken) {
        var eurekaInstance = eurekaClient.getNextServerFromEureka("PRODUCTS", false);
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", userToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        var productOutputDTO = restTemplate.exchange(
                eurekaInstance.getHomePageUrl()+getProductApiUrlBase + productId, HttpMethod.GET, requestEntity, ProductOutputDTO.class);
        return productOutputDTO.getBody();
    }

    private ProductOutputDTO updateProductAuctionStatus(final ProductOutputDTO productOutputDTOFromProductDB, final String userToken) {
        var eurekaInstance = eurekaClient.getNextServerFromEureka("PRODUCTS", false);
        ProductOutputDTO updatedDTO = productOutputDTOFromProductDB.withUpdatedInAuctionStatus(false);
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", userToken);
        HttpEntity<ProductOutputDTO> requestEntity = new HttpEntity<>(updatedDTO, headers);
        var productOutputDTO = restTemplate.exchange(eurekaInstance.getHomePageUrl()+updateProductStatusApiUrl, HttpMethod.POST, requestEntity, ProductOutputDTO.class);
        return productOutputDTO.getBody();
    }
}
