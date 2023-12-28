package com.prototype.auctions.controller;

import com.prototype.auctions.dto.BidInputDTO;
import com.prototype.auctions.dto.BidOutputDTO;
import com.prototype.auctions.dto.WinnerInputDTO;
import com.prototype.auctions.dto.WinnerOutputDTO;
import com.prototype.auctions.exception.BidException;
import com.prototype.auctions.exception.WinnerException;
import com.prototype.auctions.service.AuctionsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
public class AuctionsController {
    private AuctionsService auctionsService;

    @Autowired
    public AuctionsController(AuctionsService auctionsService) {
        this.auctionsService = auctionsService;
    }

    @PostMapping("/bid")
    public ResponseEntity<BidOutputDTO> bid(@RequestBody BidInputDTO bidInputDTO,
                                            HttpServletRequest request) {
        try {
            var result = auctionsService.bid(bidInputDTO, request.getHeader("token"));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (BidException e) {
            BidOutputDTO errorDTO = new BidOutputDTO(bidInputDTO.productId(),
                    bidInputDTO.bidPrice(), bidInputDTO.owner(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
        }
    }

    @PostMapping("/stopAuction")
    public ResponseEntity<WinnerOutputDTO> stopAuction(@RequestBody WinnerInputDTO winnerInputDTO,
                                                       HttpServletRequest request) {
        try {
            var result = auctionsService.stopAuction(winnerInputDTO, request.getHeader("token"));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (WinnerException e) {
            WinnerOutputDTO errorDTO = new WinnerOutputDTO(winnerInputDTO.productId(), -1F, "", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
        }
    }
}