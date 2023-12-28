package com.prototype.auctions.controller;

import com.prototype.auctions.dto.BidInputDTO;
import com.prototype.auctions.dto.BidOutputDTO;
import com.prototype.auctions.dto.WinnerInputDTO;
import com.prototype.auctions.dto.WinnerOutputDTO;
import com.prototype.auctions.exception.BidException;
import com.prototype.auctions.exception.WinnerException;
import com.prototype.auctions.service.AuctionsService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionsControllerTest {

    @Mock
    private AuctionsService auctionsServiceMock;
    @InjectMocks
    private AuctionsController auctionsController;
    @Mock
    HttpServletRequest request;

    BidInputDTO bidInputDTO;

    BidOutputDTO bidOutputDTO;

    WinnerInputDTO winnerInputDTO;

    WinnerOutputDTO winnerOutputDTO;
    String userToken = "233-343-erf";
    @BeforeEach
    void setUp(){
        bidInputDTO = new BidInputDTO(1L,2.5F,userToken);
        bidOutputDTO = new BidOutputDTO(1L, 2.5F, userToken,null);
        winnerInputDTO = new WinnerInputDTO(1L);
        winnerOutputDTO = new WinnerOutputDTO(1L,2.6F,"name", null);
    }
    @Test
    void bid() {
        when(request.getHeader("token")).thenReturn(userToken);
        when(auctionsServiceMock.bid(bidInputDTO,userToken)).thenReturn(bidOutputDTO);
        ResponseEntity<BidOutputDTO> bidOutputDTOResponseEntity = auctionsController.bid(bidInputDTO,request);
        assertNotNull(bidOutputDTOResponseEntity);
        assertTrue(bidOutputDTOResponseEntity.getStatusCode().is2xxSuccessful());
        assertEquals(1L,bidOutputDTOResponseEntity.getBody().productId());
        assertEquals(2.5F,bidOutputDTOResponseEntity.getBody().bidPrice());
    }

    @Test
    void bidNotSuccessfulThrowsBidException() {
        when(request.getHeader("token")).thenReturn(userToken);
        when(auctionsServiceMock.bid(bidInputDTO,userToken)).thenThrow(new BidException("Error Occurred"));
        ResponseEntity<BidOutputDTO> bidOutputDTOResponseEntity = auctionsController.bid(bidInputDTO,request);
        assertNotNull(bidOutputDTOResponseEntity);
        assertTrue(bidOutputDTOResponseEntity.getStatusCode().is4xxClientError());
        assertEquals(1L,bidOutputDTOResponseEntity.getBody().productId());
        assertEquals(2.5F,bidOutputDTOResponseEntity.getBody().bidPrice());
        assertEquals("Error Occurred", bidOutputDTOResponseEntity.getBody().error());
    }

    @Test
    void stopAuction() {
        when(request.getHeader("token")).thenReturn(userToken);
        when(auctionsServiceMock.stopAuction(winnerInputDTO,userToken)).thenReturn(winnerOutputDTO);
        ResponseEntity<WinnerOutputDTO> winnerOutputDTOResponseEntity = auctionsController.stopAuction(winnerInputDTO,request);
        assertNotNull(winnerOutputDTOResponseEntity);
        assertTrue(winnerOutputDTOResponseEntity.getStatusCode().is2xxSuccessful());
        assertEquals(1L,winnerOutputDTOResponseEntity.getBody().productId());
        assertEquals(2.6F,winnerOutputDTOResponseEntity.getBody().price());
    }

    @Test
    void stopAuctionNotSuccessfulThrowsWinnerException() {
        when(request.getHeader("token")).thenReturn(userToken);
        when(auctionsServiceMock.stopAuction(winnerInputDTO,userToken)).thenThrow(new WinnerException("Error"));
        ResponseEntity<WinnerOutputDTO> winnerOutputDTOResponseEntity = auctionsController.stopAuction(winnerInputDTO,request);
        assertNotNull(winnerOutputDTOResponseEntity);
        assertTrue(winnerOutputDTOResponseEntity.getStatusCode().is4xxClientError());
        assertEquals(1L,winnerOutputDTOResponseEntity.getBody().productId());
        assertEquals(-1F,winnerOutputDTOResponseEntity.getBody().price());
        assertEquals("Error",winnerOutputDTOResponseEntity.getBody().error());
    }
}