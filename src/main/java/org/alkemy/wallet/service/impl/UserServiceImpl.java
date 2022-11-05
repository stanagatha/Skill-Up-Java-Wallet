package org.alkemy.wallet.service.impl;

import org.alkemy.wallet.dto.AccountDto;
import org.alkemy.wallet.dto.UserDto;
import org.alkemy.wallet.exception.CustomException;
import org.alkemy.wallet.mapper.UserMapper;
import org.alkemy.wallet.model.Account;
import org.alkemy.wallet.model.Currency;
import org.alkemy.wallet.model.RoleName;
import org.alkemy.wallet.model.User;
import org.alkemy.wallet.repository.IUserRepository;
import org.alkemy.wallet.service.IAccountService;
import org.alkemy.wallet.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final UserMapper userMapper;
    private final IAccountService accountService;


    @Autowired
    public UserServiceImpl(IUserRepository userRepository, UserMapper userMapper, IAccountService accountService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.accountService = accountService;
    }
	
    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().
                map(user -> userMapper.userToUserDTO(user)).
                collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String deleteById(Long id) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // TODO : Get the correct loggedUserId based on some information provided by auth
        // So far it is hardcoded the same user id
        long loggedUserId = id;
        boolean isLoggedUserAdmin = false;
        for (GrantedAuthority grantedAuthority : auth.getAuthorities()){
            if (grantedAuthority.getAuthority().equals(RoleName.ADMIN.name())){
                isLoggedUserAdmin = true;
            }
        }

        Optional<User> user = userRepository.findById(id);

        if (loggedUserId != id && !isLoggedUserAdmin)
            throw new CustomException("You are not allow to delete other users different than you.");

        if (user.isEmpty())
            throw new CustomException("No user with id: " + id);

        if (user.get().getSoftDelete())
            throw new CustomException("The user is already deleted.");

        user.get().setSoftDelete(true);
        return "User " + id + " successfully deleted.";
    }

	@Override
	public User save(User user) {
		User existUser = userRepository.findByEmail(user.getEmail());
		
		if(existUser!=null) {
			throw new CustomException("Email already exist");
		}
		
		return userRepository.save(user);		
	}

    @Override
    public List<String> getBalance() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepository.findByEmail(userEmail).getId();

        List<AccountDto> accounts = accountService.findAllByUser(userId);
        List<String> balances = new ArrayList<>();

        for (AccountDto account : accounts) {
            balances.add(account.getCurrency() + ": " + account.getBalance());
        }

        return balances;
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}
