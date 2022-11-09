package org.alkemy.wallet.service.impl;

import org.alkemy.wallet.dto.AccountDto;

import org.alkemy.wallet.exception.BadRequestException;
import org.alkemy.wallet.exception.ForbiddenException;
import org.alkemy.wallet.exception.NotFoundException;

import org.alkemy.wallet.mapper.AccountMapper;
import org.alkemy.wallet.model.Account;
import org.alkemy.wallet.model.Currency;
import org.alkemy.wallet.model.RoleName;
import org.alkemy.wallet.model.User;
import org.alkemy.wallet.repository.IAccountRepository;
import org.alkemy.wallet.repository.IUserRepository;
import org.alkemy.wallet.service.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements IAccountService {

    private final IAccountRepository accountRepository;

    private final IUserRepository userRepository;

    private final AccountMapper accountMapper;

    @Autowired
    public AccountServiceImpl(IAccountRepository accountRepository, IUserRepository userRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public List<AccountDto> findAllByUser(Long userId) {
        String loggedUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedUser = userRepository.findByEmail(loggedUserEmail);

        if(loggedUser.getRole().getRoleName() != RoleName.ADMIN)
            throw new ForbiddenException("You do not have permission to enter.");

        if (userId == null || userId <= 0)
            throw new NotFoundException("User id is not valid.");

        Optional<User> userOptional = userRepository.findById(userId);

        if(userOptional.isPresent()){

            List<Account> accounts = accountRepository.findAllByUser(userOptional.get());

            List<AccountDto> accountsDto = new ArrayList<>();

            if(!accounts.isEmpty()){

                for (Account account: accounts) {

                    AccountDto accountDto = accountMapper.accountToAccountDto(account);

                    accountsDto.add(accountDto);

                }
            }

            return accountsDto;

        } else {

            throw new NotFoundException("User not found");

        }

    }

    @Override
    @Transactional
    public AccountDto createAccount(Currency currency) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        return createAccount(user, currency);
    }

    @Override
    @Transactional
    public  AccountDto createAccount(User user, Currency currency){
        List<Account> accounts = accountRepository.findAllByUser(user);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        accounts.forEach(account -> {
            if (account.getCurrency() == currency) {
                throw new ForbiddenException("The account of this currency already exists");
            }
        });
        Account account = new Account();
        account.setBalance(0.0);
        account.setCurrency(currency);
        account.setCreationDate(new Date());
        account.setUpdateDate(new Date());
        account.setSoftDelete(false);
        account.setUser(user);
        double limit = 0;
        if (currency == Currency.ARS) {
            limit = 300000.0;
        } else {
            limit = 1000.0;
        }
        account.setTransactionLimit(limit);
        return accountMapper.accountToAccountDto(accountRepository.save(account));
    }

    @Override
    public AccountDto editById(Long id, Double transactionLimit) {
            if (transactionLimit == null)
                throw new BadRequestException("Transaction limit is mandatory");

            String loggedUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            long loggedUserId = userRepository.findByEmail(loggedUserEmail).getId();

            Optional<Account> account = accountRepository.findById(id);
            if (account.isEmpty())
                throw new NotFoundException("Account not found");

            if (account.get().getUser().getId() != loggedUserId)
                throw new ForbiddenException("You are not allowed to modify this account");

            account.get().setTransactionLimit(transactionLimit);
            account.get().setUpdateDate(new Date());
            return accountMapper.accountToAccountDto(accountRepository.save(account.get()));
        }

    @Override
    public AccountDto getById(Long id) {
        String loggedUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        long loggedUserId = userRepository.findByEmail(loggedUserEmail).getId();

        Optional<Account> account = accountRepository.findById(id);
        if (account.isEmpty())
            throw new NotFoundException("Account not found");

        if (account.get().getUser().getId() != loggedUserId)
            throw new ForbiddenException("You are not allowed to view this account");

        return accountMapper.accountToAccountDto(account.get());
    }

}

