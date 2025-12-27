package com.example.aggregator.tests.mockservice;

import com.example.common.Ticker;
import com.example.stock.PriceUpdate;
import com.example.stock.StockPriceRequest;
import com.example.stock.StockPriceResponse;
import com.example.stock.StockServiceGrpc;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

public class StockMockService extends StockServiceGrpc.StockServiceImplBase {

    @Override
    public void getStockPrice(StockPriceRequest request, StreamObserver<StockPriceResponse> responseObserver) {
        var response = StockPriceResponse.newBuilder().setPrice(15).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPriceUpdates(Empty request, StreamObserver<PriceUpdate> responseObserver) {
        for (int i = 0; i < 5; i++) {
            var priceUpdate = PriceUpdate.newBuilder().setPrice(i).setTicker(
                    Ticker.AMAZON).build();
            responseObserver.onNext(priceUpdate);
        }
        responseObserver.onCompleted();
    }
}
