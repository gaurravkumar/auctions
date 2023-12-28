package com.prototype.auctions.service;

import com.prototype.auctions.dto.BidInputDTO;
import com.prototype.auctions.dto.BidOutputDTO;
import com.prototype.auctions.dto.ProductOutputDTO;
import com.prototype.auctions.dto.StartAuctionDTO;
import com.prototype.auctions.dto.WinnerInputDTO;
import com.prototype.auctions.dto.WinnerOutputDTO;

public interface AuctionsService {
    BidOutputDTO bid(BidInputDTO bidInputDTO, String userToken);
    WinnerOutputDTO stopAuction(WinnerInputDTO winnerInputDTO, String token);
    void startAuction(StartAuctionDTO startAuctionDTO);
}
