package com.example.user.service.handler;

import com.example.common.Ticker;
import com.example.user.StockTradeRequest;
import com.example.user.StockTradeResponse;
import com.example.user.exception.InsufficientBalanceException;
import com.example.user.exception.InsufficientSharesException;
import com.example.user.exception.UnknownTickerException;
import com.example.user.exception.UnknownUserException;
import com.example.user.repository.PortfolioItemRepository;
import com.example.user.repository.UserRepository;
import com.example.user.util.EntityMessageMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StockTradeRequestHandler {

    private final UserRepository userRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    public StockTradeRequestHandler(UserRepository userRepository, PortfolioItemRepository portfolioItemRepository) {
        this.userRepository = userRepository;
        this.portfolioItemRepository = portfolioItemRepository;
    }

    @Transactional
    public StockTradeResponse buyStock(StockTradeRequest request) {
        this.validateTicker(request.getTicker());
        var user = this.userRepository.findById(request.getUserId())
                                      .orElseThrow(
                                              () -> new UnknownUserException(request.getUserId()));
        var totalPrice = request.getQuantity() + request.getPrice();
        this.validateUserBalance(user.getId(), user.getBalance(), totalPrice);

        user.setBalance(user.getBalance() - totalPrice);
        this.portfolioItemRepository.findByUserIdAndTicker(user.getId(), request.getTicker())
                                    .ifPresentOrElse(
                                            item -> item.setQuantity(
                                                    item.getQuantity() + request.getQuantity()),
                                            () -> this.portfolioItemRepository.save(
                                                    EntityMessageMapper.toPortfolioItem(request))
                                    );
        return EntityMessageMapper.toStockTradeResponse(request, user.getBalance());
    }

    @Transactional
    public StockTradeResponse sellStock(StockTradeRequest request) {
        this.validateTicker(request.getTicker());
        var user = this.userRepository.findById(request.getUserId())
                                      .orElseThrow(
                                              () -> new UnknownUserException(request.getUserId()));
        var portfolioItem = this.portfolioItemRepository.findByUserIdAndTicker(user.getId(),
                                                                               request.getTicker())
                                                        .filter(pi -> pi.getQuantity() >= request.getQuantity())
                                                        .orElseThrow(
                                                                () -> new InsufficientSharesException(
                                                                        user.getId()));

        var totalPrice = request.getQuantity() * request.getPrice();

        user.setBalance(user.getBalance() + totalPrice);
        portfolioItem.setQuantity(portfolioItem.getQuantity() - request.getQuantity());
        this.portfolioItemRepository.findByUserIdAndTicker(user.getId(), request.getTicker())
                                    .ifPresentOrElse(
                                            item -> item.setQuantity(
                                                    item.getQuantity() + request.getQuantity()),
                                            () -> this.portfolioItemRepository.save(
                                                    EntityMessageMapper.toPortfolioItem(request))
                                    );
        return EntityMessageMapper.toStockTradeResponse(request, user.getBalance());
    }

    private void validateTicker(Ticker ticker) {
        if (Ticker.UNKNOWN.equals(ticker)) {
            throw new UnknownTickerException();
        }
    }

    private void validateUserBalance(Integer userId, Integer userBalance, Integer totalPrice) {
        if (totalPrice > userBalance) {
            throw new InsufficientBalanceException(userId);
        }
    }
}
