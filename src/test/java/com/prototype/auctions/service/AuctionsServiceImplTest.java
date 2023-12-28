package com.prototype.auctions.service;

import com.prototype.auctions.dto.BidInputDTO;
import com.prototype.auctions.dto.BidOutputDTO;
import com.prototype.auctions.dto.ProductOutputDTO;
import com.prototype.auctions.dto.UserInputDTO;
import com.prototype.auctions.dto.UserOutputDTO;
import com.prototype.auctions.dto.WinnerInputDTO;
import com.prototype.auctions.entity.BidEntity;
import com.prototype.auctions.exception.BidException;
import com.prototype.auctions.exception.WinnerException;
import com.prototype.auctions.mapper.BidMapper;
import com.prototype.auctions.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AuctionsServiceImplTest {

    @Mock
    private BidRepository bidRepositoryMock;
    @Mock
    private BidMapper bidMapperMock;
    @Mock
    private RestTemplate restTemplateMock;

    @InjectMocks
    private AuctionsServiceImpl auctionsService;

    BidInputDTO bidInputDTO;

    WinnerInputDTO winnerInputDTO;
    String userToken;

    UserOutputDTO userOutputDTO;

    UserInputDTO userInputDTO;

    ProductOutputDTO productOutputDTO;

    @Value("${config.userUrl.get}")
    private String userApiUrl;

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(auctionsService,"getProductApiUrlBase","http://localhost/");
        ReflectionTestUtils.setField(auctionsService,"updateProductStatusApiUrl","http://localhost/2");
        userToken = UUID.randomUUID().toString();
        bidInputDTO = new BidInputDTO(1L,1.5F,userToken);
        winnerInputDTO = new WinnerInputDTO(1L);
        userOutputDTO = new UserOutputDTO("name",userToken, LocalDateTime.now(),null);
        userInputDTO = new UserInputDTO(userToken);
        productOutputDTO = new ProductOutputDTO(1L,"name",1.3F,true,userToken,null);
    }

    @Test
    void bid() {
        BidEntity bidEntity = new BidEntity();
        bidEntity.setBidPrice(bidInputDTO.bidPrice());
        bidEntity.setOwner(userToken);
        bidEntity.setProductId(bidInputDTO.productId());
        bidEntity.setUpdatedDate(LocalDateTime.now());

        BidOutputDTO responseDTO = new BidOutputDTO(bidEntity.getProductId(),bidEntity.getBidPrice(),bidEntity.getOwner(),null);

        HttpEntity<Void> requestEntity = getVoidHttpEntity();
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, requestEntity, ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(productOutputDTO,HttpStatus.OK));
        when(bidMapperMock.bidInputDTOToBidEntity(bidInputDTO)).thenReturn(bidEntity);
        when(bidRepositoryMock.save(bidEntity)).thenReturn(bidEntity);
        when(bidMapperMock.bidEntityToBidOutputDTO(bidEntity)).thenReturn(responseDTO);
        var response = auctionsService.bid(bidInputDTO,userToken);

        assertNotNull(response);
    }

    @Test
    void bidWhenInvalidUser() {
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(null);
        assertThrows(BidException.class,()->auctionsService.bid(bidInputDTO,userToken),"Unable to validate User");
    }
    @Test
    void bidWhenInvalidProduct() {
        HttpEntity<Void> requestEntity = getVoidHttpEntity();
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, requestEntity, ProductOutputDTO.class)).thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        assertThrows(BidException.class,()->auctionsService.bid(bidInputDTO,userToken),"Could not find Product: 1");
    }
    @Test
    void bidOnInActiveProduct() {
        HttpEntity<Void> requestEntity = getVoidHttpEntity();
        ProductOutputDTO productOp = productOutputDTO.withUpdatedInAuctionStatus(false);
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, requestEntity, ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(productOp,HttpStatus.OK));
        assertThrows(BidException.class,()->auctionsService.bid(bidInputDTO,userToken),"Bid Process Finished for: 1");
    }
    @Test
    void bidLessThanMinimumPrice() {
        HttpEntity<Void> requestEntity = getVoidHttpEntity();
        BidInputDTO inputDTO = new BidInputDTO(1L,1.2F,"");
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, requestEntity, ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(productOutputDTO,HttpStatus.OK));
        assertThrows(BidException.class,()->auctionsService.bid(inputDTO,userToken),"Bid Should be more tha Minimum Price : 1.34");
    }

    @Test
    void startAuction() {
    }

    @Test
    void stopAuctionAndUnableToUpdateStatusOfProduct() {
        List<BidEntity> listOfBids = getBidEntityList();

        ProductOutputDTO outputDTO = new ProductOutputDTO(1L, "name",1.3F,false,userToken, null);
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", userToken);
        HttpEntity<ProductOutputDTO> requestEntityForProductUpdate= new HttpEntity<>(outputDTO,headers);

        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, getVoidHttpEntity(), ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(productOutputDTO,HttpStatus.OK));
        when(restTemplateMock.exchange("http://localhost/2", HttpMethod.POST, requestEntityForProductUpdate, ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>((ProductOutputDTO) null,HttpStatus.OK));
        when(bidRepositoryMock.findByProductId(productOutputDTO.productId())).thenReturn(Optional.of(listOfBids));
        assertThrows(WinnerException.class,()->auctionsService.stopAuction(winnerInputDTO,userToken),"Unable to update product status.");
    }

    private List<BidEntity> getBidEntityList() {
        List<BidEntity> listOfBids = new ArrayList<>();
        for(long i=1;i<5;i++){
            BidEntity bidEntity = new BidEntity();
            bidEntity.setBidId(i);
            bidEntity.setOwner(userToken);
            bidEntity.setBidPrice(1.25F*i);
            bidEntity.setProductId(1L);
            bidEntity.setUpdatedDate(LocalDateTime.now());
            listOfBids.add(bidEntity);
        }
        return listOfBids;
    }

    @Test
    void stopAuction() {
        List<BidEntity> listOfBids = getBidEntityList();

        ProductOutputDTO outputDTO = new ProductOutputDTO(1L, "name",1.3F,false,userToken, null);

        HttpHeaders headers = new HttpHeaders();
        headers.set("token", userToken);
        HttpEntity<ProductOutputDTO> requestEntityForProductUpdate= new HttpEntity<>(outputDTO,headers);

        BidOutputDTO bidResponseDTO = new BidOutputDTO(1L,5.0F,userToken,null);

                when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, getVoidHttpEntity(), ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(productOutputDTO,HttpStatus.OK));
        when(restTemplateMock.exchange("http://localhost/2", HttpMethod.POST, requestEntityForProductUpdate, ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(outputDTO,HttpStatus.OK));
        when(bidRepositoryMock.findByProductId(productOutputDTO.productId())).thenReturn(Optional.of(listOfBids));
        when(bidMapperMock.bidEntityToBidOutputDTO(listOfBids.get(listOfBids.size()-1))).thenReturn(bidResponseDTO);
        var response = auctionsService.stopAuction(winnerInputDTO,userToken);
        assertNotNull(response);
        assertEquals(1,response.productId());
        assertEquals(5.0F,response.price());
        assertEquals("name",response.name());
    }

    @Test
    void stopAuctionInvalidUser() {
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(null);
        assertThrows(WinnerException.class,()->auctionsService.stopAuction(winnerInputDTO,userToken),"Unable to validate User");
    }
    @Test
    void stopAuctionWhenInvalidProduct() {
        HttpEntity<Void> requestEntity = getVoidHttpEntity();
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, requestEntity, ProductOutputDTO.class)).thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        assertThrows(WinnerException.class,()->auctionsService.stopAuction(winnerInputDTO,userToken),"Could not find Product: 1");
    }
    @Test
    void stopAuctionOnAlreadyDoneProduct() {
        HttpEntity<Void> requestEntity = getVoidHttpEntity();
        ProductOutputDTO productOp = productOutputDTO.withUpdatedInAuctionStatus(false);
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, requestEntity, ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(productOp,HttpStatus.OK));
        assertThrows(WinnerException.class,()->auctionsService.stopAuction(winnerInputDTO,userToken),"Auction already stopped for: 1");
    }
    @Test
    void stopAuctionByUnauthorisedPerson() {
        HttpEntity<Void> requestEntity = getVoidHttpEntity();
        ProductOutputDTO productOp = new ProductOutputDTO(1L,"name",1.35F,true,UUID.randomUUID().toString(),null);
        when(restTemplateMock.postForObject(eq(userApiUrl), eq(userInputDTO), eq(UserOutputDTO.class))).thenReturn(userOutputDTO);
        when(restTemplateMock.exchange("http://localhost/1", HttpMethod.GET, requestEntity, ProductOutputDTO.class)).thenReturn(new ResponseEntity<ProductOutputDTO>(productOp,HttpStatus.OK));
        assertThrows(WinnerException.class,()->auctionsService.stopAuction(winnerInputDTO,userToken),"You do not have rights to stop bid for this product: 1");
    }
    private HttpEntity<Void> getVoidHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", userToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        return requestEntity;
    }
}