package com.prototype.auctions.mapper;

import com.prototype.auctions.dto.BidInputDTO;
import com.prototype.auctions.dto.BidOutputDTO;
import com.prototype.auctions.entity.BidEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BidMapper {

    BidEntity bidInputDTOToBidEntity(BidInputDTO bidInputDTO);

    BidOutputDTO bidEntityToBidOutputDTO(BidEntity bidEntity);
}
